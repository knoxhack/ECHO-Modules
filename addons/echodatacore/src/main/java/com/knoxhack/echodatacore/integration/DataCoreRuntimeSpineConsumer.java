package com.knoxhack.echodatacore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeSpineBus;
import com.echoplatform.echocore.api.EchoRuntimeSpineEvent;
import com.echoplatform.echocore.api.IDataView;
import com.knoxhack.echodatacore.DataCoreBuiltinKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Persists shared runtime spine events through DataCore's normal dirty/sync path.
 */
public final class DataCoreRuntimeSpineConsumer {
    private static boolean registered;

    private DataCoreRuntimeSpineConsumer() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        registerListeners();
    }

    public static synchronized void registerForTests() {
        registered = true;
        registerListeners();
    }

    private static void registerListeners() {
        EchoRuntimeSpineBus.register(DataCoreRuntimeSpineConsumer::onRuntimeSpineEvent);
    }

    private static void onRuntimeSpineEvent(EchoRuntimeSpineEvent event) {
        if (event == null || event.player() == null) {
            return;
        }
        ServerPlayer player = event.player();
        IDataView playerData = EchoCoreServices.playerData(player);
        playerData.set(DataCoreBuiltinKeys.RUNTIME_SPINE_LAST_EVENT, event.eventId().toString());
        playerData.set(DataCoreBuiltinKeys.RUNTIME_SPINE_LAST_SOURCE, event.sourceModule());
        increment(playerData, DataCoreBuiltinKeys.RUNTIME_SPINE_EVENTS);
        if (player.level() instanceof ServerLevel level) {
            increment(EchoCoreServices.worldData(level), DataCoreBuiltinKeys.RUNTIME_SPINE_WORLD_EVENTS);
        }
    }

    private static void increment(IDataView view, com.echoplatform.echocore.api.IDataKey<Long> key) {
        if (view == null || key == null) {
            return;
        }
        Long current = view.get(key);
        view.set(key, Math.max(0L, current == null ? 1L : current + 1L));
    }
}
