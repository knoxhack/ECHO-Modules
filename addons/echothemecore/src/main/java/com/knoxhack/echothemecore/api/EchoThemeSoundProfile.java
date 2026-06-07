package com.knoxhack.echothemecore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoThemeSoundProfile(
    Identifier uiClick,
    Identifier uiError,
    Identifier uiOpen,
    Identifier uiClose,
    Identifier themeMusic,
    Identifier stingerConfirm,
    Identifier stingerWarning
) {
    public Optional<Identifier> sound(EchoThemeSoundKey key) {
        return Optional.ofNullable(switch (key) {
            case UI_CLICK -> uiClick;
            case UI_ERROR -> uiError;
            case UI_OPEN -> uiOpen;
            case UI_CLOSE -> uiClose;
            case THEME_MUSIC -> themeMusic;
            case STINGER_CONFIRM -> stingerConfirm;
            case STINGER_WARNING -> stingerWarning;
        });
    }
}
