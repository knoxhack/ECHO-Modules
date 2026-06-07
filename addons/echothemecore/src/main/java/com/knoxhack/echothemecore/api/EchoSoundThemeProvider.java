package com.knoxhack.echothemecore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface EchoSoundThemeProvider {
    Optional<Identifier> sound(Player player, EchoThemeSoundKey key);
}
