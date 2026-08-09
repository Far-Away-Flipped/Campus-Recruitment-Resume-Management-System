package com.atmoto.recruit.biz.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 导出简历请求DTO
 *
 * @author atmoto-recruit
 */
@Data
public class ExportDTO {

    /** 要导出的投递记录ID列表 */
    private List<Long> applicationIds;
}
