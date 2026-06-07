package com.knoxhack.echo.guidecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoGuidePage(
        EchoGuidePageId id,
        EchoGuidePageKind kind,
        EchoGuideCategoryId categoryId,
        EchoModuleId owningModule,
        String titleTranslationKey,
        String bodyTranslationKey,
        EchoGuideVisibility visibility,
        EchoContentGate unlockGate,
        EchoContentReference iconReference,
        List<EchoGuideRelatedContent> relatedContent,
        Set<String> tags,
        int sortOrder,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoGuidePage {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoGuidePageKind.UNKNOWN : kind;
        titleTranslationKey = GuideContractGuards.requireText(titleTranslationKey, "guide page title translation key");
        bodyTranslationKey = GuideContractGuards.requireText(bodyTranslationKey, "guide page body translation key");
        visibility = visibility == null ? EchoGuideVisibility.UNKNOWN : visibility;
        unlockGate = unlockGate == null ? EchoContentGate.open() : unlockGate;
        relatedContent = GuideContractGuards.immutableList(relatedContent);
        tags = GuideContractGuards.immutableSet(tags);
        sortOrder = GuideContractGuards.nonNegative(sortOrder, "guide page sort order");
        diagnostics = GuideContractGuards.immutableList(diagnostics);
        attributes = GuideContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return unlockGate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
