package com.knoxhack.echoashfallprotocol.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ECHO Drone - Hostile flying mob that scans players with glowing effect.
 * Extends Allay for renderer compatibility.
 */
public class EchoDrone extends Monster {

    private int scanCooldown = 0;

    public EchoDrone(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Scan players instead of melee attack
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true, true));
    }

    @Override
    public void tick() {
        super.tick();

        // Scan nearby players - apply glowing effect to simulate scanning
        if (!this.level().isClientSide()) {
            scanCooldown--;
            if (scanCooldown <= 0) {
                Player nearest = this.level().getNearestPlayer(this, 16.0);
                if (nearest != null && this.hasLineOfSight(nearest)) {
                    nearest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                    scanCooldown = 60; // 3 second cooldown between scans
                }
            }
        }

        // Hover behavior - maintain altitude
        if (!this.onGround() && this.getDeltaMovement().y < 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }
}
