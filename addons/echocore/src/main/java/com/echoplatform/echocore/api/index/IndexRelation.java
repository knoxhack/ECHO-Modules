package com.echoplatform.echocore.api.index;

import net.minecraft.resources.Identifier;

public record IndexRelation(
        Identifier id,
        Identifier from,
        Identifier to,
        String kind,
        String title,
        IndexVisibility visibility,
        String sourceModId) {
    public IndexRelation {
        kind = kind == null ? "" : kind;
        title = title == null ? "" : title;
        visibility = visibility == null ? IndexVisibility.VISIBLE : visibility;
        sourceModId = sourceModId == null ? "" : sourceModId;
    }

    public Identifier fromId() {
        return from;
    }

    public Identifier toId() {
        return to;
    }
}
