package com.knoxhack.echolens.integration.runtimeguard;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.platform.LensModuleAccess;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

public final class LensRuntimeGuardHooks {
    private static boolean bridgeUnavailable;
    private static Method canRunDeepScanMethod;
    private static Method deepScanBudgetMethod;
    private static Method recordDeepScanMethod;

    private LensRuntimeGuardHooks() {
    }

    public static boolean canRunDeepScan(ServerPlayer player) {
        if (!resolveBridge()) {
            return true;
        }
        try {
            Object result = canRunDeepScanMethod.invoke(null, player);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoLens.LOGGER.warn("Lens RuntimeGuard scan bridge failed; allowing scan.", exception);
            return true;
        }
    }

    public static int deepScanBudget(ServerPlayer player, int fallback) {
        if (!resolveBridge()) {
            return fallback;
        }
        try {
            Object result = deepScanBudgetMethod.invoke(null, player, fallback);
            return result instanceof Integer value ? value : fallback;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoLens.LOGGER.warn("Lens RuntimeGuard budget bridge failed.", exception);
            return fallback;
        }
    }

    public static void recordDeepScan(ServerPlayer player, int blocksScanned, int entitiesScanned) {
        if (!resolveBridge()) {
            return;
        }
        try {
            recordDeepScanMethod.invoke(null, player, blocksScanned, entitiesScanned);
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoLens.LOGGER.warn("Lens RuntimeGuard scan accounting bridge failed.", exception);
        }
    }

    private static boolean resolveBridge() {
        if (bridgeUnavailable || !LensModuleAccess.isLoaded("echoruntimeguard")) {
            return false;
        }
        if (canRunDeepScanMethod != null && deepScanBudgetMethod != null && recordDeepScanMethod != null) {
            return true;
        }
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echolens.integration.runtimeguard.LensRuntimeGuardBridge");
            canRunDeepScanMethod = bridge.getMethod("canRunDeepScan", ServerPlayer.class);
            deepScanBudgetMethod = bridge.getMethod("deepScanBudget", ServerPlayer.class, int.class);
            recordDeepScanMethod = bridge.getMethod("recordDeepScan", ServerPlayer.class, int.class, int.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            bridgeUnavailable = true;
            EchoLens.LOGGER.warn("Lens RuntimeGuard bridge is unavailable.", exception);
            return false;
        }
    }
}
