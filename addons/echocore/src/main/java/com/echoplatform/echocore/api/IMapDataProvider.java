package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface IMapDataProvider {
    Identifier providerId();

    default List<IMapLayer> layers(Player player) {
        return List.of();
    }

    default List<IMapMarker> markers(Player player) {
        return List.of();
    }

    default boolean refresh(net.minecraft.server.level.ServerPlayer player, String reason) {
        return false;
    }
}
