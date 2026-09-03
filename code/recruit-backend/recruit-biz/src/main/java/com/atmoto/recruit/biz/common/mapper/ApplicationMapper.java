package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.admin.vo.ReportDataVO;
import com.atmoto.recruit.biz.admin.vo.TrendReportVO;
import com.atmoto.recruit.biz.common.domain.Application;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 投递记录 Mapper
 * <p>含自定义查询方法用于HR简历管理的多维筛选+数据范围校验</p>
 *
 * @author atmoto-recruit
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    /**
     * 分页查询简历列表（含数据范围约束：仅当前HR负责的岗位）
     * <p>
     * LEFT JOIN job_position 用于数据范围校验：如果HR不是全部数据权限，
     * 则只返回 owner_user_id = #{ownerUserId} 的岗位下的投递记录。
     * </p>
     *
     * @param page         分页对象
     * @param jobId        岗位ID筛选（可空）
     * @param status       状态筛选（可空）
     * @param keyword      姓名/学校关键字模糊搜索（可空）
     * @param school       学校模糊筛选（可空）
     * @param major        专业模糊筛选（可空）
     * @param degree       学历精确筛选（可空）
     * @param applyTimeStart 投递时间起始（可空）
     * @param applyTimeEnd   投递时间截止（可空）
     * @param ownerUserId  数据范围限制：不为null时只查该HR负责的岗位（可空=全部权限）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT a.*, a.version_no AS version FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.del_flag = '0' " +
            "<if test='jobId != null'>AND a.job_id = #{jobId}</if> " +
            "<if test='status != null and status != \"\"'>AND a.status = #{status}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.snapshot_name LIKE CONCAT('%', #{keyword}, '%') OR a.snapshot_school LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> " +
            "<if test='school != null and school != \"\"'>AND a.snapshot_school LIKE CONCAT('%', #{school}, '%')</if> " +
            "<if test='major != null and major != \"\"'>AND a.snapshot_major LIKE CONCAT('%', #{major}, '%')</if> " +
            "<if test='degree != null and degree != \"\"'>AND a.snapshot_degree = #{degree}</if> " +
            "<if test='applyTimeStart != null'>AND a.apply_time &gt;= #{applyTimeStart}</if> " +
            "<if test='applyTimeEnd != null'>AND a.apply_time &lt;= #{applyTimeEnd}</if> " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if> " +
            "ORDER BY a.create_time DESC" +
            "</script>")
    IPage<Application> selectResumePage(Page<Application> page,
                                        @Param("jobId") Long jobId,
                                        @Param("status") String status,
                                        @Param("keyword") String keyword,
                                        @Param("school") String school,
                                        @Param("major") String major,
                                        @Param("degree") String degree,
                                        @Param("applyTimeStart") java.time.LocalDateTime applyTimeStart,
                                        @Param("applyTimeEnd") java.time.LocalDateTime applyTimeEnd,
                                        @Param("ownerUserId") Long ownerUserId);

    /**
     * 根据ID列表查询投递记录（用于批量导出）
     * <p>支持数据范围约束：仅返回当前HR有权限查看的投递记录</p>
     *
     * @param applicationIds 投递记录ID列表
     * @param ownerUserId    数据范围限制（可空=全部权限）
     * @return 符合条件的投递记录列表
     */
    @Select("<script>" +
            "SELECT a.*, a.version_no AS version FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.application_id IN " +
            "<foreach collection='applicationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND a.del_flag = '0' " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if>" +
            "</script>")
    List<Application> selectByIdsWithScope(@Param("applicationIds") List<Long> applicationIds,
                                           @Param("ownerUserId") Long ownerUserId);

    /**
     * 根据投递记录ID查询单个投递（含数据范围约束）
     *
     * @param applicationId 投递记录ID
     * @param ownerUserId   数据范围限制（可空=全部权限）
     * @return 投递记录（无权限时返回null）
     */
    @Select("<script>" +
            "SELECT a.*, a.version_no AS version FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.application_id = #{applicationId} AND a.del_flag = '0' " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if>" +
            "</script>")
    Application selectByIdWithScope(@Param("applicationId") Long applicationId,
                                     @Param("ownerUserId") Long ownerUserId);

    // ────────────────── 报表查询方法 ──────────────────

    /**
     * 投递总量趋势：按日期分组统计每日投递数
     * <p>含数据范围约束（ownerUserId），仅统计 del_flag='0' 的投递</p>
     *
     * @param startDate   起始日期（可空）
     * @param endDate     截止日期（可空）
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 按日期排序的每日投递数列表
     */
    @Select("<script>" +
            "SELECT DATE(a.apply_time) AS date, COUNT(*) AS count " +
            "FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.del_flag = '0' " +
            "<if test='startDate != null'>AND a.apply_time &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND a.apply_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if> " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if> " +
            "GROUP BY DATE(a.apply_time) " +
            "ORDER BY date ASC" +
            "</script>")
    List<TrendReportVO> countByApplyDate(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("ownerUserId") Long ownerUserId);

    /**
     * 岗位投递排行：按岗位分组统计投递数并降序排列
     * <p>LEFT JOIN job_position 获取岗位标题，含数据范围约束</p>
     *
     * @param topN        返回前N条（默认10）
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 岗位名+投递数的降序列表
     */
    @Select("<script>" +
            "SELECT jp.title AS name, COUNT(*) AS value " +
            "FROM app_application a " +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "WHERE a.del_flag = '0' " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if> " +
            "GROUP BY a.job_id, jp.title " +
            "ORDER BY value DESC " +
            "LIMIT #{topN}" +
            "</script>")
    List<ReportDataVO> countByJob(@Param("topN") int topN,
                                   @Param("ownerUserId") Long ownerUserId);

    /**
     * 学校分布：按快照学校字段分组统计投递数
     * <p>含数据范围约束</p>
     *
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 学校名+投递数的列表
     */
    @Select("<script>" +
            "SELECT a.snapshot_school AS name, COUNT(*) AS value " +
            "FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.del_flag = '0' " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if> " +
            "GROUP BY a.snapshot_school " +
            "ORDER BY value DESC" +
            "</script>")
    List<ReportDataVO> countBySnapshotSchool(@Param("ownerUserId") Long ownerUserId);

    /**
     * 学历分布：按快照学历字段分组统计投递数
     * <p>含数据范围约束</p>
     *
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 学历+投递数的列表
     */
    @Select("<script>" +
            "SELECT a.snapshot_degree AS name, COUNT(*) AS value " +
            "FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.del_flag = '0' " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if> " +
            "GROUP BY a.snapshot_degree " +
            "ORDER BY value DESC" +
            "</script>")
    List<ReportDataVO> countBySnapshotDegree(@Param("ownerUserId") Long ownerUserId);

    /**
     * 渠道来源分布：按 source 字段分组统计投递数
     * <p>支持可空日期范围筛选，含数据范围约束</p>
     *
     * @param startDate   起始日期（可空）
     * @param endDate     截止日期（可空）
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 渠道来源+投递数的列表
     */
    @Select("<script>" +
            "SELECT COALESCE(NULLIF(a.source_label, ''), a.source) AS name, COUNT(*) AS value " +
            "FROM app_application a " +
            "<if test='ownerUserId != null'>" +
            "LEFT JOIN job_position jp ON a.job_id = jp.job_id " +
            "</if>" +
            "WHERE a.del_flag = '0' " +
            "<if test='startDate != null'>AND a.apply_time &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND a.apply_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if> " +
            "<if test='ownerUserId != null'>AND jp.owner_user_id = #{ownerUserId}</if> " +
            "GROUP BY COALESCE(NULLIF(a.source_label, ''), a.source) " +
            "ORDER BY value DESC" +
            "</script>")
    List<ReportDataVO> countBySource(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("ownerUserId") Long ownerUserId);
}
