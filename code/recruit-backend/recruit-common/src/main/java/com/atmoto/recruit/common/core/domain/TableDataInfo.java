package com.atmoto.recruit.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页数据封装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDataInfo {

    /** 总记录数 */
    private long total;

    /** 列表数据 */
    private List<?> rows;

    public static TableDataInfo of(long total, List<?> rows) {
        return new TableDataInfo(total, rows);
    }
}
