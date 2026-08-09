package com.atmoto.recruit.common.exception;

import com.atmoto.recruit.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常 —— 携带错误码与中文提示
 * <p>由 RestControllerAdvice 统一捕获并转换为 AjaxResult</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getMsg() + "：" + detail);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
