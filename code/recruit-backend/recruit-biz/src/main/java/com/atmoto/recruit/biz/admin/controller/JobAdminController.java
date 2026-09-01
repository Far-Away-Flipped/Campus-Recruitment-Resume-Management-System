package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.JobPositionService;
import com.atmoto.recruit.biz.common.domain.JobCategory;
import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.mapper.JobCategoryMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.domain.SysDictData;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.atmoto.recruit.system.service.ISysDictDataService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 岗位管理 Controller（管理后台）
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/jobs")
public class JobAdminController {

    /** 富文本安全白名单：允许基础格式化标签，禁止脚本和事件属性 */
    private static final Safelist SAFE_HTML = Safelist.basic()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "table", "thead", "tbody", "tr", "td", "th",
                    "img", "span", "div", "pre", "blockquote", "hr")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("a", "target", "rel")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")
            .addProtocols("img", "src", "http", "https");

    private final JobPositionService jobPositionService;
    private final SysDeptMapper deptMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final ObjectMapper objectMapper;
    private final ISysDictDataService dictDataService;

    @GetMapping("/list")
    public AjaxResult list(JobPosition jobPosition, PageQuery pageQuery,
                           @RequestParam(required = false) String sortField,
                           @RequestParam(required = false) String sortDir) {
        if (sortField != null && !sortField.isBlank()) {
            pageQuery.setOrderByColumn(sortField);
            pageQuery.setIsAsc(sortDir != null ? sortDir : "desc");
        }
        IPage<JobPosition> page = jobPositionService.selectJobList(jobPosition, pageQuery);
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            // 批量填充部门名称
            Set<Long> deptIds = page.getRecords().stream()
                    .map(JobPosition::getDeptId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!deptIds.isEmpty()) {
                List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
                Map<Long, String> deptNameMap = depts.stream()
                        .collect(Collectors.toMap(SysDept::getDeptId, SysDept::getDeptName, (a, b) -> a));
                page.getRecords().forEach(j -> j.setDeptName(deptNameMap.getOrDefault(j.getDeptId(), "")));
            }
            // 批量填充岗位分类名称
            Set<Long> categoryIds = page.getRecords().stream()
                    .map(JobPosition::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!categoryIds.isEmpty()) {
                List<JobCategory> categories = jobCategoryMapper.selectBatchIds(categoryIds);
                Map<Long, String> catNameMap = categories.stream()
                        .collect(Collectors.toMap(JobCategory::getCategoryId, JobCategory::getCategoryName, (a, b) -> a));
                page.getRecords().forEach(j -> j.setCategoryName(catNameMap.getOrDefault(j.getCategoryId(), "")));
            }
        }
        TableDataInfo dataInfo = TableDataInfo.of(page.getTotal(), page.getRecords());
        return AjaxResult.page(dataInfo);
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        JobPosition job = jobPositionService.selectJobById(id);
        if (job.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(job.getDeptId());
            job.setDeptName(dept != null ? dept.getDeptName() : "");
        }
        if (job.getCategoryId() != null) {
            JobCategory cat = jobCategoryMapper.selectById(job.getCategoryId());
            job.setCategoryName(cat != null ? cat.getCategoryName() : "");
        }
        return AjaxResult.success(job);
    }

    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        String title = str(body, "title");
        Long deptId = lng(body, "deptId");
        Long categoryId = lng(body, "categoryId");
        String location = loc(body, "location");
        String degreeRequirement = str(body, "degreeRequirement");
        String description = str(body, "description");
        String requirement = str(body, "requirement");
        String tags = tags(body);

        if (title == null || title.isBlank()) return AjaxResult.error("岗位名称不能为空");
        title = Jsoup.clean(title, Safelist.none());
        description = description != null ? Jsoup.clean(description, SAFE_HTML) : null;
        requirement = requirement != null ? Jsoup.clean(requirement, SAFE_HTML) : null;
        if (deptId == null) return AjaxResult.error("请选择所属部门");
        if (categoryId == null) return AjaxResult.error("请选择岗位类别");
        if (location == null || location.isBlank()) return AjaxResult.error("请选择工作地点");
        if (degreeRequirement == null || !isValidDegree(degreeRequirement)) return AjaxResult.error("学历要求不合法");

        String deadlineStr = str(body, "deadline");
        LocalDateTime deadline = null;
        if (deadlineStr != null && !deadlineStr.isBlank()) {
            try {
                if (deadlineStr.length() == 10) deadlineStr += "T23:59:59";
                deadline = LocalDateTime.parse(deadlineStr);
            } catch (Exception e) {
                return AjaxResult.error("截止日期格式不正确，请重新选择");
            }
        }
        if (deadline == null) return AjaxResult.error("请设置截止日期");
        if (deadline.isBefore(LocalDateTime.now())) return AjaxResult.error("截止日期不能早于当前时间");

        // 招聘人数校验：必须为正整数
        Integer headcount = body.get("headcount") != null ?
            Integer.parseInt(body.get("headcount").toString()) : 1;
        if (headcount <= 0) return AjaxResult.error("招聘人数必须大于0");

        JobPosition jobPosition = new JobPosition();
        jobPosition.setTitle(title);
        jobPosition.setDeptId(deptId);
        jobPosition.setCategoryId(categoryId);
        jobPosition.setLocation(location);
        jobPosition.setDegreeRequirement(degreeRequirement);
        jobPosition.setDescription(description);
        jobPosition.setRequirement(requirement);
        jobPosition.setTags(tags);
        jobPosition.setDeadline(deadline);
        jobPosition.setStatus("DRAFT");
        jobPosition.setHeadcount(headcount);

        int rows = jobPositionService.insertJob(jobPosition);
        return rows > 0 ? AjaxResult.success("创建成功", Map.of("jobId", jobPosition.getJobId())) : AjaxResult.error("新增岗位失败");
    }

    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> body) {
        Long jobId = lng(body, "jobId");
        String title = str(body, "title");
        Long deptId = lng(body, "deptId");
        Long categoryId = lng(body, "categoryId");
        String location = loc(body, "location");
        String degreeRequirement = str(body, "degreeRequirement");
        String description = str(body, "description");
        String requirement = str(body, "requirement");
        String tags = tags(body);
        String status = str(body, "status");

        if (jobId == null) return AjaxResult.error("岗位ID不能为空");
        if (title == null || title.isBlank()) return AjaxResult.error("岗位名称不能为空");
        title = Jsoup.clean(title, Safelist.none());
        description = description != null ? Jsoup.clean(description, SAFE_HTML) : null;
        requirement = requirement != null ? Jsoup.clean(requirement, SAFE_HTML) : null;
        if (degreeRequirement != null && !isValidDegree(degreeRequirement)) return AjaxResult.error("学历要求不合法");

        String deadlineStr = str(body, "deadline");
        LocalDateTime deadline = null;
        if (deadlineStr != null && !deadlineStr.isBlank()) {
            try {
                if (deadlineStr.length() == 10) deadlineStr += "T23:59:59";
                deadline = LocalDateTime.parse(deadlineStr);
            } catch (Exception e) {
                return AjaxResult.error("截止日期格式不正确，请重新选择");
            }
        }

        JobPosition update = new JobPosition();
        update.setJobId(jobId);
        if (title != null) update.setTitle(title);
        if (deptId != null) update.setDeptId(deptId);
        if (categoryId != null) update.setCategoryId(categoryId);
        if (location != null) update.setLocation(location);
        if (degreeRequirement != null) update.setDegreeRequirement(degreeRequirement);
        if (description != null) update.setDescription(description);
        if (requirement != null) update.setRequirement(requirement);
        if (tags != null) update.setTags(tags);
        // EXPIRED 为实时派生态（不持久化），编辑不接受写回 EXPIRED（保存草稿传 DRAFT 仍生效）
        if (status != null && !JobStatus.EXPIRED.getCode().equals(status)) update.setStatus(status);
        if (deadline != null) update.setDeadline(deadline);

        int rows = jobPositionService.updateJob(update);
        return rows > 0 ? AjaxResult.success("修改岗位成功") : AjaxResult.error("修改岗位失败");
    }

    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        int rows = jobPositionService.publishJob(id);
        return rows > 0 ? AjaxResult.success("岗位发布成功") : AjaxResult.error("岗位发布失败");
    }

    @PutMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id) {
        int rows = jobPositionService.offlineJob(id);
        return rows > 0 ? AjaxResult.success("岗位下架成功") : AjaxResult.error("岗位下架失败");
    }

    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int rows = jobPositionService.deleteJob(id);
        return rows > 0 ? AjaxResult.success("删除成功") : AjaxResult.error("删除失败");
    }

    // ── 辅助方法 ──
    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : null;
    }
    /**
     * 工作地点接参：兼容三种形态 → 统一输出 JSON 数组文本（如 ["BEIJING","SHANGHAI"]）
     * 1. 前端多选提交的 JSON 数组（List）
     * 2. 旧前端/直接调用提交的单值字符串（北京 或 BEIJING）
     * 3. 字符串形式的 JSON 数组文本（"[\"BEIJING\"]"）
     */
    private String loc(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        List<String> codes = new ArrayList<>();
        if (v instanceof List<?> list) {
            list.forEach(item -> { if (item != null) codes.add(String.valueOf(item)); });
        } else {
            String s = v.toString().trim();
            if (s.startsWith("[")) {
                try {
                    codes.addAll(objectMapper.readValue(s, new TypeReference<List<String>>() {}));
                } catch (Exception e) {
                    return null;
                }
            } else if (!s.isEmpty()) {
                codes.add(s);
            }
        }
        if (codes.isEmpty()) return "";
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * 标签接参：兼容三种形态 → 统一输出合法 JSON 数组文本（如 ["急聘","Java"]）
     * 1. 前端 el-input-tag 提交的 JSON 数组（List）
     * 2. 旧前端/直接调用提交的逗号分隔字符串（"Java,应届生"）
     * 3. 字符串形式的 JSON 数组文本（"[\"Java\"]"）
     * <p>避免 str() 对 List.toString() 产生 "[Java, 应届生]" 伪 JSON，导致每次编辑嵌套一层方括号。</p>
     */
    private String tags(Map<String, Object> body) {
        Object v = body.get("tags");
        if (v == null) return null;
        List<String> list = new ArrayList<>();
        if (v instanceof List<?> arr) {
            arr.forEach(item -> { if (item != null) list.add(String.valueOf(item)); });
        } else {
            String s = v.toString().trim();
            if (s.startsWith("[")) {
                try {
                    list.addAll(objectMapper.readValue(s, new TypeReference<List<String>>() {}));
                } catch (Exception e) {
                    return null;
                }
            } else if (!s.isEmpty()) {
                // 兼容逗号分隔字符串
                java.util.Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .forEach(list::add);
            }
        }
        if (list.isEmpty()) return "";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * 校验学历要求是否为 education_degree 字典中的码值（白名单）
     * <p>防止前端误发中文 label（如"本科"）入库，导致学生端按码值等值筛选时
     * 岗位从学历筛选结果中静默消失。</p>
     */
    private boolean isValidDegree(String degree) {
        if (degree == null || degree.isBlank()) return false;
        return dictDataService.selectDictDataByType("education_degree").stream()
                .map(SysDictData::getDictValue)
                .anyMatch(degree::equals);
    }
    private Long lng(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }
}
