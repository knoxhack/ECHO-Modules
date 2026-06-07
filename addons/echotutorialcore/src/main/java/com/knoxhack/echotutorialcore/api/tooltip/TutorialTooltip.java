package com.knoxhack.echotutorialcore.api.tooltip;

import java.util.List;
import net.minecraft.resources.Identifier;

public record TutorialTooltip(
        Identifier targetItem,
        List<String> lines,
        boolean requireShift,
        int priority) {

    public TutorialTooltip {
        lines = lines == null ? List.of() : List.copyOf(lines);
        requireShift = requireShift;
        priority = Math.max(0, priority);
    }
}
