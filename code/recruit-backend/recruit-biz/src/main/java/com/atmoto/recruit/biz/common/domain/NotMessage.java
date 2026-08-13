package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息表 S-015
 * <p>存储发送给学生和HR的站内信消息，支持已读/未读标记。</p>
 *
 * @author atmoto-recruit
 */
@Data
@TableName("not_message")
public class NotMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID（DB列名 recipient_id） */
    @TableField("recipient_id")
    private Long recipientId;

    /** 接收人类型：STUDENT / HR（DB列名 recipient_type） */
    @TableField("recipient_type")
    private String recipientType;

    /** 消息类型（DB列名 message_type） */
    @TableField("message_type")
    private String messageType;

    /** 幂等键（DB列名 dedup_key）：APPLICATION_STATUS_CHANGED:{app_status_history.id}，唯一防重 */
    @TableField("dedup_key")
    private String dedupKey;

    /** 消息标题 */
    private String title;

    /** 消息正文 */
    private String content;

    /** 关联业务ID（DB列名 ref_id） */
    @TableField("ref_id")
    private Long refId;

    /** 已读标记：0未读 / 1已读（DB列名 is_read） */
    @TableField("is_read")
    private String isRead;

    /** 阅读时间（DB列名 read_time） */
    @TableField("read_time")
    private LocalDateTime readTime;

    /** 发送时间（DB列名 create_time） */
    @TableField("create_time")
    private LocalDateTime createTime;
}
