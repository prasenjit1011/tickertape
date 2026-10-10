package com.tickertape.tickertape_spring.dto;

public record MonthlyHighResponse(String month, double low, String lowDate, double high, String highDate) {
}
