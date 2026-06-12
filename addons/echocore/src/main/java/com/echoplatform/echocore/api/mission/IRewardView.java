package com.echoplatform.echocore.api.mission;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public interface IRewardView {
    Identifier id();

    default MissionRewardClaimMode claimMode() {
        return MissionRewardClaimMode.IMMEDIATE;
    }

    default ItemStack stack() {
        return ItemStack.EMPTY;
    }

    default String label() {
        return "";
    }

    default String detail() {
        return "";
    }

    default boolean claimable() {
        return false;
    }

    default boolean claimed() {
        return false;
    }

    default Map<String, String> metadata() {
        return Map.of();
    }
}
