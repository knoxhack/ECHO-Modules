package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echoorbitalremnants.item.EchoTerminalItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

final class OrbitalTerminalActions {
    static final String MISSING_TERMINAL_REASON = "Carry the ECHO-7 Terminal to authorize Orbital scans.";

    private OrbitalTerminalActions() {
    }

    static boolean canScan(Player player) {
        return player != null && EchoTerminalItem.hasTerminal(player);
    }

    static boolean scan(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!canScan(player)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] " + MISSING_TERMINAL_REASON), true);
            return true;
        }
        EchoTerminalItem.performScan(player);
        return true;
    }
}
