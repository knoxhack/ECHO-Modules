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
 * ToxicWasteBarrelBlock - decorative toxic waste container.
 * Damages entities on contact. Found in Toxic Swamp and Industrial Ruins.
 */
public class ToxicWasteBarrelBlock extends Block {

    public ToxicWasteBarrelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            if (entity instanceof Player player && HazardZoneManager.hasRespiratoryProtection(player)) {
                super.stepOn(level, pos, state, entity);
                return;
            }
            // Toxic barrel leaks poison
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
        }
        super.stepOn(level, pos, state, entity);
    }
}
