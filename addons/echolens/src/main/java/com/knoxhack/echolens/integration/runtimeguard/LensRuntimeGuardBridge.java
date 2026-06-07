package com.knoxhack.echolens.integration.runtimeguard;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

public final class LensRuntimeGuardBridge {
    private static final String RUNTIME_GUARD_SERVICES = "com.knoxhack.echoruntimeguard.api.RuntimeGuardServices";
    private static final String LENS_SCAN_TYPE = "com.knoxhack.echoruntimeguard.api.LensScanType";

    private LensRuntimeGuardBridge() {
    }

    public static boolean canRunDeepScan(ServerPlayer player) {
        try {
            Object integrations = integrations();
            Object deep = scanType("DEEP");
            if (integrations == null || deep == null) {
                return true;
            }
            Method method = integrations.getClass().getMethod("canRunLensScan",
                    net.minecraft.world.entity.player.Player.class, deep.getClass());
            Object result = method.invoke(integrations, player, deep);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return true;
        }
    }

    public static int deepScanBudget(ServerPlayer player, int fallback) {
        try {
            Object integrations = integrations();
            if (integrations == null) {
                return Math.max(1, fallback);
            }
            Method method = integrations.getClass().getMethod("getDeepScanBudgetPerTick",
                    net.minecraft.world.entity.player.Player.class);
            Object result = method.invoke(integrations, player);
            int guarded = result instanceof Number number ? number.intValue() : fallback;
            return Math.max(1, Math.min(Math.max(1, fallback), guarded));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return Math.max(1, fallback);
        }
    }

    public static void recordDeepScan(ServerPlayer player, int blocksScanned, int entitiesScanned) {
        try {
            Object integrations = integrations();
            Object deep = scanType("DEEP");
            if (integrations == null || deep == null) {
                return;
            }
            Method method = integrations.getClass().getMethod("recordLensScan",
                    net.minecraft.world.entity.player.Player.class, deep.getClass(), int.class, int.class);
            method.invoke(integrations, player, deep, blocksScanned, entitiesScanned);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // RuntimeGuard is optional; Lens keeps its local scan behavior when unavailable.
        }
    }

    private static Object integrations() {
        try {
            Class<?> services = Class.forName(RUNTIME_GUARD_SERVICES);
            return services.getMethod("integrations").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Object scanType(String name) {
        try {
            Class<?> type = Class.forName(LENS_SCAN_TYPE);
            Object[] constants = type.getEnumConstants();
            if (constants == null) {
                return null;
            }
            for (Object constant : constants) {
                if (constant instanceof Enum<?> enumValue && enumValue.name().equals(name)) {
                    return constant;
                }
            }
            return null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }
}
