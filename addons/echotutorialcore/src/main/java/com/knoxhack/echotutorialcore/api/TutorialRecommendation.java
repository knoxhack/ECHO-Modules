package com.knoxhack.echotutorialcore.api;

import net.minecraft.resources.Identifier;

public record TutorialRecommendation(
        Identifier id,
        String title,
        String detail,
        Identifier cardId,
        int priority) {
    public TutorialRecommendation {
        title = title == null ? "" : title.strip();
        detail = detail == null ? "" : detail.strip();
        priority = Math.max(0, priority);
    }
}
