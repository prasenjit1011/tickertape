package com.tickertape.tickertape_spring.dto;

public record YearlyRangeResponse(int year, double low, String lowWeek, double high, String highWeek) {
}
