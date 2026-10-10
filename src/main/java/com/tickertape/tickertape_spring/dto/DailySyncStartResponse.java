package com.tickertape.tickertape_spring.dto;

public record DailySyncStartResponse(boolean started, int due, String message) {
}
