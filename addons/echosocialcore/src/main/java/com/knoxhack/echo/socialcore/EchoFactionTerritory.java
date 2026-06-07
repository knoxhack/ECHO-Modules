package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Map;
import java.util.Objects;

public record EchoFactionTerritory(
        String territoryId,
        EchoFactionId factionId,
        EchoContentId regionContent,
        EchoContentId poiContent,
        EchoContentId holomapLayerContent,
        EchoFeatureId holomapFeature,
        EchoContentGate gate,
        boolean contested,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoFactionTerritory {
        territoryId = SocialContractGuards.requireText(territoryId, "faction territory id");
        Objects.requireNonNull(factionId, "factionId");
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean hasHolomapReference() {
        return holomapLayerContent != null || holomapFeature != null;
    }
}
