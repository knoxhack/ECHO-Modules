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
 * CityStalker - fast, stealthy humanoid found in Ruined Cityscape.
 * Ambushes from above/rooftops. Fast and agile.
 */
public class CityStalker extends Monster {

    private int stealthCooldown = 0;

    public CityStalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.32D) // Fast
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.JUMP_STRENGTH, 0.8D); // Good jumper
    }

    @Override
    public void tick() {
        super.tick();

        // Stealth ability - periodically becomes harder to see
        if (!this.level().isClientSide()) {
            stealthCooldown--;
            if (stealthCooldown <= 0) {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
                stealthCooldown = 200; // 10 seconds between stealth
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);

        if (hit && target instanceof Player player) {
            // Apply blindness (disoriented by ambush)
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            // Also slow them down
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
        }

        return hit;
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }
}
