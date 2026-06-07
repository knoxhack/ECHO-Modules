package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.block.entity.ScrapDynamoBlockEntity;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ScrapDynamoFuelHandler {
    private ScrapDynamoFuelHandler() {
    }

    public static void onRightClickBlock(Object event) {
        if (interactionHandValue(event, "getHand") != InteractionHand.MAIN_HAND
                || !(eventValue(event, "getEntity") instanceof ServerPlayer player)
                || !(eventValue(event, "getPos") instanceof BlockPos pos)) {
            return;
        }

        Level level = player.level();
        if (!level.getBlockState(pos).is(ModBlocks.SCRAP_DYNAMO.get())
                || !(level.getBlockEntity(pos) instanceof ScrapDynamoBlockEntity dynamo)) {
            return;
        }

        ItemStack held = itemStackValue(event, "getItemStack");
        if (held.isEmpty() || !dynamo.isFuel(held)) {
            return;
        }

        dynamo.addFuel(held);
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.scrap_dynamo.fueled"));
        level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.15F);
        setValue(event, "setCancellationResult", InteractionResult.SUCCESS_SERVER, InteractionResult.class);
        setValue(event, "setCanceled", true, boolean.class);
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static InteractionHand interactionHandValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof InteractionHand hand ? hand : null;
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
