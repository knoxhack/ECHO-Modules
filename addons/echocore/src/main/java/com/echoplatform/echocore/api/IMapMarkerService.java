package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IMapMarkerService {
    default boolean registerProvider(IMapDataProvider provider) {
        return false;
    }

    default List<IMapLayer> layers(Player player) {
        return List.of();
    }

    default List<IMapMarker> markers(Player player) {
        return List.of();
    }

    default boolean refresh(ServerPlayer player, String reason) {
        return false;
    }

    default int providerCount() {
        return 0;
    }
}
