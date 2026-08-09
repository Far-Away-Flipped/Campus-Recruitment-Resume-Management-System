package com.atmoto.recruit.biz.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 投递历史简要VO
 * <p>用于学生详情页中的投递历史子列表展示</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationBriefVO {

    /** 投递记录ID */
    private Long applicationId;

    /** 岗位名称 */
    private String jobTitle;

    /** 投递状态码 */
    private String status;

    /** 投递状态中文标签 */
    private String statusLabel;

    /** 投递时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
}
