package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.gameplay.AshfallInteractionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glowing Nexus crack used as a visual hazard in newly generated Nexus Scars.
 */
public class EnergizedFissureBlock extends Block {
    public EnergizedFissureBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity && level.getGameTime() % 20 == 0) {
            AshfallInteractionRules.hurtServerSide(livingEntity, livingEntity.damageSources().magic(), 1.0F);
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(
                    ParticleTypes.PORTAL,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.02D,
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 0.05D,
                    0.04D,
                    (random.nextDouble() - 0.5D) * 0.05D);
        }
    }
}
