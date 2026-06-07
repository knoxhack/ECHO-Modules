package com.knoxhack.echonetcore.api;

import net.minecraft.world.entity.player.Player;

public interface EchoPayloadContext {
    EchoPayloadContext NONE = new EchoPayloadContext() {
        @Override
        public Player player() {
            return null;
        }
    };

    Player player();

    default void enqueueWork(Runnable work) {
        if (work != null) {
            work.run();
        }
    }

    static EchoPayloadContext immediate(Player player) {
        return new EchoPayloadContext() {
            @Override
            public Player player() {
                return player;
            }
        };
    }
}
