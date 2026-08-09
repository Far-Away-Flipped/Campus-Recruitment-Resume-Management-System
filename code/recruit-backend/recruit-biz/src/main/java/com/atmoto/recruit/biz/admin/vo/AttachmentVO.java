package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

/**
 * 附件VO
 *
 * @author atmoto-recruit
 */
@Data
public class AttachmentVO {

    private Long id;
    private String originalName;
    private String fileExt;
    private Long fileSize;
    private String previewStatus;
}
