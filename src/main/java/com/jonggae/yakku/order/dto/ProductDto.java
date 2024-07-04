package com.jonggae.yakku.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//@JsonIgnoreProperties(ignoreUnknown = true)

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
