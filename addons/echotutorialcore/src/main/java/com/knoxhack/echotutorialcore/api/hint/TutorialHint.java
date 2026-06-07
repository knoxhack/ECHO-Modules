package com.knoxhack.echotutorialcore.api.hint;

import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.TutorialHintType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record TutorialHint(
        Identifier id,
        TutorialHintType type,
        TutorialCategory category,
        String title,
        String message,
        String details,
        String actionLabel,
        Identifier actionCardId,
        int cooldownTicks,
        Set<TutorialGuideMode> guideModes,
        int priority,
        boolean dismissible,
        List<String> conditions) {

    public TutorialHint {
        type = type == null ? TutorialHintType.INFO : type;
        category = category == null ? TutorialCategory.START_HERE : category;
        title = title == null ? "" : title;
        message = message == null ? "" : message;
        details = details == null ? "" : details;
        actionLabel = actionLabel == null ? "" : actionLabel;
        cooldownTicks = Math.max(0, cooldownTicks);
        guideModes = guideModes == null ? EnumSet.allOf(TutorialGuideMode.class) : EnumSet.copyOf(guideModes);
        priority = Math.max(0, priority);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public boolean allowedInMode(TutorialGuideMode mode) {
        return guideModes.contains(mode);
    }
}
