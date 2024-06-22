package com.jonggae.yakku.sercurity.handler.jwt;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;


import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    /*
    인증되지 않은 접근 401 (JWT 토큰이 유효하지 않을 때)
    * 서명, 잘못된 토큰 등등 */

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ResponseEntity<ApiResponseDto<Object>> errorResponse = ApiResponseUtil.error(
                "잘못된 인증 정보입니다",
                401,
                "UNAUTHORIZED",
                null);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
