package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.model.EquityHolding;
import com.tickertape.tickertape_spring.model.EquityOrder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrokerBookParserTest {

    @Test
    void parsesPortfolioAndOrderBook() throws Exception {
        List<EquityHolding> holdings = BrokerBookParser.holdings(Files.newBufferedReader(Path.of(
                "src/main/resources/static/public/8506181188_PortFolioEqtSummary.csv")));
        List<EquityOrder> orders = BrokerBookParser.orders(Files.newBufferedReader(Path.of(
                "src/main/resources/static/public/orderBook_Equity_1791616753390.csv")));

        assertEquals(127, holdings.size());
        EquityHolding aarti = holdings.get(0);
        assertEquals("AARIND", aarti.getSymbol());
        assertEquals("AARTI INDUSTRIES LTD", aarti.getCompany());
        assertEquals(3, aarti.getQty());
        assertEquals(357.62, aarti.getAvgCost());
        assertEquals(-3.74, aarti.getDayPct());
        assertEquals(-311.83, aarti.getRealized());
        assertEquals(28.67, aarti.getUnrealizedPct());

        EquityHolding gas = holdings.get(2);
        assertEquals("ADAGAS", gas.getSymbol());
        assertEquals(0.22, gas.getDayPct());
        assertEquals(-852.50, gas.getUnrealized());
        assertEquals(-13.14, gas.getUnrealizedPct());

        assertEquals(770, orders.size());
        EquityOrder first = orders.get(0);
        assertEquals(LocalDate.of(2026, 10, 8), first.getTradeDate());
        assertEquals("EMAMILTD", first.getSymbol());
        assertEquals("Buy", first.getAction());
        assertEquals(5, first.getQty());
        assertEquals(367.05, first.getPrice());
        assertEquals(1835.25, first.getTradeValue());
        assertEquals(6.84, first.getCharges());
        assertEquals("NSE", first.getExchange());
    }
}
