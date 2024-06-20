package com.jonggae.yakku.customers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.customers.dto.CustomerRequestDto;
import com.jonggae.yakku.customers.dto.CustomerResponseDto;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/*
* Redis로 이메일 인증 토큰을 관리함
* todo: 추후 JWT도 관리할 예정*/
@Service
@AllArgsConstructor
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private static final Duration TOKEN_TTL = Duration.ofDays(1);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public String createToken(String mail, CustomerRequestDto customerDto) {
        String token = UUID.randomUUID().toString();
        try {
            String customerDtoString = objectMapper.writeValueAsString(customerDto);
            redisTemplate.opsForValue().set(token, customerDtoString, TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);
            logger.debug("Token created: {} for email: {}", token, mail);

        } catch (JsonProcessingException e) {
            logger.error("Error serializing CustomerResponseDto for token creation: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to serialize CustomerResponseDto", e);
        }
        return token;
    }

    public CustomerRequestDto getUserDetailsByToken(String token) {
        String customerDtoString = redisTemplate.opsForValue().get(token);
        try {
            return objectMapper.readValue(customerDtoString, CustomerRequestDto.class);
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing CustomerResponseDto for token: {}", token, e);
            throw new RuntimeException("Unable to deserialize CustomerResponseDto", e);
        }
    }

    public void deleteToken(String token) {
        redisTemplate.delete(token);
    }
}
