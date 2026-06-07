package com.knoxhack.echoterminal.api;

import java.util.List;
import net.minecraft.world.entity.player.Player;

/**
 * Deferred beta API. Notifications are not rendered by the public terminal chrome yet.
 */
@FunctionalInterface
@Deprecated(forRemoval = false)
public interface TerminalNotificationProvider {
    List<String> notifications(Player player);
}
