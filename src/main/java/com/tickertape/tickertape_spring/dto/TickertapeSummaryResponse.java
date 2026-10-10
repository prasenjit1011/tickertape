package com.tickertape.tickertape_spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickertapeSummaryResponse {

    private boolean success;
    private SummaryData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SummaryData {
        private List<AboutAndPeer> aboutAndPeers;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AboutAndPeer {
        private String name;
        private String ticker;
        private String sid;
        private String sector;
        private Ratios ratios;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ratios {
        private Double ttmPe;
        private Double breco;

        @JsonProperty("52wpct")
        private Double week52Pct;

        private Double marketCap;
        private Double apef;
        private Double divDps;
        private Double pbr;
    }
}
