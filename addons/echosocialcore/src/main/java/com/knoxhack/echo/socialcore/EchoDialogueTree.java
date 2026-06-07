package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoDialogueTree(
        EchoDialogueTreeId id,
        String title,
        String summary,
        EchoModuleId owningModule,
        EchoDialogueNodeId rootNodeId,
        List<EchoDialogueNode> nodes,
        Set<EchoFactionId> factions,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoDialogueTree {
        Objects.requireNonNull(id, "id");
        title = SocialContractGuards.requireText(title, "dialogue tree title");
        summary = SocialContractGuards.optionalText(summary);
        Objects.requireNonNull(rootNodeId, "rootNodeId");
        nodes = SocialContractGuards.immutableList(nodes);
        factions = SocialContractGuards.immutableSet(factions);
        optionalIntegrationFeatures = SocialContractGuards.immutableSet(optionalIntegrationFeatures);
        diagnostics = SocialContractGuards.immutableList(diagnostics);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
