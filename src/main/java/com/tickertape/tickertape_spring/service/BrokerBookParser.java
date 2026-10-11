package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.model.EquityHolding;
import com.tickertape.tickertape_spring.model.EquityOrder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BrokerBookParser {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH);

    private BrokerBookParser() {
    }

    public static List<EquityHolding> holdings(Reader reader) throws IOException {
        List<EquityHolding> holdings = new ArrayList<>();
        int line = 0;
        for (Map<String, String> row : rows(reader)) {
            String symbol = row.getOrDefault("Stock Symbol", "").trim();
            String company = row.getOrDefault("Company Name", "").trim();
            if (symbol.isEmpty() || company.isEmpty()) {
                continue;
            }
            holdings.add(EquityHolding.builder()
                    .line(line++)
                    .symbol(symbol)
                    .company(company)
                    .isin(row.getOrDefault("ISIN Code", "").trim())
                    .qty(money(row.get("Qty")))
                    .avgCost(money(row.get("Average Cost Price")))
                    .price(money(row.get("Current Market Price")))
                    .dayPct(money(row.get("% Change over prev close")))
                    .costValue(money(row.get("Value At Cost")))
                    .marketValue(money(row.get("Value At Market Price")))
                    .realized(money(row.get("Realized Profit / Loss")))
                    .unrealized(money(row.get("Unrealized Profit/Loss")))
                    .unrealizedPct(money(row.get("Unrealized Profit/Loss %")))
                    .build());
        }
        return holdings;
    }

    public static List<EquityOrder> orders(Reader reader) throws IOException {
        List<EquityOrder> orders = new ArrayList<>();
        int line = 0;
        for (Map<String, String> row : rows(reader)) {
            String symbol = row.getOrDefault("Stock", "").trim().toUpperCase(Locale.ROOT);
            LocalDate date = orderDate(row.get("Date"));
            if (symbol.isEmpty() || date == null) {
                continue;
            }
            orders.add(EquityOrder.builder()
                    .line(line++)
                    .tradeDate(date)
                    .symbol(symbol)
                    .action(row.getOrDefault("Action", "").trim())
                    .qty(money(row.get("Qty")))
                    .price(money(row.get("Price")))
                    .tradeValue(money(row.get("Trade Value")))
                    .orderRef(row.getOrDefault("Order Ref.", "").trim())
                    .settlement(row.getOrDefault("Settlement", "").trim())
                    .segment(row.getOrDefault("Segment", "").trim())
                    .dpId(row.getOrDefault("DP Id - Client DP Id", "").trim())
                    .exchange(row.getOrDefault("Exchange", "").trim())
                    .stt(money(row.get("STT")))
                    .turnoverCharges(money(row.get("Transaction and SEBI Turnover charges")))
                    .stampDuty(money(row.get("Stamp Duty")))
                    .brokerage(money(row.get("Brokerage + Service Tax")))
                    .charges(money(row.get("Brokerage Incl. Taxes")))
                    .build());
        }
        return orders;
    }

    static Double money(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        boolean negative = text.startsWith("(") || text.startsWith("-");
        String digits = text.replaceAll("[(),%\\s+]", "");
        if (digits.startsWith("-")) {
            digits = digits.substring(1);
        }
        if (digits.isEmpty()) {
            return null;
        }
        return negative ? -Double.parseDouble(digits) : Double.parseDouble(digits);
    }

    private static LocalDate orderDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim(), ORDER_DATE);
    }

    private static List<Map<String, String>> rows(Reader reader) throws IOException {
        List<List<String>> table = parse(reader);
        if (table.isEmpty()) {
            return List.of();
        }
        List<String> headers = table.get(0).stream().map(cell -> stripBom(cell).trim()).toList();
        List<Map<String, String>> records = new ArrayList<>();
        for (int index = 1; index < table.size(); index++) {
            List<String> row = table.get(index);
            if (row.stream().allMatch(cell -> cell == null || cell.isBlank())) {
                continue;
            }
            Map<String, String> record = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                if (headers.get(column).isEmpty()) {
                    continue;
                }
                String value = column < row.size() ? row.get(column) : "";
                record.put(headers.get(column), value == null ? "" : value.trim());
            }
            records.add(record);
        }
        return records;
    }

    private static String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value == null ? "" : value;
    }

    private static List<List<String>> parse(Reader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        try (BufferedReader buffered = reader instanceof BufferedReader bufferedReader
                ? bufferedReader
                : new BufferedReader(reader)) {
            int current;
            while ((current = buffered.read()) != -1) {
                char ch = (char) current;
                if (quoted) {
                    if (ch == '"') {
                        buffered.mark(1);
                        int next = buffered.read();
                        if (next == '"') {
                            cell.append('"');
                        } else {
                            quoted = false;
                            if (next != -1) {
                                buffered.reset();
                            }
                        }
                    } else {
                        cell.append(ch);
                    }
                } else if (ch == '"') {
                    quoted = true;
                } else if (ch == ',') {
                    row.add(cell.toString());
                    cell.setLength(0);
                } else if (ch == '\n') {
                    row.add(cell.toString());
                    rows.add(row);
                    row = new ArrayList<>();
                    cell.setLength(0);
                } else if (ch != '\r') {
                    cell.append(ch);
                }
            }
        }
        if (!cell.isEmpty() || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }
}
