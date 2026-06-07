package com.knoxhack.echotutorialcore.integration.worldcore;

import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import com.knoxhack.echotutorialcore.server.TutorialHintManager;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class TutorialWorldCoreIntegration {
    private static final Map<UUID, Long> LAST_WORLDCORE_HINT = new ConcurrentHashMap<>();
    private static final long HINT_COOLDOWN_TICKS = 20L * 60L * 3L;
    private static boolean registered;

    private TutorialWorldCoreIntegration() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoWorldRuntimeBus.onRegionEntered(TutorialWorldCoreIntegration::onRegionEntered);
        EchoWorldRuntimeBus.onRegionScanned(TutorialWorldCoreIntegration::onRegionScanned);
        EchoWorldRuntimeBus.onHazardChanged(TutorialWorldCoreIntegration::onHazardChanged);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with WorldCore hazard and region context.");
    }

    private static void onRegionEntered(EchoWorldRuntimeBus.RegionEntered event) {
        if (event.player() == null || event.region() == null) {
            return;
        }
        TutorialCoreApi.reportWorldHazardChanged(event.player(), event.region().definitionId(),
                new LinkedHashSet<>(event.region().hazardIds()));
    }

    private static void onRegionScanned(EchoWorldRuntimeBus.RegionScanned event) {
        if (event.player() == null || event.region() == null) {
            return;
        }
        TutorialCoreApi.reportProgress(event.player(), Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "scanned_region"));
    }

    private static void onHazardChanged(EchoWorldRuntimeBus.HazardChanged event) {
        if (event.player() == null || event.current() == null || event.current().safeZone()
                || !TutorialConfig.ENABLE_HAZARD_WARNINGS.get()) {
            return;
        }
        long now = event.player().level().getGameTime();
        long previous = LAST_WORLDCORE_HINT.getOrDefault(event.player().getUUID(), Long.MIN_VALUE);
        if (now - previous < HINT_COOLDOWN_TICKS) {
            return;
        }
        LAST_WORLDCORE_HINT.put(event.player().getUUID(), now);
        Set<Identifier> hazards = new LinkedHashSet<>(event.current().hazardIds());
        Identifier region = event.current().regionIds().isEmpty() ? null : event.current().regionIds().get(0);
        TutorialCoreApi.reportWorldHazardChanged(event.player(), region, hazards);
        TutorialHintManager.sendChatFallback(event.player(), "World Hazard",
                event.current().summary() + " Check protection, route choice, and recovery supplies.");
    }
}
