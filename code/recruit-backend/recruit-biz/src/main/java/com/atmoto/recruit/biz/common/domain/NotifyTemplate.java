package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板（A-001-T）
 * <p>存储各类通知的模板内容，支持短信/邮件/系统消息等多渠道</p>
 *
 * @author atmoto-recruit
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_template")
public class NotifyTemplate extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板编码（唯一标识，如 SCREEN_PASS_NOTIFY） */
    private String templateCode;

    /** 模板名称（中文，如"筛选通过通知"） */
    private String templateName;

    /** 通知渠道：SMS/EMAIL/SYSTEM */
    private String channel;

    /** 模板内容（支持变量占位符，如 ${candidateName}、${jobName}） */
    private String content;

    /** 状态：0=启用，1=停用 */
    private String status;

    /** 备注 */
    private String remark;
}
