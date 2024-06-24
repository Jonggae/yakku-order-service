package com.jonggae.yakku.products.repository;

import com.jonggae.yakku.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductName(String productName);
    List<Product> findAll();
}
