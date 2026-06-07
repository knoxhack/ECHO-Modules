package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;

public final class EchoDifficultyCoreEvents {
    private static volatile boolean serverDifficultyPolicyHookAttached;

    private EchoDifficultyCoreEvents() {
    }

    public static synchronized void attach() {
        if (serverDifficultyPolicyHookAttached) {
            return;
        }
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoDifficultyCoreEvents::onServerStarting);
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
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echodifficultycore",
                "server_starting",
                gameTick,
                sourceReason);
    }
}
