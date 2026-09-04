package com.atmoto.recruit.biz.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    /** 渠道详情/推荐人（内推等配置了详情字段的渠道，学生选填，长度对齐 app_application.source_detail 列宽） */
    @Size(max = 256, message = "渠道详情不能超过256字符")
    private String sourceDetail;

    /** 指定用作简历快照的附件ID（可空，不传则默认取第一条） */
    private Long fileId;
}
