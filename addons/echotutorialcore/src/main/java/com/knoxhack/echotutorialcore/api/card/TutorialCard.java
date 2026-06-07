package com.knoxhack.echotutorialcore.api.card;

import com.knoxhack.echotutorialcore.api.TutorialCategory;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record TutorialCard(
        Identifier id,
        TutorialCategory category,
        String title,
        String summary,
        List<String> body,
        List<String> steps,
        List<String> commonMistakes,
        List<Identifier> related,
        List<String> unlockTriggers,
        boolean defaultUnlocked,
        String addonOwnerId,
        int priority) {

    public TutorialCard {
        body = body == null ? List.of() : List.copyOf(body);
        steps = steps == null ? List.of() : List.copyOf(steps);
        commonMistakes = commonMistakes == null ? List.of() : List.copyOf(commonMistakes);
        related = related == null ? List.of() : List.copyOf(related);
        unlockTriggers = unlockTriggers == null ? List.of() : List.copyOf(unlockTriggers);
        addonOwnerId = addonOwnerId == null ? id.getNamespace() : addonOwnerId;
        category = category == null ? TutorialCategory.START_HERE : category;
    }

    public static TutorialCard empty(Identifier id) {
        return new TutorialCard(id, TutorialCategory.START_HERE, id.toString(), "", List.of(), List.of(), List.of(), List.of(), List.of(), false, id.getNamespace(), 0);
    }
}
