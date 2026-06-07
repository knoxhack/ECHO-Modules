package com.knoxhack.echoterminal.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface TerminalContactProvider {
    Identifier providerId();

    List<TerminalContact> contacts(Player player);
}
