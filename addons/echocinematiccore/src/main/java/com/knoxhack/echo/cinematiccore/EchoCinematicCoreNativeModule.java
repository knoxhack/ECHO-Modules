package com.knoxhack.echo.cinematiccore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCinematicCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoCinematicConstants.MOD_ID;
    public static final String SEQUENCE_RENDER_CONTRACT_ID = "echocinematiccore:rendering/sequence_contract_normalization";
    public static final String PACING_RENDER_CONTRACT_ID = "echocinematiccore:rendering/pacing_envelope";
    public static final String TRIGGER_UI_CONTRACT_ID = "echocinematiccore:ui/trigger_overlay_contract";
    public static final List<String> CONTRACT_IDS = List.of(
            SEQUENCE_RENDER_CONTRACT_ID,
            PACING_RENDER_CONTRACT_ID,
            TRIGGER_UI_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "cinematiccore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("rendering", "ui_screens"));
        result.put("runtimeTargets", List.of(
                EchoAdapterRuntime.NEOFORGE.serializedName(),
                EchoAdapterRuntime.ECHO_NATIVE.serializedName(),
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE.serializedName()));
        result.put("sequenceRenderRoundTrip", referenceProbe.get("sequenceRenderRoundTrip"));
        result.put("pacingRenderRoundTrip", referenceProbe.get("pacingRenderRoundTrip"));
        result.put("triggerUiRoundTrip", referenceProbe.get("triggerUiRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "CinematicCore native contract exercised sequence/path normalization, pacing envelopes, and trigger UI behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoCinematicCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "cinematiccore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "CinematicCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("sequenceRenderRoundTrip")),
                "CinematicCore native adapter should exercise sequence rendering behavior");
        require(Boolean.TRUE.equals(activation.get("pacingRenderRoundTrip")),
                "CinematicCore native adapter should exercise pacing rendering behavior");
        require(Boolean.TRUE.equals(activation.get("triggerUiRoundTrip")),
                "CinematicCore native adapter should exercise trigger UI behavior");
        System.out.println("cinematiccore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoCinematicSequenceId sequenceId = EchoCinematicSequenceId.of("Ashfall\\Intro_Stinger");
        EchoCinematicTrigger trigger = new EchoCinematicTrigger(
                "Mission\\Started",
                null,
                null,
                false,
                Map.of("surface", "hud")
        );
        EchoCinematicCameraPath path = new EchoCinematicCameraPath(
                "Camera\\Drop_Pod",
                null,
                null,
                true,
                Map.of("rail", "landing")
        );
        EchoCinematicPacing pacing = new EchoCinematicPacing(
                0L,
                80L,
                20L,
                1.8D,
                true,
                Map.of("tempo", "urgent")
        );
        EchoCinematicSequence sequence = new EchoCinematicSequence(
                sequenceId,
                null,
                null,
                trigger,
                path,
                pacing,
                null,
                null,
                true,
                false,
                null,
                Map.of("chapter", "opening")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sequenceRenderRoundTrip", sequence.id().value().equals("ashfall/intro_stinger")
                && sequence.kind() == EchoCinematicSequenceKind.UNKNOWN
                && sequence.cameraPath().pathId().equals("camera/drop_pod")
                && sequence.cameraPath().locksPlayerControl()
                && sequence.storyboardAssets().isEmpty()
                && !sequence.degraded()
                && sequence.screenshotModeAllowed()
                && !sequence.cinematicModeAllowed());
        result.put("pacingRenderRoundTrip", pacing.fadeInTicks() == 0L
                && pacing.holdTicks() == 80L
                && pacing.fadeOutTicks() == 20L
                && pacing.urgency() == 1.0D
                && pacing.skippable()
                && pacing.attributes().get("tempo").equals("urgent"));
        result.put("triggerUiRoundTrip", trigger.triggerId().equals("mission/started")
                && trigger.kind() == EchoCinematicTriggerKind.UNKNOWN
                && trigger.triggerSources().isEmpty()
                && !trigger.repeatable()
                && trigger.attributes().get("surface").equals("hud"));
        result.put("normalizedSequenceId", sequence.id().value());
        result.put("normalizedPathId", path.pathId());
        result.put("normalizedTriggerId", trigger.triggerId());
        result.put("pacingUrgency", pacing.urgency());
        result.put("screenshotModeAllowed", sequence.screenshotModeAllowed());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
