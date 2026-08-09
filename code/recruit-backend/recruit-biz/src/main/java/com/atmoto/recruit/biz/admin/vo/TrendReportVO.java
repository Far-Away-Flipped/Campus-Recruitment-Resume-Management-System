package com.atmoto.recruit.biz.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投递趋势报表VO
 * <p>按日期分组的每日投递数，用于折线图</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendReportVO {

    /** 日期字符串（yyyy-MM-dd） */
    private String date;

    /** 当日投递数 */
    private Long count;
}
