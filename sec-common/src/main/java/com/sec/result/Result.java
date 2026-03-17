package com.sec.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.code = ResultCode.SUCCESS;
        result.message = ResultCode.SUCCESS_MSG;
        result.data = data;
        return result;
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.code = ResultCode.SUCCESS;
        result.message = ResultCode.SUCCESS_MSG;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = ResultCode.ERROR;
        result.message = msg;
        return result;
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = msg;
        return result;
    }
}