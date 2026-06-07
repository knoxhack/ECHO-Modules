package com.knoxhack.echoashfallprotocol.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared interaction helpers for server damage and block-surface checks.
 */
public final class AshfallInteractionRules {
    private AshfallInteractionRules() {
    }

    public static boolean hurtServerSide(Entity entity, DamageSource source, float amount) {
        return entity.level() instanceof ServerLevel serverLevel
                && entity.hurtServer(serverLevel, source, amount);
    }

    public static boolean supportsPlacement(BlockState state) {
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static boolean supportsPlacement(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    public static boolean hasFluid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }
}
