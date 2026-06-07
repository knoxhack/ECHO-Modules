package com.knoxhack.echoterminal.api;

import net.minecraft.world.entity.player.Player;

/**
 * Deferred beta API. Badges are not rendered by the public terminal chrome yet.
 */
@FunctionalInterface
@Deprecated(forRemoval = false)
public interface TerminalBadgeProvider {
    String badgeText(Player player);
}
