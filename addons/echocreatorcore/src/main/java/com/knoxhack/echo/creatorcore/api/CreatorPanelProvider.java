package com.knoxhack.echo.creatorcore.api;

import net.minecraft.resources.Identifier;

public interface CreatorPanelProvider {
    Identifier id();

    String title();

    default String summary() {
        return "";
    }
}
