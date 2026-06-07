package com.knoxhack.echothemecore.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface EchoRenderThemeProvider {
    Identifier getThemeId(Player player);

    EchoThemeRenderProfile getRenderProfile(Player player);

    int resolveColor(Player player, EchoThemeRenderColorKey key);

    float resolveIntensity(Player player, EchoThemeRenderIntensityKey key);
}
