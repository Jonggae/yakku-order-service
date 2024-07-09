package com.jonggae.yakku.order.entity;

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

    private Long productId;
    private String productName;
    private Long quantity;
    private Long price;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

//    @ManyToOne
//    private Product product;
//
//    @Column(nullable = false)
//    private int quantity;
//
//    public Long getTotalPrice() {
//        return product.getPrice() * quantity;
//    }

}
