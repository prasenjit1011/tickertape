package com.tickertape.tickertape_spring.controller;

import com.tickertape.tickertape_spring.dto.HoldingResponse;
import com.tickertape.tickertape_spring.dto.OrderResponse;
import com.tickertape.tickertape_spring.service.PortfolioBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioBookController {

    private final PortfolioBookService portfolioBookService;

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingResponse>> holdings() {
        return ResponseEntity.ok(portfolioBookService.holdings());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> orders() {
        return ResponseEntity.ok(portfolioBookService.orders());
    }
}
