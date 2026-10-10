package com.tickertape.tickertape_spring.client;

import com.tickertape.tickertape_spring.dto.TickertapeInfoResponse;
import com.tickertape.tickertape_spring.dto.TickertapeQuoteResponse;
import com.tickertape.tickertape_spring.dto.TickertapeSummaryResponse;
import com.tickertape.tickertape_spring.exception.UpstreamDataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TickertapeClient {

    private final Object pace = new Object();
    private long nextCallAt;

    @Value("${app.sync.delay-ms:2000}")
    private long delayMs;

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
        waitForNextCall();
        return summaryClient.get()
                .uri("/v2/stocks/summary/{sid}", sid)
                .retrieve()
                .body(TickertapeSummaryResponse.class);
    }

    public TickertapeInfoResponse fetchInfo(String sid) {
        waitForNextCall();
        return infoClient.get()
                .uri("/stocks/info/{sid}", sid)
                .retrieve()
                .body(TickertapeInfoResponse.class);
    }

    public TickertapeQuoteResponse fetchQuote(String sid) {
        waitForNextCall();
        return quotesClient.get()
                .uri("/quotes?sids={sid}", sid)
                .retrieve()
                .body(TickertapeQuoteResponse.class);
    }

    private void waitForNextCall() {
        if (delayMs <= 0) {
            return;
        }
        synchronized (pace) {
            long wait = nextCallAt - System.currentTimeMillis();
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new UpstreamDataException("Tickertape request was interrupted");
                }
            }
            nextCallAt = System.currentTimeMillis() + delayMs;
        }
    }
}
