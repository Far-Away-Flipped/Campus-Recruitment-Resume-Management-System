package com.atmoto.recruit.biz.admin.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.atmoto.recruit.biz.admin.service.ResumeExportService;
import com.atmoto.recruit.biz.common.domain.AppHrNote;
import com.atmoto.recruit.biz.common.domain.AppSnapshot;
import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.domain.StudentProfile;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.enums.SourceChannel;
import com.atmoto.recruit.biz.common.mapper.AppHrNoteMapper;
import com.atmoto.recruit.biz.common.mapper.AppSnapshotMapper;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.biz.common.mapper.StudentProfileMapper;
import com.atmoto.recruit.common.constant.BizConstants;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.system.domain.SysDictData;
import com.atmoto.recruit.system.service.ISysDictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 简历导出服务实现
 * <p>EasyExcel导出，含水印、日累计限额、审计留痕。导出字段来自快照冗余列，手机号脱敏。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
public class ResumeExportServiceImpl implements ResumeExportService {

    private final ApplicationMapper applicationMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final AppSnapshotMapper appSnapshotMapper;
    private final AppHrNoteMapper appHrNoteMapper;
    private final ObjectMapper objectMapper;
    private final ISysDictDataService dictDataService;

    /** 导出日累计计数缓存（Caffeine，24小时过期） */
    private final Cache<String, int[]> exportCountCache;

    public ResumeExportServiceImpl(ApplicationMapper applicationMapper,
                                   StudentProfileMapper studentProfileMapper,
                                   AppSnapshotMapper appSnapshotMapper,
                                   AppHrNoteMapper appHrNoteMapper,
                                   ObjectMapper objectMapper,
                                   ISysDictDataService dictDataService,
                                   @Qualifier("exportCountCache") Cache<String, int[]> exportCountCache) {
        this.applicationMapper = applicationMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.appSnapshotMapper = appSnapshotMapper;
        this.appHrNoteMapper = appHrNoteMapper;
        this.objectMapper = objectMapper;
        this.dictDataService = dictDataService;
        this.exportCountCache = exportCountCache;
    }

    /** 实习/项目 recordType → 中文 */
    private static final Map<String, String> RECORD_TYPE_LABELS = new LinkedHashMap<>() {{
        put("I", "实习经历");
        put("P", "项目经历");
    }};

    /** 技能/证书 certType → 中文 */
    private static final Map<String, String> CERT_TYPE_LABELS = new LinkedHashMap<>() {{
        put("SKILL", "技能");
        put("CERT", "证书");
        put("LANGUAGE", "语言能力");
    }};

    /** 性别 → 中文 */
    private static final Map<String, String> GENDER_LABELS = new LinkedHashMap<>() {{
        put("M", "男");
        put("F", "女");
        put("O", "其他");
    }};

    /** 学历字典缓存（懒加载：首次导出时从 education_degree 字典读取，避免硬编码漂移） */
    private Map<String, String> degreeLabelsCache = null;

    /**
     * 学历等级（值越大学历越高），口径与学生管理列表"最高学历"一致：
     * 博士 > 硕士 > 本科 > 大专 > 其他 > 不限；未知码/空最低。
     */
    private static int educationRank(String code) {
        if (code == null) return -1;
        switch (code) {
            case "DOCTOR": return 5;
            case "MASTER": return 4;
            case "BACHELOR": return 3;
            case "ASSOCIATE": return 2;
            case "OTHER": return 1;
            case "NONE": return 0;
            default: return -1;
        }
    }

    /** 临时文件导出目录 */
    private static final String EXPORT_DIR = System.getProperty("java.io.tmpdir")
            + File.separator + "recruit-export";

    @Override
    public String exportResumeList(List<Long> applicationIds, Long operatorUserId,
                                   String operatorName, boolean hasAllDataScope) {
        // 1. 参数校验
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择要导出的简历");
        }

        // 2. 日累计上限校验（2000条）
        String today = LocalDate.now().toString();
        String countKey = "export:daily:" + operatorUserId + ":" + today;
        int[] counters = exportCountCache.get(countKey, k -> new int[]{0});
        int currentCount;
        synchronized (counters) {
            int newTotal = counters[0] + applicationIds.size();
            if (newTotal > BizConstants.EXPORT_DAILY_LIMIT) {
                throw new BizException(ErrorCode.EXPORT_DAILY_LIMIT,
                        "今日已导出 " + counters[0] + " 条，本次 " + applicationIds.size()
                                + " 条将超出日上限 " + BizConstants.EXPORT_DAILY_LIMIT + " 条");
            }
            counters[0] = newTotal;
            currentCount = counters[0];
        }
        log.info("导出计数：operatorId={}, 本次{}条, 今日累计{}条", operatorUserId, applicationIds.size(), currentCount);

