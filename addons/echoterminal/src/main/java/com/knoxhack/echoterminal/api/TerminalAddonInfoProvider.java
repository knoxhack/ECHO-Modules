package com.knoxhack.echoterminal.api;

import net.minecraft.world.entity.player.Player;

public interface TerminalAddonInfoProvider {
    String chapterId();

    TerminalAddonInfo info(Player player);
}
