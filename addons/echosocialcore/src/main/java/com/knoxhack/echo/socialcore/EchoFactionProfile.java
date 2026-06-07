package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoFactionProfile(
        EchoFactionId id,
        String displayName,
        String summary,
        EchoModuleId owningModule,
        Set<EchoFeatureId> providedFeatures,
        List<EchoFactionReputation> reputationDefaults,
        List<EchoFactionTerritory> territories,
        List<EchoFactionAlliance> alliances,
        List<EchoProgressionId> ownedProgressions,
        List<EchoContentReference> ownedContentReferences,
        List<EchoDialogueTreeId> dialogueTrees,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoFactionProfile {
        Objects.requireNonNull(id, "id");
        displayName = SocialContractGuards.requireText(displayName, "faction display name");
        summary = SocialContractGuards.optionalText(summary);
        providedFeatures = SocialContractGuards.immutableSet(providedFeatures);
        reputationDefaults = SocialContractGuards.immutableList(reputationDefaults);
        territories = SocialContractGuards.immutableList(territories);
        alliances = SocialContractGuards.immutableList(alliances);
        ownedProgressions = SocialContractGuards.immutableList(ownedProgressions);
        ownedContentReferences = SocialContractGuards.immutableList(ownedContentReferences);
        dialogueTrees = SocialContractGuards.immutableList(dialogueTrees);
        diagnostics = SocialContractGuards.immutableList(diagnostics);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
