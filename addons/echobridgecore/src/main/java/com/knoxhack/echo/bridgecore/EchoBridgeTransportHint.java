package com.knoxhack.echo.bridgecore;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record EchoBridgeTransportHint(
        EchoBridgeTransportKind kind,
        String endpoint,
        boolean localOnly,
        boolean encrypted,
        boolean recommended,
        Duration heartbeatInterval,
        String setupHint,
        Map<String, String> attributes
) {
    public EchoBridgeTransportHint {
        kind = Objects.requireNonNullElse(kind, EchoBridgeTransportKind.LOCAL_LOOPBACK);
        endpoint = BridgeContractGuards.optionalText(endpoint);
        localOnly = true;
        heartbeatInterval = BridgeContractGuards.positiveDuration(
                heartbeatInterval == null ? Duration.ofSeconds(5L) : heartbeatInterval,
                "bridge heartbeat interval"
        );
        setupHint = BridgeContractGuards.optionalText(setupHint);
        attributes = BridgeContractGuards.immutableMap(attributes);
    }

    public static EchoBridgeTransportHint loopback(String endpoint) {
        return new EchoBridgeTransportHint(
                EchoBridgeTransportKind.LOCAL_LOOPBACK,
                endpoint,
                true,
                false,
                true,
                Duration.ofSeconds(5L),
                "Use a local loopback bridge endpoint. Do not expose it to cloud or public networks.",
                Map.of()
        );
    }
}
