package com.knoxhack.echoterminal.api.mission;

import com.knoxhack.echoterminal.api.TerminalApiIds;
import net.minecraft.resources.Identifier;

public record TerminalMissionChapter(
        Identifier id,
        String title,
        String summary,
        int order,
        int accentColor,
        boolean visible) {
    public TerminalMissionChapter {
        id = TerminalApiIds.requireLowercase(id, "Terminal mission chapter");
        title = title == null || title.isBlank() ? id.toString() : title;
        summary = summary == null ? "" : summary;
    }
}
