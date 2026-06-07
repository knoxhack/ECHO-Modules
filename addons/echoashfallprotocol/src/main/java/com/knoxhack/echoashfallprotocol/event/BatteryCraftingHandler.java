package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class BatteryCraftingHandler {
    private BatteryCraftingHandler() {
    }

    public static void onItemCrafted(Object event) {
        ItemStack crafted = itemStackValue(event, "getCrafting");
        Object inventory = eventValue(event, "getInventory");
        if (!(inventory instanceof Container container)) {
            return;
        }
        if (crafted.is(ModItems.ADVANCED_BATTERY.get())) {
            preserveInputBatteryEnergy(crafted, container, ModItems.BASIC_BATTERY.get().getDefaultInstance());
        } else if (crafted.is(ModItems.ELITE_BATTERY.get())) {
            preserveInputBatteryEnergy(crafted, container, ModItems.ADVANCED_BATTERY.get().getDefaultInstance());
        }
    }

    private static void preserveInputBatteryEnergy(ItemStack crafted, Container inventory, ItemStack sourceBattery) {
        int stored = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(sourceBattery.getItem())) {
                stored += BatteryItem.getStoredEnergy(stack);
            }
        }
        if (stored > 0) {
            BatteryItem.setStoredEnergy(crafted, stored);
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
}
