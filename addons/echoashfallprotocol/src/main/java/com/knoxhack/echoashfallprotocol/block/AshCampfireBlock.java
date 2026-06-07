package com.knoxhack.echoashfallprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AshCampfireBlock extends CampfireBlock {
    public AshCampfireBlock(BlockBehaviour.Properties properties) {
        super(true, 1, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        applyShelterPulse(state, level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        applyShelterPulse(state, level, pos);
        if (state.getValue(LIT)) {
            level.scheduleTick(pos, this, 40);
        }
    }

    private void applyShelterPulse(BlockState state, ServerLevel level, BlockPos pos) {
        if (!state.getValue(LIT)) {
            return;
        }
        AABB aura = new AABB(pos).inflate(5.0D);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, aura)) {
            if (monster.getTarget() != null && monster.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 25.0D) {
                monster.setTarget(null);
            }
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state.getValue(LIT)) {
            level.scheduleTick(pos, this, 40);
        }
    }
}
