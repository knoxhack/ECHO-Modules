package com.knoxhack.echo.scriptcore.model;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoReward(
        String type,
        Optional<Identifier> item,
        int count,
        int experience,
        Optional<Identifier> mission,
        Optional<Identifier> tab,
        Optional<Identifier> entry,
        Optional<Identifier> layer,
        Optional<Identifier> marker,
        Optional<Identifier> faction,
        int amount,
        Optional<Identifier> state,
        Optional<Identifier> sound,
        Map<String, Object> metadata) {
    public EchoReward {
        type = type == null || type.isBlank() ? "custom" : type.trim().toLowerCase(java.util.Locale.ROOT);
        item = item == null ? Optional.empty() : item;
        mission = mission == null ? Optional.empty() : mission;
        tab = tab == null ? Optional.empty() : tab;
        entry = entry == null ? Optional.empty() : entry;
        layer = layer == null ? Optional.empty() : layer;
        marker = marker == null ? Optional.empty() : marker;
        faction = faction == null ? Optional.empty() : faction;
        state = state == null ? Optional.empty() : state;
        sound = sound == null ? Optional.empty() : sound;
        count = Math.max(0, count);
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
