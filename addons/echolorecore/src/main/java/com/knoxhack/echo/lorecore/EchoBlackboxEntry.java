package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoBlackboxEntry(
        String blackboxEntryId,
        EchoLoreFragmentId fragmentId,
        String incidentTranslationKey,
        EchoContentReference missionReference,
        EchoContentReference structureReference,
        List<EchoContentReference> evidenceReferences,
        Map<String, String> attributes
) {
    public EchoBlackboxEntry {
        blackboxEntryId = LoreContractGuards.id(blackboxEntryId, "blackbox entry id");
        incidentTranslationKey = LoreContractGuards.requireText(incidentTranslationKey, "blackbox incident translation key");
        evidenceReferences = LoreContractGuards.immutableList(evidenceReferences);
        attributes = LoreContractGuards.immutableMap(attributes);
    }
}
