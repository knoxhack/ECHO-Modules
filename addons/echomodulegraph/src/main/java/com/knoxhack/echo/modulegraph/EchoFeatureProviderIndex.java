package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoFeatureProviderIndex(Map<EchoFeatureId, Set<EchoModuleId>> providersByFeature) {
    public EchoFeatureProviderIndex {
        Map<EchoFeatureId, Set<EchoModuleId>> safe = new HashMap<>();
        if (providersByFeature != null) {
            providersByFeature.forEach((feature, modules) -> safe.put(feature, Set.copyOf(modules)));
        }
        providersByFeature = Map.copyOf(safe);
    }

    public Set<EchoModuleId> providersFor(EchoFeatureId featureId) {
        return providersByFeature.getOrDefault(featureId, Set.of());
    }

    public boolean hasProvider(EchoFeatureId featureId) {
        return !providersFor(featureId).isEmpty();
    }

    public static EchoFeatureProviderIndex fromModules(Collection<EchoScannedModule> modules) {
        Map<EchoFeatureId, Set<EchoModuleId>> index = new HashMap<>();
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        for (EchoScannedModule module : safeModules) {
            for (EchoFeatureId feature : module.providedFeatures()) {
                index.computeIfAbsent(feature, ignored -> new HashSet<>()).add(module.moduleId());
            }
        }
        return new EchoFeatureProviderIndex(index);
    }
}
