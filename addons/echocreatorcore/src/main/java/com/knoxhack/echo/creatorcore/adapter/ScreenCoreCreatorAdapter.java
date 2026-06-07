package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class ScreenCoreCreatorAdapter extends ModPresenceCreatorAdapter {
    public ScreenCoreCreatorAdapter() {
        super("screencore", "echoscreencore", "ECHO: ScreenCore", null,
                Set.of("screen_provider", "preview"),
                "ScreenCore not installed; CreatorCore uses its vanilla cyberglass dashboard shell.",
                "ScreenCore detected; CreatorCore 0.2.0 can open a native EUI dashboard page with vanilla fallback.",
                true);
    }
}
