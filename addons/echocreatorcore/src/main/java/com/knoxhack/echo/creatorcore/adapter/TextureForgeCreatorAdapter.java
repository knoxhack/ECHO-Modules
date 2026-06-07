package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class TextureForgeCreatorAdapter extends ModPresenceCreatorAdapter {
    public TextureForgeCreatorAdapter() {
        super("textureforge", "echotextureforge", "ECHO: TextureForge", null,
                Set.of("preview"),
                "TextureForge not installed; asset preview/generation links are unavailable.",
                "TextureForge detected; asset preview/generation handoff is reserved for a future CreatorCore release.",
                true);
    }
}
