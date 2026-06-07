package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLootPool(
        EchoLootPoolId id,
        String title,
        EchoLootSourceKind sourceKind,
        EchoLootRarity defaultRarity,
        EchoFactionId factionId,
        List<EchoLootEntry> entries,
        EchoDuplicationPolicy duplicationPolicy,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoLootPool {
        Objects.requireNonNull(id, "id");
        title = LootContractGuards.requireText(title, "loot pool title");
        sourceKind = sourceKind == null ? EchoLootSourceKind.UNKNOWN : sourceKind;
        defaultRarity = defaultRarity == null ? EchoLootRarity.UNKNOWN : defaultRarity;
        entries = LootContractGuards.immutableList(entries);
        duplicationPolicy = duplicationPolicy == null ? EchoDuplicationPolicy.UNKNOWN : duplicationPolicy;
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = LootContractGuards.immutableList(diagnostics);
        attributes = LootContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
