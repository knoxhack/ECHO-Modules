package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public record EchoExclusiveFeaturePolicy(
        Set<EchoFeatureId> exclusiveFeatures,
        Set<String> blockedTrustLevels
) implements EchoFeaturePolicy {
    public EchoExclusiveFeaturePolicy {
        exclusiveFeatures = ModuleGraphContractGuards.immutableSet(exclusiveFeatures);
        blockedTrustLevels = ModuleGraphContractGuards.immutableSet(blockedTrustLevels);
    }

    public static EchoExclusiveFeaturePolicy defaults() {
        return new EchoExclusiveFeaturePolicy(Set.of(), Set.of("blocked", "untrusted"));
    }

    @Override
    public boolean exclusive(EchoFeatureId featureId) {
        return featureId != null && exclusiveFeatures.contains(featureId);
    }

    @Override
    public boolean trustBlocked(String trustLevel, boolean officialPack) {
        if (!officialPack) {
            return false;
        }
        String normalized = trustLevel == null ? "" : trustLevel.trim().toLowerCase(java.util.Locale.ROOT);
        return blockedTrustLevels.contains(normalized);
    }
}
