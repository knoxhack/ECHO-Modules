package com.knoxhack.echothemecore.integration;

import com.knoxhack.echothemecore.api.EchoRuntimeGuardThemeProvider;
import com.knoxhack.echothemecore.api.EchoThemeApi;
import net.minecraft.world.entity.player.Player;

public final class ThemeCoreRuntimeGuardProvider implements EchoRuntimeGuardThemeProvider {
    public static final ThemeCoreRuntimeGuardProvider INSTANCE = new ThemeCoreRuntimeGuardProvider();

    private ThemeCoreRuntimeGuardProvider() {
    }

    @Override
    public float glowCostLevel(Player player) {
        return EchoThemeApi.getClientTheme().renderProfile().glowIntensity();
    }

    @Override
    public boolean distortionEnabled(Player player) {
        return EchoThemeApi.getClientTheme().renderProfile().distortionStrength() > 0.0F;
    }

    @Override
    public float particleIntensity(Player player) {
        return EchoThemeApi.getClientTheme().renderProfile().particleIntensity();
    }

    @Override
    public float animationIntensity(Player player) {
        return EchoThemeApi.getClientTheme().renderProfile().animationIntensity();
    }
}
