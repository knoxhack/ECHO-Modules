package com.knoxhack.echoaetherworks.block.entity;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoaetherworks.registry.ModBlockEntities;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AetherCondenserBlockEntity extends AetherStorageBlockEntity {
    public AetherCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AETHER_CONDENSER.get(), pos, state, 160.0D, 8.0D,
                AetherSignalType.RAW_AETHER, Set.of(AetherSignalType.RAW_AETHER));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AetherCondenserBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        entity.tickAutomationState();
        if (level.getGameTime() % 20L == 0L) {
            entity.generate(4.0D, AetherSignalType.RAW_AETHER);
            if (level instanceof ServerLevel serverLevel) {
                entity.spark(serverLevel, pos, ParticleTypes.ENCHANT);
            }
        }
        entity.pushToNeighbors(level, pos);
    }
}
