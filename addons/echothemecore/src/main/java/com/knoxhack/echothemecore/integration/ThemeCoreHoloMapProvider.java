package com.knoxhack.echothemecore.integration;

import com.knoxhack.echothemecore.api.EchoHoloMapThemeProvider;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeApi;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class ThemeCoreHoloMapProvider implements EchoHoloMapThemeProvider {
    public static final ThemeCoreHoloMapProvider INSTANCE = new ThemeCoreHoloMapProvider();

    private ThemeCoreHoloMapProvider() {
    }

    private EchoThemeColors colors(Player player) {
        return EchoThemeApi.getClientTheme().colors();
    }

    @Override
    public int gridColor(Player player) {
        return colors(player).primary();
    }

    @Override
    public int routeColor(Player player) {
        return colors(player).success();
    }

    @Override
    public int selectedMarkerColor(Player player) {
        return colors(player).selection();
    }

    @Override
    public int dangerMarkerColor(Player player) {
        return colors(player).error();
    }

    @Override
    public int anomalyMarkerColor(Player player) {
        return colors(player).accent();
    }

    @Override
    public int reclaimedMarkerColor(Player player) {
        return colors(player).success();
    }

    @Override
    public float hologramOpacity(Player player) {
        return EchoThemeApi.getClientTheme().renderProfile().hologramOpacity();
    }

    @Override
    public Optional<Identifier> energyOverlay(Player player) {
        EchoTheme theme = EchoThemeApi.getClientTheme();
        return theme.uiAssets().energyOverlay() == null ? Optional.empty()
            : Optional.of(theme.uiAssets().energyOverlay());
    }
}
