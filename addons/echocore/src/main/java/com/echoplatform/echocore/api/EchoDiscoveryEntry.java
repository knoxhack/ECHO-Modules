package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoDiscoveryEntry(
        Identifier id,
        Identifier chapterId,
        EchoDiscoveryCategory category,
        String title,
        String lockedTitle,
        String lockedSummary,
        String summary,
        Identifier icon,
        Identifier heroArt,
        int accentColor,
        Identifier target,
        int sortOrder) {
    public EchoDiscoveryEntry {
        category = category == null ? EchoDiscoveryCategory.STRUCTURE : category;
        title = title == null ? "" : title;
        lockedTitle = lockedTitle == null ? "" : lockedTitle;
        lockedSummary = lockedSummary == null ? "" : lockedSummary;
        summary = summary == null ? "" : summary;
    }

    public Identifier iconArt() {
        return icon;
    }

    public String revealedTitle() {
        return title;
    }

    public String lockedHintTitle() {
        return lockedTitle;
    }

    public String revealedSummary() {
        return summary;
    }

    public String hintText() {
        return lockedSummary;
    }

    public Identifier relatedMissionId() {
        return target;
    }
}
