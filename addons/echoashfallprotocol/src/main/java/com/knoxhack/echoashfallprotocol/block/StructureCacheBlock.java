package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.StructureCacheBlockEntity;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class StructureCacheBlock extends Block implements EntityBlock {
    public StructureCacheBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureCacheBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            useStructureCache(level, pos, serverPlayer, "structure_cache_block", true);
        } else if (level.getBlockEntity(pos) instanceof StructureCacheBlockEntity cache) {
            player.openMenu(cache);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static NativeResult useStructureCache(
            Level level,
            BlockPos pos,
            ServerPlayer serverPlayer,
            String source,
            boolean publishMachineUse) {
        if (!(level.getBlockEntity(pos) instanceof StructureCacheBlockEntity cache)) {
            return NativeResult.noop("No live structure cache block entity was available.", java.util.Map.of(
                    "cacheOpened", false,
                    "position", pos.toShortString()));
        }
        if (publishMachineUse) {
            AshfallAdapterCoreMachineRuntimeHost.dispatchUseBlock(
                    serverPlayer, level, pos, "echoashfallprotocol:recovery_cache", false);
        }
        NativeResult cacheOpened = AshfallAdapterCoreExplorationRuntime.cacheOpened(
                serverPlayer,
                pos,
                source == null || source.isBlank() ? "structure_cache_block" : source);
        serverPlayer.openMenu(cache);
        if (cacheOpened.mutated()) {
            return cacheOpened;
        }
        return NativeResult.mutated("Opened live structure cache menu.", java.util.Map.of(
                "cacheOpened", true,
                "menuOpened", true,
                "source", source == null ? "" : source,
                "cacheEventStatus", cacheOpened.status()));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof StructureCacheBlockEntity cache) {
            Containers.dropContents(level, pos, cache);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
