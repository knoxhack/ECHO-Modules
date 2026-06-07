package com.knoxhack.echotextureforge.common.spec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echotextureforge.EchoTextureForgeMod;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureResolution;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureSpecStatus;
import com.knoxhack.echotextureforge.api.spec.TextureStyleFamily;
import com.knoxhack.echotextureforge.api.spec.TextureType;
import com.knoxhack.echotextureforge.common.scan.ResourceScanResult;
import com.knoxhack.echotextureforge.common.style.TextureStyleFamilies;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TextureSpecLoader {
    private TextureSpecLoader() {
    }

    public static List<TextureSpec> loadManualSpecs(ResourceScanResult resources, String namespaceFilter) {
        List<TextureSpec> specs = new ArrayList<>();
        resources.namespaces().forEach((namespace, assets) -> {
            if (namespaceFilter != null && !namespaceFilter.isBlank() && !namespaceFilter.equals(namespace)) {
                return;
            }
            for (String specPath : assets.specFiles()) {
                assets.firstPath(specPath).ifPresent(path -> specs.addAll(loadFile(namespace, path)));
            }
        });
        return List.copyOf(specs);
    }

    private static List<TextureSpec> loadFile(String namespace, Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return List.of();
            }
            JsonObject object = root.getAsJsonObject();
            TextureStyleFamily style = TextureStyleFamily.byId(string(object, "styleFamily", ""),
                    TextureStyleFamilies.defaultForNamespace(namespace));
            TextureResolution resolution = TextureResolution.parse(string(object, "defaultResolution", "32x32"),
                    TextureResolution.DEFAULT_32);
            JsonArray assets = array(object, "assets");
            List<TextureSpec> specs = new ArrayList<>();
            if (assets == null) {
                return List.of();
            }
            for (JsonElement element : assets) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject asset = element.getAsJsonObject();
                String id = string(asset, "id", "");
                if (id.isBlank()) {
                    continue;
                }
                TextureKind kind = TextureKind.byId(string(asset, "kind", string(asset, "assetKind", "item")), TextureKind.ITEM);
                TextureAuditSeverity severity = severity(string(asset, "severity", string(asset, "severityIfMissing", "CRITICAL")),
                        TextureAuditSeverity.CRITICAL);
                TextureSpec.Builder builder = TextureSpec.builder(namespace, id, kind)
                        .textureType(TextureType.byId(string(asset, "textureType", ""), null))
                        .expectedResolution(TextureResolution.parse(string(asset, "expectedResolution", ""), resolution))
                        .styleFamily(TextureStyleFamily.byId(string(asset, "styleFamily", ""), style))
                        .palette(string(asset, "palette", ""))
                        .outputPath(string(asset, "outputPath", ""))
                        .modelPath(string(asset, "modelPath", ""))
                        .blockstatePath(string(asset, "blockstatePath", ""))
                        .langKey(string(asset, "langKey", ""))
                        .notes(string(asset, "notes", ""))
                        .requiredViews(strings(asset, "requiredViews"))
                        .requiredLayers(strings(asset, "requiredLayers"))
                        .mustHave(strings(asset, "mustHave"))
                        .avoid(strings(asset, "avoid"))
                        .promptTags(strings(asset, "promptTags"))
                        .sourceRegistryId(string(asset, "sourceRegistryId", "manual:" + path.getFileName() + ":" + id))
                        .registryId(string(asset, "registryId", "manual:" + namespace + ":" + id))
                        .sourceAddon(string(asset, "sourceAddon", namespace))
                        .promptPriority(integer(asset, "promptPriority", 0))
                        .severity(severity)
                        .severityIfMissing(severity)
                        .machineFacesRequired(strings(asset, "machineFacesRequired"))
                        .colorPaletteHints(strings(asset, "colorPaletteHints"))
                        .silhouetteNotes(string(asset, "silhouetteNotes", ""))
                        .minecraftReadabilityNotes(string(asset, "minecraftReadabilityNotes", ""))
                        .generatedPrompt(string(asset, "generatedPrompt", ""))
                        .sheetGroup(string(asset, "sheetGroup", ""))
                        .sheetCell(string(asset, "sheetCell", ""))
                        .status(TextureSpecStatus.byId(string(asset, "status", ""), TextureSpecStatus.MISSING));
                if (asset.has("transparencyRequired")) {
                    builder.transparencyRequired(bool(asset, "transparencyRequired", true));
                }
                if (asset.has("animationRequired")) {
                    builder.animationRequired(bool(asset, "animationRequired", false));
                }
                if (asset.has("emissiveOverlayRequired")) {
                    builder.emissiveOverlayRequired(bool(asset, "emissiveOverlayRequired", false));
                }
                if (asset.has("hasActiveVariant")) {
                    builder.hasActiveVariant(bool(asset, "hasActiveVariant", false));
                }
                if (asset.has("hasInactiveVariant")) {
                    builder.hasInactiveVariant(bool(asset, "hasInactiveVariant", false));
                }
                specs.add(builder.build());
            }
            return specs;
        } catch (RuntimeException | IOException exception) {
            EchoTextureForgeMod.LOGGER.warn("TextureForge could not parse manual spec {}.", path, exception);
            return List.of();
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return Math.max(0, element.getAsInt());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static TextureAuditSeverity severity(String raw, TextureAuditSeverity fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return TextureAuditSeverity.valueOf(raw.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static List<String> strings(JsonObject object, String key) {
        JsonArray array = array(object, key);
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonNull()) {
                try {
                    values.add(element.getAsString());
                } catch (RuntimeException ignored) {
                    // Ignore malformed manual spec list entries and keep loading the rest of the file.
                }
            }
        }
        return List.copyOf(values);
    }
}
