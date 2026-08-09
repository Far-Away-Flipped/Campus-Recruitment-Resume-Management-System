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
import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.domain.StudentProfile;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.biz.common.mapper.StudentProfileMapper;
import com.atmoto.recruit.common.constant.BizConstants;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.List;
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

    /** 导出日累计计数缓存（Caffeine，24小时过期） */
    private final Cache<String, int[]> exportCountCache;

    public ResumeExportServiceImpl(ApplicationMapper applicationMapper,
                                   StudentProfileMapper studentProfileMapper,
                                   @Qualifier("exportCountCache") Cache<String, int[]> exportCountCache) {
        this.applicationMapper = applicationMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.exportCountCache = exportCountCache;
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

        // 5. 构建导出行数据（使用快照冗余列）
        List<ResumeExportRow> rows = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Application app : applications) {
            ResumeExportRow row = new ResumeExportRow();
            row.setName(app.getSnapshotName() != null ? app.getSnapshotName() : "");
            row.setSchool(app.getSnapshotSchool() != null ? app.getSnapshotSchool() : "");
            row.setMajor(app.getSnapshotMajor() != null ? app.getSnapshotMajor() : "");
            row.setDegree(app.getSnapshotDegree() != null ? app.getSnapshotDegree() : "");

            // 手机号脱敏：138****1234
            String phone = "";
            for (StudentProfile p : profiles) {
                if (p.getStudentId().equals(app.getStudentId())) {
                    phone = p.getPhone() != null ? p.getPhone() : "";
                    break;
                }
            }
            row.setPhone(maskPhone(phone));

            // 投递时间
            row.setApplyTime(app.getApplyTime() != null ? app.getApplyTime().format(dtf) : "");

            // 当前状态（中文标签）
            try {
                row.setStatus(ApplicationStatus.fromCode(app.getStatus()).getLabel());
            } catch (IllegalArgumentException e) {
                row.setStatus(app.getStatus());
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
     * 手机号脱敏：138****1234
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone != null ? phone : "";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
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

        @ExcelProperty(value = "手机号", index = 4)
        private String phone;

        @ExcelProperty(value = "投递时间", index = 5)
        private String applyTime;

        @ExcelProperty(value = "当前状态", index = 6)
        private String status;
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
