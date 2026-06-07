package com.knoxhack.echo.logisticscore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLogisticsRoute(
        EchoLogisticsRouteId id,
        EchoLogisticsNodeId fromNode,
        EchoLogisticsNodeId toNode,
        List<EchoLogisticsChannelKind> channels,
        int priority,
        boolean bidirectional,
        EchoContentReference routeReference,
        Map<String, String> attributes
) {
    public EchoLogisticsRoute {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fromNode, "fromNode");
        Objects.requireNonNull(toNode, "toNode");
        channels = LogisticsContractGuards.immutableList(channels);
        priority = LogisticsContractGuards.nonNegative(priority, "route priority");
        attributes = LogisticsContractGuards.immutableMap(attributes);
    }
}
