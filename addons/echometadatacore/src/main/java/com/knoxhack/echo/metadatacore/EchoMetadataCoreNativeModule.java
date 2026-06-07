package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoMetadataCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoMetadataConstants.MOD_ID;
    public static final String MODULE_MANIFEST_CONTRACT_ID = "echometadatacore:data/module_manifest";
    public static final String AI_METADATA_CONTRACT_ID = "echometadatacore:data/ai_metadata";
    public static final String METADATA_VALIDATION_CONTRACT_ID = "echometadatacore:diagnostic/metadata_validation";
    public static final String PACK_METADATA_SCAN_CONTRACT_ID = "echometadatacore:pack/metadata_scan";
    public static final List<String> CONTRACT_IDS = List.of(
            MODULE_MANIFEST_CONTRACT_ID,
            AI_METADATA_CONTRACT_ID,
            METADATA_VALIDATION_CONTRACT_ID,
            PACK_METADATA_SCAN_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "metadatacore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics", "packs"));
        result.put("runtimeTargets", List.of("echo_native"));
        result.put("manifestNormalizationRoundTrip", referenceProbe.get("manifestNormalizationRoundTrip"));
        result.put("schemaValidationRoundTrip", referenceProbe.get("schemaValidationRoundTrip"));
        result.put("conflictDetectionRoundTrip", referenceProbe.get("conflictDetectionRoundTrip"));
        result.put("fallbackScanRoundTrip", referenceProbe.get("fallbackScanRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("requiresManifestBridge", true);
        result.put("requiresAiMetadataBridge", true);
        result.put("requiresValidationBridge", true);
        result.put("requiresPackScanBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "MetadataCore native contract exercised manifest normalization, validation, conflict detection, and fallback scan behavior for AdapterCore.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoMetadataCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agent4-metadatacore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "MetadataCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("manifestNormalizationRoundTrip")),
                "MetadataCore native adapter should exercise manifest normalization");
        require(Boolean.TRUE.equals(activation.get("schemaValidationRoundTrip")),
                "MetadataCore native adapter should exercise schema validation");
        require(Boolean.TRUE.equals(activation.get("conflictDetectionRoundTrip")),
                "MetadataCore native adapter should exercise conflict detection");
        require(Boolean.TRUE.equals(activation.get("fallbackScanRoundTrip")),
                "MetadataCore native adapter should exercise fallback scan status");
        System.out.println("metadatacore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoModuleId moduleId = EchoModuleId.of(MODULE_ID);
        EchoModuleManifest manifest = EchoMetadataNormalizer.minimalManifest(moduleId, "", "1.0.0");
        List<EchoMetadataIssue> schemaIssues = EchoMetadataSchemaValidator.requireSchema(
                moduleId,
                Map.of("id", MODULE_ID),
                "addons/echometadatacore/src/main/resources/META-INF/echo.mod.json"
        );
        List<EchoMetadataIssue> conflictIssues = EchoMetadataConflictDetector.idMismatch(
                moduleId,
                "wrongmetadata",
                "addons/echometadatacore/src/main/resources/META-INF/echo.mod.json"
        );
        EchoMetadataStatus fallback = EchoMetadataFallbackResolver.resolve(EchoMetadataStatus.MISSING, true);
        EchoMetadataParseResult parseResult = new EchoMetadataParseResult(
                moduleId,
                EchoMetadataFileKind.MODULE_MANIFEST,
                fallback,
                "addons/echometadatacore/src/main/resources/META-INF/echo.mod.json",
                EchoMetadataFallbackResolver.fallbackUsed(fallback),
                manifest,
                null,
                Map.of("schema", "echo.mod.v1", "id", MODULE_ID),
                List.of()
        );
        EchoMetadataScanResult scan = new EchoMetadataScanResult(
                "Echo",
                "addons",
                List.of(parseResult),
                List.of(),
                Map.of(moduleId, fallback),
                Map.of(),
                List.of()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("manifestNormalizationRoundTrip", manifest.id().equals(moduleId)
                && manifest.name().equals(MODULE_ID)
                && manifest.commonSide()
                && manifest.schema().id().equals(EchoMetadataConstants.SCHEMA_ECHO_MOD_MANIFEST));
        result.put("schemaValidationRoundTrip", schemaIssues.size() == 1
                && schemaIssues.get(0).blocking()
                && schemaIssues.get(0).code().equals("metadata.schema_missing"));
        result.put("conflictDetectionRoundTrip", conflictIssues.size() == 1
                && conflictIssues.get(0).blocking()
                && conflictIssues.get(0).code().equals("metadata.id_mismatch"));
        result.put("fallbackScanRoundTrip", parseResult.valid()
                && parseResult.fallbackUsed()
                && scan.moduleStatuses().get(moduleId) == EchoMetadataStatus.FALLBACK
                && EchoMetadataFallbackResolver.fallbackUsed(fallback));
        result.put("schemaDescriptorCount", EchoMetadataConstants.SCHEMA_DESCRIPTORS.size());
        result.put("schemaIssueCount", schemaIssues.size());
        result.put("conflictIssueCount", conflictIssues.size());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
