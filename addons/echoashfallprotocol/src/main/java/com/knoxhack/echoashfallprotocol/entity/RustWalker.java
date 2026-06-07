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
 * RustWalker - animated scrap metal construct found in Industrial Ruins.
 * High armor, slow, powerful hits. Animated machinery hostile.
 */
public class RustWalker extends Monster {

    public RustWalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.7D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.14D) // Slow
                .add(Attributes.MAX_HEALTH, 40.0D) // High health
                .add(Attributes.ATTACK_DAMAGE, 8.0D) // Powerful hits
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.ARMOR, 10.0D) // Heavy armor
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D); // Hard to knock back
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);

        if (hit && target instanceof Player player) {
            // Heavy blow knocks player back and weakens them
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
        }

        return hit;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true; // Rust walkers take extra damage from water (rusting)
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Emit rust particle effects
        if (this.level().isClientSide() && this.random.nextInt(15) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    0.0, 0.05, 0.0);
        }
    }
}
