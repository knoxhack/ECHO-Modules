package com.knoxhack.echoplayercore.event;

import net.minecraft.server.level.ServerPlayer;

public class PlayerHomeDeletedEvent extends PlayerCoreEvent {
    private final ServerPlayer player;
    private final String homeName;

    public PlayerHomeDeletedEvent(ServerPlayer player, String homeName) {
        this.player = player;
        this.homeName = homeName;
    }

    public ServerPlayer player() {
        return player;
    }

    public String homeName() {
        return homeName;
    }
}
