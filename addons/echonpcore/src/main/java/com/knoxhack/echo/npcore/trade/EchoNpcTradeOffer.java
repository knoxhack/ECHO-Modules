package com.knoxhack.echo.npcore.trade;

import java.util.List;

public record EchoNpcTradeOffer(
        String id,
        String title,
        List<EchoNpcTradeCost> input,
        EchoNpcTradeCost output,
        int stock,
        int restockTime,
        String requiresMission,
        int requiresFactionStanding,
        String disabledReason) {
    public EchoNpcTradeOffer {
        id = clean(id, "offer");
        title = clean(title, id);
        input = List.copyOf(input == null ? List.of() : input);
        stock = Math.max(0, stock);
        restockTime = Math.max(0, restockTime);
        requiresMission = clean(requiresMission, "");
        disabledReason = clean(disabledReason, "");
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
