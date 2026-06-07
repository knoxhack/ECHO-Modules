package com.knoxhack.echoterminal.client.screen;

import java.util.Objects;
import net.minecraft.client.Minecraft;

public record TerminalScreenTheme(
        String title,
        StatusProvider statusProvider,
        String footerText,
        int backgroundColor,
        int panelColor,
        int contentColor,
        int accentColor,
        int borderColor,
        int textColor,
        int mutedColor,
        int panelMaxWidth,
        int panelMaxHeight) {
    public TerminalScreenTheme {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Terminal title is required.");
        }
        statusProvider = Objects.requireNonNullElse(statusProvider, minecraft -> "LINK ONLINE");
        footerText = footerText == null ? "" : footerText;
        panelMaxWidth = Math.max(360, panelMaxWidth);
        panelMaxHeight = Math.max(270, panelMaxHeight);
    }

    public static TerminalScreenTheme modular() {
        return new TerminalScreenTheme(
                "ECHO-7 MODULAR TERMINAL",
                minecraft -> "LINK ONLINE",
                "ESC/M close | tabs | sections | wheel/page scroll",
                0xEE050B10,
                0xF20A1218,
                0xDD071017,
                0xFF66D9FF,
                0xFF244352,
                0xFFE9FBFF,
                0xFF8CA7B5,
                1500,
                820);
    }

    @FunctionalInterface
    public interface StatusProvider {
        String statusLine(Minecraft minecraft);
    }
}
