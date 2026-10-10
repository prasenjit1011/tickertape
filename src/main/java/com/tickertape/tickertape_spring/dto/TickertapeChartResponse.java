package com.tickertape.tickertape_spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickertapeChartResponse {

    private List<Series> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Series {
        private String sid;
        private List<Point> points;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Point {
        private Instant ts;
        private Double lp;
    }
}
