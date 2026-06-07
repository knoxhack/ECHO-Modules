package com.knoxhack.echoterminal.api;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public final class TerminalThemedSounds {
    private TerminalThemedSounds() {
    }

    public static SoundEvent click() {
        return sound("UI_CLICK", Identifier.fromNamespaceAndPath("echosoundcore", "ui.terminal.select"),
                SoundEvents.UI_BUTTON_CLICK.value());
    }

    public static SoundEvent error() {
        return sound("UI_ERROR", Identifier.fromNamespaceAndPath("echosoundcore", "ui.terminal.error"),
                SoundEvents.NOTE_BLOCK_BASS.value());
    }

    private static SoundEvent sound(String themeKey, Identifier soundCoreFallback, SoundEvent vanillaFallback) {
        Identifier themed = currentThemeSound(themeKey);
        if (themed != null) {
            Optional<SoundEvent> event = BuiltInRegistries.SOUND_EVENT.getOptional(themed);
            if (event.isPresent()) {
                return event.get();
            }
        }
        return BuiltInRegistries.SOUND_EVENT.getOptional(soundCoreFallback).orElse(vanillaFallback);
    }

    private static Identifier currentThemeSound(String key) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return null;
            }
            Class<?> soundKey = Class.forName("com.knoxhack.echothemecore.api.EchoThemeSoundKey");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object enumValue = Enum.valueOf((Class) soundKey.asSubclass(Enum.class), key);
            Object optional = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi")
                    .getMethod("getSound", Player.class, soundKey)
                    .invoke(null, player, enumValue);
            return optional instanceof Optional<?> resolved && resolved.isPresent() && resolved.get() instanceof Identifier id
                    ? id
                    : null;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            return null;
        }
    }
}
