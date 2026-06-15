package com.base.basic.service.impl;

import com.base.basic.dao.BaseDAO;
import com.base.basic.po.BasePO;
import com.base.basic.service.BaseService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author base
 * @since 2026-06-11
 */
@Slf4j
@NoArgsConstructor
public class BaseServiceImpl<M extends BaseDAO<M, T, ID>, T extends BasePO<ID>, ID extends Serializable> extends CustomServiceImpl<M, T> implements BaseService<T, ID> {
}
