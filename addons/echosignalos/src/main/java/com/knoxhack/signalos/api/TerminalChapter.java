package com.knoxhack.signalos.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

/**
 * A top-level navigation chapter in the terminal sidebar.
 */
public record TerminalChapter(
        Identifier id,
        String title,
        String section,
        int order,
        int accentColor,
        Identifier icon,
        List<String> pages,
        boolean visible) {
    public TerminalChapter {
        id = TerminalIds.requireLowercase(id, "Terminal chapter");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        section = cleanSection(section);
        accentColor = opaque(accentColor == 0 ? 0x66E8FF : accentColor);
        pages = List.copyOf(pages == null || pages.isEmpty()
                ? List.of("missions", "archives", "rewards", "diagnostics")
                : pages);
    }

    public static Builder builder(String id) {
        return new Builder(TerminalIds.parse(id, "Terminal chapter"));
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    private static String cleanSection(String value) {
        String cleaned = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return switch (cleaned) {
            case "command", "progress", "intel", "system" -> cleaned;
            default -> "progress";
        };
    }

    private static int opaque(int color) {
        return (color >>> 24) == 0 ? 0xFF000000 | color : color;
    }

    public static final class Builder {
        private final Identifier id;
        private String title = "";
        private String section = "progress";
        private int order;
        private int accentColor = 0xFF66E8FF;
        private Identifier icon;
        private final List<String> pages = new ArrayList<>();
        private boolean visible = true;

        private Builder(Identifier id) {
            this.id = TerminalIds.requireLowercase(id, "Terminal chapter");
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder section(String section) {
            this.section = section;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon == null || icon.isBlank() ? null : TerminalIds.parse(icon, "Terminal chapter icon");
            return this;
        }

        public Builder page(String page) {
            if (page != null && !page.isBlank()) {
                pages.add(page.strip().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public TerminalChapter build() {
            return new TerminalChapter(id, title, section, order, accentColor, icon, pages, visible);
        }
    }
}
