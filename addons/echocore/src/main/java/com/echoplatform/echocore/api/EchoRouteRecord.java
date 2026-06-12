package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoRouteRecord(
        Identifier id,
        String chapterId,
        String title,
        String category,
        String dimensionHint,
        String status,
        String summary,
        boolean complete) {
    public EchoRouteRecord {
        chapterId = chapterId == null ? "" : chapterId;
        title = title == null ? "" : title;
        category = category == null ? "" : category;
        dimensionHint = dimensionHint == null ? "" : dimensionHint;
        status = status == null ? "" : status;
        summary = summary == null ? "" : summary;
    }
}
