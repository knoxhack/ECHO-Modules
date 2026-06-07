package com.knoxhack.echoashfallprotocol.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Alliance Status Effect — Granted to players who choose the RESTORE path.
 * Reduces radiation accumulation and provides passive regeneration.
 */
public class AllianceEffect extends MobEffect {

    public AllianceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00AAFF); // Light blue color
    }

    @Override
    public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // Passive regeneration (same as regeneration effect)
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(0.5f);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Apply every 2 seconds (40 ticks)
        return duration % 40 == 0;
    }
}
