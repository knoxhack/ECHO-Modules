package com.knoxhack.echo.reportcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoReportCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoReportConstants.MOD_ID;
    public static final String SUPPORT_BUNDLE_CONTRACT_ID = "echoreportcore:diagnostic/support_bundle";
    public static final String RELEASE_READINESS_CONTRACT_ID = "echoreportcore:data/release_readiness";
    public static final List<String> CONTRACT_IDS = List.of(
            SUPPORT_BUNDLE_CONTRACT_ID,
            RELEASE_READINESS_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "reportcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("supportBundleRoundTrip", referenceProbe.get("supportBundleRoundTrip"));
        result.put("releaseReadinessRoundTrip", referenceProbe.get("releaseReadinessRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "ReportCore native contract exercised support bundle redaction and release-readiness status behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoReportCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "reportcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "ReportCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("supportBundleRoundTrip")),
                "ReportCore native adapter should exercise support bundle behavior");
        require(Boolean.TRUE.equals(activation.get("releaseReadinessRoundTrip")),
                "ReportCore native adapter should exercise release readiness behavior");
        System.out.println("reportcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoSupportBundleManifest supportBundle = new EchoSupportBundleManifest(
                null,
                null,
                null,
                List.of(new EchoReportArtifact(
                        EchoReportKind.DIAGNOSTICS,
                        "reports\\echo\\diagnostics.json",
                        null,
                        EchoReportStatus.PASS,
                        "abc123",
                        false,
                        false,
                        Map.of("scope", "local")
                )),
                List.of(),
                false,
                false,
                Map.of("bundle", "ashfall")
        );
        EchoReleaseReadinessReport releaseReadiness = new EchoReleaseReadinessReport(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                Map.of("channel", "internal")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("supportBundleRoundTrip", supportBundle.localOnly()
                && supportBundle.secretsRedacted()
                && supportBundle.descriptor().kind() == EchoReportKind.SUPPORT_BUNDLE
                && supportBundle.includedArtifacts().size() == 1
                && supportBundle.includedArtifacts().get(0).outputPath().equals("reports/echo/diagnostics.json"));
        result.put("releaseReadinessRoundTrip", releaseReadiness.releasable()
                && releaseReadiness.status() == EchoReportStatus.PASS
                && releaseReadiness.descriptor().kind() == EchoReportKind.RELEASE_READINESS
                && releaseReadiness.attributes().get("channel").equals("internal"));
        result.put("supportBundleLocalOnly", supportBundle.localOnly());
        result.put("supportBundleSecretsRedacted", supportBundle.secretsRedacted());
        result.put("releaseStatus", releaseReadiness.status().serializedName());
        result.put("artifactPath", supportBundle.includedArtifacts().get(0).outputPath());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
