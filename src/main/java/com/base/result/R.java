package com.base.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author base
 * @since 2026-06-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class R extends CommR<Object> {
    @Serial
    private static final long serialVersionUID = 1L;
}
