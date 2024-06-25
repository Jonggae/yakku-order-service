package com.jonggae.yakku.products.service;

import com.jonggae.yakku.exceptions.NotFoundProductException;
import com.jonggae.yakku.products.dto.CustomPageDto;
import com.jonggae.yakku.products.dto.ProductDto;
import com.jonggae.yakku.products.entity.Product;
import com.jonggae.yakku.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/*
* - 등록되어 있는 상품의 리스트를 보여주고 사용자가 구매할 수 있는 인터페이스를 제공합니다.
  - 상품을 클릭시 상품의 상세 정보를 제공해야합니다.
  - 상품의 재고 관리를 위한 유저 인터페이스는 별도로 구현하지 않습니다.*/

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    //상품 등록 todo : 누가 상품을 올리는 건지 모르겠는데, 권한을 지정해야하는지?
    public ProductDto addProduct(ProductDto productDto) {
        Product product = ProductDto.toEntity(productDto);
        return ProductDto.from(productRepository.save(product));
    }

    //전체 조회
    public CustomPageDto<ProductDto> showAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findAll(pageable);
        Page<ProductDto> productDtoPage = productPage.map(ProductDto::from);

        return CustomPageDto.from(productDtoPage);
    }

    //상품 단일 조회 (상세 조회)
    public ProductDto showProductInfo(Long productId) {
        return productRepository.findById(productId)
                .map(ProductDto::from)
                .orElseThrow(NotFoundProductException::new);
    }

    // 상품 정보 수정
    public ProductDto updateProduct(Long productId, ProductDto productDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
        product.updateFromDto(productDto);
        return ProductDto.from(productRepository.save(product));
    }

    // 상품 삭제 -삭제 후 전체 목록 반환
    public CustomPageDto<ProductDto> deleteProduct(Long productId, int page, int size){
        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
        productRepository.delete(product);
        return showAllProducts(page, size);
    }
}
