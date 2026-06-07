package com.knoxhack.echoarcanacore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record CurseDefinition(
        Identifier id,
        String translationKey,
        CurseCategory category,
        int stageCount,
        int maxSeverity,
        boolean visible,
        Identifier discoveryCondition,
        List<String> symptoms,
        List<String> effectsPerStage,
        List<Identifier> cleansingMethods,
        Identifier contractOption,
        Identifier visualProfile,
        Identifier soundProfile,
        Identifier indexPageId,
        Identifier grimoirePageId) {
    public CurseDefinition {
        translationKey = translationKey == null || translationKey.isBlank() ? id.toString() : translationKey.strip();
        category = category == null ? CurseCategory.MIND : category;
        stageCount = Math.max(1, stageCount);
        maxSeverity = Math.max(1, maxSeverity);
        symptoms = List.copyOf(symptoms == null ? List.of() : symptoms);
        effectsPerStage = List.copyOf(effectsPerStage == null ? List.of() : effectsPerStage);
        cleansingMethods = List.copyOf(cleansingMethods == null ? List.of() : cleansingMethods);
    }
}
