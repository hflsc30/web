package com.base.exception;

/**
 * 未登录 / 未认证异常，默认 code = 401。
  * @author base
 * @since 2026-06-11
 */
public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super(401, message);
    }
}
