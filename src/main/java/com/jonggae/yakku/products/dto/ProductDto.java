package com.jonggae.yakku.products.dto;

import com.jonggae.yakku.products.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long id;
    private String productName;
    private String productDescription;
    private Long price;
    private Long stock;

    public static ProductDto from(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .productDescription(product.getProductDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
    public static Product toEntity(ProductDto productDto) {
        return Product.builder()
                .productName(productDto.getProductName())
                .productDescription(productDto.getProductDescription())
                .price(productDto.getPrice())
                .stock(productDto.getStock())
                .build();
    }
}
