package com.knoxhack.echo.npcore.conversion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcReplacementManager {
    private static volatile Map<Identifier, EchoNpcReplacementMapping> mappings = Map.of();

    private EchoNpcReplacementManager() {
    }

    public static void replace(Map<Identifier, EchoNpcReplacementMapping> loaded) {
        mappings = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static Optional<Identifier> profileForProfession(Identifier professionId) {
        for (EchoNpcReplacementMapping mapping : mappings.values()) {
            Identifier profile = mapping.replace().get(professionId);
            if (profile != null) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public static Optional<Identifier> profileForEntityType(Identifier entityTypeId) {
        for (EchoNpcReplacementMapping mapping : mappings.values()) {
            Identifier profile = mapping.entityTypes().get(entityTypeId);
            if (profile != null) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public static int count() {
        return mappings.size();
    }

    public static Map<Identifier, EchoNpcReplacementMapping> snapshot() {
        return new LinkedHashMap<>(mappings);
    }
}
