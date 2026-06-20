package com.knoxhack.echo.difficultycore;

import java.util.function.Consumer;

public final class EchoDifficultyCoreEvents {
    private static volatile boolean serverDifficultyPolicyHookAttached;

    private EchoDifficultyCoreEvents() {
    }

    public static synchronized void attach() {
        if (serverDifficultyPolicyHookAttached) {
            return;
        }
        registerGameEventHandler(EchoDifficultyCoreEvents::onServerStarting);
        serverDifficultyPolicyHookAttached = true;
    }

    public static boolean serverDifficultyPolicyHookAttached() {
        return serverDifficultyPolicyHookAttached;
    }

    public static EchoServerDifficultyPolicy activeServerPolicy() {
        return EchoDifficultyRuntimeState.activeServerPolicy();
    }

    public static void recordAgent7LiveHookForTests() {
        recordAgent7LiveHook(0L, "EchoDifficultyCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onServerStarting(Object event) {
        recordAgent7LiveHook(0L, "EchoDifficultyCoreEvents.onServerStarting");
        EchoDifficultyRuntimeState.materializeServerPolicy(
                "server_starting",
                EchoDifficultyProfileId.of("hard"),
                serverType(event));
    }

    private static String serverType(Object event) {
        if (event == null) {
            return "unknown";
        }
        try {
            Object server = event.getClass().getMethod("getServer").invoke(event);
            return server == null ? "unknown" : server.getClass().getName();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return event.getClass().getName();
        }
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        try {
            Class.forName("com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge")
                    .getMethod("recordExactCallback", String.class, String.class, long.class, String.class)
                    .invoke(null, "echodifficultycore", "server_starting", gameTick, sourceReason);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // DifficultyCore can run as a native/standalone module before the NeoForge lifecycle bridge exists.
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
