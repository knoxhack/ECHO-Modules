package com.knoxhack.echo.equipmentcore.api;

import net.minecraft.world.item.ItemStack;

/**
 * Implemented by items that provide equipment stats for ECHO hazard integration.
 */
public interface IEquipmentProvider {
    EquipmentStats getStats(ItemStack stack);
}
