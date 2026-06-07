package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.gameplay.AshfallInteractionRules;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * HazardousBushBlock - A plant that damages and poisons entities that step on it.
 */
public class HazardousBushBlock extends BushBlock {
    public static final MapCodec<HazardousBushBlock> CODEC = simpleCodec(HazardousBushBlock::new);

    public HazardousBushBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MapCodec<BushBlock> codec() {
        return (MapCodec) CODEC;
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
