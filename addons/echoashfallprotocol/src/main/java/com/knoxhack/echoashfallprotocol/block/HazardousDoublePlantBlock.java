package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.gameplay.AshfallInteractionRules;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * HazardousDoublePlantBlock - A 2-block tall plant that damages and poisons entities.
 */
public class HazardousDoublePlantBlock extends DoublePlantBlock {
    public static final MapCodec<HazardousDoublePlantBlock> CODEC = simpleCodec(HazardousDoublePlantBlock::new);

    public HazardousDoublePlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            // Apply thorn damage and brief poison
            AshfallInteractionRules.hurtServerSide(livingEntity, level.damageSources().cactus(), 1.0F);
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
        }
        super.stepOn(level, pos, state, entity);
    }
}
