package com.knoxhack.echoscreencore.api.contract;

public record EchoResponsiveScaleRule(
    int denseBelowWidth,
    int compactBelowWidth,
    int stackBelowWidth,
    float minScale,
    float maxScale,
    boolean preserveTextReadability
) {
    public EchoResponsiveScaleRule {
        denseBelowWidth = Math.max(0, denseBelowWidth);
        compactBelowWidth = Math.max(0, compactBelowWidth);
        stackBelowWidth = Math.max(0, stackBelowWidth);
        minScale = Math.max(0.5F, minScale);
        maxScale = Math.max(minScale, maxScale);
    }

    public static EchoResponsiveScaleRule cyberglassApp() {
        return new EchoResponsiveScaleRule(760, 1080, 920, 0.92F, 1.0F, true);
    }

    public float clampScale(float requested) {
        return Math.max(minScale, Math.min(maxScale, requested));
    }
}
