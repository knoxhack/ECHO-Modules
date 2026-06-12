package com.echoplatform.echocore.api.mission;

import net.minecraft.resources.Identifier;

public record MissionChapterDefinition(
        Identifier id,
        String title,
        String summary,
        int order,
        int color) {
    public int accentColor() {
        return color;
    }
}
