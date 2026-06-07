package com.knoxhack.echoscreencore.api.theme;

public record EchoAccessibilitySettings(
    boolean largeText,
    boolean highContrast,
    boolean reducedClutter,
    boolean reduceGlow,
    boolean compactDensity,
    boolean comfortableDensity,
    boolean hideDebugInfo,
    boolean simplifiedMode
) {
    public static final EchoAccessibilitySettings DEFAULT =
        new EchoAccessibilitySettings(false, false, false, false, false, false, false, false);

    public float fontScale() {
        return largeText ? 1.25F : 1.0F;
    }

    public float spacingScale() {
        if (compactDensity) {
            return 0.82F;
        }
        if (comfortableDensity || largeText) {
            return 1.18F;
        }
        return 1.0F;
    }

    public boolean quietVisuals() {
        return reducedClutter || reduceGlow || simplifiedMode;
    }

    public EchoAccessibilitySettings withLargeText(boolean value) {
        return new EchoAccessibilitySettings(value, highContrast, reducedClutter, reduceGlow, compactDensity, comfortableDensity, hideDebugInfo, simplifiedMode);
    }

    public EchoAccessibilitySettings withHighContrast(boolean value) {
        return new EchoAccessibilitySettings(largeText, value, reducedClutter, reduceGlow, compactDensity, comfortableDensity, hideDebugInfo, simplifiedMode);
    }

    public EchoAccessibilitySettings withReducedClutter(boolean value) {
        return new EchoAccessibilitySettings(largeText, highContrast, value, reduceGlow, compactDensity, comfortableDensity, hideDebugInfo, simplifiedMode);
    }

    public EchoAccessibilitySettings withDensity(String density) {
        String value = density == null ? "" : density.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "compact" -> new EchoAccessibilitySettings(largeText, highContrast, reducedClutter, reduceGlow, true, false, hideDebugInfo, simplifiedMode);
            case "comfortable" -> new EchoAccessibilitySettings(largeText, highContrast, reducedClutter, reduceGlow, false, true, hideDebugInfo, simplifiedMode);
            default -> new EchoAccessibilitySettings(largeText, highContrast, reducedClutter, reduceGlow, false, false, hideDebugInfo, simplifiedMode);
        };
    }
}
