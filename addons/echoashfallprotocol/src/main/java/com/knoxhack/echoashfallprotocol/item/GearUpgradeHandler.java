package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.world.item.ItemStack;

/**
 * Compatibility facade for older Ashfall item code/tests.
 *
 * <p>The subscribed upgrade events live in
 * {@link com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler}.</p>
 */
public final class GearUpgradeHandler {
    private GearUpgradeHandler() {
    }

    public static boolean isUpgradeable(ItemStack stack) {
        return com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler.isUpgradeable(stack);
    }

    public static int getUpgradeLevel(ItemStack stack) {
        return com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler.getUpgradeLevel(stack);
    }

    public static float getDamageBonus(ItemStack weapon) {
        return com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler.getBonusDamage(weapon);
    }

    public static boolean hasGlowEffect(ItemStack stack) {
        return com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler.hasGlowEffect(stack);
    }
}
