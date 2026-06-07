package com.knoxhack.echotextureforge.common.scan;

import com.knoxhack.echotextureforge.api.report.TextureAuditIssue;
import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.scan.TextureValidationRules;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureSpecRegistry;
import com.knoxhack.echotextureforge.api.spec.TextureType;
import com.knoxhack.echotextureforge.common.config.TextureForgeConfig;
import com.knoxhack.echotextureforge.common.spec.TextureSpecGenerator;
import com.knoxhack.echotextureforge.common.spec.TextureSpecLoader;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TextureForgeScanner {
    private TextureForgeScanner() {
    }

    public static ScanOutput scan(TextureForgePaths paths, String namespaceFilter, boolean includeRegistry) {
        ResourceScanResult resources = ResourceAssetScanner.scan(paths);
        TextureSpecRegistry registry = new TextureSpecRegistry();
        RegistryAssetScanner.RegistrySpecs registrySpecs = includeRegistry
                ? RegistryAssetScanner.scan(namespaceFilter)
                : new RegistryAssetScanner.RegistrySpecs(List.of(), 0, 0);
        registry.registerAll(TextureSpecGenerator.fromResources(resources, namespaceFilter));
        registry.registerAll(registrySpecs.specs());
        registry.registerAll(TextureSpecLoader.loadManualSpecs(resources, namespaceFilter));

        List<TextureSpec> specs = registry.byNamespace(namespaceFilter);
        List<TextureAuditIssue> issues = new ArrayList<>();
        auditRequiredAssets(resources, specs, issues);
        auditDuplicateFiles(resources, issues);
        issues.addAll(TextureImageValidator.validate(resources, specs, new TextureValidationRules(
                com.knoxhack.echotextureforge.api.spec.TextureResolution.DEFAULT_32,
                TextureForgeConfig.validate32x32(),
                true,
                true,
                TextureForgeConfig.strictMode())));
        TextureModelReferenceValidator.Result modelRefs = TextureModelReferenceValidator.validate(resources);
        issues.addAll(modelRefs.issues());
        issues.addAll(TextureNamingValidator.validate(specs, resources));
        auditUnusedTextures(resources, specs, modelRefs.referencedTextures(), issues);

        TextureAuditReport report = buildReport(paths, resources, registrySpecs, specs, issues);
        return new ScanOutput(report, resources);
    }

    private static void auditRequiredAssets(ResourceScanResult resources, List<TextureSpec> specs, List<TextureAuditIssue> issues) {
        for (TextureSpec spec : specs) {
            if (!spec.outputPath().isBlank() && !resources.hasAsset(spec.namespace(), spec.outputPath())) {
                issues.add(new TextureAuditIssue(spec.severity(), "MISSING_TEXTURE",
                        spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/" + spec.outputPath(),
                        "Missing texture for " + spec.sourceRegistryId() + "."));
            }
            if (!spec.modelPath().isBlank() && !resources.hasAsset(spec.namespace(), spec.modelPath())) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.CRITICAL, spec.isBlockLike() ? "MISSING_BLOCK_MODEL" : "MISSING_ITEM_MODEL",
                        spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/" + spec.modelPath(),
                        "Missing model for " + spec.sourceRegistryId() + "."));
            }
            if (!spec.blockstatePath().isBlank() && !resources.hasAsset(spec.namespace(), spec.blockstatePath())) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.CRITICAL, "MISSING_BLOCKSTATE",
                        spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/" + spec.blockstatePath(),
                        "Missing blockstate for " + spec.sourceRegistryId() + "."));
            }
            if (spec.isBlockLike() && !resources.hasAsset(spec.namespace(), "models/item/" + spec.assetId() + ".json")) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.CRITICAL, "MISSING_ITEM_MODEL",
                        spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/models/item/" + spec.assetId() + ".json",
                        "Missing block item model for " + spec.sourceRegistryId() + "."));
            }
            if (!spec.langKey().isBlank()
                    && resources.assets(spec.namespace()).map(assets -> !assets.langKeys().contains(spec.langKey())).orElse(true)) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "MISSING_LANG_KEY",
                        spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/lang/en_us.json",
                        "Missing lang key " + spec.langKey() + "."));
            }
            auditMachineVariants(resources, spec, issues);
        }
    }

    private static void auditMachineVariants(ResourceScanResult resources, TextureSpec spec, List<TextureAuditIssue> issues) {
        if (spec.assetKind() != TextureKind.MACHINE
                && spec.textureType() != TextureType.MACHINE_FRONT_SIDE_TOP
                && spec.textureType() != TextureType.MACHINE_ACTIVE_INACTIVE) {
            return;
        }
        String id = spec.assetId();
        List<String> requiredFaces = spec.machineFacesRequired().isEmpty()
                ? List.of("front", "side", "top")
                : spec.machineFacesRequired();
        for (String face : requiredFaces) {
            String path = "textures/block/" + id + "_" + face + ".png";
            if (!resources.hasAsset(spec.namespace(), path)) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "MISSING_MACHINE_VARIANT",
                        spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/" + path,
                        "Machine-style asset requires a " + face + " face texture."));
            }
        }
        if (spec.textureType() == TextureType.MACHINE_ACTIVE_INACTIVE || spec.hasActiveVariant() || spec.hasInactiveVariant()) {
            List<String> activePaths = List.of("textures/block/" + id + "_active_front.png",
                    "textures/block/" + id + "_inactive_front.png");
            for (String path : activePaths) {
                if (!resources.hasAsset(spec.namespace(), path)) {
                    issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "MISSING_MACHINE_ACTIVE_VARIANT",
                            spec.namespace(), spec.assetId(), spec.assetKind(), "assets/" + spec.namespace() + "/" + path,
                            "Machine active/inactive texture set is incomplete."));
                }
            }
        }
    }

    private static void auditDuplicateFiles(ResourceScanResult resources, List<TextureAuditIssue> issues) {
        resources.namespaces().forEach((namespace, assets) -> assets.duplicateFiles().forEach((relativePath, paths) ->
                issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "DUPLICATE_RESOURCE_PATH",
                        namespace, assetId(relativePath), null, relativePath,
                        "Resource path exists in multiple scanned roots: " + paths))));
    }

    private static void auditUnusedTextures(
            ResourceScanResult resources,
            List<TextureSpec> specs,
            Set<String> referencedTextures,
            List<TextureAuditIssue> issues) {
        Set<String> expected = new HashSet<>();
        for (TextureSpec spec : specs) {
            if (!spec.outputPath().isBlank()) {
                expected.add(spec.namespace() + ":" + spec.outputPath());
            }
        }
        resources.namespaces().forEach((namespace, assets) -> {
            for (String texture : assets.textureFiles()) {
                String key = namespace + ":" + texture;
                if (!expected.contains(key) && !referencedTextures.contains(key)) {
                    issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "UNUSED_TEXTURE",
                            namespace, assetId(texture), kind(texture), "assets/" + namespace + "/" + texture,
                            "Texture is not referenced by generated specs or model texture references."));
                }
            }
        });
    }

    private static TextureAuditReport buildReport(
            TextureForgePaths paths,
            ResourceScanResult resources,
            RegistryAssetScanner.RegistrySpecs registrySpecs,
            List<TextureSpec> specs,
            List<TextureAuditIssue> issues) {
        Map<TextureAuditSeverity, Integer> severities = new EnumMap<>(TextureAuditSeverity.class);
        for (TextureAuditSeverity severity : TextureAuditSeverity.values()) {
            severities.put(severity, 0);
        }
        for (TextureAuditIssue issue : issues) {
            severities.compute(issue.severity(), (ignored, count) -> count == null ? 1 : count + 1);
        }
        return new TextureAuditReport(
                Instant.now(),
                paths.workspaceRoot(),
                paths.outputRoot(),
                resources.scannedModuleRoots().size(),
                registrySpecs.registeredItems(),
                registrySpecs.registeredBlocks(),
                specs.size(),
                resources.textureCount(),
                resources.itemModelCount(),
                resources.blockModelCount(),
                resources.blockstateCount(),
                count(issues, "MISSING_TEXTURE"),
                count(issues, "MISSING_ITEM_MODEL") + count(issues, "MISSING_BLOCK_MODEL"),
                count(issues, "MISSING_BLOCKSTATE"),
                count(issues, "MISSING_LANG_KEY"),
                count(issues, "WRONG_TEXTURE_SIZE"),
                count(issues, "UNUSED_TEXTURE"),
                severities,
                issues.stream().sorted(TextureForgeScanner::compareIssues).toList(),
                specs,
                List.of(),
                List.of());
    }

    private static int compareIssues(TextureAuditIssue left, TextureAuditIssue right) {
        int severity = Integer.compare(rank(left.severity()), rank(right.severity()));
        if (severity != 0) {
            return severity;
        }
        int namespace = left.namespace().compareTo(right.namespace());
        if (namespace != 0) {
            return namespace;
        }
        int asset = left.assetId().compareTo(right.assetId());
        return asset != 0 ? asset : left.code().compareTo(right.code());
    }

    private static int rank(TextureAuditSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case WARNING -> 1;
            case INFO -> 2;
        };
    }

    private static int count(List<TextureAuditIssue> issues, String code) {
        return (int) issues.stream().filter(issue -> code.equals(issue.code())).count();
    }

    private static String assetId(String relativePath) {
        String value = relativePath.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }

    private static TextureKind kind(String texture) {
        if (texture.startsWith("textures/block/")) {
            return TextureKind.BLOCK;
        }
        if (texture.startsWith("textures/entity/")) {
            return TextureKind.ENTITY;
        }
        if (texture.startsWith("textures/gui/")) {
            return TextureKind.UI;
        }
        return TextureKind.ITEM;
    }

    public record ScanOutput(TextureAuditReport report, ResourceScanResult resources) {
    }
}
