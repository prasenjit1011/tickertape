package com.tickertape.tickertape_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductViewController {

    @GetMapping({"/", "/products"})
    public String productsPage() {
        return "forward:/index.html";
    }
}
