package com.knoxhack.echoplayercore.event;

import com.knoxhack.echoplayercore.data.WarpLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlayerWarpTeleportEvent extends PlayerCoreEvent {
    private final ServerPlayer player;
    private final WarpLocation warp;

    public PlayerWarpTeleportEvent(ServerPlayer player, WarpLocation warp) {
        this.player = player;
        this.warp = warp;
    }

    public ServerPlayer player() {
        return player;
    }

    public WarpLocation warp() {
        return warp;
    }
}
