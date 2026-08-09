package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HR内部备注表（app_hr_note）
 * <p>HR对投递记录的内部批注，仅HR可见，学生端不可见</p>
 *
 * @author atmoto-recruit
 */
@Data
@TableName("app_hr_note")
public class AppHrNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的投递记录ID */
    private Long applicationId;

    /** 备注内容（DB列名 note_content） */
    @TableField("note_content")
    private String content;

    /** 操作人HR的用户ID（DB列名 hr_user_id） */
    @TableField("hr_user_id")
    private Long operatorId;

    /** 操作人用户名（仅VO展示，不持久化到DB） */
    @TableField(exist = false)
    private String operatorName;

    /** 创建时间（仅插入，不可修改） */
    private LocalDateTime createTime;
}
