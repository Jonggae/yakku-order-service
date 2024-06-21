package com.jonggae.yakku.sercurity.controller;

import com.jonggae.yakku.customers.service.TokenService;
import com.jonggae.yakku.sercurity.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// todo: response 응답 형태 정리하기
@RestController
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenController {

    private final TokenProvider tokenProvider;
    private final TokenService tokenService;

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || !tokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.badRequest().body("invalid refresh token");
        }
        String customerName = tokenProvider.getCustomerNameFromToken(refreshToken);
        String storedCustomerName = tokenService.getCustomerNameByRefreshToken(refreshToken);

        if (storedCustomerName == null || !storedCustomerName.equals(customerName)) {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }

        Authentication authentication = tokenProvider.getAuthenticationFromRefreshToken(refreshToken);
        String newAccessToken = tokenProvider.createAccessToken(authentication);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}
