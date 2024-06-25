package com.jonggae.yakku.wishlist.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.common.messageUtil.MessageUtil;
import com.jonggae.yakku.sercurity.utils.SecurityUtil;
import com.jonggae.yakku.wishlist.dto.WishlistDto;
import com.jonggae.yakku.wishlist.dto.WishlistItemDto;
import com.jonggae.yakku.wishlist.messages.WishlistApiMessages;
import com.jonggae.yakku.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final SecurityUtil securityUtil;

    @GetMapping("/my-wishlist")
    public ResponseEntity<ApiResponseDto<WishlistDto>> getWishlistItems() {
        Long customerId = securityUtil.getCurrentCustomerId();
        WishlistDto wishlistDto = wishlistService.getWishlistItems(customerId);
        String message = MessageUtil.getMessage(WishlistApiMessages.WISHLIST_SHOW_SUCCESS);
        return ApiResponseUtil.success(message, wishlistDto, 200);
    }

    @PostMapping("/my-wishlist/items")
    public ResponseEntity<ApiResponseDto<WishlistItemDto>> addWishItem(@RequestBody WishlistItemDto wishlistItemDto) {
        Long customerId = securityUtil.getCurrentCustomerId();
        WishlistItemDto updatedItemDto = wishlistService.addWishItem(customerId, wishlistItemDto);
        String message = MessageUtil.getMessage(WishlistApiMessages.WISHLIST_ITEM_ADD_SUCCESS);
        return ApiResponseUtil.success(message, updatedItemDto, 200);
    }

    @PatchMapping("/my-wishlist/items/{itemId}")
    public ResponseEntity<ApiResponseDto<WishlistDto>> updateWishlistItem(@PathVariable(name = "itemId") Long itemId,
                                                                          @RequestBody WishlistItemDto wishlistItemDto) {
        Long customerId = securityUtil.getCurrentCustomerId();
        WishlistDto updatedWishlist = wishlistService.updateWishItem(customerId, itemId, wishlistItemDto);
        String message = MessageUtil.getMessage(WishlistApiMessages.WISHLIST_ITEM_UPDATE_SUCCESS);
        return ApiResponseUtil.success(message, updatedWishlist, 200);
    }

    @DeleteMapping("/my-wishlist/items/{itemId}")
    public ResponseEntity<ApiResponseDto<WishlistDto>> deleteWishlistItem(@PathVariable(name = "itemId") Long itemId) {
        Long customerId = securityUtil.getCurrentCustomerId();
        WishlistDto updatedWishlist = wishlistService.deleteWishItem(customerId, itemId);
        String message = MessageUtil.getMessage(WishlistApiMessages.WISHLIST_ITEM_DELETE_SUCCESS);
        return ApiResponseUtil.success(message, updatedWishlist, 200);
    }

    @DeleteMapping("/my-wishlist")
    public ResponseEntity<ApiResponseDto<String>> clearCart() {
        Long customerId = securityUtil.getCurrentCustomerId();
        wishlistService.clearWishlist(customerId);
        String message = MessageUtil.getMessage(WishlistApiMessages.WISHLIST_CLEAR_SUCCESS);
        return ApiResponseUtil.success(message, null, 200);
    }

}
