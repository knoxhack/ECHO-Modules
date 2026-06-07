package com.knoxhack.echo.npcore.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcProfileManager {
    private static volatile Map<Identifier, EchoNpcProfile> profiles = Map.of();
    private static volatile List<String> warnings = List.of();

    private EchoNpcProfileManager() {
    }

    public static void replace(Map<Identifier, EchoNpcProfile> loaded, List<String> loadWarnings) {
        profiles = Map.copyOf(loaded == null ? Map.of() : loaded);
        warnings = List.copyOf(loadWarnings == null ? List.of() : loadWarnings);
    }

    public static Optional<EchoNpcProfile> get(Identifier id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public static EchoNpcProfile getOrFallback(Identifier id) {
        EchoNpcProfile profile = profiles.get(id);
        if (profile != null) {
            return profile;
        }
        profile = profiles.get(EchoNpcProfile.FALLBACK_ID);
        if (profile != null) {
            return profile;
        }
        return new EchoNpcProfile(
                EchoNpcProfile.FALLBACK_ID,
                "Test Survivor",
                "Field Survivor",
                Identifier.fromNamespaceAndPath("echonpcore", "survivors"),
                EchoNpcProfile.FALLBACK_ID,
                EchoNpcProfile.FALLBACK_ID,
                Identifier.fromNamespaceAndPath("echonpcore", "test_survivor_basic"),
                Identifier.fromNamespaceAndPath("echonpcore", "test_survivor_services"),
                List.of(),
                List.of("No profile data loaded."),
                5.0D,
                EchoNpcBehaviorSettings.DEFAULT,
                EchoNpcIntegrationHints.DEFAULT,
                Map.of());
    }

    public static int count() {
        return profiles.size();
    }

    public static List<Identifier> ids() {
        return new ArrayList<>(profiles.keySet()).stream().sorted().toList();
    }

    public static List<String> warnings() {
        return warnings;
    }

    public static Map<Identifier, EchoNpcProfile> snapshot() {
        return new LinkedHashMap<>(profiles);
    }
}
