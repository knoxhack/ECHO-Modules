package com.knoxhack.echo.npcore.service;

import com.knoxhack.echo.npcore.trade.EchoNpcTradeCost;
import java.util.List;

public record EchoNpcServiceDefinition(
        String id,
        String title,
        String description,
        List<EchoNpcTradeCost> cost,
        String action,
        int amount,
        int cooldown,
        String requiresMission,
        int requiresFactionStanding,
        String target,
        String actionId,
        String disabledReason) {
    public EchoNpcServiceDefinition(String id, String title, String description, List<EchoNpcTradeCost> cost,
            String action, int amount, int cooldown) {
        this(id, title, description, cost, action, amount, cooldown, "", Integer.MIN_VALUE, "", "", "");
    }

    public EchoNpcServiceDefinition {
        id = clean(id, "service");
        title = clean(title, id);
        description = clean(description, "");
        cost = List.copyOf(cost == null ? List.of() : cost);
        action = clean(action, "noop");
        amount = Math.max(0, amount);
        cooldown = Math.max(0, cooldown);
        requiresMission = clean(requiresMission, "");
        target = clean(target, "");
        actionId = clean(actionId, "");
        disabledReason = clean(disabledReason, "");
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
