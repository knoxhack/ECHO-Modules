package com.knoxhack.echo.cameracore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCameraCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoCameraConstants.MOD_ID;
    public static final String RENDER_PROFILE_CONTRACT_ID = "echocameracore:rendering/profile_contract_normalization";
    public static final String SHAKE_SAFETY_CONTRACT_ID = "echocameracore:rendering/shake_safety_envelope";
    public static final String INPUT_TARGET_CONTRACT_ID = "echocameracore:input/target_anchor_contract";
    public static final List<String> CONTRACT_IDS = List.of(
            RENDER_PROFILE_CONTRACT_ID,
            SHAKE_SAFETY_CONTRACT_ID,
            INPUT_TARGET_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "cameracore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("input", "rendering"));
        result.put("runtimeTargets", List.of(
                EchoAdapterRuntime.NEOFORGE.serializedName(),
                EchoAdapterRuntime.ECHO_NATIVE.serializedName(),
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE.serializedName()));
        result.put("renderProfileRoundTrip", referenceProbe.get("renderProfileRoundTrip"));
        result.put("shakeSafetyRoundTrip", referenceProbe.get("shakeSafetyRoundTrip"));
        result.put("inputTargetRoundTrip", referenceProbe.get("inputTargetRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "CameraCore native contract exercised camera profile normalization, shake/safety envelopes, and target anchor input behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoCameraCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "cameracore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "CameraCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("renderProfileRoundTrip")),
                "CameraCore native adapter should exercise rendering profile behavior");
        require(Boolean.TRUE.equals(activation.get("shakeSafetyRoundTrip")),
                "CameraCore native adapter should exercise shake and safety behavior");
        require(Boolean.TRUE.equals(activation.get("inputTargetRoundTrip")),
                "CameraCore native adapter should exercise input target anchor behavior");
        System.out.println("cameracore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoCameraProfileId id = EchoCameraProfileId.of("Prime\\Screenshot_Mode");
        EchoCameraShakeProfile shake = new EchoCameraShakeProfile(
                "Ashfall\\Nexus_Burst",
                1.75D,
                3.5D,
                -20L,
                true,
                Map.of("source", "nexus")
        );
        EchoCameraSafetyConstraint safety = new EchoCameraSafetyConstraint(
                false,
                1.4D,
                0.0D,
                true,
                false,
                Map.of("profile", "reduced_motion")
        );
        EchoCameraTargetRef target = new EchoCameraTargetRef(
                null,
                null,
                "  Player Head  ",
                Map.of("input", "look")
        );
        EchoCameraProfile profile = new EchoCameraProfile(
                id,
                null,
                null,
                List.of(target),
                null,
                null,
                shake,
                safety,
                null,
                null,
                Map.of("mode", "screenshot")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("renderProfileRoundTrip", profile.id().value().equals("prime/screenshot_mode")
                && profile.mode() == EchoCameraMode.UNKNOWN
                && !profile.degraded()
                && profile.creatorToolAssets().isEmpty()
                && profile.attributes().get("mode").equals("screenshot"));
        result.put("shakeSafetyRoundTrip", shake.shakeId().equals("ashfall/nexus_burst")
                && shake.intensity() == 1.0D
                && shake.frequency() == 3.5D
                && shake.durationTicks() == 0L
                && safety.maxShakeIntensity() == 1.0D
                && safety.maxFovChange() == 0.0D
                && safety.respectReducedMotion());
        result.put("inputTargetRoundTrip", profile.targets().size() == 1
                && target.kind() == EchoCameraAnchorKind.UNKNOWN
                && target.anchorName().equals("Player Head")
                && target.attributes().get("input").equals("look"));
        result.put("normalizedProfileId", profile.id().value());
        result.put("normalizedShakeId", shake.shakeId());
        result.put("shakeIntensity", shake.intensity());
        result.put("maxFovChange", safety.maxFovChange());
        result.put("targetAnchor", target.anchorName());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
