package com.knoxhack.echo.npcore.service;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface NpcHoloMapBridge {
    default void revealSettlementMarker(ServerPlayer player, Identifier settlementId, BlockPos position) {
        if (player == null || settlementId == null) {
            return;
        }
        BlockPos pos = position == null ? player.blockPosition() : position;
        WorldMarker marker = new WorldMarker(settlementId, null, WorldMarkerType.OUTPOST,
                settlementId.getPath(), "NPCore contact marker.", player.level().dimension(), pos, 24, true,
                player.level().getGameTime());
        EchoCoreServices.worldRegions().revealMarker(player, marker);
        EchoCoreServices.refreshMapMarkers(player, "npcore_holomap_bridge");
    }
}
