package com.knoxhack.echo.statuscore;

import java.util.function.Consumer;

public final class EchoStatusCoreEvents {
    private static volatile boolean statusRegistryHookAttached;

    private EchoStatusCoreEvents() {
    }

    public static synchronized void attach() {
        if (statusRegistryHookAttached) {
            return;
        }
        registerGameEventHandler(EchoStatusCoreEvents::onServerStarting);
        statusRegistryHookAttached = true;
    }

    public static boolean statusRegistryHookAttached() {
        return statusRegistryHookAttached;
    }

    public static EchoStatusRuntimeState.ActiveStatusRegistry activeRegistry() {
        return EchoStatusRuntimeState.activeRegistry();
    }

    public static void recordAgent7LiveHookForTests() {
        recordAgent7LiveHook(0L, "EchoStatusCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onServerStarting(Object event) {
        recordAgent7LiveHook(0L, "EchoStatusCoreEvents.onServerStarting");
        EchoStatusRuntimeState.materializeServerRegistry(
                serverType(event));
    }

    private static String serverType(Object event) {
        if (event == null) {
            return "server.starting";
        }
        try {
            Object server = event.getClass().getMethod("getServer").invoke(event);
            return server == null ? "server.starting" : server.getClass().getName();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return event.getClass().getName();
        }
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        try {
            Class.forName("com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge")
                    .getMethod("recordExactCallback", String.class, String.class, long.class, String.class)
                    .invoke(null, "echostatuscore", "server_starting", gameTick, sourceReason);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // StatusCore can still run as a native/standalone module before the NeoForge lifecycle bridge exists.
        }
    }

    private static void registerGameEventHandler(Consumer<Object> listener) {
        try {
            Class.forName("com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge")
                    .getMethod("registerGameEventHandler", Consumer.class)
                    .invoke(null, listener);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // The native/standalone source-safety harness does not provide the NeoForge lifecycle bridge.
        }
    }
}
