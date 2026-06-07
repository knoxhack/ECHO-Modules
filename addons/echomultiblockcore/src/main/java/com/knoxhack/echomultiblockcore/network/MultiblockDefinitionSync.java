package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.server.level.ServerPlayer;

public final class MultiblockDefinitionSync {
    private MultiblockDefinitionSync() {
    }

    public static void sendTo(ServerPlayer player) {
        EchoNetSend.toPlayer(player, MultiblockDefinitionMetadataPacket.current());
        EchoNetSend.toPlayer(player, AutomationRecipeMetadataPacket.current());
        EchoNetSend.toPlayer(player, MultiblockBuildAssistPacket.current());
    }
}
