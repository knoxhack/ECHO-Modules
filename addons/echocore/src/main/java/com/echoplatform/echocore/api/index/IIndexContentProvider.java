package com.echoplatform.echocore.api.index;

import net.minecraft.resources.Identifier;

public interface IIndexContentProvider {
    Identifier id();

    IndexContentSnapshot snapshot(IndexBuildContext context);
}
