package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class Result<T> implements Serializable {
    private Integer code; //编码：1成功，0为失败
    private String message; //错误信息
    private T data; //数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.message = "success";
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<>();
        result.data = object;
        result.code = 1;
        result.message = "success";
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.message = message;
        result.code = 0;
        return result;
    }
}
