package com.knoxhack.echotutorialcore.api.trigger;

import com.knoxhack.echotutorialcore.api.TutorialCategory;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;

public record TutorialFlow(
        Identifier id,
        String title,
        TutorialCategory category,
        List<TutorialStep> steps,
        List<Identifier> unlockCards,
        boolean defaultUnlocked) {

    public TutorialFlow {
        steps = steps == null ? List.of() : List.copyOf(steps);
        unlockCards = unlockCards == null ? List.of() : List.copyOf(unlockCards);
        category = category == null ? TutorialCategory.START_HERE : category;
    }
}
