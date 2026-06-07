package com.knoxhack.echoashfallprotocol.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * Profession block for villager job sites.
 * Villagers will claim these blocks to gain professions.
 */
public class ProfessionBlock extends Block {

    public ProfessionBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
