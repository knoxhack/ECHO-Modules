package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface MultiblockMapMarkerProvider {
    default Identifier providerId() {
        return MultiblockIntegrationServices.generatedProviderId(this, "map_marker");
    }

    List<MultiblockMapMarkerSnapshot> markers(Player player);

    default boolean refresh(ServerPlayer player, String reason) {
        return false;
    }
}
