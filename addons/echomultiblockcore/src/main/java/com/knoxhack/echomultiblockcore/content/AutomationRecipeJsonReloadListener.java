package com.knoxhack.echomultiblockcore.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationIngredient;
import com.knoxhack.echomultiblockcore.api.AutomationOutput;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.CapabilityRequirement;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipeParseResult;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.api.WorkcellType;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class AutomationRecipeJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, MultiblockAutomationRecipe>> {
    private static final String DIRECTORY = "echo_multiblock_tasks";

    @Override
    protected Map<Identifier, MultiblockAutomationRecipe> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, MultiblockAutomationRecipe> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                MultiblockAutomationRecipeParseResult result = parseRecipeResult(resourceId, fallbackId, root.getAsJsonObject());
                result.warnings().forEach(warning -> EchoMultiblockCore.LOGGER.warn(
                        "Automation recipe warning in {} [{}]: {}", resourceId, result.recipeId(), warning));
                result.errors().forEach(error -> EchoMultiblockCore.LOGGER.warn(
                        "Automation recipe error in {} [{}]: {}", resourceId, result.recipeId(), error));
                if (!result.valid()) {
                    continue;
                }
                MultiblockAutomationRecipe recipe = result.recipe();
                if (loaded.put(recipe.id(), recipe) != null) {
                    EchoMultiblockCore.LOGGER.warn("Duplicate automation recipe id {} from {} replaced an earlier entry.",
                            recipe.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("Could not parse automation recipe file {}.", resourceId, exception);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, MultiblockAutomationRecipe> content, ResourceManager manager, ProfilerFiller profiler) {
        if ((content == null || content.isEmpty()) && !AutomationRecipeRegistry.snapshot().isEmpty()) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore reload produced no valid automation recipes; keeping last good registry.");
            return;
        }
        AutomationRecipeRegistry.replaceRecipes(content);
    }

    public static MultiblockAutomationRecipe parseRecipeForTests(Identifier fallbackId, JsonObject json) {
        MultiblockAutomationRecipeParseResult result = parseRecipeResultForTests(fallbackId, json);
        if (!result.valid()) {
            throw new JsonParseException(String.join("; ", result.errors()));
        }
        return result.recipe();
    }

    public static MultiblockAutomationRecipeParseResult parseRecipeResultForTests(Identifier fallbackId, JsonObject json) {
        return parseRecipeResult(fallbackId, fallbackId, json);
    }

    private static MultiblockAutomationRecipeParseResult parseRecipeResult(Identifier resourceId, Identifier fallbackId, JsonObject json) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Identifier parsedId = fallbackId;
        MultiblockAutomationRecipe recipe = null;
        try {
            warnInvalidEnum(json, "required_workcell", WorkcellType.class, "$.required_workcell", warnings);
            warnInvalidTools(json.get("required_tools"), "$.required_tools", warnings);
            recipe = parseRecipe(fallbackId, json, warnings);
            parsedId = recipe.id();
            validateRecipe(recipe, warnings, errors);
        } catch (RuntimeException exception) {
            errors.add("$.root: " + exception.getMessage());
        }
        return new MultiblockAutomationRecipeParseResult(resourceId, parsedId, errors.isEmpty() ? recipe : null, warnings, errors);
    }

    private static MultiblockAutomationRecipe parseRecipe(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        Identifier category = category(json, "category", Identifier.fromNamespaceAndPath(id.getNamespace(), "assembly"));
        return new MultiblockAutomationRecipe(
                id,
                string(json, "display_name", string(json, "displayName", id.getPath().replace('_', ' '))),
                category,
                identifiers(json.get("allowed_multiblocks")),
                enumValue(WorkcellType.class, string(json, "required_workcell", WorkcellType.ASSEMBLY.name()), WorkcellType.ASSEMBLY),
                toolTypes(json.get("required_tools")),
                ingredients(json.get("inputs")),
                outputs(json.get("outputs")),
                bool(json, "consume_inputs_on_start", true),
                integer(json, "duration_ticks", integer(json, "duration", 200)),
                integer(json, "heat_per_second", integer(json, "heat", 2)),
                string(json, "animation", "assemble"),
                integer(json, "integrity_repair", 0),
                strings(json.get("notes")),
                effectIds(json.get("effects"), "$.effects", warnings),
                capabilityRequirements(json.get("capability_costs"), "$.capability_costs", warnings),
                identifiers(json.get("required_upgrades")),
                integer(json, "repair_priority", integer(json, "repairPriority", 0)),
                identifierOptional(json, "animation_profile", null),
                bool(json, "auto_builder_eligible", true));
    }

    private static List<AutomationIngredient> ingredients(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("$.inputs: expected an array.");
        }
        List<AutomationIngredient> values = new ArrayList<>();
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException("$.inputs[" + index + "]: expected an object.");
            }
            JsonObject json = entry.getAsJsonObject();
            int count = integer(json, "count", 1);
            String label = string(json, "label", "");
            if (json.has("item")) {
                values.add(new AutomationIngredient(Identifier.parse(json.get("item").getAsString()), null, count, label));
            } else if (json.has("tag")) {
                values.add(new AutomationIngredient(null, Identifier.parse(json.get("tag").getAsString()), count, label));
            } else {
                throw new JsonParseException("$.inputs[" + index + "]: expected item or tag.");
            }
            index++;
        }
        return List.copyOf(values);
    }

    private static List<AutomationOutput> outputs(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("$.outputs: expected an array.");
        }
        List<AutomationOutput> values = new ArrayList<>();
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException("$.outputs[" + index + "]: expected an object.");
            }
            JsonObject json = entry.getAsJsonObject();
            if (!json.has("item")) {
                throw new JsonParseException("$.outputs[" + index + "]: expected item.");
            }
            values.add(new AutomationOutput(
                    Identifier.parse(json.get("item").getAsString()),
                    integer(json, "count", 1),
                    string(json, "label", "")));
            index++;
        }
        return List.copyOf(values);
    }

    private static void validateRecipe(MultiblockAutomationRecipe recipe, List<String> warnings, List<String> errors) {
        if (recipe.inputs().isEmpty() && recipe.outputs().isEmpty() && recipe.integrityRepair() <= 0) {
            warnings.add("$.outputs: recipe has no inputs, outputs, or repair effect.");
        }
        for (int i = 0; i < recipe.inputs().size(); i++) {
            AutomationIngredient ingredient = recipe.inputs().get(i);
            if (ingredient.itemId() != null && BuiltInRegistries.ITEM.getOptional(ingredient.itemId()).isEmpty()) {
                errors.add("$.inputs[" + i + "].item: unknown item id " + ingredient.itemId() + ".");
            }
        }
        for (int i = 0; i < recipe.outputs().size(); i++) {
            AutomationOutput output = recipe.outputs().get(i);
            if (BuiltInRegistries.ITEM.getOptional(output.itemId()).isEmpty()) {
                errors.add("$.outputs[" + i + "].item: unknown item id " + output.itemId() + ".");
            }
        }
    }

    private static List<RobotToolType> toolTypes(JsonElement element) {
        return strings(element).stream()
                .map(value -> enumValue(RobotToolType.class, value, RobotToolType.GRIPPER))
                .toList();
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

    private static List<Identifier> effectIds(JsonElement element, String path, List<String> warnings) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            warnings.add(path + ": expected an array of namespaced effect identifiers; ignoring effects.");
            return List.of();
        }
        List<Identifier> values = new ArrayList<>();
        int index = 0;
        for (JsonElement value : element.getAsJsonArray()) {
            String raw = value == null || value.isJsonNull() ? "" : value.getAsString();
            Identifier effectId = Identifier.tryParse(raw);
            if (raw.isBlank() || effectId == null || !raw.contains(":")) {
                warnings.add(path + "[" + index + "]: invalid automation effect id '" + raw + "'; ignoring.");
            } else {
                values.add(effectId);
            }
            index++;
        }
        return List.copyOf(values);
    }

    private static List<CapabilityRequirement> capabilityRequirements(JsonElement element, String path, List<String> warnings) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            warnings.add(path + ": expected an array of capability requirement objects; ignoring.");
            return List.of();
        }
        List<CapabilityRequirement> values = new ArrayList<>();
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                warnings.add(path + "[" + index + "]: expected an object; ignoring.");
                index++;
                continue;
            }
            JsonObject json = entry.getAsJsonObject();
            String raw = string(json, "capability", string(json, "id", ""));
            Identifier capabilityId = Identifier.tryParse(raw);
            if (raw.isBlank() || capabilityId == null || !raw.contains(":")) {
                warnings.add(path + "[" + index + "].capability: invalid capability id '" + raw + "'; ignoring.");
            } else {
                values.add(new CapabilityRequirement(
                        capabilityId,
                        integer(json, "amount", integer(json, "cost", 0)),
                        integer(json, "throughput", 0),
                        string(json, "unit", "units"),
                        bool(json, "required", true)));
            }
            index++;
        }
        return List.copyOf(values);
    }

    private static List<String> strings(JsonElement element) {
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

    private static Identifier category(JsonObject json, String key, Identifier fallback) {
        String raw = string(json, key, "");
        if (raw.isBlank()) {
            return fallback;
        }
        return raw.contains(":")
                ? Identifier.parse(raw)
                : Identifier.fromNamespaceAndPath(fallback.getNamespace(), raw);
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static Identifier identifierOptional(JsonObject json, String key, Identifier fallback) {
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

    private static <E extends Enum<E>> void warnInvalidEnum(JsonObject json, String key, Class<E> type, String path, List<String> warnings) {
        if (json != null && json.has(key) && !isEnumValue(type, string(json, key, ""))) {
            warnings.add(path + ": invalid " + type.getSimpleName() + " '" + string(json, key, "") + "', using default.");
        }
    }

    private static void warnInvalidTools(JsonElement element, String path, List<String> warnings) {
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            return;
        }
        int index = 0;
        for (JsonElement value : element.getAsJsonArray()) {
            if (!isEnumValue(RobotToolType.class, value.getAsString())) {
                warnings.add(path + "[" + index + "]: invalid RobotToolType '" + value.getAsString() + "', falling back to GRIPPER.");
            }
            index++;
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
