package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionHandler;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges {@code EchoRuntimeSpineBus} events into the AdapterCore truth layer.
 *
 * <p>Any module can publish a spine event through this publisher.
 * The publisher converts the event into an {@link EchoRuntimeAction},
 * dispatches it through {@link EchoRuntimeActionDispatcher},
 * records the result in {@link EchoRuntimeMutationLedger},
 * and falls back to the legacy {@code EchoRuntimeSpineBus} for listeners
 * that have not yet been ported to AdapterCore handlers.
 *
 * <p>Acceptance: the returned {@link NativeResult} tells the truth about
 * whether any listener or host mutated real state.
 */
public final class EchoAdapterCoreSpinePublisher {
    public static final String RUNTIME_HOST_ID = "echoadaptercore:spine_publisher";
    public static final String ACTION_SPINE_EVENT = "spine.event";

    private static final EchoAdapterCoreSpinePublisher INSTANCE = new EchoAdapterCoreSpinePublisher();
    private static boolean registered;

    private final EchoRuntimeActionDispatcher dispatcher;

    private EchoAdapterCoreSpinePublisher() {
        this.dispatcher = EchoRuntimeActionDispatcher.global();
    }

    public static EchoAdapterCoreSpinePublisher instance() {
        return INSTANCE;
    }

    /**
     * Registers the built-in spine-event action handler on the global dispatcher.
     * Safe to call multiple times; only the first call has effect.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoRuntimeActionDispatcher.global().registerAction(
                RUNTIME_HOST_ID,
                ACTION_SPINE_EVENT,
                new SpineEventHandler());
    }

    /**
     * Publishes a spine event through the truth layer.
     *
     * @param sourceModule  the module that originated the event
     * @param eventId       canonical event id (should come from {@link EchoCanonicalContentIds})
     * @param playerId      the player UUID string, or null for world/global events
     * @param targetId      the target content id (item, block, entity, region, etc.)
     * @param amount        quantity associated with the event
     * @param context       extra string-keyed context
     * @param idempotencyKey idempotency key for this publication
     * @return a {@link NativeResult} with truthful mutation status
     */
    public NativeResult publish(
            String sourceModule,
            String eventId,
            String playerId,
            String targetId,
            int amount,
            Map<String, String> context,
            String idempotencyKey) {
        register();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceModule", AdapterContractGuards.optionalText(sourceModule));
        payload.put("eventId", EchoCanonicalContentIds.normalizeEventName(eventId));
        payload.put("targetId", EchoCanonicalContentIds.normalizeContentId(targetId == null ? "" : targetId));
        payload.put("amount", Math.max(1, amount));
        if (context != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                payload.put(entry.getKey(), entry.getValue());
            }
        }

        NativeMutationContext mutationContext = new NativeMutationContext(
                AdapterContractGuards.optionalText(sourceModule),
                "",
                AdapterContractGuards.requireText(idempotencyKey, "spine publisher idempotency key"),
                "server",
                System.currentTimeMillis(),
                Map.of("publisher", "EchoAdapterCoreSpinePublisher"));

        NativePlayerRef playerRef = playerId == null || playerId.isBlank()
                ? null
                : new NativePlayerRef(playerId);

        EchoRuntimeAction action = new EchoRuntimeAction(
                ACTION_SPINE_EVENT,
                RUNTIME_HOST_ID,
                Map.copyOf(payload),
                playerRef,
                "",
                null,
                null,
                mutationContext);

        NativeResult result = dispatcher.dispatch(action);

        // If the dispatcher could not find a handler, fall back to legacy spine bus
        // but still record the fact that we fell back as a truth-layer entry.
        if (result.resultStatus() == NativeResultStatus.UNSUPPORTED) {
            boolean legacyPublished = publishLegacyFallback(payload, mutationContext);
            result = legacyPublished
                    ? NativeResult.noop("Legacy spine bus accepted event; no AdapterCore handler claimed mutation.",
                            Map.copyOf(payload))
                    : NativeResult.failed("Legacy spine bus rejected event.", Map.copyOf(payload));
        }

