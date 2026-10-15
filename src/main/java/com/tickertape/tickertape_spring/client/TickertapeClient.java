package com.tickertape.tickertape_spring.client;

import com.tickertape.tickertape_spring.dto.TickertapeChartResponse;
import com.tickertape.tickertape_spring.dto.TickertapeInfoResponse;
import com.tickertape.tickertape_spring.dto.TickertapeQuoteResponse;
import com.tickertape.tickertape_spring.dto.TickertapeSummaryResponse;
import com.tickertape.tickertape_spring.exception.UpstreamDataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

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

    public TickertapeChartResponse fetchChart(String sid) {
        return fetchChart(sid, "max");
    }

    public TickertapeChartResponse fetchChart(String sid, String duration) {
        waitForNextCall();
        return infoClient.get()
                .uri("/stocks/charts/inter/{sid}?duration={duration}", sid, duration)
                .retrieve()
                .body(TickertapeChartResponse.class);
    }

    public TickertapeQuoteResponse fetchQuote(String sid) {
        return fetchQuotes(List.of(sid));
    }

    public TickertapeQuoteResponse fetchQuotes(List<String> sids) {
        if (sids == null || sids.isEmpty()) {
            return new TickertapeQuoteResponse();
        }
        waitForNextCall();
        String joined = sids.stream()
                .filter(sid -> sid != null && sid.matches("[A-Za-z0-9]+"))
                .map(sid -> sid.toUpperCase(Locale.ROOT))
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        if (joined.isEmpty()) {
            return new TickertapeQuoteResponse();
        }
        return quotesClient.get()
                .uri(java.net.URI.create("https://quotes-api.tickertape.in/quotes?sids=" + joined))
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
