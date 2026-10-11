package com.tickertape.tickertape_spring.job;

import com.tickertape.tickertape_spring.service.PortfolioBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioBookImporter implements ApplicationRunner {

    private final PortfolioBookService portfolioBookService;

    @Value("${app.portfolio.import-on-startup:true}")
    private boolean importOnStartup;

    @Value("${app.portfolio.holdings:classpath:static/public/8506181188_PortFolioEqtSummary.csv}")
    private Resource holdingsFile;

    @Value("${app.portfolio.orders:classpath:static/public/orderBook_Equity_1791616753390.csv}")
    private Resource ordersFile;

    @Override
    public void run(ApplicationArguments args) {
        if (!importOnStartup) {
            return;
        }
        try {
            portfolioBookService.replaceFrom(holdingsFile, ordersFile);
        } catch (Exception ex) {
            log.error("Could not store portfolio holdings and orders", ex);
        }
    }
}
