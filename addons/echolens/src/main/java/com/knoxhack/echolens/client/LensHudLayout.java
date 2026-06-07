package com.knoxhack.echolens.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class LensHudLayout {
    public static final int SCREEN_PADDING = 8;

    private LensHudLayout() {
    }

    public static Bounds clampPanel(int preferredX, int preferredY, int width, int height, int screenWidth,
            int screenHeight) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        int maxX = Math.max(SCREEN_PADDING, screenWidth - safeWidth - SCREEN_PADDING);
        int maxY = Math.max(SCREEN_PADDING, screenHeight - safeHeight - SCREEN_PADDING);
        return new Bounds(clamp(preferredX, SCREEN_PADDING, maxX), clamp(preferredY, SCREEN_PADDING, maxY),
                safeWidth, safeHeight);
    }

    public static HeaderLayout headerLayout(int panelWidth, boolean hasIcon, int preferredBadgeWidth) {
        int titleX = hasIcon ? 28 : 9;
        int rightPadding = 7;
        int gap = 8;
        int available = Math.max(1, panelWidth - titleX - rightPadding - gap);
        int minimumTitleWidth = Math.min(104, Math.max(52, available * 45 / 100));
        int maximumBadgeWidth = Math.max(36, available - minimumTitleWidth);
        int minimumBadgeWidth = Math.min(48, maximumBadgeWidth);
        int badgeWidth = clamp(preferredBadgeWidth, minimumBadgeWidth, maximumBadgeWidth);
        int badgeX = Math.max(titleX + minimumTitleWidth + gap, panelWidth - rightPadding - badgeWidth);
        int titleWidth = Math.max(1, badgeX - titleX - gap);
        return new HeaderLayout(titleX, titleWidth, badgeX, badgeWidth);
    }

    public static RowColumns rowColumns(int panelWidth) {
        int labelX = 24;
        int rightPadding = 9;
        int gap = 8;
        int contentRight = Math.max(labelX + 1, panelWidth - rightPadding);
        int available = Math.max(1, contentRight - labelX);
        int minimumValueWidth = Math.min(56, Math.max(32, available / 3));
        int labelWidth = Math.min(76, Math.max(32, available - gap - minimumValueWidth));
        int valueX = labelX + labelWidth + gap;
        int valueWidth = Math.max(1, contentRight - valueX);
        if (valueWidth < minimumValueWidth && labelWidth > 32) {
            int reclaimed = Math.min(labelWidth - 32, minimumValueWidth - valueWidth);
            labelWidth -= reclaimed;
            valueX = labelX + labelWidth + gap;
            valueWidth = Math.max(1, contentRight - valueX);
        }
        return new RowColumns(10, labelX, labelWidth, valueX, valueWidth);
    }

    public static String firstFitting(List<String> candidates, int maxWidth, ToIntFunction<String> textWidth) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        String fallback = "";
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            fallback = candidate;
            int width = textWidth == null ? candidate.length() : textWidth.applyAsInt(candidate);
            if (width <= maxWidth) {
                return candidate;
            }
        }
        return fallback;
    }

    public static ActionStrip actionStrip(int panelWidth, int[] preferredChipWidths, int chipHeight, int gap,
            int horizontalPadding) {
        if (preferredChipWidths == null || preferredChipWidths.length == 0) {
            return new ActionStrip(List.of(), 0);
        }
        int contentWidth = Math.max(1, panelWidth - horizontalPadding * 2);
        List<ActionChip> chips = new ArrayList<>();
        int x = 0;
        int y = 0;
        for (int index = 0; index < preferredChipWidths.length; index++) {
            int chipWidth = clamp(preferredChipWidths[index], Math.min(36, contentWidth), contentWidth);
            if (x > 0 && x + chipWidth > contentWidth) {
                x = 0;
                y += chipHeight + gap;
            }
            chips.add(new ActionChip(index, horizontalPadding + x, y, chipWidth));
            x += chipWidth + gap;
        }
        return new ActionStrip(chips, y + chipHeight);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public record Bounds(int x, int y, int width, int height) {
    }

    public record HeaderLayout(int titleX, int titleWidth, int badgeX, int badgeWidth) {
    }

    public record RowColumns(int iconX, int labelX, int labelWidth, int valueX, int valueWidth) {
    }

    public record ActionStrip(List<ActionChip> chips, int height) {
        public ActionStrip {
            chips = List.copyOf(chips == null ? List.of() : chips);
            height = Math.max(0, height);
        }
    }

    public record ActionChip(int index, int x, int y, int width) {
    }
}
