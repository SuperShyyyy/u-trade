package com.u.common.exception;

import com.u.common.constant.ErrorCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private Integer code;

    public BaseException() {
        super();
        this.code = ErrorCode.ERROR;
    }

    public BaseException(String msg) {
        super(msg);
        this.code = ErrorCode.ERROR;
    }

    public BaseException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

}
