package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.progressioncore.EchoObjectiveId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoPoiMetadata(
        EchoPoiId id,
        String displayName,
        EchoStructureId structureId,
        EchoDangerRating dangerRating,
        EchoFactionId factionId,
        EchoContentReference regionReference,
        EchoContentReference holomapDiscoveryReference,
        EchoContentReference lensScanReference,
        EchoContentReference lootProfileReference,
        EchoProgressionId missionProgressionId,
        EchoObjectiveId objectiveId,
        EchoDiscoveryState discoveryState,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoPoiMetadata {
        Objects.requireNonNull(id, "id");
        displayName = StructureContractGuards.requireText(displayName, "poi display name");
        dangerRating = dangerRating == null ? EchoDangerRating.UNKNOWN : dangerRating;
        discoveryState = discoveryState == null ? EchoDiscoveryState.UNKNOWN : discoveryState;
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = StructureContractGuards.immutableList(diagnostics);
        attributes = StructureContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
