package com.jonggae.yakku.wishlist.dto;

import com.jonggae.yakku.wishlist.entity.WishlistItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDto {
    private Long wishlistItemId;
    private Long productId;
    private String productName;
    private Long quantity;
    private Long price;
    private Long totalPrice;

    public static WishlistItemDto from(WishlistItem wishlistItem) {
        return WishlistItemDto.builder()
                .wishlistItemId(wishlistItem.getId())
                .productId(wishlistItem.getProduct().getId())
                .productName(wishlistItem.getProduct().getProductName())
                .quantity(wishlistItem.getQuantity())
                .price(wishlistItem.getProduct().getPrice())
                .totalPrice(wishlistItem.getTotalPrice())
                .build();
    }
}
