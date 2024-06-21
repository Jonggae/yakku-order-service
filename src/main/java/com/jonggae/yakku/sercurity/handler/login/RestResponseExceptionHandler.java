package com.jonggae.yakku.sercurity.handler.login;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.exceptions.DuplicateMemberException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {
    //회원 가입 시 중복 회원 정보가 있을 때
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = {DuplicateMemberException.class})
    @ResponseBody
    protected ResponseEntity<ApiResponseDto<Object>> duplicateMemberException(DuplicateMemberException ex, WebRequest request) {
        String errorMessage = ex.getMessage();
        return ApiResponseUtil.error(errorMessage, 400, "DUPLICATE_MEMBER", null);

    }

    // 권한이 없는 계정으로 접근하였을 때 mvc 레벨
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = {DuplicateMemberException.class, AccessDeniedException.class})
    @ResponseBody
    protected ResponseEntity<ApiResponseDto<Object>> forbidden(RuntimeException ex, WebRequest request) {
        String additionalMsg = "접근 권한이 없습니다. 계정을 확인해주세요.";
        return ApiResponseUtil.error(additionalMsg, 403, "AUTH ERROR", null);
    }
}
