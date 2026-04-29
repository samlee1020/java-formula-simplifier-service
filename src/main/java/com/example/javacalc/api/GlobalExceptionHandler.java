package com.example.javacalc.api;

import com.example.javacalc.api.dto.ErrorResponse;
import com.example.javacalc.service.CalculatorException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CalculatorException.class)
    public ResponseEntity<ErrorResponse> handleCalculatorException(CalculatorException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetail()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("INVALID_REQUEST", "请求 JSON 格式错误", rootMessage(ex)));
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        IllegalStateException.class,
        IndexOutOfBoundsException.class,
        NullPointerException.class,
        NumberFormatException.class
    })
    public ResponseEntity<ErrorResponse> handleBadExpression(RuntimeException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("INVALID_EXPRESSION", "表达式格式错误", rootMessage(ex)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "服务内部错误", rootMessage(ex)));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return Objects.toString(current.getMessage(), current.getClass().getSimpleName());
    }
}
