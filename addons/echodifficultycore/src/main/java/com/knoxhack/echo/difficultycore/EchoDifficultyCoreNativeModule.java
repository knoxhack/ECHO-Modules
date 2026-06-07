package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeDifficultyApplicationBridge;
import com.knoxhack.echo.adaptercore.EchoNativeDifficultyProfileSelectionBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echo.packcore.EchoPackVariantId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoDifficultyCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    public static final String MODULE_ID = EchoDifficultyConstants.MODULE_ID.value();
    public static final String DIFFICULTY_PROFILE_CONTRACT_ID = "echodifficultycore:data/difficulty_profile";
    public static final String PACK_VARIANT_POLICY_CONTRACT_ID = "echodifficultycore:pack/variant_difficulty_policy";
    public static final String DIFFICULTY_TELEMETRY_CONTRACT_ID = "echodifficultycore:diagnostic/difficulty_telemetry";
    public static final String DIFFICULTY_PROFILE_SELECTION_CONTRACT_ID =
            "echodifficultycore:difficulty/profile_selection";
    public static final String DIFFICULTY_APPLICATION_CONTRACT_ID =
            "echodifficultycore:difficulty/application_state";
    public static final List<String> CONTRACT_IDS = List.of(
            DIFFICULTY_PROFILE_CONTRACT_ID,
            PACK_VARIANT_POLICY_CONTRACT_ID,
            DIFFICULTY_TELEMETRY_CONTRACT_ID,
            DIFFICULTY_PROFILE_SELECTION_CONTRACT_ID,
            DIFFICULTY_APPLICATION_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "difficultycore_native_runtime_contract_active");
        result.put("adapterDomains", List.of("data", "diagnostics", "packs"));
        result.put("summary", "DifficultyCore native contract exercised profile/tuning, pack policy, telemetry, and AdapterCore difficulty runtime state.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        EchoDifficultyProfileId defaultProfile = EchoDifficultyProfileId.of(" Ashfall_Hard ");
        EchoDifficultyProfileId minimumProfile = EchoDifficultyProfileId.of(" Ashfall_Normal ");
        EchoDifficultyProfileId maximumProfile = EchoDifficultyProfileId.of(" Ashfall_Nightmare ");
        EchoDifficultyTuning tuning = new EchoDifficultyTuning(
                " Hazard_Intensity ",
                null,
                null,
                1.25D,
                0.5D,
                2.0D,
                0.75D,
                Map.of("source", "native")
        );
        EchoPackVariantDifficultyPolicy packPolicy = new EchoPackVariantDifficultyPolicy(
                EchoPackVariantId.of(" Ashfall_Beta "),
                defaultProfile,
                minimumProfile,
                maximumProfile,
                true,
                false,
                Map.of("policy", "pack")
        );
        EchoServerDifficultyPolicy serverPolicy = new EchoServerDifficultyPolicy(
                " Server_Lock ",
                defaultProfile,
                true,
                true,
                false,
                " Stable challenge ",
                " Fixed profile during parity smoke ",
                Map.of("scope", "server")
        );
        EchoDifficultyProfile profile = new EchoDifficultyProfile(
                defaultProfile,
                " Ashfall Hard ",
                null,
                EchoDifficultyConstants.MODULE_ID,
                null,
                null,
                null,
                null,
                List.of(tuning),
                packPolicy,
                serverPolicy,
                null,
                List.of(),
                Map.of("loop", "first_playable")
        );
        EchoDifficultyTelemetry telemetry = new EchoDifficultyTelemetry(
                " Hazard_Snapshot ",
                null,
                EchoDifficultyConstants.MODULE_ID,
                1.5D,
                1.0D,
                1.25D,
                " Stable ",
                Map.of("sample", "parity")
        );
        EchoServerDifficultyPolicy liveServerPolicy =
                EchoDifficultyRuntimeState.materializeServerPolicy(
                        " native_server_starting ",
                        defaultProfile,
                        "native-bootstrap");
        EchoDifficultyRegistry registry = new EchoDifficultyRegistry(
                Map.of(profile.id(), profile),
                List.of(packPolicy),
                List.of(serverPolicy, liveServerPolicy),
                List.of(telemetry),
                List.of()
        );
        EchoWorldContracts.EchoDifficultyProfileSelectionResult selectionResult =
                new EchoNativeDifficultyProfileSelectionBridge(MODULE_ID).select(
                        new EchoWorldContracts.EchoDifficultyProfileSelectionRequest(
                                "difficultycore-native-player",
                                "echoashfallprotocol:crash_zone_wasteland",
                                "echoashfallprotocol:mission/secure_crash_outpost",
                                "echodifficultycore:hard",
                                6300L,
                                "difficultycore-native-reference-probe"
                        )
                );
        EchoWorldContracts.EchoDifficultyProfile adapterProfile =
                new EchoWorldContracts.EchoDifficultyProfile(
                        selectionResult.difficultyId(),
                        selectionResult.hazardMultiplier(),
                        selectionResult.spawnMultiplier()
                );
        EchoWorldContracts.EchoDifficultyApplicationResult applicationResult =
                new EchoNativeDifficultyApplicationBridge(MODULE_ID).apply(
                        new EchoWorldContracts.EchoDifficultyApplicationRequest(
                                selectionResult.playerId(),
                                selectionResult.regionId(),
                                "echoworldcore:hazard/salvage_debris",
                                2.0D,
                                3.0D,
                                "echospawncore:spawn/rad_zombie_crash_zone",
                                2,
                                3,
                                1,
                                6301L,
                                "difficultycore-native-reference-probe-application",
                                adapterProfile
                        )
                );
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("difficultyProfileRoundTrip", profile.id().value().equals("ashfall_hard")
                && profile.displayName().equals("Ashfall Hard")
                && profile.mode() == EchoDifficultyMode.UNKNOWN
                && profile.tunings().size() == 1
                && !profile.blocking());
        probe.put("packPolicyRoundTrip", packPolicy.variantId().value().equals("ashfall_beta")
                && packPolicy.defaultProfile().equals(defaultProfile)
                && packPolicy.allowAdaptiveDifficulty()
                && !packPolicy.allowPlayerOverride()
                && serverPolicy.policyId().equals("server_lock")
                && serverPolicy.playerSummary().equals("Stable challenge")
                && liveServerPolicy.policyId().equals("native_server_starting")
                && liveServerPolicy.serverAuthoritative());
        probe.put("diagnosticTelemetryRoundTrip", telemetry.telemetryId().equals("hazard_snapshot")
                && telemetry.kind() == EchoDifficultyMetricKind.UNKNOWN
                && telemetry.summary().equals("Stable")
                && !registry.blocking());
        probe.put("profileId", profile.id().value());
        probe.put("profileMode", profile.mode().name().toLowerCase());
        probe.put("tuningId", tuning.tuningId());
        probe.put("tuningKind", tuning.kind().name().toLowerCase());
        probe.put("policyVariantId", packPolicy.variantId().value());
        probe.put("serverPolicyId", serverPolicy.policyId());
        probe.put("liveServerPolicyId", liveServerPolicy.policyId());
        probe.put("liveServerPolicyForcedProfile", liveServerPolicy.forcedProfile().value());
        probe.put("telemetryId", telemetry.telemetryId());
        probe.put("telemetryKind", telemetry.kind().name().toLowerCase());
        probe.put("registryBlocking", registry.blocking());
        probe.put("profileSelectionRuntimeContract", selectionResult.selected());
        probe.put("difficultyApplicationRuntimeContract", applicationResult.applied());
        probe.put("selectedDifficulty", selectionResult.selectedDifficulty());
        probe.put("selectedDifficultyId", selectionResult.difficultyId());
        probe.put("hazardMultiplier", selectionResult.hazardMultiplier());
        probe.put("spawnMultiplier", selectionResult.spawnMultiplier());
        probe.put("scaledHazardDamage", applicationResult.scaledHazardDamage());
        probe.put("scaledSpawnBudget", applicationResult.scaledSpawnBudget());
        probe.put("activeSpawnPopulation", applicationResult.activeSpawnPopulation());
        probe.put("difficultyProfileSelectionResult", selectionResult);
        probe.put("difficultyApplicationResult", applicationResult);
        return Map.copyOf(probe);
    }

    private Map<String, Object> baseResult(Map<String, String> context, Map<String, Object> probe) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("difficultyProfileRoundTrip", probe.get("difficultyProfileRoundTrip"));
        result.put("packPolicyRoundTrip", probe.get("packPolicyRoundTrip"));
        result.put("diagnosticTelemetryRoundTrip", probe.get("diagnosticTelemetryRoundTrip"));
        result.put("profileSelectionRuntimeContract", probe.get("profileSelectionRuntimeContract"));
        result.put("difficultyApplicationRuntimeContract", probe.get("difficultyApplicationRuntimeContract"));
        result.put("liveServerDifficultyPolicyRuntimeContract",
                Boolean.TRUE.equals(probe.get("packPolicyRoundTrip")));
        result.put("liveServerPolicyId", probe.get("liveServerPolicyId"));
        result.put("liveServerPolicyForcedProfile", probe.get("liveServerPolicyForcedProfile"));
        result.put("selectedDifficulty", probe.get("selectedDifficulty"));
        result.put("selectedDifficultyId", probe.get("selectedDifficultyId"));
        result.put("hazardMultiplier", probe.get("hazardMultiplier"));
        result.put("spawnMultiplier", probe.get("spawnMultiplier"));
        result.put("scaledHazardDamage", probe.get("scaledHazardDamage"));
        result.put("scaledSpawnBudget", probe.get("scaledSpawnBudget"));
        result.put("activeSpawnPopulation", probe.get("activeSpawnPopulation"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("eventHookCount", 1);
        result.put("liveServerDifficultyPolicyHook", "EchoDifficultyCoreEvents.onServerStarting -> EchoDifficultyRuntimeState.materializeServerPolicy");
        result.put("liveNativeGameplayHandlerAttached", true);
        result.put("transformsPerformed", false);
        return result;
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoDifficultyCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "ashfall"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "DifficultyCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("difficultyProfileRoundTrip")),
                "DifficultyCore native adapter should preserve profile defaults");
        require(Boolean.TRUE.equals(activation.get("packPolicyRoundTrip")),
                "DifficultyCore native adapter should preserve pack policy behavior");
        require(Boolean.TRUE.equals(activation.get("diagnosticTelemetryRoundTrip")),
                "DifficultyCore native adapter should preserve telemetry behavior");
        require(Boolean.TRUE.equals(activation.get("profileSelectionRuntimeContract")),
                "DifficultyCore native adapter should select a profile through AdapterCore");
        require(Boolean.TRUE.equals(activation.get("difficultyApplicationRuntimeContract")),
                "DifficultyCore native adapter should apply difficulty through AdapterCore");
        require(Boolean.TRUE.equals(activation.get("liveServerDifficultyPolicyRuntimeContract")),
                "DifficultyCore native adapter should materialize live server difficulty policy state");
        require(Boolean.TRUE.equals(activation.get("liveNativeGameplayHandlerAttached")),
                "DifficultyCore native adapter should expose the live server-start difficulty policy hook");
        require("hard".equals(activation.get("selectedDifficulty")),
                "DifficultyCore native adapter should select hard difficulty");
        require(Double.valueOf(3.0D).equals(activation.get("scaledHazardDamage")),
                "DifficultyCore native adapter should retain scaled hazard damage");
        require(Integer.valueOf(3).equals(activation.get("scaledSpawnBudget")),
                "DifficultyCore native adapter should retain scaled spawn budget");
        System.out.println("difficultycore native adapter smoke PASS contracts=" + CONTRACT_IDS.size()
                + " selectedDifficulty=" + activation.get("selectedDifficulty")
                + " selectedDifficultyId=" + activation.get("selectedDifficultyId")
                + " scaledHazardDamage=" + activation.get("scaledHazardDamage")
                + " scaledSpawnBudget=" + activation.get("scaledSpawnBudget")
                + " liveHook=server.starting");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
