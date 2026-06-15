package com.base.basic.service;

import com.base.basic.po.BasePO;

import java.io.Serializable;

/**
 * @author base
 * @since 2026-06-11
 */
public interface BaseService<T extends BasePO<ID>, ID extends Serializable> extends CustomService<T>, Serializable {
}
