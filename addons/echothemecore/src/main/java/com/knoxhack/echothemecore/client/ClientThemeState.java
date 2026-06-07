package com.knoxhack.echothemecore.client;

import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Client-local visual theme authority. Server sync packets and common registry
 * state intentionally do not mutate this state.
 */
public final class ClientThemeState {
    private static Identifier activeThemeId = ThemeRegistry.ECHO_PLATFORM_ID;
    private static Identifier previousThemeId = ThemeRegistry.ECHO_PLATFORM_ID;
    private static int transitionTicks;
    private static int transitionAge;
    private static boolean initialized;
    private static boolean localOverrideMode = true;

    private ClientThemeState() {
    }

    public static void bootstrap() {
        if (initialized) {
            return;
        }
        initialized = true;
        Identifier stored = ThemeRegistry.resolveAlias(ThemeCoreConfig.localClientTheme());
        activeThemeId = validOrFallback(stored);
        previousThemeId = activeThemeId;
        transitionTicks = 0;
        transitionAge = 0;
        syncTerminalTheme();
    }

    public static EchoTheme currentTheme() {
        bootstrap();
        return ThemeRegistry.get(activeThemeId);
    }

    public static Identifier currentThemeId() {
        bootstrap();
        return currentTheme().id();
    }

    public static Identifier previousThemeId() {
        bootstrap();
        return previousThemeId;
    }

    public static boolean localOverrideMode() {
        return localOverrideMode;
    }

    public static void setLocalOverrideMode(boolean enabled) {
        localOverrideMode = enabled;
    }

    public static EchoTheme setTheme(Identifier themeId) {
        return setTheme(themeId, true);
    }

    public static EchoTheme setTheme(Identifier themeId, boolean persist) {
        bootstrap();
        Identifier resolved = validOrFallback(ThemeRegistry.resolveAlias(themeId));
        if (resolved.equals(activeThemeId)) {
            if (persist) {
                ThemeCoreConfig.setLocalClientTheme(resolved);
            }
            syncTerminalTheme();
            return ThemeRegistry.get(resolved);
        }
        previousThemeId = activeThemeId;
        activeThemeId = resolved;
        transitionTicks = ThemeCoreConfig.enableThemeTransitions() ? ThemeCoreConfig.themeTransitionTicks() : 0;
        transitionAge = 0;
        if (persist) {
            ThemeCoreConfig.setLocalClientTheme(resolved);
        }
        syncTerminalTheme();
        return ThemeRegistry.get(resolved);
    }

    public static EchoTheme cycleTheme(int direction) {
        bootstrap();
        EchoTheme next = ThemeRegistry.nextPublicTheme(activeThemeId, direction);
        return setTheme(next.id(), true);
    }

    public static List<EchoTheme> listPublicThemes() {
        return ThemeRegistry.listPublicThemes();
    }

    public static EchoTheme reset() {
        return setTheme(ThemeRegistry.ECHO_PLATFORM_ID, true);
    }

    public static void reconcileLoadedTheme() {
        bootstrap();
        Identifier resolved = validOrFallback(activeThemeId);
        if (!resolved.equals(activeThemeId)) {
            previousThemeId = activeThemeId == null ? resolved : activeThemeId;
            activeThemeId = resolved;
            transitionTicks = 0;
            transitionAge = 0;
            ThemeCoreConfig.setLocalClientTheme(resolved);
        }
        syncTerminalTheme();
    }

    public static void ignoreServerTheme(Identifier ignoredThemeId) {
        reconcileLoadedTheme();
    }

    public static float transitionProgress() {
        bootstrap();
        if (transitionTicks <= 0) {
            return 1.0F;
        }
        return Math.min(1.0F, transitionAge / (float) transitionTicks);
    }

    public static boolean transitioning() {
        bootstrap();
        return transitionTicks > 0 && transitionAge < transitionTicks;
    }

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        bootstrap();
        if (transitionAge < transitionTicks) {
            transitionAge++;
        }
    }

    private static Identifier validOrFallback(Identifier themeId) {
        Identifier resolved = ThemeRegistry.resolveAlias(themeId);
        if (resolved != null && ThemeRegistry.find(resolved).isPresent()) {
            return resolved;
        }
        return ThemeRegistry.ECHO_PLATFORM_ID;
    }

    private static void syncTerminalTheme() {
        if (!EchoRuntimeModules.isLoaded("echoterminal")) {
            return;
        }
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echothemecore.integration.ThemeCoreTerminalBridge");
            bridge.getMethod("syncClientTheme", Identifier.class).invoke(null, activeThemeId);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Terminal is optional; ThemeCore remains authoritative even when the bridge is unavailable.
        }
    }
}
