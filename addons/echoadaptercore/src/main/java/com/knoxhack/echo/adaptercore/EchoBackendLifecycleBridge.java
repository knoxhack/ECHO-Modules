package com.knoxhack.echo.adaptercore;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * AdapterCore backend bridge for mod lifecycle and event-bus wiring.
 */
public final class EchoBackendLifecycleBridge {
    private EchoBackendLifecycleBridge() {
    }

    public static void registerModListener(Object eventBus, Consumer<?> listener) {
        if (eventBus != null && listener != null) {
            invokeOneArgument(eventBus, "addListener", listener);
        }
    }

    public static void registerGameEventListener(Object listener) {
        Object eventBus = neoForgeEventBus();
        if (eventBus != null && listener != null) {
            invokeOneArgument(eventBus, "register", listener);
        }
    }

    public static void registerGameEventHandler(Consumer<?> listener) {
        Object eventBus = neoForgeEventBus();
        if (eventBus != null && listener != null) {
            invokeOneArgument(eventBus, "addListener", listener);
        }
    }

    public static void postGameEvent(Object event) {
        Object eventBus = neoForgeEventBus();
        if (eventBus != null && event != null) {
            invokeOneArgument(eventBus, "post", event);
        }
    }

    public static void runCommonSetupWork(Object event, Runnable work) {
        if (work == null) {
            return;
        }
        if (event != null && invokeOneArgument(event, "enqueueWork", work)) {
            return;
        }
        work.run();
    }

    private static Object neoForgeEventBus() {
        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            return neoForge.getField("EVENT_BUS").get(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static boolean invokeOneArgument(Object target, String methodName, Object argument) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            try {
                method.invoke(target, argument);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next overload; standalone runtime may not have the backend-specific type.
            }
        }
        return false;
    }
}
