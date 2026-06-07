package com.knoxhack.echothemecore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface EchoBlockPaletteProvider {
    List<Identifier> recommendedPanelBlocks(Player player);

    List<Identifier> glassBlocks(Player player);

    List<Identifier> lightBlocks(Player player);

    List<Identifier> accentBlocks(Player player);

    List<Identifier> decorativeBlocks(Player player);
}
