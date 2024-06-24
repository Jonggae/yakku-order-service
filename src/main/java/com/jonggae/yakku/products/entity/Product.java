package com.jonggae.yakku.products.entity;

import com.jonggae.yakku.products.dto.ProductDto;
import jakarta.persistence.*;
import lombok.*;

/*
* Product_name
* Product_description
* stock_quantity
* price
* create_at
* updated_at*/
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "procudt_name", nullable = false)
    private String productName;
    
    @Column(name = "product_description")
    private String productDescription;
    
    @Column(name= "product_price", nullable = false)
    private long price; //정수 가격만 사용
    
    @Column(name = "stock", nullable = false)
    private Long stock;
    
    // 이후 메서드 추가

    public void updateFromDto(ProductDto productDto) {
        this.productName = productDto.getProductName();
        this.productDescription = productDto.getProductDescription();
        this.price = productDto.getPrice();
        this.stock = productDto.getStock();
    }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }

    public boolean checkStock(int quantity) {
        return this.stock >= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}
