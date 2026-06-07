package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoValidationCategory;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoContentCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoContentConstants.MOD_ID;
    public static final String BLOCK_CATALOG_CONTRACT_ID = "echocontentcore:block/content_catalog";
    public static final String ITEM_CATALOG_CONTRACT_ID = "echocontentcore:item/content_catalog";
    public static final String ENTITY_CATALOG_CONTRACT_ID = "echocontentcore:entity/content_catalog";
    public static final String RECIPE_CATALOG_CONTRACT_ID = "echocontentcore:recipe/content_catalog";
    public static final String LOOT_CATALOG_CONTRACT_ID = "echocontentcore:loot/content_catalog";
    public static final String STRUCTURE_CATALOG_CONTRACT_ID = "echocontentcore:structure/content_catalog";
    public static final String CONTENT_REGISTRY_CONTRACT_ID = "echocontentcore:data/content_registry";
    public static final List<String> CONTRACT_IDS = List.of(
            BLOCK_CATALOG_CONTRACT_ID,
            ITEM_CATALOG_CONTRACT_ID,
            ENTITY_CATALOG_CONTRACT_ID,
            RECIPE_CATALOG_CONTRACT_ID,
            LOOT_CATALOG_CONTRACT_ID,
            STRUCTURE_CATALOG_CONTRACT_ID,
            CONTENT_REGISTRY_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "contentcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("blocks", "data", "entities", "items", "loot", "recipes", "structures"));
        result.put("runtimeTargets", List.of("echo_native"));
        result.put("ownerLookupRoundTrip", referenceProbe.get("ownerLookupRoundTrip"));
        result.put("referenceLookupRoundTrip", referenceProbe.get("referenceLookupRoundTrip"));
        result.put("gateAvailabilityRoundTrip", referenceProbe.get("gateAvailabilityRoundTrip"));
        result.put("validationIssueRoundTrip", referenceProbe.get("validationIssueRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("requiresContentRegistryBridge", true);
        result.put("requiresContentReferenceBridge", true);
        result.put("requiresContentGateBridge", true);
        result.put("requiresContentValidationBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "ContentCore native contract exercised content registry, ownership, reference, gate, and validation behavior for AdapterCore.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoContentCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agent4-contentcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "ContentCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("ownerLookupRoundTrip")),
                "ContentCore native adapter should exercise owner lookup behavior");
        require(Boolean.TRUE.equals(activation.get("referenceLookupRoundTrip")),
                "ContentCore native adapter should exercise reference lookup behavior");
        require(Boolean.TRUE.equals(activation.get("gateAvailabilityRoundTrip")),
                "ContentCore native adapter should exercise gate and availability behavior");
        require(Boolean.TRUE.equals(activation.get("validationIssueRoundTrip")),
                "ContentCore native adapter should exercise validation issue behavior");
        System.out.println("contentcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoModuleId moduleId = EchoModuleId.of(MODULE_ID);
        EchoContentId blockId = EchoContentId.of(MODULE_ID, "reference_block");
        EchoContentId itemId = EchoContentId.of(MODULE_ID, "reference_item");
        EchoContentSource source = new EchoContentSource(
                "contentcore.native.reference",
                EchoContentSourceKind.JAVA_REGISTRATION,
                moduleId,
                null,
                "addons/echocontentcore/src/main/java",
                null,
                null,
                null,
                false,
                "Native AdapterCore content reference probe",
                Map.of("contract", CONTENT_REGISTRY_CONTRACT_ID)
        );
        EchoContentOwner blockOwner = owner(blockId, EchoContentKind.BLOCK, moduleId, source, "Reference Block");
        EchoContentOwner itemOwner = owner(itemId, EchoContentKind.ITEM, moduleId, source, "Reference Item");
        EchoContentReference reference = new EchoContentReference(
                "contentcore.reference.block_to_item",
                blockId,
                EchoContentKind.BLOCK,
                itemId,
                EchoContentKind.ITEM,
                moduleId,
                EchoContentAvailability.PRESENT,
                EchoContentReferenceKind.REQUIRES,
                false,
                EchoContentGate.open(),
                source,
                List.of(),
                "Reference item is present.",
                "AdapterCore native content probe resolved a required content reference.",
                Map.of("contract", CONTENT_REGISTRY_CONTRACT_ID)
        );
        EchoContentValidationIssue issue = new EchoContentValidationIssue(
                "contentcore.reference.issue",
                blockId,
                EchoContentKind.BLOCK,
                EchoContentAvailability.PRESENT,
                EchoValidationCategory.CONTENT_REFERENCE,
                null,
                source,
                false,
                "Reference content is valid.",
                "Nonblocking issue row used to exercise validation issue filtering.",
                List.of("reports/echo/adaptercore/module-runtime-gap-audit.json"),
                Map.of("contract", CONTENT_REGISTRY_CONTRACT_ID)
        );
        EchoContentRegistry registry = new EchoContentRegistry(
                "contentcore.native.registry",
                1L,
                List.of(blockOwner, itemOwner),
                List.of(reference),
                List.of(issue),
                Map.of("runtime", "echo_native")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ownerLookupRoundTrip", registry.ownerOf(blockId)
                .map(owner -> owner.contentId().equals(blockId)
                        && owner.kind() == EchoContentKind.BLOCK
                        && !owner.gated())
                .orElse(false));
        result.put("referenceLookupRoundTrip", registry.referencesFrom(blockId).size() == 1
                && registry.referencesTo(itemId).size() == 1
                && !reference.blocking());
        result.put("gateAvailabilityRoundTrip", EchoContentAvailability.PRESENT.available()
                && !EchoContentAvailability.PRESENT.blocking()
                && !EchoContentGate.open().blocksWhenMissing()
                && EchoContentReferenceKind.REQUIRES.blockingWhenUnavailable());
        result.put("validationIssueRoundTrip", registry.issuesFor(blockId).size() == 1
                && !issue.blocking()
                && !registry.hasBlockingIssues());
        result.put("ownerCount", registry.owners().size());
        result.put("referenceCount", registry.references().size());
        result.put("issueCount", registry.issues().size());
        return Map.copyOf(result);
    }

    private static EchoContentOwner owner(
            EchoContentId contentId,
            EchoContentKind kind,
            EchoModuleId moduleId,
            EchoContentSource source,
            String displayName
    ) {
        return new EchoContentOwner(
                contentId,
                kind,
                moduleId,
                null,
                source,
                displayName,
                "AdapterCore native content ownership probe.",
                Set.of(),
                EchoContentGate.open(),
                true,
                Map.of("contract", CONTENT_REGISTRY_CONTRACT_ID)
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
