package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.admin.vo.*;
import com.atmoto.recruit.biz.common.domain.Student;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生账号 Mapper
 * <p>含学生管理后台所需的列表查询与详情查询方法</p>
 *
 * @author atmoto-recruit
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询学生用户列表（含投递数 + 最高学历学校/专业）
     * <p>ROW_NUMBER() 窗口函数按学历优先级取一行，保证 schoolName/major 原子性</p>
     *
     * @param sortColumn 排序 SQL 列（必须经 Controller 白名单映射后传入，禁止直接接收用户输入）
     * @param sortOrder  排序方向 asc/desc（同上，必须经白名单校验）
     */
    @Select("<script>" +
            "SELECT s.student_id AS studentId, s.phone AS phone, " +
            "COALESCE(p.real_name, s.real_name) AS realName, " +
            "COALESCE(edu.school_name, '') AS schoolName, " +
            "COALESCE(edu.major, '') AS major, " +
            "s.create_time AS createTime, s.status AS status, " +
            "(SELECT COUNT(*) FROM app_application a WHERE a.student_id = s.student_id AND a.del_flag = '0') AS applyCount " +
            "FROM stu_user s " +
            "LEFT JOIN stu_profile p ON s.student_id = p.student_id " +
            "LEFT JOIN ( " +
            "  SELECT student_id, school_name, major, degree, " +
            "    ROW_NUMBER() OVER (PARTITION BY student_id ORDER BY FIELD(degree, '博士','硕士','本科','大专','其他'), sort_order ASC) AS rn " +
            "  FROM stu_education WHERE del_flag = '0' " +
            ") edu ON s.student_id = edu.student_id AND edu.rn = 1 " +
            "WHERE s.del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (s.phone LIKE CONCAT('%', #{keyword}, '%') OR COALESCE(p.real_name, s.real_name) LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>AND s.status = #{status}</if> " +
            "ORDER BY ${sortColumn} IS NULL, ${sortColumn} ${sortOrder}, s.create_time DESC" +
            "</script>")
    IPage<StudentManageVO> selectStudentPage(Page<StudentManageVO> page,
                                              @Param("keyword") String keyword,
                                              @Param("status") String status,
                                              @Param("sortColumn") String sortColumn,
                                              @Param("sortOrder") String sortOrder);

    /** 查询学生基本信息（详情用，手机号不脱敏） */
    @Select("SELECT s.student_id AS studentId, s.phone AS phone, s.status AS status, " +
            "s.create_time AS createTime, " +
            "COALESCE(p.real_name, s.real_name) AS realName, " +
            "COALESCE(p.email, s.email) AS email, " +
            "p.gender AS gender, p.birth_date AS birthDate, " +
            "p.current_residence AS currentCity, p.avatar_url AS avatarUrl " +
            "FROM stu_user s " +
            "LEFT JOIN stu_profile p ON s.student_id = p.student_id " +
            "WHERE s.student_id = #{studentId} AND s.del_flag = '0'")
    StudentDetailVO selectStudentDetail(@Param("studentId") Long studentId);

    /** 查询教育经历（按 sort_order 升序） */
    @Select("SELECT id, school_name AS schoolName, major, degree, " +
            "start_date AS startDate, end_date AS endDate, gpa_rank AS gpa " +
            "FROM stu_education " +
            "WHERE student_id = #{studentId} AND del_flag = '0' " +
            "ORDER BY sort_order ASC")
    List<EducationVO> selectEducationsByStudentId(@Param("studentId") Long studentId);

    /** 查询实习/项目经历（按 sort_order 升序） */
    @Select("SELECT id, record_type AS recordType, org_name AS orgName, " +
            "position, start_date AS startDate, end_date AS endDate, description " +
            "FROM stu_internship " +
            "WHERE student_id = #{studentId} AND del_flag = '0' " +
            "ORDER BY sort_order ASC")
    List<InternshipBriefVO> selectInternshipsByStudentId(@Param("studentId") Long studentId);

    /** 查询简历附件（当前版本，按上传时间降序） */
    @Select("SELECT id, file_name AS originalName, file_type AS fileExt, " +
            "file_size AS fileSize, preview_status AS previewStatus, " +
            "upload_time AS uploadTime " +
            "FROM stu_resume_file " +
            "WHERE student_id = #{studentId} AND is_current = '1' AND del_flag = '0' " +
            "ORDER BY upload_time DESC")
    List<ResumeFileBriefVO> selectResumeFilesByStudentId(@Param("studentId") Long studentId);

    /** 查询投递历史（JOIN job_position 取岗位名，最近10条） */
    @Select("SELECT a.application_id AS applicationId, " +
            "COALESCE(j.title, '(已删除岗位)') AS jobTitle, " +
            "a.status AS status, a.apply_time AS applyTime " +
            "FROM app_application a " +
            "LEFT JOIN job_position j ON a.job_id = j.job_id " +
            "WHERE a.student_id = #{studentId} AND a.del_flag = '0' " +
            "ORDER BY a.apply_time DESC LIMIT 10")
    List<ApplicationBriefVO> selectApplicationsByStudentId(@Param("studentId") Long studentId);
}
