package com.jonggae.yakku.kafka.kafkaDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ProductInfoRequest {
    private Long productId;
    private Long customerId;
    private Long OrderId;
}
