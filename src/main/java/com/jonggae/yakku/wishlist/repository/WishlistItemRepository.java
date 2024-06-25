package com.jonggae.yakku.wishlist.repository;

import com.jonggae.yakku.wishlist.entity.Wishlist;
import com.jonggae.yakku.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByWishlistId(Long wishlistId);

    void deleteAllByWishlist(Wishlist wishlist);
}
