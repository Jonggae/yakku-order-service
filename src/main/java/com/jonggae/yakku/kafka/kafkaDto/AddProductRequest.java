package com.jonggae.yakku.kafka.kafkaDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProductRequest {
    private Long productId;
    private int quantity;
}
