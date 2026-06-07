package com.knoxhack.echo.codexcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoCodexEntry(
        EchoCodexEntryId id,
        EchoCodexEntryKind kind,
        EchoCodexCategory category,
        EchoModuleId owningModule,
        String titleTranslationKey,
        String bodyTranslationKey,
        EchoCodexDiscoveryState discoveryState,
        EchoContentGate unlockGate,
        EchoContentReference primaryContent,
        EchoContentReference terminalArchiveReference,
        List<EchoContentReference> relatedContent,
        EchoCodexSearchMetadata searchMetadata,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCodexEntry {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoCodexEntryKind.UNKNOWN : kind;
        category = category == null ? EchoCodexCategory.UNKNOWN : category;
        titleTranslationKey = CodexContractGuards.requireText(titleTranslationKey, "codex title translation key");
        bodyTranslationKey = CodexContractGuards.requireText(bodyTranslationKey, "codex body translation key");
        discoveryState = discoveryState == null ? EchoCodexDiscoveryState.UNKNOWN : discoveryState;
        unlockGate = unlockGate == null ? EchoContentGate.open() : unlockGate;
        relatedContent = CodexContractGuards.immutableList(relatedContent);
        searchMetadata = searchMetadata == null
                ? new EchoCodexSearchMetadata(java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), 0, java.util.Map.of())
                : searchMetadata;
        diagnostics = CodexContractGuards.immutableList(diagnostics);
        attributes = CodexContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return unlockGate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
