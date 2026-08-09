package com.atmoto.recruit.biz.portal.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的投递列表 VO（S-014 我的投递页面）
 * <p>学生端投递列表每一项的数据结构</p>
 *
 * @author atmoto-recruit
 */
@Data
public class ApplicationListVo {

    /** 投递记录ID */
    private Long applicationId;

    /** 岗位名称 */
    private String jobTitle;

    /** 所属部门（公司/部门名） */
    private String company;

    /** 投递状态码（英文） */
    private String status;

    /** 投递状态标签（中文） */
    private String statusLabel;

    /** 投递时间 */
    private LocalDateTime applyTime;
}
