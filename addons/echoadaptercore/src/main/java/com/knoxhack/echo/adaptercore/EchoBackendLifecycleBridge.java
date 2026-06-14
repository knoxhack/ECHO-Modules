package com.knoxhack.echo.adaptercore;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * AdapterCore backend bridge for mod lifecycle and event-bus wiring.
 */
public final class EchoBackendLifecycleBridge {
    private static final String I_EVENT_BUS = "net.neoforged.bus.api.IEventBus";
    private static final String REGISTER_GAME_TESTS_EVENT =
            "net.neoforged.neoforge.event.RegisterGameTestsEvent";

    private EchoBackendLifecycleBridge() {
    }

    public static void registerModListener(Object eventBus, Consumer<?> listener) {
        if (eventBus != null && listener != null) {
            invokeOneArgument(eventBus, "addListener", listener);
        }
    }

    public static void registerModListener(Object eventBus, String eventClassName, Consumer<?> listener) {
        if (eventBus == null || eventClassName == null || eventClassName.isBlank() || listener == null) {
            return;
        }
        Class<?> eventClass = resolveClass(eventClassName);
        if (eventClass != null && invokeClassConsumer(eventBus, "addListener", eventClass, listener)) {
            return;
        }
        registerModListener(eventBus, listener);
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

    public static void registerGameEventHandler(String eventClassName, Consumer<?> listener) {
        Object eventBus = neoForgeEventBus();
        if (eventBus == null || eventClassName == null || eventClassName.isBlank() || listener == null) {
            return;
        }
        Class<?> eventClass = resolveClass(eventClassName);
        if (eventClass != null && invokeClassConsumer(eventBus, "addListener", eventClass, listener)) {
            return;
        }
        registerGameEventHandler(listener);
    }

    public static void registerOptionalGameTests(Object eventBus, String testClassName) {
        if (eventBus == null || testClassName == null || testClassName.isBlank()) {
            return;
        }
        invokeStaticOneArgument(testClassName, "register", I_EVENT_BUS, eventBus);
        registerModListener(eventBus, REGISTER_GAME_TESTS_EVENT,
                event -> invokeStaticOneArgument(testClassName, "registerTests", REGISTER_GAME_TESTS_EVENT, event));
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

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static boolean invokeClassConsumer(Object target, String methodName, Class<?> eventClass, Object listener) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!Class.class.isAssignableFrom(parameterTypes[0])
                    || !parameterTypes[1].isAssignableFrom(listener.getClass())) {
                continue;
            }
            try {
                method.invoke(target, eventClass, listener);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next overload; standalone runtime may not have the backend-specific type.
            }
        }
        return false;
    }

    private static boolean invokeStaticOneArgument(
            String className, String methodName, String argumentClassName, Object argument) {
        Class<?> argumentClass = resolveClass(argumentClassName);
        if (argumentClass == null || argument == null) {
            return false;
        }
        try {
            Class.forName(className).getMethod(methodName, argumentClass).invoke(null, argument);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
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
