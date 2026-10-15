package com.tickertape.tickertape_spring.service;

import com.tickertape.tickertape_spring.model.StockPeer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HoldingNameMatcher {

    private static final Set<String> STOP = Set.of(
            "LTD", "LIMITED", "INDIA", "THE", "AND", "OF", "CO", "COMPANY",
            "CORP", "CORPORATION", "PLC", "PVT", "PRIVATE", "INC", "IND"
    );

    private HoldingNameMatcher() {
    }

    public static StockPeer match(String company, List<StockPeer> stocks) {
        List<String> name = tokens(company);
        StockPeer best = null;
        double bestScore = 0;
        double second = 0;
        for (StockPeer stock : stocks) {
            double score = score(name, tokens(stock.getName()));
            if (score > bestScore) {
                second = bestScore;
                bestScore = score;
                best = stock;
            } else if (score > second) {
                second = score;
            }
        }
        if (best == null) {
            return null;
        }
        String first = name.isEmpty() ? "" : name.get(0);
        List<String> stockTokens = tokens(best.getName());
        if (first.length() >= 4 && stockTokens.stream().noneMatch(token -> softEqual(first, token))) {
            return null;
        }
        if (bestScore >= 0.75 && bestScore - second >= 0.08) {
            return best;
        }
        if (bestScore >= 0.67 && bestScore - second >= 0.15) {
            return best;
        }
        return null;
    }

    private static double score(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int hit = overlap(left, right);
        double dice = (2.0 * hit) / (left.size() + right.size());
        boolean leading = dice >= 0.62
                && !left.isEmpty()
                && !right.isEmpty()
                && left.get(0).length() >= 5
                && softEqual(left.get(0), right.get(0))
                && left.size() > 1
                && right.size() > 1
                && softEqual(left.get(1), right.get(1));
        return leading ? Math.max(dice, 0.86) : dice;
    }

    private static int overlap(List<String> left, List<String> right) {
        Set<Integer> used = new HashSet<>();
        int hit = 0;
        for (String token : left) {
            for (int index = 0; index < right.size(); index++) {
                if (!used.contains(index) && softEqual(token, right.get(index))) {
                    used.add(index);
                    hit++;
                    break;
                }
            }
        }
        return hit;
    }

    private static boolean softEqual(String left, String right) {
        if (left.equals(right)) {
            return true;
        }
        if (left.length() < 4 || right.length() < 4) {
            return false;
        }
        return left.startsWith(right) || right.startsWith(left);
    }

    private static List<String> tokens(String name) {
        String normalized = String.valueOf(name == null ? "" : name)
                .toUpperCase(Locale.ROOT)
                .replace("&", " AND ")
                .replaceAll("[^A-Z0-9 ]", " ");
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() > 1 && !STOP.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
