package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 简历附件（S-007，必填）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_resume_file")
public class ResumeFile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    /** DB列名 file_name */
    @TableField("file_name")
    private String originalName;
    private String filePath;
    /** DB列名 file_type */
    @TableField("file_type")
    private String fileExt;
    private Long fileSize;
    private String previewPath;
    private String previewStatus;

    /** 是否当前版本（DB列名 is_current） */
    @TableField("is_current")
    private String isCurrent;

    /** 预览错误信息（DB列名 preview_error） */
    @TableField("preview_error")
    private String previewError;

    /** 上传时间（DB列名 upload_time） */
    @TableField("upload_time")
    private LocalDateTime uploadTime;
}
