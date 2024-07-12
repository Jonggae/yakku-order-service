package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.order.dto.ProductDto;
import com.jonggae.yakku.order.feignDto.StockReservationRequestDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{productId}/order-info")
    @Cacheable(value = "productInfo", key = "#productId")
    ProductDto getProductOrderInfo(@PathVariable Long productId);

    @PostMapping("/api/products/{productId}/reserve-stock")
    Boolean reserveStock(@PathVariable Long productId, @RequestBody StockReservationRequestDto request);

    @GetMapping("/api/products/{productId}/stock")
    Long checkStock(@PathVariable Long productId);

    @PostMapping("/api/products/{productId}/cancel-reservation")
    void releaseStock(@PathVariable Long productId, @RequestBody StockReservationRequestDto request);

    @PostMapping("/api/products/{productId}/confirm")
    void confirmStockReservation(@PathVariable("productId") Long productId, @RequestParam("quantity") Long quantity);

    @PostMapping("/api/products/{productId}/cancel")
    void cancelStockReservation(@PathVariable("productId") Long productId, @RequestParam("quantity") Long quantity);
}
