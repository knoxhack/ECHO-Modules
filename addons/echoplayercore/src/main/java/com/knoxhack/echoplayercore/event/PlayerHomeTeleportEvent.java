package com.knoxhack.echoplayercore.event;

import com.knoxhack.echoplayercore.data.HomeLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlayerHomeTeleportEvent extends PlayerCoreEvent {
    private final ServerPlayer player;
    private final HomeLocation home;

    public PlayerHomeTeleportEvent(ServerPlayer player, HomeLocation home) {
        this.player = player;
        this.home = home;
    }

    public ServerPlayer player() {
        return player;
    }

    public HomeLocation home() {
        return home;
    }
}
