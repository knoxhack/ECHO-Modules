package com.knoxhack.echoashfallprotocol.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * AshWraith - ghostly figure that emerges during dust storms in Crash Zone.
 * Attacks by disorienting and weakening players.
 * Semi-transparent and hard to see in dust.
 */
public class AshWraith extends Monster {

    private int attackCooldown = 0;

    public AshWraith(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();

        // AshWraiths are harder to see - apply invisibility effect periodically
        if (!this.level().isClientSide() && this.level().getGameTime() % 60 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
        }

        // Special attack: disorient players on contact.
        if (!this.level().isClientSide() && attackCooldown > 0) {
            attackCooldown--;
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);

        if (hit && target instanceof ServerPlayer player && attackCooldown <= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
            player.sendSystemMessage(Component.literal(
                    "\u00A7c[ECHO-7]\u00A7r Ash Wraith contact detected. Vision and muscle response disrupted."));
            attackCooldown = 40; // 2 second cooldown
        }

        return hit;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true; // Water dissipates ash
    }
}
