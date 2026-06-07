package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoCreatureScanMetadata(
        EchoContentReference lensScanReference,
        EchoContentReference codexEntryReference,
        EchoContentReference renderProfileReference,
        EchoContentReference soundProfileReference,
        EchoContentReference lootProfileReference,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        List<String> scanTags,
        Map<String, String> attributes
) {
    public EchoCreatureScanMetadata {
        optionalIntegrationFeatures = CreatureContractGuards.immutableSet(optionalIntegrationFeatures);
        scanTags = CreatureContractGuards.immutableList(scanTags);
        attributes = CreatureContractGuards.immutableMap(attributes);
    }
}
