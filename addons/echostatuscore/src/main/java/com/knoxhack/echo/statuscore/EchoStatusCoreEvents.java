package com.knoxhack.echo.statuscore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;

public final class EchoStatusCoreEvents {
    private static volatile boolean statusRegistryHookAttached;

    private EchoStatusCoreEvents() {
    }

    public static synchronized void attach() {
        if (statusRegistryHookAttached) {
            return;
        }
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoStatusCoreEvents::onServerStarting);
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
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echostatuscore",
                "server_starting",
                gameTick,
                sourceReason);
    }
}
