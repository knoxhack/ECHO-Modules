package com.knoxhack.echomultiblockcore.block;

import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import com.knoxhack.echomultiblockcore.item.ToolHeadItem;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class RoboticArmBlock extends Block implements EntityBlock {
    public RoboticArmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoboticArmBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.ROBOTIC_ARM.get()
                ? (tickLevel, pos, blockState, blockEntity) -> RoboticArmBlockEntity.tick(tickLevel, pos, blockState, (RoboticArmBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RoboticArmBlockEntity arm && stack.getItem() instanceof ToolHeadItem) {
            if (arm.installTool(stack, player)) {
                stack.shrink(1);
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RoboticArmBlockEntity arm) {
            if (player.isShiftKeyDown()) {
                ItemStack removed = arm.removeTool(player);
                if (!removed.isEmpty() && !player.getInventory().add(removed)) {
                    Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, removed);
                }
            } else {
                player.sendSystemMessage(arm.statusComponent());
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof RoboticArmBlockEntity arm && !arm.installedTool().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, arm.removeTool(null));
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
