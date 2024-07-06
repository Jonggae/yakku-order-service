package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{productId}/order-info")
    ProductDto getProductOrderInfo(@PathVariable Long productId);
}
