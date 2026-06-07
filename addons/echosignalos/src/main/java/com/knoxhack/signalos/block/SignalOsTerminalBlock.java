package com.knoxhack.signalos.block;

import com.knoxhack.signalos.block.entity.SignalOsTerminalBlockEntity;
import com.knoxhack.signalos.integration.SignalOsMissionHooks;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SignalOsTerminalBlock extends Block implements EntityBlock {
    public SignalOsTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignalOsTerminalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                dispatchAshfallMachineUse(serverPlayer, level, pos, "signalos:terminal");
            }
            if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof SignalOsTerminalBlockEntity terminal) {
                ItemStack extracted = terminal.extractDrive();
                if (!extracted.isEmpty() && !player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                player.sendSystemMessage(Component.literal(extracted.isEmpty()
                        ? "[SignalOS] No boot drive installed."
                        : "[SignalOS] Boot drive ejected."));
                return InteractionResult.SUCCESS_SERVER;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                SignalOsTerminalServices.openBlockTerminal(serverPlayer, level, pos);
                SignalOsMissionHooks.recordBootTerminal(serverPlayer, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SignalOsTerminalBlockEntity terminal)) {
            return InteractionResult.PASS;
        }
        if (!stack.is(com.knoxhack.signalos.registry.ModBlocks.DATA_DRIVE.get())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            dispatchAshfallMachineUse(serverPlayer, level, pos, "signalos:terminal");
        }
        if (terminal.insertDrive(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendSystemMessage(Component.literal("[SignalOS] Boot drive installed."));
            return InteractionResult.SUCCESS_SERVER;
        }
        player.sendSystemMessage(Component.literal("[SignalOS] Boot drive slot is occupied."));
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity,
            ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof SignalOsTerminalBlockEntity terminal) {
            Containers.dropContents(level, pos, terminal.bootDrive());
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    private static void dispatchAshfallMachineUse(ServerPlayer player, Level level, BlockPos pos, String machineId) {
        try {
            Class<?> runtime = Class.forName(
                    "com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost");
            runtime.getMethod("dispatchUseBlock", ServerPlayer.class, Level.class, BlockPos.class, String.class)
                    .invoke(null, player, level, pos, machineId);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // SignalOS stays standalone when Ashfall's AdapterCore machine host is absent.
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            SignalOsComputerNetworkService.invalidateCache();
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        SignalOsComputerNetworkService.invalidateCache();
    }
}
