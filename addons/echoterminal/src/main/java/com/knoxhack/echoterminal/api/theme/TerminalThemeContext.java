package com.knoxhack.echoterminal.api.theme;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public record TerminalThemeContext(
        Identifier activeTabId,
        String navigationGroup,
        String chapterId,
        String chapterTitle,
        String namespace,
        int tick,
        boolean visualAssets,
        boolean reducedMotion) {
    public TerminalThemeContext {
        navigationGroup = clean(navigationGroup);
        chapterId = clean(chapterId);
        chapterTitle = chapterTitle == null ? "" : chapterTitle.strip();
        namespace = clean(namespace);
    }

    public static TerminalThemeContext empty() {
        return new TerminalThemeContext(null, "", "", "", "", 0, true, false);
    }

    public TerminalThemeContext withTick(int tick) {
        return new TerminalThemeContext(activeTabId, navigationGroup, chapterId, chapterTitle,
                namespace, tick, visualAssets, reducedMotion);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
