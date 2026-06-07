package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoLootEntry(
        String entryId,
        EchoContentReference contentReference,
        EchoLootRarity rarity,
        int weight,
        int minCount,
        int maxCount,
        boolean unique,
        EchoContentGate gate,
        Map<String, String> attributes
) {
    public EchoLootEntry {
        entryId = LootContractGuards.requireText(entryId, "loot entry id");
        rarity = rarity == null ? EchoLootRarity.UNKNOWN : rarity;
        weight = LootContractGuards.positiveOrOne(weight);
        minCount = LootContractGuards.nonNegative(minCount, "loot min count");
        maxCount = Math.max(minCount, maxCount);
        gate = gate == null ? EchoContentGate.open() : gate;
        attributes = LootContractGuards.immutableMap(attributes);
    }
}
