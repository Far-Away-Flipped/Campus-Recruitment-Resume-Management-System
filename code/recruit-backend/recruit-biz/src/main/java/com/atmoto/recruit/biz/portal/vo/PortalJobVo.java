package com.atmoto.recruit.biz.portal.vo;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门户端岗位详情 VO
 * <p>在 JobPosition 基础上扩充部门名称和岗位类别名称，供门户前端展示</p>
 *
 * @author atmoto-recruit
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalJobVo extends JobPosition {

    /** 部门名称（连表 sys_dept） */
    private String deptName;

    /** 岗位类别名称（连表 job_category） */
    private String categoryName;

    /** 当前学生是否已投递该岗位（匿名用户为false） */
    private Boolean hasApplied;
}
