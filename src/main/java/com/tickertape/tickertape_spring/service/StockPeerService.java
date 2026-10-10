package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.client.TickertapeClient;
import com.tickertape.tickertape_spring.dto.MonthlyHighResponse;
import com.tickertape.tickertape_spring.dto.StockPeerResponse;
import com.tickertape.tickertape_spring.dto.TickertapeChartResponse;
import com.tickertape.tickertape_spring.dto.TickertapeInfoResponse;
import com.tickertape.tickertape_spring.dto.YearlyRangeResponse;
import com.tickertape.tickertape_spring.dto.TickertapeQuoteResponse;
import com.tickertape.tickertape_spring.dto.TickertapeSummaryResponse;
import com.tickertape.tickertape_spring.exception.UpstreamDataException;
import com.tickertape.tickertape_spring.model.StockPeer;
import com.tickertape.tickertape_spring.repository.StockPeerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockPeerService {

    private final TickertapeClient tickertapeClient;
    private final StockPeerRepository stockPeerRepository;

    public List<StockPeerResponse> syncFromSummary(String sid) {
        TickertapeSummaryResponse summary = tickertapeClient.fetchSummary(sid);
        if (summary == null || summary.getData() == null || summary.getData().getAboutAndPeers() == null) {
            throw new UpstreamDataException("Tickertape summary for '" + sid + "' did not include aboutAndPeers");
        }

        Instant now = Instant.now();
        List<StockPeer> saved = new ArrayList<>();
        for (TickertapeSummaryResponse.AboutAndPeer peer : summary.getData().getAboutAndPeers()) {
            saved.add(stockPeerRepository.save(upsert(peer, now)));
        }

        TickertapeInfoResponse info = tickertapeClient.fetchInfo(sid);
        if (info == null || info.getData() == null || info.getData().getRatios() == null) {
            throw new UpstreamDataException("Tickertape info for '" + sid + "' did not include ratios");
        }

        String infoSid = info.getData().getSid() != null ? info.getData().getSid() : sid;
        StockPeer target = stockPeerRepository.findBySid(infoSid).orElseGet(StockPeer::new);
        if (target.getSid() == null) {
            target.setSid(infoSid);
        }
        applyInfoRatios(target, info.getData().getRatios(), now);
        applyQuote(target);
        StockPeer updated = stockPeerRepository.save(target);

        boolean replaced = false;
        List<StockPeerResponse> responses = new ArrayList<>();
        for (StockPeer peer : saved) {
            StockPeer current = peer.getSid().equals(updated.getSid()) ? updated : peer;
            if (current == updated) {
                replaced = true;
            }
            responses.add(toResponse(current));
        }
        if (!replaced) {
            responses.add(toResponse(updated));
        }
        return responses;
    }

    public List<StockPeerResponse> findAll() {
        return stockPeerRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<StockPeer> findStored() {
        return stockPeerRepository.findAll();
    }

    public List<String> findAllSids() {
        return stockPeerRepository.findAll().stream()
                .map(StockPeer::getSid)
                .filter(sid -> sid != null && !sid.isBlank())
                .distinct()
                .toList();
    }

    public List<YearlyRangeResponse> yearlyRanges(String sid) {
        Map<Integer, YearBucket> byYear = new HashMap<>();
        for (TickertapeChartResponse.Point point : chartPoints(tickertapeClient.fetchChart(sid))) {
            if (point.getTs() == null || point.getLp() == null) {
                continue;
            }
            int year = point.getTs().atZone(ZoneOffset.UTC).getYear();
            byYear.computeIfAbsent(year, ignored -> new YearBucket()).accept(point.getTs(), point.getLp());
        }
        if (byYear.isEmpty()) {
            throw new UpstreamDataException("Tickertape chart for '" + sid + "' did not include prices");
        }
        return byYear.entrySet().stream()
                .sorted(Map.Entry.<Integer, YearBucket>comparingByKey(Comparator.reverseOrder()))
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    public List<MonthlyHighResponse> monthlyHighs(String sid) {
        Map<YearMonth, MonthBucket> byMonth = new HashMap<>();
        for (TickertapeChartResponse.Point point : chartPoints(tickertapeClient.fetchChart(sid, "max"))) {
            if (point.getTs() == null || point.getLp() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(point.getTs().atZone(ZoneOffset.UTC));
            byMonth.computeIfAbsent(month, ignored -> new MonthBucket()).accept(point.getTs(), point.getLp());
        }
        if (byMonth.isEmpty()) {
            throw new UpstreamDataException("Tickertape chart for '" + sid + "' did not include prices");
        }
        return byMonth.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, MonthBucket>comparingByKey(Comparator.reverseOrder()))
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    private static List<TickertapeChartResponse.Point> chartPoints(TickertapeChartResponse chart) {
        if (chart == null || chart.getData() == null) {
            return List.of();
        }
        return chart.getData().stream()
                .filter(series -> series.getPoints() != null)
                .flatMap(series -> series.getPoints().stream())
                .toList();
    }

    public StockPeerResponse syncOne(String sid) {
        TickertapeInfoResponse info = tickertapeClient.fetchInfo(sid);
        if (info == null || info.getData() == null || info.getData().getRatios() == null) {
            throw new UpstreamDataException("Tickertape info for '" + sid + "' did not include ratios");
        }

        String infoSid = info.getData().getSid() != null ? info.getData().getSid() : sid;
        StockPeer stock = stockPeerRepository.findBySid(infoSid).orElseGet(StockPeer::new);
        if (stock.getSid() == null) {
            stock.setSid(infoSid);
        }
        TickertapeInfoResponse.Profile profile = info.getData().getInfo();
        if (profile != null) {
            if (profile.getName() != null) {
                stock.setName(profile.getName());
            }
            if (profile.getTicker() != null) {
                stock.setTicker(profile.getTicker());
            }
            if (profile.getSector() != null) {
                stock.setSector(profile.getSector());
            }
        }
        applyInfoRatios(stock, info.getData().getRatios(), Instant.now());
        applyQuote(stock);
        return toResponse(stockPeerRepository.save(stock));
    }

    private StockPeer upsert(TickertapeSummaryResponse.AboutAndPeer source, Instant now) {
        StockPeer peer = stockPeerRepository.findBySid(source.getSid()).orElseGet(StockPeer::new);
        TickertapeSummaryResponse.Ratios ratios = source.getRatios();

        peer.setSid(source.getSid());
        peer.setName(source.getName());
        peer.setTicker(source.getTicker());
        peer.setSector(source.getSector());
        if (ratios != null) {
            peer.setTtmPe(ratios.getTtmPe());
            peer.setBreco(ratios.getBreco());
            peer.setWeek52Pct(ratios.getWeek52Pct());
            peer.setApef(ratios.getApef());
            peer.setDivDps(ratios.getDivDps());
            peer.setPbr(ratios.getPbr());
        }
        peer.setUpdatedAt(now);
        return peer;
    }

    private void applyInfoRatios(StockPeer peer, TickertapeInfoResponse.Ratios ratios, Instant now) {
        peer.setWeek52High(ratios.getWeek52High());
        peer.setWeek52Low(ratios.getWeek52Low());
        peer.setWeek52Pct(ratios.getWeek52Pct());
        peer.setWeek4Pct(ratios.getWeek4Pct());
        peer.setDivYield(ratios.getDivYield());
        peer.setMarketCap(ratios.getMarketCap());
        peer.setAvgVolume3m(ratios.getAvgVolume3m());
        peer.setPe(ratios.getPe());
        peer.setPb(ratios.getPb());
        peer.setBeta(ratios.getBeta());
        peer.setInddy(ratios.getInddy());
        peer.setIndpb(ratios.getIndpb());
        peer.setIndpe(ratios.getIndpe());
        peer.setLastPrice(ratios.getLastPrice());
        peer.setRisk(ratios.getRisk());
        peer.setMrktCapRank(ratios.getMrktCapRank());
        peer.setEps(ratios.getEps());
        peer.setBps(ratios.getBps());
        peer.setRoe(ratios.getRoe());
        peer.setShareholderCount(ratios.getNShareholders());
        peer.setTtmPe(ratios.getTtmPe());
        peer.setMarketCapLabel(ratios.getMarketCapLabel());
        peer.setMrktCapf(ratios.getMrktCapf());
        peer.setVol12m(ratios.getVol12m());
        peer.setApef(ratios.getApef());
        peer.setPbr(ratios.getPbr());
        peer.setExpenseRatio(ratios.getExpenseRatio());
        peer.setTrackErr(ratios.getTrackErr());
        peer.setIndExpenseRatio(ratios.getIndExpenseRatio());
        peer.setIndTrackErr(ratios.getIndTrackErr());
        peer.setAsstUnderMan(ratios.getAsstUnderMan());
        peer.setEtfLiq(ratios.getEtfLiq());
        peer.setEtfLiqLabel(ratios.getEtfLiqLabel());
        peer.setUpdatedAt(now);
    }

    private void applyQuote(StockPeer stock) {
        TickertapeQuoteResponse response = tickertapeClient.fetchQuote(stock.getSid());
        TickertapeQuoteResponse.Quote quote = response == null || response.getData() == null
                ? null
                : response.getData().stream()
                .filter(item -> stock.getSid().equals(item.getSid()))
                .findFirst()
                .orElse(response.getData().isEmpty() ? null : response.getData().get(0));
        if (quote == null) {
            throw new UpstreamDataException("Tickertape quote for '" + stock.getSid() + "' was not found");
        }

        stock.setType(quote.getType());
        stock.setExchange(quote.getExchange());
        stock.setPrice(quote.getPrice());
        stock.setO(quote.getO());
        stock.setH(quote.getH());
        stock.setL(quote.getL());
        stock.setC(quote.getC());
        stock.setChange(quote.getChange());
        stock.setVol(quote.getVol());
        stock.setTs(quote.getTs());
        stock.setWkRange(quote.getWkRange());
        stock.setMnRange(quote.getMnRange());
        stock.setDyChange(quote.getDyChange());
        stock.setWkChange(quote.getWkChange());
        stock.setMnChange(quote.getMnChange());
        stock.setCrossedHigh(quote.getCrossedHigh());
        stock.setCrossedLow(quote.getCrossedLow());
        stock.setAway52wH(quote.getAway52wH());
        stock.setAway52wL(quote.getAway52wL());
        stock.setTurnover(quote.getTurnover());
        stock.setVolBreakout(quote.getVolBreakout());
        stock.setLow52w(quote.getLow52w());
        stock.setHigh52w(quote.getHigh52w());
    }

    private StockPeerResponse toResponse(StockPeer peer) {
        return StockPeerResponse.builder()
                .id(peer.getId())
                .name(peer.getName())
                .ticker(peer.getTicker())
                .sid(peer.getSid())
                .sector(peer.getSector())
                .ttmPe(peer.getTtmPe())
                .breco(peer.getBreco())
                .week52Pct(peer.getWeek52Pct())
                .marketCap(peer.getMarketCap())
                .apef(peer.getApef())
                .divDps(peer.getDivDps())
                .pbr(peer.getPbr())
                .week52High(peer.getWeek52High())
                .week52Low(peer.getWeek52Low())
                .week4Pct(peer.getWeek4Pct())
                .divYield(peer.getDivYield())
                .avgVolume3m(peer.getAvgVolume3m())
                .pe(peer.getPe())
                .pb(peer.getPb())
                .beta(peer.getBeta())
                .inddy(peer.getInddy())
                .indpb(peer.getIndpb())
                .indpe(peer.getIndpe())
                .lastPrice(peer.getLastPrice())
                .risk(peer.getRisk())
                .mrktCapRank(peer.getMrktCapRank())
                .eps(peer.getEps())
                .bps(peer.getBps())
                .roe(peer.getRoe())
                .shareholderCount(peer.getShareholderCount())
                .marketCapLabel(peer.getMarketCapLabel())
                .mrktCapf(peer.getMrktCapf())
                .vol12m(peer.getVol12m())
                .expenseRatio(peer.getExpenseRatio())
                .trackErr(peer.getTrackErr())
                .indExpenseRatio(peer.getIndExpenseRatio())
                .indTrackErr(peer.getIndTrackErr())
                .asstUnderMan(peer.getAsstUnderMan())
                .etfLiq(peer.getEtfLiq())
                .etfLiqLabel(peer.getEtfLiqLabel())
                .type(peer.getType())
                .exchange(peer.getExchange())
                .price(peer.getPrice())
                .o(peer.getO())
                .h(peer.getH())
                .l(peer.getL())
                .c(peer.getC())
                .change(peer.getChange())
                .vol(peer.getVol())
                .ts(peer.getTs())
                .wkRange(peer.getWkRange())
                .mnRange(peer.getMnRange())
                .dyChange(peer.getDyChange())
                .wkChange(peer.getWkChange())
                .mnChange(peer.getMnChange())
                .crossedHigh(peer.getCrossedHigh())
                .crossedLow(peer.getCrossedLow())
                .away52wH(peer.getAway52wH())
                .away52wL(peer.getAway52wL())
                .turnover(peer.getTurnover())
                .volBreakout(peer.getVolBreakout())
                .low52w(peer.getLow52w())
                .high52w(peer.getHigh52w())
                .updatedAt(peer.getUpdatedAt())
                .build();
    }

    private static final class YearBucket {
        private double low = Double.POSITIVE_INFINITY;
        private double high = Double.NEGATIVE_INFINITY;
        private Instant lowTs;
        private Instant highTs;

        private void accept(Instant ts, double price) {
            if (price < low) {
                low = price;
                lowTs = ts;
            }
            if (price > high) {
                high = price;
                highTs = ts;
            }
        }

        private YearlyRangeResponse toResponse(int year) {
            return new YearlyRangeResponse(year, low, week(lowTs), high, week(highTs));
        }

        private static String week(Instant ts) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(ts.atZone(ZoneOffset.UTC));
        }
    }

    private static final class MonthBucket {
        private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        private double low = Double.POSITIVE_INFINITY;
        private double high = Double.NEGATIVE_INFINITY;
        private Instant lowTs;
        private Instant highTs;

        private void accept(Instant ts, double price) {
            if (price < low) {
                low = price;
                lowTs = ts;
            }
            if (price > high) {
                high = price;
                highTs = ts;
            }
        }

        private MonthlyHighResponse toResponse(YearMonth month) {
            return new MonthlyHighResponse(
                    month.format(MONTH),
                    low,
                    date(lowTs),
                    high,
                    date(highTs));
        }

        private static String date(Instant ts) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(ts.atZone(ZoneOffset.UTC));
        }
    }
}
