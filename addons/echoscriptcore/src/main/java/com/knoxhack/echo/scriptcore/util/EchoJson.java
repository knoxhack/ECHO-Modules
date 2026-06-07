package com.knoxhack.echo.scriptcore.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoJson {
    private EchoJson() {
    }

    public static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    public static Optional<String> optionalString(JsonObject json, String key) {
        String value = string(json, key, "");
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json == null ? null : json.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static double decimal(JsonObject json, String key, double fallback) {
        JsonElement element = json == null ? null : json.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json == null ? null : json.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static JsonObject object(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    public static JsonArray array(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    public static List<String> strings(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return List.copyOf(values);
    }

    public static Optional<Identifier> id(JsonObject json, String key) {
        String value = string(json, key, "");
        Identifier id = value.isBlank() ? null : Identifier.tryParse(value);
        return Optional.ofNullable(id);
    }

    public static Identifier id(JsonObject json, String key, Identifier fallback) {
        return id(json, key).orElse(fallback);
    }

    public static List<Identifier> ids(JsonObject json, String key) {
        List<Identifier> ids = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    public static List<EchoCondition> conditions(JsonObject json, String key) {
        List<EchoCondition> conditions = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (element.isJsonObject()) {
                conditions.add(condition(element.getAsJsonObject()));
            }
        }
        return List.copyOf(conditions);
    }

    public static EchoCondition condition(JsonObject json) {
        return new EchoCondition(
                string(json, "type", "always"),
                optionalString(json, "id"),
                bool(json, "not", false) || bool(json, "negated", false),
                conditions(json, "all"),
                conditions(json, "any"),
                id(json, "mission"),
                optionalString(json, "objective"),
                id(json, "item"),
                id(json, "block"),
                id(json, "entity"),
                id(json, "poi"),
                id(json, "region"),
                id(json, "faction"),
                optionalInt(json, "amount"),
                id(json, "state"),
                id(json, "weather"),
                id(json, "dimension"),
                id(json, "biome"),
                optionalInt(json, "count"),
                optionalPrimitive(json, "value"),
                optionalString(json, "metric"),
                objectMap(object(json, "metadata")),
                json);
    }

    public static List<EchoAction> actions(JsonObject json, String key) {
        List<EchoAction> actions = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (element.isJsonObject()) {
                actions.add(action(element.getAsJsonObject()));
            }
        }
        return List.copyOf(actions);
    }

    public static EchoAction action(JsonObject json) {
        return new EchoAction(
                string(json, "type", "noop"),
                optionalString(json, "id"),
                id(json, "mission"),
                optionalString(json, "objective"),
                id(json, "item"),
                optionalInt(json, "count"),
                id(json, "entry"),
                id(json, "tab"),
                id(json, "layer"),
                id(json, "marker"),
                id(json, "faction"),
                optionalInt(json, "amount"),
                id(json, "state"),
                id(json, "weather"),
                id(json, "sound"),
                optionalString(json, "message"),
                optionalString(json, "title"),
                optionalString(json, "metric"),
                optionalPrimitive(json, "value"),
                objectMap(object(json, "metadata")),
                json);
    }

    public static Optional<Integer> optionalInt(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsInt()) : Optional.empty();
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<String> optionalPrimitive(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsString()) : Optional.empty();
    }

    public static Map<String, Object> objectMap(JsonObject json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null) {
            return Map.of();
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), value(entry.getValue()));
        }
        return Map.copyOf(map);
    }

    private static Object value(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                return primitive.getAsNumber();
            }
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                values.add(value(child));
            }
            return List.copyOf(values);
        }
        return objectMap(element.getAsJsonObject());
    }
}
