package com.u.common.exception;

import com.u.common.constant.ErrorCode;

public class FileException extends BaseException {
    public FileException() {
        super(ErrorCode.INTERNAL_ERROR, "文件操作异常");
    }

    public FileException(String msg) {
        super(ErrorCode.INTERNAL_ERROR, msg);
    }
}
