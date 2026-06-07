package com.knoxhack.echoashfallprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * OilStainedConcreteBlock - slippery, hazardous industrial flooring.
 * Slows movement and can catch fire. Found in Industrial Ruins.
 */
public class OilStainedConcreteBlock extends Block {

    public OilStainedConcreteBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            // Oil slows you down
            livingEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
        }
        super.stepOn(level, pos, state, entity);
    }
}
