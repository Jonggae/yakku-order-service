package com.jonggae.yakku.order.dto;

import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long orderId;
    private Long customerId;
    private LocalDateTime orderDateTime;
    private List<OrderItemDto> orderItemList;
    private OrderStatus status;

    public static OrderDto from(Order order){
        return OrderDto.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .orderDateTime(order.getOrderDate())
                .status(order.getOrderStatus())
                .orderItemList(order.getOrderItemList().stream()
                        .map(OrderItemDto::from)
                        .collect(Collectors.toList()))
                .build();
    }


}
