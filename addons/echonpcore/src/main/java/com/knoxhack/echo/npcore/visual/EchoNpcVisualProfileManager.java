package com.knoxhack.echo.npcore.visual;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcVisualProfileManager {
    private static volatile Map<Identifier, EchoNpcVisualProfile> visualProfiles = Map.of();

    private EchoNpcVisualProfileManager() {
    }

    public static void replace(Map<Identifier, EchoNpcVisualProfile> loaded) {
        visualProfiles = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static Optional<EchoNpcVisualProfile> get(Identifier id) {
        return Optional.ofNullable(visualProfiles.get(id));
    }

    public static EchoNpcVisualProfile getOrFallback(Identifier id) {
        EchoNpcVisualProfile visual = visualProfiles.get(id);
        if (visual != null) {
            return visual;
        }
        visual = visualProfiles.get(Identifier.fromNamespaceAndPath("echonpcore", "test_survivor"));
        if (visual != null) {
            return visual;
        }
        return new EchoNpcVisualProfile(
                Identifier.fromNamespaceAndPath("echonpcore", "missing"),
                Identifier.fromNamespaceAndPath("echonpcore", "humanoid_basic"),
                EchoNpcVisualProfile.FALLBACK_TEXTURE,
                null,
                null,
                null,
                null,
                "missing",
                Identifier.fromNamespaceAndPath("echonpcore", "ashfall_survivor"),
                java.util.List.of());
    }

    public static int count() {
        return visualProfiles.size();
    }
}
