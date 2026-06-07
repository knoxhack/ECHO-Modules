package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoEnvironmentalStory(
        String storyId,
        String titleTranslationKey,
        EchoContentReference locationReference,
        List<EchoContentReference> sceneReferences,
        List<EchoLoreFragmentId> fragments,
        Map<String, String> attributes
) {
    public EchoEnvironmentalStory {
        storyId = LoreContractGuards.id(storyId, "environmental story id");
        titleTranslationKey = LoreContractGuards.requireText(titleTranslationKey, "environmental story title translation key");
        sceneReferences = LoreContractGuards.immutableList(sceneReferences);
        fragments = LoreContractGuards.immutableList(fragments);
        attributes = LoreContractGuards.immutableMap(attributes);
    }
}
