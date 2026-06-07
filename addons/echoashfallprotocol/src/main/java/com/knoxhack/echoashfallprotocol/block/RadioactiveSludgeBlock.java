package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Radioactive sludge used in radiation landmarks and hot spots.
 */
public class RadioactiveSludgeBlock extends Block {
    public RadioactiveSludgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            boolean protectedFromFumes = entity instanceof Player player
                    && HazardZoneManager.hasRespiratoryProtection(player);
            if (!protectedFromFumes) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
            }
            if (entity instanceof ServerPlayer player && level.getGameTime() % 40 == 0) {
                RadiationHelper.addEnvironmentalRadiation(player, 3.0F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.05D,
                    pos.getZ() + random.nextDouble(),
                    0.0D,
                    0.02D,
                    0.0D);
        }
    }
}
