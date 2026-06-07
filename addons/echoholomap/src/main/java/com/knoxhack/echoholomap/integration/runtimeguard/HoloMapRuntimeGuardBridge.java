package com.knoxhack.echoholomap.integration.runtimeguard;

import com.knoxhack.echoholomap.EchoHoloMap;
import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class HoloMapRuntimeGuardBridge {
    private static final String RUNTIME_GUARD_SERVICES = "com.knoxhack.echoruntimeguard.api.RuntimeGuardServices";
    private static final String NETWORK_PRIORITY = "com.knoxhack.echoruntimeguard.api.NetworkPriority";

    private HoloMapRuntimeGuardBridge() {
    }

    public static boolean shouldRefreshMarkers(ServerPlayer player, String reason) {
        if (isPlayerRequested(reason)) {
            return true;
        }
        try {
            Object integrations = service("integrations");
            if (integrations == null) {
                return true;
            }
            Method method = integrations.getClass().getMethod("shouldRefreshHoloMapMarkers",
                    net.minecraft.core.BlockPos.class);
            Object result = method.invoke(integrations, player == null ? null : player.blockPosition());
            return result instanceof Boolean value ? value : true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return true;
        }
    }

    public static int refreshIntervalTicks(int fallback) {
        try {
            Object integrations = service("integrations");
            if (integrations == null) {
                return Math.max(1, fallback);
            }
            Method method = integrations.getClass().getMethod("getHoloMapRefreshIntervalTicks");
            Object result = method.invoke(integrations);
            int guarded = result instanceof Number number ? number.intValue() : fallback;
            return Math.max(Math.max(1, fallback), guarded);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return Math.max(1, fallback);
        }
    }

    public static void recordSync(String channelPath, int estimatedBytes, String priorityName) {
        try {
            Object network = service("network");
            Object priority = priority(priorityName);
            if (network == null || priority == null) {
                return;
            }
            Method method = network.getClass().getMethod("recordSend",
                    Identifier.class, int.class, priority.getClass());
            method.invoke(network, Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, channelPath),
                    estimatedBytes, priority);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // RuntimeGuard is optional; HoloMap keeps its local behavior when the bridge is unavailable.
        }
    }

    private static boolean isPlayerRequested(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        String normalized = reason.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("manual")
                || normalized.contains("command")
                || normalized.contains("button")
                || normalized.contains("player")
                || normalized.contains("test");
    }

    private static Object service(String methodName) {
        try {
            Class<?> services = Class.forName(RUNTIME_GUARD_SERVICES);
            return services.getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Object priority(String name) {
        Class<?> priorityClass;
        try {
            priorityClass = Class.forName(NETWORK_PRIORITY);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        Object fallback = enumValue(priorityClass, "BACKGROUND_SYNC");
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Object value = enumValue(priorityClass, name);
        return value == null ? fallback : value;
    }

    private static Object enumValue(Class<?> enumClass, String name) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumValue && enumValue.name().equals(name)) {
                return constant;
            }
        }
        return null;
    }
}
