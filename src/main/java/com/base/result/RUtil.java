package com.base.result;

import org.springframework.http.HttpStatus;

/**
 * @author base
 * @since 2026-06-11
 */
public class RUtil {

    /**
     * 根据响应码封装结果对象
     *
     * @param msg          消息
     * @param resultStatus 响应码
     * @param obj          {@link Object}
     * @param success      {@link Boolean}
     * @return {@link R}
     */
    public static R returnByCode(String msg, int resultStatus, Object obj, Boolean success) {
        R result = new R();
        result.setCode(resultStatus);
        result.setData(obj);
        result.setMsg(msg);
        result.setSuccess(success);
        return result;
    }

    /**
     * 封装操作成功的结果对象
     *
     * @param msg 成功信息
     * @param obj {@link Object}
     * @return {@link R}
     */
    public static R success(String msg, Object obj) {
        return returnByCode(msg, HttpStatus.OK.value(), obj, true);
    }

    /**
     * 封装操作成功的结果对象
     *
     * @param msg 成功信息
     * @return {@link R}
     */
    public static R success(String msg) {
        return success(msg, null);
    }

    /**
     * 封装操作失败的结果对象
     *
     * @param msg 错误信息
     * @param obj {@link Object}
     * @return {@link R}
     */
    public static R fail(String msg, Object obj) {
        return returnByCode(msg, HttpStatus.INTERNAL_SERVER_ERROR.value(), obj, false);
    }

    /**
     * 封装操作失败的结果对象
     *
     * @param msg 错误信息
     * @return {@link R}
     */
    public static R fail(String msg) {
        return fail(msg, null);
    }
}
