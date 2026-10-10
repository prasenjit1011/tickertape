package com.tickertape.tickertape_spring.job;

import com.tickertape.tickertape_spring.dto.StockPeerResponse;
import com.tickertape.tickertape_spring.service.StockPeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncJob {

    private final StockPeerService stockPeerService;

    @Value("${app.sync.sids:HYU}")
    private String sids;

    @Scheduled(cron = "${app.sync.cron:0 0 16 * * MON-FRI}", zone = "${app.sync.zone:Asia/Kolkata}")
    public void collectOnSchedule() {
        for (String sid : configuredSids()) {
            try {
                log.info("Scheduled stock collection started for {}", sid);
                stockPeerService.syncFromSummary(sid);
                log.info("Scheduled stock collection finished for {}", sid);
            } catch (RuntimeException ex) {
                log.error("Scheduled stock collection failed for {}", sid, ex);
            }
        }
    }

    public List<StockPeerResponse> collect(String sid) {
        List<String> targets = sid == null || sid.isBlank() ? configuredSids() : List.of(sid.trim().toUpperCase());
        List<StockPeerResponse> collected = new ArrayList<>();
        for (String target : targets) {
            log.info("Stock collection started for {}", target);
            collected.addAll(stockPeerService.syncFromSummary(target));
        }
        return collected;
    }

    public int syncAll() {
        List<String> targets = stockPeerService.findAllSids();
        if (targets.isEmpty()) {
            targets = configuredSids();
        }
        int synced = 0;
        for (String sid : targets) {
            try {
                log.info("Syncing stored stock {}", sid);
                stockPeerService.syncFromSummary(sid);
                synced++;
            } catch (RuntimeException ex) {
                log.error("Sync failed for {}", sid, ex);
            }
        }
        log.info("Synced {} of {} stocks", synced, targets.size());
        return synced;
    }

    private List<String> configuredSids() {
        return Arrays.stream(sids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toUpperCase)
                .toList();
    }
}
