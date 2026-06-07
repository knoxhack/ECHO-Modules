package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.socialcore.EchoFactionId;

import java.util.Map;

public record EchoStructureBinding(
        String bindingId,
        EchoStructureBindingKind kind,
        EchoStructureId structureId,
        EchoPoiId poiId,
        EchoFactionId factionId,
        EchoProgressionId progressionId,
        EchoContentReference contentReference,
        EchoFeatureId optionalIntegrationFeature,
        boolean optional,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoStructureBinding {
        bindingId = StructureContractGuards.requireText(bindingId, "structure binding id");
        kind = kind == null ? EchoStructureBindingKind.UNKNOWN : kind;
        playerSummary = StructureContractGuards.optionalText(playerSummary);
        developerDetails = StructureContractGuards.optionalText(developerDetails);
        attributes = StructureContractGuards.immutableMap(attributes);
    }
}
