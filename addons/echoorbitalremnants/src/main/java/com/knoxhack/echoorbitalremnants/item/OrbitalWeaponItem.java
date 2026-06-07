package com.knoxhack.echoorbitalremnants.item;

import java.util.Comparator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OrbitalWeaponItem extends Item {
    private final WeaponProfile profile;

    public OrbitalWeaponItem(WeaponProfile profile, Properties properties) {
        super(properties);
        this.profile = profile;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ItemStack stack = player.getItemInHand(hand);
            LivingEntity target = findTarget(level, player, profile.range);
            if (target == null) {
                player.sendSystemMessage(Component.literal("ECHO-7 // " + profile.displayName + " found no lock."));
                player.getCooldowns().addCooldown(stack, 15);
                return InteractionResult.CONSUME;
            }

            target.hurtServer(serverLevel, profile.damageSource(player), profile.damage);
            if (profile.knockback > 0.0D) {
                Vec3 push = target.position().subtract(player.position()).normalize().scale(profile.knockback);
                target.push(push.x, 0.12D + push.y * 0.2D, push.z);
            }
            if (profile.explosion > 0.0F) {
                level.explode(player, target.getX(), target.getY(), target.getZ(), profile.explosion, Level.ExplosionInteraction.NONE);
            }
            stack.hurtAndBreak(1, player, hand);
            player.getCooldowns().addCooldown(stack, profile.cooldownTicks);
            player.sendSystemMessage(Component.literal("ECHO-7 // " + profile.displayName + " discharge confirmed."));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, profile.damageSource(attacker), Math.max(3.0F, profile.damage * 0.55F));
            if (profile.knockback > 0.0D) {
                Vec3 push = target.position().subtract(attacker.position()).normalize().scale(profile.knockback * 0.55D);
                target.push(push.x, 0.08D, push.z);
            }
        }
    }

    private LivingEntity findTarget(Level level, Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().inflate(range);
        return level.getEntities(player, search, entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && living != player
                        && living.distanceToSqr(player) <= range * range)
                .stream()
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> {
                    Vec3 direction = entity.getEyePosition().subtract(eye).normalize();
                    return direction.dot(look) >= profile.lockCone;
                })
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
    }

    public enum WeaponProfile {
        PLASMA_CUTTER("Plasma Cutter", 5.0D, 7.0F, 0.15D, 0.0F, 18, 0.45D),
        RAIL_SPIKE_LAUNCHER("Rail Spike Launcher", 12.0D, 10.0F, 0.35D, 0.0F, 35, 0.70D),
        GRAVITY_HAMMER("Gravity Hammer", 4.0D, 12.0F, 1.8D, 0.0F, 45, 0.10D),
        SOLAR_LANCE("Solar Lance", 10.0D, 9.0F, 0.25D, 0.0F, 28, 0.62D),
        NEXUS_PULSE_BLADE("Nexus Pulse Blade", 6.0D, 14.0F, 0.75D, 1.0F, 55, 0.35D);

        private final String displayName;
        private final double range;
        private final float damage;
        private final double knockback;
        private final float explosion;
        private final int cooldownTicks;
        private final double lockCone;

        WeaponProfile(String displayName, double range, float damage, double knockback, float explosion, int cooldownTicks, double lockCone) {
            this.displayName = displayName;
            this.range = range;
            this.damage = damage;
            this.knockback = knockback;
            this.explosion = explosion;
            this.cooldownTicks = cooldownTicks;
            this.lockCone = lockCone;
        }

        private net.minecraft.world.damagesource.DamageSource damageSource(LivingEntity attacker) {
            return attacker instanceof Player player ? attacker.damageSources().playerAttack(player) : attacker.damageSources().magic();
        }
    }
}
