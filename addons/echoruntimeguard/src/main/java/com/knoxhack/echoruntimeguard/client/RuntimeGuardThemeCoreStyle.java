package com.knoxhack.echoruntimeguard.client;

import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;

final class RuntimeGuardThemeCoreStyle {
    private static boolean resolved;
    private static Method visualSettingsMethod;
    private static Method particlesEnabledMethod;
    private static Method edgeGlowEnabledMethod;
    private static Method animationIntensityMethod;

    private RuntimeGuardThemeCoreStyle() {
    }

    static boolean particlesEnabled(boolean fallback) {
        Object settings = visualSettings();
        return settings == null ? fallback : bool(settings, particlesEnabledMethod, fallback);
    }

    static boolean edgeGlowEnabled(boolean fallback) {
        Object settings = visualSettings();
        return settings == null ? fallback : bool(settings, edgeGlowEnabledMethod, fallback);
    }

    static float animationIntensity(float fallback) {
        Object settings = visualSettings();
        return settings == null ? fallback : decimal(settings, animationIntensityMethod, fallback);
    }

    private static Object visualSettings() {
        try {
            if (!resolve()) {
                return null;
            }
            return visualSettingsMethod.invoke(null, new Object[] { null });
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return visualSettingsMethod != null;
        }
        resolved = true;
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Class<?> settings = Class.forName("com.knoxhack.echothemecore.api.ThemeVisualSettings");
            visualSettingsMethod = api.getMethod("getEffectiveVisualSettings", Player.class);
            particlesEnabledMethod = settings.getMethod("particlesEnabled");
            edgeGlowEnabledMethod = settings.getMethod("edgeGlowEnabled");
            animationIntensityMethod = settings.getMethod("animationIntensity");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            visualSettingsMethod = null;
            particlesEnabledMethod = null;
            edgeGlowEnabledMethod = null;
            animationIntensityMethod = null;
            return false;
        }
    }

    private static boolean bool(Object target, Method accessor, boolean fallback) {
        if (accessor == null) {
            return fallback;
        }
        try {
            Object value = accessor.invoke(target);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return fallback;
        }
    }

    private static float decimal(Object target, Method accessor, float fallback) {
        if (accessor == null) {
            return fallback;
        }
        try {
            Object value = accessor.invoke(target);
            return value instanceof Number number ? number.floatValue() : fallback;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return fallback;
        }
    }
}
