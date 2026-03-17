package com.sec.constant;

public class ErrorCode {

    private ErrorCode() {}

    public static final Integer SUCCESS = 1;
    public static final Integer ERROR = 0;

    public static final Integer BUSINESS_ERROR = 400;
    public static final Integer PERMISSION_DENIED = 403;
    public static final Integer NOT_FOUND = 404;
    public static final Integer VALIDATION_ERROR = 422;
    public static final Integer INTERNAL_ERROR = 500;
}