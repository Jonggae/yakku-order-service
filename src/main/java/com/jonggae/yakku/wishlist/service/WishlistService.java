package com.jonggae.yakku.wishlist.service;

import com.jonggae.yakku.exceptions.NotFoundProductException;
import com.jonggae.yakku.exceptions.NotFoundWishlistException;
import com.jonggae.yakku.exceptions.NotFoundWishlistItemException;
import com.jonggae.yakku.products.entity.Product;
import com.jonggae.yakku.products.repository.ProductRepository;
import com.jonggae.yakku.wishlist.dto.WishlistDto;
import com.jonggae.yakku.wishlist.dto.WishlistItemDto;
import com.jonggae.yakku.wishlist.entity.Wishlist;
import com.jonggae.yakku.wishlist.entity.WishlistItem;
import com.jonggae.yakku.wishlist.repository.WishlistItemRepository;
import com.jonggae.yakku.wishlist.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    //내 위시리스트 조회

    public WishlistDto getWishlistItems(Long customerId) {
        Wishlist wishList = wishlistRepository.findByCustomerId(customerId)
                .orElseThrow(NotFoundWishlistException::new);
        return WishlistDto.from(wishList);
    }

    // 항목 추가

    public WishlistItemDto addWishItem(Long customerId, WishlistItemDto wishlistItemDto){
        Wishlist wishlist = wishlistRepository.findByCustomerId(customerId)
                .orElseGet(() -> wishlistRepository.save(new Wishlist(customerId)));

        Product product = productRepository.findById(wishlistItemDto.getProductId())
                .orElseThrow(NotFoundProductException::new);

        WishlistItem addedItem = WishlistItem.builder()
                .wishlist(wishlist)
                .product(product)
                .quantity(wishlistItemDto.getQuantity())
                .build();
        return WishlistItemDto.from(wishlistItemRepository.save(addedItem));
    }

    //위시리스트 항목 수량 수정
    public WishlistDto updateWishItem(Long customerId, Long itemId, WishlistItemDto wishlistItemDto) {
        WishlistItem wishlistItem = wishlistItemRepository.findById(itemId)
                .orElseThrow(NotFoundWishlistItemException::new);

        wishlistItem.setQuantity(wishlistItemDto.getQuantity());
        wishlistItemRepository.save(wishlistItem);
        return getWishlistItems(customerId);
    }

    //항목 삭제
    public WishlistDto deleteWishItem(Long customerId, Long itemId) {
        WishlistItem wishlistItem = wishlistItemRepository.findById(itemId)
                .orElseThrow(NotFoundWishlistItemException::new);
        wishlistItemRepository.deleteById(wishlistItem.getId());
        return getWishlistItems(customerId);
    }

    //전체 비우기
    @Transactional
    public void clearWishlist(Long customerId){
        Wishlist wishlist = wishlistRepository.findByCustomerId(customerId)
                .orElseThrow(NotFoundWishlistException::new);
        wishlistItemRepository.deleteAllByWishlist(wishlist);
        WishlistDto.from(wishlist);
    }

}
