package com.knoxhack.echothemecore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface EchoThemedUiProvider {
    int getPanelColor(Player player);

    int getTextColor(Player player);

    int getAccentColor(Player player);

    int getWarningColor(Player player);

    int getErrorColor(Player player);

    Optional<Identifier> getBackgroundTexture(Player player);

    Optional<Identifier> getButtonTexture(Player player);
}
