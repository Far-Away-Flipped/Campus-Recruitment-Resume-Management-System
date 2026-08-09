package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 状态流转历史 VO
 * <p>用于简历详情页的状态变更时间线展示</p>
 */
@Data
public class StatusHistoryVO {
    private String fromStatus;
    private String toStatus;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime operateTime;
}
