package com.tickertape.tickertape_spring.job;

import com.tickertape.tickertape_spring.dto.DailySyncStartResponse;
import com.tickertape.tickertape_spring.dto.StockPeerResponse;
import com.tickertape.tickertape_spring.model.EquityHolding;
import com.tickertape.tickertape_spring.model.StockPeer;
import com.tickertape.tickertape_spring.repository.EquityHoldingRepository;
import com.tickertape.tickertape_spring.service.HoldingNameMatcher;
import com.tickertape.tickertape_spring.service.StockPeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncJob {

    private static final int[][] RANK_BANDS = {{1, 50}, {51, 100}, {101, 150}, {151, 200}};

    private final StockPeerService stockPeerService;
    private final EquityHoldingRepository equityHoldingRepository;
    private final Semaphore syncPermit = new Semaphore(1);
    private int quotePass;

    @Value("${app.sync.sids:HYU}")
    private String sids;

    @Value("${app.sync.zone:Asia/Kolkata}")
    private String zoneId;

    @Value("${app.sync.priority-rank-max:500}")
    private double priorityRankMax;

    @Value("${app.sync.on-startup:true}")
    private boolean syncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void syncWhenReady() {
        if (!syncOnStartup) {
            return;
        }
        Thread thread = new Thread(this::syncDue, "stock-daily-sync");
        thread.setDaemon(true);
        thread.start();
    }

    @Scheduled(cron = "${app.sync.cron:0 0 16 * * *}", zone = "${app.sync.zone:Asia/Kolkata}")
    public void collectOnSchedule() {
        log.info("Daily stock sync triggered");
        syncDue();
    }

    public void syncDue() {
        syncPermit.acquireUninterruptibly();
        try {
            refreshStocksDueToday();
        } finally {
            syncPermit.release();
        }
    }

    public DailySyncStartResponse startNow() {
        if (!syncPermit.tryAcquire()) {
            return new DailySyncStartResponse(false, 0, "Quick update is already running");
        }
        try {
            int pass = quotePass;
            String label = passLabel(pass);
            List<String> sids = sidsForPass(pass);
            if (sids.isEmpty()) {
                quotePass = (quotePass + 1) % RANK_BANDS.length;
                return new DailySyncStartResponse(true, 0, "Nothing to update (" + label + ")");
            }
            List<StockPeerResponse> updated = stockPeerService.refreshQuotes(sids);
            quotePass = (quotePass + 1) % RANK_BANDS.length;
            return new DailySyncStartResponse(true, updated.size(),
                    "Updated " + updated.size() + " stocks (" + label + ")", updated);
        } finally {
            syncPermit.release();
        }
    }

    public List<StockPeerResponse> collect(String sid) {
        List<String> targets = sid == null || sid.isBlank() ? configuredSids() : List.of(sid.trim().toUpperCase());
        List<StockPeerResponse> collected = new ArrayList<>();
        syncPermit.acquireUninterruptibly();
        try {
            for (String target : targets) {
                log.info("Stock collection started for {}", target);
                collected.addAll(stockPeerService.syncFromSummary(target));
            }
        } finally {
            syncPermit.release();
        }
        return collected;
    }

    public int syncAll() {
        List<String> targets = stockPeerService.findAllSids();
        if (targets.isEmpty()) {
            targets = configuredSids();
        }
        int synced = 0;
        syncPermit.acquireUninterruptibly();
        try {
            for (String sid : targets) {
                try {
                    log.info("Syncing stored stock {}", sid);
                    stockPeerService.syncFromSummary(sid);
                    synced++;
                } catch (RuntimeException ex) {
                    log.error("Sync failed for {}", sid, ex);
                }
            }
        } finally {
            syncPermit.release();
        }
        log.info("Synced {} of {} stocks", synced, targets.size());
        return synced;
    }

    private void refreshStocksDueToday() {
        List<StockPeer> due = stocksDueToday();
        if (due.isEmpty()) {
            log.info("Daily stock sync has nothing left for {}", LocalDate.now(ZoneId.of(zoneId)));
            return;
        }
        refreshStocks(due);
    }

    private List<StockPeer> stocksDueToday() {
        ZoneId zone = ZoneId.of(zoneId);
        LocalDate today = LocalDate.now(zone);
        return stockPeerService.findStored().stream()
                .filter(stock -> stock.getSid() != null && !stock.getSid().isBlank())
                .filter(stock -> !updatedToday(stock, today, zone))
                .sorted(dueOrder())
                .toList();
    }

    private void refreshStocks(List<StockPeer> due) {
        log.info("Daily stock sync updating {} stocks, ranks 1-{} first", due.size(), (int) priorityRankMax);
        int synced = 0;
        for (StockPeer stock : due) {
            try {
                log.info("Daily sync {} rank {}", stock.getSid(), stock.getMrktCapRank());
                stockPeerService.syncOne(stock.getSid());
                synced++;
            } catch (RuntimeException ex) {
                log.error("Daily sync failed for {}", stock.getSid(), ex);
            }
        }
        log.info("Daily stock sync finished {} of {} stocks", synced, due.size());
    }

    private boolean updatedToday(StockPeer stock, LocalDate today, ZoneId zone) {
        return stock.getUpdatedAt() != null && !stock.getUpdatedAt().atZone(zone).toLocalDate().isBefore(today);
    }

    private Comparator<StockPeer> dueOrder() {
        return Comparator
                .comparingInt(this::priorityBand)
                .thenComparing(StockPeer::getMrktCapRank, Comparator.nullsLast(Double::compareTo))
                .thenComparing(StockPeer::getSid, Comparator.nullsLast(String::compareTo));
    }

    private int priorityBand(StockPeer stock) {
        Double rank = stock.getMrktCapRank();
        if (rank != null && rank >= 1 && rank <= priorityRankMax) {
            return 0;
        }
        if (rank != null) {
            return 1;
        }
        return 2;
    }

    private List<String> sidsForPass(int pass) {
        List<StockPeer> stored = stockPeerService.findStored();
        LinkedHashSet<String> sids = new LinkedHashSet<>();
        if (pass == 0) {
            sids.addAll(investedSids(stored));
        }
        int[] band = RANK_BANDS[pass];
        sids.addAll(rankSids(stored, band[0], band[1]));
        return List.copyOf(sids);
    }

    private StockPeer stockForHolding(EquityHolding holding, List<StockPeer> stored) {
        String symbol = holding.getSymbol() == null ? "" : holding.getSymbol().trim();
        for (StockPeer stock : stored) {
            if (stock.getTicker() != null && stock.getTicker().equalsIgnoreCase(symbol)) {
                return stock;
            }
        }
        return HoldingNameMatcher.match(holding.getCompany(), stored);
    }

    private List<String> investedSids(List<StockPeer> stored) {
        LinkedHashSet<String> sids = new LinkedHashSet<>();
        for (EquityHolding holding : equityHoldingRepository.findAll()) {
            if (holding.getCostValue() == null || holding.getCostValue() <= 0 || holding.getCompany() == null) {
                continue;
            }
            StockPeer stock = stockForHolding(holding, stored);
            if (stock != null && stock.getSid() != null && !stock.getSid().isBlank()) {
                sids.add(stock.getSid());
            }
        }
        return List.copyOf(sids);
    }

    private List<String> rankSids(List<StockPeer> stored, int low, int high) {
        return stored.stream()
                .filter(stock -> stock.getSid() != null && !stock.getSid().isBlank())
                .filter(stock -> {
                    Double rank = stock.getMrktCapRank();
                    return rank != null && rank >= low && rank <= high;
                })
                .map(stock -> stock.getSid().trim().toUpperCase())
                .distinct()
                .toList();
    }

    private String passLabel(int pass) {
        int[] band = RANK_BANDS[pass];
        String ranks = "ranks " + band[0] + "–" + band[1];
        return pass == 0 ? "holdings and " + ranks : ranks;
    }

    private List<String> configuredSids() {
        return Arrays.stream(sids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toUpperCase)
                .toList();
    }
}
