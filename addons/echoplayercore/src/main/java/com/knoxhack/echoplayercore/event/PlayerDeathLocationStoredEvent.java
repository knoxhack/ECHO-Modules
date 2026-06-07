package com.knoxhack.echoplayercore.event;

import com.knoxhack.echoplayercore.data.TeleportLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlayerDeathLocationStoredEvent extends PlayerCoreEvent {
    private final ServerPlayer player;
    private final TeleportLocation location;

    public PlayerDeathLocationStoredEvent(ServerPlayer player, TeleportLocation location) {
        this.player = player;
        this.location = location;
    }

    public ServerPlayer player() {
        return player;
    }

    public TeleportLocation location() {
        return location;
    }
}
