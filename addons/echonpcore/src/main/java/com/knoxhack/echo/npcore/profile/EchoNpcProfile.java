package com.knoxhack.echo.npcore.profile;

import com.knoxhack.echo.npcore.EchoNpcCore;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record EchoNpcProfile(
        Identifier id,
        String displayName,
        String role,
        Identifier faction,
        Identifier visualProfile,
        Identifier dialogue,
        Identifier trades,
        Identifier services,
        List<Identifier> missions,
        List<String> ambientLines,
        double interactionRange,
        EchoNpcBehaviorSettings behavior,
        EchoNpcIntegrationHints integrations,
        Map<String, String> convertedFrom) {
    public static final Identifier FALLBACK_ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "test_survivor");
    public static final Identifier NO_TRADES = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "none");
    public static final Identifier NO_SERVICES = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "none");

    public EchoNpcProfile {
        id = id == null ? FALLBACK_ID : id;
        displayName = clean(displayName, id.getPath());
        role = clean(role, "Contact");
        faction = faction == null ? Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "survivors") : faction;
        visualProfile = visualProfile == null ? id : visualProfile;
        dialogue = dialogue == null ? id : dialogue;
        trades = trades == null ? NO_TRADES : trades;
        services = services == null ? NO_SERVICES : services;
        missions = List.copyOf(missions == null ? List.of() : missions);
        ambientLines = List.copyOf(ambientLines == null ? List.of() : ambientLines);
        behavior = behavior == null ? EchoNpcBehaviorSettings.DEFAULT : behavior;
        integrations = integrations == null ? EchoNpcIntegrationHints.DEFAULT : integrations;
        convertedFrom = Map.copyOf(convertedFrom == null ? Map.of() : convertedFrom);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
