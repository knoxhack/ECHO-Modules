package com.knoxhack.echomultiblockcore.block;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MultiblockCrateBlock extends Block implements EntityBlock {
    private final CrateKind kind;

    public MultiblockCrateBlock(CrateKind kind, Properties properties) {
        super(properties);
        this.kind = kind == null ? CrateKind.INPUT : kind;
    }

    public CrateKind kind() {
        return kind;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockCrateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MultiblockCrateBlockEntity crate && !stack.isEmpty()) {
            ItemStack copy = stack.copy();
            int inserted = crate.insertStack(copy);
            if (inserted > 0) {
                stack.shrink(inserted);
                player.sendSystemMessage(Component.translatable("message.echomultiblockcore.crate.inserted", inserted, crate.kind().label()));
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
        if (level.getBlockEntity(pos) instanceof MultiblockCrateBlockEntity crate) {
            if (player.isShiftKeyDown()) {
                ItemStack extracted = crate.extractFirst();
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, extracted);
                    }
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
            player.openMenu(crate);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public enum CrateKind {
        INPUT("Input"),
        OUTPUT("Output");

        private final String label;

        CrateKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
