package com.tickertape.tickertape_spring.repository;

import com.tickertape.tickertape_spring.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}
