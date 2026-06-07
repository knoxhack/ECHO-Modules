package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.adaptercore.EchoNativeSpawnRuleEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeSpawnZoneStateBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;

public final class EchoSpawnRuntimeState {
    private static final String MODULE_ID = "echospawncore";
    private static volatile LiveSpawnEventState activeSpawnEvent = LiveSpawnEventState.empty();

    private EchoSpawnRuntimeState() {
    }

    public static LiveSpawnEventState activeSpawnEvent() {
        return activeSpawnEvent;
    }

    public static synchronized LiveSpawnEventState materializeFinalizeSpawn(
            String entityTypeId,
            String regionId,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason) {
        String entityId = entityTypeId == null || entityTypeId.isBlank() ? "minecraft:unknown" : entityTypeId.strip();
        String region = regionId == null || regionId.isBlank() ? "echospawncore:live_finalize_region" : regionId.strip();
        String source = sourceReason == null || sourceReason.isBlank() ? "echo_native.finalize_spawn" : sourceReason.strip();
        EchoWorldContracts.EchoSpawnRule spawnRule = new EchoWorldContracts.EchoSpawnRule(
                "echospawncore:spawn/live_finalize",
                entityId,
                region,
                1,
                1.0D);
        EchoWorldContracts.EchoDifficultyProfile difficulty = new EchoWorldContracts.EchoDifficultyProfile(
                "echodifficultycore:normal",
                1.0D,
                1.0D);
        EchoWorldContracts.EchoSpawnRuleEventResult event =
                new EchoNativeSpawnRuleEventBridge(MODULE_ID).plan(
                        new EchoWorldContracts.EchoSpawnRuleEventRequest(
                                "spawncore-live-finalize",
                                region,
                                x,
                                y,
                                z,
                                0,
                                gameTick,
                                source,
                                spawnRule,
                                difficulty));
        EchoWorldContracts.EchoSpawnZoneStateResult zone =
                new EchoNativeSpawnZoneStateBridge(MODULE_ID).persist(
                        new EchoWorldContracts.EchoSpawnZoneStateRequest(
                                "spawncore-live-finalize",
                                "spawncore-live-zone",
                                event));
        LiveSpawnEventState state = new LiveSpawnEventState(event, zone);
        activeSpawnEvent = state;
        return state;
    }

    public record LiveSpawnEventState(
            EchoWorldContracts.EchoSpawnRuleEventResult event,
            EchoWorldContracts.EchoSpawnZoneStateResult zoneState
    ) {
        public static LiveSpawnEventState empty() {
            return new LiveSpawnEventState(null, null);
        }

        public boolean materialized() {
            return event != null
                    && zoneState != null
                    && "SPAWN_ALLOWED".equals(event.eventType())
                    && zoneState.activePopulation() > 0;
        }
    }
}
