package com.knoxhack.signalos.block;

import com.knoxhack.signalos.block.entity.SignalOsServerRackBlockEntity;
import com.knoxhack.signalos.integration.SignalOsMissionHooks;
import com.knoxhack.signalos.network.SignalOsTerminalSync;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class SignalOsServerRackBlock extends Block implements EntityBlock {
    public SignalOsServerRackBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignalOsServerRackBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SignalOsServerRackBlockEntity rack) {
            if (player.isShiftKeyDown()) {
                ItemStack extracted = rack.extractDrive();
                if (!extracted.isEmpty() && !player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                player.sendSystemMessage(Component.literal(rack.statusLine()));
                return InteractionResult.SUCCESS_SERVER;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(rack, pos);
                SignalOsTerminalSync.send(serverPlayer);
                if (rack.driveCount() > 0) {
                    SignalOsMissionHooks.recordRackNetworkOnline(serverPlayer, "open_rack");
                }
                return InteractionResult.SUCCESS_SERVER;
            }
            player.sendSystemMessage(Component.literal(rack.statusLine()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SignalOsServerRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        if (!stack.is(ModBlocks.DATA_DRIVE.get())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (rack.insertDrive(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendSystemMessage(Component.literal(rack.statusLine()));
            SignalOsMissionHooks.recordRackNetworkOnline(player, "insert_drive");
            return InteractionResult.SUCCESS_SERVER;
        }
        player.sendSystemMessage(Component.literal("[SignalOS] Server rack drive bays are full."));
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity,
            ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof SignalOsServerRackBlockEntity rack) {
            Containers.dropContents(level, pos, rack.drives());
            rack.clearContent();
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
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
