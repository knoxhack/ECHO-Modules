package com.knoxhack.echoashfallprotocol.entity.boss;

import com.knoxhack.echoashfallprotocol.boss.BossHudSync;
import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.endgame.NexusFinalBossProfile;
import com.knoxhack.echoashfallprotocol.endgame.NexusFinalBossProfiles;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Profile-backed final boss for the path-specific Warfront finale.
 */
public class NexusFinalBossEntity extends PathfinderMob {
    private final ServerBossEvent bossEvent;
    private boolean profileConfigured;
    private boolean announced;
    private int phase = 1;
    private int pulseCooldown = 95;
    private int summonCooldown = 220;

    public NexusFinalBossEntity(EntityType<? extends NexusFinalBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 420;
        this.setPersistenceRequired();
        NexusFinalBossProfile profile = profile();
        this.bossEvent = new ServerBossEvent(
                UUID.randomUUID(),
                Component.literal(profile.title()),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 54.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.92D)
                .add(Attributes.ARMOR, 9.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.12D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.70D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 42.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        configureProfileOnce();
        NexusFinalBossProfile profile = profile();
        bossEvent.setProgress(Math.max(0.0F, this.getHealth() / Math.max(1.0F, this.getMaxHealth())));
        if (this.tickCount % 10 == 0) {
            BossHudSync.syncLiveBoss(this, phase);
        }
        if (!announced && this.tickCount > 20) {
            announced = true;
            announceNearby("[NEXUS] " + profile.title() + " active. " + profile.subtitle());
            playSoundCue(SoundEvents.BEACON_ACTIVATE, 1.3F, 0.55F);
        }

        updatePhase(profile);
        if (pulseCooldown > 0) {
            pulseCooldown--;
        }
        if (summonCooldown > 0) {
            summonCooldown--;
        }
        if (this.getTarget() != null && pulseCooldown <= 0) {
            pulse(profile);
            pulseCooldown = Math.max(45, 110 - phase * 18);
        }
        if (this.getTarget() != null && summonCooldown <= 0) {
            summonDefenders(profile, phase);
            summonCooldown = Math.max(90, 230 - phase * 42);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && target instanceof Player player) {
            switch (profile().path()) {
                case RESTORE -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 70, 0, false, true));
                case DESTROY -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 70, 0, false, true));
                case CONTROL -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0, false, true));
                case NONE -> {
                }
            }
        }
        return hurt;
    }

    @Override
    public void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof Player)) {
            amount *= 0.45F;
        }
        super.actuallyHurt(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (int i = 0; i < 6 + this.random.nextInt(4); i++) {
            this.spawnAtLocation(level, new ItemStack(ModItems.NEXUS_CRYSTAL.get()));
        }
        for (int i = 0; i < 4 + this.random.nextInt(4); i++) {
            this.spawnAtLocation(level, new ItemStack(ModItems.ENERGY_CELL.get()));
        }
        this.spawnAtLocation(level, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2));

        bossEvent.removeAllPlayers();
        announceNearby("[ECHO-7] " + profile().defeatLine());
        playSoundCue(SoundEvents.BEACON_DEACTIVATE, 1.4F, 0.75F);
        ServerPlayer directPlayer = null;
        if (source.getEntity() instanceof ServerPlayer player && PostNexusData.get(player).isPath(profile().path())) {
            directPlayer = player;
        }
        ServerPlayer creditPlayer = directPlayer != null
                ? directPlayer
                : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(72.0D),
                        player -> PostNexusData.get(player).isPath(profile().path())).stream().findFirst().orElse(null);
        if (creditPlayer != null) {
            NexusCampaignActions.creditFinaleBoss(creditPlayer, profile().path());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
        BossHudSync.clearBoss(player, profile().entityId());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("ProfileConfigured", profileConfigured);
        output.putBoolean("Announced", announced);
        output.putInt("Phase", phase);
        output.putInt("PulseCooldown", pulseCooldown);
        output.putInt("SummonCooldown", summonCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        profileConfigured = input.getBooleanOr("ProfileConfigured", false);
        announced = input.getBooleanOr("Announced", false);
        phase = input.getIntOr("Phase", 1);
        pulseCooldown = input.getIntOr("PulseCooldown", 95);
        summonCooldown = input.getIntOr("SummonCooldown", 220);
        bossEvent.setName(Component.literal(profile().title()));
    }

    private void configureProfileOnce() {
        if (profileConfigured) {
            return;
        }
        NexusFinalBossProfile profile = profile();
        setAttribute(Attributes.MAX_HEALTH, profile.maxHealth());
        setAttribute(Attributes.ATTACK_DAMAGE, profile.attackDamage());
        setAttribute(Attributes.MOVEMENT_SPEED, profile.movementSpeed());
        setAttribute(Attributes.ARMOR, profile.armor());
        this.setHealth(this.getMaxHealth());
        profileConfigured = true;
    }

    private void setAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void updatePhase(NexusFinalBossProfile profile) {
        float pct = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        if (phase == 1 && pct <= 0.66F) {
            phase = 2;
            announceNearby("[NEXUS] " + profile.phaseTwoLine());
            summonDefenders(profile, 2);
            playSoundCue(SoundEvents.WITHER_AMBIENT, 1.1F, 0.85F);
        } else if (phase == 2 && pct <= 0.33F) {
            phase = 3;
            announceNearby("[NEXUS] " + profile.phaseThreeLine());
            summonDefenders(profile, 3);
            playSoundCue(SoundEvents.WITHER_SPAWN, 1.2F, 1.05F);
        }
    }

    private void pulse(NexusFinalBossProfile profile) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = this.getBoundingBox().inflate(8.0D + phase);
        for (Player player : level.getEntitiesOfClass(Player.class, box, Player::isAlive)) {
            player.hurtServer(level, this.damageSources().magic(), profile.pulseDamage() + phase);
            switch (profile.path()) {
                case RESTORE -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 75, 0, false, false));
                case DESTROY -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, 85, 0, false, false));
                case CONTROL -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 90, 0, false, false));
                case NONE -> {
                }
            }
        }
        playSoundCue(SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.65F + phase * 0.12F);
    }

    private void summonDefenders(NexusFinalBossProfile profile, int count) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<NexusPressureMobEntity> defenderType = switch (profile.path()) {
            case RESTORE -> ModEntities.GRIDBOUND_HUSK.get();
            case DESTROY -> ModEntities.RELAY_WARDEN.get();
            case CONTROL -> ModEntities.SIGNAL_LEECH.get();
            case NONE -> ModEntities.GRIDBOUND_HUSK.get();
        };
        int existing = level.getEntitiesOfClass(NexusPressureMobEntity.class, this.getBoundingBox().inflate(22.0D),
                mob -> mob.isAlive() && mob.getType() == defenderType).size();
        int allowed = Math.min(Math.max(0, count), Math.max(0, 5 - existing));
        for (int i = 0; i < allowed; i++) {
            NexusPressureMobEntity defender = defenderType.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (defender == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 4.0D + this.random.nextDouble() * 5.0D;
            defender.setPos(this.getX() + Math.cos(angle) * distance, this.getY(), this.getZ() + Math.sin(angle) * distance);
            if (this.getTarget() instanceof Player target) {
                defender.setTarget(target);
            }
            level.addFreshEntity(defender);
        }
    }

    private void announceNearby(String message) {
        List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(72.0D));
        for (Player player : players) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private void playSoundCue(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
        }
    }

    public NexusFinalBossProfile profile() {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).toString();
        return NexusFinalBossProfiles.byEntityId(key)
                .orElseGet(() -> NexusFinalBossProfiles.byPath(PostNexusData.NexusPath.RESTORE).orElseThrow());
    }

    public PostNexusData.NexusPath path() {
        return profile().path();
    }
}
