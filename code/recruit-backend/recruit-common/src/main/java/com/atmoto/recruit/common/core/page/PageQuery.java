package com.atmoto.recruit.common.core.page;

import lombok.Data;

/**
 * 分页请求参数
 */
@Data
public class PageQuery {

    /** 当前页码（从1开始，负数自动修正为1） */
    private Integer pageNum = 1;

    public void setPageNum(Integer pageNum) {
        this.pageNum = (pageNum != null && pageNum >= 1) ? pageNum : 1;
    }

    /** 每页条数（最大100，防止DoS） */
    private Integer pageSize = 10;

    public void setPageSize(Integer pageSize) {
        if (pageSize != null && pageSize > 100) {
            this.pageSize = 100;
        } else {
            this.pageSize = pageSize != null ? pageSize : 10;
        }
    }

    /** 排序列 */
    private String orderByColumn;

    /** 排序方向：asc/desc */
    private String isAsc = "desc";
}
