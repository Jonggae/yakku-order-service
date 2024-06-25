package com.jonggae.yakku.wishlist.dto;

import com.jonggae.yakku.wishlist.entity.Wishlist;
import com.jonggae.yakku.wishlist.entity.WishlistItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDto {

    private Long customerId;
    private List<WishlistItemDto> wishlistItemListDto;
    private Long totalPrice;

    public static WishlistDto from(Wishlist wishlist){
        Long totalPrice = wishlist.getWishlistItemList().stream()
                .mapToLong(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        return WishlistDto.builder()
                .customerId(wishlist.getCustomer().getId())
                .wishlistItemListDto(wishlist.getWishlistItemList().stream()
                        .map(WishlistItemDto::from)
                        .collect(Collectors.toList()))
                .totalPrice(totalPrice)
                .build();
    }
}
