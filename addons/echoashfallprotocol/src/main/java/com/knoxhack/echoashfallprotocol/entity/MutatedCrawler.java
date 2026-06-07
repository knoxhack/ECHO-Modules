package com.knoxhack.echoashfallprotocol.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * MutatedCrawler - wall-crawling fast mutant found in Toxic Swamp.
 * Attacks in packs. Fast and agile wall-crawling mutant.
 */
public class MutatedCrawler extends Monster {

    public MutatedCrawler(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers(MutatedCrawler.class));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35D) // Very fast
                .add(Attributes.MAX_HEALTH, 10.0D) // Low health
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.JUMP_STRENGTH, 1.0D); // Excellent jumper
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, false));
            // Slowness from claw swipe
            if (this.random.nextFloat() < 0.3f) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, false));
            }
        }
        return hit;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true; // Mutated crawlers are harmed by water (purification)
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Emit acid spit particles
        if (this.level().isClientSide() && this.random.nextInt(20) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_LAVA,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    0.0, -0.1, 0.0);
        }
    }
}
