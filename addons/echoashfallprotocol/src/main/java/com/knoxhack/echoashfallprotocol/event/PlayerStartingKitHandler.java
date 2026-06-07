package com.knoxhack.echoashfallprotocol.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Routes player-login events into the AdapterCore-backed first-spawn runtime.
 */
public final class PlayerStartingKitHandler {
    private PlayerStartingKitHandler() {
    }

    public static void onPlayerLoggedIn(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer player) {
            AshfallAdapterCoreFirstSpawnRuntime.execute(player);
        }
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
