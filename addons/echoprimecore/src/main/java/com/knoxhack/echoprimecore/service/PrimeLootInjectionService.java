package com.knoxhack.echoprimecore.service;

import com.echoplatform.echocore.api.prime.PrimeLootRegistry;
import com.knoxhack.echoprimecore.integration.PrimeIntegrationLoader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

public final class PrimeLootInjectionService {
    private PrimeLootInjectionService() {
    }

    public static List<PrimeLootRegistry.PrimeLootPool> pools() {
        return PrimeIntegrationLoader.registry().pools();
    }

    public static List<PrimeLootRegistry.PrimeLootInjection> injections() {
        return PrimeIntegrationLoader.registry().injections();
    }

    public static List<PrimeLootRegistry.PrimeLootInjection> injectionsForPool(Identifier poolId) {
        return injections().stream()
                .filter(injection -> injection.poolId().equals(poolId))
                .toList();
    }

    public static Map<Identifier, List<PrimeLootRegistry.PrimeLootInjection>> injectionsByPool() {
        return injections().stream()
                .collect(Collectors.groupingBy(PrimeLootRegistry.PrimeLootInjection::poolId));
    }
}
