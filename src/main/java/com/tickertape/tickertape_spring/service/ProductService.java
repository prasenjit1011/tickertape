package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.dto.ProductRequest;
import com.tickertape.tickertape_spring.dto.ProductResponse;
import com.tickertape.tickertape_spring.exception.DuplicateResourceException;
import com.tickertape.tickertape_spring.exception.ResourceNotFoundException;
import com.tickertape.tickertape_spring.model.Product;
import com.tickertape.tickertape_spring.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toResponse(productRepository.save(product));
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    public ProductResponse findById(String id) {
        return toResponse(getProduct(id));
    }

    public ProductResponse findBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU '" + sku + "' not found"));
        return toResponse(product);
    }

    public ProductResponse update(String id, ProductRequest request) {
        Product product = getProduct(id);

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
        }

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setUpdatedAt(Instant.now());

        return toResponse(productRepository.save(product));
    }

    public void delete(String id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    private Product getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found"));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
