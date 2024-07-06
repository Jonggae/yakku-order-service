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
    private Long id;
    private String productDescription;
    private String productName;
    private Long price;
    private Long stock;
}
