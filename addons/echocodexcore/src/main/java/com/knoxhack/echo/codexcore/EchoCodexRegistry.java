package com.knoxhack.echo.codexcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoCodexRegistry(
        Map<EchoCodexEntryId, EchoCodexEntry> entries,
        List<EchoCodexArchive> archives,
        List<EchoDiagnostic> diagnostics
) {
    public EchoCodexRegistry {
        entries = CodexContractGuards.immutableMap(entries);
        archives = CodexContractGuards.immutableList(archives);
        diagnostics = CodexContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || entries.values().stream().anyMatch(EchoCodexEntry::blocking);
    }
}
