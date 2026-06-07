package com.knoxhack.echo.scriptcore.api;

import java.util.List;

public record EchoScriptDiagnosticsSummary(
        List<String> loadedPacks,
        int definitionCount,
        long errors,
        long warnings,
        long infos,
        List<String> missingAdapters,
        long brokenReferences,
        long circularMissionChains,
        long invalidObjectives,
        long unknownActions,
        long unknownConditions,
        long holomapMarkerIssues,
        long unreachableArchiveEntries,
        long impossibleEndings,
        boolean runtimeStorageAvailable,
        String runtimeStorageBackend) {
    public EchoScriptDiagnosticsSummary {
        loadedPacks = List.copyOf(loadedPacks == null ? List.of() : loadedPacks);
        missingAdapters = List.copyOf(missingAdapters == null ? List.of() : missingAdapters);
        runtimeStorageBackend = runtimeStorageBackend == null ? "unknown" : runtimeStorageBackend;
    }
}
