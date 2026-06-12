package com.knoxhack.echo.adaptercore.bridge;

import com.knoxhack.echo.adaptercore.EchoAdapterCoreSpinePublisher;
import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;

import java.util.Map;

/**
 * Bridges {@code MissionRuntimeBus} events into the AdapterCore truth layer.
 *
 * <p>Every mission lifecycle event (started, objective progressed, completed,
 * reward claimed, chapter unlocked) is published through
 * {@link EchoAdapterCoreSpinePublisher} so the mutation ledger records it.
 *
 * <p>Install once per server runtime:
 * <pre>
 * EchoMissionCoreTruthBridge.register();
 * </pre>
 */
public final class EchoMissionCoreTruthBridge {
    private static final String SOURCE_MODULE = "echomissioncore";
    private static boolean registered;

    private EchoMissionCoreTruthBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Bridge MissionRuntimeBus events to the truth layer via reflection
        // to avoid a compile-time dependency on echocore's mission API.
        try {
            Class<?> busClass = Class.forName("com.echoplatform.echocore.api.mission.MissionRuntimeBus");
            Class<?> eventClass = Class.forName("com.echoplatform.echocore.api.mission.MissionRuntimeEvent");

            busClass.getMethod("register", java.util.function.Consumer.class)
                    .invoke(null, (java.util.function.Consumer<Object>) event -> {
                        if (event == null) {
                            return;
                        }
                        try {
                            Object missionId = eventClass.getMethod("missionId").invoke(event);
                            Object objectiveId = eventClass.getMethod("objectiveId").invoke(event);
                            Object player = eventClass.getMethod("player").invoke(event);
                            String eventType = String.valueOf(eventClass.getMethod("eventType").invoke(event));
                            int amount = ((Number) eventClass.getMethod("amount").invoke(event)).intValue();

                            String playerId = player == null ? "" : String.valueOf(
                                    player.getClass().getMethod("getUUID").invoke(player));

                            String canonicalEvent = mapEventType(eventType);
                            String targetId = objectiveId != null ? String.valueOf(objectiveId)
                                    : missionId != null ? String.valueOf(missionId) : "";

                            EchoAdapterCoreSpinePublisher.instance().publish(
                                    SOURCE_MODULE,
                                    canonicalEvent,
                                    playerId.isBlank() ? null : playerId,
                                    targetId,
                                    amount,
                                    Map.of(
                                            "missionId", String.valueOf(missionId),
                                            "objectiveId", objectiveId == null ? "" : String.valueOf(objectiveId),
                                            "eventType", eventType),
                                    "missioncore:" + eventType + ":" + (playerId.isBlank() ? "world" : playerId) + ":" + System.currentTimeMillis());
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            // MissionRuntimeBus not available in this classpath
        }
    }

    private static String mapEventType(String eventType) {
        return switch (eventType) {
            case "MISSION_STARTED" -> EchoCanonicalContentIds.EVENT_PLAYER_SPAWNED;
            case "MISSION_COMPLETED" -> EchoCanonicalContentIds.EVENT_MISSION_COMPLETED;
            case "OBJECTIVE_PROGRESSED" -> EchoCanonicalContentIds.EVENT_MISSION_OBJECTIVE_COMPLETED;
            case "REWARD_CLAIMED" -> EchoCanonicalContentIds.EVENT_PLAYER_ITEM_COLLECTED;
            case "CHAPTER_UNLOCKED" -> EchoCanonicalContentIds.EVENT_ASHFALL_PERK_UNLOCKED;
            default -> EchoCanonicalContentIds.EVENT_MISSION_OBJECTIVE_COMPLETED;
        };
    }
}
