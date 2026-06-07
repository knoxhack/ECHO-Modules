package com.knoxhack.echoterminal.api.theme;

import java.util.Objects;
import net.minecraft.resources.Identifier;

public record TerminalChapterStyle(
        String key,
        String displayName,
        int accentColor,
        int secondaryColor,
        Identifier banner,
        Identifier panel,
        Identifier border,
        TerminalIconSet icons) {
    public TerminalChapterStyle {
        key = key == null ? "" : key.strip().toLowerCase(java.util.Locale.ROOT);
        displayName = displayName == null ? "" : displayName.strip();
        icons = icons == null ? TerminalIconSet.builder().build() : icons;
    }

    public static Builder builder(String key, String displayName) {
        return new Builder(key, displayName);
    }

    public static final class Builder {
        private final String key;
        private final String displayName;
        private int accentColor = 0xFF66E8FF;
        private int secondaryColor = 0xFF8CA7B5;
        private Identifier banner;
        private Identifier panel;
        private Identifier border;
        private TerminalIconSet icons = TerminalIconSet.builder().build();

        private Builder(String key, String displayName) {
            this.key = Objects.requireNonNullElse(key, "");
            this.displayName = Objects.requireNonNullElse(displayName, "");
        }

        public Builder colors(int accentColor, int secondaryColor) {
            this.accentColor = accentColor;
            this.secondaryColor = secondaryColor;
            return this;
        }

        public Builder banner(Identifier banner) {
            this.banner = banner;
            return this;
        }

        public Builder panel(Identifier panel) {
            this.panel = panel;
            return this;
        }

        public Builder border(Identifier border) {
            this.border = border;
            return this;
        }

        public Builder icons(TerminalIconSet icons) {
            this.icons = icons == null ? TerminalIconSet.builder().build() : icons;
            return this;
        }

        public TerminalChapterStyle build() {
            return new TerminalChapterStyle(key, displayName, accentColor, secondaryColor,
                    banner, panel, border, icons);
        }
    }
}
