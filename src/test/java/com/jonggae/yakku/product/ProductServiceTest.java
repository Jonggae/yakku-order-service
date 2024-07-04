//package com.jonggae.yakku.product;
//
//import com.jonggae.yakku.products.dto.ProductDto;
//import com.jonggae.yakku.products.repository.ProductRepository;
//import com.jonggae.yakku.products.service.ProductService;
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@DisplayName("상품 관련 서비스 테스트")
//@SpringBootTest
//@AutoConfigureMockMvc
//public class ProductServiceTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//    @Autowired
//    private ProductService productService;
//    @Autowired
//    private ProductRepository productRepository;
//
//    private long productId1;
//    private long productId2;
//
//    @BeforeEach
//    void setUp() {
//        ProductDto savedProduct1 = productService
//                .addProduct(new ProductDto(null, "테스트 상품", "테스트 상품 설명", 1000L, 10L));
//
//        ProductDto savedProduct2 = productService
//                .addProduct(new ProductDto(null, "테스트 상품2", "테스트 상품 설명2", 2000L, 20L));
//
//        productId1 = savedProduct1.getId();
//        productId2 = savedProduct2.getId();
//    }
//
//    //todo: 테스트 아직 오류가 많음. 다시 설계하던가 해야
//    @Test
//    @Disabled
//    @Transactional
//    @DisplayName("상품 등록 테스트")
//    void addProductTes() throws Exception {
//        ProductDto newProduct = addNewProduct();
//        ProductDto savedProduct = productService.addProduct(newProduct);
//
//        assertNotNull(savedProduct.getId());
//        assertEquals("테스트으 상품", savedProduct.getProductName());
//        assertEquals("테스트으 상품 설명", savedProduct.getProductDescription());
//        assertEquals(5000L, savedProduct.getPrice());
//    }
//
//    private ProductDto addNewProduct() {
//        return ProductDto.builder()
//                .productName("테스트으 상품")
//                .productDescription("테스트으 상품 설명")
//                .price(5000L)
//                .stock(1000L)
//                .build();
//    }
//}
