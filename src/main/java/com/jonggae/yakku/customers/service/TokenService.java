package com.jonggae.yakku.customers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.customers.dto.CustomerRequestDto;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
 * Redis로 이메일 인증 토큰, 리프레시 토큰을 관리함
 */
@Service
@AllArgsConstructor
public class TokenService {
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private static final Duration EMAIL_TOKEN_TTL = Duration.ofMinutes(10);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public String createEmailToken(String mail, CustomerRequestDto customerDto) {
        String token = UUID.randomUUID().toString();
        try {
            String customerDtoString = objectMapper.writeValueAsString(customerDto);
            redisTemplate.opsForValue().set(token, customerDtoString, EMAIL_TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize CustomerResponseDto", e);
        }
        return token;
    }

    public CustomerRequestDto getUserDetailsByEmailToken(String token) {
        String customerDtoString = redisTemplate.opsForValue().get(token);
        try {
            return objectMapper.readValue(customerDtoString, CustomerRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to deserialize CustomerResponseDto", e);
        }
    }

    public void deleteEmailToken(String token) {
        redisTemplate.delete(token);
    }


    public void saveRefreshToken(String refreshToken, String customerName) {
        redisTemplate.opsForValue().set(refreshToken, customerName, REFRESH_TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);
        String storedCustomerName = redisTemplate.opsForValue().get(refreshToken);
    }
    public String getCustomerNameByRefreshToken(String refreshToken){

        return redisTemplate.opsForValue().get(refreshToken);
    }

    public void deleteRefreshToken(String refreshToken){
        redisTemplate.delete(refreshToken);
    }
}
