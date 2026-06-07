package com.knoxhack.echo.guidecore;

import java.util.Map;
import java.util.Set;

public record EchoGuideSearchIndex(
        String indexId,
        Set<EchoGuidePageId> indexedPages,
        Set<String> tags,
        Set<String> keywords,
        Map<String, String> attributes
) {
    public EchoGuideSearchIndex {
        indexId = GuideContractGuards.id(indexId, "guide search index id");
        indexedPages = GuideContractGuards.immutableSet(indexedPages);
        tags = GuideContractGuards.immutableSet(tags);
        keywords = GuideContractGuards.immutableSet(keywords);
        attributes = GuideContractGuards.immutableMap(attributes);
    }
}
