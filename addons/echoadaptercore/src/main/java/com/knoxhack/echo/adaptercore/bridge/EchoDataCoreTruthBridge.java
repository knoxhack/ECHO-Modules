package com.knoxhack.echo.adaptercore.bridge;

import com.knoxhack.echo.adaptercore.EchoAdapterCoreSpinePublisher;
import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;

import java.util.Map;

/**
 * Bridges {@code EchoCoreServices} data writes into the AdapterCore truth layer.
 *
 * <p>Install once per server runtime:
 * <pre>
 * EchoDataCoreTruthBridge.register();
 * </pre>
 */
public final class EchoDataCoreTruthBridge {
    private static final String SOURCE_MODULE = "echodatacore";
    private static boolean registered;

    private EchoDataCoreTruthBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Bridge EchoRuntimeSpineBus events that originate from datacore
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
                            if (!"echodatacore".equals(sourceModule) && !"echoworldcore".equals(sourceModule)) {
                                return;
                            }
                            Object eventId = eventClass.getMethod("eventId").invoke(event);
                            Object player = eventClass.getMethod("player").invoke(event);
                            Object targetId = eventClass.getMethod("targetId").invoke(event);
                            int amount = ((Number) eventClass.getMethod("amount").invoke(event)).intValue();

                            String playerId = player == null ? "" : String.valueOf(
                                    player.getClass().getMethod("getUUID").invoke(player));

                            String canonicalEvent = String.valueOf(eventId);
                            String target = targetId != null ? String.valueOf(targetId) : "";

                            EchoAdapterCoreSpinePublisher.instance().publish(
                                    String.valueOf(sourceModule),
                                    canonicalEvent,
                                    playerId.isBlank() ? null : playerId,
                                    target,
                                    amount,
                                    Map.of("source", "datacore_spine_bridge"),
                                    "datacore:" + canonicalEvent + ":" + (playerId.isBlank() ? "world" : playerId) + ":" + System.currentTimeMillis());
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            // EchoRuntimeSpineBus not available
        }
    }
}
