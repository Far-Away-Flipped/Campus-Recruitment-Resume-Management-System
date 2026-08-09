package com.atmoto.recruit.biz.portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 投递请求 DTO（S-013 投递流程）
 * <p>学生端投递岗位的请求体</p>
 *
 * @author atmoto-recruit
 */
@Data
public class SubmitApplicationRequest {

    /** 目标岗位ID（必填） */
    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    /** 渠道来源，默认 OFFICIAL_SITE */
    private String source;

    /** 指定用作简历快照的附件ID（可空，不传则默认取第一条） */
    private Long fileId;
}
