package com.knoxhack.echo.scriptcore.api;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoAction(
        String type,
        Optional<String> id,
        Optional<Identifier> mission,
        Optional<String> objective,
        Optional<Identifier> item,
        Optional<Integer> count,
        Optional<Identifier> entry,
        Optional<Identifier> tab,
        Optional<Identifier> layer,
        Optional<Identifier> marker,
        Optional<Identifier> faction,
        Optional<Integer> amount,
        Optional<Identifier> state,
        Optional<Identifier> weather,
        Optional<Identifier> sound,
        Optional<String> message,
        Optional<String> title,
        Optional<String> metric,
        Optional<String> value,
        Map<String, Object> metadata,
        JsonObject rawJson) {
    public EchoAction {
        type = type == null || type.isBlank() ? "noop" : type.trim().toLowerCase(java.util.Locale.ROOT);
        id = id == null ? Optional.empty() : id;
        mission = mission == null ? Optional.empty() : mission;
        objective = objective == null ? Optional.empty() : objective;
        item = item == null ? Optional.empty() : item;
        count = count == null ? Optional.empty() : count;
        entry = entry == null ? Optional.empty() : entry;
        tab = tab == null ? Optional.empty() : tab;
        layer = layer == null ? Optional.empty() : layer;
        marker = marker == null ? Optional.empty() : marker;
        faction = faction == null ? Optional.empty() : faction;
        amount = amount == null ? Optional.empty() : amount;
        state = state == null ? Optional.empty() : state;
        weather = weather == null ? Optional.empty() : weather;
        sound = sound == null ? Optional.empty() : sound;
        message = message == null ? Optional.empty() : message;
        title = title == null ? Optional.empty() : title;
        metric = metric == null ? Optional.empty() : metric;
        value = value == null ? Optional.empty() : value;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        rawJson = rawJson == null ? new JsonObject() : rawJson.deepCopy();
    }
}
