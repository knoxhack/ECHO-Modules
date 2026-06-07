package com.knoxhack.echoashfallprotocol.entity;

import com.knoxhack.echoashfallprotocol.endgame.NexusPressureMobProfile;
import com.knoxhack.echoashfallprotocol.endgame.NexusPressureMobProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Profile-backed pressure mob used only by Nexus Prime Relay encounters.
 */
public class NexusPressureMobEntity extends Monster {
    private boolean profileConfigured;
    private int pulseCooldown = 80;

    public NexusPressureMobEntity(EntityType<? extends NexusPressureMobEntity> type, Level level) {
        super(type, level);
        this.xpReward = 22;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.08D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.82D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 18.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        configureProfileOnce();
        if (pulseCooldown > 0) {
            pulseCooldown--;
        }
        if (this.getTarget() instanceof Player && pulseCooldown <= 0) {
            performPulse(profile());
            pulseCooldown = profile().ability() == NexusPressureMobProfile.Ability.SIGNAL_LEECH ? 55 : 85;
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && target instanceof Player player) {
            applyStrikeEffect(profile(), player);
        }
        return hurt;
    }

    @Override
    public void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof Player) && profile().ability() == NexusPressureMobProfile.Ability.WARDEN_BULWARK) {
            amount *= 0.55F;
        }
        super.actuallyHurt(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("ProfileConfigured", profileConfigured);
        output.putInt("PulseCooldown", pulseCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        profileConfigured = input.getBooleanOr("ProfileConfigured", false);
        pulseCooldown = input.getIntOr("PulseCooldown", 80);
    }

    private void configureProfileOnce() {
        if (profileConfigured) {
            return;
        }
        NexusPressureMobProfile profile = profile();
        setAttribute(Attributes.MAX_HEALTH, profile.maxHealth());
        setAttribute(Attributes.ATTACK_DAMAGE, profile.attackDamage());
        setAttribute(Attributes.MOVEMENT_SPEED, profile.movementSpeed());
        setAttribute(Attributes.ARMOR, profile.armor());
        if (profile.ability() == NexusPressureMobProfile.Ability.WARDEN_BULWARK) {
            setAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.80D);
        }
        this.setHealth(this.getMaxHealth());
        profileConfigured = true;
    }

    private void setAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void performPulse(NexusPressureMobProfile profile) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = this.getBoundingBox().inflate(profile.ability() == NexusPressureMobProfile.Ability.NULL_FIELD ? 8.0D : 5.5D);
        for (Player player : level.getEntitiesOfClass(Player.class, box, Player::isAlive)) {
            player.hurtServer(level, this.damageSources().magic(), profile.pulseDamage());
            applyPulseEffect(profile, player);
        }
        if (profile.ability() == NexusPressureMobProfile.Ability.WARDEN_BULWARK) {
            this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 70, 0, false, false));
        }
    }

    private void applyStrikeEffect(NexusPressureMobProfile profile, Player player) {
        switch (profile.ability()) {
            case GRID_PRESSURE -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0, false, true));
            case WARDEN_BULWARK -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0, false, true));
            case SIGNAL_LEECH -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, true));
            case NULL_FIELD -> player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 90, 0, false, true));
        }
    }

    private void applyPulseEffect(NexusPressureMobProfile profile, Player player) {
        switch (profile.ability()) {
            case GRID_PRESSURE -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 65, 0, false, false));
            case WARDEN_BULWARK -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0, false, false));
            case SIGNAL_LEECH -> player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 0, false, false));
            case NULL_FIELD -> {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 110, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0, false, false));
            }
        }
    }

    public NexusPressureMobProfile profile() {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).toString();
        return NexusPressureMobProfiles.byEntityId(key)
                .orElseGet(() -> NexusPressureMobProfiles.byEntityId("echoashfallprotocol:gridbound_husk").orElseThrow());
    }
}
