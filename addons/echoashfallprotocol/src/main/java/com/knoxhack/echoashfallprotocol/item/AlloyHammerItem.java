package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Alloy Hammer — Tier 2 blunt weapon.
 * 10.0 damage, slow swing. AOE knockback in 1.5-block radius.
 */
public class AlloyHammerItem extends Item {

    private static final float BASE_DAMAGE = 10.0f;
    private static final double AOE_RADIUS = 1.5;

    public AlloyHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.isAlive() && target.level() instanceof ServerLevel sl) {
            var src = (attacker instanceof Player p) ? p.damageSources().playerAttack(p) : attacker.damageSources().generic();
            target.hurtServer(sl, src, BASE_DAMAGE * 0.5f);
            sl.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 1.6f);

            // AOE knockback to nearby entities
            AABB aoe = target.getBoundingBox().inflate(AOE_RADIUS);
            List<LivingEntity> nearby = target.level().getEntitiesOfClass(LivingEntity.class, aoe,
                    e -> e != attacker && e != target);
            for (LivingEntity entity : nearby) {
                double dx = entity.getX() - target.getX();
                double dz = entity.getZ() - target.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    entity.knockback(1.0, -dx / dist, -dz / dist);
                }
            }
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
