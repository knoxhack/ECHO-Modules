package com.knoxhack.echoarcanacore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record RitualDefinition(
        Identifier id,
        String translationKey,
        RitualFamily family,
        String altarType,
        Identifier structurePattern,
        Identifier centerBlock,
        List<Identifier> pedestalInputs,
        List<Identifier> requiredItems,
        List<Identifier> requiredFluids,
        double requiredAether,
        List<Identifier> requiredResonanceCategories,
        List<String> worldConditions,
        Identifier requiredResearch,
        List<Identifier> requiredNearbyBlocks,
        List<String> requiredWorldState,
        double instability,
        Identifier failureTable,
        Identifier successEffect,
        double pressureDelta,
        double fractureDelta,
        double curseRisk,
        Identifier indexPageId,
        Identifier grimoirePageId) {
    public RitualDefinition {
        translationKey = translationKey == null || translationKey.isBlank() ? id.toString() : translationKey.strip();
        family = family == null ? RitualFamily.CRAFTING : family;
        altarType = altarType == null ? "" : altarType.strip();
        pedestalInputs = List.copyOf(pedestalInputs == null ? List.of() : pedestalInputs);
        requiredItems = List.copyOf(requiredItems == null ? List.of() : requiredItems);
        requiredFluids = List.copyOf(requiredFluids == null ? List.of() : requiredFluids);
        requiredAether = Math.max(0.0D, requiredAether);
        requiredResonanceCategories = List.copyOf(requiredResonanceCategories == null ? List.of() : requiredResonanceCategories);
        worldConditions = List.copyOf(worldConditions == null ? List.of() : worldConditions);
        requiredNearbyBlocks = List.copyOf(requiredNearbyBlocks == null ? List.of() : requiredNearbyBlocks);
        requiredWorldState = List.copyOf(requiredWorldState == null ? List.of() : requiredWorldState);
        instability = Math.max(0.0D, instability);
        curseRisk = Math.max(0.0D, curseRisk);
    }
}
