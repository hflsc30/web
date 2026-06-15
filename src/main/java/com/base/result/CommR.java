package com.base.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author base
 * @since 2026-06-11
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CommR<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private Boolean success;
    private T data;

    public boolean isSuccess() {
        return this.code == HttpStatus.OK.value();
    }
}
