package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoLoreRegistry(
        Map<EchoLoreFragmentId, EchoLoreFragment> fragments,
        List<EchoAudioLog> audioLogs,
        List<EchoBlackboxEntry> blackboxEntries,
        List<EchoEnvironmentalStory> environmentalStories,
        List<EchoDiagnostic> diagnostics
) {
    public EchoLoreRegistry {
        fragments = LoreContractGuards.immutableMap(fragments);
        audioLogs = LoreContractGuards.immutableList(audioLogs);
        blackboxEntries = LoreContractGuards.immutableList(blackboxEntries);
        environmentalStories = LoreContractGuards.immutableList(environmentalStories);
        diagnostics = LoreContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || fragments.values().stream().anyMatch(EchoLoreFragment::blocking);
    }
}
