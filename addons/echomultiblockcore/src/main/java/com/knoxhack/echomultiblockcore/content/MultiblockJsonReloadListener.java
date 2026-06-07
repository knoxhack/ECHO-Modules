package com.knoxhack.echomultiblockcore.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.CapabilityRequirement;
import com.knoxhack.echomultiblockcore.api.MultiblockCapability;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinitionParseResult;
import com.knoxhack.echomultiblockcore.api.MultiblockRole;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.api.StructureBlockRequirement;
import com.knoxhack.echomultiblockcore.api.UpgradeSlotRule;
import com.knoxhack.echomultiblockcore.api.WorkcellDefinition;
import com.knoxhack.echomultiblockcore.api.WorkcellType;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class MultiblockJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, MultiblockDefinition>> {
    private static final String DIRECTORY = "echo_multiblocks";

    @Override
    protected Map<Identifier, MultiblockDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, MultiblockDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                MultiblockDefinitionParseResult result = parseDefinitionResult(resourceId, fallbackId, root.getAsJsonObject());
                result.warnings().forEach(warning -> EchoMultiblockCore.LOGGER.warn(
                        "Multiblock definition warning in {} [{}]: {}", resourceId, result.definitionId(), warning));
                result.errors().forEach(error -> EchoMultiblockCore.LOGGER.warn(
                        "Multiblock definition error in {} [{}]: {}", resourceId, result.definitionId(), error));
                if (!result.valid()) {
                    continue;
                }
                MultiblockDefinition definition = result.definition();
                if (loaded.put(definition.id(), definition) != null) {
                    EchoMultiblockCore.LOGGER.warn("Duplicate multiblock definition id {} from {} replaced an earlier entry.",
                            definition.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("Could not parse multiblock definition file {}.", resourceId, exception);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, MultiblockDefinition> content, ResourceManager manager, ProfilerFiller profiler) {
        if ((content == null || content.isEmpty()) && !MultiblockContent.snapshot().isEmpty()) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore reload produced no valid definitions; keeping last good registry.");
            return;
        }
        MultiblockContent.replaceDefinitions(content);
    }

    public static MultiblockDefinition parseDefinitionForTests(Identifier fallbackId, JsonObject json) {
        MultiblockDefinitionParseResult result = parseDefinitionResultForTests(fallbackId, json);
        if (!result.valid()) {
            throw new JsonParseException(String.join("; ", result.errors()));
        }
        return result.definition();
    }

    public static MultiblockDefinitionParseResult parseDefinitionResultForTests(Identifier fallbackId, JsonObject json) {
        return parseDefinitionResult(fallbackId, fallbackId, json);
    }

    private static MultiblockDefinitionParseResult parseDefinitionResult(Identifier resourceId, Identifier fallbackId, JsonObject json) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Identifier parsedId = fallbackId;
        MultiblockDefinition definition = null;
        try {
            warnInvalidEnum(json, "role", MultiblockRole.class, warnings);
            warnInvalidPreviewColor(json, warnings);
            warnInvalidWorkcellEnums(json.get("workcells"), warnings);
            definition = parseDefinition(fallbackId, json);
            parsedId = definition.id();
            validateDefinition(definition, warnings, errors);
        } catch (RuntimeException exception) {
            errors.add("$.root: " + exception.getMessage());
        }
        return new MultiblockDefinitionParseResult(resourceId, parsedId, errors.isEmpty() ? definition : null, warnings, errors);
    }

    private static MultiblockDefinition parseDefinition(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        int[] size = intArray(json, "size", 3);
        if (size[0] < 1 || size[1] < 1 || size[2] < 1) {
            throw new JsonParseException("Multiblock " + id + " size must be positive.");
        }
        List<List<String>> layers = layers(json, size[0], size[1], size[2], id);
        Map<Character, StructureBlockRequirement> palette = palette(json.getAsJsonObject("palette"));
        validatePalette(id, layers, palette);
        return new MultiblockDefinition(
                id,
                string(json, "display_name", string(json, "displayName", id.getPath())),
                string(json, "translation_key", ""),
                string(json, "category", "general"),
                enumValue(MultiblockRole.class, string(json, "role", MultiblockRole.INFRASTRUCTURE.name()), MultiblockRole.INFRASTRUCTURE),
                size[0],
                size[1],
                size[2],
                identifier(json, "controller", id),
                palette,
                layers,
                bool(json, "allowed_rotations", true),
                bool(json, "mirrorable", false),
                bool(json, "requires_foundation", false),
                capabilities(json.get("capabilities")),
                identifiers(json.get("optional_tags")),
                stringList(json.get("upgrade_rules")),
                previewColor(json.getAsJsonObject("preview")),
                integrity(json.getAsJsonObject("integrity")),
                workcells(id, json.get("workcells")),
                robotics(json.get("robotics")),
                capabilityRequirements(json.get("capability_requirements"), "$.capability_requirements", List.of()),
                upgradeSlotRules(json.get("upgrade_slots"), id),
                string(json, "facility_stage", ""),
                string(json, "facility_route", ""),
                string(json, "primary_use", ""),
                stringList(json.get("integration_hints")));
    }

    private static void validatePalette(Identifier id, List<List<String>> layers, Map<Character, StructureBlockRequirement> palette) {
        if (palette.isEmpty()) {
            throw new JsonParseException("Multiblock " + id + " must define a palette.");
        }
        Set<Character> used = new java.util.LinkedHashSet<>();
        for (List<String> layer : layers) {
            for (String row : layer) {
                for (int i = 0; i < row.length(); i++) {
                    char key = row.charAt(i);
                    used.add(key);
                    if (!palette.containsKey(key)) {
                        throw new JsonParseException("Multiblock " + id + " layer references missing palette key '" + key + "'.");
                    }
                }
            }
        }
    }

    private static Map<Character, StructureBlockRequirement> palette(JsonObject json) {
        if (json == null) {
            throw new JsonParseException("Missing palette object.");
        }
        Map<Character, StructureBlockRequirement> palette = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            if (entry.getKey().length() != 1) {
                throw new JsonParseException("$.palette." + entry.getKey() + ": palette keys must be exactly one character.");
            }
            if (!entry.getValue().isJsonObject()) {
                throw new JsonParseException("Palette entry '" + entry.getKey() + "' must be an object.");
            }
            palette.put(entry.getKey().charAt(0), requirement(entry.getValue().getAsJsonObject()));
        }
        return palette;
    }

    private static StructureBlockRequirement requirement(JsonObject json) {
        boolean optional = bool(json, "optional", false);
        StructureBlockRequirement.SlotKind forcedKind = null;
        if (bool(json, "controller", false)) {
            forcedKind = StructureBlockRequirement.SlotKind.CONTROLLER;
        } else if (bool(json, "robotics", false)) {
            forcedKind = StructureBlockRequirement.SlotKind.ROBOTICS;
        } else if (bool(json, "component", false)) {
            forcedKind = StructureBlockRequirement.SlotKind.COMPONENT;
        } else if (bool(json, "upgrade", false)) {
            forcedKind = StructureBlockRequirement.SlotKind.UPGRADE;
        }
        StructureBlockRequirement requirement;
        if (bool(json, "air", false)) {
            requirement = StructureBlockRequirement.air();
        } else if (bool(json, "wildcard", false)) {
            requirement = StructureBlockRequirement.wildcard();
        } else if (json.has("tag")) {
            requirement = new StructureBlockRequirement(StructureBlockRequirement.SlotKind.BLOCK_TAG,
                    null, Identifier.parse(json.get("tag").getAsString()), List.of(), optional, "");
        } else if (json.has("blocks")) {
            requirement = new StructureBlockRequirement(StructureBlockRequirement.SlotKind.BLOCK_LIST,
                    null, null, identifiers(json.get("blocks")), optional, "");
        } else if (json.has("block")) {
            Identifier block = Identifier.parse(json.get("block").getAsString());
            requirement = new StructureBlockRequirement(forcedKind == null ? StructureBlockRequirement.SlotKind.EXACT_BLOCK : forcedKind,
                    block, null, List.of(), optional, "");
        } else {
            requirement = StructureBlockRequirement.wildcard();
        }
        if (optional && !requirement.optional()) {
            requirement = requirement.asOptional();
        }
        return requirement;
    }

    private static List<List<String>> layers(JsonObject json, int width, int height, int depth, Identifier id) {
        JsonElement element = json.get("layers");
        if (element == null || !element.isJsonArray()) {
            throw new JsonParseException("Multiblock " + id + " must define layers.");
        }
        JsonArray layerArray = element.getAsJsonArray();
        if (layerArray.size() != height) {
            throw new JsonParseException("Multiblock " + id + " expected " + height + " layer(s), found " + layerArray.size() + ".");
        }
        List<List<String>> layers = new ArrayList<>();
        for (JsonElement layerElement : layerArray) {
            if (!layerElement.isJsonArray()) {
                throw new JsonParseException("Each layer in " + id + " must be an array of row strings.");
            }
            JsonArray rows = layerElement.getAsJsonArray();
            if (rows.size() != depth) {
                throw new JsonParseException("Multiblock " + id + " layer expected " + depth + " row(s), found " + rows.size() + ".");
            }
            List<String> layer = new ArrayList<>();
            for (JsonElement rowElement : rows) {
                String row = rowElement.getAsString();
                if (row.length() != width) {
                    throw new JsonParseException("Multiblock " + id + " row '" + row + "' expected width " + width + ".");
                }
                layer.add(row);
            }
            layers.add(List.copyOf(layer));
        }
        return List.copyOf(layers);
    }

    private static MultiblockDefinition.IntegrityRules integrity(JsonObject json) {
        if (json == null) {
            return MultiblockDefinition.IntegrityRules.DEFAULT;
        }
        int max = integer(json, "max", 100);
        return new MultiblockDefinition.IntegrityRules(
                max,
                bool(json, "damageable", true),
                integer(json, "damaged_threshold", Math.round(max * 0.7F)),
                integer(json, "offline_threshold", Math.round(max * 0.25F)));
    }

    private static List<WorkcellDefinition> workcells(Identifier definitionId, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("workcells must be an array.");
        }
        List<WorkcellDefinition> workcells = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            JsonObject json = entry.getAsJsonObject();
            workcells.add(new WorkcellDefinition(
                    identifier(json, "id", Identifier.fromNamespaceAndPath(definitionId.getNamespace(), definitionId.getPath() + "/workcell_" + workcells.size())),
                    enumValue(WorkcellType.class, string(json, "type", WorkcellType.ASSEMBLY.name()), WorkcellType.ASSEMBLY),
                    blockPos(json.get("pos")),
                    blockPos(json.get("size"), new BlockPos(1, 1, 1)),
                    identifiers(json.get("required_blocks")),
                    identifiers(json.get("allowed_tasks")),
                    toolTypes(json.get("required_tools")),
                    string(json, "status", "Idle")));
        }
        return List.copyOf(workcells);
    }

    private static List<MultiblockDefinition.RoboticsRequirement> robotics(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonObject()) {
            throw new JsonParseException("robotics must be an object.");
        }
        JsonObject json = element.getAsJsonObject();
        return List.of(new MultiblockDefinition.RoboticsRequirement(integer(json, "min_arms", 0), toolTypes(json.get("required_tools"))));
    }

    private static List<CapabilityRequirement> capabilityRequirements(JsonElement element, String path, List<String> warnings) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException(path + ": expected an array.");
        }
        List<CapabilityRequirement> values = new ArrayList<>();
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException(path + "[" + index + "]: expected an object.");
            }
            JsonObject json = entry.getAsJsonObject();
            String raw = string(json, "capability", string(json, "id", ""));
            Identifier capabilityId = Identifier.tryParse(raw);
            if (raw.isBlank() || capabilityId == null || !raw.contains(":")) {
                throw new JsonParseException(path + "[" + index + "].capability: invalid capability id '" + raw + "'.");
            }
            values.add(new CapabilityRequirement(
                    capabilityId,
                    integer(json, "amount", integer(json, "cost", 0)),
                    integer(json, "throughput", 0),
                    string(json, "unit", "units"),
                    bool(json, "required", true)));
            index++;
        }
        return List.copyOf(values);
    }

    private static List<UpgradeSlotRule> upgradeSlotRules(JsonElement element, Identifier definitionId) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("$.upgrade_slots: expected an array.");
        }
        List<UpgradeSlotRule> values = new ArrayList<>();
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException("$.upgrade_slots[" + index + "]: expected an object.");
            }
            JsonObject json = entry.getAsJsonObject();
            values.add(new UpgradeSlotRule(
                    identifier(json, "id", Identifier.fromNamespaceAndPath(definitionId.getNamespace(), definitionId.getPath() + "/upgrade_" + index)),
                    blockPos(json.get("pos")),
                    identifiers(json.get("allowed_upgrades")),
                    bool(json, "required", false)));
            index++;
        }
        return List.copyOf(values);
    }

    private static List<MultiblockCapability> capabilities(JsonElement element) {
        return identifiers(element).stream().map(MultiblockCapability::of).toList();
    }

    private static List<Identifier> identifiers(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Expected array of identifiers.");
        }
        List<Identifier> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            if (!value.isJsonNull()) {
                values.add(Identifier.parse(value.getAsString()));
            }
        }
        return List.copyOf(values);
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Expected array of strings.");
        }
        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            String text = value.getAsString();
            if (!text.isBlank()) {
                values.add(text.strip());
            }
        }
        return List.copyOf(values);
    }

    private static List<RobotToolType> toolTypes(JsonElement element) {
        return stringList(element).stream()
                .map(value -> enumValue(RobotToolType.class, value, RobotToolType.GRIPPER))
                .toList();
    }

    private static int previewColor(JsonObject json) {
        String raw = json == null ? "00d8ff" : string(json, "color", "00d8ff");
        String clean = raw.replace("#", "").strip();
        try {
            return 0xFF000000 | Integer.parseUnsignedInt(clean, 16);
        } catch (RuntimeException exception) {
            return 0xFF00D8FF;
        }
    }

    private static void validateDefinition(MultiblockDefinition definition, List<String> warnings, List<String> errors) {
        if (BuiltInRegistries.BLOCK.getOptional(definition.controllerBlockId()).isEmpty()) {
            errors.add("$.controller: unknown controller block id " + definition.controllerBlockId() + ".");
        }
        int maxVolume;
        try {
            maxVolume = com.knoxhack.echomultiblockcore.Config.MAX_VALIDATION_VOLUME.get();
        } catch (RuntimeException exception) {
            maxVolume = 4096;
        }
        if (definition.volume() > maxVolume) {
            errors.add("$.size: volume " + definition.volume() + " exceeds configured maximum " + maxVolume + ".");
        }
        Set<Character> used = usedPaletteKeys(definition.layers());
        definition.palette().keySet().stream()
                .filter(key -> !used.contains(key))
                .forEach(key -> warnings.add("$.palette." + key + ": palette key is defined but not used by any layer."));
        Set<Identifier> workcellIds = new java.util.LinkedHashSet<>();
        for (WorkcellDefinition workcell : definition.workcells()) {
            if (!workcellIds.add(workcell.id())) {
                errors.add("$.workcells: duplicate workcell id " + workcell.id() + ".");
            }
        }
    }

    private static Set<Character> usedPaletteKeys(List<List<String>> layers) {
        Set<Character> used = new java.util.LinkedHashSet<>();
        for (List<String> layer : layers) {
            for (String row : layer) {
                for (int i = 0; i < row.length(); i++) {
                    used.add(row.charAt(i));
                }
            }
        }
        return used;
    }

    private static void warnInvalidPreviewColor(JsonObject json, List<String> warnings) {
        JsonObject preview = json == null ? null : json.getAsJsonObject("preview");
        if (preview == null || !preview.has("color")) {
            return;
        }
        String clean = string(preview, "color", "").replace("#", "").strip();
        if (!clean.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) {
            warnings.add("$.preview.color: invalid color '" + clean + "', falling back to 00d8ff.");
        }
    }

    private static void warnInvalidWorkcellEnums(JsonElement element, List<String> warnings) {
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            return;
        }
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonObject()) {
                JsonObject json = entry.getAsJsonObject();
                warnInvalidEnum(json, "type", WorkcellType.class, "$.workcells[" + index + "].type", warnings);
                if (json.has("required_tools") && json.get("required_tools").isJsonArray()) {
                    int toolIndex = 0;
                    for (JsonElement tool : json.getAsJsonArray("required_tools")) {
                        if (!isEnumValue(RobotToolType.class, tool.getAsString())) {
                            warnings.add("$.workcells[" + index + "].required_tools[" + toolIndex
                                    + "]: invalid tool type '" + tool.getAsString() + "', falling back to GRIPPER.");
                        }
                        toolIndex++;
                    }
                }
            }
            index++;
        }
    }

    private static <E extends Enum<E>> void warnInvalidEnum(JsonObject json, String key, Class<E> type, List<String> warnings) {
        warnInvalidEnum(json, key, type, "$." + key, warnings);
    }

    private static <E extends Enum<E>> void warnInvalidEnum(JsonObject json, String key, Class<E> type, String path, List<String> warnings) {
        if (json != null && json.has(key) && !isEnumValue(type, string(json, key, ""))) {
            warnings.add(path + ": invalid " + type.getSimpleName() + " '" + string(json, key, "") + "', using default.");
        }
    }

    private static <E extends Enum<E>> boolean isEnumValue(Class<E> type, String raw) {
        String normalized = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            Enum.valueOf(type, normalized);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static BlockPos blockPos(JsonElement element) {
        return blockPos(element, BlockPos.ZERO);
    }

    private static BlockPos blockPos(JsonElement element, BlockPos fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonArray() || element.getAsJsonArray().size() < 3) {
            throw new JsonParseException("Expected block position array [x,y,z].");
        }
        JsonArray array = element.getAsJsonArray();
        return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    private static int[] intArray(JsonObject json, String key, int expected) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() < expected) {
            throw new JsonParseException("Field '" + key + "' must be an array of " + expected + " integers.");
        }
        int[] values = new int[expected];
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < expected; i++) {
            values[i] = array.get(i).getAsInt();
        }
        return values;
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static String string(JsonObject json, String key, String fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, E fallback) {
        String normalized = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Enum.valueOf(type, normalized);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static Identifier contentId(Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = DIRECTORY + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }
}
