package com.tickertape.tickertape_spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickertapeQuoteResponse {

    private List<Quote> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Quote {
        private String sid;
        private String type;
        private String exchange;
        private Double price;
        private Double o;
        private Double h;
        private Double l;
        private Double c;
        private Double change;
        private Long vol;
        private Long ts;
        private Double wkRange;
        private Double mnRange;
        private Double dyChange;
        private Double wkChange;
        private Double mnChange;
        private Boolean crossedHigh;
        private Boolean crossedLow;
        private Double away52wH;
        private Double away52wL;
        private Double turnover;
        private Double volBreakout;
        private Double low52w;
        private Double high52w;
    }
}
