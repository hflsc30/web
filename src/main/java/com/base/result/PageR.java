package com.base.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class PageR<T> extends CommR<T> {
    @Serial
    private static final long serialVersionUID = 1L;

    private Pagination pagination;

    public PageR() {
        this.setCode(200);
        this.setMsg("成功");
        this.setSuccess(true);
    }
}
