package com.knoxhack.echoashfallprotocol.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MutatedLeavesBlock - Concrete implementation of leaves for mutated vegetation.
 */
public class MutatedLeavesBlock extends LeavesBlock {
    public static final MapCodec<MutatedLeavesBlock> CODEC = simpleCodec(MutatedLeavesBlock::new);

    public MutatedLeavesBlock(Properties properties) {
        super(0.01f, properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    public void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > 0.12F) {
            return;
        }
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() - 0.05D;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.MYCELIUM, x, y, z, 0.0D, -0.015D, 0.0D);
    }
}
