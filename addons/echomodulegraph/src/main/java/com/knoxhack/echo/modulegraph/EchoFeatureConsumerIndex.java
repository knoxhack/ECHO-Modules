package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoFeatureConsumerIndex(
        Map<EchoFeatureId, Set<EchoModuleId>> consumersByFeature,
        Map<EchoFeatureId, Set<EchoModuleId>> requiredConsumersByFeature
) {
    public EchoFeatureConsumerIndex {
        consumersByFeature = immutableNested(consumersByFeature);
        requiredConsumersByFeature = immutableNested(requiredConsumersByFeature);
    }

    public Set<EchoModuleId> consumersFor(EchoFeatureId featureId) {
        return consumersByFeature.getOrDefault(featureId, Set.of());
    }

    public Set<EchoModuleId> requiredConsumersFor(EchoFeatureId featureId) {
        return requiredConsumersByFeature.getOrDefault(featureId, Set.of());
    }

    public Set<EchoModuleId> optionalConsumersFor(EchoFeatureId featureId) {
        Set<EchoModuleId> consumers = consumersFor(featureId);
        Set<EchoModuleId> required = requiredConsumersFor(featureId);
        if (consumers.isEmpty()) {
            return Set.of();
        }
        Set<EchoModuleId> optional = new HashSet<>(consumers);
        optional.removeAll(required);
        return Set.copyOf(optional);
    }

    public static EchoFeatureConsumerIndex fromModules(Collection<EchoScannedModule> modules) {
        Map<EchoFeatureId, Set<EchoModuleId>> consumers = new HashMap<>();
        Map<EchoFeatureId, Set<EchoModuleId>> required = new HashMap<>();
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        for (EchoScannedModule module : safeModules) {
            for (EchoFeatureRequirement requirement : module.consumedFeatures()) {
                consumers.computeIfAbsent(requirement.featureId(), ignored -> new HashSet<>()).add(module.moduleId());
                if (requirement.required()) {
                    required.computeIfAbsent(requirement.featureId(), ignored -> new HashSet<>()).add(module.moduleId());
                }
            }
        }
        return new EchoFeatureConsumerIndex(consumers, required);
    }

    private static Map<EchoFeatureId, Set<EchoModuleId>> immutableNested(Map<EchoFeatureId, Set<EchoModuleId>> values) {
        Map<EchoFeatureId, Set<EchoModuleId>> safe = new HashMap<>();
        if (values != null) {
            values.forEach((feature, modules) -> safe.put(feature, Set.copyOf(modules)));
        }
        return Map.copyOf(safe);
    }
}
