package com.knoxhack.echoplayercore.event;

import com.knoxhack.echoplayercore.data.TeleportLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlayerRandomTeleportEvent extends PlayerCoreEvent {
    private final ServerPlayer player;
    private final TeleportLocation target;

    public PlayerRandomTeleportEvent(ServerPlayer player, TeleportLocation target) {
        this.player = player;
        this.target = target;
    }

    public ServerPlayer player() {
        return player;
    }

    public TeleportLocation target() {
        return target;
    }
}
