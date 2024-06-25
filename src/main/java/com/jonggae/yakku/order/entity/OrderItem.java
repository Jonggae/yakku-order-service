package com.jonggae.yakku.order.entity;

import com.jonggae.yakku.products.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    private Product product;

    @Column(nullable = false)
    private int quantity;

    public Long getTotalPrice() {
        return product.getPrice() * quantity;
    }

}
