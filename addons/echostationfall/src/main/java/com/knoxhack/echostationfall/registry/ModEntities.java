package com.knoxhack.echostationfall.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationfallCooldown;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallStationState;
import com.knoxhack.echostationfall.integration.StationfallSuitState;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ModEntities {
    private static final int ENTITY_PULSE_COOLDOWN_TICKS = 60;
    private static final int GLOBAL_ENTITY_PULSE_COOLDOWN_TICKS = 20;
    private static final int MIMIC_MESSAGE_COOLDOWN_TICKS = 20 * 90;
    private static final int ENTITY_MESSAGE_COOLDOWN_TICKS = 20 * 90;

    public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoStationfall.MODID);

    public static final EchoBackendRegistryEntry<EntityType<HollowCrewmanEntity>> HOLLOW_CREWMAN =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "hollow_crewman",
                    HollowCrewmanEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8)
            );
    public static final EchoBackendRegistryEntry<EntityType<EvaStalkerEntity>> EVA_STALKER =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "eva_stalker",
                    EvaStalkerEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.65F, 2.05F).clientTrackingRange(10)
            );
    public static final EchoBackendRegistryEntry<EntityType<MedicalHuskEntity>> MEDICAL_HUSK =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "medical_husk",
                    MedicalHuskEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8)
            );
    public static final EchoBackendRegistryEntry<EntityType<HydroponicGrowthEntity>> HYDROPONIC_GROWTH =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "hydroponic_growth",
                    HydroponicGrowthEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.85F, 1.35F).clientTrackingRange(8)
            );
    public static final EchoBackendRegistryEntry<EntityType<MaintenanceDroneEntity>> MAINTENANCE_DRONE =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "maintenance_drone",
                    MaintenanceDroneEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.7F, 0.7F).clientTrackingRange(10)
            );
    public static final EchoBackendRegistryEntry<EntityType<ScreamingSignalEntity>> SCREAMING_SIGNAL =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "screaming_signal",
                    ScreamingSignalEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.5F, 0.9F).clientTrackingRange(8)
            );
    public static final EchoBackendRegistryEntry<EntityType<StationMimicEntity>> STATION_MIMIC =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "station_mimic",
                    StationMimicEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.9F, 1.2F).clientTrackingRange(8)
            );
    public static final EchoBackendRegistryEntry<EntityType<SuitWithoutBodyEntity>> SUIT_WITHOUT_BODY =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "suit_without_body",
                    SuitWithoutBodyEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.65F, 2.05F).clientTrackingRange(10)
            );
    public static final EchoBackendRegistryEntry<EntityType<StationMotherEntity>> STATION_MOTHER =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "station_mother",
                    StationMotherEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(1.1F, 2.6F).clientTrackingRange(12)
            );

    private ModEntities() {
    }

    public static void register(Object bus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, bus);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, HOLLOW_CREWMAN, HollowCrewmanEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, EVA_STALKER, EvaStalkerEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, MEDICAL_HUSK, MedicalHuskEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, HYDROPONIC_GROWTH, HydroponicGrowthEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, MAINTENANCE_DRONE, MaintenanceDroneEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SCREAMING_SIGNAL, ScreamingSignalEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, STATION_MIMIC, StationMimicEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SUIT_WITHOUT_BODY, SuitWithoutBodyEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, STATION_MOTHER, StationMotherEntity.createAttributes().build());
    }

    static void give(Player player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    static void pulse(Player player, int oxygen, int pressure, int panic) {
        StationfallSuitState suit = StationfallSuitState.get(player);
        suit.drainOxygen(oxygen);
        if (pressure > 0) {
            suit.compromisePressure(pressure);
        }
        suit.save(player);
        SignalPanicState.get(player).gain(player, panic);
    }

    static boolean pulseReady(Player player, String key) {
        return StationfallCooldown.ready(player, "entity.pulse." + key, ENTITY_PULSE_COOLDOWN_TICKS);
    }

    static boolean hazardPulseReady(Player player, String key) {
        return StationfallCooldown.ready(player, "entity.pulse.global", GLOBAL_ENTITY_PULSE_COOLDOWN_TICKS)
                && pulseReady(player, key);
    }

    static boolean messageReady(Player player, String key) {
        return StationfallCooldown.ready(player, "message.entity." + key, ENTITY_MESSAGE_COOLDOWN_TICKS);
    }

    static StationPowerState localPower(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel && StationfallDimensions.isStation(serverLevel)) {
            return StationfallStationState.get(serverLevel)
                    .powerState(StationSection.fromPosition(entity.blockPosition()));
        }
        return StationPowerState.EMERGENCY;
    }

    public static class HollowCrewmanEntity extends Zombie {
        public HollowCrewmanEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 30)
                    .add(Attributes.ATTACK_DAMAGE, 5)
                    .add(Attributes.ARMOR, 3);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide()
                    && tickCount % 70 == 0
                    && getTarget() instanceof Player player
                    && distanceToSqr(player) < 16) {
                if (hazardPulseReady(player, "hollow_crewman")) {
                    pulse(player, 4, 3, 5);
                }
                if (messageReady(player, "hollow_crewman.check_in")) {
                    player.sendSystemMessage(Component.literal("FALSE ECHO // Crew check-in accepted."));
                }
            }
        }
    }

    public static class EvaStalkerEntity extends Zombie {
        public EvaStalkerEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 34)
                    .add(Attributes.ATTACK_DAMAGE, 6)
                    .add(Attributes.MOVEMENT_SPEED, 0.28)
                    .add(Attributes.ARMOR, 5);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide() && getTarget() instanceof Player player) {
                if (tickCount % 120 == 0 && distanceToSqr(player) > 16) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    teleportTo(player.getX() + Math.cos(angle) * 4, player.getY(), player.getZ() + Math.sin(angle) * 4);
                    if (hazardPulseReady(player, "eva_stalker.teleport")) {
                        SignalPanicState.get(player).gain(player, 8);
                    }
                }
                if (tickCount % 80 == 0 && distanceToSqr(player) < 25 && hazardPulseReady(player, "eva_stalker")) {
                    pulse(player, 3, 6, 4);
                }
            }
        }
    }

    public static class MedicalHuskEntity extends Zombie {
        public MedicalHuskEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 22)
                    .add(Attributes.ATTACK_DAMAGE, 4)
                    .add(Attributes.MOVEMENT_SPEED, 0.35);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide()
                    && tickCount % 50 == 0
                    && getTarget() instanceof Player player
                    && distanceToSqr(player) < 9) {
                if (hazardPulseReady(player, "medical_husk")) {
                    pulse(player, 5, 0, 6);
                }
            }
        }
    }

    public static class HydroponicGrowthEntity extends Zombie {
        public HydroponicGrowthEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 42)
                    .add(Attributes.ATTACK_DAMAGE, 3)
                    .add(Attributes.MOVEMENT_SPEED, 0.12)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide() && tickCount % 60 == 0) {
                Player player = level().getNearestPlayer(this, 6);
                if (player != null) {
                    if (hazardPulseReady(player, "hydroponic_growth")) {
                        pulse(player, 2, 0, 4);
                    }
                }
            }
        }
    }

    public static class MaintenanceDroneEntity extends Vex {
        public MaintenanceDroneEntity(EntityType<? extends Vex> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Vex.createAttributes()
                    .add(Attributes.MAX_HEALTH, 28)
                    .add(Attributes.ATTACK_DAMAGE, 5);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide() && tickCount % 40 == 0) {
                Player player = level().getNearestPlayer(this, 10);
                if (player != null && (localPower(this).hostile() || SignalPanicState.get(player).high())) {
                    setTarget(player);
                    if (hazardPulseReady(player, "maintenance_drone")) {
                        pulse(player, 2, 2, 3);
                    }
                }
            }
        }
    }

    public static class ScreamingSignalEntity extends Vex {
        public ScreamingSignalEntity(EntityType<? extends Vex> type, Level level) {
            super(type, level);
            setGlowingTag(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Vex.createAttributes()
                    .add(Attributes.MAX_HEALTH, 18)
                    .add(Attributes.ATTACK_DAMAGE, 3);
        }

        @Override
        public void tick() {
            super.tick();
            setInvisible(false);
            setGlowingTag(true);
            if (!level().isClientSide() && tickCount % 35 == 0) {
                Player player = level().getNearestPlayer(this, 9);
                if (player != null) {
                    setTarget(player);
                    if (messageReady(player, "screaming_signal.static")) {
                        player.sendSystemMessage(Component.literal("ECHO-7 // Screaming Signal nearby. Break line of sight or stabilize power."));
                    }
                    if (hazardPulseReady(player, "screaming_signal")) {
                        SignalPanicState.get(player).gain(player, 10);
                        if (SignalPanicState.get(player).critical()) {
                            StationfallSuitState suit = StationfallSuitState.get(player);
                            suit.drainOxygen(4);
                            suit.save(player);
                        }
                    }
                }
            }
        }
    }

    public static class StationMimicEntity extends Zombie {
        private boolean awake;

        public StationMimicEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
            setNoAi(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 26)
                    .add(Attributes.ATTACK_DAMAGE, 7)
                    .add(Attributes.MOVEMENT_SPEED, 0.24)
                    .add(Attributes.ARMOR, 6);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide() && !awake) {
                Player player = level().getNearestPlayer(this, 4);
                if (player != null) {
                    awake = true;
                    setNoAi(false);
                    setTarget(player);
                    SignalPanicState.get(player).gain(player, 12);
                    if (StationfallCooldown.ready(player, "message.entity.mimic.storage_manifest", MIMIC_MESSAGE_COOLDOWN_TICKS)) {
                        player.sendSystemMessage(Component.literal("FALSE ECHO // Storage manifest updated. Door label changed."));
                    }
                }
            }
        }
    }

    public static class SuitWithoutBodyEntity extends Zombie {
        public SuitWithoutBodyEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 54)
                    .add(Attributes.ATTACK_DAMAGE, 9)
                    .add(Attributes.MOVEMENT_SPEED, 0.31)
                    .add(Attributes.ARMOR, 8);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide()
                    && tickCount % 90 == 0
                    && getTarget() instanceof Player player
                    && distanceToSqr(player) < 36) {
                if (hazardPulseReady(player, "suit_without_body")) {
                    pulse(player, 8, 8, 12);
                }
                if (messageReady(player, "suit_without_body.bio_signature")) {
                    player.sendSystemMessage(Component.literal("ECHO-7 // No biological signature inside that suit."));
                }
            }
        }
    }

    public static class StationMotherEntity extends Zombie {
        private final ServerBossEvent boss;
        private int phase = 1;

        public StationMotherEntity(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
            this.boss = new ServerBossEvent(
                    getUUID(),
                    Component.literal("The Station Mother"),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            setPersistenceRequired();
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, 220)
                    .add(Attributes.ATTACK_DAMAGE, 11)
                    .add(Attributes.MOVEMENT_SPEED, 0.25)
                    .add(Attributes.ARMOR, 14)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.85);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide()) {
                return;
            }
            updatePhase();
            if (tickCount % 45 == 0 && getTarget() instanceof Player player) {
                if (StationfallCooldown.ready(player, "entity.pulse.global", GLOBAL_ENTITY_PULSE_COOLDOWN_TICKS)
                        && StationfallCooldown.ready(player, "entity.pulse.station_mother", 45)) {
                    pulse(player, phase >= 3 ? 8 : 3 + phase, phase >= 3 ? 10 : phase, phase == 4 ? 16 : 7 + phase);
                }
            }
            if (level() instanceof ServerLevel serverLevel) {
                if (phase == 1 && tickCount % 90 == 0) {
                    summon(serverLevel, MAINTENANCE_DRONE.get());
                } else if (phase == 2 && tickCount % 80 == 0) {
                    summon(serverLevel, random.nextBoolean() ? HOLLOW_CREWMAN.get() : SCREAMING_SIGNAL.get());
                } else if (phase == 3 && tickCount % 70 == 0) {
                    StationfallStationState.get(serverLevel)
                            .setPower(StationSection.COMMAND_MODULE, StationPowerState.OVERLOADED);
                    summon(serverLevel, MEDICAL_HUSK.get());
                } else if (phase == 4 && tickCount % 65 == 0) {
                    summon(serverLevel, random.nextBoolean() ? SUIT_WITHOUT_BODY.get() : SCREAMING_SIGNAL.get());
                    if (getTarget() instanceof Player player) {
                        if (StationfallCooldown.ready(player, "message.entity.station_mother.preservation", ENTITY_MESSAGE_COOLDOWN_TICKS)) {
                            player.sendSystemMessage(Component.literal(
                                    "STATION MOTHER // Crew preservation successful. Biological movement was not required."
                            ));
                        }
                    }
                }
            }
        }

        @Override
        public void die(DamageSource damageSource) {
            if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
                List<ServerPlayer> players = serverLevel.players().stream()
                        .filter(player -> player.distanceToSqr(this) < 4096)
                        .toList();
                if (players.isEmpty() && damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
                    players = List.of(serverPlayer);
                }
                completeEncounter(serverLevel, players);
            }
            boss.removeAllPlayers();
            super.die(damageSource);
        }

        @Override
        protected void customServerAiStep(ServerLevel level) {
            super.customServerAiStep(level);
            boss.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        }

        @Override
        public void startSeenByPlayer(ServerPlayer player) {
            super.startSeenByPlayer(player);
            boss.addPlayer(player);
        }

        @Override
        public void stopSeenByPlayer(ServerPlayer player) {
            super.stopSeenByPlayer(player);
            boss.removePlayer(player);
        }

        public int phase() {
            return phase;
        }

        public static boolean grantRewards(ServerPlayer player) {
            return completeEncounter(player.level(), List.of(player));
        }

        public static boolean completeEncounter(ServerLevel level, List<ServerPlayer> players) {
            StationfallStationState station = StationfallStationState.get(level);
            station.defeatBoss();
            boolean any = false;
            for (ServerPlayer player : players) {
                StationfallProgress progress = StationfallProgress.get(player);
                if (!progress.blackboxRetrieved()) {
                    give(player, new ItemStack(ModItems.STATIONFALL_BLACKBOX.get()));
                    give(player, new ItemStack(ModItems.AI_OVERRIDE_CORE.get()));
                    give(player, new ItemStack(ModItems.ORBITAL_MEMORY_FRAGMENT.get()));
                    progress.markBlackboxRetrieved(player);
                    any = true;
                    player.sendSystemMessage(Component.literal(
                            "ECHO-7 // They did not die when the station fell. They were uploaded before impact."
                    ));
                } else {
                    progress.markBossDefeated(player);
                    player.sendSystemMessage(Component.literal(
                            "ECHO-7 // Station Mother already defeated. Blackbox reward remains logged."
                    ));
                }
            }
            station.markBlackboxRewarded();
            return any;
        }

        private void updatePhase() {
            float ratio = getHealth() / getMaxHealth();
            int next = ratio <= 0.25F ? 4 : ratio <= 0.5F ? 3 : ratio <= 0.75F ? 2 : 1;
            if (next != phase) {
                phase = next;
                if (getTarget() instanceof Player player) {
                    String line = switch (phase) {
                        case 2 -> "STATION MOTHER // Crew Memory protocol opening.";
                        case 3 -> "STATION MOTHER // Hull breach is an acceptable preservation cost.";
                        case 4 -> "STATION MOTHER // Mother Signal active. Do not resist upload.";
                        default -> "STATION MOTHER // Emergency Protocol active.";
                    };
                    player.sendSystemMessage(Component.literal(line));
                }
            }
        }

        private void summon(ServerLevel level, EntityType<? extends Mob> type) {
            Entity entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (entity instanceof Mob mob) {
                mob.setPos(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4);
                if (getTarget() instanceof LivingEntity target) {
                    mob.setTarget(target);
                }
                level.addFreshEntity(mob);
            }
        }
    }
}
