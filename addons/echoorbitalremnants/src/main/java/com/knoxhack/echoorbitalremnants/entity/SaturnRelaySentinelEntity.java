package com.knoxhack.echoorbitalremnants.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SaturnRelaySentinelEntity extends Vex {
    private boolean announced;

    public SaturnRelaySentinelEntity(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 80 == 0 && getTarget() instanceof Player player && distanceToSqr(player) < 81.0D) {
            if (!announced) {
                announced = true;
                player.sendSystemMessage(Component.literal("ECHO-7 // First contact: Saturn Relay Sentinel. Watch for relay pulses before pressure shear."));
            }
            level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.8F, 0.65F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5D, getZ(), 12, 0.35D, 0.35D, 0.35D, 0.02D);
            }
            player.sendSystemMessage(Component.literal("ECHO-7 // Saturn relay sentinel is bending the return signal."));
        }
    }
}