        return result;
    }

    /**
     * Convenience overload that auto-generates an idempotency key.
     */
    public NativeResult publish(
            String sourceModule,
            String eventId,
            String playerId,
            String targetId,
            int amount,
            Map<String, String> context) {
        String idempotencyKey = sourceModule + ":" + eventId + ":" + (playerId == null ? "world" : playerId) + ":" + System.currentTimeMillis();
        return publish(sourceModule, eventId, playerId, targetId, amount, context, idempotencyKey);
    }

    /**
     * Publishes without requiring a player (world/global events).
     */
    public NativeResult publishWorldEvent(
            String sourceModule,
            String eventId,
            String targetId,
            int amount,
            Map<String, String> context,
            String idempotencyKey) {
        return publish(sourceModule, eventId, null, targetId, amount, context, idempotencyKey);
    }

    private static boolean publishLegacyFallback(Map<String, Object> payload, NativeMutationContext context) {
        // This is a no-op placeholder. In a real build, this would call:
        // EchoRuntimeSpineBus.publish(...)
        // We avoid the direct dependency here to keep the adaptercore build boundary clean.
        // The NeoForge adapter can bridge this at runtime.
        return false;
    }

    /**
     * Built-in handler for spine events. Dispatches to registered consumers and
     * produces a truthful {@link EchoRuntimeActionOutcome}.
     */
    private static final class SpineEventHandler implements EchoRuntimeActionHandler {
        @Override
        public EchoRuntimeActionOutcome dispatch(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
            Map<String, Object> payload = action.inputPayload();
            String eventId = String.valueOf(payload.getOrDefault("eventId", ""));
            String sourceModule = String.valueOf(payload.getOrDefault("sourceModule", ""));

            Map<String, Object> before = Map.of(
                    "eventId", eventId,
                    "sourceModule", sourceModule,
                    "spineConsumersActive", registeredConsumerCount());

            // Publish to the legacy bus through a runtime-resolved bridge if available.
            boolean legacyPublished = tryLegacyPublish(payload);

            Map<String, Object> after = Map.of(
                    "eventId", eventId,
                    "sourceModule", sourceModule,
                    "legacyPublished", legacyPublished);

            // Until consumers are ported to AdapterCore handlers, we treat
            // legacy publication as NOOP because we cannot verify real mutation.
            NativeResult result = legacyPublished
                    ? NativeResult.noop("Spine event published to legacy bus; truth-layer consumers not yet ported.", after)
                    : NativeResult.unsupported("No spine consumers registered for this event.", after);

            return EchoRuntimeActionOutcome.of(before, result, after, false, legacyPublished);
        }

        private static boolean tryLegacyPublish(Map<String, Object> payload) {
            // Resolved at runtime to avoid compile-time dependency on echocore.
            // EchoRuntimeSpineEvent.of(String, Identifier, ServerPlayer, Identifier, int, Map)
            try {
                Class<?> busClass = Class.forName("com.echoplatform.echocore.api.EchoRuntimeSpineBus");
                Class<?> eventClass = Class.forName("com.echoplatform.echocore.api.EchoRuntimeSpineEvent");
                Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");

                String sourceModule = String.valueOf(payload.getOrDefault("sourceModule", "unknown"));
                String eventIdStr = String.valueOf(payload.getOrDefault("eventId", "echocore:runtime_spine_event"));
                String targetIdStr = String.valueOf(payload.getOrDefault("targetId", eventIdStr));
                int amount = payload.get("amount") instanceof Number n ? n.intValue() : 1;

                Object eventId = identifierClass.getMethod("tryParse", String.class).invoke(null, eventIdStr);
                if (eventId == null) {
                    eventId = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                            .invoke(null, "echocore", "runtime_spine_event");
                }
                Object targetId = identifierClass.getMethod("tryParse", String.class).invoke(null, targetIdStr);
                if (targetId == null) {
                    targetId = eventId;
                }

                Object event = eventClass
                        .getMethod("of", String.class, identifierClass,
                                Class.forName("net.minecraft.server.level.ServerPlayer"),
                                identifierClass, int.class, Map.class)
                        .invoke(null, sourceModule, eventId, null, targetId, amount, payload);
                return Boolean.TRUE.equals(
                        busClass.getMethod("publish", eventClass).invoke(null, event));
            } catch (Exception e) {
                return false;
            }
        }

        private static int registeredConsumerCount() {
            try {
                Class<?> busClass = Class.forName("com.echoplatform.echocore.api.EchoRuntimeSpineBus");
                java.lang.reflect.Field listenersField = busClass.getDeclaredField("LISTENERS");
                listenersField.setAccessible(true);
                java.util.List<?> listeners = (java.util.List<?>) listenersField.get(null);
                return listeners == null ? 0 : listeners.size();
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
