package com.base.exception;

import com.base.result.R;
import com.base.result.RUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，捕获所有异常，HTTP 状态始终返回 200，错误信息通过 R.code 区分。
  * @author base
 * @since 2026-06-11
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 业务异常 ──

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[{}] {} → {}", e.getCode(), request.getRequestURI(), e.getMessage());
        return RUtil.returnByCode(e.getMessage(), e.getCode(), null, false);
    }

    // ── 400 Bad Request ──

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        String msg = "缺少参数: " + e.getParameterName();
        log.warn("[400] {} → {}", request.getRequestURI(), msg);
        return RUtil.returnByCode(msg, 400, null, false);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[400] {} → {}", request.getRequestURI(), msg);
        return RUtil.returnByCode(msg, 400, null, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("[400] {} → 请求体不可读", request.getRequestURI());
        return RUtil.returnByCode("请求体格式错误", 400, null, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("[400] {} → {}", request.getRequestURI(), e.getMessage());
        return RUtil.returnByCode(e.getMessage(), 400, null, false);
    }

    // ── 405 Method Not Allowed ──

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("[405] {} → {}", request.getRequestURI(), e.getMessage());
        return RUtil.returnByCode("不支持的请求方法: " + e.getMethod(), 405, null, false);
    }

    // ── 415 Unsupported Media Type ──

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("[415] {} → {}", request.getRequestURI(), e.getMessage());
        return RUtil.returnByCode("不支持的媒体类型", 415, null, false);
    }

    // ── 兜底：所有未处理的异常 ──

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleThrowable(Throwable e, HttpServletRequest request) {
        log.error("[500] {} → {}", request.getRequestURI(), e.getMessage(), e);
        return RUtil.returnByCode("服务器内部错误", 500, null, false);
    }
}
