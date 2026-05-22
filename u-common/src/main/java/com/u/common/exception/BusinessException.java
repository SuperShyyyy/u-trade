package com.u.common.exception;

import com.u.common.constant.ErrorCode;

public class BusinessException extends BaseException {
    public BusinessException() {
        super(ErrorCode.BUSINESS_ERROR, "业务异常");
    }

    public BusinessException(String msg) {
        super(ErrorCode.BUSINESS_ERROR, msg);
    }

    public BusinessException(Integer code, String msg) {
        super(code, msg);
    }
}
