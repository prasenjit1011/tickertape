package com.tickertape.tickertape_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StockViewController {

    @GetMapping({"/","/stocks"})
    public String stocksPage() {
        return "forward:/stocks.html";
    }

    @GetMapping("/portfolio")
    public String portfolioPage() {
        return "forward:/portfolio.html";
    }
}
