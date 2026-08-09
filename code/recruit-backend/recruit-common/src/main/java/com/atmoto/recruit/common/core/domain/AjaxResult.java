package com.atmoto.recruit.common.core.domain;

import com.atmoto.recruit.common.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体 —— 所有API返回此结构
 * <p>
 * 约定：HTTP状态码始终为200（框架级400/401/403/500除外），业务错误通过code字段区分。
 * code=200表示成功，其他为错误码。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AjaxResult {

    /** 业务状态码：200=成功，其他=错误 */
    private int code;

    /** 提示消息（中文） */
    private String msg;

    /** 响应数据，成功时返回，失败时null */
    private Object data;

    // ────────────────── 静态工厂方法 ──────────────────

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功", null);
    }

    public static AjaxResult success(String msg) {
        return new AjaxResult(200, msg, null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(ErrorCode.PARAM_INVALID.getCode(), msg, null);
    }

    // ────────────────── 分页专用 ──────────────────

    public static AjaxResult page(TableDataInfo tableDataInfo) {
        return new AjaxResult(200, "查询成功", tableDataInfo);
    }
}
