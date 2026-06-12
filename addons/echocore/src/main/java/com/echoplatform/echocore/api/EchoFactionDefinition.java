package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoFactionDefinition(
        Identifier id,
        String displayName,
        String shortName,
        String route,
        String summary,
        String hazard,
        String prepHint,
        String serviceSummary,
        int accentColor,
        boolean landmarkFaction,
        List<EchoNpcRole> roles,
        List<EchoFactionAction> actions,
        List<EchoFactionContract> contracts,
        List<EchoFactionPoiAffinity> poiAffinities,
        EchoDialogueTree dialogue) {
    public EchoFactionDefinition {
        displayName = displayName == null ? "" : displayName;
        shortName = shortName == null || shortName.isBlank() ? displayName : shortName;
        route = route == null ? "" : route;
        summary = summary == null ? "" : summary;
        hazard = hazard == null ? "" : hazard;
        prepHint = prepHint == null ? "" : prepHint;
        serviceSummary = serviceSummary == null ? "" : serviceSummary;
        roles = roles == null ? List.of() : List.copyOf(roles);
        actions = actions == null ? List.of() : List.copyOf(actions);
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
        poiAffinities = poiAffinities == null ? List.of() : List.copyOf(poiAffinities);
        dialogue = dialogue == null ? new EchoDialogueTree("", List.of(), "") : dialogue;
    }

    public String modId() {
        return id == null ? "" : id.getNamespace();
    }
}
