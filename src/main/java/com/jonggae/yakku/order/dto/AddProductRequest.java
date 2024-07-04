package com.jonggae.yakku.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProductRequest {
    private Long productId;
    private int quantity;
}
