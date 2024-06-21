package com.jonggae.yakku.sercurity.handler.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.customers.service.TokenService;
import com.jonggae.yakku.sercurity.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenProvider tokenProvider;
    private final TokenService tokenService;

    public LoginSuccessHandler(TokenProvider tokenProvider, TokenService tokenService) {
        this.tokenProvider = tokenProvider;
        this.tokenService = tokenService;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken();

        String customerName = authentication.getName();
        tokenService.saveRefreshToken(refreshToken, customerName);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(tokens));
        response.setStatus(HttpServletResponse.SC_OK);

    }
}
