package com.knoxhack.signalos.api;

import net.minecraft.resources.Identifier;

/**
 * Lightweight theme token record for the MVP terminal shell.
 */
public record TerminalTheme(
        Identifier id,
        String title,
        int backgroundColor,
        int panelColor,
        int accentColor,
        int warningColor,
        boolean glitch) {
    public TerminalTheme {
        id = TerminalIds.requireLowercase(id, "Terminal theme");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        backgroundColor = opaque(backgroundColor == 0 ? 0x050B10 : backgroundColor);
        panelColor = opaque(panelColor == 0 ? 0x071017 : panelColor);
        accentColor = opaque(accentColor == 0 ? 0x66E8FF : accentColor);
        warningColor = opaque(warningColor == 0 ? 0xFF3355 : warningColor);
    }

    public static Builder builder(String id) {
        return new Builder(TerminalIds.parse(id, "Terminal theme"));
    }

    public static TerminalTheme defaultTheme() {
        return builder("signalos:default")
                .title("SignalOS Default")
                .backgroundColor(0xFF050B10)
                .panelColor(0xFF071017)
                .accentColor(0xFF66E8FF)
                .warningColor(0xFFFFD166)
                .build();
    }

    private static int opaque(int color) {
        return (color >>> 24) == 0 ? 0xFF000000 | color : color;
    }

    public static final class Builder {
        private final Identifier id;
        private String title = "";
        private int backgroundColor;
        private int panelColor;
        private int accentColor;
        private int warningColor;
        private boolean glitch;

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder backgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder panelColor(int panelColor) {
            this.panelColor = panelColor;
            return this;
        }

        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder warningColor(int warningColor) {
            this.warningColor = warningColor;
            return this;
        }

        public Builder glitch(boolean glitch) {
            this.glitch = glitch;
            return this;
        }

        public TerminalTheme build() {
            return new TerminalTheme(id, title, backgroundColor, panelColor, accentColor, warningColor, glitch);
        }
    }
}
