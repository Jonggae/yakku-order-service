package com.jonggae.yakku.order.dto;

import com.jonggae.yakku.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    private Long itemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Long price;

    public static OrderItemDto from(OrderItem orderItem) {
        return OrderItemDto.builder()
                .itemId(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProduct().getProductName())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getTotalPrice())
                .build();
    }
}
