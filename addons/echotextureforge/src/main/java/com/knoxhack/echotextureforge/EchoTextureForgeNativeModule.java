package com.knoxhack.echotextureforge;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import com.knoxhack.echotextureforge.api.prompt.TexturePromptTemplate;
import com.knoxhack.echotextureforge.api.report.TextureAuditIssue;
import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.scan.TextureValidationRules;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureResolution;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureSpecRegistry;
import com.knoxhack.echotextureforge.api.spec.TextureSpecStatus;
import com.knoxhack.echotextureforge.api.spec.TextureStyleFamily;
import com.knoxhack.echotextureforge.client.screen.TextureForgeDashboardPlaceholder;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoTextureForgeNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echotextureforge";
    public static final String SPEC_REGISTRY_CONTRACT_ID = "echotextureforge:assets/spec_registry";
    public static final String PROMPT_EXPORT_CONTRACT_ID = "echotextureforge:assets/prompt_export";
    public static final String REVIEW_STATE_CONTRACT_ID = "echotextureforge:data/review_state";
    public static final String TEXTURE_AUDIT_CONTRACT_ID = "echotextureforge:diagnostic/texture_audit";
    public static final String DASHBOARD_CONTRACT_ID = "echotextureforge:ui/dashboard";
    public static final List<String> CONTRACT_IDS = List.of(
            SPEC_REGISTRY_CONTRACT_ID,
            PROMPT_EXPORT_CONTRACT_ID,
            REVIEW_STATE_CONTRACT_ID,
            TEXTURE_AUDIT_CONTRACT_ID,
            DASHBOARD_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "textureforge_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("assets", "data", "diagnostics", "ui_screens"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("specRegistryRoundTrip", referenceProbe.get("specRegistryRoundTrip"));
        result.put("promptExportRoundTrip", referenceProbe.get("promptExportRoundTrip"));
        result.put("reviewStateRoundTrip", referenceProbe.get("reviewStateRoundTrip"));
        result.put("textureAuditRoundTrip", referenceProbe.get("textureAuditRoundTrip"));
        result.put("dashboardSurfaceResolved", referenceProbe.get("dashboardSurfaceResolved"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "TextureForge native contract exercised spec registry, prompt template, review-state enum, audit report, validation, and dashboard surface behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoTextureForgeNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agent4-textureforge-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "TextureForge native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("specRegistryRoundTrip")),
                "TextureForge native adapter should exercise spec registry behavior");
        require(Boolean.TRUE.equals(activation.get("promptExportRoundTrip")),
                "TextureForge native adapter should exercise prompt export behavior");
        require(Boolean.TRUE.equals(activation.get("textureAuditRoundTrip")),
                "TextureForge native adapter should exercise audit behavior");
        require(Boolean.TRUE.equals(activation.get("dashboardSurfaceResolved")),
                "TextureForge native adapter should resolve dashboard surface behavior");
        System.out.println("textureforge native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        TextureSpec itemSpec = TextureSpec.builder("EchoTextureForge", "Status Lens", TextureKind.ITEM)
                .displayName("Status Lens")
                .styleFamily(TextureStyleFamily.ECHO_CYBERGLASS)
                .colorPaletteHints(List.of("signal cyan", "dark glass"))
                .silhouetteNotes("round lens with a clear notch")
                .minecraftReadabilityNotes("must read as a small icon")
                .promptPriority(7)
                .status(TextureSpecStatus.MISSING)
                .build();
        TextureSpec blockSpec = TextureSpec.block("echotextureforge", "preview_bench");
        TextureSpecRegistry registry = new TextureSpecRegistry();
        registry.register(itemSpec);
        registry.register(blockSpec);
        String prompt = TexturePromptTemplate.singleTexturePrompt(itemSpec);
        TextureAuditIssue issue = new TextureAuditIssue(
                TextureAuditSeverity.WARNING,
                "MISSING_TEXTURE",
                itemSpec.namespace(),
                itemSpec.assetId(),
                itemSpec.assetKind(),
                itemSpec.outputPath(),
                "Generated texture has not been staged yet."
        );
        TextureAuditReport report = new TextureAuditReport(
                Instant.EPOCH,
                Path.of("."),
                Path.of("build/textureforge/reports"),
                1,
                1,
                1,
                registry.size(),
                0,
                1,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                Map.of(TextureAuditSeverity.WARNING, 1),
                List.of(issue),
                registry.all(),
                List.of(Path.of("build/textureforge/prompts/master_codex_texture_prompts.md")),
                List.of(Path.of("build/textureforge/reports/texture_audit.json"))
        );
        TextureValidationRules rules = TextureValidationRules.defaults();
        String dashboardStatus = TextureForgeDashboardPlaceholder.status();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("specRegistryRoundTrip", itemSpec.namespace().equals("echotextureforge")
                && itemSpec.assetId().equals("status lens")
                && itemSpec.outputPath().equals("textures/item/status lens.png")
                && registry.find("echotextureforge", "Status Lens", TextureKind.ITEM).isPresent()
                && registry.byNamespace("echotextureforge").size() == 2
                && registry.all().get(0).assetKind() == TextureKind.BLOCK);
        result.put("promptExportRoundTrip", prompt.contains("Mod ID: echotextureforge")
                && prompt.contains("Asset ID: status lens")
                && prompt.contains("Output Path: assets/echotextureforge/textures/item/status lens.png")
                && prompt.contains("no text")
                && prompt.contains("transparent background"));
        result.put("reviewStateRoundTrip", TextureSpecStatus.MISSING.id().equals("missing")
                && itemSpec.toBuilder().status(TextureSpecStatus.GENERATED_PENDING_REVIEW).build().status()
                == TextureSpecStatus.GENERATED_PENDING_REVIEW);
        result.put("textureAuditRoundTrip", report.totalSpecs() == 2
                && report.issues(TextureAuditSeverity.WARNING).size() == 1
                && report.severitySummary().get(TextureAuditSeverity.WARNING) == 1
                && rules.defaultResolution().equals(TextureResolution.DEFAULT_32)
                && rules.requirePowerOfTwo());
        result.put("dashboardSurfaceResolved", dashboardStatus.contains("dashboard bridge")
                && dashboardStatus.contains("ScreenCore"));
        result.put("promptLength", prompt.length());
        result.put("registrySize", registry.size());
        result.put("dashboardStatus", dashboardStatus);
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
