package com.base.util;

/**
 * @author base
 * @since 2026-05-15
 */
public class ClassUtil {

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(T obj) {
        return (Class<T>) obj.getClass();
    }
}
