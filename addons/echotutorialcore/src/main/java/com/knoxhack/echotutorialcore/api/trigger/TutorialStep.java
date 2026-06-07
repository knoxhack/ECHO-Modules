package com.knoxhack.echotutorialcore.api.trigger;

import net.minecraft.resources.Identifier;

public record TutorialStep(
        String id,
        TutorialTriggerType type,
        Identifier target,
        String text,
        boolean optional) {

    public TutorialStep {
        type = type == null ? TutorialTriggerType.CUSTOM : type;
        text = text == null ? "" : text;
    }
}
