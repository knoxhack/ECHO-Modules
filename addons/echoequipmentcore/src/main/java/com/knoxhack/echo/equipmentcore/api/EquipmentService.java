package com.knoxhack.echo.equipmentcore.api;

import com.echoplatform.echocore.api.EchoServiceRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Central equipment runtime. Packs register slots and query equipped stats.
 */
public final class EquipmentService {
    private static EquipmentService instance;

    private final Map<Identifier, EquipmentSlot> slots = new ConcurrentHashMap<>();
    private final Map<Identifier, Item> defaultItems = new ConcurrentHashMap<>();

    private EquipmentService() {
        registerBuiltinSlots();
    }

    public static synchronized EquipmentService getInstance() {
        if (instance == null) {
            instance = new EquipmentService();
            EchoServiceRegistry.register(EquipmentService.class, instance);
        }
        return instance;
    }

    public static EquipmentService find() {
        return EchoServiceRegistry.find(EquipmentService.class).orElseGet(EquipmentService::getInstance);
    }

    private void registerBuiltinSlots() {
        registerSlot(EquipmentSlot.SUIT_FRAME);
        registerSlot(EquipmentSlot.REBREATHER);
        registerSlot(EquipmentSlot.LIGHT_SENSOR);
        registerSlot(EquipmentSlot.TOOL_MOUNT);
    }

    public void registerSlot(EquipmentSlot slot) {
        slots.put(slot.id(), slot);
    }

    public EquipmentSlot getSlot(Identifier id) {
        return slots.get(id);
    }

    public List<EquipmentSlot> getSlots() {
        return List.copyOf(slots.values());
    }

    public void registerDefaultItem(Identifier slotId, Item item) {
        defaultItems.put(slotId, item);
    }

    public Item getDefaultItem(Identifier slotId) {
        return defaultItems.get(slotId);
    }

    public EquipmentStats getEquippedStats(ServerPlayer player, Identifier slotId) {
        EquipmentSlot slot = getSlot(slotId);
        if (slot == null) {
            return EquipmentStats.ZERO;
        }
        ItemStack stack = stackForSlot(player, slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IEquipmentProvider provider)) {
            return EquipmentStats.ZERO;
        }
        return provider.getStats(stack);
    }

    public EquipmentStats getTotalStats(ServerPlayer player) {
        EquipmentStats total = EquipmentStats.ZERO;
        for (EquipmentSlot slot : getSlots()) {
            total = total.add(getEquippedStats(player, slot.id()));
        }
        return total;
    }

    private static ItemStack stackForSlot(ServerPlayer player, EquipmentSlot slot) {
        Identifier id = slot.id();
        if (EquipmentSlot.SUIT_FRAME.equals(slot)) {
            return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        }
        if (EquipmentSlot.REBREATHER.equals(slot)) {
            return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        }
        if (EquipmentSlot.LIGHT_SENSOR.equals(slot)) {
            return player.getOffhandItem();
        }
        if (EquipmentSlot.TOOL_MOUNT.equals(slot)) {
            return player.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }
}
