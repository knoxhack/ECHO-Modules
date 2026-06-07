package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLootProfile(
        EchoLootProfileId id,
        String displayName,
        EchoModuleId owningModule,
        EchoFactionId factionId,
        EchoContentReference poiReference,
        EchoContentReference weatherEventReference,
        EchoContentReference relicReference,
        List<EchoLootPool> pools,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoLootProfile {
        Objects.requireNonNull(id, "id");
        displayName = LootContractGuards.requireText(displayName, "loot profile display name");
        pools = LootContractGuards.immutableList(pools);
        diagnostics = LootContractGuards.immutableList(diagnostics);
        attributes = LootContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || pools.stream().anyMatch(EchoLootPool::blocking);
    }
}
