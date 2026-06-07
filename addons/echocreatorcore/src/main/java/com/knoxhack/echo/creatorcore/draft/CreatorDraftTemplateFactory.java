package com.knoxhack.echo.creatorcore.draft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class CreatorDraftTemplateFactory {
    private CreatorDraftTemplateFactory() {
    }

    public static CreatorDraft create(String type, Identifier id, String pack, String createdBy) {
        String safeType = type == null || type.isBlank() ? "mission" : type;
        JsonObject content = switch (safeType) {
            case "mission" -> mission(id);
            case "archive_entry" -> archiveEntry(id);
            case "lens_scan" -> lensScan(id);
            case "holomap_layer" -> holomapLayer(id);
            case "holomap_marker" -> holomapMarker(id);
            case "weather_event" -> weatherEvent(id);
            case "faction" -> faction(id);
            case "world_state" -> worldState(id);
            case "tutorial_hint" -> tutorialHint(id);
            case "dialogue" -> dialogue(id);
            case "ending" -> ending(id);
            case "recipe_unlock" -> recipeUnlock(id);
            case "loot_profile" -> lootProfile(id);
            default -> generic(safeType, id);
        };
        String title = titleFrom(id, safeType);
        Instant now = Instant.now();
        return new CreatorDraft(id, safeType, pack, title, content, "internal", now, now,
                createdBy == null || createdBy.isBlank() ? "system" : createdBy,
                List.of(), CreatorDraft.DraftStatus.NEW);
    }

    public static CreatorDraft exampleMission() {
        Identifier id = Identifier.fromNamespaceAndPath("example", "repair_radio");
        CreatorDraft draft = create("mission", id, "example", "creatorcore");
        draft.content().add("objectives", arrayOf(
                object("id", "iron", "type", "collect_item", "item", "minecraft:iron_ingot", "count", 4),
                object("id", "redstone", "type", "collect_item", "item", "minecraft:redstone", "count", 2)));
        draft.content().add("rewards", arrayOf(
                object("type", "unlock_archive_entry", "entry", "example:first_signal")));
        return draft;
    }

    public static boolean applyTemplateSection(JsonObject content, String type, String section) {
        if (content == null || section == null || section.isBlank()) {
            return false;
        }
        String normalized = section.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "objective", "objectives" -> append(content, "objectives",
                    object("id", "collect_sample", "type", "collect_item", "item", "minecraft:iron_ingot", "count", 1));
            case "reward", "rewards" -> append(content, "rewards", object("type", "noop"));
            case "condition", "conditions" -> append(content, "conditions", object("type", "always"));
            case "unlock_condition", "unlock_conditions" -> append(content, "unlock_conditions", object("type", "always"));
            case "action", "actions" -> append(content, "actions", object("type", "noop"));
            case "on_start" -> append(content, "on_start", object("type", "noop"));
            case "on_complete" -> append(content, "on_complete", object("type", "noop"));
            case "choice", "choices" -> append(content, "choices",
                    object("id", "continue", "label", "Continue", "next_dialogue", ""));
            case "rank", "ranks" -> append(content, "ranks",
                    object("name", "Neutral", "min", 0, "color", "#b7d7e3"));
            case "marker", "markers" -> append(content, "markers",
                    object("id", "example:shelter", "type", "holomap_marker", "title", "Shelter"));
            case "effect", "effects" -> append(content, "effects", object("type", "noop"));
            default -> {
                return false;
            }
        }
        return true;
    }

    private static JsonObject mission(Identifier id) {
        JsonObject root = base("mission", id);
        root.addProperty("chapter", "example:chapter_one");
        root.addProperty("phase", "Opening Signal");
        root.addProperty("briefing", "Write the mission briefing here.");
        root.addProperty("description", "Describe the mission route and player goal.");
        root.addProperty("role", "optional");
        root.add("prerequisites", new JsonArray());
        root.add("objectives", arrayOf(object("id", "collect_sample", "type", "collect_item",
                "item", "minecraft:iron_ingot", "count", 1)));
        root.add("rewards", arrayOf(object("type", "noop")));
        return root;
    }

    private static JsonObject archiveEntry(Identifier id) {
        JsonObject root = base("archive_entry", id);
        root.addProperty("category", "lore");
        root.add("content", arrayOf("Write archive text here."));
        return root;
    }

    private static JsonObject lensScan(Identifier id) {
        JsonObject root = base("lens_scan", id);
        root.addProperty("target", "minecraft:stone");
        root.addProperty("target_type", "block");
        root.addProperty("summary", "Scan result placeholder.");
        root.add("details", arrayOf("Add scan detail lines here."));
        return root;
    }

    private static JsonObject holomapLayer(Identifier id) {
        JsonObject root = base("holomap_layer", id);
        root.addProperty("locked_by_default", false);
        root.add("markers", new JsonArray());
        return root;
    }

    private static JsonObject holomapMarker(Identifier id) {
        JsonObject root = base("holomap_marker", id);
        root.addProperty("dimension", "minecraft:overworld");
        root.addProperty("x", 0);
        root.addProperty("y", 64);
        root.addProperty("z", 0);
        root.addProperty("icon", "generic");
        root.addProperty("layer", id.getNamespace() + ":default_layer");
        return root;
    }

    private static JsonObject weatherEvent(Identifier id) {
        JsonObject root = base("weather_event", id);
        root.addProperty("duration_ticks", 6000);
        root.addProperty("warning_seconds", 10);
        root.add("effects", arrayOf(object("type", "noop")));
        return root;
    }

    private static JsonObject faction(Identifier id) {
        JsonObject root = base("faction", id);
        root.addProperty("display_name", titleFrom(id, "faction"));
        root.addProperty("description", "Faction description.");
        root.addProperty("starting_reputation", 0);
        root.add("ranks", arrayOf(object("name", "Neutral", "min", 0, "color", "#b7d7e3")));
        root.add("reputation_events", new JsonArray());
        return root;
    }

    private static JsonObject worldState(Identifier id) {
        JsonObject root = base("world_state", id);
        root.add("set_by", arrayOf(object("type", "always")));
        root.add("effects", arrayOf(object("type", "noop")));
        return root;
    }

    private static JsonObject tutorialHint(Identifier id) {
        JsonObject root = base("tutorial_hint", id);
        root.addProperty("message", "Tutorial hint placeholder.");
        root.addProperty("priority", 0);
        root.addProperty("once", true);
        root.add("trigger_conditions", arrayOf(object("type", "always")));
        return root;
    }

    private static JsonObject dialogue(Identifier id) {
        JsonObject root = base("dialogue", id);
        root.addProperty("speaker", "Guide");
        root.add("lines", arrayOf("Write dialogue here."));
        root.add("choices", arrayOf(object("id", "continue", "label", "Continue", "next_dialogue", "")));
        return root;
    }

    private static JsonObject ending(Identifier id) {
        JsonObject root = base("ending", id);
        root.addProperty("description", "Describe this ending.");
        root.addProperty("priority", 0);
        root.add("conditions", arrayOf(object("type", "always")));
        return root;
    }

    private static JsonObject recipeUnlock(Identifier id) {
        JsonObject root = base("recipe_unlock", id);
        root.addProperty("recipe", "minecraft:bread");
        root.add("unlock_conditions", arrayOf(object("type", "always")));
        root.add("actions", arrayOf(object("type", "noop")));
        return root;
    }

    private static JsonObject lootProfile(Identifier id) {
        JsonObject root = base("loot_profile", id);
        root.addProperty("table", "minecraft:chests/simple_dungeon");
        root.add("entries", new JsonArray());
        root.add("unlock_conditions", arrayOf(object("type", "always")));
        return root;
    }

    private static JsonObject generic(String type, Identifier id) {
        JsonObject root = base(type, id);
        root.addProperty("notes", "Generic CreatorCore draft placeholder.");
        return root;
    }

    private static JsonObject base(String type, Identifier id) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        root.addProperty("pack", id == null ? "example" : id.getNamespace());
        root.addProperty("id", id == null ? "example:untitled" : id.toString());
        root.addProperty("type", type);
        root.addProperty("title", titleFrom(id, type));
        return root;
    }

    private static String titleFrom(Identifier id, String type) {
        String path = id == null ? type : id.getPath();
        String cleaned = path.replace('_', ' ').replace('/', ' ');
        return cleaned.isBlank() ? type : Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private static JsonArray arrayOf(JsonObject... objects) {
        JsonArray array = new JsonArray();
        for (JsonObject object : objects) {
            array.add(object);
        }
        return array;
    }

    private static JsonArray arrayOf(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject object(String k1, String v1) {
        JsonObject object = new JsonObject();
        object.addProperty(k1, v1);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, String v2) {
        JsonObject object = object(k1, v1);
        object.addProperty(k2, v2);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, int v2) {
        JsonObject object = object(k1, v1);
        object.addProperty(k2, v2);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, String v2, String k3, String v3) {
        JsonObject object = object(k1, v1, k2, v2);
        object.addProperty(k3, v3);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, String v2, String k3, int v3) {
        JsonObject object = object(k1, v1, k2, v2);
        object.addProperty(k3, v3);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, int v2, String k3, String v3) {
        JsonObject object = object(k1, v1, k2, v2);
        object.addProperty(k3, v3);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
        JsonObject object = object(k1, v1, k2, v2, k3, v3);
        object.addProperty(k4, v4);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, String v2, String k3, String v3, String k4, int v4) {
        JsonObject object = object(k1, v1, k2, v2, k3, v3);
        object.addProperty(k4, v4);
        return object;
    }

    private static JsonObject object(String k1, String v1, String k2, int v2, String k3, String v3, String k4, String v4) {
        JsonObject object = object(k1, v1, k2, v2, k3, v3);
        object.addProperty(k4, v4);
        return object;
    }

    private static void append(JsonObject root, String arrayName, JsonObject value) {
        JsonArray array = root.has(arrayName) && root.get(arrayName).isJsonArray()
                ? root.getAsJsonArray(arrayName)
                : new JsonArray();
        array.add(value);
        root.add(arrayName, array);
    }
}
