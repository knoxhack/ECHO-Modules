package com.knoxhack.echoashfallprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Irradiated cactus that glows and deals radiation damage to entities.
 */
public class IrradiatedCactusBlock extends Block {
    
    public IrradiatedCactusBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            // Apply poison effect (radiation damage)
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
        }
        super.stepOn(level, pos, state, entity);
    }
}
