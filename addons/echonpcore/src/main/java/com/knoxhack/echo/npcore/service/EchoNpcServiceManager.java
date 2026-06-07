package com.knoxhack.echo.npcore.service;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcServiceManager {
    private static volatile Map<Identifier, EchoNpcServiceSet> services = Map.of();

    private EchoNpcServiceManager() {
    }

    public static void replace(Map<Identifier, EchoNpcServiceSet> loaded) {
        services = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static Optional<EchoNpcServiceSet> get(Identifier id) {
        return Optional.ofNullable(services.get(id));
    }

    public static EchoNpcServiceSet getOrEmpty(Identifier id) {
        EchoNpcServiceSet set = services.get(id);
        return set == null ? new EchoNpcServiceSet(id, java.util.List.of()) : set;
    }

    public static int count() {
        return services.size();
    }
}
