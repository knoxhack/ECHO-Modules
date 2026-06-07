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
 * Acidic Sludge - hazardous liquid block found in Toxic Swamp.
 * Deals poison damage and armor corrosion to entities that contact it.
 * Gas mask provides partial protection.
 */
public class AcidicSludgeBlock extends Block {

    public AcidicSludgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            boolean acidProtected = entity instanceof Player player && HazardZoneManager.hasAcidProtection(player);
            boolean respiratoryProtected = entity instanceof Player player && HazardZoneManager.hasRespiratoryProtection(player);

            if (acidProtected) {
                // Full hazmat seals acid contact.
            } else if (!respiratoryProtected) {
                // Strong poison + armor damage simulation via weakness
                livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            } else {
                // Respiratory protection reduces fumes, but sludge contact is still dangerous.
                livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
