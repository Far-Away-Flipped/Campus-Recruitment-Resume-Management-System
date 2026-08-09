package com.atmoto.recruit.biz.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量筛选结果VO
 * <p>每条记录的操作结果，部分失败时给出原因</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchResultVO {

    /** 投递记录ID */
    private Long applicationId;

    /** 是否成功 */
    private boolean success;

    /** 失败原因（成功时为null） */
    private String reason;
}
