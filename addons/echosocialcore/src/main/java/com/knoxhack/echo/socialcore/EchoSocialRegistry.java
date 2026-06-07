package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoSocialRegistry(
        Map<EchoFactionId, EchoFactionProfile> factions,
        Map<EchoDialogueTreeId, EchoDialogueTree> dialogueTrees,
        Map<EchoNpcAiProfileId, EchoNpcAiProfile> aiProfiles,
        Map<EchoNpcProfileId, EchoNpcProfile> npcProfiles,
        List<EchoVillagerReplacementPlan> villagerReplacementPlans,
        List<EchoDiagnostic> diagnostics
) {
    public EchoSocialRegistry {
        factions = SocialContractGuards.immutableMap(factions);
        dialogueTrees = SocialContractGuards.immutableMap(dialogueTrees);
        aiProfiles = SocialContractGuards.immutableMap(aiProfiles);
        npcProfiles = SocialContractGuards.immutableMap(npcProfiles);
        villagerReplacementPlans = SocialContractGuards.immutableList(villagerReplacementPlans);
        diagnostics = SocialContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || factions.values().stream().anyMatch(EchoFactionProfile::blocking)
                || dialogueTrees.values().stream().anyMatch(EchoDialogueTree::blocking)
                || npcProfiles.values().stream().anyMatch(EchoNpcProfile::blocking)
                || villagerReplacementPlans.stream().anyMatch(EchoVillagerReplacementPlan::blocking);
    }
}
