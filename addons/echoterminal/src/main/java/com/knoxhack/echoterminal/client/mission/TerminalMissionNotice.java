package com.knoxhack.echoterminal.client.mission;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record TerminalMissionNotice(
        TerminalMissionNoticeType type,
        Identifier chapterId,
        Identifier missionId,
        String chapterTitle,
        String title,
        String detail,
        String routeHint,
        String statusLabel,
        ItemStack icon,
        int accentColor,
        float progress,
        int count) {
    public TerminalMissionNotice {
        type = type == null ? TerminalMissionNoticeType.MISSION_AVAILABLE : type;
        chapterTitle = clean(chapterTitle, "ECHO Network");
        title = clean(title, "Mission signal updated");
        detail = clean(detail, "Open the ECHO terminal for the next step.");
        routeHint = clean(routeHint, "");
        statusLabel = clean(statusLabel, type.label());
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        count = Math.max(1, count);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
