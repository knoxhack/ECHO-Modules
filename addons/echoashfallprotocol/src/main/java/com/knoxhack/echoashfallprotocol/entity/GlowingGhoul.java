package com.knoxhack.echoashfallprotocol.entity;

import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Glowing Ghoul - highly irradiated zombie variant.
 * Emits light and radiation aura that affects nearby players.
 * Found exclusively in Radiation Zones.
 */
public class GlowingGhoul extends Monster {

    public GlowingGhoul(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    public void tick() {
        super.tick();

        // Emit radiation to nearby players every 2 seconds
        if (!this.level().isClientSide() && this.level().getGameTime() % 40 == 0) {
            for (Player player : this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(5.0D))) {
                if (player instanceof ServerPlayer serverPlayer) {
                    RadiationHelper.addRadiation(serverPlayer, 1.5f);
                }
            }
        }

        // Glowing effect - self-illuminated (refresh every 80 ticks, effect lasts 100)
        if (!this.level().isClientSide() && this.level().getGameTime() % 80 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
        }

        // Berserk at low health — speed + strength surge
        if (!this.level().isClientSide() && this.getHealth() < 8.0f && this.level().getGameTime() % 60 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 1, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, false, false));
        }

        // Emit light particles - visible glow effect
        if (this.level().isClientSide() && this.random.nextInt(5) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    0.0, 0.05, 0.0);
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return true; // Glowing ghouls take damage from water (purification)
    }
}
