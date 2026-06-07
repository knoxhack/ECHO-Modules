package com.knoxhack.echotutorialcore.api;

import net.minecraft.resources.Identifier;

public record TutorialRequirement(
        Identifier id,
        TutorialConditionType type,
        Identifier target,
        int count,
        String label,
        Identifier helpCardId) {
    public TutorialRequirement {
        type = type == null ? TutorialConditionType.PROGRESS : type;
        count = Math.max(1, count);
        label = label == null ? "" : label.strip();
    }
}
