package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AshfallNativeClientRouteBridge {
    private static final String REGISTRAR_CLASS =
            "com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeClientRouteRegistrar";
    private static final String DISPATCHER_CLASS = REGISTRAR_CLASS + "$SurfaceDispatcher";
    private static final AtomicBoolean LOGGED_UNAVAILABLE = new AtomicBoolean(false);

    private AshfallNativeClientRouteBridge() {
    }

    public static boolean register(boolean nativeLoaderActive, SurfaceDispatcher dispatcher) {
        if (!nativeLoaderActive || dispatcher == null) {
            return false;
        }
        try {
            Class<?> registrar = registrarClass();
            Class<?> dispatcherType = Class.forName(DISPATCHER_CLASS, false, registrar.getClassLoader());
            Object proxy = Proxy.newProxyInstance(
                    dispatcherType.getClassLoader(),
                    new Class<?>[]{dispatcherType},
                    dispatcherInvocation(dispatcher));
            registrar.getMethod("register", boolean.class, dispatcherType).invoke(null, true, proxy);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    public static boolean dispatch(String surfaceType, String action) {
        if (!nativeLoaderActive()) {
            return false;
        }
        try {
            Object result = registrarClass()
                    .getMethod("dispatch", String.class, String.class)
                    .invoke(null, surfaceType, action);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logUnavailable(exception);
            return false;
        }
    }

    public static void registerInputBinding(
            String surfaceType,
            String action,
            Map<String, Object> binding
    ) {
        if (!nativeLoaderActive()) {
            return;
        }
        try {
            registrarClass()
                    .getMethod("registerInputBinding", String.class, String.class, Map.class)
                    .invoke(null, surfaceType, action, binding);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    public static void publishLifecycleEvent(
            String surfaceType,
            String phase,
            String action,
            Map<String, Object> metadata
    ) {
        if (!nativeLoaderActive()) {
            return;
        }
        try {
            registrarClass()
                    .getMethod("publishLifecycleEvent", String.class, String.class, String.class, Map.class)
                    .invoke(null, surfaceType, phase, action, metadata);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logUnavailable(exception);
        }
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private static Class<?> registrarClass() throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = AshfallNativeClientRouteBridge.class.getClassLoader();
        }
        return Class.forName(REGISTRAR_CLASS, false, loader);
    }

    private static InvocationHandler dispatcherInvocation(SurfaceDispatcher dispatcher) {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            if ("dispatch".equals(method.getName()) && args != null && args.length == 3) {
                return dispatcher.dispatch(
                        String.valueOf(args[0]),
                        String.valueOf(args[1]),
                        stringObjectMap(args[2]));
            }
            throw new UnsupportedOperationException("Unsupported Ashfall native route dispatcher method: "
                    + method.getName());
        };
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "AshfallNativeClientRouteBridge.SurfaceDispatcherProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    private static Map<String, Object> stringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    private static void logUnavailable(Throwable exception) {
        if (LOGGED_UNAVAILABLE.compareAndSet(false, true)) {
            EchoAshfallProtocol.LOGGER.debug(
                    "ECHO Ashfall Native client route registry unavailable; continuing without Native route binding.",
                    exception);
        }
    }

    @FunctionalInterface
    public interface SurfaceDispatcher {
        boolean dispatch(String surfaceType, String action, Map<String, Object> actionMetadata);
    }
}
