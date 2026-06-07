package com.knoxhack.echoaetherworks.block.entity;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoaetherworks.registry.ModBlockEntities;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AetherCellBlockEntity extends AetherStorageBlockEntity {
    public AetherCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AETHER_CELL.get(), pos, state, 480.0D, 18.0D,
                AetherSignalType.RAW_AETHER, Set.of(AetherSignalType.values()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AetherCellBlockEntity entity) {
        if (!level.isClientSide()) {
            entity.tickAutomationState();
        }
    }
}
