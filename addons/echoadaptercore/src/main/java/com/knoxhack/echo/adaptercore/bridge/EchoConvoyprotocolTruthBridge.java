package com.knoxhack.echo.adaptercore.bridge;

import com.knoxhack.echo.adaptercore.EchoAdapterCoreSpinePublisher;
import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;

import java.util.Map;

/**
 * Bridges {echoconvoyprotocol} spine events into the AdapterCore truth layer.
 */
public final class EchoConvoyprotocolTruthBridge {
    private static final String SOURCE_MODULE = "echoconvoyprotocol";
    private static boolean registered;

    private EchoConvoyprotocolTruthBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        try {
            Class<?> busClass = Class.forName("com.knoxhack.echocore.api.EchoRuntimeSpineBus");
            Class<?> eventClass = Class.forName("com.knoxhack.echocore.api.EchoRuntimeSpineEvent");

            busClass.getMethod("register", java.util.function.Consumer.class)
                    .invoke(null, (java.util.function.Consumer<Object>) event -> {
                        if (event == null) {
                            return;
                        }
                        try {
                            Object sourceModule = eventClass.getMethod("sourceModule").invoke(event);
                            if (!"echoconvoyprotocol".equals(sourceModule)) {
                                return;
                            }
                            Object eventId = eventClass.getMethod("eventId").invoke(event);
                            Object player = eventClass.getMethod("player").invoke(event);
                            Object targetId = eventClass.getMethod("targetId").invoke(event);
                            int amount = ((Number) eventClass.getMethod("amount").invoke(event)).intValue();
                            Object context = eventClass.getMethod("context").invoke(event);

                            String playerId = player == null ? "" : String.valueOf(
                                    player.getClass().getMethod("getUUID").invoke(player));
                            String canonicalEvent = EchoCanonicalContentIds.normalizeEventName(String.valueOf(eventId));
                            String target = targetId != null ? String.valueOf(targetId) : "";

                            EchoAdapterCoreSpinePublisher.instance().publish(
                                    SOURCE_MODULE,
                                    canonicalEvent,
                                    playerId.isBlank() ? null : playerId,
                                    target,
                                    amount,
                                    context instanceof Map ? (Map<String, String>) context : Map.of(),
                                    "echoconvoyprotocol:" + canonicalEvent + ":" + (playerId.isBlank() ? "world" : playerId) + ":" + System.currentTimeMillis());
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            // EchoRuntimeSpineBus not available
        }
    }
}
