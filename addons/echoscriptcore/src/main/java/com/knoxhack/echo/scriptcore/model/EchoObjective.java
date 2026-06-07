package com.knoxhack.echo.scriptcore.model;

import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoObjective(
        String id,
        String type,
        String title,
        String description,
        Optional<Identifier> target,
        Optional<Identifier> item,
        Optional<Identifier> block,
        Optional<Identifier> entity,
        Optional<Identifier> poi,
        Optional<Identifier> region,
        int count,
        boolean optional,
        boolean hidden,
        List<EchoCondition> conditions,
        Map<String, Object> metadata) {
    public EchoObjective {
        id = id == null || id.isBlank() ? "objective" : id;
        type = type == null || type.isBlank() ? "custom" : type.trim().toLowerCase(java.util.Locale.ROOT);
        title = title == null ? id : title;
        description = description == null ? "" : description;
        target = target == null ? Optional.empty() : target;
        item = item == null ? Optional.empty() : item;
        block = block == null ? Optional.empty() : block;
        entity = entity == null ? Optional.empty() : entity;
        poi = poi == null ? Optional.empty() : poi;
        region = region == null ? Optional.empty() : region;
        count = Math.max(1, count);
        conditions = List.copyOf(conditions == null ? List.of() : conditions);
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
