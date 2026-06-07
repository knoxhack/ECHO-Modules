package com.knoxhack.echomissioncore.integration;

import com.knoxhack.echocore.api.EchoRuntimeSpineBus;
import com.knoxhack.echocore.api.EchoRuntimeSpineEvent;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reduces generic module runtime events into MissionCore objective progress.
 */
public final class MissionCoreRuntimeSpineConsumer {
    private static final String WORLDCORE_SOURCE = "echoworldcore";
    private static boolean registered;

    private MissionCoreRuntimeSpineConsumer() {
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
        EchoRuntimeSpineBus.register(MissionCoreRuntimeSpineConsumer::onRuntimeSpineEvent);
    }

    private static void onRuntimeSpineEvent(EchoRuntimeSpineEvent event) {
        if (event == null || event.player() == null || WORLDCORE_SOURCE.equals(event.sourceModule())) {
            return;
        }
        ServerPlayer player = event.player();
        Map<String, String> context = new LinkedHashMap<>(event.context());
        context.putIfAbsent("runtime_spine_event", event.eventId().toString());
        context.putIfAbsent("runtime_spine_source", event.sourceModule());
        MissionObjectiveType type = MissionObjectiveType.byId(context.getOrDefault("objective_type", "custom"));
        MissionCoreService.INSTANCE.recordObjective(player, type, event.targetId(), event.amount(), context);
    }
}
