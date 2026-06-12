package com.echoplatform.echocore.api.mission;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public interface IObjectiveView {
    Identifier id();

    default MissionObjectiveType type() {
        return MissionObjectiveType.CUSTOM;
    }

    default String label() {
        return "";
    }

    default String detail() {
        return "";
    }

    default ItemStack icon() {
        return ItemStack.EMPTY;
    }

    default int progress() {
        return 0;
    }

    default int required() {
        return 1;
    }

    default boolean complete() {
        return false;
    }

    default boolean hidden() {
        return false;
    }

    default Map<String, String> criteria() {
        return Map.of();
    }
}
