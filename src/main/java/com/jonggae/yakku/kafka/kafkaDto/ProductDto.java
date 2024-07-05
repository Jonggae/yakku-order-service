package com.jonggae.yakku.kafka.kafkaDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProductDto {
    private Long id;
    private String productDescription;
    private Long productId;
    private String productName;
    private Long orderId;
    private Long price;
    private int stock;
}
