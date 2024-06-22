package com.jonggae.yakku.sercurity.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
// todo: response 응답 형태 정리하기
@RestController
@AllArgsConstructor
public class AuthController {
    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/api/customer/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        String customerName = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            customerName = ((UserDetails) authentication.getPrincipal()).getUsername();
        }

        String refreshToken = getCookieValue(request);
        String accessToken = getHeaderToken(request);
        if (refreshToken != null && Boolean.TRUE.equals(redisTemplate.hasKey(refreshToken))) {
            redisTemplate.delete(refreshToken);
        }

        if (accessToken != null) {
            long accessTokenExpirationMillis = 600000;
            redisTemplate.opsForValue().set(accessToken, "blacklisted", accessTokenExpirationMillis, TimeUnit.MILLISECONDS);
        }

        return ResponseEntity.ok((customerName != null ? customerName : "사용자") + "로그아웃 되었습니다.");
    }

    private String getCookieValue(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    private String getHeaderToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
