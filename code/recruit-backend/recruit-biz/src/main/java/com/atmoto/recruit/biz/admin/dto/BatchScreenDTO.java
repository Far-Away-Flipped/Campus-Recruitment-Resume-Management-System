package com.atmoto.recruit.biz.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量筛选请求DTO
 *
 * @author atmoto-recruit
 */
@Data
public class BatchScreenDTO {

    /** 投递记录ID列表 */
    private List<Long> applicationIds;

    /** 操作类型：pass（通过） / eliminate（淘汰） */
    private String action;
}
