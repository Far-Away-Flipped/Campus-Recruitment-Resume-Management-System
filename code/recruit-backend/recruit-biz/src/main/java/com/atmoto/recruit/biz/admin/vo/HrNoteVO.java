package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * HR备注VO
 *
 * @author atmoto-recruit
 */
@Data
public class HrNoteVO {

    private Long id;
    private Long applicationId;
    private String content;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
