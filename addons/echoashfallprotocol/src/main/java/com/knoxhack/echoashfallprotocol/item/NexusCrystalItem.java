package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Nexus Crystal — rare endgame crafting material.
 * Obtained by resolving the Nexus Core (restore, destroy, or control).
 * Required for Tier 3 weapons and armor.
 */
public class NexusCrystalItem extends Item {

    public NexusCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
