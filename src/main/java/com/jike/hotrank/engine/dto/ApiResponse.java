package com.jike.hotrank.engine.dto;

import lombok.Data;

/**
 * 统一API响应体
 *
 * @param <T> 数据类型
 */
@Data
public class ApiResponse<T> {

    /** 响应码：0-成功，其他-失败 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(0);
        response.setMessage("success");
        return response;
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(0);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 失败响应
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
