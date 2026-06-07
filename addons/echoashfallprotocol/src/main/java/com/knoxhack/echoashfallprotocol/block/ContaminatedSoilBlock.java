package com.knoxhack.echoashfallprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ContaminatedSoilBlock - soil that emits radiation particles.
 * Found in Radiation Zone. Visibly contaminated ground.
 */
public class ContaminatedSoilBlock extends Block {

    public ContaminatedSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Emit radiation particles occasionally
        if (random.nextInt(15) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.1;
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.05, 0.0);
        }
    }
}
