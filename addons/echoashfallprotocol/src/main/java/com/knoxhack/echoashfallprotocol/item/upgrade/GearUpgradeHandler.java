package com.knoxhack.echoashfallprotocol.item.upgrade;

import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Handles Tier 5 gear upgrades via anvil.
 * Nexus weapons can be upgraded with Nexus Crystals: +1 damage per crystal, max +5.
 */
public class GearUpgradeHandler {

    private static final String NBT_UPGRADE_KEY = "nexus_upgrades";
    private static final int MAX_UPGRADES = 5;

    /**
     * Check if an item is a Nexus upgradable weapon
     */
    public static boolean isNexusWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ModItems.NEXUS_BLADE.get()) ||
               stack.is(ModItems.NEXUS_ANNIHILATOR.get()) ||
               stack.is(ModItems.ALLOY_BLADE.get());
    }

    public static boolean isUpgradeable(ItemStack stack) {
        return isNexusWeapon(stack);
    }

    /**
     * Get current upgrade level from item NBT
     */
    public static int getUpgradeLevel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            // Use getIntOr with default value
            return tag.getIntOr(NBT_UPGRADE_KEY, 0);
        }
        return 0;
    }

    /**
     * Set upgrade level on item NBT
     */
    public static void setUpgradeLevel(ItemStack stack, int level) {
        int cappedLevel = Math.min(MAX_UPGRADES, Math.max(0, level));
        
        CompoundTag tag = new CompoundTag();
        CustomData existingData = stack.get(DataComponents.CUSTOM_DATA);
        if (existingData != null) {
            tag = existingData.copyTag();
        }
        
        tag.putInt(NBT_UPGRADE_KEY, cappedLevel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        // Add lore indicating upgrade level
        updateUpgradeLore(stack, cappedLevel);
    }

    /**
     * Update item lore to show upgrade level
     */
    private static void updateUpgradeLore(ItemStack stack, int level) {
        if (level <= 0) return;
        
        // The lore is handled via tooltip events in the item classes
        // This is just a marker that the upgrade exists
    }

    /**
     * Calculate bonus damage from upgrades
     */
    public static float getBonusDamage(ItemStack stack) {
        return getUpgradeLevel(stack) * 1.0f; // +1 damage per level
    }

    public static boolean hasGlowEffect(ItemStack stack) {
        return getUpgradeLevel(stack) >= 3;
    }

    /**
     * Handle direct field upgrades. Anvil upgrades stay supported below; this path
     * keeps the original right-click interaction while using the canonical NBT
     * storage and single subscribed handler.
     */
    public static void onRightClickItem(Object event) {
        if (!(eventValue(event, "getEntity") instanceof Player player)) {
            return;
        }
        ItemStack heldItem = itemStackValue(event, "getItemStack");
        if (!heldItem.is(ModItems.NEXUS_CRYSTAL.get()) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack target = player.getOffhandItem();
        if (!isUpgradeable(target)) {
            target = heldItem;
        }
        if (!isUpgradeable(target)) {
            return;
        }

        attemptDirectUpgrade(serverPlayer, target);
        setBooleanValue(event, "setCanceled", true);
    }

    private static void attemptDirectUpgrade(ServerPlayer player, ItemStack item) {
        int currentLevel = getUpgradeLevel(item);
        if (currentLevel >= MAX_UPGRADES) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7cThis item is already at maximum upgrade level (+" + MAX_UPGRADES + ")"));
            return;
        }

        ItemStack crystalStack = findCrystalInInventory(player);
        if (crystalStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7cYou need a Nexus Crystal to upgrade this item"));
            return;
        }

        crystalStack.shrink(1);
        int newLevel = currentLevel + 1;
        setUpgradeLevel(item, newLevel);
        player.sendSystemMessage(Component.literal("\u00a7aItem upgraded to +" + newLevel + "! Max +" + MAX_UPGRADES));
        if (newLevel == 3) {
            player.sendSystemMessage(Component.literal("\u00a7dYour weapon begins to glow with ethereal runes..."));
        } else if (newLevel == 5) {
            player.sendSystemMessage(Component.literal(
                    "\u00a76Your weapon reaches maximum power! A particle trail follows your strikes."));
        }
    }

    private static ItemStack findCrystalInInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.NEXUS_CRYSTAL.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Handle anvil updates for gear upgrades
     */
    public static void onAnvilUpdate(Object event) {
        ItemStack left = itemStackValue(event, "getLeft");
        ItemStack right = itemStackValue(event, "getRight");

        // Check if this is a Nexus weapon upgrade
        if (isNexusWeapon(left) && right.is(ModItems.NEXUS_CRYSTAL.get())) {
            int currentLevel = getUpgradeLevel(left);
            
            if (currentLevel >= MAX_UPGRADES) {
                // Maxed out - no upgrade possible
                setBooleanValue(event, "setCanceled", true);
                return;
            }

            // Create upgraded output
            ItemStack output = left.copy();
            setUpgradeLevel(output, currentLevel + 1);

            setItemStackValue(event, "setOutput", output);
            // Cost is handled by setOutput, material cost by consuming right item
        }
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void setBooleanValue(Object event, String methodName, boolean value) {
        if (event == null) {
            return;
        }
        try {
            event.getClass().getMethod(methodName, boolean.class).invoke(event, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Native event views may expose mutation through a different host path.
        }
    }

    private static void setItemStackValue(Object event, String methodName, ItemStack value) {
        if (event == null) {
            return;
        }
        try {
            event.getClass().getMethod(methodName, ItemStack.class).invoke(event, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Native event views may expose mutation through a different host path.
        }
    }
}
