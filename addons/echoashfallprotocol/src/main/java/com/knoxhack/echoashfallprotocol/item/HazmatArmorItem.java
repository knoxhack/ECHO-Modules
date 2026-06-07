package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Hazmat Suit armor pieces provide radiation protection.
 * Full set bonus: 100% radiation immunity + Gas Mask functionality.
 * Individual pieces: 25% radiation reduction each (75% max without full set).
 * 
 * Uses the Equippable component for slot assignment.
 */
public class HazmatArmorItem extends Item {

    private final EquipmentSlot slot;
    private final float radiationReduction;
    private final String tooltipKey;

    public HazmatArmorItem(Properties properties, EquipmentSlot slot) {
        super(properties);
        this.slot = slot;
        // Each piece provides 25% radiation reduction
        this.radiationReduction = 0.25f;
        this.tooltipKey = "tooltip.EchoAshfallProtocol.hazmat_" + slot.getSerializedName().toLowerCase();
    }

    /**
     * Get the equipment slot for this armor piece
     */
    public EquipmentSlot getEquipmentSlot() {
        return slot;
    }

    /**
     * Get the radiation reduction percentage for this armor piece (0.0-1.0)
     */
    public float getRadiationReduction() {
        return radiationReduction;
    }

    /**
     * Check if this is a helmet (provides gas mask functionality when full set worn)
     */
    public boolean isHelmet() {
        return this.slot == EquipmentSlot.HEAD;
    }

    /**
     * Calculate total radiation resistance for a player wearing this armor
     * This is called by the radiation system
     */
    public static float getTotalRadiationResistance(Iterable<ItemStack> armorItems) {
        float totalReduction = 0.0f;
        int hazmatPieces = 0;

        for (ItemStack stack : armorItems) {
            Item item = stack.getItem();
            if (item instanceof HazmatArmorItem hazmatItem) {
                totalReduction += hazmatItem.getRadiationReduction();
                hazmatPieces++;
            }
        }

        // Full set bonus: complete immunity
        if (hazmatPieces >= 4) {
            return 1.0f; // 100% radiation immunity
        }

        // Partial protection from individual pieces (capped at 75%)
        return Math.min(totalReduction, 0.75f);
    }

    /**
     * Check if player is wearing full hazmat suit
     */
    public static boolean hasFullSet(Iterable<ItemStack> armorItems) {
        int hazmatPieces = 0;
        for (ItemStack stack : armorItems) {
            Item item = stack.getItem();
            if (item instanceof HazmatArmorItem) {
                hazmatPieces++;
            }
        }
        return hazmatPieces >= 4;
    }

    /**
     * Check if player has hazmat helmet equipped (for gas mask functionality)
     */
    public static boolean hasHazmatHelmet(net.minecraft.world.entity.player.Player player) {
        ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);
        Item item = headSlot.getItem();
        return item instanceof HazmatArmorItem hazmat && hazmat.isHelmet();
    }
}
