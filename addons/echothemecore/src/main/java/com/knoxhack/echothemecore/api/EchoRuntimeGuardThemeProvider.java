package com.knoxhack.echothemecore.api;

import net.minecraft.world.entity.player.Player;

public interface EchoRuntimeGuardThemeProvider {
    float glowCostLevel(Player player);

    boolean distortionEnabled(Player player);

    float particleIntensity(Player player);

    float animationIntensity(Player player);
}
