package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Toxic Puddle - hazardous environmental block.
 * Applies poison to entities standing in it.
 * Gas mask provides protection.
 */
public class ToxicPuddleBlock extends Block {

    public ToxicPuddleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            boolean hasProtection = entity instanceof Player player
                    && HazardZoneManager.hasRespiratoryProtection(player);

            if (!hasProtection) {
                // Apply poison I for 3 seconds
                livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
