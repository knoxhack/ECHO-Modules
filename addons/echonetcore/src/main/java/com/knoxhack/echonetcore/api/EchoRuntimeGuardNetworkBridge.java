package com.knoxhack.echonetcore.api;

import com.knoxhack.echocore.api.network.EchoPacketKind;
import java.lang.reflect.Method;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class EchoRuntimeGuardNetworkBridge {
    private static final String RUNTIME_GUARD_SERVICES = "com.knoxhack.echoruntimeguard.api.RuntimeGuardServices";
    private static final String NETWORK_PRIORITY = "com.knoxhack.echoruntimeguard.api.NetworkPriority";
    private static boolean unavailable;

    private EchoRuntimeGuardNetworkBridge() {
    }

    public static void recordSend(CustomPacketPayload payload, EchoPacketKind kind) {
        if (payload == null || unavailable) {
            return;
        }
        try {
            Object network = service("network");
            Object priority = priority(kind);
            if (network == null || priority == null) {
                return;
            }
            Method method = network.getClass().getMethod("recordSend", Identifier.class, int.class, priority.getClass());
            method.invoke(network, payload.type().id(), estimateBytes(payload), priority);
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
        }
    }

    public static boolean shouldDropNonCriticalDuplicate(CustomPacketPayload payload, EchoPacketKind kind) {
        if (payload == null || unavailable || protectedKind(kind)) {
            return false;
        }
        try {
            Object network = service("network");
            if (network == null) {
                return false;
            }
            Method duplicate = network.getClass().getMethod("shouldDropDuplicate", Identifier.class, int.class);
            Object result = duplicate.invoke(network, payload.type().id(), payloadHash(payload));
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
            return false;
        }
    }

    public static boolean isNonCriticalOverBudget(CustomPacketPayload payload, EchoPacketKind kind) {
        if (payload == null || unavailable || protectedKind(kind)) {
            return false;
        }
        try {
            Object network = service("network");
            Object priority = priority(kind);
            if (network == null || priority == null) {
                return false;
            }
            Method canSend = network.getClass().getMethod("canSend", Identifier.class, priority.getClass());
            Object result = canSend.invoke(network, payload.type().id(), priority);
            return result instanceof Boolean value && !value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
            return false;
        }
    }

    private static Object service(String methodName) {
        try {
            Class<?> services = Class.forName(RUNTIME_GUARD_SERVICES);
            return services.getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Object priority(EchoPacketKind kind) {
        Class<?> priorityClass;
        try {
            priorityClass = Class.forName(NETWORK_PRIORITY);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        return enumValue(priorityClass, switch (kind == null ? EchoPacketKind.CLIENTBOUND_SYNC : kind) {
            case SERVERBOUND_ACTION -> "GAMEPLAY";
            case CLIENTBOUND_SYNC -> "BACKGROUND_SYNC";
            case DEBUG_DEV -> "DEBUG";
            case OPTIONAL_ADDON -> "BACKGROUND_SYNC";
        });
    }

    private static boolean protectedKind(EchoPacketKind kind) {
        return kind == EchoPacketKind.SERVERBOUND_ACTION;
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

    private static int estimateBytes(CustomPacketPayload payload) {
        String className = payload.getClass().getName();
        String packetId = payload.type().id().toString();
        return Math.max(32, className.length() + packetId.length() + 48);
    }

    private static int payloadHash(CustomPacketPayload payload) {
        return 31 * payload.type().id().hashCode() + payload.getClass().getName().hashCode();
    }
}
