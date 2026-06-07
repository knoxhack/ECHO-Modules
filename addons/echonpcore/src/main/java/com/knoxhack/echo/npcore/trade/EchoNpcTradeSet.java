package com.knoxhack.echo.npcore.trade;

import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoNpcTradeSet(Identifier id, List<EchoNpcTradeGroup> groups) {
    public EchoNpcTradeSet {
        groups = List.copyOf(groups == null ? List.of() : groups);
    }

    public EchoNpcTradeOffer offer(String offerId) {
        for (EchoNpcTradeGroup group : groups) {
            for (EchoNpcTradeOffer offer : group.offers()) {
                if (offer.id().equals(offerId)) {
                    return offer;
                }
            }
        }
        return null;
    }
}
