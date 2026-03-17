package com.sec.exception;

import com.sec.constant.ErrorCode;

public class PermissionDeniedException extends BaseException {
    public PermissionDeniedException() {
        super(ErrorCode.PERMISSION_DENIED, "权限不足");
    }

    public PermissionDeniedException(String msg) {
        super(ErrorCode.PERMISSION_DENIED, msg);
    }
}
