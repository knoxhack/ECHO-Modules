package com.knoxhack.echo.guidecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoGuideRegistry(
        Map<EchoGuidePageId, EchoGuidePage> pages,
        Map<EchoGuideCategoryId, EchoGuideCategory> categories,
        List<EchoGuideSearchIndex> searchIndexes,
        List<EchoDiagnostic> diagnostics
) {
    public EchoGuideRegistry {
        pages = GuideContractGuards.immutableMap(pages);
        categories = GuideContractGuards.immutableMap(categories);
        searchIndexes = GuideContractGuards.immutableList(searchIndexes);
        diagnostics = GuideContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || pages.values().stream().anyMatch(EchoGuidePage::blocking);
    }
}
