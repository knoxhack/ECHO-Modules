package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoStructureProfile(
        EchoStructureId id,
        EchoStructureKind kind,
        String displayName,
        EchoModuleId owningModule,
        EchoFactionId factionId,
        EchoContentReference structureContent,
        EchoContentReference lootProfileReference,
        EchoContentReference holomapLayerReference,
        EchoContentReference lensScanReference,
        List<EchoPoiMetadata> poiMetadata,
        List<EchoProgressionId> linkedMissions,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoStructureProfile {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoStructureKind.UNKNOWN : kind;
        displayName = StructureContractGuards.requireText(displayName, "structure display name");
        poiMetadata = StructureContractGuards.immutableList(poiMetadata);
        linkedMissions = StructureContractGuards.immutableList(linkedMissions);
        diagnostics = StructureContractGuards.immutableList(diagnostics);
        attributes = StructureContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || poiMetadata.stream().anyMatch(EchoPoiMetadata::blocking);
    }
}
