package com.knoxhack.echo.adaptercore;

import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface EchoBlockEntityCapabilityProvider {
    Object get(BlockEntity blockEntity, Object side);
}
