package com.knoxhack.echorendercore.profile;

import net.minecraft.resources.Identifier;

public record ProfilePerformanceSummary(
        Identifier profileId,
        int layerCount,
        int maskedLayerCount,
        int animationClipCount,
        int animationTrackCount,
        int emitterCount,
        int estimatedMaxEmitterBurst,
        float estimatedEmitterRate,
        int activeEffectCount,
        int estimatedEffectCost,
        int estimatedBloomCost,
        int advancedEffectPassCount,
        String primaryEffectTargetScope,
        int estimatedMaskSubmissions,
        int estimatedBloomChannelCount,
        int estimatedBloomDownscale,
        int estimatedPrioritySkips,
        String advancedFxMode,
        boolean isolatedFxAvailable,
        boolean fullscreenFallbackAvailable
) {
}
