package com.tickertape.tickertape_spring.dto;

import java.util.List;

public record DailySyncStartResponse(boolean started, int due, String message, List<StockPeerResponse> stocks) {

    public DailySyncStartResponse(boolean started, int due, String message) {
        this(started, due, message, List.of());
    }
}
