package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echo.adaptercore.EchoEnergyHandler;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BatteryTransferHandler {
    private static final int DIRECT_TRANSFER = 512;

    private BatteryTransferHandler() {
    }

    public static void onRightClickBlock(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack held = itemStackValue(event, "getItemStack");
        if (!EnergyAccess.isEnergyItem(held)) {
            return;
        }

        Level level = player.level();
        if (!(eventValue(event, "getPos") instanceof BlockPos pos)) {
            return;
        }
        EchoEnergyHandler blockEnergy = EnergyAccess.getBlockEnergy(level, pos, directionValue(event, "getFace"));
        EchoEnergyHandler itemEnergy = EnergyAccess.getItemEnergy(held);
        if (blockEnergy == null || itemEnergy == null) {
            return;
        }

        int moved = 0;
        if (itemEnergy.amount() < itemEnergy.capacity()) {
            int receivable = EnergyAccess.simulateInsert(itemEnergy, DIRECT_TRANSFER);
            int extracted = EnergyAccess.extract(blockEnergy, receivable);
            moved = EnergyAccess.insert(itemEnergy, extracted);
            if (moved < extracted) {
                EnergyAccess.insert(blockEnergy, extracted - moved);
            }
            if (moved > 0) {
                player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.battery.pull", moved));
            }
        }

        if (moved == 0 && itemEnergy.amount() > 0) {
            int receivable = EnergyAccess.simulateInsert(blockEnergy, DIRECT_TRANSFER);
            int extracted = EnergyAccess.extract(itemEnergy, receivable);
            moved = EnergyAccess.insert(blockEnergy, extracted);
            if (moved < extracted) {
                EnergyAccess.insert(itemEnergy, extracted - moved);
            }
            if (moved > 0) {
                player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.battery.push", moved));
            }
        }

        if (moved > 0) {
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.45F, 1.25F);
            setValue(event, "setCancellationResult", InteractionResult.SUCCESS_SERVER, InteractionResult.class);
            setValue(event, "setCanceled", true, boolean.class);
        }
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Direction directionValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof Direction direction ? direction : null;
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

    private static void setValue(Object event, String methodName, Object value, Class<?> parameterType) {
        if (event == null) {
            return;
        }
        try {
            event.getClass().getMethod(methodName, parameterType).invoke(event, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Native event views may model cancellation without these compatibility setters.
        }
    }
}
