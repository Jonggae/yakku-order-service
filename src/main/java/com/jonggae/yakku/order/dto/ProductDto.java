package com.jonggae.yakku.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProductDto {

    private Long productId;
    private String productName;
    private Long OrderId;
    private Long price;
    private int stock;
}
