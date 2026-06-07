package com.knoxhack.echo.logisticscore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLogisticsStorageNode(
        EchoLogisticsNodeId id,
        EchoLogisticsNodeKind kind,
        EchoContentReference contentReference,
        List<EchoLogisticsChannelKind> channels,
        int itemSlots,
        int fluidTanks,
        int signalCapacity,
        boolean acceptsRemoteRequests,
        Map<String, String> attributes
) {
    public EchoLogisticsStorageNode {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoLogisticsNodeKind.UNKNOWN : kind;
        channels = LogisticsContractGuards.immutableList(channels);
        itemSlots = LogisticsContractGuards.nonNegative(itemSlots, "item slots");
        fluidTanks = LogisticsContractGuards.nonNegative(fluidTanks, "fluid tanks");
        signalCapacity = LogisticsContractGuards.nonNegative(signalCapacity, "signal capacity");
        attributes = LogisticsContractGuards.immutableMap(attributes);
    }
}
