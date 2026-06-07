package com.knoxhack.echotextureforge.common.scan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echotextureforge.api.report.TextureAuditIssue;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class TextureModelReferenceValidator {
    private TextureModelReferenceValidator() {
    }

    public static Result validate(ResourceScanResult resources) {
        List<TextureAuditIssue> issues = new ArrayList<>();
        Set<String> referencedTextures = new LinkedHashSet<>();
        resources.namespaces().forEach((namespace, assets) -> {
            assets.itemModels().forEach(model -> validateModel(namespace, model, TextureKind.ITEM, assets, resources,
                    issues, referencedTextures));
            assets.blockModels().forEach(model -> validateModel(namespace, model, TextureKind.BLOCK, assets, resources,
                    issues, referencedTextures));
            assets.blockstates().forEach(blockstate -> validateBlockstate(namespace, blockstate, assets, resources, issues));
        });
        return new Result(List.copyOf(issues), Set.copyOf(referencedTextures));
    }

    private static void validateModel(
            String namespace,
            String modelPath,
            TextureKind kind,
            ResourceScanResult.NamespaceAssets assets,
            ResourceScanResult resources,
            List<TextureAuditIssue> issues,
            Set<String> referencedTextures) {
        assets.firstPath(modelPath).ifPresent(path -> {
            JsonObject model = parse(path.toString(), path, issues, namespace, stripModel(modelPath), kind);
            if (model == null) {
                return;
            }
            String parent = string(model, "parent");
            if (!parent.isBlank()) {
                ModelRef ref = modelRef(namespace, parent);
                if (!"minecraft".equals(ref.namespace()) && !resources.hasAsset(ref.namespace(), ref.modelPath())) {
                    issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "BROKEN_PARENT_MODEL_REF",
                            namespace, stripModel(modelPath), kind, modelPath,
                            "Parent model reference is missing: " + parent + " -> " + ref.namespace() + ":" + ref.modelPath()));
                }
            }
            JsonElement textures = model.get("textures");
            if (textures != null && textures.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : textures.getAsJsonObject().entrySet()) {
                    if (!entry.getValue().isJsonPrimitive()) {
                        continue;
                    }
                    String raw = entry.getValue().getAsString();
                    if (raw.startsWith("#")) {
                        continue;
                    }
                    TextureRef ref = textureRef(namespace, raw);
                    referencedTextures.add(ref.namespace() + ":" + ref.texturePath());
                    if (!"minecraft".equals(ref.namespace()) && !resources.hasAsset(ref.namespace(), ref.texturePath())) {
                        issues.add(new TextureAuditIssue(TextureAuditSeverity.CRITICAL, "MODEL_TEXTURE_MISSING",
                                namespace, stripModel(modelPath), kind, modelPath,
                                "Model texture reference is missing: " + raw + " -> " + ref.namespace() + ":" + ref.texturePath()));
                    }
                }
            }
        });
    }

    private static void validateBlockstate(
            String namespace,
            String blockstatePath,
            ResourceScanResult.NamespaceAssets assets,
            ResourceScanResult resources,
            List<TextureAuditIssue> issues) {
        assets.firstPath(blockstatePath).ifPresent(path -> {
            JsonObject blockstate = parse(path.toString(), path, issues, namespace, strip(blockstatePath, "blockstates/", ".json"),
                    TextureKind.BLOCK);
            if (blockstate == null) {
                return;
            }
            List<String> models = new ArrayList<>();
            collectModelRefs(blockstate, models);
            String blockId = strip(blockstatePath, "blockstates/", ".json");
            for (String raw : models) {
                ModelRef ref = modelRef(namespace, raw);
                if (!"minecraft".equals(ref.namespace()) && !resources.hasAsset(ref.namespace(), ref.modelPath())) {
                    issues.add(new TextureAuditIssue(TextureAuditSeverity.CRITICAL, "BLOCKSTATE_MODEL_MISSING",
                            namespace, blockId, TextureKind.BLOCK, blockstatePath,
                            "Blockstate model reference is missing: " + raw + " -> " + ref.namespace() + ":" + ref.modelPath()));
                }
            }
        });
    }

    private static void collectModelRefs(JsonElement element, List<String> models) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement model = object.get("model");
            if (model != null && model.isJsonPrimitive()) {
                models.add(model.getAsString());
            }
            object.entrySet().forEach(entry -> collectModelRefs(entry.getValue(), models));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectModelRefs(child, models));
        }
    }

    private static JsonObject parse(String label, java.nio.file.Path path, List<TextureAuditIssue> issues,
                                    String namespace, String assetId, TextureKind kind) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (IOException | RuntimeException exception) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "BROKEN_JSON",
                    namespace, assetId, kind, label, "JSON could not be parsed: " + exception.getMessage()));
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static ModelRef modelRef(String currentNamespace, String raw) {
        Identifier id = parseId(currentNamespace, raw);
        String path = id.getPath();
        if (!path.startsWith("item/") && !path.startsWith("block/")) {
            path = "block/" + path;
        }
        return new ModelRef(id.getNamespace(), "models/" + path + ".json");
    }

    private static TextureRef textureRef(String currentNamespace, String raw) {
        Identifier id = parseId(currentNamespace, raw);
        String path = id.getPath();
        if (!path.startsWith("item/") && !path.startsWith("block/")
                && !path.startsWith("entity/") && !path.startsWith("gui/")
                && !path.startsWith("particle/")) {
            path = "block/" + path;
        }
        return new TextureRef(id.getNamespace(), "textures/" + path + ".png");
    }

    private static Identifier parseId(String currentNamespace, String raw) {
        if (raw == null || raw.isBlank()) {
            return Identifier.fromNamespaceAndPath(currentNamespace, "");
        }
        String value = raw.strip();
        try {
            if (value.contains(":")) {
                return Identifier.parse(value);
            }
            return Identifier.fromNamespaceAndPath(currentNamespace, value);
        } catch (RuntimeException exception) {
            String cleanNamespace = sanitize(currentNamespace);
            String cleanPath = sanitize(value.replace(':', '_'));
            try {
                return Identifier.fromNamespaceAndPath(cleanNamespace, cleanPath);
            } catch (RuntimeException ignored) {
                return Identifier.fromNamespaceAndPath("minecraft", "missingno");
            }
        }
    }

    private static String sanitize(String value) {
        String cleaned = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
        return cleaned.isBlank() ? "missingno" : cleaned;
    }

    private static String stripModel(String modelPath) {
        String stripped = strip(modelPath, "models/item/", ".json");
        return stripped.equals(modelPath) ? strip(modelPath, "models/block/", ".json") : stripped;
    }

    private static String strip(String value, String prefix, String suffix) {
        String stripped = value;
        if (stripped.startsWith(prefix)) {
            stripped = stripped.substring(prefix.length());
        }
        if (stripped.endsWith(suffix)) {
            stripped = stripped.substring(0, stripped.length() - suffix.length());
        }
        return stripped;
    }

    private record ModelRef(String namespace, String modelPath) {
    }

    private record TextureRef(String namespace, String texturePath) {
    }

    public record Result(List<TextureAuditIssue> issues, Set<String> referencedTextures) {
    }
}
