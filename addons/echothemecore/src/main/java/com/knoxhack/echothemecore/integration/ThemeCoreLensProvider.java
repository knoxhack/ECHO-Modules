package com.knoxhack.echothemecore.integration;

import com.knoxhack.echothemecore.api.EchoLensThemeProvider;
import com.knoxhack.echothemecore.api.EchoThemeApi;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import net.minecraft.world.entity.player.Player;

public final class ThemeCoreLensProvider implements EchoLensThemeProvider {
    public static final ThemeCoreLensProvider INSTANCE = new ThemeCoreLensProvider();

    private ThemeCoreLensProvider() {
    }

    private EchoThemeColors colors(Player player) {
        return EchoThemeApi.getClientTheme().colors();
    }

    @Override
    public int scanOutlineColor(Player player) {
        return colors(player).primary();
    }

    @Override
    public int targetHighlightColor(Player player) {
        return colors(player).glow();
    }

    @Override
    public int weakPointColor(Player player) {
        return colors(player).accent();
    }

    @Override
    public int dangerWarningColor(Player player) {
        return colors(player).warning();
    }

    @Override
    public int anomalyColor(Player player) {
        return colors(player).secondary();
    }

    @Override
    public int scanPulseParticleColor(Player player) {
        return colors(player).primary();
    }

    @Override
    public float overlayOpacity(Player player) {
        return EchoThemeApi.getClientTheme().renderProfile().glassOpacity();
    }
}
