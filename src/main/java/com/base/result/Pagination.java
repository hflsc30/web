package com.base.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author base
 * @since 2026-06-11
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pagination implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long total;
    private long pages;
    private long pageSize;
    private long currentPage;
}
