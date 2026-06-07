package com.knoxhack.echothemecore.api;

import java.util.Locale;

public enum EchoThemeDensity {
    COMPACT(0.86F, 6, 24, 40),
    STANDARD(1.0F, 8, 28, 48),
    COMFORTABLE(1.16F, 10, 32, 56);

    private final float spacingScale;
    private final int panelGap;
    private final int buttonHeight;
    private final int listRowHeight;

    EchoThemeDensity(float spacingScale, int panelGap, int buttonHeight, int listRowHeight) {
        this.spacingScale = spacingScale;
        this.panelGap = panelGap;
        this.buttonHeight = buttonHeight;
        this.listRowHeight = listRowHeight;
    }

    public float spacingScale() {
        return spacingScale;
    }

    public int panelGap() {
        return panelGap;
    }

    public int buttonHeight() {
        return buttonHeight;
    }

    public int listRowHeight() {
        return listRowHeight;
    }

    public static EchoThemeDensity byName(String raw, EchoThemeDensity fallback) {
        String value = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT);
        for (EchoThemeDensity density : values()) {
            if (density.name().equals(value)) {
                return density;
            }
        }
        return fallback == null ? STANDARD : fallback;
    }
}
