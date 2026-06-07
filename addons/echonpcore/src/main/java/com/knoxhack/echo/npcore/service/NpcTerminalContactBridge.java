package com.knoxhack.echo.npcore.service;

import com.knoxhack.echo.npcore.data.NpcContactData;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface NpcTerminalContactBridge {
    default void discoverContact(ServerPlayer player, Identifier npcProfileId) {
        if (player != null && npcProfileId != null) {
            NpcContactData.discover(player, EchoNpcProfileManager.getOrFallback(npcProfileId), player.level().getGameTime());
        }
    }
}
