package com.base.exception;

/**
 * 无权限 / 未授权异常，默认 code = 403。
  * @author base
 * @since 2026-06-11
 */
public class AuthorizationException extends BusinessException {

    public AuthorizationException(String message) {
        super(403, message);
    }
}
