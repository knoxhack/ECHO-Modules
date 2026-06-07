package com.knoxhack.echoritualcore.block;

import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class OfferingPedestalBlock extends Block implements EntityBlock {
    public OfferingPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OfferingPedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof OfferingPedestalBlockEntity pedestal && !pedestal.isEmpty()) {
            ItemStack stack = pedestal.removeItem(OfferingPedestalBlockEntity.SLOT, pedestal.getItem(OfferingPedestalBlockEntity.SLOT).getCount());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.translatable("block.echoritualcore.offering_pedestal.take"));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.translatable("block.echoritualcore.offering_pedestal.empty"));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack == null || stack.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof OfferingPedestalBlockEntity pedestal) {
            if (!pedestal.isEmpty()) {
                player.sendSystemMessage(Component.translatable("block.echoritualcore.offering_pedestal.occupied"));
                return InteractionResult.SUCCESS;
            }
            ItemStack placed = stack.copyWithCount(1);
            pedestal.setItem(OfferingPedestalBlockEntity.SLOT, placed);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendSystemMessage(Component.translatable("block.echoritualcore.offering_pedestal.insert"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof OfferingPedestalBlockEntity pedestal) {
            Containers.dropContents(level, pos, pedestal);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
