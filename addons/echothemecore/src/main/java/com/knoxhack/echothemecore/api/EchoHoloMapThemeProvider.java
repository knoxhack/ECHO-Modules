package com.knoxhack.echothemecore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface EchoHoloMapThemeProvider {
    int gridColor(Player player);

    int routeColor(Player player);

    int selectedMarkerColor(Player player);

    int dangerMarkerColor(Player player);

    int anomalyMarkerColor(Player player);

    int reclaimedMarkerColor(Player player);

    float hologramOpacity(Player player);

    Optional<Identifier> energyOverlay(Player player);
}
