package com.knoxhack.echo.guidecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;
import java.util.Set;

public record EchoGuideCategory(
        EchoGuideCategoryId id,
        String titleTranslationKey,
        EchoContentReference iconReference,
        EchoContentGate visibilityGate,
        Set<String> tags,
        int sortOrder,
        Map<String, String> attributes
) {
    public EchoGuideCategory {
        titleTranslationKey = GuideContractGuards.requireText(titleTranslationKey, "guide category title translation key");
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        tags = GuideContractGuards.immutableSet(tags);
        sortOrder = GuideContractGuards.nonNegative(sortOrder, "guide category sort order");
        attributes = GuideContractGuards.immutableMap(attributes);
    }
}
