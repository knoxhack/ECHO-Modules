package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallout Dust Block - soft radioactive powder found in Radiation Zones.
 * Slowly emits radiation to players standing on it.
 */
public class FalloutDustBlock extends Block {

    public FalloutDustBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            // Apply radiation every 2 seconds while standing on fallout dust
            if (level.getGameTime() % 40 == 0) {
                RadiationHelper.addEnvironmentalRadiation(player, 2.0f);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
