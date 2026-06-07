package com.knoxhack.echoholomap.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IHoloMapDataProvider {
    Identifier providerId();

    default List<HoloMapLayerData> layers(Player player) {
        return List.of();
    }

    default List<HoloMapMarkerData> markers(Player player) {
        return List.of();
    }

    default List<HoloMapMarkerData> markers(Player player, HoloMapQuery query) {
        return markers(player);
    }

    default List<HoloMapRouteData> routes(Player player) {
        return List.of();
    }

    default List<HoloMapRouteData> routes(Player player, HoloMapQuery query) {
        return routes(player);
    }

    default List<HoloMapOverlayData> overlays(Player player) {
        return List.of();
    }

    default List<HoloMapOverlayData> overlays(Player player, HoloMapQuery query) {
        return overlays(player);
    }

    default List<HoloMapZoneData> zones(Player player) {
        return List.of();
    }

    default List<HoloMapZoneData> zones(Player player, HoloMapQuery query) {
        return zones(player);
    }

    default boolean refresh(ServerPlayer player, String reason) {
        return false;
    }
}
