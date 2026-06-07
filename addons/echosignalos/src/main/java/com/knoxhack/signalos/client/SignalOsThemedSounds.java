package com.knoxhack.signalos.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

final class SignalOsThemedSounds {
    private SignalOsThemedSounds() {
    }

    static SoundEvent click() {
        Identifier themed = currentThemeSound("UI_CLICK");
        if (themed != null) {
            return BuiltInRegistries.SOUND_EVENT.getOptional(themed).orElse(SoundEvents.UI_BUTTON_CLICK.value());
        }
        Identifier soundCore = Identifier.fromNamespaceAndPath("echosoundcore", "ui.terminal.select");
        return BuiltInRegistries.SOUND_EVENT.getOptional(soundCore).orElse(SoundEvents.UI_BUTTON_CLICK.value());
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
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Object optional = api.getMethod("getSound", Player.class, soundKey).invoke(null, player, enumValue);
            return optional instanceof Optional<?> resolved && resolved.isPresent() && resolved.get() instanceof Identifier id
                    ? id
                    : null;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            return null;
        }
    }
}
