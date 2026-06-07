package com.knoxhack.echo.codexcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;
import java.util.Set;

public record EchoCodexArchive(
        String archiveId,
        String titleTranslationKey,
        EchoCodexCategory category,
        Set<EchoCodexEntryId> entries,
        EchoContentReference terminalArchiveReference,
        EchoContentGate visibilityGate,
        Map<String, String> attributes
) {
    public EchoCodexArchive {
        archiveId = CodexContractGuards.id(archiveId, "codex archive id");
        titleTranslationKey = CodexContractGuards.requireText(titleTranslationKey, "codex archive title translation key");
        category = category == null ? EchoCodexCategory.UNKNOWN : category;
        entries = CodexContractGuards.immutableSet(entries);
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        attributes = CodexContractGuards.immutableMap(attributes);
    }
}
