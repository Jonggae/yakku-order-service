package com.jonggae.yakku.order.dto;

import com.jonggae.yakku.order.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateDto {
    private OrderStatus status;
}
