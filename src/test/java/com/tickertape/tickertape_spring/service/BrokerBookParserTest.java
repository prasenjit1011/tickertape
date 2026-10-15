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
    void parsesOrderBookIntoOpenHoldings() throws Exception {
        List<EquityOrder> orders = BrokerBookParser.orders(Files.newBufferedReader(Path.of(
                "src/main/resources/static/public/orderBook_Equity_1792023937412.csv")));
        List<EquityHolding> holdings = BrokerBookParser.holdingsFromOrders(orders);

        assertEquals(784, orders.size());
        EquityOrder first = orders.get(0);
        assertEquals(LocalDate.of(2026, 10, 14), first.getTradeDate());
        assertEquals("JKTYRE", first.getSymbol());
        assertEquals("Buy", first.getAction());
        assertEquals(2, first.getQty());
        assertEquals(334.9, first.getPrice());
        assertEquals(669.8, first.getTradeValue());

        assertEquals(127, holdings.size());
        EquityHolding dabur = holdings.stream().filter(holding -> "DABUR".equals(holding.getSymbol())).findFirst().orElseThrow();
        assertEquals(67, dabur.getQty());
        assertEquals(33031.47, dabur.getCostValue(), 0.05);
        assertEquals(372382.33, holdings.stream().mapToDouble(EquityHolding::getCostValue).sum(), 0.05);
    }
}
