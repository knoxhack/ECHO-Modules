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
 * SteamWraith - ghostly figure near steam vents in Industrial Ruins.
 * Burn damage + fear effect. Burned worker apparition.
 */
public class SteamWraith extends Monster {

    public SteamWraith(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();

        // Steam wraiths emit cloud particles (client-side only)
        if (this.level().isClientSide() && this.level().getGameTime() % 10 == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    0.0, 0.1, 0.0);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);

        if (hit && target instanceof Player player) {
            // Burn damage
            player.setRemainingFireTicks(60); // 3 seconds of fire
            // Fear effect (simulated with nausea)
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
        }

        return hit;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true; // Water disperses steam wraiths
    }
}
