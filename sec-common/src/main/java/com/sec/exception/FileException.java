package com.sec.exception;

import com.sec.constant.ErrorCode;

public class FileException extends BaseException {
    public FileException() {
        super(ErrorCode.INTERNAL_ERROR, "文件操作异常");
    }

    public FileException(String msg) {
        super(ErrorCode.INTERNAL_ERROR, msg);
    }
}