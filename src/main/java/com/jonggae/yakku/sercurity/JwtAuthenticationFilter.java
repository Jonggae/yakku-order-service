package com.jonggae.yakku.sercurity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/test/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        logger.info("Request path: {}", path);

        String token = extractToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            logger.warn("Missing JWT token");
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenUtil.validateToken(token)) {
            logger.warn("Invalid JWT token");
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = jwtTokenUtil.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

