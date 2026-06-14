package com.knoxhack.echo.equipmentcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.equipmentcore.api.EquipmentSlot;
import com.knoxhack.echo.equipmentcore.api.EquipmentStats;
import com.knoxhack.echo.equipmentcore.api.IEquipmentProvider;
import com.knoxhack.echo.equipmentcore.item.DivingSuitItem;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EquipmentEvents {
    private EquipmentEvents() {
    }

    public static void onLivingDamage(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.livingDamageServerPlayer(event);
        if (player == null) {
            return;
        }
        float amount = EchoBackendWorldEventBridge.livingDamageAmount(event);
        if (amount <= 0.0F) {
            return;
        }
        reduceDurabilityForSlot(player, net.minecraft.world.entity.EquipmentSlot.CHEST);
    }

    public static void onPlayerTick(Object event) {
        Player tickPlayer = EchoBackendWorldEventBridge.postTickPlayer(event);
        if (!(tickPlayer instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        long time = player.level().getGameTime();
        if (time % 20L != 0L) {
            return;
        }
        List<ItemStack> equipment = List.of(
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
        );
        for (ItemStack stack : equipment) {
            if (stack.getItem() instanceof DivingSuitItem) {
                DivingSuitItem.initialize(stack);
            }
        }
    }

    private static void reduceDurabilityForSlot(ServerPlayer player, net.minecraft.world.entity.EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IEquipmentProvider)) {
            return;
        }
        EquipmentStats stats = ((IEquipmentProvider) stack.getItem()).getStats(stack);
        if (stats.durability() <= 0) {
            return;
        }
        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
            if (!stack.isEmpty()) {
                stack.setDamageValue(0);
            }
        } else {
            stack.setDamageValue(nextDamage);
        }
    }
}
