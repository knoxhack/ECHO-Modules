package com.knoxhack.echoholomap.integration.runtimeguard;

import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.platform.HoloMapModuleAccess;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

public final class HoloMapRuntimeGuardHooks {
    private static boolean bridgeUnavailable;
    private static Method shouldRefreshMarkersMethod;
    private static Method refreshIntervalTicksMethod;
    private static Method recordSyncMethod;

    private HoloMapRuntimeGuardHooks() {
    }

    public static boolean shouldRefreshMarkers(ServerPlayer player, String reason) {
        if (!resolveBridge()) {
            return true;
        }
        try {
            Object result = shouldRefreshMarkersMethod.invoke(null, player, reason);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoHoloMap.LOGGER.warn("HoloMap RuntimeGuard refresh bridge failed; allowing refresh.", exception);
            return true;
        }
    }

    public static int refreshIntervalTicks(int fallback) {
        if (!resolveBridge()) {
            return fallback;
        }
        try {
            Object result = refreshIntervalTicksMethod.invoke(null, fallback);
            return result instanceof Integer value ? value : fallback;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoHoloMap.LOGGER.warn("HoloMap RuntimeGuard interval bridge failed.", exception);
            return fallback;
        }
    }

    public static void recordSync(String channelPath, int estimatedBytes, String priorityName) {
        if (!resolveBridge()) {
            return;
        }
        try {
            recordSyncMethod.invoke(null, channelPath, estimatedBytes, priorityName);
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoHoloMap.LOGGER.warn("HoloMap RuntimeGuard network bridge failed.", exception);
        }
    }

    private static boolean resolveBridge() {
        if (bridgeUnavailable || !HoloMapModuleAccess.isLoaded("echoruntimeguard")) {
            return false;
        }
        if (shouldRefreshMarkersMethod != null && refreshIntervalTicksMethod != null && recordSyncMethod != null) {
            return true;
        }
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echoholomap.integration.runtimeguard.HoloMapRuntimeGuardBridge");
            shouldRefreshMarkersMethod = bridge.getMethod("shouldRefreshMarkers", ServerPlayer.class, String.class);
            refreshIntervalTicksMethod = bridge.getMethod("refreshIntervalTicks", int.class);
            recordSyncMethod = bridge.getMethod("recordSync", String.class, int.class, String.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoHoloMap.LOGGER.warn("HoloMap RuntimeGuard bridge is unavailable.", exception);
            return false;
        }
    }
}
