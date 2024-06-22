package com.jonggae.yakku.common.apiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class ApiResponseUtil {
    public static <T> ResponseEntity<ApiResponseDto<T>> success(String message, T data, int statusCode) {
        ApiResponseDto<T> response = ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .statusCode(statusCode)
                .timeStamp(LocalDateTime.now())
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<ApiResponseDto<T>> error(String message, int statusCode, String errorCode, Object errorDetails) {
        ApiResponseDto<T> response = ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .timeStamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.valueOf(statusCode));
    }
}
