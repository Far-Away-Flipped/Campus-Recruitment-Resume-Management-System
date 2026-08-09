package com.atmoto.recruit.biz.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报表通用数据VO
 * <p>用于岗位排行、学校分布、学历分布、渠道来源分布等 name-value 型报表</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataVO {

    /** 分组名称（岗位名/学校名/学历/渠道来源等） */
    private String name;

    /** 统计数值（投递数） */
    private Long value;
}
