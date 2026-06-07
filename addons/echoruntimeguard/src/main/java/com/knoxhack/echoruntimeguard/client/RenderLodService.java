package com.knoxhack.echoruntimeguard.client;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.RenderQuality;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import com.knoxhack.echoruntimeguard.runtime.RuntimeModeService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class RenderLodService {
    public static final RenderLodService INSTANCE = new RenderLodService();

    private RenderLodService() {
    }

    public RenderQuality getHologramQuality(double distance) {
        if (!RuntimeGuardConfig.safeBool(RuntimeGuardConfig.HOLOGRAM_LOD_ENABLED, true)) {
            return RenderQuality.FULL;
        }
        return qualityForDistance(distance, 48.0D, 128.0D);
    }

    public RenderQuality getRoboticAnimationQuality(double distance) {
        if (!RuntimeGuardConfig.safeBool(RuntimeGuardConfig.ROBOTIC_ANIMATION_LOD_ENABLED, true)) {
            return RenderQuality.FULL;
        }
        return qualityForDistance(distance, 32.0D, 96.0D);
    }

    public boolean shouldRenderDecorativeOverlay(BlockPos pos) {
        return RuntimeModeService.INSTANCE.mode() != RuntimeMode.EMERGENCY
                && RuntimeGuardThemeCoreStyle.particlesEnabled(true);
    }

    public boolean shouldRenderFarGlow(BlockPos pos) {
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        return (mode == RuntimeMode.CINEMATIC || mode == RuntimeMode.DEBUG)
                && RuntimeGuardThemeCoreStyle.edgeGlowEnabled(true);
    }

    public boolean shouldAnimateTerminal(BlockPos pos) {
        if (!RuntimeGuardConfig.safeBool(RuntimeGuardConfig.TERMINAL_ANIMATION_THROTTLE, true)) {
            return true;
        }
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        return mode != RuntimeMode.EMERGENCY
                && mode != RuntimeMode.POTATO
                && mode != RuntimeMode.SERVER
                && RuntimeGuardThemeCoreStyle.animationIntensity(1.0F) > 0.01F;
    }

    public boolean shouldAnimateHoloMapMarker(Identifier markerId, double distance) {
        return qualityForDistance(distance, 48.0D, 128.0D) != RenderQuality.OFF;
    }

    private static RenderQuality qualityForDistance(double distance, double near, double far) {
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.EMERGENCY) {
            return distance <= near ? RenderQuality.SIMPLE : RenderQuality.OFF;
        }
        if (mode == RuntimeMode.POTATO || mode == RuntimeMode.SERVER) {
            return distance <= near ? RenderQuality.REDUCED : RenderQuality.OFF;
        }
        if (mode == RuntimeMode.CINEMATIC) {
            return distance <= far * 1.5D ? RenderQuality.FULL : RenderQuality.REDUCED;
        }
        if (distance <= near) {
            return RenderQuality.FULL;
        }
        if (distance <= far) {
            return RenderQuality.REDUCED;
        }
        return RenderQuality.SIMPLE;
    }
}
