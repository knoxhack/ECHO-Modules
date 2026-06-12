package com.echoplatform.echocore.api.mission;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record ObjectiveDefinition(
        Identifier id,
        MissionObjectiveType type,
        String label,
        String description,
        ItemStack icon,
        int required,
        boolean optional,
        Map<String, String> metadata) implements IObjectiveView {
    public ObjectiveDefinition {
        label = label == null ? "" : label;
        description = description == null ? "" : description;
        icon = icon == null ? ItemStack.EMPTY : icon;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public int targetCount() {
        return required;
    }

    @Override
    public String detail() {
        return description;
    }

    @Override
    public Map<String, String> criteria() {
        return metadata;
    }

    @Override
    public boolean hidden() {
        return optional || Boolean.parseBoolean(metadata.getOrDefault("hidden", "false"));
    }

    public static ObjectiveDefinition simple(Identifier id, MissionObjectiveType type, String label,
            String description, ItemStack icon, int required) {
        return new ObjectiveDefinition(id, type, label, description, icon, required, false, Map.of());
    }
}
