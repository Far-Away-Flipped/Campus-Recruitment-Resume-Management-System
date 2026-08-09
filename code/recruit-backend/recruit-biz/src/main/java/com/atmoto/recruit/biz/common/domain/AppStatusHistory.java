package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 状态流转历史（不可变日志，仅追加）
 * <p>不继承 BaseEntity，不含 delFlag，审计表不可删除</p>
 */
@Data
@TableName("app_status_history")
public class AppStatusHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;
    private String fromStatus;
    private String toStatus;
    private String operatorType;
    private Long operatorId;
    private String remark;

    /** DB列名 create_time */
    @TableField("create_time")
    private LocalDateTime operateTime;
}
