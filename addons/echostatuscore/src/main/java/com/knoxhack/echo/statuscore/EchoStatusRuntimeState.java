package com.knoxhack.echo.statuscore;

import java.util.Map;

public final class EchoStatusRuntimeState {
    private static volatile ActiveStatusRegistry activeRegistry = ActiveStatusRegistry.empty();

    private EchoStatusRuntimeState() {
    }

    public static ActiveStatusRegistry activeRegistry() {
        return activeRegistry;
    }

    public static synchronized ActiveStatusRegistry materializeServerRegistry(String sourceReason) {
        String source = sourceReason == null || sourceReason.isBlank() ? "server.starting" : sourceReason;
        ActiveStatusRegistry registry = new ActiveStatusRegistry(
                EchoStatusId.of("salvage_debris"),
                EchoStatusKind.HAZARD,
                "server_starting_salvage_debris",
                "scraplined_boots",
                1,
                1,
                Map.of("runtimeSource", source));
        activeRegistry = registry;
        return registry;
    }

    public record ActiveStatusRegistry(
            EchoStatusId statusId,
            EchoStatusKind kind,
            String exposureId,
            String resistanceId,
            int exposureCount,
            int resistanceCount,
            Map<String, String> attributes
    ) {
        public ActiveStatusRegistry {
            statusId = statusId == null ? EchoStatusId.of("unknown") : statusId;
            kind = kind == null ? EchoStatusKind.UNKNOWN : kind;
            exposureId = StatusContractGuards.optionalText(exposureId);
            resistanceId = StatusContractGuards.optionalText(resistanceId);
            exposureCount = StatusContractGuards.nonNegative(exposureCount, "status exposure count");
            resistanceCount = StatusContractGuards.nonNegative(resistanceCount, "status resistance count");
            attributes = StatusContractGuards.immutableMap(attributes);
        }

        public static ActiveStatusRegistry empty() {
            return new ActiveStatusRegistry(
                    EchoStatusId.of("unknown"),
                    EchoStatusKind.UNKNOWN,
                    "",
                    "",
                    0,
                    0,
                    Map.of());
        }

        public boolean materialized() {
            return !exposureId.isBlank()
                    && !resistanceId.isBlank()
                    && exposureCount > 0
                    && resistanceCount > 0;
        }
    }
}
