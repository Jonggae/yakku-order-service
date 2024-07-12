package com.jonggae.yakku.order.dto;

import com.jonggae.yakku.order.entity.OrderItem;
import lombok.*;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    private Long itemId;
    private Long productId;
    private String productName;
    private Long quantity;
    private Long price;

   public static OrderItemDto from(OrderItem orderItem){
       return OrderItemDto.builder()
               .itemId(orderItem.getId())
               .productId(orderItem.getProductId())
               .productName(orderItem.getProductName())
               .quantity(orderItem.getQuantity())
               .price(orderItem.getPrice())
               .build();
    }

}
