package com.echoplatform.echocore.api;

import net.minecraft.world.entity.player.Player;

public interface EchoAddonChapter {
    String id();

    String modId();

    String displayName();

    String summary();

    default boolean isAvailable(Player player) {
        return true;
    }

    default String statusLine(Player player) {
        return summary();
    }
}
