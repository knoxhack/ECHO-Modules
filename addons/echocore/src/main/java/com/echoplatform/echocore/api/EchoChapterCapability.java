package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoChapterCapability(
        Identifier id,
        String displayName,
        boolean installed,
        boolean available,
        String statusLine) {
    public EchoChapterCapability {
        displayName = displayName == null ? "" : displayName;
        statusLine = statusLine == null ? "" : statusLine;
    }
}
