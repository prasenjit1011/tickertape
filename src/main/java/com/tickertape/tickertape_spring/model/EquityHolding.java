package com.tickertape.tickertape_spring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "equity_holdings")
public class EquityHolding {

    @Id
    private String id;

    private int line;

    @Indexed(unique = true)
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
