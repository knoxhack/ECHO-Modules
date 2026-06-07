package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Portable FE storage for moving power between machines.
 */
public class BatteryItem extends Item {
    private final Tier tier;

    public BatteryItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    public int getCapacity() {
        return tier.capacity;
    }

    public int getMaxReceive() {
        return tier.transferRate;
    }

    public int getMaxExtract() {
        return tier.transferRate;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getStoredEnergy(stack) / getCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float charge = (float) getStoredEnergy(stack) / getCapacity();
        return Mth.hsvToRgb(0.50F + charge * 0.10F, 0.85F, 1.0F);
    }

    public static int getStoredEnergy(ItemStack stack) {
        if (!(stack.getItem() instanceof BatteryItem battery)) {
            return 0;
        }
        int stored = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
        return Mth.clamp(stored, 0, battery.getCapacity());
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        if (!(stack.getItem() instanceof BatteryItem battery)) {
            return;
        }
        stack.set(ModDataComponents.STORED_ENERGY.get(), Mth.clamp(energy, 0, battery.getCapacity()));
    }

    public static ItemStack withEnergy(Item item, int energy) {
        ItemStack stack = new ItemStack(item);
        setStoredEnergy(stack, energy);
        return stack;
    }

    public enum Tier {
        BASIC(2_000, 64),
        ADVANCED(10_000, 256),
        ELITE(50_000, 1_024);

        private final int capacity;
        private final int transferRate;

        Tier(int capacity, int transferRate) {
            this.capacity = capacity;
            this.transferRate = transferRate;
        }
    }
}
