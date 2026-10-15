package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.model.StockPeer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HoldingNameMatcherTest {

    @Test
    void matchesCompanyNameToStoredStock() {
        StockPeer reliance = StockPeer.builder().sid("RELI").name("Reliance Industries Ltd").build();
        StockPeer tcs = StockPeer.builder().sid("TCS").name("Tata Consultancy Services Ltd").build();

        StockPeer matched = HoldingNameMatcher.match("RELIANCE INDUSTRIES LIMITED", List.of(reliance, tcs));

        assertEquals("RELI", matched.getSid());
    }

    @Test
    void skipsANameThatDoesNotMatch() {
        StockPeer tcs = StockPeer.builder().sid("TCS").name("Tata Consultancy Services Ltd").build();

        assertNull(HoldingNameMatcher.match("AARTI INDUSTRIES LTD", List.of(tcs)));
    }
}
