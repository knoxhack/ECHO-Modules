package com.knoxhack.echo.npcore.trade;

import net.minecraft.resources.Identifier;

public record EchoNpcTradeCost(Identifier item, int count) {
    public EchoNpcTradeCost {
        count = Math.max(1, count);
    }
}
