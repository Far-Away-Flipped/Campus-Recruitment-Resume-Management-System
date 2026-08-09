package com.atmoto.recruit.framework.web.exception;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public AjaxResult handleBizException(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public AjaxResult handleValidationException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return AjaxResult.error(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public AjaxResult handleMissingParam(MissingServletRequestParameterException e) {
        return AjaxResult.error(ErrorCode.PARAM_INVALID.getCode(), "缺少必要参数：" + e.getParameterName());
    }

    /** 文件上传超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public AjaxResult handleFileSizeExceeded() {
        return AjaxResult.error(ErrorCode.FILE_SIZE_EXCEEDED.getCode(), ErrorCode.FILE_SIZE_EXCEEDED.getMsg());
    }

    /** 请求体JSON解析/日期格式错误 → 友好400而非500 traceId */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AjaxResult handleMessageNotReadable(HttpMessageNotReadableException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("LocalDate")) {
            return AjaxResult.error(ErrorCode.PARAM_INVALID.getCode(), "日期格式不正确，请使用 yyyy-MM-dd 格式");
        }
        // Only show encoding error for actual GBK/encoding issues (Invalid UTF-8 start byte)
        if (msg != null && msg.contains("Invalid UTF-8 start byte")) {
            return AjaxResult.error(ErrorCode.PARAM_INVALID.getCode(), "请求体编码错误，请使用 UTF-8 编码");
        }
        log.warn("请求体解析失败: {}", e.getMessage());
        return AjaxResult.error(ErrorCode.PARAM_INVALID.getCode(), "请求数据格式不正确，请检查JSON格式和字段类型");
    }

    /** 不存在的URL — 返回友好404而非500 traceId */
    @ExceptionHandler(NoResourceFoundException.class)
    public AjaxResult handleNoResourceFound(NoResourceFoundException e) {
        return AjaxResult.error(404, "接口路径不存在: " + e.getResourcePath());
    }

    /** 唯一键冲突（含防重复投递） */
    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult handleDuplicateKey(DuplicateKeyException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("uk_student_job")) {
            return AjaxResult.error(ErrorCode.APPLICATION_DUPLICATE.getCode(), ErrorCode.APPLICATION_DUPLICATE.getMsg());
        }
        if (msg != null && msg.contains("uk_phone")) {
            return AjaxResult.error(ErrorCode.STUDENT_PHONE_EXISTS.getCode(), ErrorCode.STUDENT_PHONE_EXISTS.getMsg());
        }
        log.warn("数据库唯一键冲突: {}", e.getMessage());
        return AjaxResult.error(ErrorCode.REPEAT_SUBMIT.getCode(), "数据已存在，请勿重复操作");
    }

    /** 运行时异常 —— 兜底 */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("系统异常 traceId={} path={}: ", traceId, request.getRequestURI(), e);
        return AjaxResult.error(ErrorCode.INTERNAL_ERROR.getCode(),
                "系统内部错误，请稍后重试");
    }

    /** 所有剩下未处理的异常 */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("未预期异常 traceId={} path={}: ", traceId, request.getRequestURI(), e);
        return AjaxResult.error(ErrorCode.INTERNAL_ERROR.getCode(),
                "系统内部错误，请稍后重试");
    }
}
