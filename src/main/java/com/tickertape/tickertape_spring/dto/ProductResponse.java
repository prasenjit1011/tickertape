package com.tickertape.tickertape_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
