package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record WorldHazardSnapshot(
        List<Identifier> regionIds,
        List<Identifier> hazardIds,
        int severity,
        boolean safeZone,
        String summary) {
    public WorldHazardSnapshot {
        regionIds = regionIds == null ? List.of() : List.copyOf(regionIds);
        hazardIds = hazardIds == null ? List.of() : List.copyOf(hazardIds);
        summary = summary == null ? "" : summary;
    }

    public WorldHazardSnapshot(boolean safeZone, String summary, int severity, List<Identifier> hazardIds) {
        this(List.of(), hazardIds, severity, safeZone, summary);
    }

    public static WorldHazardSnapshot fromTelemetry(EchoHazardTelemetry telemetry) {
        EchoHazardTelemetry safe = telemetry == null ? EchoHazardTelemetry.nominal() : telemetry;
        return new WorldHazardSnapshot(List.of(), List.of(), safe.severity(), safe.safeZone(), safe.summary());
    }

    public static WorldHazardSnapshot nominal() {
        return new WorldHazardSnapshot(List.of(), List.of(), 0, true, "Stable");
    }
}
