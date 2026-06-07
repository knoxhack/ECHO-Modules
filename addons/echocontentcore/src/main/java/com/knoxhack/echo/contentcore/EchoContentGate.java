package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Map;
import java.util.Set;

public record EchoContentGate(
        String gateId,
        Set<EchoFeatureId> requiredFeatures,
        Set<EchoModuleId> requiredModules,
        Set<EchoPackId> requiredPacks,
        Set<EchoGameModeId> gameModes,
        Set<EchoRuntimeSide> sides,
        boolean optional,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoContentGate {
        gateId = ContentContractGuards.requireText(gateId, "content gate id");
        requiredFeatures = ContentContractGuards.immutableSet(requiredFeatures);
        requiredModules = ContentContractGuards.immutableSet(requiredModules);
        requiredPacks = ContentContractGuards.immutableSet(requiredPacks);
        gameModes = ContentContractGuards.immutableSet(gameModes);
        sides = ContentContractGuards.immutableSet(sides);
        playerSummary = ContentContractGuards.optionalText(playerSummary);
        developerDetails = ContentContractGuards.optionalText(developerDetails);
        attributes = ContentContractGuards.immutableMap(attributes);
    }

    public static EchoContentGate open() {
        return new EchoContentGate("open", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), true, "", "", Map.of());
    }

    public boolean gated() {
        return !requiredFeatures.isEmpty()
                || !requiredModules.isEmpty()
                || !requiredPacks.isEmpty()
                || !gameModes.isEmpty()
                || !sides.isEmpty()
                || !playerSummary.isEmpty()
                || !developerDetails.isEmpty();
    }

    public boolean blocksWhenMissing() {
        return gated() && !optional;
    }
}
