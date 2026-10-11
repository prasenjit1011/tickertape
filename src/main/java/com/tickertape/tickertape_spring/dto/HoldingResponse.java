package com.tickertape.tickertape_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {

    private String symbol;
    private String company;
    private String isin;
    private Double qty;
    private Double avgCost;
    private Double price;
    private Double dayPct;
    private Double costValue;
    private Double marketValue;
    private Double realized;
    private Double unrealized;
    private Double unrealizedPct;
}
