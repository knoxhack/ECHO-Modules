package com.knoxhack.echoprimecore.service;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echoprimecore.integration.PrimeIntegrationLoader;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class PrimeRouteService {
    private PrimeRouteService() {
    }

    public static List<RouteReadiness> routeReadiness() {
        return PrimeIntegrationLoader.registry().routes().stream()
                .map(PrimeRouteService::readiness)
                .toList();
    }

    public static RouteReadiness readiness(PrimeRouteRegistry.PrimeRoute route) {
        List<String> missing = route.requiredModules().stream()
                .filter(module -> !EchoRuntimeModules.isLoaded(module))
                .toList();
        return new RouteReadiness(
                route.id(),
                route.title(),
                missing.isEmpty(),
                route.requiredModules().size() - missing.size(),
                route.requiredModules().size(),
                missing,
                route.unlockFlag());
    }

    public static boolean isReady(PrimeRouteRegistry.PrimeRoute route) {
        return readiness(route).ready();
    }

    public record RouteReadiness(
            Identifier routeId,
            String title,
            boolean ready,
            int loadedModules,
            int requiredModules,
            List<String> missingModules,
            Identifier unlockFlag) {
        public RouteReadiness {
            missingModules = missingModules == null ? List.of() : List.copyOf(missingModules);
        }
    }
}
