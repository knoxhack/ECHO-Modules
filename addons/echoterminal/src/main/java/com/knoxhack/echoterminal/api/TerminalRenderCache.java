package com.knoxhack.echoterminal.api;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Small per-frame cache for repeated terminal text measurements.
 */
public final class TerminalRenderCache {
    private static long nextFrameId;
    private static final ThreadLocal<TerminalRenderCache> CURRENT =
            ThreadLocal.withInitial(TerminalRenderCache::new);

    private final Map<TextKey, String> trimCache = new HashMap<>();
    private final Map<TextKey, Integer> wrappedHeightCache = new HashMap<>();
    private long frameId;

    private TerminalRenderCache() {
    }

    public static void beginFrame() {
        TerminalRenderCache cache = CURRENT.get();
        cache.clear();
        cache.frameId = ++nextFrameId;
    }

    public static TerminalRenderCache current() {
        return CURRENT.get();
    }

    public String trim(Font font, String text, int maxWidth) {
        String value = text == null ? "" : text;
        TextKey key = new TextKey(value, Math.max(0, maxWidth));
        return trimCache.computeIfAbsent(key, ignored -> trimDirect(font, value, maxWidth));
    }

    public int wrappedHeight(Font font, String text, int maxWidth) {
        String value = text == null ? "" : text;
        TextKey key = new TextKey(value, Math.max(24, maxWidth));
        return wrappedHeightCache.computeIfAbsent(key, ignored -> wrappedHeightDirect(font, value, maxWidth));
    }

    public long frameId() {
        return frameId;
    }

    private void clear() {
        trimCache.clear();
        wrappedHeightCache.clear();
    }

    private static String trimDirect(Font font, String text, int maxWidth) {
        if (font == null) {
            return trimApproximate(text, maxWidth);
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        if (maxWidth <= font.width("...")) {
            return "";
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    private static int wrappedHeightDirect(Font font, String text, int maxWidth) {
        if (font == null) {
            return wrappedHeightApproximate(text, maxWidth);
        }
        int lines = 0;
        for (String paragraph : text.split("\\R", -1)) {
            lines += paragraph.isEmpty()
                    ? 1
                    : font.split(Component.literal(paragraph), Math.max(24, maxWidth)).size();
        }
        return lines * 11;
    }

    private static String trimApproximate(String text, int maxWidth) {
        int maxChars = Math.max(0, maxWidth / 6);
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return "";
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private static int wrappedHeightApproximate(String text, int maxWidth) {
        int charsPerLine = Math.max(4, maxWidth / 6);
        int lines = 0;
        for (String paragraph : text.split("\\R", -1)) {
            lines += paragraph.isEmpty()
                    ? 1
                    : Math.max(1, (paragraph.length() + charsPerLine - 1) / charsPerLine);
        }
        return lines * 11;
    }

    private record TextKey(String text, int width) {
    }
}
