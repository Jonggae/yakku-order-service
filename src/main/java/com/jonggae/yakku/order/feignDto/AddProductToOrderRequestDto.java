package com.jonggae.yakku.order.feignDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddProductToOrderRequestDto {
    private Long productId;
    private Long quantity;
}
