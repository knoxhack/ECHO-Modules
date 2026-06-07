package com.knoxhack.echoashfallprotocol.entity.boss;

import com.knoxhack.echoashfallprotocol.boss.BossHudSync;
import com.knoxhack.echoashfallprotocol.dimension.ModDimensions;
import com.knoxhack.echoashfallprotocol.endgame.PrefallArchivesArenaService;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The Warden - Final boss of the Pre-Fall Archives.
 * 300 HP, 3 phases, summons defenders, AOE attacks.
 */
public class WardenBossEntity extends PathfinderMob {

    private static final float MAX_HEALTH = 300.0f;
    private static final float ATTACK_DAMAGE = 12.0f;
    private static final float MOVEMENT_SPEED = 0.25f;
    private static final int MAX_DEFENDERS = 8;
    private static final double DEFENDER_SCAN_RADIUS = 24.0D;
    
    // Phase thresholds (percentage of max health)
    private static final float PHASE_2_THRESHOLD = 0.60f; // 60% HP
    private static final float PHASE_3_THRESHOLD = 0.30f;   // 30% HP
    
    private int phase = 1;
    private int attackCooldown = 0;
    private int aoeCooldown = 0;
    private int summonCooldown = 0;
    private boolean hasIntroduced = false;
    private final ServerBossEvent bossEvent;

