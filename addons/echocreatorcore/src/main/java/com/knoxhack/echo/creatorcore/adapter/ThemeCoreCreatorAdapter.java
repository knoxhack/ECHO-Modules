package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class ThemeCoreCreatorAdapter extends ModPresenceCreatorAdapter {
    public ThemeCoreCreatorAdapter() {
        super("themecore", "echothemecore", "ECHO: ThemeCore", null,
                Set.of("preview"),
                "ThemeCore not installed; CreatorCore uses fallback cyberglass colors.",
                "ThemeCore detected; dashboard uses fallback colors in 1.0.0 and reserves token binding for 0.2.0.",
                true);
    }
}
