package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.block.entity.RainCollectorBlockEntity;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Early-game contaminated water collection.
 */
public final class WaterCollectionHandler {
    private WaterCollectionHandler() {
    }

    public static void onRightClickBlock(Object event) {
        if (!(eventValue(event, "getEntity") instanceof Player player)) {
            return;
        }

        ItemStack held = itemStackValue(event, "getItemStack");
        if (!held.is(Items.GLASS_BOTTLE)) {
            return;
        }

        Level level = player.level();
        if (!(eventValue(event, "getPos") instanceof BlockPos pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.RAIN_COLLECTOR.get())) {
            if (player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof RainCollectorBlockEntity collector) {
                collector.fillBottle(level, serverPlayer, interactionHandValue(event, "getHand"));
            }
            cancelInteraction(event, level);
            return;
        }

        Direction face = eventValue(event, "getFace") instanceof Direction direction ? direction : Direction.UP;
        if (!isDirtyWaterSource(level, state, pos, face)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            fillDirtyWater(level, serverPlayer, interactionHandValue(event, "getHand"), pos);
        }
        cancelInteraction(event, level);
    }

    public static void onRightClickItem(Object event) {
        if (!(eventValue(event, "getEntity") instanceof Player player)) {
            return;
        }

        ItemStack held = itemStackValue(event, "getItemStack");
        if (!held.is(Items.GLASS_BOTTLE)) {
            return;
        }

        Level level = player.level();
        BlockHitResult hit = getWaterSourceHit(level, player);
        if (hit == null || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            fillDirtyWater(level, serverPlayer, interactionHandValue(event, "getHand"), hit.getBlockPos());
        }
        cancelInteraction(event, level);
    }

    private static void fillDirtyWater(Level level, ServerPlayer player, InteractionHand hand, BlockPos soundPos) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack dirtyWater = new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get());
        ItemStack result = ItemUtils.createFilledResult(held, player, dirtyWater);
        player.setItemInHand(hand, result);

        AshfallAdapterCoreEarlyEventRuntime.dirtyWaterCollected(player, soundPos);

        level.playSound(null, soundPos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.7F, 0.8F);
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.water.collect_dirty"));
    }

    private static BlockHitResult getWaterSourceHit(Level level, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double reach = player.blockInteractionRange();
        Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean isDirtyWaterSource(Level level, BlockState state, BlockPos pos, Direction face) {
        if (state.is(Blocks.WATER_CAULDRON)) {
            return false;
        }

        if (state.is(ModBlocks.TOXIC_PUDDLE.get()) || state.is(ModBlocks.ACIDIC_SLUDGE.get())) {
            return true;
        }

        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }

        return level.getFluidState(pos.relative(face)).is(FluidTags.WATER);
    }

    private static void cancelInteraction(Object event, Level level) {
        setValue(event, "setCancellationResult",
                level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER,
                InteractionResult.class);
        setValue(event, "setCanceled", true, boolean.class);
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static InteractionHand interactionHandValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof InteractionHand hand ? hand : InteractionHand.MAIN_HAND;
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
            // Native event views may express cancellation through host receipts.
        }
    }
}
