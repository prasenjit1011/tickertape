package com.tickertape.tickertape_spring.controller;

import com.tickertape.tickertape_spring.dto.DailySyncStartResponse;
import com.tickertape.tickertape_spring.dto.StockPeerResponse;
import com.tickertape.tickertape_spring.dto.YearlyRangeResponse;
import com.tickertape.tickertape_spring.job.StockSyncJob;
import com.tickertape.tickertape_spring.service.StockPeerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockPeerController {

    private final StockPeerService stockPeerService;
    private final StockSyncJob stockSyncJob;

    @PostMapping("/sync-due")
    public ResponseEntity<DailySyncStartResponse> startDailySync() {
        return ResponseEntity.accepted().body(stockSyncJob.startNow());
    }

    @PostMapping("/sync-all")
    public ResponseEntity<List<StockPeerResponse>> syncAll() {
        stockSyncJob.syncAll();
        return ResponseEntity.ok(stockPeerService.findAll());
    }

    @PostMapping("/sync")
    public ResponseEntity<List<StockPeerResponse>> syncData(@RequestParam(required = false) String sid) {
        return ResponseEntity.ok(stockSyncJob.collect(sid));
    }

    @PostMapping("/{sid}/sync")
    public ResponseEntity<List<StockPeerResponse>> sync(@PathVariable String sid) {
        return ResponseEntity.ok(stockPeerService.syncFromSummary(sid));
    }

    @PostMapping("/{sid}/refresh")
    public ResponseEntity<StockPeerResponse> refreshOne(@PathVariable String sid) {
        return ResponseEntity.ok(stockPeerService.syncOne(sid));
    }

    @GetMapping("/{sid}/yearly")
    public ResponseEntity<List<YearlyRangeResponse>> yearly(@PathVariable String sid) {
        return ResponseEntity.ok(stockPeerService.yearlyRanges(sid));
    }

    @GetMapping
    public ResponseEntity<List<StockPeerResponse>> findAll() {
        return ResponseEntity.ok(stockPeerService.findAll());
    }
}
