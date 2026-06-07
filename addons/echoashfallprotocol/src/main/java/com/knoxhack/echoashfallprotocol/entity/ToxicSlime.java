package com.knoxhack.echoashfallprotocol.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ToxicSlime - living chemical blob found in Toxic Swamp.
 * Poisons players on contact and hit.
 * Splits into smaller versions when hit.
 */
public class ToxicSlime extends Monster {

    public ToxicSlime(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.8D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 1.0D);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);

        if (hit && target instanceof Player player) {
            // Apply poison on hit
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
        }

        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Emit toxic bubble particles
        if (this.level().isClientSide() && this.random.nextInt(10) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_WATER,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    0.0, 0.05, 0.0);
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return false; // Slimes are fine with water, it's toxic sludge
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Split into smaller slimes when damaged (if not killed and big enough)
        if (!this.level().isClientSide() && this.getHealth() > amount && this.getHealth() > 6.0f) {
            if (this.random.nextFloat() < 0.25f) {
                // Spawn a smaller slime
                ToxicSlime baby = ModEntities.TOXIC_SLIME.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (baby != null) {
                    baby.setPos(this.getX(), this.getY(), this.getZ());
                    baby.setBaby(true);
                    level.addFreshEntity(baby);
                }
            }
        }
        return super.hurtServer(level, source, amount);
    }
}
