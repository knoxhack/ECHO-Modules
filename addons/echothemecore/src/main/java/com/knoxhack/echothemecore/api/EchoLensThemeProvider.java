package com.knoxhack.echothemecore.api;

import net.minecraft.world.entity.player.Player;

public interface EchoLensThemeProvider {
    int scanOutlineColor(Player player);

    int targetHighlightColor(Player player);

    int weakPointColor(Player player);

    int dangerWarningColor(Player player);

    int anomalyColor(Player player);

    int scanPulseParticleColor(Player player);

    float overlayOpacity(Player player);
}
