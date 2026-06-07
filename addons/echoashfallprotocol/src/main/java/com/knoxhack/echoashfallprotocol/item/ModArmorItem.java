package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Thin Item wrapper that adds in-game tooltips showing defense and toughness values.
 */
public class ModArmorItem extends Item {

    private final double defense;
    private final double toughness;
    private final double knockbackResistance;
    private final String flavourLine;

    public ModArmorItem(Properties properties, double defense, double toughness, double knockbackResistance, String flavourLine) {
        super(properties);
        this.defense = defense;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.flavourLine = flavourLine;
    }
}
