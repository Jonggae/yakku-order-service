package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.order.dto.ProductDto;
import com.jonggae.yakku.order.feignDto.StockReservationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{productId}/order-info")
    ProductDto getProductOrderInfo(@PathVariable Long productId);

    @PostMapping("/api/products/{productId}/reserve-stock")
    Boolean reserveStock(@PathVariable Long productId, @RequestBody StockReservationRequestDto request);

    @GetMapping("/api/products/{productId}/stock")
    Long checkStock(@PathVariable Long productId);

    @PostMapping("/api/products/{productId}/cancel-reservation")
    void releaseStock(@PathVariable Long productId, @RequestBody StockReservationRequestDto request);
}
