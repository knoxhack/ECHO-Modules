package com.knoxhack.echo.npcore.trade;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcTradeManager {
    private static volatile Map<Identifier, EchoNpcTradeSet> trades = Map.of();

    private EchoNpcTradeManager() {
    }

    public static void replace(Map<Identifier, EchoNpcTradeSet> loaded) {
        trades = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static Optional<EchoNpcTradeSet> get(Identifier id) {
        return Optional.ofNullable(trades.get(id));
    }

    public static EchoNpcTradeSet getOrEmpty(Identifier id) {
        EchoNpcTradeSet set = trades.get(id);
        return set == null ? new EchoNpcTradeSet(id, java.util.List.of()) : set;
    }

    public static int count() {
        return trades.size();
    }
}
