package com.knoxhack.echorelictech.block;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.block.entity.ContainmentLockerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ContainmentLockerBlock extends Block implements EntityBlock {
    public ContainmentLockerBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ContainmentLockerBlockEntity be) {
            if (!stack.isEmpty() && RelicTechApi.isRelic(stack)) {
                if (be.addRelic(stack, player)) {
                    player.sendSystemMessage(Component.translatable("block.echorelictech.containment_locker.stored"));
                } else {
                    player.sendSystemMessage(Component.translatable("block.echorelictech.containment_locker.full"));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ContainmentLockerBlockEntity be) {
            if (be.isEmpty()) {
                player.sendSystemMessage(Component.translatable("block.echorelictech.containment_locker.empty"));
                return InteractionResult.SUCCESS;
            }
            // Retrieve first contained relic
            for (int i = 0; i < be.getContainerSize(); i++) {
                ItemStack removed = be.removeRelic(i);
                if (!removed.isEmpty()) {
                    if (!player.getInventory().add(removed)) {
                        player.drop(removed, false);
                    }
                    player.sendSystemMessage(Component.translatable("block.echorelictech.containment_locker.retrieved"));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        player.sendSystemMessage(Component.translatable("block.echorelictech.containment_locker.empty"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContainmentLockerBlockEntity(pos, state);
    }
}
