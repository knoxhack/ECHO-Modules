package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public interface IThemeService {
    default boolean available() {
        return false;
    }

    default int resolveColor(String token, int fallback) {
        return EchoThemeToken.resolveDefault(token, fallback);
    }

    default Optional<Identifier> resolveColorId(String token) {
        return Optional.empty();
    }

    default Optional<Identifier> resolveTexture(String token) {
        return Optional.empty();
    }

    default float resolveFloat(String token, float fallback) {
        return fallback;
    }

    default List<String> knownTokens() {
        return EchoThemeToken.defaultDarkColorTokens();
    }

    default String currentThemeName() {
        return "ECHO Core fallback";
    }
}
