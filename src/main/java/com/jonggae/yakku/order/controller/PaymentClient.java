package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.order.dto.PaymentRequestDto;
import com.jonggae.yakku.order.dto.PaymentResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {
    @PostMapping("/api/payments/process")
    PaymentResult processPayment(@RequestBody PaymentRequestDto requestDto);
}

