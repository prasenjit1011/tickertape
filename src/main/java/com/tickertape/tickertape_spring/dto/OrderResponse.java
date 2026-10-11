package com.tickertape.tickertape_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private LocalDate date;
    private String symbol;
    private String action;
    private Double qty;
    private Double price;
    private Double value;
    private Double charges;
    private String exchange;
}
