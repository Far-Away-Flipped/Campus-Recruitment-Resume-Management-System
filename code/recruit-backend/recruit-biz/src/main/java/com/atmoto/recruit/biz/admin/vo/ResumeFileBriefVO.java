package com.atmoto.recruit.biz.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历附件VO
 * <p>用于学生详情页中的简历附件列表展示</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeFileBriefVO {

    /** 附件ID */
    private Long id;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型 */
    private String fileExt;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 预览状态 */
    private String previewStatus;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
}
