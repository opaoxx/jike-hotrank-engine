package com.jike.hotrank.engine.exception;

import com.jike.hotrank.engine.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBusinessExceptionCodeAndMessage() {
        ApiResponse<Void> response = handler.handleBusinessException(new BusinessException(404, "话题不存在"));

        assertEquals(404, response.getCode());
        assertEquals("话题不存在", response.getMessage());
    }

    @Test
    void shouldReturnBadRequestForMissingParameter() {
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException("userId", "Long");

        ApiResponse<Void> response = handler.handleMissingRequestParameter(exception);

        assertEquals(400, response.getCode());
        assertEquals("缺少必要参数：userId", response.getMessage());
    }

    @Test
    void shouldReturnBadRequestForTypeMismatch() {
        MethodArgumentTypeMismatchException exception =
            new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

        ApiResponse<Void> response = handler.handleTypeMismatch(exception);

        assertEquals(400, response.getCode());
        assertEquals("参数格式错误：id 必须为 Long", response.getMessage());
    }
}
