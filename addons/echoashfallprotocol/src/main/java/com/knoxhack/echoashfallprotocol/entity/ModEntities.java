package com.knoxhack.echoashfallprotocol.entity;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity;
import com.knoxhack.echoashfallprotocol.entity.boss.WardenBossEntity;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.Function;

/**
 * Entity registry for ECHO: ASHFALL PROTOCOL custom mobs.
 */
public class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(BuiltInRegistries.ENTITY_TYPE, EchoAshfallProtocol.MODID);

    public static final EchoBackendRegistryEntry<EntityType<RadZombie>> RAD_ZOMBIE =
            registerEntityType("rad_zombie", RadZombie::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<ScavengerBandit>> SCAVENGER_BANDIT =
            registerEntityType("scavenger_bandit", ScavengerBandit::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<IrradiatedWolf>> IRRADIATED_WOLF =
            registerEntityType("irradiated_wolf", IrradiatedWolf::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 0.85F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<EchoDrone>> ECHO_DRONE =
            registerEntityType("echo_drone", EchoDrone::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<ScoutDrone>> SCOUT_DRONE =
            registerEntityType("scout_drone", ScoutDrone::new, MobCategory.MISC,
                    builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(64).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<EchoCompanionDrone>> ECHO_COMPANION_DRONE =
            registerEntityType("echo_companion_drone", EchoCompanionDrone::new, MobCategory.MISC,
                    builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(64).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<GlowingGhoul>> GLOWING_GHOUL =
            registerEntityType("glowing_ghoul", GlowingGhoul::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<AshWraith>> ASH_WRAITH =
            registerEntityType("ash_wraith", AshWraith::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.9F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<ToxicSlime>> TOXIC_SLIME =
            registerEntityType("toxic_slime", ToxicSlime::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.8F, 0.8F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<CityStalker>> CITY_STALKER =
            registerEntityType("city_stalker", CityStalker::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.8F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<RustWalker>> RUST_WALKER =
            registerEntityType("rust_walker", RustWalker::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.0F, 2.2F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<SteamWraith>> STEAM_WRAITH =
            registerEntityType("steam_wraith", SteamWraith::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.9F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<MutatedCrawler>> MUTATED_CRAWLER =
            registerEntityType("mutated_crawler", MutatedCrawler::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.5F, 0.7F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<WildDog>> WILD_DOG =
            registerEntityType("wild_dog", WildDog::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.65F, 0.9F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<FeralHuman>> FERAL_HUMAN =
            registerEntityType("feral_human", FeralHuman::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<CrashSurvivor>> CRASH_SURVIVOR =
            registerEntityType("crash_survivor", CrashSurvivor::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64));

    public static final EchoBackendRegistryEntry<EntityType<FactionNpcEntity>> FACTION_NPC =
            registerEntityType("faction_npc", FactionNpcEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64));

    // Nexus Warfront pressure mobs
    public static final EchoBackendRegistryEntry<EntityType<NexusPressureMobEntity>> GRIDBOUND_HUSK =
            registerEntityType("gridbound_husk", NexusPressureMobEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(80));

    public static final EchoBackendRegistryEntry<EntityType<NexusPressureMobEntity>> RELAY_WARDEN =
            registerEntityType("relay_warden", NexusPressureMobEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.9F, 2.4F).clientTrackingRange(88).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<NexusPressureMobEntity>> SIGNAL_LEECH =
            registerEntityType("signal_leech", NexusPressureMobEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.55F, 1.45F).clientTrackingRange(80));

    public static final EchoBackendRegistryEntry<EntityType<NexusPressureMobEntity>> NEXUS_NULLIFIER =
            registerEntityType("nexus_nullifier", NexusPressureMobEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.8F, 2.3F).clientTrackingRange(96).fireImmune());

    // Boss Entities
    public static final EchoBackendRegistryEntry<EntityType<WardenBossEntity>> WARDEN_BOSS =
            registerEntityType("warden_boss", WardenBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.5F, 3.0F).clientTrackingRange(128).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> WASTELAND_SENTINEL =
            registerEntityType("wasteland_sentinel", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.0F, 2.5F).clientTrackingRange(96));

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> CRASH_ZONE_COLOSSUS =
            registerEntityType("crash_zone_colossus", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.2F, 2.8F).clientTrackingRange(96).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> CRYOGENIC_OVERSEER =
            registerEntityType("cryogenic_overseer", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.0F, 2.6F).clientTrackingRange(96).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> INDUSTRIAL_JUGGERNAUT =
            registerEntityType("industrial_juggernaut", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.3F, 2.9F).clientTrackingRange(96).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> NEXUS_SCAR_AVATAR =
            registerEntityType("nexus_scar_avatar", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.1F, 2.8F).clientTrackingRange(112).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> RADIATION_BEHEMOTH =
            registerEntityType("radiation_behemoth", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.3F, 3.0F).clientTrackingRange(96).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> CITY_RUIN_STALKER =
            registerEntityType("city_ruin_stalker", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.9F, 2.4F).clientTrackingRange(96));

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> PLAINS_WARLORD =
            registerEntityType("plains_warlord", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.0F, 2.5F).clientTrackingRange(96));

    public static final EchoBackendRegistryEntry<EntityType<BiomeBossEntity>> TOXIC_HIVE_MATRIARCH =
            registerEntityType("toxic_hive_matriarch", BiomeBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.2F, 2.5F).clientTrackingRange(96));

    public static final EchoBackendRegistryEntry<EntityType<NexusFinalBossEntity>> CORRUPTION_BLOOM =
            registerEntityType("corruption_bloom", NexusFinalBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.35F, 2.9F).clientTrackingRange(128).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<NexusFinalBossEntity>> SEVERANCE_ENGINE =
            registerEntityType("severance_engine", NexusFinalBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.45F, 3.0F).clientTrackingRange(128).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<NexusFinalBossEntity>> MIRROR_COMMAND =
            registerEntityType("mirror_command", NexusFinalBossEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.25F, 2.85F).clientTrackingRange(128).fireImmune());

    private static <T extends Entity> EchoBackendRegistryEntry<EntityType<T>> registerEntityType(
            String id,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            Function<EntityType.Builder<T>, EntityType.Builder<T>> builderCustomizer) {
        return EchoBackendEntityBridge.registerEntityType(ENTITIES, id, factory, category, builderCustomizer);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, RAD_ZOMBIE.get(), RadZombie.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SCAVENGER_BANDIT.get(), ScavengerBandit.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, IRRADIATED_WOLF.get(), IrradiatedWolf.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ECHO_DRONE.get(), EchoDrone.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SCOUT_DRONE.get(), ScoutDrone.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ECHO_COMPANION_DRONE.get(), EchoCompanionDrone.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, GLOWING_GHOUL.get(), GlowingGhoul.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ASH_WRAITH.get(), AshWraith.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, TOXIC_SLIME.get(), ToxicSlime.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, GRIDBOUND_HUSK.get(), NexusPressureMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, RELAY_WARDEN.get(), NexusPressureMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SIGNAL_LEECH.get(), NexusPressureMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, NEXUS_NULLIFIER.get(), NexusPressureMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CITY_STALKER.get(), CityStalker.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, RUST_WALKER.get(), RustWalker.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, STEAM_WRAITH.get(), SteamWraith.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, MUTATED_CRAWLER.get(), MutatedCrawler.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, WILD_DOG.get(), WildDog.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, FERAL_HUMAN.get(), FeralHuman.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CRASH_SURVIVOR.get(), CrashSurvivor.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, FACTION_NPC.get(), FactionNpcEntity.createAttributes().build());
        
        // Boss Entities
        EchoBackendEntityBridge.putAttributes(event, WARDEN_BOSS.get(), WardenBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, WASTELAND_SENTINEL.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CRASH_ZONE_COLOSSUS.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CRYOGENIC_OVERSEER.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, INDUSTRIAL_JUGGERNAUT.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, NEXUS_SCAR_AVATAR.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, RADIATION_BEHEMOTH.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CITY_RUIN_STALKER.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, PLAINS_WARLORD.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, TOXIC_HIVE_MATRIARCH.get(), BiomeBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CORRUPTION_BLOOM.get(), NexusFinalBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SEVERANCE_ENGINE.get(), NexusFinalBossEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, MIRROR_COMMAND.get(), NexusFinalBossEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(Object event) {
        registerMonsterSpawn(event, RAD_ZOMBIE);
        registerMonsterSpawn(event, SCAVENGER_BANDIT);
        registerMonsterSpawn(event, IRRADIATED_WOLF);
        registerMonsterSpawn(event, ECHO_DRONE);
        registerMonsterSpawn(event, GLOWING_GHOUL);
        registerMonsterSpawn(event, ASH_WRAITH);
        registerMonsterSpawn(event, TOXIC_SLIME);
        registerMonsterSpawn(event, GRIDBOUND_HUSK);
        registerMonsterSpawn(event, RELAY_WARDEN);
        registerMonsterSpawn(event, SIGNAL_LEECH);
        registerMonsterSpawn(event, NEXUS_NULLIFIER);
        registerMonsterSpawn(event, CITY_STALKER);
        registerMonsterSpawn(event, RUST_WALKER);
        registerMonsterSpawn(event, STEAM_WRAITH);
        registerMonsterSpawn(event, MUTATED_CRAWLER);
        registerMonsterSpawn(event, WILD_DOG);
        registerMonsterSpawn(event, FERAL_HUMAN);
        registerGroundMobSpawn(event, FACTION_NPC);
        registerGroundMobSpawn(event, WARDEN_BOSS);
        registerGroundMobSpawn(event, WASTELAND_SENTINEL);
        registerGroundMobSpawn(event, CRASH_ZONE_COLOSSUS);
        registerGroundMobSpawn(event, CRYOGENIC_OVERSEER);
        registerGroundMobSpawn(event, INDUSTRIAL_JUGGERNAUT);
        registerGroundMobSpawn(event, NEXUS_SCAR_AVATAR);
        registerGroundMobSpawn(event, RADIATION_BEHEMOTH);
        registerGroundMobSpawn(event, CITY_RUIN_STALKER);
        registerGroundMobSpawn(event, PLAINS_WARLORD);
        registerGroundMobSpawn(event, TOXIC_HIVE_MATRIARCH);
        registerGroundMobSpawn(event, CORRUPTION_BLOOM);
        registerGroundMobSpawn(event, SEVERANCE_ENGINE);
        registerGroundMobSpawn(event, MIRROR_COMMAND);

        registerGroundMobSpawn(event, CRASH_SURVIVOR);
        registerNoRestrictionSpawn(event, SCOUT_DRONE);
        registerNoRestrictionSpawn(event, ECHO_COMPANION_DRONE);
    }

    private static <T extends Monster> void registerMonsterSpawn(
            Object event,
            EchoBackendRegistryEntry<EntityType<T>> entity
    ) {
        EchoBackendEntityBridge.registerSpawnPlacement(
                event,
                entity,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
        );
    }

    private static <T extends Mob> void registerGroundMobSpawn(
            Object event,
            EchoBackendRegistryEntry<EntityType<T>> entity
    ) {
        EchoBackendEntityBridge.registerSpawnPlacement(
                event,
                entity,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Mob.checkMobSpawnRules(type, level, reason, pos, random)
        );
    }

    private static <T extends Mob> void registerNoRestrictionSpawn(
            Object event,
            EchoBackendRegistryEntry<EntityType<T>> entity
    ) {
        EchoBackendEntityBridge.registerSpawnPlacement(
                event,
                entity,
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> true
        );
    }
}
