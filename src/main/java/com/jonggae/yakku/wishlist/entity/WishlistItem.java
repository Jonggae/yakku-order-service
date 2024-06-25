package com.jonggae.yakku.wishlist.entity;

import com.jonggae.yakku.products.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "wishlist_item")
public class WishlistItem {

    @Id
    @Column(name = "wishlist_item_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "wishlist_id")
    private Wishlist wishlist;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Long quantity;

    public Long getTotalPrice() {
        return product.getPrice() * quantity;
    }

}
