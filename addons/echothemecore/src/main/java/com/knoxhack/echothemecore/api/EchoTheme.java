package com.knoxhack.echothemecore.api;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoTheme(
    Identifier id,
    String displayName,
    String description,
    EchoThemeColors colors,
    EchoThemeUiAssets uiAssets,
    EchoThemeRenderProfile renderProfile,
    EchoThemeSoundProfile soundProfile,
    EchoThemeBlockPalette blockPalette,
    EchoThemeVanillaUiProfile vanillaUiProfile,
    Map<EchoThemeTextureKey, Identifier> moduleTextures,
    Map<String, String> metadata
) {
    public EchoTheme {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        moduleTextures = moduleTextures == null || moduleTextures.isEmpty()
            ? Map.of()
            : Collections.unmodifiableMap(new EnumMap<>(moduleTextures));
        if (vanillaUiProfile == null) {
            vanillaUiProfile = EchoThemeVanillaUiProfile.fromParts(colors, uiAssets, renderProfile);
        }
    }

    public Optional<Identifier> moduleTexture(EchoThemeTextureKey key) {
        return Optional.ofNullable(moduleTextures.get(key));
    }
}
