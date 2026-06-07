package com.knoxhack.echotutorialcore.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.api.TutorialCondition;
import com.knoxhack.echotutorialcore.api.TutorialConditionType;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.TutorialHintType;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.api.tooltip.TutorialTooltip;
import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialStep;
import com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class TutorialDataReloadListener extends SimplePreparableReloadListener<Void> {
    private static final String CARDS_DIR = "tutorial_cards";
    private static final String HINTS_DIR = "tutorial_hints";
    private static final String FLOWS_DIR = "tutorial_flows";
    private static final String TOOLTIPS_DIR = "tutorial_tooltips";

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
        TutorialCoreRegistries.clearAll();
        loadCards(manager);
        loadHints(manager);
        loadFlows(manager);
        loadTooltips(manager);
        List<String> warnings = TutorialCoreRegistries.validate();
        for (String warning : warnings) {
            EchoTutorialCore.LOGGER.warn("TutorialCore data validation: {}", warning);
        }
        EchoTutorialCore.LOGGER.info("TutorialCore reloaded: {} cards, {} hints, {} flows, {} tooltips.",
                TutorialCoreRegistries.cardCount(),
                TutorialCoreRegistries.hintCount(),
                TutorialCoreRegistries.flowCount(),
                TutorialCoreRegistries.tooltipCount());
    }

    private static void loadCards(ResourceManager manager) {
        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, CARDS_DIR).entrySet()) {
            try {
                TutorialCard card = parseCard(entry.getKey(), entry.getValue());
                if (card != null) {
                    TutorialCoreRegistries.registerCard(card);
                }
            } catch (Exception e) {
                EchoTutorialCore.LOGGER.error("Failed to parse tutorial card {}.", entry.getKey(), e);
            }
        }
    }

    private static void loadHints(ResourceManager manager) {
        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, HINTS_DIR).entrySet()) {
            try {
                TutorialHint hint = parseHint(entry.getKey(), entry.getValue());
                if (hint != null) {
                    TutorialCoreRegistries.registerHint(hint);
                }
            } catch (Exception e) {
                EchoTutorialCore.LOGGER.error("Failed to parse tutorial hint {}.", entry.getKey(), e);
            }
        }
    }

    private static void loadFlows(ResourceManager manager) {
        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, FLOWS_DIR).entrySet()) {
            try {
                TutorialFlow flow = parseFlow(entry.getKey(), entry.getValue());
                if (flow != null) {
                    TutorialCoreRegistries.registerFlow(flow);
                }
            } catch (Exception e) {
                EchoTutorialCore.LOGGER.error("Failed to parse tutorial flow {}.", entry.getKey(), e);
            }
        }
    }

    private static TutorialCard parseCard(Identifier id, JsonObject json) {
        Identifier cardId = idFromJson(json, "id", id);
        TutorialCategory category = category(string(json, "category", "START_HERE"));
        String title = string(json, "title", cardId.toString());
        String summary = string(json, "summary", "");
        List<String> body = strings(json, "body");
        List<String> steps = strings(json, "steps");
        List<String> mistakes = strings(json, "commonMistakes");
        List<Identifier> related = ids(json, "related");
        List<String> unlockTriggers = strings(json, "unlockTriggers");
        boolean defaultUnlocked = bool(json, "defaultUnlocked", false);
        String addonOwner = string(json, "addonOwnerId", cardId.getNamespace());
        int priority = integer(json, "priority", 0);
        return new TutorialCard(cardId, category, title, summary, body, steps, mistakes, related, unlockTriggers, defaultUnlocked, addonOwner, priority);
    }

    private static TutorialHint parseHint(Identifier id, JsonObject json) {
        Identifier hintId = idFromJson(json, "id", id);
        TutorialHintType type = hintType(string(json, "type", "INFO"));
        TutorialCategory category = category(string(json, "category", "START_HERE"));
        String title = string(json, "title", hintId.toString());
        String message = string(json, "message", "");
        String details = string(json, "details", "");
        String actionLabel = string(json, "actionLabel", "");
        Identifier actionCardId = idFromString(json, "actionCardId");
        int cooldownTicks = integer(json, "cooldownTicks", 6000);
        Set<TutorialGuideMode> guideModes = guideModes(json, "guideModes");
        int priority = integer(json, "priority", 0);
        boolean dismissible = bool(json, "dismissible", true);
        List<String> conditions = conditionStrings(json, "conditions");
        return new TutorialHint(hintId, type, category, title, message, details, actionLabel, actionCardId, cooldownTicks, guideModes, priority, dismissible, conditions);
    }

    private static void loadTooltips(ResourceManager manager) {
        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, TOOLTIPS_DIR).entrySet()) {
            try {
                TutorialTooltip tooltip = parseTooltip(entry.getKey(), entry.getValue());
                if (tooltip != null) {
                    TutorialCoreRegistries.registerTooltip(tooltip);
                }
            } catch (Exception e) {
                EchoTutorialCore.LOGGER.error("Failed to parse tutorial tooltip {}.", entry.getKey(), e);
            }
        }
    }

    private static TutorialFlow parseFlow(Identifier id, JsonObject json) {
        Identifier flowId = idFromJson(json, "id", id);
        String title = string(json, "title", flowId.toString());
        TutorialCategory category = category(string(json, "category", "START_HERE"));
        List<TutorialStep> steps = new ArrayList<>();
        JsonArray stepsArray = array(json, "steps");
        if (stepsArray != null) {
            for (JsonElement e : stepsArray) {
                if (e.isJsonObject()) {
                    TutorialStep step = parseStep(e.getAsJsonObject());
                    if (step != null) steps.add(step);
                }
            }
        }
        List<Identifier> unlockCards = ids(json, "unlockCards");
        boolean defaultUnlocked = bool(json, "defaultUnlocked", false);
        return new TutorialFlow(flowId, title, category, steps, unlockCards, defaultUnlocked);
    }

    private static TutorialStep parseStep(JsonObject json) {
        String id = string(json, "id", "");
        TutorialTriggerType type = triggerType(string(json, "type", "CUSTOM"));
        Identifier target = idFromString(json, "target");
        String text = string(json, "text", "");
        boolean optional = bool(json, "optional", false);
        return new TutorialStep(id, type, target, text, optional);
    }

    private static TutorialTooltip parseTooltip(Identifier id, JsonObject json) {
        Identifier targetItem = idFromJson(json, "targetItem", id);
        List<String> lines = strings(json, "lines");
        boolean requireShift = bool(json, "requireShift", false);
        int priority = integer(json, "priority", 0);
        return new TutorialTooltip(targetItem, lines, requireShift, priority);
    }

    private static Map<Identifier, JsonObject> jsonObjects(ResourceManager manager, String directory) {
        Map<Identifier, JsonObject> objects = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, p -> p.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier id = contentId(resourceId, directory);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                objects.put(id, root.getAsJsonObject());
            } catch (IOException | RuntimeException e) {
                EchoTutorialCore.LOGGER.warn("Could not parse TutorialCore data file {}.", resourceId, e);
            }
        }
        return objects;
    }

    private static Identifier contentId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        String prefix = directory + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static Identifier idFromJson(JsonObject json, String key, Identifier fallback) {
        JsonElement e = json.get(key);
        if (e == null || e.isJsonNull()) return fallback;
        Identifier id = Identifier.tryParse(e.getAsString());
        return id != null ? id : fallback;
    }

    private static Identifier idFromString(JsonObject json, String key) {
        JsonElement e = json.get(key);
        if (e == null || e.isJsonNull()) return null;
        return Identifier.tryParse(e.getAsString());
    }

    private static List<Identifier> ids(JsonObject json, String key) {
        List<Identifier> list = new ArrayList<>();
        JsonArray arr = array(json, key);
        if (arr != null) {
            for (JsonElement e : arr) {
                Identifier id = Identifier.tryParse(e.getAsString());
                if (id != null) list.add(id);
            }
        }
        return list;
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> list = new ArrayList<>();
        JsonArray arr = array(json, key);
        if (arr != null) {
            for (JsonElement e : arr) {
                if (e.isJsonPrimitive()) list.add(e.getAsString());
            }
        }
        return list;
    }

    private static List<String> conditionStrings(JsonObject json, String key) {
        List<String> list = new ArrayList<>();
        JsonArray arr = array(json, key);
        if (arr != null) {
            for (JsonElement e : arr) {
                if (e.isJsonPrimitive()) {
                    list.add(e.getAsString());
                } else if (e.isJsonObject()) {
                    list.add(parseCondition(e.getAsJsonObject()).asLegacyString());
                }
            }
        }
        return list;
    }

    private static TutorialCondition parseCondition(JsonObject json) {
        TutorialConditionType type = TutorialConditionType.byName(string(json, "type", "PROGRESS"));
        Identifier target = idFromString(json, "target");
        int count = integer(json, "count", 1);
        boolean invert = bool(json, "invert", false);
        String addon = string(json, "addon", "");
        Map<String, String> context = new LinkedHashMap<>();
        JsonObject contextObject = object(json, "context");
        if (contextObject != null) {
            for (Map.Entry<String, JsonElement> entry : contextObject.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    context.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        if (json.has("value") && json.get("value").isJsonPrimitive()) {
            context.put("value", json.get("value").getAsString());
        }
        return new TutorialCondition(type, target, count, invert, addon, context);
    }

    private static Set<TutorialGuideMode> guideModes(JsonObject json, String key) {
        JsonArray arr = array(json, key);
        if (arr == null) return EnumSet.allOf(TutorialGuideMode.class);
        EnumSet<TutorialGuideMode> modes = EnumSet.noneOf(TutorialGuideMode.class);
        for (JsonElement e : arr) {
            modes.add(TutorialGuideMode.byName(e.getAsString()));
        }
        return modes.isEmpty() ? EnumSet.allOf(TutorialGuideMode.class) : modes;
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsInt();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement e = json.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsBoolean();
    }

    private static TutorialCategory category(String value) {
        try {
            return TutorialCategory.valueOf(normalize(value));
        } catch (IllegalArgumentException e) {
            return TutorialCategory.START_HERE;
        }
    }

    private static TutorialHintType hintType(String value) {
        try {
            return TutorialHintType.valueOf(normalize(value));
        } catch (IllegalArgumentException e) {
            return TutorialHintType.INFO;
        }
    }

    private static TutorialTriggerType triggerType(String value) {
        try {
            return TutorialTriggerType.valueOf(normalize(value));
        } catch (IllegalArgumentException e) {
            return TutorialTriggerType.CUSTOM;
        }
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
