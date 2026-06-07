package com.knoxhack.echo.npcore.faction;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcFactionManager {
    private static volatile Map<Identifier, EchoNpcFactionDefinition> factions = Map.of();

    private EchoNpcFactionManager() {
    }

    public static void replace(Map<Identifier, EchoNpcFactionDefinition> loaded) {
        factions = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static Optional<EchoNpcFactionDefinition> get(Identifier id) {
        return Optional.ofNullable(factions.get(id));
    }

    public static int count() {
        return factions.size();
    }
}
