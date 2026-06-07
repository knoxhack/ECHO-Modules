package com.knoxhack.echoashfallprotocol.entity.boss;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.boss.BossHudSync;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
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

import java.util.List;
import java.util.UUID;

/**
 * Profile-driven boss behavior for the underground biome guardian arc.
 */
public class BiomeBossEntity extends PathfinderMob {
    private static final float FALLBACK_MAX_HEALTH = 160.0f;

    private final ServerBossEvent bossEvent;
    private int pulseCooldown = 80;
    private int summonCooldown = 220;
    private int phase = 1;
    private boolean announced;
    private boolean profileConfigured;

    public BiomeBossEntity(EntityType<? extends BiomeBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 240;
        this.setPersistenceRequired();
        BiomeGuardianProfile profile = profile();
        this.bossEvent = new ServerBossEvent(
                UUID.randomUUID(),
                Component.literal(profile.title()),
                profile.visual().bossBarColor(),
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.bossEvent.setDarkenScreen(profile.visual().darkenScreen());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, FALLBACK_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 44.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.80)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 36.0F));
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
        bossEvent.setProgress(Math.max(0.0f, this.getHealth() / Math.max(1.0f, this.getMaxHealth())));
        if (this.tickCount % 10 == 0) {
            BossHudSync.syncLiveBoss(this, phase);
        }

        BiomeGuardianProfile profile = profile();
        if (!announced && this.tickCount > 20) {
            announced = true;
            announceNearby("\u00A75[" + profile.title() + "]\u00A7r " + profile.lore());
            playGuardianSound(SoundEvents.BEACON_ACTIVATE, 1.0f, 0.65f);
        }

        updatePhase(profile);

        if (pulseCooldown > 0) pulseCooldown--;
        if (summonCooldown > 0) summonCooldown--;

        if (this.getTarget() != null && pulseCooldown <= 0 && this.distanceToSqr(this.getTarget()) < 144.0) {
            performProfilePulse(profile);
            pulseCooldown = Math.max(35, profile.pulseCooldownBase() + this.random.nextInt(45) - (phase - 1) * 18);
        }

        if (this.getTarget() != null && summonCooldown <= 0) {
            summonDefenders(profile);
            summonCooldown = Math.max(80, profile.summonCooldownBase() + this.random.nextInt(90) - (phase - 1) * 35);
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
        if (!(source.getEntity() instanceof Player)) {
            amount *= 0.35f;
        }
        super.actuallyHurt(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        BiomeGuardianProfile profile = profile();
        dropProfileLoot(level, profile);
        awardGuardianFactionReputation(level, source, profile);
        announceNearby("\u00A76[ECHO-7]\u00A7r " + profile.defeatLine());
        playGuardianSound(SoundEvents.BEACON_DEACTIVATE, 1.3f, 0.75f);
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
        output.putInt("PulseCooldown", pulseCooldown);
        output.putInt("SummonCooldown", summonCooldown);
        output.putInt("GuardianPhase", phase);
        output.putBoolean("GuardianAnnounced", announced);
        output.putBoolean("ProfileConfigured", profileConfigured);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        pulseCooldown = input.getIntOr("PulseCooldown", 80);
        summonCooldown = input.getIntOr("SummonCooldown", 220);
        phase = input.getIntOr("GuardianPhase", 1);
        announced = input.getBooleanOr("GuardianAnnounced", false);
        profileConfigured = input.getBooleanOr("ProfileConfigured", false);
        bossEvent.setName(Component.literal(profile().title()));
    }

    private void configureProfileOnce() {
        if (profileConfigured) {
            return;
        }
        BiomeGuardianProfile profile = profile();
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(profile.maxHealth());
        }
        var attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(profile.attackDamage());
        }
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(profile.movementSpeed());
        }
        var armor = this.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(profile.armor());
        }
        this.setHealth(this.getMaxHealth());
        profileConfigured = true;
    }

    private void updatePhase(BiomeGuardianProfile profile) {
        float healthPercent = this.getHealth() / Math.max(1.0f, this.getMaxHealth());
        if (phase == 1 && healthPercent <= 0.66f) {
            phase = 2;
            announceNearby("\u00A75[" + profile.title() + "]\u00A7r " + profile.phaseTwoLine());
            summonDefenders(profile, 1);
            spawnPhaseParticles(profile, 28);
            playGuardianSound(SoundEvents.WITHER_AMBIENT, 1.2f, 0.85f);
        } else if (phase == 2 && healthPercent <= 0.33f) {
            phase = 3;
            pulseCooldown = Math.min(pulseCooldown, 35);
            summonCooldown = Math.min(summonCooldown, 80);
            announceNearby("\u00A74[" + profile.title() + "]\u00A7r " + profile.phaseThreeLine());
            summonDefenders(profile, 2);
            spawnPhaseParticles(profile, 52);
            playGuardianSound(SoundEvents.WITHER_SPAWN, 1.4f, 1.05f);
        }
    }

    private void performProfilePulse(BiomeGuardianProfile profile) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }

        AABB box = this.getBoundingBox().inflate(profile.pulseRadius() + phase * 0.65f);
        for (Player player : sl.getEntitiesOfClass(Player.class, box)) {
            float damage = profile.pulseDamage() + phase;
            player.hurtServer(sl, this.damageSources().magic(), damage);
            applyEffects(profile.pulseEffects(), player);
            if (profile.pulseKnockback()) {
                applyKnockback(player, 0.70D + phase * 0.10D);
            }
        }

        performSignatureAbility(profile, sl);
        spawnPhaseParticles(profile, 18 + phase * 8);
        playGuardianSound(profile.pulseSound().sound(), 1.4f, pulsePitch(profile));
    }

    private void applyStrikeEffect(BiomeGuardianProfile profile, Player player) {
        applyEffects(profile.strikeEffects(), player);
    }

    private void applyEffects(List<BiomeGuardianProfile.StatusEffect> effects, Player player) {
        for (BiomeGuardianProfile.StatusEffect effect : effects) {
            player.addEffect(effect.instance(phase));
        }
    }

    private void applyKnockback(Player player, double strength) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double length = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
        player.push(dx / length * strength, 0.32D + phase * 0.04D, dz / length * strength);
    }

    private void summonDefenders(BiomeGuardianProfile profile) {
        summonDefenders(profile, 0);
    }

    private void summonDefenders(BiomeGuardianProfile profile, int bonus) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }

        EntityType<? extends Entity> defenderType = profile.defenderType().get();
        int existing = sl.getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(18.0),
                entity -> entity.isAlive() && entity.getType() == defenderType).size();
        int count = Math.min(1 + phase + bonus, Math.max(0, profile.maxDefenders() - existing));

        for (int i = 0; i < count; i++) {
            Entity defender = defenderType.create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (defender == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 3.0 + this.random.nextDouble() * 4.5;
            defender.setPos(this.getX() + Math.cos(angle) * distance, this.getY(), this.getZ() + Math.sin(angle) * distance);
            if (defender instanceof Mob mob && this.getTarget() instanceof Player target) {
                mob.setTarget(target);
            }
            sl.addFreshEntity(defender);
        }
    }

    private void dropProfileLoot(ServerLevel level, BiomeGuardianProfile profile) {
        for (BiomeGuardianProfile.LootEntry entry : profile.rewardBundle().entries()) {
            ItemStack stack = entry.stack(this.random);
            this.spawnAtLocation(level, stack);
        }
    }

    private void awardGuardianFactionReputation(ServerLevel level, DamageSource source, BiomeGuardianProfile profile) {
        if (source.getEntity() instanceof ServerPlayer player) {
            awardGuardianFactionReputation(player, profile);
            return;
        }
        AABB box = this.getBoundingBox().inflate(56.0);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player instanceof ServerPlayer serverPlayer) {
                awardGuardianFactionReputation(serverPlayer, profile);
            }
        }
    }

    private void awardGuardianFactionReputation(ServerPlayer player, BiomeGuardianProfile profile) {
        EchoCoreServices.addFactionReputation(player, profile.ownerFaction(), 12);
        EchoCoreServices.markFactionContacted(player, profile.ownerFaction());
        player.sendSystemMessage(Component.literal("\u00A76[ECHO-7]\u00A7r "
                + AshfallFactionMap.displayName(profile.ownerFaction())
                + " archived the " + profile.title() + " datacore."));
    }

    private void spawnPhaseParticles(BiomeGuardianProfile profile, int count) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        var particle = profile.particleCue().particle();
        sl.sendParticles(particle, this.getX(), this.getY() + 1.4, this.getZ(), count, 1.8, 1.0, 1.8, 0.04);
    }

    private void playGuardianSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
        }
    }

    private float pulsePitch(BiomeGuardianProfile profile) {
        return switch (profile.ability()) {
            case CRYO_LOCK -> 0.55f;
            case TOXIC_BROOD -> 0.65f;
            case NEXUS_RECURSION -> 1.35f;
            case SHADOW_AMBUSH -> 1.1f;
            default -> 0.75f;
        };
    }

    private void performSignatureAbility(BiomeGuardianProfile profile, ServerLevel level) {
        if (profile.signatureHeals()) {
            this.heal(2.0f + phase);
        }
        if (profile.signatureGrantsResistance()) {
            this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    60 + phase * 20, phase >= 3 && profile.signatureHeals() ? 1 : 0));
        }
        int bonusSummons = profile.signatureSummonBonus(phase);
        if (bonusSummons > 0) {
            summonDefenders(profile, bonusSummons);
        }
        if (profile.signatureRetargetsDefenders()) {
            retargetNearbyDefenders(level, profile);
        }
        double teleportRange = profile.signatureTeleportRange();
        if (teleportRange > 0.0D) {
            teleportNearTarget(teleportRange);
        }
        float damageScale = profile.signatureDamageScale(phase);
        if (damageScale > 0.0F) {
            applyAbilityShock(level, profile, damageScale);
        }
    }

    private void applyAbilityShock(ServerLevel level, BiomeGuardianProfile profile, float damageScale) {
        AABB box = this.getBoundingBox().inflate(profile.pulseRadius() + 1.5D + phase);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            float damage = Math.max(1.0f, profile.pulseDamage() * damageScale + phase * 0.5f);
            player.hurtServer(level, this.damageSources().magic(), damage);
            applyEffects(profile.pulseEffects(), player);
            if (profile.pulseKnockback()) {
                applyKnockback(player, 0.45D + phase * 0.08D);
            }
        }
        spawnPhaseParticles(profile, 10 + phase * 6);
    }

    private void retargetNearbyDefenders(ServerLevel level, BiomeGuardianProfile profile) {
        if (!(this.getTarget() instanceof Player target)) {
            return;
        }
        EntityType<? extends Entity> defenderType = profile.defenderType().get();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(20.0),
                entity -> entity.isAlive() && entity.getType() == defenderType)) {
            if (entity instanceof Mob mob) {
                mob.setTarget(target);
            }
        }
    }

    private void teleportNearTarget(double range) {
        if (!(this.getTarget() instanceof Player target)) {
            return;
        }
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double distance = 2.5D + this.random.nextDouble() * range;
        double x = target.getX() + Math.cos(angle) * distance;
        double z = target.getZ() + Math.sin(angle) * distance;
        this.teleportTo(x, target.getY(), z);
    }

    private void announceNearby(String message) {
        List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(56.0));
        for (Player player : players) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private BiomeGuardianProfile profile() {
        return BiomeGuardianProfiles.byBossPath(getBossKey())
                .or(() -> BiomeGuardianProfiles.all().stream().findFirst())
                .orElseThrow();
    }

    public int getGuardianPhase() {
        return phase;
    }

    private String getBossKey() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
    }

}
