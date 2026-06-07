package com.knoxhack.echoaetherworks.block;

import com.knoxhack.echoaetherworks.api.AetherWorksApi;
import com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity;
import com.knoxhack.echoaetherworks.menu.AetherMachineMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AetherMachineBlock extends BaseEntityBlock {
    private final net.minecraft.resources.Identifier machineId;

    protected AetherMachineBlock(Properties props, net.minecraft.resources.Identifier machineId) {
        super(props);
        this.machineId = machineId;
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (player instanceof ServerPlayer serverPlayer && blockEntity instanceof AetherStorageBlockEntity storage) {
            if (player.isShiftKeyDown()) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.diagnostic",
                        Math.round(storage.storedAmount()), Math.round(storage.capacity()),
                        storage.aetherStorage().outputType().serializedName()));
                AetherWorksApi.record(serverPlayer, machineId, "diagnostic");
            } else {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, operator) -> new AetherMachineMenu(id, inventory, level, pos),
                        Component.translatable("screen.echoaetherworks.aether_machine")), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof AetherStorageBlockEntity storage) {
            Containers.dropContents(level, pos, storage);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof AetherStorageBlockEntity storage ? storage.comparatorSignal() : 0;
    }
}
