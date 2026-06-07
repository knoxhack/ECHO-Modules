package com.knoxhack.echoriftworlds.block;

import com.knoxhack.echoriftworlds.api.RiftWorldsApi;
import com.knoxhack.echoriftworlds.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RiftCrackBlock extends Block {
    public RiftCrackBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RiftWorldsApi.scanRiftCrack(serverPlayer, pos);
            if (player.isShiftKeyDown()) {
                BlockPos pocket = pos.above();
                if (level.isEmptyBlock(pocket)) {
                    level.setBlockAndUpdate(pocket, ModBlocks.POCKET_RIFT.get().defaultBlockState());
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
