package com.knoxhack.echo.scriptcore.api;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoCondition(
        String type,
        Optional<String> id,
        boolean negated,
        List<EchoCondition> all,
        List<EchoCondition> any,
        Optional<Identifier> mission,
        Optional<String> objective,
        Optional<Identifier> item,
        Optional<Identifier> block,
        Optional<Identifier> entity,
        Optional<Identifier> poi,
        Optional<Identifier> region,
        Optional<Identifier> faction,
        Optional<Integer> amount,
        Optional<Identifier> state,
        Optional<Identifier> weather,
        Optional<Identifier> dimension,
        Optional<Identifier> biome,
        Optional<Integer> count,
        Optional<String> value,
        Optional<String> metric,
        Map<String, Object> metadata,
        JsonObject rawJson) {
    public EchoCondition {
        type = type == null || type.isBlank() ? "always" : type.trim().toLowerCase(java.util.Locale.ROOT);
        id = id == null ? Optional.empty() : id;
        all = List.copyOf(all == null ? List.of() : all);
        any = List.copyOf(any == null ? List.of() : any);
        mission = mission == null ? Optional.empty() : mission;
        objective = objective == null ? Optional.empty() : objective;
        item = item == null ? Optional.empty() : item;
        block = block == null ? Optional.empty() : block;
        entity = entity == null ? Optional.empty() : entity;
        poi = poi == null ? Optional.empty() : poi;
        region = region == null ? Optional.empty() : region;
        faction = faction == null ? Optional.empty() : faction;
        amount = amount == null ? Optional.empty() : amount;
        state = state == null ? Optional.empty() : state;
        weather = weather == null ? Optional.empty() : weather;
        dimension = dimension == null ? Optional.empty() : dimension;
        biome = biome == null ? Optional.empty() : biome;
        count = count == null ? Optional.empty() : count;
        value = value == null ? Optional.empty() : value;
        metric = metric == null ? Optional.empty() : metric;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        rawJson = rawJson == null ? new JsonObject() : rawJson.deepCopy();
    }

    public boolean not() {
        return negated;
    }
}
