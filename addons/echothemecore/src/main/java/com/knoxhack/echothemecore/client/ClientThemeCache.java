package com.knoxhack.echothemecore.client;

import com.knoxhack.echothemecore.api.EchoTheme;
import net.minecraft.resources.Identifier;

/**
 * Compatibility facade for older ThemeCore callers. New client renderers should
 * use {@link ClientThemeState}; server packet application is intentionally ignored.
 */
public final class ClientThemeCache {
    private ClientThemeCache() {
    }

    public static EchoTheme currentTheme() {
        return ClientThemeState.currentTheme();
    }

    public static Identifier currentThemeId() {
        return ClientThemeState.currentThemeId();
    }

    public static Identifier previousThemeId() {
        return ClientThemeState.previousThemeId();
    }

    public static void applyServerTheme(Identifier themeId) {
        ClientThemeState.ignoreServerTheme(themeId);
    }

    public static void reset() {
        ClientThemeState.reset();
    }

    public static float transitionProgress() {
        return ClientThemeState.transitionProgress();
    }

    public static boolean transitioning() {
        return ClientThemeState.transitioning();
    }

    public static void onClientTick() {
        ClientThemeState.onClientTick();
    }
}