    public WardenBossEntity(EntityType<? extends WardenBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
        this.bossEvent = new ServerBossEvent(
                this.getUUID(),
                Component.literal("The Warden"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new AOEAttackGoal(this));
        this.goalSelector.addGoal(4, new SummonDefendersGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 50.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide()) return;
        bossEvent.setProgress(Math.max(0.0F, this.getHealth() / Math.max(1.0F, this.getMaxHealth())));
        if (this.tickCount % 10 == 0) {
            BossHudSync.syncLiveBoss(this, phase);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            leashToArena(serverLevel);
            trimAndLeashDefenders(serverLevel);
        }
        
        // Introduction dialogue on first spawn
        if (!hasIntroduced && this.tickCount > 20) {
            hasIntroduced = true;
            announceToNearbyPlayers("[THE WARDEN] I guard the last human record. Claimants must survive judgment.");
        }
        
        // Phase transitions based on health percentage
        float healthPercent = this.getHealth() / this.getMaxHealth();
        
        if (phase == 1 && healthPercent <= PHASE_2_THRESHOLD) {
            enterPhase2();
        } else if (phase == 2 && healthPercent <= PHASE_3_THRESHOLD) {
            enterPhase3();
        }
        
        // Cooldown management
        if (attackCooldown > 0) attackCooldown--;
        if (aoeCooldown > 0) aoeCooldown--;
        if (summonCooldown > 0) summonCooldown--;
        
        // Phase 3: Enrage effect - periodic AOE pulse
        if (phase == 3 && tickCount % 200 == 0) {
            performAOEPulse();
        }
    }

    private void enterPhase2() {
        phase = 2;
        announceToNearbyPlayers("[THE WARDEN] Archive lockdown active. Defender lanes opening; keep moving.");
        
        // Summon defenders immediately
        summonDefenders(2);
        
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 2, this.getZ(), 20, 1, 1, 1, 0);
            sl.playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 2.0f, 0.8f);
        }
    }

    private void enterPhase3() {
        phase = 3;
        announceToNearbyPlayers("[THE WARDEN] Final archive quarantine. Pulses accelerating; break the lockdown now.");
        
        // Speed boost and summon more defenders
        summonDefenders(4);
        
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 2, this.getZ(), 50, 2, 2, 2, 0.1);
            sl.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0f, 1.0f);
        }
    }

    @Override
    public void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        // Boss resistance - reduce damage from non-player sources
        if (!(source.getEntity() instanceof Player)) {
            amount *= 0.5f;
        }
        
        super.actuallyHurt(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        
        // Drop boss rewards
        // 5-8 Nexus Crystals
        int crystalCount = 5 + this.random.nextInt(4);
        for (int i = 0; i < crystalCount; i++) {
            this.spawnAtLocation(level, ModItems.NEXUS_CRYSTAL.get());
        }
        
        // Dense Alloy chunks
        int alloyCount = 3 + this.random.nextInt(5);
        for (int i = 0; i < alloyCount; i++) {
            this.spawnAtLocation(level, ModItems.DENSE_ALLOY_CHUNK.get());
        }
        
        // Energy cells
        int cellCount = 5 + this.random.nextInt(6);
        for (int i = 0; i < cellCount; i++) {
            this.spawnAtLocation(level, ModItems.ENERGY_CELL.get());
        }

        Player killer = source.getEntity() instanceof Player player ? player : null;
        ItemStack cipher = new ItemStack(ModItems.WARDEN_ARCHIVE_CIPHER.get());
        if (killer == null || !killer.getInventory().contains(cipher)) {
            this.spawnAtLocation(level, cipher);
        }
        
        // Announce victory
        if (killer != null) {
            announceToAllPlayers("[ECHO-7] " + killer.getName().getString()
                    + " defeated The Warden. Final protocol ready at the terminal.");
        }
        bossEvent.removeAllPlayers();
    }

    private void announceToNearbyPlayers(String message) {
        AABB box = this.getBoundingBox().inflate(50.0);
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class, box);
        for (Player player : nearby) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private void announceToAllPlayers(String message) {
        if (this.level() instanceof ServerLevel sl) {
            for (Player player : sl.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal(message));
            }
        }
    }

    private void summonDefenders(int count) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        int allowed = Math.min(count, Math.max(0, MAX_DEFENDERS - countActiveDefenders(sl)));
        if (allowed <= 0) {
            return;
        }
        Player target = nearestArenaPlayer();
        
        for (int i = 0; i < allowed; i++) {
            // Spawn Echo Drone as defender
            var defender = ModEntities.ECHO_DRONE.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (defender != null) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double distance = 5 + this.random.nextDouble() * 5;
                Vec3 spawn = PrefallArchivesArenaService.clampInsideArena(
                    this.getX() + Math.cos(angle) * distance,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * distance
                );
                defender.setPos(spawn.x(), spawn.y(), spawn.z());
                defender.setYRot(this.random.nextFloat() * 360.0F);
                defender.setXRot(0.0F);
                
                if (target != null) {
                    defender.setTarget(target);
                }
                
                sl.addFreshEntity(defender);
            }
        }
    }

    private void leashToArena(ServerLevel level) {
        if (!ModDimensions.isPrefallArchives(level) || PrefallArchivesArenaService.isInsideArena(this)) {
            return;
        }
        Vec3 safe = Vec3.atCenterOf(PrefallArchivesArenaService.WARDEN_POS);
        this.setPos(safe.x(), safe.y(), safe.z());
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
    }

    private void trimAndLeashDefenders(ServerLevel level) {
        if (!ModDimensions.isPrefallArchives(level)) {
            return;
        }
        List<Mob> defenders = activeDefenders(level);
        defenders.sort(Comparator.comparingDouble(this::distanceToSqr));
        for (int i = 0; i < defenders.size(); i++) {
            Mob defender = defenders.get(i);
            if (i >= MAX_DEFENDERS) {
                defender.discard();
                continue;
            }
            if (!PrefallArchivesArenaService.ARENA_BOX.contains(defender.position())) {
                Vec3 safe = PrefallArchivesArenaService.clampInsideArena(defender.getX(), defender.getY(), defender.getZ());
                defender.setPos(safe.x(), safe.y(), safe.z());
                defender.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private int countActiveDefenders(ServerLevel level) {
        return activeDefenders(level).size();
    }

    private List<Mob> activeDefenders(ServerLevel level) {
        return level.getEntitiesOfClass(Mob.class, PrefallArchivesArenaService.ARENA_BOX.inflate(DEFENDER_SCAN_RADIUS),
                mob -> mob.isAlive() && mob.getType() == ModEntities.ECHO_DRONE.get());
    }

    private Player nearestArenaPlayer() {
        return this.level().getEntitiesOfClass(Player.class, PrefallArchivesArenaService.ARENA_BOX, Player::isAlive)
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    private void performAOEPulse() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        
        // AOE damage to nearby players
        AABB box = this.getBoundingBox().inflate(8.0);
        List<Player> nearby = sl.getEntitiesOfClass(Player.class, box);
        
        for (Player player : nearby) {
            player.hurtServer(sl, this.damageSources().magic(), 8.0f);
        }
        
        // Visual and audio effects
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1, this.getZ(), 10, 4, 2, 4, 0);
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
        
        announceToNearbyPlayers("[THE WARDEN] Archive pulse!");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Phase", phase);
        output.putBoolean("HasIntroduced", hasIntroduced);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.phase = input.getIntOr("Phase", 1);
        this.hasIntroduced = input.getBooleanOr("HasIntroduced", false);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Boss doesn't despawn
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
        BossHudSync.clearBoss(player, "echoashfallprotocol:warden_boss");
    }

    public int getPhase() {
        return phase;
    }

    /**
     * Custom goal for AOE attack
     */
    static class AOEAttackGoal extends Goal {
        private final WardenBossEntity boss;
        private int cooldown = 0;

        AOEAttackGoal(WardenBossEntity boss) {
            this.boss = boss;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return boss.phase >= 2 && boss.getTarget() != null && 
                   boss.distanceToSqr(boss.getTarget()) < 64.0; // Within 8 blocks
        }

        @Override
        public void start() {
            boss.performAOEPulse();
            cooldown = 100; // 5 second cooldown
        }
    }

    /**
     * Custom goal for summoning defenders
     */
    static class SummonDefendersGoal extends Goal {
        private final WardenBossEntity boss;
        private int cooldown = 0;

        SummonDefendersGoal(WardenBossEntity boss) {
            this.boss = boss;
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return boss.getTarget() != null && boss.random.nextFloat() < 0.02f;
        }

        @Override
        public void start() {
            int count = boss.phase == 1 ? 1 : (boss.phase == 2 ? 2 : 3);
            boss.summonDefenders(count);
            cooldown = 300; // 15 second cooldown
        }
    }
}
