package com.knoxhack.echo.npcore.trade;

import java.util.List;

public record EchoNpcTradeGroup(String id, String title, List<EchoNpcTradeOffer> offers) {
    public EchoNpcTradeGroup {
        id = id == null || id.isBlank() ? "default" : id.trim();
        title = title == null || title.isBlank() ? id : title.trim();
        offers = List.copyOf(offers == null ? List.of() : offers);
    }
}
