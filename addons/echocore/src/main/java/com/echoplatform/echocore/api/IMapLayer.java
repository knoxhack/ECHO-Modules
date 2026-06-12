package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public interface IMapLayer {
    Identifier id();

    String title();

    int sortOrder();

    int color();

    boolean visibleByDefault();
}
