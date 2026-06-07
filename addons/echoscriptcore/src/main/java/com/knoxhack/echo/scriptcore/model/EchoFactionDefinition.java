package com.knoxhack.echo.scriptcore.model;

import java.util.List;

public record EchoFactionDefinition(
        EchoScriptDefinition base,
        String displayName,
        String descriptionText,
        int startingReputation,
        List<EchoFactionRank> ranks,
        List<EchoFactionReputationEvent> reputationEvents) implements DelegatingScriptDefinition {
    public EchoFactionDefinition {
        displayName = displayName == null || displayName.isBlank() ? base.id().toString() : displayName;
        descriptionText = descriptionText == null ? "" : descriptionText;
        ranks = List.copyOf(ranks == null ? List.of() : ranks);
        reputationEvents = List.copyOf(reputationEvents == null ? List.of() : reputationEvents);
    }
}
