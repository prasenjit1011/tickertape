package com.tickertape.tickertape_spring.client;

import com.tickertape.tickertape_spring.dto.TickertapeInfoResponse;
import com.tickertape.tickertape_spring.dto.TickertapeQuoteResponse;
import com.tickertape.tickertape_spring.dto.TickertapeSummaryResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TickertapeClient {

    private final RestClient summaryClient = RestClient.builder()
            .baseUrl("https://analyze.api.tickertape.in")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("User-Agent", "tickertape-spring")
            .build();

    private final RestClient infoClient = RestClient.builder()
            .baseUrl("https://api.tickertape.in")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("User-Agent", "tickertape-spring")
            .build();

    private final RestClient quotesClient = RestClient.builder()
            .baseUrl("https://quotes-api.tickertape.in")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("User-Agent", "tickertape-spring")
            .build();

    public TickertapeSummaryResponse fetchSummary(String sid) {
        return summaryClient.get()
                .uri("/v2/stocks/summary/{sid}", sid)
                .retrieve()
                .body(TickertapeSummaryResponse.class);
    }

    public TickertapeInfoResponse fetchInfo(String sid) {
        return infoClient.get()
                .uri("/stocks/info/{sid}", sid)
                .retrieve()
                .body(TickertapeInfoResponse.class);
    }

    public TickertapeQuoteResponse fetchQuote(String sid) {
        return quotesClient.get()
                .uri("/quotes?sids={sid}", sid)
                .retrieve()
                .body(TickertapeQuoteResponse.class);
    }
}
