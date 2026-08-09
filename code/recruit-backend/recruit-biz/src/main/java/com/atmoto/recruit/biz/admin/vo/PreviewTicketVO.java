package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

/**
 * 预览ticket响应VO
 *
 * @author atmoto-recruit
 */
@Data
public class PreviewTicketVO {

    /** 一次性预览ticket（60秒有效） */
    private String ticket;
}