        // 3. 数据范围约束：仅返回当前HR有权限查看的投递记录
        Long ownerUserId = hasAllDataScope ? null : operatorUserId;
        List<Application> applications = applicationMapper.selectByIdsWithScope(applicationIds, ownerUserId);
        if (applications == null || applications.isEmpty()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "所选投递记录不存在或无权限导出");
        }

        // 4. 批量查询学生资料（获取手机号用于脱敏）
        Set<Long> studentIds = applications.stream()
                .map(Application::getStudentId)
                .collect(Collectors.toSet());
        List<StudentProfile> profiles = studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>()
                        .in(StudentProfile::getStudentId, studentIds));

        // 5. 构建导出行数据（基本信息用快照冗余列，经历用投递快照 JSON）
        List<ResumeExportRow> rows = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Application app : applications) {
            ResumeExportRow row = new ResumeExportRow();
            row.setName(app.getSnapshotName() != null ? app.getSnapshotName() : "");
            row.setSchool(app.getSnapshotSchool() != null ? app.getSnapshotSchool() : "");
            row.setMajor(app.getSnapshotMajor() != null ? app.getSnapshotMajor() : "");
            row.setDegree(degreeLabel(app.getSnapshotDegree()));

            // 性别：快照无性别冗余列，从 stu_profile 实时取（静态字段，投递后不变也合理）
            StudentProfile matchedProfile = null;
            for (StudentProfile p : profiles) {
                if (p.getStudentId().equals(app.getStudentId())) {
                    matchedProfile = p;
                    break;
                }
            }
            row.setGender(matchedProfile != null && matchedProfile.getGender() != null
                    ? GENDER_LABELS.getOrDefault(matchedProfile.getGender(), matchedProfile.getGender()) : "");

            // 手机号脱敏：138****1234
            String phone = matchedProfile != null && matchedProfile.getPhone() != null
                    ? matchedProfile.getPhone() : "";
            row.setPhone(maskPhone(phone));

            // 投递时间
            row.setApplyTime(app.getApplyTime() != null ? app.getApplyTime().format(dtf) : "");

            // 当前状态（中文标签）
            try {
                row.setStatus(ApplicationStatus.fromCode(app.getStatus()).getLabel());
            } catch (IllegalArgumentException e) {
                row.setStatus(app.getStatus());
            }

            // 来源渠道：读投递时固化的中文快照；存量/异常时按字典或枚举兜底为中文，仍未知原码
            row.setSourceLabel(app.getSourceLabel() != null && !app.getSourceLabel().isBlank()
                    ? app.getSourceLabel() : sourceLabelFallback(app.getSource()));

            // 教育经历、实习/项目、技能/证书、社团经历 —— 从投递快照读（改造前老快照无实习/技能/社团列为空，教育快照自始即有）
            if (app.getCurrentSnapshotId() != null) {
                AppSnapshot snapshot = appSnapshotMapper.selectById(app.getCurrentSnapshotId());
                if (snapshot != null) {
                    row.setEducations(parseEducations(snapshot.getSnapshotEducations()));
                    // 学校/专业/学历三列：优先取快照教育中的最高学历一条（按学历等级，与学生管理列表口径一致），
                    // 解析不到则保留上方投递冗余列值兜底
                    Map<String, Object> highest = pickHighestEducation(snapshot.getSnapshotEducations());
                    if (highest != null) {
                        String hs = str(highest, "schoolName");
                        row.setSchool(hs != null ? hs : "");
                        String hm = str(highest, "major");
                        row.setMajor(hm != null ? hm : "");
                        row.setDegree(degreeLabel(str(highest, "degree")));
                    }
                    row.setInternships(parseInternships(snapshot.getSnapshotInternships()));
                    row.setCertificates(parseCertificates(snapshot.getSnapshotCertificates()));
                    row.setActivities(parseActivities(snapshot.getSnapshotActivities()));
                }
            }

            rows.add(row);
        }

        // 6. 生成含有Header水印的Excel文件
        File exportDir = new File(EXPORT_DIR);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String fileName = "简历导出_" + operatorName + "_" + today.replace("-", "") + "_"
                + System.currentTimeMillis() + ".xlsx";
        String filePath = EXPORT_DIR + File.separator + fileName;

        // 水印文字（打印时可见于页眉三处）
        String watermarkText = "遨天科技校园招聘 内部资料 " + operatorName + " " + today;

        // 表头样式
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headFont.setFontHeightInPoints((short) 11);
        headStyle.setWriteFont(headFont);

        EasyExcel.write(filePath, ResumeExportRow.class)
                .registerWriteHandler(new WatermarkSheetHandler(watermarkText))
                .registerWriteHandler(new HorizontalCellStyleStrategy(headStyle, new WriteCellStyle()))
                .sheet("简历列表")
                .doWrite(rows);

        log.info("导出Excel完成：filePath={}, rowCount={}, operatorId={}",
                filePath, rows.size(), operatorUserId);

        // 7. TODO: 审计留痕 —— 记录导出操作到 audit_resume_access 表
        // 后续里程碑接入审计模块

        return filePath;
    }

    /**
     * 来源渠道中文兜底：码 → SourceChannel 枚举中文；已是中文/未知值原样返回（可审计）
     */
    private String sourceLabelFallback(String source) {
        if (source == null || source.isBlank()) return "";
        for (SourceChannel c : SourceChannel.values()) {
            if (c.getCode().equals(source)) return c.getLabel();
        }
        return source;
    }

    /**
     * 手机号脱敏：138****1234
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone != null ? phone : "";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 学历码值 → 中文（从 education_degree 字典读取，未知码原样返回） */
    private String degreeLabel(String code) {
        if (code == null) return "";
        if (degreeLabelsCache == null) {
            Map<String, String> map = new LinkedHashMap<>();
            try {
                List<SysDictData> dict = dictDataService.selectDictDataByType("education_degree");
                if (dict != null) {
                    for (SysDictData d : dict) {
                        map.put(d.getDictValue(), d.getDictLabel());
                    }
                }
            } catch (Exception e) {
                log.warn("读取学历字典失败,学历将按码值导出: {}", e.getMessage());
            }
            degreeLabelsCache = map;
        }
        return degreeLabelsCache.getOrDefault(code, code);
    }

    /**
     * 解析教育经历快照 JSON → 可读文本（导出全部教育经历，非仅最高学历）
     * 快照 key 与实体字段对齐：schoolName/major/degree/startDate/endDate/gpa
     * 学历 degree 为码值，经字典转中文；多条用换行分隔
     */
    private String parseEducations(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null || list.isEmpty()) return "";
            List<String> lines = new ArrayList<>();
            for (Map<String, Object> m : list) {
                StringBuilder sb = new StringBuilder();
                sb.append(str(m, "schoolName") != null ? str(m, "schoolName") : "");
                String major = str(m, "major");
                if (major != null && !major.isEmpty()) {
                    sb.append(" / ").append(major);
                }
                String degree = degreeLabel(str(m, "degree"));
                if (!degree.isEmpty()) {
                    sb.append(" / ").append(degree);
                }
                String dates = dateRange(str(m, "startDate"), str(m, "endDate"));
                if (dates != null && !dates.isEmpty()) {
                    sb.append("（").append(dates).append("）");
                }
                String gpa = str(m, "gpa");
                if (gpa != null && !gpa.isEmpty()) {
                    sb.append(" GPA:").append(gpa);
                }
                lines.add(sb.toString());
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            log.warn("导出教育经历快照解析失败,置空: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从教育经历快照 JSON 中挑最高学历那条（按 educationRank 等级，同学生管理列表口径）；
     * 空/解析失败返回 null。
     */
    private Map<String, Object> pickHighestEducation(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null || list.isEmpty()) return null;
            Map<String, Object> best = null;
            int bestRank = -1;
            for (Map<String, Object> m : list) {
                int rank = educationRank(str(m, "degree"));
                if (rank > bestRank) {
                    bestRank = rank;
                    best = m;
                }
            }
            return best;
        } catch (Exception e) {
            log.warn("导出教育经历快照解析失败(最高学历): {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析实习/项目经历快照 JSON → 可读文本
     * 快照 key 与 VO 字段对齐：recordType/orgName/position/startDate/endDate/description
     */
    private String parseInternships(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null || list.isEmpty()) return "";
            List<String> lines = new ArrayList<>();
            for (Map<String, Object> m : list) {
                StringBuilder sb = new StringBuilder();
                String recordType = str(m, "recordType");
                if (recordType != null && !recordType.isEmpty()) {
                    sb.append(RECORD_TYPE_LABELS.getOrDefault(recordType, recordType)).append("：");
                }
                sb.append(str(m, "orgName") != null && !str(m, "orgName").isEmpty() ? str(m, "orgName") : "");
                String position = str(m, "position");
                if (position != null && !position.isEmpty()) {
                    sb.append(" / ").append(position);
                }
                String dates = dateRange(str(m, "startDate"), str(m, "endDate"));
                if (dates != null && !dates.isEmpty()) {
                    sb.append("（").append(dates).append("）");
                }
                String desc = str(m, "description");
                if (desc != null && !desc.isEmpty()) {
                    sb.append(" ").append(desc);
                }
                lines.add(sb.toString());
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            log.warn("导出实习经历快照解析失败,置空: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析技能/证书/语言快照 JSON → 可读文本
     * 快照 key：certType/certName/certLevel/description
     */
    private String parseCertificates(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null || list.isEmpty()) return "";
            List<String> lines = new ArrayList<>();
            for (Map<String, Object> m : list) {
                StringBuilder sb = new StringBuilder();
                String certType = str(m, "certType");
                if (certType != null && !certType.isEmpty()) {
                    sb.append(CERT_TYPE_LABELS.getOrDefault(certType, certType)).append("：");
                }
                sb.append(str(m, "certName") != null ? str(m, "certName") : "");
                String level = str(m, "certLevel");
                if (level != null && !level.isEmpty()) {
                    sb.append(" / ").append(level);
                }
                String desc = str(m, "description");
                if (desc != null && !desc.isEmpty()) {
                    sb.append(" ").append(desc);
                }
                lines.add(sb.toString());
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            log.warn("导出技能/证书快照解析失败,置空: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析社团/校园经历快照 JSON → 可读文本
     * 快照 key：orgName/position/description
     */
    private String parseActivities(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null || list.isEmpty()) return "";
            List<String> lines = new ArrayList<>();
            for (Map<String, Object> m : list) {
                StringBuilder sb = new StringBuilder();
                sb.append(str(m, "orgName") != null ? str(m, "orgName") : "");
                String position = str(m, "position");
                if (position != null && !position.isEmpty()) {
                    sb.append(" / ").append(position);
                }
                String desc = str(m, "description");
                if (desc != null && !desc.isEmpty()) {
                    sb.append(" ").append(desc);
                }
                lines.add(sb.toString());
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            log.warn("导出社团经历快照解析失败,置空: {}", e.getMessage());
            return "";
        }
    }

    /** 取 Map 中 String 值（null 安全） */
    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    /** 起止日期区间文本，如 2025-07-01 ~ 2025-12-31 */
    private String dateRange(String start, String end) {
        if ((start == null || start.isEmpty()) && (end == null || end.isEmpty())) return "";
        return (start == null || start.isEmpty() ? "?" : start) + " ~ " + (end == null || end.isEmpty() ? "至今" : end);
    }

    // ── 内部类：Excel 导出行映射 ──

    @Data
    @ExcelIgnoreUnannotated
    public static class ResumeExportRow {
        @ExcelProperty(value = "姓名", index = 0)
        private String name;

        @ExcelProperty(value = "学校", index = 1)
        private String school;

        @ExcelProperty(value = "专业", index = 2)
        private String major;

        @ExcelProperty(value = "学历", index = 3)
        private String degree;

        @ExcelProperty(value = "性别", index = 4)
        private String gender;

        @ExcelProperty(value = "手机号", index = 5)
        private String phone;

        @ExcelProperty(value = "投递时间", index = 6)
        private String applyTime;

        @ExcelProperty(value = "当前状态", index = 7)
        private String status;

        @ExcelProperty(value = "实习/项目经历", index = 8)
        private String internships;

        @ExcelProperty(value = "技能证书/语言", index = 9)
        private String certificates;

        @ExcelProperty(value = "社团/校园经历", index = 10)
        private String activities;

        @ExcelProperty(value = "来源渠道", index = 11)
        private String sourceLabel;

        @ExcelProperty(value = "教育经历(全部)", index = 12)
        private String educations;
    }

    // ── 内部类：Header水印处理器 ──

    /**
     * Excel 页眉水印处理器
     * <p>在打印/预览时将水印文字置于页眉三处（左/中/右），不遮挡表格内容</p>
     */
    private static class WatermarkSheetHandler implements SheetWriteHandler {

        private final String watermarkText;

        WatermarkSheetHandler(String watermarkText) {
            this.watermarkText = watermarkText;
        }

        @Override
        public void afterSheetCreate(SheetWriteHandlerContext context) {
            Sheet sheet = context.getWriteSheetHolder().getSheet();
            // Header水印文字：灰色、小字、微软雅黑
            String headerStr = "&\"微软雅黑,常规\"&10&808080" + watermarkText;
            Header header = sheet.getHeader();
            header.setLeft(headerStr);
            header.setCenter(headerStr);
            header.setRight(headerStr);
        }
    }
}
