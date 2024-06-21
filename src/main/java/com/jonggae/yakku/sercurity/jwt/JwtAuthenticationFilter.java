package com.jonggae.yakku.sercurity.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.customers.dto.LoginRequestDto;
import com.jonggae.yakku.sercurity.handler.login.LoginFailureHandler;
import com.jonggae.yakku.sercurity.handler.login.LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final LoginProvider loginProvider;

    public JwtAuthenticationFilter(LoginProvider loginProvider, LoginSuccessHandler successHandler, LoginFailureHandler failureHandler) {
        super();
        this.loginProvider = loginProvider;
        // 성공 핸들러와 실패 핸들러 설정
        this.setAuthenticationSuccessHandler(successHandler);
        this.setAuthenticationFailureHandler(failureHandler);

        // 로그인 URL 설정
        setFilterProcessesUrl("/api/customer/login");
    }

    @Override //로그인 시도
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequestDto loginRequestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    loginRequestDto.getCustomerName(),
                    loginRequestDto.getPassword());
            return loginProvider.authenticate(authentication);


        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 실패", e);
        }
    }

}
