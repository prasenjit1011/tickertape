package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.dto.HoldingResponse;
import com.tickertape.tickertape_spring.dto.OrderResponse;
import com.tickertape.tickertape_spring.model.EquityHolding;
import com.tickertape.tickertape_spring.model.EquityOrder;
import com.tickertape.tickertape_spring.repository.EquityHoldingRepository;
import com.tickertape.tickertape_spring.repository.EquityOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioBookService {

    private final EquityHoldingRepository equityHoldingRepository;
    private final EquityOrderRepository equityOrderRepository;

    public void replaceFrom(Resource ordersFile) throws IOException {
        if (ordersFile == null || !ordersFile.exists()) {
            log.warn("Order book CSV is missing; stored holdings and orders were left unchanged");
            return;
        }
        List<EquityOrder> orders;
        try (InputStreamReader ordersReader = new InputStreamReader(ordersFile.getInputStream(), StandardCharsets.UTF_8)) {
            orders = BrokerBookParser.orders(ordersReader);
        }
        List<EquityHolding> holdings = BrokerBookParser.holdingsFromOrders(orders);
        if (holdings.isEmpty() || orders.isEmpty()) {
            log.warn("Order book parse produced no open holdings; stored holdings and orders were left unchanged");
            return;
        }
        equityHoldingRepository.deleteAll();
        equityOrderRepository.deleteAll();
        equityHoldingRepository.saveAll(holdings);
        equityOrderRepository.saveAll(orders);
        log.info("Stored {} holdings and {} orders", holdings.size(), orders.size());
    }

    public List<HoldingResponse> holdings() {
        return equityHoldingRepository.findAllByOrderByLineAsc().stream()
                .map(this::toHolding)
                .toList();
    }

    public List<OrderResponse> orders() {
        return equityOrderRepository.findAllByOrderByTradeDateDescLineAsc().stream()
                .map(this::toOrder)
                .toList();
    }

    private HoldingResponse toHolding(EquityHolding holding) {
        return HoldingResponse.builder()
                .symbol(holding.getSymbol())
                .company(holding.getCompany())
                .isin(holding.getIsin())
                .qty(holding.getQty())
                .avgCost(holding.getAvgCost())
                .price(holding.getPrice())
                .dayPct(holding.getDayPct())
                .costValue(holding.getCostValue())
                .marketValue(holding.getMarketValue())
                .realized(holding.getRealized())
                .unrealized(holding.getUnrealized())
                .unrealizedPct(holding.getUnrealizedPct())
                .build();
    }

    private OrderResponse toOrder(EquityOrder order) {
        return OrderResponse.builder()
                .date(order.getTradeDate())
                .symbol(order.getSymbol())
                .action(order.getAction())
                .qty(order.getQty())
                .price(order.getPrice())
                .value(order.getTradeValue())
                .charges(order.getCharges())
                .exchange(order.getExchange())
                .build();
    }
}
