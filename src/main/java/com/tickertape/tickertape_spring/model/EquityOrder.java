package com.tickertape.tickertape_spring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "equity_orders")
public class EquityOrder {

    @Id
    private String id;

    private int line;

    private LocalDate tradeDate;

    @Indexed
    private String symbol;

    private String action;
    private Double qty;
    private Double price;
    private Double tradeValue;

    @Indexed(unique = true)
    private String orderRef;

    private String settlement;
    private String segment;
    private String dpId;
    private String exchange;
    private Double stt;
    private Double turnoverCharges;
    private Double stampDuty;
    private Double brokerage;
    private Double charges;
}
