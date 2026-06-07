package com.knoxhack.echolens.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public record LensContext(
        Player player,
        Level level,
        LensTargetKind targetKind,
        BlockPos blockPos,
        BlockState blockState,
        FluidState fluidState,
        Entity entity,
        LensScanMode scanMode,
        LensAccessPolicy accessPolicy) {
    public LensContext {
        scanMode = scanMode == null ? LensScanMode.COMPACT : scanMode;
        accessPolicy = accessPolicy == null ? LensAccessPolicy.PUBLIC_ONLY : accessPolicy;
        if (targetKind == null) {
            if (entity != null) {
                targetKind = LensTargetKind.ENTITY;
            } else if (blockState != null) {
                targetKind = LensTargetKind.BLOCK;
            } else if (fluidState != null && !fluidState.isEmpty()) {
                targetKind = LensTargetKind.FLUID;
            } else {
                targetKind = LensTargetKind.MISS;
            }
        }
    }

    public static LensContext block(Player player, Level level, BlockPos pos, BlockState state,
            FluidState fluidState, LensScanMode mode, LensAccessPolicy accessPolicy) {
        LensTargetKind kind = fluidState != null && !fluidState.isEmpty() && (state == null || state.isAir())
                ? LensTargetKind.FLUID
                : LensTargetKind.BLOCK;
        return new LensContext(player, level, kind, pos, state, fluidState, null, mode, accessPolicy);
    }

    public static LensContext entity(Player player, Level level, Entity entity, LensScanMode mode,
            LensAccessPolicy accessPolicy) {
        return new LensContext(player, level, LensTargetKind.ENTITY, entity == null ? null : entity.blockPosition(),
                null, null, entity, mode, accessPolicy);
    }

    public boolean hasBlock() {
        return blockState != null && blockPos != null;
    }

    public boolean hasEntity() {
        return entity != null;
    }

    public boolean hasFluid() {
        return fluidState != null && !fluidState.isEmpty();
    }
}
