package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoMapLayer(
        Identifier id,
        String title,
        int sortOrder,
        int color,
        boolean visibleByDefault) implements IMapLayer {
    public EchoMapLayer {
        title = title == null ? "" : title;
    }
}
