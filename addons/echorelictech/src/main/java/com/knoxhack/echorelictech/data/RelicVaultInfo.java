package com.knoxhack.echorelictech.data;

import java.util.List;

public record RelicVaultInfo(
        String id,
        String displayName,
        String tier,
        String lootTable,
        String materialLootTable,
        String securityLevel,
        int minY,
        int maxY,
        int spawnWeight,
        List<String> requiredBiomeTags,
        List<String> excludedBiomes,
        String markerText,
        String progressionPhase,
        List<String> notes) {
    public RelicVaultInfo {
        requiredBiomeTags = List.copyOf(requiredBiomeTags == null ? List.of() : requiredBiomeTags);
        excludedBiomes = List.copyOf(excludedBiomes == null ? List.of() : excludedBiomes);
        notes = List.copyOf(notes == null ? List.of() : notes);
        markerText = markerText == null || markerText.isBlank() ? displayName : markerText;
        progressionPhase = progressionPhase == null || progressionPhase.isBlank() ? "relic_ops" : progressionPhase;
    }
}
