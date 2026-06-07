package com.knoxhack.echoaetherworks.block.entity;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoaetherworks.registry.ModBlockEntities;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AetherConduitBlockEntity extends AetherStorageBlockEntity {
    public AetherConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AETHER_CONDUIT.get(), pos, state, 96.0D, 24.0D,
                AetherSignalType.RAW_AETHER, Set.of(AetherSignalType.values()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AetherConduitBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        entity.tickAutomationState();
        entity.pushToNeighbors(level, pos);
        if (entity.storedAmount() > 0.0D && level.getGameTime() % 10L == 0L && level instanceof ServerLevel serverLevel) {
            entity.spark(serverLevel, pos, ParticleTypes.END_ROD);
        }
    }
}
