package com.knoxhack.echo.guidecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoGuideRelatedContent(
        String relationId,
        String relationKind,
        EchoContentReference contentReference,
        String labelTranslationKey,
        Map<String, String> attributes
) {
    public EchoGuideRelatedContent {
        relationId = GuideContractGuards.id(relationId, "guide related content id");
        relationKind = GuideContractGuards.requireText(relationKind, "guide related content kind");
        labelTranslationKey = GuideContractGuards.optionalText(labelTranslationKey);
        attributes = GuideContractGuards.immutableMap(attributes);
    }
}
