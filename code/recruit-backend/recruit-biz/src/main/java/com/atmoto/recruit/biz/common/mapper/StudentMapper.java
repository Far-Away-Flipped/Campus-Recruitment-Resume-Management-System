package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.admin.vo.StudentManageVO;
import com.atmoto.recruit.biz.common.domain.Student;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学生账号 Mapper
 * <p>含学生管理后台所需的列表查询方法</p>
 *
 * @author atmoto-recruit
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询学生用户列表（含投递数统计）
     * <p>LEFT JOIN stu_profile 获取姓名/邮箱，子查询统计投递数</p>
     *
     * @param page    分页对象
     * @param keyword 手机号/姓名模糊搜索（可空）
     * @param status  账号状态筛选（可空）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT s.student_id AS studentId, s.phone AS phone, " +
            "COALESCE(p.real_name, s.real_name) AS realName, " +
            "COALESCE(p.email, s.email) AS email, " +
            "s.create_time AS createTime, s.status AS status, " +
            "(SELECT COUNT(*) FROM app_application a WHERE a.student_id = s.student_id AND a.del_flag = '0') AS applyCount " +
            "FROM stu_user s " +
            "LEFT JOIN stu_profile p ON s.student_id = p.student_id " +
            "WHERE s.del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (s.phone LIKE CONCAT('%', #{keyword}, '%') OR COALESCE(p.real_name, s.real_name) LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>AND s.status = #{status}</if> " +
            "ORDER BY s.create_time DESC" +
            "</script>")
    IPage<StudentManageVO> selectStudentPage(Page<StudentManageVO> page,
                                              @Param("keyword") String keyword,
                                              @Param("status") String status);
}
