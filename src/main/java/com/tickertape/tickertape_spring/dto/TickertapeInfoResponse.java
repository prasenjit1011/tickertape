package com.tickertape.tickertape_spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickertapeInfoResponse {

    private boolean success;
    private InfoData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoData {
        private String sid;
        private Profile info;
        private Ratios ratios;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String name;
        private String ticker;
        private String sector;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ratios {
        @JsonProperty("52wHigh")
        private Double week52High;

        @JsonProperty("52wLow")
        private Double week52Low;

        @JsonProperty("52wpct")
        private Double week52Pct;

        @JsonProperty("4wpct")
        private Double week4Pct;

        private Double divYield;
        private Double marketCap;

        @JsonProperty("3mAvgVol")
        private Double avgVolume3m;

        private Double pe;
        private Double pb;
        private Double beta;
        private Double inddy;
        private Double indpb;
        private Double indpe;
        private Double lastPrice;
        private Double risk;
        private Double mrktCapRank;
        private Double eps;
        private Double bps;
        private Double roe;
        private Double nShareholders;
        private Double ttmPe;
        private String marketCapLabel;
        private Double mrktCapf;

        @JsonProperty("12mVol")
        private Double vol12m;

        private Double apef;
        private Double pbr;
        private Double expenseRatio;
        private Double trackErr;
        private Double indExpenseRatio;
        private Double indTrackErr;
        private Double asstUnderMan;
        private Double etfLiq;
        private String etfLiqLabel;
    }
}
