package com.knoxhack.echotextureforge.api.spec;

import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record TextureSpec(
        String namespace,
        String assetId,
        String displayName,
        TextureKind assetKind,
        TextureType textureType,
        TextureResolution expectedResolution,
        String outputPath,
        String modelPath,
        String blockstatePath,
        String langKey,
        TextureStyleFamily styleFamily,
        String palette,
        List<String> requiredViews,
        List<String> requiredLayers,
        boolean transparencyRequired,
        boolean animationRequired,
        boolean emissiveOverlayRequired,
        String notes,
        List<String> mustHave,
        List<String> avoid,
        List<String> promptTags,
        String sourceRegistryId,
        TextureAuditSeverity severityIfMissing,
        String registryId,
        String sourceAddon,
        int promptPriority,
        TextureAuditSeverity severity,
        List<String> machineFacesRequired,
        boolean hasActiveVariant,
        boolean hasInactiveVariant,
        List<String> colorPaletteHints,
        String silhouetteNotes,
        String minecraftReadabilityNotes,
        String generatedPrompt,
        String sheetGroup,
        String sheetCell,
        TextureSpecStatus status) {
    public TextureSpec {
        namespace = cleanId(namespace);
        assetId = cleanPath(assetId);
        displayName = cleanText(displayName, humanize(assetId));
        assetKind = assetKind == null ? TextureKind.ITEM : assetKind;
        textureType = textureType == null ? defaultType(assetKind) : textureType;
        expectedResolution = expectedResolution == null ? TextureResolution.DEFAULT_32 : expectedResolution;
        outputPath = cleanText(outputPath, defaultOutputPath(assetKind, assetId));
        modelPath = cleanText(modelPath, defaultModelPath(assetKind, assetId));
        blockstatePath = cleanText(blockstatePath, assetKind == TextureKind.BLOCK || assetKind == TextureKind.MACHINE
                ? "blockstates/" + assetId + ".json" : "");
        langKey = cleanText(langKey, defaultLangKey(namespace, assetKind, assetId));
        styleFamily = styleFamily == null ? TextureStyleFamily.ASHFALL_SURVIVAL : styleFamily;
        palette = cleanText(palette, "");
        requiredViews = copy(requiredViews);
        requiredLayers = copy(requiredLayers);
        notes = cleanText(notes, "");
        mustHave = copy(mustHave);
        avoid = copy(avoid);
        promptTags = copy(promptTags);
        sourceRegistryId = cleanText(sourceRegistryId, namespace + ":" + assetId);
        severityIfMissing = severityIfMissing == null ? TextureAuditSeverity.CRITICAL : severityIfMissing;
        registryId = cleanText(registryId, sourceRegistryId);
        sourceAddon = cleanText(sourceAddon, namespace);
        promptPriority = Math.max(0, promptPriority);
        severity = severity == null ? severityIfMissing : severity;
        machineFacesRequired = copy(machineFacesRequired);
        colorPaletteHints = copy(colorPaletteHints);
        silhouetteNotes = cleanText(silhouetteNotes, "");
        minecraftReadabilityNotes = cleanText(minecraftReadabilityNotes, "");
        generatedPrompt = cleanText(generatedPrompt, "");
        sheetGroup = cleanText(sheetGroup, defaultSheetGroup(assetKind));
        sheetCell = cleanText(sheetCell, "");
        status = status == null ? TextureSpecStatus.MISSING : status;
    }

    public String key() {
        return key(namespace, assetId, assetKind);
    }

    public static String key(String namespace, String assetId, TextureKind kind) {
        TextureKind assetKind = kind == null ? TextureKind.ITEM : kind;
        return cleanId(namespace) + ":" + cleanPath(assetId) + ":" + assetKind.id();
    }

    public TextureKind kind() {
        return assetKind;
    }

    public boolean isBlockLike() {
        return assetKind == TextureKind.BLOCK || assetKind == TextureKind.MACHINE || assetKind == TextureKind.FLUID;
    }

    public boolean isItemLike() {
        return assetKind == TextureKind.ITEM || assetKind == TextureKind.ARMOR || assetKind == TextureKind.STRUCTURE_ICON;
    }

    public Builder toBuilder() {
        return builder(namespace, assetId, assetKind)
                .displayName(displayName)
                .textureType(textureType)
                .expectedResolution(expectedResolution)
                .outputPath(outputPath)
                .modelPath(modelPath)
                .blockstatePath(blockstatePath)
                .langKey(langKey)
                .styleFamily(styleFamily)
                .palette(palette)
                .requiredViews(requiredViews)
                .requiredLayers(requiredLayers)
                .transparencyRequired(transparencyRequired)
                .animationRequired(animationRequired)
                .emissiveOverlayRequired(emissiveOverlayRequired)
                .notes(notes)
                .mustHave(mustHave)
                .avoid(avoid)
                .promptTags(promptTags)
                .sourceRegistryId(sourceRegistryId)
                .severityIfMissing(severityIfMissing)
                .registryId(registryId)
                .sourceAddon(sourceAddon)
                .promptPriority(promptPriority)
                .severity(severity)
                .machineFacesRequired(machineFacesRequired)
                .hasActiveVariant(hasActiveVariant)
                .hasInactiveVariant(hasInactiveVariant)
                .colorPaletteHints(colorPaletteHints)
                .silhouetteNotes(silhouetteNotes)
                .minecraftReadabilityNotes(minecraftReadabilityNotes)
                .generatedPrompt(generatedPrompt)
                .sheetGroup(sheetGroup)
                .sheetCell(sheetCell)
                .status(status);
    }

    public static Builder builder(String namespace, String assetId, TextureKind kind) {
        return new Builder(namespace, assetId, kind);
    }

    public static TextureSpec item(String namespace, String assetId) {
        return builder(namespace, assetId, TextureKind.ITEM).build();
    }

    public static TextureSpec block(String namespace, String assetId) {
        return builder(namespace, assetId, TextureKind.BLOCK)
                .textureType(TextureType.CUBE_ALL)
                .transparencyRequired(false)
                .build();
    }

    private static TextureType defaultType(TextureKind kind) {
        return switch (kind) {
            case BLOCK -> TextureType.CUBE_ALL;
            case MACHINE -> TextureType.MACHINE_FRONT_SIDE_TOP;
            case ARMOR -> TextureType.ARMOR_PIECE;
            case ENTITY -> TextureType.MOB_BASE;
            case UI, STRUCTURE_ICON -> TextureType.ICON;
            case PARTICLE -> TextureType.TRANSPARENT_CUTOUT;
            case FLUID -> TextureType.FLUID_BLOCK;
            default -> TextureType.SIMPLE_ITEM;
        };
    }

    private static String defaultOutputPath(TextureKind kind, String assetId) {
        return switch (kind) {
            case BLOCK, MACHINE, FLUID -> "textures/block/" + assetId + ".png";
            case ENTITY -> "textures/entity/" + assetId + ".png";
            case UI, STRUCTURE_ICON -> "textures/gui/" + assetId + ".png";
            case PARTICLE -> "textures/particle/" + assetId + ".png";
            default -> "textures/item/" + assetId + ".png";
        };
    }

    private static String defaultModelPath(TextureKind kind, String assetId) {
        return switch (kind) {
            case BLOCK, MACHINE, FLUID -> "models/block/" + assetId + ".json";
            case ITEM, ARMOR, STRUCTURE_ICON -> "models/item/" + assetId + ".json";
            default -> "";
        };
    }

    private static String defaultLangKey(String namespace, TextureKind kind, String assetId) {
        return switch (kind) {
            case BLOCK, MACHINE, FLUID -> "block." + namespace + "." + assetId;
            case ITEM, ARMOR, STRUCTURE_ICON -> "item." + namespace + "." + assetId;
            default -> "";
        };
    }

    private static String defaultSheetGroup(TextureKind kind) {
        return switch (kind) {
            case BLOCK, FLUID -> "block";
            case MACHINE -> "machine";
            case ARMOR -> "armor";
            case ENTITY -> "entity";
            case UI, STRUCTURE_ICON -> "ui";
            default -> "item";
        };
    }

    private static String humanize(String id) {
        if (id == null || id.isBlank()) {
            return "Unnamed Texture";
        }
        String[] parts = id.replace('/', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.isEmpty() ? id : result.toString();
    }

    private static String cleanId(String value) {
        return cleanText(value, "").toLowerCase(Locale.ROOT);
    }

    private static String cleanPath(String value) {
        return cleanText(value, "").toLowerCase(Locale.ROOT).replace('\\', '/');
    }

    private static String cleanText(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static List<String> copy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.strip());
            }
        }
        return List.copyOf(cleaned);
    }

    public static final class Builder {
        private final String namespace;
        private final String assetId;
        private final TextureKind assetKind;
        private String displayName;
        private TextureType textureType;
        private TextureResolution expectedResolution = TextureResolution.DEFAULT_32;
        private String outputPath;
        private String modelPath;
        private String blockstatePath;
        private String langKey;
        private TextureStyleFamily styleFamily;
        private String palette = "";
        private List<String> requiredViews = List.of();
        private List<String> requiredLayers = List.of();
        private boolean transparencyRequired = true;
        private boolean animationRequired;
        private boolean emissiveOverlayRequired;
        private String notes = "";
        private List<String> mustHave = List.of();
        private List<String> avoid = List.of();
        private List<String> promptTags = List.of();
        private String sourceRegistryId;
        private TextureAuditSeverity severityIfMissing = TextureAuditSeverity.CRITICAL;
        private String registryId;
        private String sourceAddon;
        private int promptPriority;
        private TextureAuditSeverity severity;
        private List<String> machineFacesRequired = List.of();
        private boolean hasActiveVariant;
        private boolean hasInactiveVariant;
        private List<String> colorPaletteHints = List.of();
        private String silhouetteNotes = "";
        private String minecraftReadabilityNotes = "";
        private String generatedPrompt = "";
        private String sheetGroup = "";
        private String sheetCell = "";
        private TextureSpecStatus status = TextureSpecStatus.MISSING;

        private Builder(String namespace, String assetId, TextureKind assetKind) {
            this.namespace = namespace;
            this.assetId = assetId;
            this.assetKind = assetKind;
            if (assetKind == TextureKind.BLOCK || assetKind == TextureKind.MACHINE || assetKind == TextureKind.FLUID) {
                this.transparencyRequired = false;
            }
            if (assetKind == TextureKind.MACHINE) {
                this.machineFacesRequired = List.of("front", "side", "top");
            }
        }

        public Builder displayName(String value) { this.displayName = value; return this; }
        public Builder textureType(TextureType value) { this.textureType = value; return this; }
        public Builder expectedResolution(TextureResolution value) { this.expectedResolution = value; return this; }
        public Builder outputPath(String value) { this.outputPath = value; return this; }
        public Builder modelPath(String value) { this.modelPath = value; return this; }
        public Builder blockstatePath(String value) { this.blockstatePath = value; return this; }
        public Builder langKey(String value) { this.langKey = value; return this; }
        public Builder styleFamily(TextureStyleFamily value) { this.styleFamily = value; return this; }
        public Builder palette(String value) { this.palette = value; return this; }
        public Builder requiredViews(List<String> value) { this.requiredViews = value; return this; }
        public Builder requiredLayers(List<String> value) { this.requiredLayers = value; return this; }
        public Builder transparencyRequired(boolean value) { this.transparencyRequired = value; return this; }
        public Builder animationRequired(boolean value) { this.animationRequired = value; return this; }
        public Builder emissiveOverlayRequired(boolean value) { this.emissiveOverlayRequired = value; return this; }
        public Builder notes(String value) { this.notes = value; return this; }
        public Builder mustHave(List<String> value) { this.mustHave = value; return this; }
        public Builder avoid(List<String> value) { this.avoid = value; return this; }
        public Builder promptTags(List<String> value) { this.promptTags = value; return this; }
        public Builder sourceRegistryId(String value) { this.sourceRegistryId = value; return this; }
        public Builder severityIfMissing(TextureAuditSeverity value) { this.severityIfMissing = value; return this; }
        public Builder registryId(String value) { this.registryId = value; return this; }
        public Builder sourceAddon(String value) { this.sourceAddon = value; return this; }
        public Builder promptPriority(int value) { this.promptPriority = value; return this; }
        public Builder severity(TextureAuditSeverity value) { this.severity = value; return this; }
        public Builder machineFacesRequired(List<String> value) { this.machineFacesRequired = value; return this; }
        public Builder hasActiveVariant(boolean value) { this.hasActiveVariant = value; return this; }
        public Builder hasInactiveVariant(boolean value) { this.hasInactiveVariant = value; return this; }
        public Builder colorPaletteHints(List<String> value) { this.colorPaletteHints = value; return this; }
        public Builder silhouetteNotes(String value) { this.silhouetteNotes = value; return this; }
        public Builder minecraftReadabilityNotes(String value) { this.minecraftReadabilityNotes = value; return this; }
        public Builder generatedPrompt(String value) { this.generatedPrompt = value; return this; }
        public Builder sheetGroup(String value) { this.sheetGroup = value; return this; }
        public Builder sheetCell(String value) { this.sheetCell = value; return this; }
        public Builder status(TextureSpecStatus value) { this.status = value; return this; }

        public TextureSpec build() {
            return new TextureSpec(namespace, assetId, displayName, assetKind, textureType, expectedResolution,
                    outputPath, modelPath, blockstatePath, langKey, styleFamily, palette, requiredViews, requiredLayers,
                    transparencyRequired, animationRequired, emissiveOverlayRequired, notes, mustHave, avoid, promptTags,
                    sourceRegistryId, severityIfMissing, registryId, sourceAddon, promptPriority, severity,
                    machineFacesRequired, hasActiveVariant, hasInactiveVariant, colorPaletteHints, silhouetteNotes,
                    minecraftReadabilityNotes, generatedPrompt, sheetGroup, sheetCell, status);
        }
    }
}
