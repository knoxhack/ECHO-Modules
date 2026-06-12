package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;

import java.util.Map;

/**
 * Per-module spine bus publisher that routes events through both
 * {@link EchoRuntimeSpineBus} (legacy) and {@link EchoAdapterCoreSpinePublisher}
 * (truth layer) with correct {@code sourceModule} attribution.
 *
 * <p>Usage:
 * <pre>
 * EchoModuleSpineBusPublisher pub = EchoModuleSpineBusPublisher.forModule("echoindustrialnexus");
 * pub.publishEvent("machine.tick", playerId, machineId, 1, Map.of("flux", "1200"));
 * </pre>
 */
public final class EchoModuleSpineBusPublisher {
    private final String sourceModule;

    private EchoModuleSpineBusPublisher(String sourceModule) {
        this.sourceModule = AdapterContractGuards.requireText(sourceModule, "source module");
    }

    public static EchoModuleSpineBusPublisher forModule(String sourceModule) {
        return new EchoModuleSpineBusPublisher(sourceModule);
    }

    /**
     * Publishes an event through both the legacy spine bus and the truth layer.
     *
     * @return the {@link NativeResult} from the truth-layer publisher
     */
    public NativeResult publishEvent(
            String eventId,
            String playerId,
            String targetId,
            int amount,
            Map<String, String> context) {

        // Publish through truth layer first
        NativeResult truthResult = EchoAdapterCoreSpinePublisher.instance().publish(
                sourceModule,
                eventId,
                playerId,
                targetId,
                amount,
                context,
                sourceModule + ":" + eventId + ":" + (playerId == null ? "world" : playerId) + ":" + System.currentTimeMillis());

        // Also publish to legacy EchoRuntimeSpineBus for backward compatibility
        publishToLegacySpineBus(eventId, playerId, targetId, amount, context);

        return truthResult;
    }

    private void publishToLegacySpineBus(
            String eventId,
            String playerId,
            String targetId,
            int amount,
            Map<String, String> context) {
        try {
            Class<?> busClass = Class.forName("com.echoplatform.echocore.api.EchoRuntimeSpineBus");
            Class<?> eventClass = Class.forName("com.echoplatform.echocore.api.EchoRuntimeSpineEvent");

            Object event = eventClass
                    .getMethod("of", String.class, String.class, String.class, String.class, int.class, Map.class)
                    .invoke(null, sourceModule, eventId, playerId, targetId, amount, context);

            busClass.getMethod("publish", eventClass).invoke(null, event);
        } catch (Exception e) {
            // EchoRuntimeSpineBus not available — silently skip legacy publish
        }
    }
}
