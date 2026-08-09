package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.vo.StudentManageVO;
import com.atmoto.recruit.biz.common.domain.Student;
import com.atmoto.recruit.biz.common.mapper.StudentMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 学生用户管理 Controller
 * <p>管理后台学生用户管理：分页查询、启用/禁用、逻辑删除</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/students")
public class StudentManageController {

    private final StudentMapper studentMapper;

    /** 合法的账号状态值集合 */
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "DISABLED");

    /**
     * 分页查询学生用户列表
     * <p>JOIN stu_user 和 stu_profile，返回手机号/姓名/邮箱/注册时间/状态/投递数</p>
     *
     * @param pageNum  当前页码（默认1）
     * @param pageSize 每页条数（默认20）
     * @param keyword  手机号/姓名模糊搜索（可空）
     * @param status   状态筛选（可空，ACTIVE/DISABLED）
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "20") int pageSize,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<StudentManageVO> page = new Page<>(pageNum, pageSize);
        IPage<StudentManageVO> result = studentMapper.selectStudentPage(page, keyword, status);
        // 电话号码脱敏：138****1111
        for (StudentManageVO vo : result.getRecords()) {
            if (vo.getPhone() != null && vo.getPhone().length() >= 7) {
                vo.setPhone(vo.getPhone().substring(0, 3) + "****" + vo.getPhone().substring(7));
            }
        }
        return AjaxResult.page(TableDataInfo.of(
                (int) result.getTotal(), result.getRecords()));
    }

    /**
     * 启用/禁用学生账号
     *
     * @param id     学生ID
     * @param status 目标状态：ACTIVE 或 DISABLED
     */
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(@PathVariable Long id,
                                   @RequestParam String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            return AjaxResult.error("无效的状态值，仅支持 ACTIVE / DISABLED");
        }

        Student student = studentMapper.selectById(id);
        if (student == null) {
            return AjaxResult.error("学生账号不存在");
        }

        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Student::getStudentId, id)
                .set(Student::getStatus, status);
        studentMapper.update(null, wrapper);

        String action = "ACTIVE".equals(status) ? "启用" : "禁用";
        log.info("学生账号{}成功：studentId={}, phone={}", action, id, student.getPhone());
        return AjaxResult.success(action + "成功");
    }

    /**
     * 逻辑删除学生账号
     * <p>MyBatis-Plus @TableLogic 自动将 del_flag 设为 '2'，不会物理删除</p>
     *
     * @param id 学生ID
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            return AjaxResult.error("学生账号不存在");
        }

        int rows = studentMapper.deleteById(id);
        if (rows > 0) {
            log.info("学生账号逻辑删除成功：studentId={}, phone={}", id, student.getPhone());
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }
}
