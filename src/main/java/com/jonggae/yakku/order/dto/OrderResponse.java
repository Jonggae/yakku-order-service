package com.jonggae.yakku.order.dto;

import com.jonggae.yakku.order.entity.OrderResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private OrderDto orderDto;
    private OrderResult result;
}
