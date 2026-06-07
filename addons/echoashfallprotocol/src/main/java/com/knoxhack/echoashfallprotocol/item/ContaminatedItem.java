package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Contaminated resource item — poisons the player while held in inventory.
 * Effect is applied via ContaminatedItemTickHandler event.
 * Must be purified in the Water Purifier or Atmospheric Scrubber.
 */
public class ContaminatedItem extends Item {

    public ContaminatedItem(Properties properties) {
        super(properties);
    }
}
