package com.knoxhack.echoorbitalremnants.test;

import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity;
import com.knoxhack.echoorbitalremnants.entity.AbandonedCaptainEntity;
import com.knoxhack.echoorbitalremnants.entity.CorruptedDockingAiEntity;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echoorbitalremnants.entity.EchoZeroEntity;
import com.knoxhack.echoorbitalremnants.entity.EuropaCryoWardenEntity;
import com.knoxhack.echoorbitalremnants.entity.OrbitalFactionNpcEntity;
import com.knoxhack.echoorbitalremnants.faction.OrbitalFactionDialogueService;
import com.knoxhack.echoorbitalremnants.faction.OrbitalOutpostSpawner;
import com.knoxhack.echoorbitalremnants.faction.OrbitalOutpostProfiles;
import com.knoxhack.echoorbitalremnants.integration.AshfallCompat;
import com.knoxhack.echoorbitalremnants.integration.OrbitalFactions;
import com.knoxhack.echoorbitalremnants.integration.OrbitalMachineCoreAdapter;
import com.knoxhack.echoorbitalremnants.integration.OrbitalMachineCoreRuntimeProvider;
import com.knoxhack.echoorbitalremnants.integration.OrbitalMissionProvider;
import com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalIds;
import com.knoxhack.echoorbitalremnants.item.FactionPledgeItem;
import com.knoxhack.echoorbitalremnants.menu.OrbitalMachineMenu;
import com.knoxhack.echoorbitalremnants.network.EchoTerminalSnapshot;
import com.knoxhack.echoorbitalremnants.network.OrbitalFactionNpcActionPayload;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.progression.FactionStanding;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.progression.ModAdvancements;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import com.knoxhack.echoorbitalremnants.world.EuropaCryoOcean;
import com.knoxhack.echoorbitalremnants.world.GroundRecoverySite;
import com.knoxhack.echoorbitalremnants.world.GroundRecoverySiteType;
import com.knoxhack.echoorbitalremnants.world.GroundRecoverySites;
import com.knoxhack.echoorbitalremnants.world.LunarScarZone;
import com.knoxhack.echoorbitalremnants.world.MarsAshBasin;
import com.knoxhack.echoorbitalremnants.world.NexusAnomalyBelt;
import com.knoxhack.echoorbitalremnants.world.OrbitalDebrisField;
import com.knoxhack.echoorbitalremnants.world.RouteTerrainGenerator;
import com.echoplatform.echocore.api.EchoChapterCapability;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoPackMode;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final String ACTION_SCAN = "scan";
    private static final String ACTION_CLAIM_CACHE = "claim_cache";
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoOrbitalRemnants.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_PROCESSING =
            TEST_FUNCTIONS.register("machine_processing", () -> ModGameTests::machineProcessing);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAUNCH_READINESS =
            TEST_FUNCTIONS.register("launch_readiness", () -> ModGameTests::launchReadiness);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROCKET_ASSEMBLY =
            TEST_FUNCTIONS.register("rocket_assembly", () -> ModGameTests::rocketAssembly);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_PROGRESS =
            TEST_FUNCTIONS.register("terminal_progress", () -> ModGameTests::terminalProgress);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_GATES =
            TEST_FUNCTIONS.register("route_gates", () -> ModGameTests::routeGates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SUIT_STATE =
            TEST_FUNCTIONS.register("suit_state", () -> ModGameTests::suitState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTION_REWARD =
            TEST_FUNCTIONS.register("faction_reward", () -> ModGameTests::factionReward);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FINAL_PROTOCOL =
            TEST_FUNCTIONS.register("final_protocol", () -> ModGameTests::finalProtocol);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_CACHES =
            TEST_FUNCTIONS.register("route_caches", () -> ModGameTests::routeCaches);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_TERRAIN =
            TEST_FUNCTIONS.register("route_terrain", () -> ModGameTests::routeTerrain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SURVEY_PROGRESS =
            TEST_FUNCTIONS.register("survey_progress", () -> ModGameTests::surveyProgress);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SURVEY_HARDENING =
            TEST_FUNCTIONS.register("survey_hardening", () -> ModGameTests::surveyHardening);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_SITE_FAMILIES =
            TEST_FUNCTIONS.register("route_site_families", () -> ModGameTests::routeSiteFamilies);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_SURVIVAL_CHAIN =
            TEST_FUNCTIONS.register("beta_survival_chain", () -> ModGameTests::betaSurvivalChain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_TERMINAL_GUIDANCE =
            TEST_FUNCTIONS.register("beta_terminal_guidance", () -> ModGameTests::betaTerminalGuidance);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_CACHE_SUPPORT =
            TEST_FUNCTIONS.register("beta_cache_support", () -> ModGameTests::betaCacheSupport);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EARLY_GROUND_RECOVERY =
            TEST_FUNCTIONS.register("early_ground_recovery", () -> ModGameTests::earlyGroundRecovery);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EARLY_LAUNCH_TO_ORBIT =
            TEST_FUNCTIONS.register("early_launch_to_orbit", () -> ModGameTests::earlyLaunchToOrbit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROCKET_VEHICLE_PLACEMENT =
            TEST_FUNCTIONS.register("rocket_vehicle_placement", () -> ModGameTests::rocketVehiclePlacement);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROCKET_COUNTDOWN_ABORT =
            TEST_FUNCTIONS.register("rocket_countdown_abort", () -> ModGameTests::rocketCountdownAbort);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROCKET_IGNITION_COMMIT =
            TEST_FUNCTIONS.register("rocket_ignition_commit", () -> ModGameTests::rocketIgnitionCommit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROCKET_DAMAGE_DROP =
            TEST_FUNCTIONS.register("rocket_damage_drop", () -> ModGameTests::rocketDamageDrop);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_FACTION_CONTRACTS =
            TEST_FUNCTIONS.register("beta_faction_contracts", () -> ModGameTests::betaFactionContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_RADWARDEN_CONTRACT =
            TEST_FUNCTIONS.register("beta_radwarden_contract", () -> ModGameTests::betaRadwardenContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_CRASHBREAK_CONTRACT =
            TEST_FUNCTIONS.register("beta_crashbreak_contract", () -> ModGameTests::betaCrashbreakContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_SPOREBOUND_CONTRACT =
            TEST_FUNCTIONS.register("beta_sporebound_contract", () -> ModGameTests::betaSporeboundContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_NPC_REGISTRATION =
            TEST_FUNCTIONS.register("outpost_npc_registration", () -> ModGameTests::outpostNpcRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_CHARTER_GATING =
            TEST_FUNCTIONS.register("outpost_charter_gating", () -> ModGameTests::outpostCharterGating);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_LEGACY_MIGRATION =
            TEST_FUNCTIONS.register("outpost_legacy_migration", () -> ModGameTests::outpostLegacyMigration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_ACTION_VALIDATION =
            TEST_FUNCTIONS.register("outpost_action_validation", () -> ModGameTests::outpostActionValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_NPC_PERSISTENCE =
            TEST_FUNCTIONS.register("outpost_npc_persistence", () -> ModGameTests::outpostNpcPersistence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_SPAWN_CAPS =
            TEST_FUNCTIONS.register("outpost_spawn_caps", () -> ModGameTests::outpostSpawnCaps);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OUTPOST_BARTER_BEHAVIOR =
            TEST_FUNCTIONS.register("outpost_barter_behavior", () -> ModGameTests::outpostBarterBehavior);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MID_GAME_OBJECTIVE_CHAIN =
            TEST_FUNCTIONS.register("mid_game_objective_chain", () -> ModGameTests::midGameObjectiveChain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MID_GAME_ROUTE_GATES =
            TEST_FUNCTIONS.register("mid_game_route_gates", () -> ModGameTests::midGameRouteGates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MID_GAME_RECIPES_AND_SITES =
            TEST_FUNCTIONS.register("mid_game_recipes_and_sites", () -> ModGameTests::midGameRecipesAndSites);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EUROPA_CRYO_WARDEN =
            TEST_FUNCTIONS.register("europa_cryo_warden", () -> ModGameTests::europaCryoWarden);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOSS_IDENTITY =
            TEST_FUNCTIONS.register("boss_identity", () -> ModGameTests::bossIdentity);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_CACHE_STATE =
            TEST_FUNCTIONS.register("terminal_mission_cache_state", () -> ModGameTests::terminalMissionCacheState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_INTEGRATION =
            TEST_FUNCTIONS.register("terminal_mission_integration", () -> ModGameTests::terminalMissionIntegration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_INTEGRATION_CONTRACT =
            TEST_FUNCTIONS.register("core_integration_contract", () -> ModGameTests::coreIntegrationContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_LORE_TAXONOMY =
            TEST_FUNCTIONS.register("terminal_lore_taxonomy", () -> ModGameTests::terminalLoreTaxonomy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BETA_RC_POLISH =
            TEST_FUNCTIONS.register("beta_rc_polish", () -> ModGameTests::betaRcPolish);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASSET_COMPLETENESS =
            TEST_FUNCTIONS.register("asset_completeness", () -> ModGameTests::assetCompleteness);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_BREAK_DROPS_INVENTORY =
            TEST_FUNCTIONS.register("machine_break_drops_inventory", () -> ModGameTests::machineBreakDropsInventory);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROCKET_ASSEMBLY_VIRTUAL_OUTPUT =
            TEST_FUNCTIONS.register("rocket_assembly_virtual_output", () -> ModGameTests::rocketAssemblyVirtualOutput);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_ARRIVAL_SEED_ONCE =
            TEST_FUNCTIONS.register("route_arrival_seed_once", () -> ModGameTests::routeArrivalSeedOnce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HIGH_ALTITUDE_SCAN_GATE =
            TEST_FUNCTIONS.register("high_altitude_scan_gate", () -> ModGameTests::highAltitudeScanGate);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AMBIENT_THREAT_CAP =
            TEST_FUNCTIONS.register("ambient_threat_cap", () -> ModGameTests::ambientThreatCap);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRICT_PLAYABLE_PATH =
            TEST_FUNCTIONS.register("strict_playable_path", () -> ModGameTests::strictPlayablePath);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESS_PERSISTENCE_ROUND_TRIP =
            TEST_FUNCTIONS.register("progress_persistence_round_trip", () -> ModGameTests::progressPersistenceRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_RUNTIME =
            TEST_FUNCTIONS.register("orbital_machinecore_runtime_snapshot_contract",
                    () -> ModGameTests::orbitalMachineCoreRuntimeSnapshotContract);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("release_v1"));
        register(event, environment, "machine_processing", MACHINE_PROCESSING.getId());
        register(event, environment, "launch_readiness", LAUNCH_READINESS.getId());
        register(event, environment, "rocket_assembly", ROCKET_ASSEMBLY.getId());
        register(event, environment, "terminal_progress", TERMINAL_PROGRESS.getId());
        register(event, environment, "route_gates", ROUTE_GATES.getId());
        register(event, environment, "suit_state", SUIT_STATE.getId());
        register(event, environment, "faction_reward", FACTION_REWARD.getId());
        register(event, environment, "final_protocol", FINAL_PROTOCOL.getId());
        register(event, environment, "route_caches", ROUTE_CACHES.getId());
        register(event, environment, "route_terrain", ROUTE_TERRAIN.getId());
        register(event, environment, "survey_progress", SURVEY_PROGRESS.getId());
        register(event, environment, "survey_hardening", SURVEY_HARDENING.getId());
        register(event, environment, "route_site_families", ROUTE_SITE_FAMILIES.getId());
        register(event, environment, "beta_survival_chain", BETA_SURVIVAL_CHAIN.getId());
        register(event, environment, "beta_terminal_guidance", BETA_TERMINAL_GUIDANCE.getId());
        register(event, environment, "beta_cache_support", BETA_CACHE_SUPPORT.getId());
        register(event, environment, "early_ground_recovery", EARLY_GROUND_RECOVERY.getId());
        register(event, environment, "early_launch_to_orbit", EARLY_LAUNCH_TO_ORBIT.getId());
        register(event, environment, "rocket_vehicle_placement", ROCKET_VEHICLE_PLACEMENT.getId());
        register(event, environment, "rocket_countdown_abort", ROCKET_COUNTDOWN_ABORT.getId());
        register(event, environment, "rocket_ignition_commit", ROCKET_IGNITION_COMMIT.getId());
        register(event, environment, "rocket_damage_drop", ROCKET_DAMAGE_DROP.getId());
        register(event, environment, "beta_faction_contracts", BETA_FACTION_CONTRACTS.getId());
        register(event, environment, "beta_radwarden_contract", BETA_RADWARDEN_CONTRACT.getId());
        register(event, environment, "beta_crashbreak_contract", BETA_CRASHBREAK_CONTRACT.getId());
        register(event, environment, "beta_sporebound_contract", BETA_SPOREBOUND_CONTRACT.getId());
        register(event, environment, "outpost_npc_registration", OUTPOST_NPC_REGISTRATION.getId());
        register(event, environment, "outpost_charter_gating", OUTPOST_CHARTER_GATING.getId());
        register(event, environment, "outpost_legacy_migration", OUTPOST_LEGACY_MIGRATION.getId());
        register(event, environment, "outpost_action_validation", OUTPOST_ACTION_VALIDATION.getId());
        register(event, environment, "outpost_npc_persistence", OUTPOST_NPC_PERSISTENCE.getId());
        register(event, environment, "outpost_spawn_caps", OUTPOST_SPAWN_CAPS.getId());
        register(event, environment, "outpost_barter_behavior", OUTPOST_BARTER_BEHAVIOR.getId());
        register(event, environment, "mid_game_objective_chain", MID_GAME_OBJECTIVE_CHAIN.getId());
        register(event, environment, "mid_game_route_gates", MID_GAME_ROUTE_GATES.getId());
        register(event, environment, "mid_game_recipes_and_sites", MID_GAME_RECIPES_AND_SITES.getId());
        register(event, environment, "europa_cryo_warden", EUROPA_CRYO_WARDEN.getId());
        register(event, environment, "boss_identity", BOSS_IDENTITY.getId());
        register(event, environment, "terminal_mission_cache_state", TERMINAL_MISSION_CACHE_STATE.getId());
        register(event, environment, "terminal_mission_integration", TERMINAL_MISSION_INTEGRATION.getId());
        register(event, environment, "core_integration_contract", CORE_INTEGRATION_CONTRACT.getId());
        register(event, environment, "terminal_lore_taxonomy", TERMINAL_LORE_TAXONOMY.getId());
        register(event, environment, "beta_rc_polish", BETA_RC_POLISH.getId());
        register(event, environment, "asset_completeness", ASSET_COMPLETENESS.getId());
        register(event, environment, "machine_break_drops_inventory", MACHINE_BREAK_DROPS_INVENTORY.getId());
        register(event, environment, "rocket_assembly_virtual_output", ROCKET_ASSEMBLY_VIRTUAL_OUTPUT.getId());
        register(event, environment, "route_arrival_seed_once", ROUTE_ARRIVAL_SEED_ONCE.getId());
        register(event, environment, "high_altitude_scan_gate", HIGH_ALTITUDE_SCAN_GATE.getId());
        register(event, environment, "ambient_threat_cap", AMBIENT_THREAT_CAP.getId());
        register(event, environment, "strict_playable_path", STRICT_PLAYABLE_PATH.getId());
        register(event, environment, "progress_persistence_round_trip", PROGRESS_PERSISTENCE_ROUND_TRIP.getId());
        register(event, environment, "orbital_machinecore_runtime_snapshot_contract", MACHINECORE_RUNTIME.getId());
    }

    private static void orbitalMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        BlockPos compressorPos = new BlockPos(1, 1, 1);
        helper.setBlock(compressorPos, ModBlocks.OXYGEN_COMPRESSOR.get());
        OrbitalMachineBlockEntity compressor = helper.getBlockEntity(compressorPos, OrbitalMachineBlockEntity.class);
        compressor.setItem(OrbitalMachineBlockEntity.INPUT_SLOT, new ItemStack(Items.GLASS_BOTTLE, 2));
        OrbitalMachineBlockEntity.tick(helper.getLevel(), compressor.getBlockPos(), compressor.getBlockState(), compressor);

        EchoMachineRuntimeSnapshot direct = OrbitalMachineCoreAdapter.runtimeSnapshot(compressor);
        helper.assertTrue(direct.id().value().equals(EchoOrbitalRemnants.id("oxygen_compressor").toString())
                        && direct.ownerModule().value().equals(EchoOrbitalRemnants.MODID)
                        && direct.kind() == EchoMachineKind.SINGLE_BLOCK
                        && direct.state() == EchoMachineState.ACTIVE,
                "Orbital MachineCore adapter should publish the placed Oxygen Compressor identity and live state");
        helper.assertTrue(direct.energy().stored() > 0 && direct.energy().capacity() == 100
                        && "charge".equals(direct.energy().unit()),
                "Orbital MachineCore snapshot should map live internal machine charge");
        helper.assertTrue(direct.inventory().totalSlots() == 2
                        && direct.inventory().occupiedSlots() == 1
                        && direct.inventory().slots().get(OrbitalMachineBlockEntity.INPUT_SLOT).role().equals("input"),
                "Orbital MachineCore snapshot should expose input/output machine slots");
        helper.assertTrue("echoorbitalremnants:machine_emergency_oxygen_cell".equals(direct.process().recipeContract())
                        && direct.process().progressTicks() > 0
                        && direct.process().maxProgressTicks() > 0
                        && "15".equals(direct.process().attributes().get("recipeChargeCost")),
                "Orbital MachineCore snapshot should expose the live oxygen compressor recipe and process state");
        helper.assertTrue(direct.savedState().persistedKeys().contains("Items")
                        && direct.savedState().persistedKeys().contains("progress")
                        && direct.savedState().persistedKeys().contains("charge")
                        && EchoMachineUiBridge.hasAutomationSurface(direct)
                        && EchoMachineUiBridge.position(direct).filter(compressor.getBlockPos()::equals).isPresent(),
                "Orbital MachineCore snapshot should expose saved-state keys plus UI bridge side and position metadata");

        OrbitalMachineCoreRuntimeProvider.register();
        EchoMachineRuntimeSnapshot registered = EchoMachineRuntimeRegistry.snapshot(helper.getLevel(), compressor.getBlockPos())
                .orElse(null);
        helper.assertTrue(registered != null && registered.id().value().equals(direct.id().value())
                        && EchoMachineUiBridge.energyLine(registered).endsWith(" charge"),
                "MachineCore registry should discover the placed Orbital Oxygen Compressor through its runtime provider");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(compressor.getBlockPos().above().getCenter());
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> snapshot.id().value().equals(direct.id().value())
                                && EchoMachineUiBridge.hasAutomationSurface(snapshot)),
                "MachineCore player snapshots should include the nearby Orbital machine");
        List<EchoMachineProfile> profiles = EchoMachineRuntimeRegistry.profiles(player);
        helper.assertTrue(profiles.stream().anyMatch(profile -> profile.id().value().equals(direct.id().value())
                        && profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                                && "echoorbitalremnants:machine_emergency_oxygen_cell".equals(binding.recipeId().value()))),
                "MachineCore profiles should expose the Orbital oxygen compressor recipe binding");
        helper.succeed();
    }

    private static void machineProcessing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.OXYGEN_COMPRESSOR.get());
        OrbitalMachineBlockEntity machine = helper.getBlockEntity(pos, OrbitalMachineBlockEntity.class);
        machine.setItem(OrbitalMachineBlockEntity.INPUT_SLOT, new ItemStack(Items.GLASS_BOTTLE));
        for (int i = 0; i < 140; i++) {
            OrbitalMachineBlockEntity.tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
        }
        helper.assertTrue(machine.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT).is(ModItems.EMERGENCY_OXYGEN_CELL.get()),
                "Oxygen compressor should process glass bottles into emergency oxygen cells");

        BlockPos reclaimerPos = new BlockPos(3, 1, 1);
        helper.setBlock(reclaimerPos, ModBlocks.SOLAR_RECLAIMER.get());
        OrbitalMachineBlockEntity reclaimer = helper.getBlockEntity(reclaimerPos, OrbitalMachineBlockEntity.class);
        reclaimer.setItem(OrbitalMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.VACUUM_CIRCUIT.get()));
        for (int i = 0; i < 180; i++) {
            OrbitalMachineBlockEntity.tick(helper.getLevel(), reclaimer.getBlockPos(), reclaimer.getBlockState(), reclaimer);
        }
        helper.assertTrue(reclaimer.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT).is(ModItems.NAVIGATION_CHIP.get()),
                "Solar Reclaimer should process Vacuum Circuits into Navigation Chips for late-route crafting");

        BlockPos analyzerPos = new BlockPos(5, 1, 1);
        helper.setBlock(analyzerPos, ModBlocks.SIGNAL_ANALYZER.get());
        OrbitalMachineBlockEntity analyzer = helper.getBlockEntity(analyzerPos, OrbitalMachineBlockEntity.class);
        analyzer.setItem(OrbitalMachineBlockEntity.INPUT_SLOT, new ItemStack(ModBlocks.BROKEN_SOLAR_PANEL.get()));
        for (int i = 0; i < 220; i++) {
            OrbitalMachineBlockEntity.tick(helper.getLevel(), analyzer.getBlockPos(), analyzer.getBlockState(), analyzer);
        }
        helper.assertTrue(analyzer.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT).is(ModItems.ORBIT_SURVEY_DATA.get()),
                "Signal Analyzer should process salvage into Orbit Survey Data");
        helper.succeed();
    }

    private static void launchReadiness(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(LaunchReadiness.evaluateForLaunch(player).ready(), "Launch readiness should reject an unprepared player");
        placeLaunchComplex(helper, player, new BlockPos(6, 1, 6));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        player.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));
        helper.assertTrue(LaunchReadiness.evaluateForLaunch(player).ready(), "Launch readiness should accept suit, oxygen, and infrastructure");
        helper.succeed();
    }

    private static void rocketAssembly(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos framePos = placeLaunchComplex(helper, player, new BlockPos(6, 1, 6));
        giveAssemblyParts(player);
        OrbitalMachineBlockEntity frame = helper.getBlockEntity(framePos, OrbitalMachineBlockEntity.class);
        OrbitalMachineMenu menu = new OrbitalMachineMenu(1, player.getInventory(), frame, frame.data());
        menu.broadcastChanges();
        helper.assertTrue(frame.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT).is(ModItems.EMERGENCY_ROCKET.get()),
                "Rocket Assembly Frame should offer an Emergency Rocket when all parts are ready");
        menu.quickMoveStack(player, OrbitalMachineBlockEntity.OUTPUT_SLOT);
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.EMERGENCY_ROCKET.get())),
                "Taking the assembly output should grant an Emergency Rocket");
        helper.assertTrue(count(player.getInventory(), ModItems.FUEL_TANK.get()) == 0,
                "Taking the rocket should consume the required parts");
        helper.succeed();
    }

    private static void machineBreakDropsInventory(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.OXYGEN_COMPRESSOR.get());
        OrbitalMachineBlockEntity machine = helper.getBlockEntity(pos, OrbitalMachineBlockEntity.class);
        machine.setItem(OrbitalMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.VACUUM_CIRCUIT.get(), 2));
        machine.setItem(OrbitalMachineBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.NAVIGATION_CHIP.get()));

        BlockPos absolute = helper.absolutePos(pos);
        helper.getLevel().destroyBlock(absolute, false);
        helper.assertTrue(countItemDropsNear(helper, absolute, ModItems.VACUUM_CIRCUIT.get()) >= 2,
                "Breaking a machine should drop stored input instead of deleting it");
        helper.assertTrue(countItemDropsNear(helper, absolute, ModItems.NAVIGATION_CHIP.get()) >= 1,
                "Breaking a machine should drop stored output instead of deleting it");
        helper.succeed();
    }

    private static void rocketAssemblyVirtualOutput(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos framePos = placeLaunchComplex(helper, player, new BlockPos(6, 1, 6));
        giveAssemblyParts(player);
        OrbitalMachineBlockEntity frame = helper.getBlockEntity(framePos, OrbitalMachineBlockEntity.class);
        OrbitalMachineMenu menu = new OrbitalMachineMenu(1, player.getInventory(), frame, frame.data());
        menu.broadcastChanges();
        helper.assertTrue(frame.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT).is(ModItems.EMERGENCY_ROCKET.get()),
                "Rocket Assembly Frame should expose a virtual Emergency Rocket output");
        CompoundTag savedFrame = frame.saveWithFullMetadata(helper.getLevel().registryAccess());
        String rocketId = BuiltInRegistries.ITEM.getKey(ModItems.EMERGENCY_ROCKET.get()).toString();
        helper.assertFalse(savedFrame.toString().contains(rocketId),
                "Rocket Assembly Frame should not persist the virtual Emergency Rocket output as inventory");
        int rocketsBeforeBlockedTake = count(player.getInventory(), ModItems.EMERGENCY_ROCKET.get());
        removeOne(player.getInventory(), ModItems.ROCKET_NOSE_CONE.get());
        helper.assertFalse(menu.getSlot(OrbitalMachineBlockEntity.OUTPUT_SLOT).mayPickup(player),
                "Rocket Assembly Frame should re-check readiness before granting the virtual output");
        helper.assertTrue(menu.quickMoveStack(player, OrbitalMachineBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Shift-clicking a stale virtual rocket output should be blocked when readiness is no longer true");
        helper.assertTrue(count(player.getInventory(), ModItems.EMERGENCY_ROCKET.get()) == rocketsBeforeBlockedTake,
                "Blocked stale virtual output shift-click should not grant a rocket");

        BlockPos absolute = helper.absolutePos(framePos);
        int dropsBefore = countItemDropsNear(helper, absolute, ModItems.EMERGENCY_ROCKET.get());
        helper.getLevel().destroyBlock(absolute, false);
        int dropsAfter = countItemDropsNear(helper, absolute, ModItems.EMERGENCY_ROCKET.get());
        helper.assertTrue(dropsAfter == dropsBefore,
                "Breaking a Rocket Assembly Frame should not drop its virtual Emergency Rocket output");
        helper.succeed();
    }

    private static void routeArrivalSeedOnce(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        EchoTerminalProgress.reset(player);
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MARS_TRANSFER_WINDOW.get()));

        InteractionResult first = ModItems.MARS_TRANSFER_WINDOW.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(first == InteractionResult.SUCCESS_SERVER, "First route burn should succeed");
        helper.assertTrue(EchoTerminalProgress.get(player).hasRouteArrivalSeeded("mars_ash_basin"),
                "First Mars route burn should persist the arrival seed token");
        net.minecraft.server.level.ServerLevel routeLevel = (net.minecraft.server.level.ServerLevel) player.level();
        BlockPos firstArrival = player.blockPosition();
        int chestsAfterFirst = countChestsNear(routeLevel, firstArrival, 18);
        int threatsAfterFirst = countOrbitalThreatsNear(routeLevel, firstArrival, 24.0D);

        InteractionResult second = ModItems.MARS_TRANSFER_WINDOW.get().use(routeLevel, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(second == InteractionResult.SUCCESS_SERVER, "Repeated route burn should remain usable");
        helper.assertTrue(countChestsNear(routeLevel, firstArrival, 18) == chestsAfterFirst,
                "Repeated route burn should not reseed the first-arrival cache");
        helper.assertTrue(countOrbitalThreatsNear(routeLevel, firstArrival, 24.0D) == threatsAfterFirst,
                "Repeated route burn should not duplicate the first-arrival threat wave");
        helper.succeed();
    }

    private static void highAltitudeScanGate(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        player.getInventory().add(new ItemStack(ModBlocks.STATION_LIFE_SUPPORT_CORE.get()));

        helper.assertTrue(SuitEvents.isOrbitalExposure(player), "High overworld altitude should still count as survival exposure");
        helper.assertFalse(SuitEvents.isOrbitalProgressionScan(player),
                "High overworld altitude should not count as orbital progression before a real launch");
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertFalse(progress.lowOrbitReached(),
                "High-altitude overworld scan should not mark Low Earth Orbit before launch");
        helper.assertFalse(progress.stationLifeSupportRestored(),
                "High-altitude overworld scan should not restore station state before launch");
        helper.succeed();
    }

    private static void ambientThreatCap(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        for (int i = 0; i < 6; i++) {
            Entity entity = ModEntities.ECHO_DEFENSE_DRONE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(entity != null, "Ambient threat should be spawnable for cap testing");
            entity.setPos(player.getX() + i, player.getY(), player.getZ());
            helper.getLevel().addFreshEntity(entity);
        }

        int before = countOrbitalThreatsNear(helper.getLevel(), player, 18.0D);
        helper.assertTrue(SuitEvents.localThreatCapReached(helper.getLevel(), player),
                "Six nearby orbital threats should trip the local ambient cap");
        try {
            Method method = SuitEvents.class.getDeclaredMethod("spawnFeatureThreat",
                    net.minecraft.server.level.ServerLevel.class,
                    net.minecraft.world.entity.player.Player.class);
            method.setAccessible(true);
            method.invoke(null, helper.getLevel(), player);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Unable to invoke feature threat spawn for cap test", exception);
        }
        helper.assertTrue(countOrbitalThreatsNear(helper.getLevel(), player, 18.0D) == before,
                "Ambient threat cap should prevent another nearby feature threat spawn");
        helper.succeed();
    }

    private static void strictPlayablePath(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        EchoTerminalProgress.reset(player);
        markAshfallNexusChoice(helper, player);

        BlockPos scanOrigin = helper.absolutePos(new BlockPos(20, 10, 70));
        player.teleportTo(helper.getLevel(), scanOrigin.getX() + 0.5D, scanOrigin.getY(), scanOrigin.getZ() + 0.5D,
                Set.of(), player.getYRot(), player.getXRot(), false);
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.hasGroundRecoverySites(), "Playable path should begin with ECHO-7 Earth recovery calibration");

        for (GroundRecoverySite site : progress.groundRecoverySites()) {
            player.teleportTo(helper.getLevel(), site.pos().getX() + 0.5D, site.pos().getY(), site.pos().getZ() + 0.5D,
                    Set.of(), player.getYRot(), player.getXRot(), false);
            com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        }
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.allGroundRecoverySitesComplete(), "Playable path should complete all tracked Earth recovery sites");
        String recoveredObjective = EchoTerminalSnapshot.from(player).nextObjective();
        helper.assertTrue(recoveredObjective.contains("launch") || recoveredObjective.contains("Rocket")
                        || recoveredObjective.contains("stage"),
                "Terminal should guide a recovered player into launch construction or rocket staging");

        BlockPos framePos = placeLaunchComplex(helper, player, new BlockPos(6, 1, 6));
        giveAssemblyParts(player);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        player.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));

        OrbitalMachineBlockEntity frame = helper.getBlockEntity(framePos, OrbitalMachineBlockEntity.class);
        OrbitalMachineMenu menu = new OrbitalMachineMenu(1, player.getInventory(), frame, frame.data());
        menu.broadcastChanges();
        helper.assertTrue(frame.getItem(OrbitalMachineBlockEntity.OUTPUT_SLOT).is(ModItems.EMERGENCY_ROCKET.get()),
                "Rocket Assembly Frame should expose the craftable Emergency Rocket during the playable path");
        int rocketsBeforeAssembly = count(player.getInventory(), ModItems.EMERGENCY_ROCKET.get());
        ItemStack assemblyTake = menu.quickMoveStack(player, OrbitalMachineBlockEntity.OUTPUT_SLOT);
        helper.assertFalse(assemblyTake.isEmpty(), "Playable path should be able to take the assembled Emergency Rocket");
        helper.assertTrue(count(player.getInventory(), ModItems.EMERGENCY_ROCKET.get()) == rocketsBeforeAssembly + 1,
                "Assembling should grant exactly one Emergency Rocket");
        helper.assertTrue(count(player.getInventory(), ModItems.FUEL_TANK.get()) == 0,
                "Assembling should consume the real rocket parts instead of duplicating them");
        helper.assertTrue(moveOneToHand(player, ModItems.EMERGENCY_ROCKET.get()),
                "Playable path should move the assembled rocket into the player's hand for staging");

        InteractionResult stageResult = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(stageResult == InteractionResult.SUCCESS_SERVER, "Prepared Emergency Rocket staging should succeed");
        EmergencyRocketEntity rocket = firstRocketNear(helper.getLevel(), player);
        helper.assertTrue(rocket != null, "Staging should place one launch vehicle on the pad");
        helper.assertTrue(countRocketsNear(helper.getLevel(), player) == 1,
                "Staging should create exactly one launch vehicle in the playable path");
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getVehicle() == rocket, "Playable path should allow boarding the staged rocket");
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(rocket.launchState() == EmergencyRocketEntity.LaunchState.COUNTDOWN,
                "Playable path should start the launch countdown from the cabin");
        for (int i = 0; i < EmergencyRocketEntity.COUNTDOWN_TICKS + EmergencyRocketEntity.ASCENT_TICKS + 10; i++) {
            rocket.tick();
        }

        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.lowOrbitReached(), "Completed launch should mark Low Earth Orbit reached");
        helper.assertTrue(progress.hasEarthReturnPoint(), "Completed launch should save a safe Earth return vector");
        helper.assertTrue(progress.hasRouteArrivalSeeded("low_earth_orbit"),
                "First launch should seed Low Earth Orbit arrival support only once");

        player.getInventory().add(new ItemStack(ModBlocks.STATION_LIFE_SUPPORT_CORE.get()));
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.stationLifeSupportRestored(), "Orbit scan should restore station life support when the player has the core");
        helper.assertTrue(progress.lunarSignalUnlocked(), "Station life support scan should unlock the Lunar Signal");
        helper.assertTrue(EchoTerminalSnapshot.from(player).scanReport().contains("Station life support"),
                "Terminal feedback should explain the successful station scan");

        progress.repairStationRelay(player, "strict:station:1");
        progress = EchoTerminalProgress.get(player);
        progress.repairStationRelay(player, "strict:station:2");
        progress = EchoTerminalProgress.get(player);
        progress.repairStationRelay(player, "strict:station:3");
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.stationNetworkGateOpen(), "Station relay repairs should open the shuttle route objective gate");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ORBITAL_SHUTTLE.get()));
        InteractionResult shuttleResult = ModItems.ORBITAL_SHUTTLE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(shuttleResult == InteractionResult.SUCCESS_SERVER, "Orbital Shuttle should burn to the Lunar route");
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.lunarSignalInvestigated(), "Shuttle burn should persist Lunar Signal investigation");
        helper.assertTrue(progress.hasReturnPoint(), "Shuttle burn should save a route return vector");
        helper.assertTrue(progress.hasRouteArrivalSeeded("lunar_scar_zone"),
                "First Lunar arrival should persist its arrival seed token");

        progress.repairLunarExtractor(player, "strict:lunar:1");
        progress = EchoTerminalProgress.get(player);
        progress.repairLunarExtractor(player, "strict:lunar:2");
        progress = EchoTerminalProgress.get(player);
        progress.repairLunarExtractor(player, "strict:lunar:3");
        player.getInventory().add(new ItemStack(ModItems.HELIUM_3_CELL.get()));
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.marsRouteUnlocked(), "Scanning Lunar Helium-3 after extractor repairs should unlock Mars");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MARS_TRANSFER_WINDOW.get()));
        InteractionResult marsResult = ModItems.MARS_TRANSFER_WINDOW.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(marsResult == InteractionResult.SUCCESS_SERVER, "Mars Transfer Window should burn after route unlock");
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.marsAshBasinVisited(), "Mars route burn should persist Mars arrival progress");
        helper.assertTrue(progress.hasRouteArrivalSeeded("mars_ash_basin"),
                "First Mars arrival should persist its arrival seed token");
        EchoTerminalSnapshot routeSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(routeSnapshot.marsVisited() && routeSnapshot.routeReturnSaved(),
                "Terminal snapshot should expose route state and the saved return vector");

        player.setShiftKeyDown(true);
        InteractionResult returnResult = ModItems.MARS_TRANSFER_WINDOW.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertTrue(returnResult == InteractionResult.SUCCESS_SERVER, "Sneak-using a route item should execute the saved return vector");
        helper.assertTrue(EchoTerminalSnapshot.from(player).missionHelp().contains("ECHO NOTE"),
                "Terminal should continue to provide next-step guidance after the playable route loop");
        helper.succeed();
    }

    private static void progressPersistenceRoundTrip(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.setEarthReturnPoint(player, 10.0D, 64.0D, -10.0D, "minecraft:overworld");
        progress.setReturnPoint(player, 20.0D, 96.0D, -20.0D, "echoorbitalremnants:lunar_scar_zone");
        progress.markLowOrbitReached(player);
        progress.restoreStationLifeSupport(player);
        progress.markRouteArrivalSeeded(player, "low_earth_orbit");
        progress.markRouteArrivalSeeded(player, "lunar_scar_zone");
        progress.markRouteArrivalSeeded(player, "mars_ash_basin");
        progress.markTerminalMissionCacheClaimed(player, "strict_cache");

        progress = EchoTerminalProgress.get(player);
        for (int i = 1; i <= 3; i++) {
            progress.recordOrbitSurvey(player, "persist:orbit:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordMoonSurvey(player, "persist:moon:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordMarsSurvey(player, "persist:mars:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordEuropaSurvey(player, "persist:europa:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordSaturnSurvey(player, "persist:saturn:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordTitanSurvey(player, "persist:titan:" + i);
            progress = EchoTerminalProgress.get(player);
        }
        progress.markEchoZeroEncountered(player);
        progress = EchoTerminalProgress.get(player);
        for (int i = 1; i <= 3; i++) {
            progress.recordNexusStabilization(player, "persist:nexus:" + i);
            progress = EchoTerminalProgress.get(player);
        }

        for (int i = 1; i <= 3; i++) {
            progress.repairStationRelay(player, "persist:station:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.repairLunarExtractor(player, "persist:lunar:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.repairMarsPressureConsole(player, "persist:mars_pressure:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.repairEuropaThermalArray(player, "persist:europa_array:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.repairSaturnRingRelay(player, "persist:saturn_relay:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.repairTitanMethanePump(player, "persist:titan_pump:" + i);
            progress = EchoTerminalProgress.get(player);
        }

        completeAllOutpostCharters(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.finalNetworkSealed(), "Pre-save state should be seal-complete before reload simulation");

        CompoundTag saved = player.getPersistentData().getCompoundOrEmpty("echoorbitalremnants_progress").copy();
        EchoTerminalProgress.reset(player);
        helper.assertFalse(EchoTerminalProgress.get(player).hasReturnPoint(),
                "Reset should clear route vectors before restoring saved progress data");
        player.getPersistentData().put("echoorbitalremnants_progress", saved.copy());
        EchoTerminalProgress loaded = EchoTerminalProgress.get(player);

        helper.assertTrue(loaded.lowOrbitReached(), "Reloaded progress should retain Low Earth Orbit completion");
        helper.assertTrue(loaded.hasEarthReturnPoint() && loaded.earthReturnX() == 10.0D,
                "Reloaded progress should retain the Earth return vector");
        helper.assertTrue(loaded.hasReturnPoint() && loaded.returnDimension().equals("echoorbitalremnants:lunar_scar_zone"),
                "Reloaded progress should retain the route return vector");
        helper.assertTrue(loaded.hasRouteArrivalSeeded("low_earth_orbit")
                        && loaded.hasRouteArrivalSeeded("lunar_scar_zone")
                        && loaded.hasRouteArrivalSeeded("mars_ash_basin"),
                "Reloaded progress should retain route arrival seed tokens");
        helper.assertTrue(loaded.hasTerminalMissionCacheClaimed("strict_cache"),
                "Reloaded progress should retain once-only Terminal cache claims");
        helper.assertTrue(loaded.allSurveysComplete() && loaded.totalSurveyCount() == 21,
                "Reloaded progress should retain all survey and Nexus stabilization state");
        helper.assertTrue(loaded.stationNetworkGateOpen() && loaded.lunarExtractorGateOpen()
                        && loaded.marsHabitatGateOpen() && loaded.europaArrayGateOpen()
                        && loaded.saturnRelayGateOpen() && loaded.titanPumpGateOpen(),
                "Reloaded progress should retain all route repair gates");
        helper.assertTrue(loaded.completedOutpostCharterCount() == 3,
                "Reloaded progress should retain completed outpost charter count");
        helper.assertTrue(loaded.finalNetworkSealed(),
                "Reloaded progress should retain final seal state");
        helper.assertTrue(loaded.lastTerminalReport().contains("Orbital Remnants arc complete"),
                "Reloaded final state should retain Terminal completion feedback");
        helper.succeed();
    }

    private static void terminalProgress(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.markOrbitalContact(player);
        progress.markLowOrbitReached(player);
        player.getInventory().add(new ItemStack(ModBlocks.STATION_LIFE_SUPPORT_CORE.get()));
        player.setPos(1.5D, Config.ORBITAL_ALTITUDE.get(), 1.5D);
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        EchoTerminalSnapshot snapshot = EchoTerminalSnapshot.from(player);
        progress.unlockMarsRoute(player);
        progress.unlockEuropaRoute(player);
        progress.unlockDeepSpaceProtocol(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.lowOrbitReached(), "Low orbit flag should persist");
        helper.assertTrue(progress.lunarSignalUnlocked(), "Terminal scan should detect a nearby Station Life Support Core");
        helper.assertTrue(snapshot.scanReport().contains("Station life support"), "Terminal snapshot should expose scan feedback");
        helper.assertTrue(!snapshot.scanRequirement().isBlank(), "Terminal snapshot should expose the next scan requirement");
        helper.assertTrue(progress.marsRouteUnlocked(), "Mars route should persist");
        helper.assertTrue(progress.europaRouteUnlocked(), "Europa route should persist");
        helper.assertTrue(progress.deepSpaceProtocolUnlocked(), "Deep Space Protocol should persist");
        helper.succeed();
    }

    private static void routeGates(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MARS_TRANSFER_WINDOW.get()));
        InteractionResult locked = ModItems.MARS_TRANSFER_WINDOW.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(locked == InteractionResult.CONSUME, "Mars route item should be blocked before telemetry unlock");
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertFalse(progress.marsAshBasinVisited(), "Locked route use should not mark Mars visited");
        progress.unlockMarsRoute(player);
        helper.assertTrue(progress.marsRouteUnlocked(), "Terminal progress should expose the Mars route gate flag");
        InteractionResult wrongState = ModItems.MARS_TRANSFER_WINDOW.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(wrongState == InteractionResult.CONSUME, "Unlocked Mars route should still require orbital staging");
        helper.assertFalse(progress.marsAshBasinVisited(), "Wrong-state route use should not mark Mars visited");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        InteractionResult noReturn = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(noReturn == InteractionResult.CONSUME, "Emergency Rocket should reject orbital re-entry without an Earth return vector");
        helper.succeed();
    }

    private static void suitState(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        SuitState state = SuitState.get(player);
        state.tickVacuum(player, false, false, true, false);
        state.compromisePressure(20);
        state.addRadiation(false);
        helper.assertTrue(state.oxygen() < 100, "Vacuum exposure without a suit should drain oxygen");
        helper.assertTrue(state.pressure() < 100, "Pressure compromise should reduce pressure");
        helper.assertTrue(state.radiation() > 0, "Radiation should increase");
        helper.succeed();
    }

    private static void factionReward(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pledge = new ItemStack(ModItems.ORBITAL_REMNANT_BADGE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pledge);
        ModItems.ORBITAL_REMNANT_BADGE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.orbitalRemnantStanding() == FactionStanding.ALIGNED,
                "Radwarden pledge should persist aligned standing");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.OXYGEN_BOOSTER.get())),
                "Radwarden pledge should grant oxygen support");
        int boosters = count(player.getInventory(), ModItems.OXYGEN_BOOSTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ORBITAL_REMNANT_BADGE.get()));
        ModItems.ORBITAL_REMNANT_BADGE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(count(player.getInventory(), ModItems.OXYGEN_BOOSTER.get()) == boosters,
                "Repeating the same pledge should not double-grant pledge rewards");
        helper.succeed();
    }

    private static void finalProtocol(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.markAnomalyBeltEntered(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.anomalyBeltEntered(), "Entering Nexus should mark the anomaly route");
        helper.assertFalse(progress.echoZeroEncountered(), "Entering Nexus should not complete ECHO-0");
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            helper.assertFalse(ModAdvancements.hasEchoZeroResolved(serverPlayer), "Nexus entry should not grant the final advancement");
        }
        helper.assertTrue(EchoZeroEntity.completeEncounter(player), "Completing ECHO-0 should report a new final protocol");
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.echoZeroEncountered(), "ECHO-0 completion should persist");
        helper.assertTrue(progress.echoZeroRewardClaimed(), "ECHO-0 reward claim state should persist");
        helper.assertTrue(progress.lastTerminalReport().contains("ECHO-0 resolved"), "Terminal report should describe final completion");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.NEXUS_DRIVE_CORE.get())),
                "Final completion should grant the baseline Nexus reward");
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            helper.assertTrue(ModAdvancements.hasEchoZeroResolved(serverPlayer), "ECHO-0 completion should grant the final advancement");
        }
        int nexusCoreCount = count(player.getInventory(), ModItems.NEXUS_DRIVE_CORE.get());
        helper.assertFalse(EchoZeroEntity.completeEncounter(player), "ECHO-0 final reward should only be granted once");
        helper.assertTrue(count(player.getInventory(), ModItems.NEXUS_DRIVE_CORE.get()) == nexusCoreCount,
                "Repeated ECHO-0 completion checks should not duplicate final rewards");
        helper.succeed();
    }

    private static void routeCaches(GameTestHelper helper) {
        var level = helper.getLevel();
        OrbitalDebrisField.seedArrivalField(level, new BlockPos(20, 10, 20));
        LunarScarZone.seedLandingSite(level, new BlockPos(60, 10, 20));
        MarsAshBasin.seedLandingSite(level, new BlockPos(100, 10, 20));
        EuropaCryoOcean.seedLandingSite(level, new BlockPos(140, 10, 20));
        NexusAnomalyBelt.seedEntrySite(level, new BlockPos(180, 10, 20));

        helper.assertTrue(hasCacheWith(level, new BlockPos(20, 10, 20), ModItems.VACUUM_CIRCUIT.get()),
                "Orbital arrival should place a station salvage cache");
        helper.assertTrue(hasCacheWith(level, new BlockPos(60, 10, 20), ModItems.HELIUM_3_CELL.get()),
                "Lunar arrival should place a Helium-3 cache");
        helper.assertTrue(hasCacheWith(level, new BlockPos(100, 10, 20), ModItems.MARTIAN_SILICA.get()),
                "Mars arrival should place a Martian Silica cache");
        helper.assertTrue(hasCacheWith(level, new BlockPos(140, 10, 20), ModItems.CRYO_CRYSTAL.get()),
                "Europa arrival should place a Cryo resource cache");
        helper.assertTrue(hasCacheWith(level, new BlockPos(180, 10, 20), ModItems.LUNAR_CORE_FRAGMENT.get()),
                "Nexus arrival should place a final anomaly cache");
        assertArrivalCadence(helper, level, new BlockPos(20, 10, 20), RouteTerrainGenerator.Route.ORBIT);
        assertArrivalCadence(helper, level, new BlockPos(60, 10, 20), RouteTerrainGenerator.Route.MOON);
        assertArrivalCadence(helper, level, new BlockPos(100, 10, 20), RouteTerrainGenerator.Route.MARS);
        assertArrivalCadence(helper, level, new BlockPos(140, 10, 20), RouteTerrainGenerator.Route.EUROPA);
        assertArrivalCadence(helper, level, new BlockPos(180, 10, 20), RouteTerrainGenerator.Route.NEXUS);
        helper.succeed();
    }

    private static void routeTerrain(GameTestHelper helper) {
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.ORBIT, 0, 0) > 58,
                "Orbit route terrain should generate debris bands");
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.MOON, 0, 0) > 58,
                "Moon route terrain should generate a cratered surface");
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.MARS, 0, 0) > 58,
                "Mars route terrain should generate ash basin terrain");
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.EUROPA, 0, 0) > 58,
                "Europa route terrain should generate ice shelves");
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.SATURN, 16, 16) > 58,
                "Saturn route terrain should generate ring graveyard platforms");
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.TITAN, 0, 0) > 58,
                "Titan route terrain should generate methane shelf terrain");
        helper.assertTrue(RouteTerrainGenerator.topHeight(RouteTerrainGenerator.Route.NEXUS, 23, 23) > 58,
                "Nexus route terrain should generate anomaly islands");
        helper.assertTrue(RouteTerrainGenerator.landmarkBlock(RouteTerrainGenerator.Route.NEXUS).is(ModBlocks.NEXUS_ANCHOR.get()),
                "Nexus terrain landmarks should expose anchor objective blocks");
        helper.assertTrue(RouteTerrainGenerator.landmarkBlock(RouteTerrainGenerator.Route.SATURN).is(ModBlocks.SATURN_RING_RELAY.get()),
                "Saturn repeatable landmarks should expose Ring Relays");
        helper.assertTrue(RouteTerrainGenerator.landmarkBlock(RouteTerrainGenerator.Route.TITAN).is(ModBlocks.TITAN_METHANE_PUMP.get()),
                "Titan repeatable landmarks should expose Methane Pumps");
        helper.assertTrue(RouteTerrainGenerator.routeObjectiveBlock(RouteTerrainGenerator.Route.ORBIT).is(ModBlocks.STATION_RELAY_NODE.get()),
                "Orbit terrain should expose station relay repair nodes");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.ORBIT).stream().anyMatch(stack -> stack.is(ModItems.ORBIT_SURVEY_DATA.get())),
                "Orbit repeatable landmarks should include route survey data caches");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.NEXUS).stream().anyMatch(stack -> stack.is(ModItems.NEXUS_STABILIZER_SHARD.get())),
                "Nexus repeatable landmarks should include stabilizer shard caches");
        helper.assertTrue(RouteTerrainGenerator.hasDeepSite(RouteTerrainGenerator.Route.ORBIT, 0, 0)
                        || RouteTerrainGenerator.hasDeepSite(RouteTerrainGenerator.Route.ORBIT, 1, 0)
                        || RouteTerrainGenerator.hasDeepSite(RouteTerrainGenerator.Route.ORBIT, 0, 1),
                "Route terrain should deterministically place deep sites near common test chunks");
        helper.succeed();
    }

    private static void surveyProgress(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.recordOrbitSurvey(player, "orbit:site:a");
        EchoTerminalProgress.SurveyResult duplicate = EchoTerminalProgress.get(player).recordOrbitSurvey(player, "orbit:site:a");
        helper.assertTrue(duplicate.duplicate(), "Scanning the same orbit site twice should be rejected");
        EchoTerminalProgress.get(player).recordOrbitSurvey(player, "orbit:site:b");
        helper.assertFalse(EchoTerminalProgress.get(player).orbitSurveyComplete(), "Two unique orbit survey scans should not complete the route");
        EchoTerminalProgress.get(player).recordOrbitSurvey(player, "orbit:site:c");
        helper.assertTrue(EchoTerminalProgress.get(player).orbitSurveyComplete(), "Three orbit survey scans should complete the route");

        EchoTerminalProgress.SurveyResult locked = EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:site:a");
        helper.assertFalse(EchoTerminalProgress.get(player).nexusStabilized(), "Nexus stabilization should stay locked before ECHO-0");
        helper.assertFalse(locked.newlyComplete(), "Locked Nexus stabilization should not report completion");
        EchoTerminalProgress.get(player).markEchoZeroEncountered(player);
        EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:site:a");
        EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:site:b");
        EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:site:c");
        helper.assertTrue(EchoTerminalProgress.get(player).nexusStabilized(), "Nexus stabilization should complete after ECHO-0 and three anchor scans");
        helper.succeed();
    }

    private static void surveyHardening(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        EchoTerminalSnapshot snapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(snapshot.surveyLines().size() >= 9, "Terminal snapshot should expose route objectives plus all route survey rows");
        helper.assertTrue(snapshot.surveyLines().stream().anyMatch(line -> line.contains("Orbit") && line.contains("Signal Relay")),
                "Terminal survey rows should name scan hooks");
        helper.assertTrue(snapshot.surveyLines().stream().anyMatch(line -> line.contains("Station Relay Node")),
                "Terminal survey rows should name mid-game repair hooks");
        boolean analyzerRecipe = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.id().identifier().equals(id("signal_analyzer")));
        helper.assertTrue(analyzerRecipe, "Signal Analyzer should have a survival crafting recipe");
        helper.succeed();
    }

    private static void routeSiteFamilies(GameTestHelper helper) {
        for (RouteTerrainGenerator.Route route : RouteTerrainGenerator.Route.values()) {
            int variants = 0;
            for (int chunkX = -12; chunkX <= 12; chunkX++) {
                for (int chunkZ = -12; chunkZ <= 12; chunkZ++) {
                    int variant = RouteTerrainGenerator.siteVariant(route, chunkX, chunkZ);
                    if (variant >= 0) {
                        variants |= 1 << variant;
                    }
                }
            }
            helper.assertTrue(variants == 0b111, route.getSerializedName() + " should expose all three deep-site variants");
            for (int variant = 0; variant < 3; variant++) {
                String siteName = RouteTerrainGenerator.siteName(route, variant);
                helper.assertTrue(siteName.equals(expectedSiteName(route, variant)),
                        route.getSerializedName() + " variant should keep its documented site name");
                helper.assertFalse(RouteTerrainGenerator.hazardBlock(route, variant).isAir(),
                        route.getSerializedName() + " variant should expose a hazard block");
                helper.assertTrue(RouteTerrainGenerator.cacheItems(route, variant).size() >= 3,
                        route.getSerializedName() + " variant should expose a conservative route cache");
                List<ItemStack> cacheItems = RouteTerrainGenerator.cacheItems(route, variant);
                helper.assertTrue(hasProgressionValue(route, cacheItems),
                        route.getSerializedName() + " beta cache should include route progression value");
                helper.assertTrue(hasCraftingSupport(cacheItems),
                        route.getSerializedName() + " beta cache should include crafting support");
                helper.assertTrue(hasRecoverabilitySupport(cacheItems),
                        route.getSerializedName() + " beta cache should include oxygen or seal support");
                helper.assertTrue(RouteTerrainGenerator.landmarkBlock(route).getBlock() != Blocks.AIR,
                        route.getSerializedName() + " should expose an objective block");
                helper.assertTrue(RouteTerrainGenerator.routeObjectiveBlock(route).getBlock() != Blocks.AIR,
                        route.getSerializedName() + " should expose a repair objective block");
            }
        }

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos featurePos = new BlockPos(1, 1, 1);
        helper.setBlock(featurePos, ModBlocks.BROKEN_SOLAR_PANEL.get());
        BlockPos absoluteFeaturePos = helper.absolutePos(featurePos);
        player.setPos(absoluteFeaturePos.getX() + 0.5D, absoluteFeaturePos.getY(), absoluteFeaturePos.getZ() + 0.5D);
        helper.assertTrue(SuitEvents.isFeatureThreatZone(player), "Feature blocks should mark ambient threat zones");
        String hazardText = EchoTerminalProgress.get(player).localHazardText(player);
        helper.assertTrue(hazardText.contains("Deep-site signal"), "Terminal hazard text should name nearby deep route features");
        helper.succeed();
    }

    private static void betaSurvivalChain(GameTestHelper helper) {
        assertRecipe(helper, "echo_terminal");
        assertRecipe(helper, "echo_terminal_from_transponder");
        assertRecipe(helper, "signal_analyzer");
        assertRecipe(helper, "fuel_refinery");
        assertRecipe(helper, "oxygen_compressor");
        assertRecipe(helper, "rocket_assembly_frame");
        assertRecipe(helper, "launch_platform");
        assertRecipe(helper, "pressurized_helmet");
        assertRecipe(helper, "pressurized_helmet_from_fragments");
        assertRecipe(helper, "pressurized_chestplate");
        assertRecipe(helper, "pressurized_chestplate_from_fragments");
        assertRecipe(helper, "pressurized_leggings");
        assertRecipe(helper, "pressurized_leggings_from_fragments");
        assertRecipe(helper, "magnetic_boots");
        assertRecipe(helper, "magnetic_boots_from_fragments");
        assertRecipe(helper, "oxygen_tank");
        assertRecipe(helper, "navigation_computer_from_chip");
        assertRecipe(helper, "echo_flight_core_from_transponder");
        assertRecipe(helper, "orbital_shuttle");
        assertRecipe(helper, "mars_transfer_window");
        assertRecipe(helper, "europa_transfer_window");
        assertRecipe(helper, "nexus_drive_vessel");
        assertRecipe(helper, "machine_orbit_survey_data");
        assertRecipe(helper, "machine_lunar_core_sample");
        assertRecipe(helper, "machine_martian_pressure_valve");
        assertRecipe(helper, "machine_europa_thermal_probe");
        assertRecipe(helper, "machine_nexus_stabilizer_shard");
        assertRecipe(helper, "machine_station_relay_fuse");
        assertRecipe(helper, "machine_station_power_matrix");
        assertRecipe(helper, "machine_helium_extractor_core");
        assertRecipe(helper, "machine_lunar_pressure_map");
        assertRecipe(helper, "machine_pressure_regulator");
        assertRecipe(helper, "machine_martian_habitat_key");
        assertRecipe(helper, "machine_europa_probe_array");
        assertRecipe(helper, "machine_thermal_stabilizer");
        assertNoRecipe(helper, "emergency_rocket");

        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.ORBIT).stream().anyMatch(stack -> stack.is(ModItems.ORBIT_SURVEY_DATA.get())),
                "Orbit survey data should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.ORBIT).stream().anyMatch(stack -> stack.is(ModItems.STATION_RELAY_FUSE.get())),
                "Station relay fuses should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.MOON).stream().anyMatch(stack -> stack.is(ModItems.LUNAR_CORE_SAMPLE.get())),
                "Lunar core samples should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.MOON).stream().anyMatch(stack -> stack.is(ModItems.HELIUM_EXTRACTOR_CORE.get())),
                "Helium extractor cores should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.MARS).stream().anyMatch(stack -> stack.is(ModItems.MARTIAN_PRESSURE_VALVE.get())),
                "Martian pressure valves should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.MARS).stream().anyMatch(stack -> stack.is(ModItems.PRESSURE_REGULATOR.get())),
                "Pressure regulators should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.EUROPA).stream().anyMatch(stack -> stack.is(ModItems.EUROPA_THERMAL_PROBE.get())),
                "Europa thermal probes should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.EUROPA).stream().anyMatch(stack -> stack.is(ModItems.EUROPA_PROBE_ARRAY.get())),
                "Europa probe arrays should have generated-cache recovery");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.SATURN).stream().anyMatch(stack -> stack.is(ModItems.SATURN_RELAY_LENS.get())),
                "Saturn relay lenses should have generated-cache recovery");
        helper.assertTrue(hasRecoverabilitySupport(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.SATURN)),
                "Saturn generated caches should include route recoverability support");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.TITAN).stream().anyMatch(stack -> stack.is(ModItems.TITAN_METHANE_CELL.get())),
                "Titan methane cells should have generated-cache recovery");
        helper.assertTrue(hasRecoverabilitySupport(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.TITAN)),
                "Titan generated caches should include route recoverability support");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.NEXUS).stream().anyMatch(stack -> stack.is(ModItems.NEXUS_STABILIZER_SHARD.get())),
                "Nexus stabilizer shards should have generated-cache recovery");
        helper.succeed();
    }

    private static void betaTerminalGuidance(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        markAshfallNexusChoice(helper, player);
        EchoTerminalSnapshot fresh = EchoTerminalSnapshot.from(player);
        helper.assertTrue(fresh.nextObjective().contains("Next Step"), "Fresh terminal snapshot should expose a compact next step");
        helper.assertTrue(fresh.nextObjective().contains("calibrate"), "Fresh terminal should direct Earth calibration");

        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.markOrbitalContact(player);
        EchoTerminalSnapshot launchBlocked = EchoTerminalSnapshot.from(player);
        helper.assertTrue(launchBlocked.nextObjective().contains("Missing"), "Blocked launch guidance should summarize missing prep");
        helper.assertFalse(launchBlocked.launchMissing().isEmpty(), "Launch tab should expose missing systems");

        progress.markLowOrbitReached(player);
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        String report = EchoTerminalProgress.get(player).lastTerminalReport();
        helper.assertTrue(report.contains("Station Life Support Core"), "Bad orbit scan should name the missing station hook");

        EchoTerminalProgress.get(player).restoreStationLifeSupport(player);
        EchoTerminalSnapshot midGameBlocked = EchoTerminalSnapshot.from(player);
        helper.assertTrue(midGameBlocked.scanRequirement().contains("(0/3)"),
                "Mid-game terminal guidance should expose the current repair count");
        helper.assertTrue(midGameBlocked.missionHelp().contains("ECHO NOTE"),
                "Mid-game terminal help should use consistent ECHO NOTE wording");

        EchoTerminalProgress.get(player).markLunarSignalInvestigated(player);
        EchoTerminalProgress.get(player).unlockMarsRoute(player);
        EchoTerminalProgress.get(player).markMarsAshBasinVisited(player);
        EchoTerminalProgress.get(player).unlockEuropaRoute(player);
        EchoTerminalProgress.get(player).markEuropaCryoOceanVisited(player);
        EchoTerminalSnapshot europaBlocked = EchoTerminalSnapshot.from(player);
        helper.assertTrue(europaBlocked.nextObjective().contains("Europa Thermal Arrays")
                        && europaBlocked.nextObjective().contains("Saturn"),
                "Europa guidance should clearly name Saturn as the next outer route");
        EchoTerminalProgress.get(player).repairEuropaThermalArray(player, "europa:a");
        EchoTerminalProgress.get(player).repairEuropaThermalArray(player, "europa:b");
        EchoTerminalProgress.get(player).repairEuropaThermalArray(player, "europa:c");
        EchoTerminalSnapshot saturnOpen = EchoTerminalSnapshot.from(player);
        helper.assertTrue(saturnOpen.saturnOpen(), "Terminal snapshot should expose the Saturn route gate");
        helper.assertTrue(saturnOpen.nextObjective().contains("Saturn Transfer Window"),
                "Terminal guidance should tell players to use the Saturn Transfer Window");
        EchoTerminalProgress.get(player).markSaturnRingGraveyardVisited(player);
        EchoTerminalSnapshot saturnVisited = EchoTerminalSnapshot.from(player);
        helper.assertTrue(saturnVisited.saturnVisited(), "Terminal snapshot should expose Saturn visited state");
        helper.assertTrue(saturnVisited.nextObjective().contains("Saturn Ring Relays")
                        && saturnVisited.nextObjective().contains("Titan"),
                "Saturn guidance should clearly name Titan descent");
        EchoTerminalProgress.get(player).repairSaturnRingRelay(player, "saturn:a");
        EchoTerminalProgress.get(player).repairSaturnRingRelay(player, "saturn:b");
        EchoTerminalProgress.get(player).repairSaturnRingRelay(player, "saturn:c");
        EchoTerminalSnapshot titanOpen = EchoTerminalSnapshot.from(player);
        helper.assertTrue(titanOpen.titanOpen(), "Terminal snapshot should expose the Titan route gate");
        helper.assertTrue(titanOpen.nextObjective().contains("Titan Transfer Window"),
                "Terminal guidance should tell players to use the Titan Transfer Window");
        EchoTerminalProgress.get(player).markTitanMethaneShelfVisited(player);
        EchoTerminalSnapshot titanVisited = EchoTerminalSnapshot.from(player);
        helper.assertTrue(titanVisited.titanVisited(), "Terminal snapshot should expose Titan visited state");
        helper.assertTrue(titanVisited.nextObjective().contains("Titan Methane Pumps")
                        && titanVisited.nextObjective().contains("Deep Space Protocol"),
                "Titan guidance should clearly name Deep Space Protocol");
        EchoTerminalProgress.get(player).repairTitanMethanePump(player, "titan:a");
        EchoTerminalProgress.get(player).repairTitanMethanePump(player, "titan:b");
        EchoTerminalProgress.get(player).repairTitanMethanePump(player, "titan:c");
        EchoTerminalProgress.get(player).unlockDeepSpaceProtocol(player);
        EchoTerminalProgress.get(player).markAnomalyBeltEntered(player);
        EchoTerminalSnapshot beforeEchoZero = EchoTerminalSnapshot.from(player);
        helper.assertTrue(beforeEchoZero.nextObjective().contains("ECHO-0"),
                "Finale guidance should name ECHO-0 before Nexus stabilization can begin");
        helper.assertTrue(beforeEchoZero.scanRequirement().contains("ECHO-0"),
                "Scan guidance should name ECHO-0 as the pre-stabilization blocker");

        EchoTerminalProgress.get(player).markEchoZeroEncountered(player);
        EchoTerminalSnapshot afterEchoZero = EchoTerminalSnapshot.from(player);
        helper.assertTrue(afterEchoZero.nextObjective().contains("0/3"),
                "Post-ECHO guidance should expose Nexus stabilization progress");
        helper.assertTrue(afterEchoZero.scanRequirement().contains("Anchor/Growth"),
                "Post-ECHO scan guidance should name Nexus Anchor/Growth sites");
        helper.assertTrue(afterEchoZero.missionHelp().contains("Signal Analyzer"),
                "Post-ECHO help should describe shard recovery when landmarks are hard to find");

        EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:a");
        EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:b");
        EchoTerminalSnapshot partialNexus = EchoTerminalSnapshot.from(player);
        helper.assertTrue(partialNexus.nextObjective().contains("2/3"),
                "Partial Nexus stabilization should report exact progress");

        EchoTerminalProgress.get(player).recordOrbitSurvey(player, "orbit:a");
        EchoTerminalProgress.get(player).recordOrbitSurvey(player, "orbit:b");
        EchoTerminalProgress.get(player).recordOrbitSurvey(player, "orbit:c");
        EchoTerminalProgress.get(player).recordMoonSurvey(player, "moon:a");
        EchoTerminalProgress.get(player).recordMoonSurvey(player, "moon:b");
        EchoTerminalProgress.get(player).recordMoonSurvey(player, "moon:c");
        EchoTerminalProgress.get(player).recordMarsSurvey(player, "mars:a");
        EchoTerminalProgress.get(player).recordMarsSurvey(player, "mars:b");
        EchoTerminalProgress.get(player).recordMarsSurvey(player, "mars:c");
        EchoTerminalProgress.get(player).recordEuropaSurvey(player, "europa:a");
        EchoTerminalProgress.get(player).recordEuropaSurvey(player, "europa:b");
        EchoTerminalProgress.get(player).recordEuropaSurvey(player, "europa:c");
        EchoTerminalProgress.get(player).recordSaturnSurvey(player, "saturn:a");
        EchoTerminalProgress.get(player).recordSaturnSurvey(player, "saturn:b");
        EchoTerminalProgress.get(player).recordSaturnSurvey(player, "saturn:c");
        EchoTerminalProgress.get(player).recordTitanSurvey(player, "titan:a");
        EchoTerminalProgress.get(player).recordTitanSurvey(player, "titan:b");
        EchoTerminalProgress.get(player).recordTitanSurvey(player, "titan:c");
        EchoTerminalProgress.get(player).recordNexusStabilization(player, "nexus:c");
        EchoTerminalSnapshot needsContract = EchoTerminalSnapshot.from(player);
        helper.assertTrue(needsContract.nextObjective().contains("outpost charters"),
                "Complete surveys should point to the required outpost charters");
        helper.assertTrue(needsContract.scanRequirement().contains("Crashbreak NPC outpost"),
                "Missing outpost support should name the first required NPC charter");

        EchoTerminalProgress.get(player).acceptOutpostCharter(player, FactionPledgeItem.Faction.VOID_SALVAGERS);
        EchoTerminalSnapshot activeContract = EchoTerminalSnapshot.from(player);
        helper.assertTrue(activeContract.factionContract().contains("Crashbreak"),
                "Active outpost guidance should expose Crashbreak charter state");
        EchoTerminalProgress.get(player).completeOutpostCharter(player, FactionPledgeItem.Faction.VOID_SALVAGERS);
        EchoTerminalProgress.get(player).acceptOutpostCharter(player, FactionPledgeItem.Faction.ORBITAL_REMNANT);
        EchoTerminalProgress.get(player).completeOutpostCharter(player, FactionPledgeItem.Faction.ORBITAL_REMNANT);
        EchoTerminalProgress.get(player).acceptOutpostCharter(player, FactionPledgeItem.Faction.NEXUS_CHOIR);
        EchoTerminalProgress.get(player).completeOutpostCharter(player, FactionPledgeItem.Faction.NEXUS_CHOIR);
        EchoTerminalSnapshot finalSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(finalSnapshot.nextObjective().contains("Orbital Remnants arc complete"),
                "Final terminal state should clearly name completed surveys and outpost support");
        helper.assertTrue(finalSnapshot.missionHelp().contains("Orbital Remnants arc complete"),
                "Final terminal help should name the completed orbital arc");
        helper.assertTrue(finalSnapshot.scanReport().contains("Orbital Remnants arc complete"),
                "The scan that completes the final outpost charter should emit one final completion report");
        helper.assertTrue(finalSnapshot.finalComplete(),
                "Final terminal state should only set the final-complete flag after the network is sealed");
        int stabilizedCores = count(player.getInventory(), ModItems.STABILIZED_ECHO_CORE.get());
        int nexusDust = count(player.getInventory(), ModItems.NEXUS_DUST.get());
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(count(player.getInventory(), ModItems.STABILIZED_ECHO_CORE.get()) == stabilizedCores,
                "Repeated final scans should not duplicate final seal core rewards");
        helper.assertTrue(count(player.getInventory(), ModItems.NEXUS_DUST.get()) == nexusDust,
                "Repeated final scans should not duplicate final seal dust rewards");
        helper.assertTrue(EchoTerminalProgress.get(player).lastTerminalReport().contains("Orbital Remnants arc complete"),
                "Repeated final scans should refresh completion guidance instead of faction cooldown noise");
        helper.succeed();
    }

    private static void terminalMissionCacheState(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress fresh = EchoTerminalProgress.get(player);
        helper.assertTrue(fresh.claimedTerminalMissionCaches().isBlank(),
                "Old saves should default to no claimed terminal mission caches");
        helper.assertTrue(fresh.markTerminalMissionCacheClaimed(player, "earth_calibration"),
                "First terminal mission cache claim should persist");
        helper.assertFalse(EchoTerminalProgress.get(player).markTerminalMissionCacheClaimed(player, "earth_calibration"),
                "Terminal mission cache claims should be once-only");
        helper.assertTrue(EchoTerminalProgress.get(player).hasTerminalMissionCacheClaimed("earth_calibration"),
                "Claimed terminal mission cache ids should round-trip through Orbital progress");
        helper.succeed();
    }

    private static void terminalMissionIntegration(GameTestHelper helper) {
        try {
            Class<?> commonIntegration = Class.forName(
                    "com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalCommonIntegration");
            commonIntegration.getMethod("register").invoke(null);

            Class<?> providerClass = Class.forName(
                    "com.knoxhack.echoorbitalremnants.integration.OrbitalMissionProvider");
            Object provider = providerClass.getField("INSTANCE").get(null);
            java.lang.reflect.Method missionsMethod = providerClass.getMethod(
                    "missions", net.minecraft.world.entity.player.Player.class);
            java.lang.reflect.Method snapshotMethod = providerClass.getMethod(
                    "snapshot", net.minecraft.world.entity.player.Player.class, Identifier.class);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            EchoTerminalProgress.reset(player);
            markAshfallNexusChoice(helper, player);
            Identifier earthCalibration = id("earth_calibration");
            List<?> missions = (List<?>) missionsMethod.invoke(provider, player);
            helper.assertTrue(missions.size() == 14, "Orbital Terminal provider should expose all planned mission records");
            Object freshSnapshot = snapshotMethod.invoke(provider, player, earthCalibration);
            helper.assertTrue("UNLOCKED".equals(snapshotStatus(freshSnapshot)),
                    "Earth calibration mission should be active before contact");

            EchoTerminalProgress.get(player).markOrbitalContact(player);
            Object readySnapshot = snapshotMethod.invoke(provider, player, earthCalibration);
            helper.assertTrue("CLAIMABLE".equals(snapshotStatus(readySnapshot)),
                    "Completed Orbital mission records should expose a claimable cache");

            EchoTerminalProgress.get(player).markTerminalMissionCacheClaimed(player, "earth_calibration");
            Object claimedSnapshot = snapshotMethod.invoke(provider, player, earthCalibration);
            helper.assertTrue("CLAIMED".equals(snapshotStatus(claimedSnapshot)),
                    "Claimed mission cache status should be visible in the Terminal mission snapshot");

            Class<?> missionRegistry = Class.forName("com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry");
            Object registeredProvider = missionRegistry.getMethod("provider", Identifier.class)
                    .invoke(null, id("orbital_remnants"));
            helper.assertTrue((Boolean) registeredProvider.getClass().getMethod("isPresent").invoke(registeredProvider),
                    "Orbital common integration should register its Terminal mission provider");
            Class<?> actionRegistry = Class.forName("com.knoxhack.echoterminal.api.TerminalActionRegistry");
            java.lang.reflect.Field handlersField = actionRegistry.getDeclaredField("HANDLERS");
            handlersField.setAccessible(true);
            Map<?, ?> handlers = (Map<?, ?>) handlersField.get(null);
            helper.assertTrue(hasTerminalActionHandler(handlers, id("orbital"), id("scan")),
                    "Orbital command tab should register a server-side SCAN action");
            helper.assertTrue(hasTerminalActionHandler(handlers, id("orbital_survey"), id("scan")),
                    "Orbital survey tab should register a server-side SCAN action");
            helper.assertTrue(hasTerminalActionHandler(handlers, id("orbital_echo"),
                            Identifier.fromNamespaceAndPath("echoterminal", "mission_action")),
                    "Orbital ECHO tab should register the shared Terminal mission action");

            var routePlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            EchoTerminalProgress.reset(routePlayer);
            markAshfallNexusChoice(helper, routePlayer);
            List<TerminalMissionDefinition> typedMissions = OrbitalMissionProvider.INSTANCE.missions(routePlayer);
            assertOrbitalRoutePlacement(helper, routePlayer, typedMissions, "earth_calibration", 10, 0);
            assertOrbitalRoutePlacement(helper, routePlayer, typedMissions, "station_network", 10, 101);
            assertOrbitalRoutePlacement(helper, routePlayer, typedMissions, "mars_route", 13, 300);
            assertOrbitalRoutePlacement(helper, routePlayer, typedMissions, "deep_space_protocol", 13, 700);
            assertOrbitalRoutePlacement(helper, routePlayer, typedMissions, "echo_zero", 14, 701);
            assertOrbitalRoutePlacement(helper, routePlayer, typedMissions, "final_seal", 15, 900);

            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            List<TerminalMissionDefinition> survivalRoute = MainSurvivalQuestProvider.INSTANCE.missions(routePlayer);
            assertSurvivalRoutePhase(helper, survivalRoute, "earth_calibration", 10);
            assertSurvivalRoutePhase(helper, survivalRoute, "station_network", 10);
            assertSurvivalRoutePhase(helper, survivalRoute, "mars_route", 13);
            assertSurvivalRoutePhase(helper, survivalRoute, "deep_space_protocol", 13);
            assertSurvivalRoutePhase(helper, survivalRoute, "echo_zero", 14);
            assertSurvivalRoutePhase(helper, survivalRoute, "survey_network", 15);
            assertSurvivalRoutePhase(helper, survivalRoute, "faction_contract", 15);
            assertSurvivalRoutePhase(helper, survivalRoute, "final_seal", 15);

            TerminalMissionSnapshot survivalSnapshot =
                    MainSurvivalQuestProvider.INSTANCE.snapshot(routePlayer, id("launch_chain"));
            helper.assertTrue(survivalSnapshot.actions().stream().anyMatch(action -> ACTION_SCAN.equals(action.id())),
                    "Survival Route should preserve Orbital child scan actions");
            EchoTerminalProgress.get(routePlayer).markLowOrbitReached(routePlayer);
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            TerminalMissionSnapshot survivalClaimable =
                    MainSurvivalQuestProvider.INSTANCE.snapshot(routePlayer, id("low_orbit"));
            helper.assertTrue(survivalClaimable.actions().stream()
                            .anyMatch(action -> action.enabled() && ACTION_CLAIM_CACHE.equals(action.id())),
                    "Survival Route should preserve Orbital child claim-cache actions");
            helper.succeed();
        } catch (ClassNotFoundException missingTerminal) {
            helper.succeed();
        } catch (ReflectiveOperationException | RuntimeException error) {
            helper.assertTrue(false, "Terminal mission integration reflection failed: " + error.getMessage());
        }
    }

    private static void coreIntegrationContract(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        AshfallCompat.registerAddonChapter();

        boolean ashfallLoaded = ModList.get().isLoaded("echoashfallprotocol");
        TerminalMissionSnapshot freshMission = OrbitalMissionProvider.INSTANCE.snapshot(player, id("earth_calibration"));
        if (ashfallLoaded) {
            helper.assertTrue(AshfallCompat.isOrbitalCalibrationLocked(player),
                    "Ashfall-loaded Orbital should lock Earth calibration before a Nexus choice");
            helper.assertFalse(AshfallCompat.hasPostNexusChoice(player),
                    "Fresh Ashfall player data should not count as post-Nexus");
            helper.assertTrue(freshMission.status() == TerminalMissionStatus.LOCKED,
                    "Shared Terminal mission should mirror the Ashfall handoff lock");
            helper.assertTrue(hasDiagnostic(EchoCoreServices.diagnostics(player), "orbital_ashfall_handoff_locked"),
                    "Core diagnostics should publish the Ashfall handoff blocker");
            markAshfallNexusChoice(helper, player);
        } else {
            helper.assertFalse(AshfallCompat.isOrbitalCalibrationLocked(player),
                    "Standalone Orbital should not lock Earth calibration on missing Ashfall data");
            helper.assertTrue(AshfallCompat.hasPostNexusChoice(player),
                    "Standalone Orbital should expose a recovered handoff to Core availability checks");
            helper.assertTrue(EchoCoreServices.packMode(player) == EchoPackMode.ORBITAL_STANDALONE,
                    "Current Orbital GameTest runtime should resolve as Orbital standalone");
            helper.assertTrue(freshMission.status() == TerminalMissionStatus.UNLOCKED,
                    "Standalone Earth calibration should be immediately available");
        }

        helper.assertFalse(AshfallCompat.isOrbitalCalibrationLocked(player),
                "Orbital calibration should be unlocked after the active handoff condition is satisfied");
        helper.assertTrue(AshfallCompat.hasPostNexusChoice(player),
                "Core-facing Orbital availability should be true after the handoff condition is satisfied");

        EchoChapterCapability capability = EchoCoreServices.chapterCapabilities(player).stream()
                .filter(entry -> "orbital_remnants".equals(entry.id().getPath()))
                .findFirst()
                .orElse(null);
        helper.assertTrue(capability != null && capability.installed(),
                "ECHO Core should report Orbital Remnants as an installed chapter");
        helper.assertTrue(capability.available(), "ECHO Core should report Orbital available after handoff");
        helper.assertTrue(capability.statusLine().contains(ashfallLoaded ? "Earth calibration" : "Standalone"),
                "Core chapter status should explain the active Orbital handoff mode");

        String ashfallSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/integration/AshfallCompat.java")
                .replace("\r\n", "\n");
        helper.assertTrue(ashfallSource.contains("PostNexusData")
                        && ashfallSource.contains("getSelectedPath")
                        && ashfallSource.contains("hasMadeChoice"),
                "Ashfall compatibility should keep reading the real post-Nexus data shape");
        helper.assertFalse(ashfallSource.contains("isOrbitalCalibrationLocked(Player player) {\n        return false;"),
                "Ashfall calibration lock should not regress to the old stub");
        helper.assertFalse(ashfallSource.contains("hasPostNexusChoice(Player player) {\n        return player != null;"),
                "Post-Nexus detection should not regress to the old non-null-player stub");

        List<EchoDiagnosticBlocker> freshDiagnostics = EchoCoreServices.diagnostics(player);
        helper.assertTrue(hasDiagnostic(freshDiagnostics, "orbital_calibration_needed"),
                "Core diagnostics should expose the initial Earth calibration objective");
        helper.assertFalse(hasDiagnostic(freshDiagnostics, "orbital_ashfall_handoff_locked"),
                "Ashfall handoff blocker should clear once the handoff condition is satisfied");

        List<EchoRouteRecord> freshRoutes = orbitalRouteRecords(player);
        helper.assertTrue(freshRoutes.size() == 5, "Core should publish all five Orbital route records");
        EchoRouteRecord earthFresh = routeRecord(freshRoutes, "orbital_earth_recontact");
        helper.assertTrue(earthFresh != null && "SCAN REQUIRED".equals(earthFresh.status()) && !earthFresh.complete(),
                "Fresh Core route record should ask for Earth calibration");
        helper.assertTrue(routeRecord(freshRoutes, "orbital_launch_chain") != null,
                "Core route records should include the launch chain");
        EchoRouteRecord stationfallFresh = routeRecord(freshRoutes, "orbital_stationfall_handoff");
        helper.assertTrue(stationfallFresh != null && "LOCKED".equals(stationfallFresh.status()) && !stationfallFresh.complete(),
                "Core route records should include the optional Stationfall handoff before it opens");
        helper.assertTrue(routeRecord(freshRoutes, "orbital_route_worlds") != null,
                "Core route records should include the route-world survey state");
        helper.assertTrue(routeRecord(freshRoutes, "orbital_echo_zero") != null,
                "Core route records should include the ECHO-0 quarantine state");

        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.markOrbitalContact(player);
        TerminalMissionSnapshot calibratedMission = OrbitalMissionProvider.INSTANCE.snapshot(player, id("earth_calibration"));
        helper.assertTrue(calibratedMission.status() == TerminalMissionStatus.CLAIMABLE,
                "Calibrated Earth mission should become claimable in the shared Terminal provider");
        EchoRouteRecord earthCalibrated = routeRecord(orbitalRouteRecords(player), "orbital_earth_recontact");
        helper.assertTrue(earthCalibrated != null && "CALIBRATED".equals(earthCalibrated.status()) && earthCalibrated.complete(),
                "Core Earth route record should mirror launch-site calibration");

        EchoTerminalProgress.get(player).markLowOrbitReached(player);
        TerminalMissionSnapshot lowOrbitMission = OrbitalMissionProvider.INSTANCE.snapshot(player, id("low_orbit"));
        helper.assertTrue(lowOrbitMission.status() == TerminalMissionStatus.CLAIMABLE,
                "Low Orbit mission should become claimable once the launch vector is reached");
        EchoRouteRecord launchComplete = routeRecord(orbitalRouteRecords(player), "orbital_launch_chain");
        helper.assertTrue(launchComplete != null && "COMPLETE".equals(launchComplete.status()) && launchComplete.complete(),
                "Core launch route record should mirror Low Earth Orbit arrival");
        EchoRouteRecord stationfallOpen = routeRecord(orbitalRouteRecords(player), "orbital_stationfall_handoff");
        helper.assertTrue(stationfallOpen != null && "COORDINATES RECOVERED".equals(stationfallOpen.status())
                        && stationfallOpen.complete(),
                "Core Stationfall handoff record should open when Orbital recovers Station ECHO coordinates");

        EchoTerminalProgress.get(player).completeFullArcForQa(player);
        List<EchoRouteRecord> finalRoutes = orbitalRouteRecords(player);
        EchoRouteRecord stationfallReady = routeRecord(finalRoutes, "orbital_stationfall_handoff");
        helper.assertTrue(stationfallReady != null && "BOARDING READY".equals(stationfallReady.status())
                        && stationfallReady.complete(),
                "Core Stationfall handoff record should become boarding-ready when the station network is restored");
        helper.assertTrue(routeRecord(finalRoutes, "orbital_route_worlds").complete(),
                "Core route-world record should complete once all surveys and Nexus stabilization are complete");
        EchoRouteRecord echoZero = routeRecord(finalRoutes, "orbital_echo_zero");
        helper.assertTrue(echoZero != null && "SEALED".equals(echoZero.status()) && echoZero.complete(),
                "Core ECHO-0 route record should mirror the final network seal");
        TerminalMissionSnapshot finalSeal = OrbitalMissionProvider.INSTANCE.snapshot(player, id("final_seal"));
        helper.assertTrue(finalSeal.status() == TerminalMissionStatus.CLAIMABLE,
                "Final seal mission should become claimable after the implemented main loop is complete");

        helper.assertTrue(OrbitalFactions.ORBITAL_REMNANTS.equals(Identifier.fromNamespaceAndPath("echoashfallprotocol", "radwarden_compact")),
                "Radwarden orbital lane should mirror into the Ashfall Radwarden faction id");
        helper.assertTrue(OrbitalFactions.VOID_SALVAGERS.equals(Identifier.fromNamespaceAndPath("echoashfallprotocol", "crashbreak_salvage")),
                "Crashbreak orbital lane should mirror into the Ashfall Crashbreak faction id");
        helper.assertTrue(OrbitalFactions.NEXUS_CHOIR.equals(Identifier.fromNamespaceAndPath("echoashfallprotocol", "sporebound_sanctum")),
                "Sporebound orbital lane should mirror into the Ashfall Sporebound faction id");
        helper.assertTrue(EchoCoreServices.factionDefinitions().stream()
                        .filter(definition -> EchoOrbitalRemnants.MODID.equals(definition.id().getNamespace()))
                        .count() == 0,
                "Orbital should mirror standing into Ashfall factions without registering Orbital faction definitions");
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(player);
        helper.assertTrue(telemetry.exposure() >= 85 && telemetry.statusLine().contains("Orbital exposure"),
                "Core hazard telemetry should expose active Orbital suit exposure");
        helper.succeed();
    }

    private static void terminalLoreTaxonomy(GameTestHelper helper) {
        helper.assertTrue("ECHO-0 ROUTE CHAIN".equals(OrbitalMissionProvider.INSTANCE.chapter().title()),
                "Orbital mission chapter should render as the ECHO-0 route chain");
        assertOrbitalTabChrome(helper,
                "com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalIntegration$OrbitalCommandTab",
                "Orbital Command");
        assertOrbitalTabChrome(helper,
                "com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalIntegration$OrbitalSurveyTab",
                "Route Survey");
        assertOrbitalTabChrome(helper,
                "com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalIntegration$OrbitalEchoTab",
                "ECHO-0 Records");
        assertOrbitalNavigationProfiles(helper);
        helper.succeed();
    }

    private static void betaRcPolish(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.markLowOrbitReached(player);
        progress.markLunarSignalInvestigated(player);
        progress.unlockMarsRoute(player);
        progress.repairLunarExtractor(player, "moon:a");
        progress.repairLunarExtractor(player, "moon:b");
        progress.repairLunarExtractor(player, "moon:c");
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MARS_TRANSFER_WINDOW.get()));
        int routeKeys = count(player.getInventory(), ModItems.MARS_TRANSFER_WINDOW.get()) + player.getMainHandItem().getCount();
        InteractionResult result = ModItems.MARS_TRANSFER_WINDOW.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.SUCCESS_SERVER,
                "Reusable Mars route key should perform the route burn from orbital exposure");
        int routeKeysAfter = count(player.getInventory(), ModItems.MARS_TRANSFER_WINDOW.get()) + player.getMainHandItem().getCount();
        helper.assertTrue(routeKeysAfter == routeKeys,
                "Reusable route keys should not be consumed by successful route burns");

        String nexusSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/item/NexusDriveVesselItem.java");
        helper.assertTrue(nexusSource.contains("Europa Thermal Array repairs before Nexus entry can hold")
                        && nexusSource.contains("Saturn Ring Relay repairs before Nexus entry can hold")
                        && nexusSource.contains("Titan Methane Pump repairs before Nexus entry can hold"),
                "Nexus Drive lock copy should frame Europa/Saturn/Titan repairs as Nexus-entry blockers");
        String suitSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/suit/SuitEvents.java");
        helper.assertFalse(suitSource.contains("SATELLITE_PLATING.get()) ? 2 : 2"),
                "Low Orbit satellite-plating hazard tuning should not contain a no-op ternary");
        String terminalSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/item/EchoTerminalItem.java");
        helper.assertTrue(terminalSource.contains("No pledge detected")
                        && terminalSource.contains("already serviced at this hub")
                        && terminalSource.contains("Vendor cache is paused")
                        && terminalSource.contains("faction.vendorCacheReport()"),
                "Faction hub scan copy should distinguish no pledge, serviced, active-contract, and authorized-cache states");
        String pledgeSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/item/FactionPledgeItem.java");
        helper.assertTrue(pledgeSource.contains("Radwarden orbital containment cache authorized")
                        && pledgeSource.contains("Crashbreak orbital salvage cache authorized")
                        && pledgeSource.contains("Sporebound anomaly support cache authorized"),
                "Faction cache reports should use the three-faction Orbital lane vocabulary");
        helper.assertTrue(terminalSource.contains("Cache role confirmed: route proof, crafting support, and survival recovery"),
                "Survey/cache scan feedback should explain why route caches are worth opening");
        String routeItemSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/item/PlanetaryRouteItem.java");
        helper.assertTrue(routeItemSource.contains("sendFeedback(player")
                        && routeItemSource.contains("unlockHint()")
                        && !routeItemSource.contains("Terminal telemetry lacks the handoff proof"),
                "Route item lock feedback should stay action-bar visible and name the next concrete proof");
        String shuttleSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/item/OrbitalShuttleItem.java");
        helper.assertTrue(shuttleSource.contains("playShuttleFeedback")
                        && shuttleSource.contains("sendFeedback(player")
                        && shuttleSource.contains("Restore Station Life Support"),
                "Orbital Shuttle handoff feedback should include particles, action-bar status, and concrete lock guidance");
        String rocketSource = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/item/EmergencyRocketItem.java");
        helper.assertTrue(rocketSource.contains("sendFeedback(player, \"Launch hold.")
                        && rocketSource.contains("readiness.missing().size()"),
                "Rocket staging holds should keep an action-bar summary before detailed missing checks");
        String suitSourceForFeedback = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/suit/SuitEvents.java");
        helper.assertTrue(suitSourceForFeedback.contains("Recovery cache opened: route proof, crafting support, and survival recovery stock")
                        && suitSourceForFeedback.contains("BlockScanCache")
                        && suitSourceForFeedback.contains("pulseNearbyRouteObjective"),
                "Route cache, objective affordance, and cached block scan feedback should remain wired");
        helper.assertTrue(sourceText("src/main/java/com/knoxhack/echoorbitalremnants/entity/CorruptedDockingAiEntity.java").contains("First contact: Corrupted Docking AI")
                        && sourceText("src/main/java/com/knoxhack/echoorbitalremnants/entity/EuropaCryoWardenEntity.java").contains("First contact: Europa Cryo Warden")
                        && sourceText("src/main/java/com/knoxhack/echoorbitalremnants/entity/EchoZeroEntity.java").contains("First contact: ECHO-0"),
                "Major encounters should keep first-contact readability tells");
        helper.succeed();
    }

    private static void assetCompleteness(GameTestHelper helper) {
        String lang = resourceText("assets/echoorbitalremnants/lang/en_us.json");
        ModItems.creativeItems().forEach(item -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(item.get());
            if (!EchoOrbitalRemnants.MODID.equals(id.getNamespace())) {
                return;
            }
            helper.assertTrue(lang.contains("\"item." + EchoOrbitalRemnants.MODID + "." + id.getPath() + "\"")
                            || lang.contains("\"block." + EchoOrbitalRemnants.MODID + "." + id.getPath() + "\""),
                    id + " should have item or block lang coverage");
            helper.assertTrue(hasResource("assets/echoorbitalremnants/items/" + id.getPath() + ".json")
                            || hasResource("assets/echoorbitalremnants/models/item/" + id.getPath() + ".json"),
                    id + " should have an item definition or item model");
        });
        ModBlocks.ALL_BLOCKS.forEach(block -> {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block.get());
            helper.assertTrue(lang.contains("\"block." + EchoOrbitalRemnants.MODID + "." + id.getPath() + "\""),
                    id + " should have block lang coverage");
            helper.assertTrue(hasResource("assets/echoorbitalremnants/blockstates/" + id.getPath() + ".json"),
                    id + " should have blockstate coverage");
            helper.assertTrue(hasResource("assets/echoorbitalremnants/models/block/" + id.getPath() + ".json"),
                    id + " should have block model coverage");
        });
        List.of(
                ModEntities.EMERGENCY_ROCKET_VEHICLE.get(),
                ModEntities.ECHO_DEFENSE_DRONE.get(),
                ModEntities.VACUUM_WRAITH.get(),
                ModEntities.BROKEN_ASTRONAUT.get(),
                ModEntities.NEXUS_HUSK.get(),
                ModEntities.CORRUPTED_DOCKING_AI.get(),
                ModEntities.LUNAR_NEXUS_HUSK.get(),
                ModEntities.ABANDONED_CAPTAIN.get(),
                ModEntities.ECHO_ZERO.get(),
                ModEntities.EUROPA_CRYO_WARDEN.get(),
                ModEntities.SATURN_RELAY_SENTINEL.get(),
                ModEntities.TITAN_METHANE_STALKER.get(),
                ModEntities.ORBITAL_FACTION_NPC.get()
        ).forEach(type -> {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            helper.assertTrue(lang.contains("\"entity." + EchoOrbitalRemnants.MODID + "." + id.getPath() + "\""),
                    id + " should have entity lang coverage");
            if (type != ModEntities.EMERGENCY_ROCKET_VEHICLE.get()) {
                helper.assertTrue(hasResource("data/echoorbitalremnants/loot_table/entities/" + id.getPath() + ".json"),
                        id + " should have entity loot table coverage");
            }
        });
        String build = sourceText("build.gradle");
        String modToml = sourceText("src/main/templates/META-INF/neoforge.mods.toml");
        String deferredAnimationLib = "gecko" + "lib";
        helper.assertFalse(build.toLowerCase(java.util.Locale.ROOT).contains(deferredAnimationLib)
                        || modToml.toLowerCase(java.util.Locale.ROOT).contains(deferredAnimationLib),
                "Deferred animation library should remain absent from build metadata");
        String commonIntegration = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/integration/OrbitalTerminalCommonIntegration.java");
        helper.assertFalse(commonIntegration.contains(".client."),
                "Common terminal integration should not import client-only terminal classes");
        String modEntry = sourceText("src/main/java/com/knoxhack/echoorbitalremnants/EchoOrbitalRemnants.java");
        helper.assertTrue(modEntry.contains("ModList.get().isLoaded(\"echoterminal\")"),
                "Optional ECHO Terminal registration should remain ModList-gated");
        Path industrialCompat = projectPath("src/main/java/com/knoxhack/echoorbitalremnants/integration/OrbitalIndustrialCompat.java");
        if (industrialAddonIncluded()) {
            helper.assertTrue(Files.exists(industrialCompat),
                    "Industrial compat bridge should ship once Industrial Nexus is included");
        }
        helper.succeed();
    }

    private static boolean industrialAddonIncluded() {
        String addonSet = System.getProperty("echoAddonSet", "beta").trim().toLowerCase(java.util.Locale.ROOT);
        return addonSet.equals("all") || addonSet.contains("industrial");
    }

    private static String sourceText(String relativePath) {
        Path path = projectPath(relativePath);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String resourceText(String relativePath) {
        Path path = projectPath("src/main/resources/" + relativePath);
        try {
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(relativePath)) {
                return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            return "";
        }
    }

    private static boolean hasResource(String relativePath) {
        return Files.exists(projectPath("src/main/resources/" + relativePath))
                || ModGameTests.class.getClassLoader().getResource(relativePath) != null;
    }

    private static Path projectPath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path addonPath = current.resolve(Path.of("addons", "echoorbitalremnants", relativePath));
            if (Files.exists(addonPath)) {
                return addonPath;
            }
            Path directPath = current.resolve(relativePath);
            if (Files.exists(directPath) && current.getFileName() != null
                    && "echoorbitalremnants".equals(current.getFileName().toString())) {
                return directPath;
            }
            current = current.getParent();
        }
        return Path.of(relativePath);
    }

    private static void assertOrbitalTabChrome(GameTestHelper helper, String className, String title) {
        try {
            Class<?> tabClass = Class.forName(className);
            Constructor<?> constructor = tabClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            TerminalTab tab = (TerminalTab) constructor.newInstance();
            TerminalTabChrome chrome = tab.chrome();
            helper.assertTrue(title.equals(chrome.shortTitle()),
                    className + " should render as " + title);
            helper.assertTrue(TerminalTabChrome.GROUP_ORBITAL.equals(chrome.group()),
                    title + " should live in the ORBITAL group");
        } catch (ReflectiveOperationException | RuntimeException error) {
            helper.assertTrue(false, "Orbital terminal taxonomy reflection failed: " + error.getMessage());
        }
    }

    private static void assertOrbitalNavigationProfiles(GameTestHelper helper) {
        try {
            TerminalNavigationProfiles.withClearedForTests(() -> {
                try {
                    Class<?> integration = Class.forName("com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalIntegration");
                    java.lang.reflect.Field registeredField = integration.getDeclaredField("REGISTERED");
                    registeredField.setAccessible(true);
                    ((AtomicBoolean) registeredField.get(null)).set(false);
                    integration.getMethod("register").invoke(null);
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
                assertOrbitalNavigationProfile(helper, OrbitalTerminalIds.COMMAND_TAB, 300);
                assertOrbitalNavigationProfile(helper, OrbitalTerminalIds.SURVEY_TAB, 310);
                assertOrbitalNavigationProfile(helper, OrbitalTerminalIds.ECHO_TAB, 320);
            });
        } catch (RuntimeException error) {
            helper.assertTrue(false, "Orbital terminal navigation profile registration failed: " + error.getMessage());
        }
    }

    private static void assertOrbitalNavigationProfile(GameTestHelper helper, Identifier tabId, int order) {
        TerminalNavigationProfile profile = TerminalNavigationProfiles.profile(tabId).orElse(null);
        helper.assertTrue(profile != null, tabId + " should register a Terminal navigation profile");
        helper.assertTrue(profile.section() == TerminalNavigationSection.CHAPTERS,
                tabId + " should live in the terminal Progress mod section");
        helper.assertTrue(OrbitalTerminalIds.CHAPTER_ID.toString().equals(profile.chapterId()),
                tabId + " should use the shared Orbital chapter id");
        helper.assertTrue("Orbital Remnants".equals(profile.chapterTitle()),
                tabId + " should use the Orbital Remnants chapter title");
        helper.assertTrue("OR".equals(profile.chapterIcon()),
                tabId + " should use the OR chapter badge");
        helper.assertTrue(profile.order() == order,
                tabId + " should preserve tab ordering in the Orbital Remnants chapter");
    }

    private static void betaCacheSupport(GameTestHelper helper) {
        helper.assertTrue(Config.DEFAULT_HAZARD_DRAIN_MULTIPLIER == 75,
                "Integrated route pacing should default hazard drain to 75 percent");
        helper.assertTrue(Config.DEFAULT_ORBITAL_EVENT_FREQUENCY == 3000,
                "Integrated route pacing should space orbital event pressure to 3000 ticks");
        helper.assertTrue(Config.DEFAULT_MACHINE_BASE_DURATION == 140,
                "Integrated route pacing should reduce machine base duration");
        helper.assertTrue(Config.DEFAULT_MACHINE_CHARGE_REGEN_TICKS == 16,
                "Integrated route pacing should reduce machine charge regen wait");
        var level = helper.getLevel();
        List<GroundRecoverySite> sites = GroundRecoverySites.seedStarterSites(level, new BlockPos(20, 10, 70));
        helper.assertTrue(sites.size() == 5, "Starter calibration should seed five critical Earth recovery sites");
        helper.assertTrue(hasCacheWith(level, sitePos(sites, GroundRecoverySiteType.ABANDONED_LAUNCH_PAD), ModItems.ROCKET_NOSE_CONE.get()),
                "Starter launch site should place a rocket-part support cache");
        helper.assertTrue(hasCacheWith(level, sitePos(sites, GroundRecoverySiteType.CRASHED_SATELLITE_FIELD), ModItems.ORBITAL_TRANSPONDER.get()),
                "Starter satellite field should place transponder support");
        helper.assertTrue(hasCacheWith(level, sitePos(sites, GroundRecoverySiteType.ORBITAL_COMMS_ARRAY), ModItems.NAVIGATION_CHIP.get()),
                "Starter comms site should place a navigation support cache");
        helper.assertTrue(hasCacheWith(level, sitePos(sites, GroundRecoverySiteType.CRYO_CREW_BUNKER), ModItems.SEALED_SUIT_FRAGMENT.get()),
                "Starter cryo bunker should place sealed suit fragments");
        helper.assertTrue(hasCacheWith(level, sitePos(sites, GroundRecoverySiteType.FALLEN_ESCAPE_POD), ModItems.SALVAGED_ENGINE.get()),
                "Starter escape pod should place engine salvage");

        OrbitalDebrisField.seedArrivalField(level, new BlockPos(80, 10, 70));
        helper.assertTrue(hasCacheCount(level, new BlockPos(80, 10, 70), ModItems.EMERGENCY_OXYGEN_CELL.get(), 6),
                "Arrival support multiplier should make orbital emergency oxygen recoverable");
        MarsAshBasin.seedLandingSite(level, new BlockPos(120, 10, 70));
        helper.assertTrue(hasCacheWith(level, new BlockPos(120, 10, 70), ModItems.SUIT_SEALANT_PATCH.get()),
                "Mars arrival should include pressure recovery support");
        com.knoxhack.echoorbitalremnants.world.SaturnRingGraveyard.seedLandingSite(level, new BlockPos(160, 10, 70));
        helper.assertTrue(hasCacheWith(level, new BlockPos(160, 10, 70), ModItems.EMERGENCY_OXYGEN_CELL.get())
                        || hasCacheWith(level, new BlockPos(160, 10, 70), ModItems.SUIT_SEALANT_PATCH.get())
                        || hasCacheWith(level, new BlockPos(160, 10, 70), ModItems.OXYGEN_CANISTER.get()),
                "Saturn arrival should include route recoverability support");
        assertArrivalCadence(helper, level, new BlockPos(160, 10, 70), RouteTerrainGenerator.Route.SATURN);
        com.knoxhack.echoorbitalremnants.world.TitanMethaneShelf.seedLandingSite(level, new BlockPos(200, 10, 70));
        helper.assertTrue(hasCacheWith(level, new BlockPos(200, 10, 70), ModItems.EMERGENCY_OXYGEN_CELL.get())
                        || hasCacheWith(level, new BlockPos(200, 10, 70), ModItems.SUIT_SEALANT_PATCH.get())
                        || hasCacheWith(level, new BlockPos(200, 10, 70), ModItems.OXYGEN_CANISTER.get()),
                "Titan arrival should include route recoverability support");
        assertArrivalCadence(helper, level, new BlockPos(200, 10, 70), RouteTerrainGenerator.Route.TITAN);
        String deferredAnimationLib = "geo" + "ckolib";
        helper.assertFalse(net.neoforged.fml.ModList.get().isLoaded(deferredAnimationLib),
                "Deferred animation library should remain absent from the Orbital Remnants runtime");
        helper.succeed();
    }

    private static void earlyGroundRecovery(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        markAshfallNexusChoice(helper, player);
        BlockPos scanOrigin = helper.absolutePos(new BlockPos(20, 10, 70));
        player.setPos(scanOrigin.getX() + 0.5D, scanOrigin.getY(), scanOrigin.getZ() + 0.5D);
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);

        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.hasGroundRecoverySites(), "First terminal scan should seed Earth recovery site records");
        helper.assertTrue(progress.groundRecoverySites().size() == 5, "First terminal scan should track all five critical recovery sites");
        helper.assertFalse(EchoTerminalSnapshot.from(player).groundSiteLines().isEmpty(), "Terminal snapshot should expose ground site lines");

        for (GroundRecoverySite site : progress.groundRecoverySites()) {
            player.setPos(site.pos().getX() + 0.5D, site.pos().getY(), site.pos().getZ() + 0.5D);
            com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        }

        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.allGroundRecoverySitesComplete(), "Scanning near each tracked landmark should complete Earth recovery");
        helper.assertTrue(EchoTerminalSnapshot.from(player).nextObjective().contains("Build launch"),
                "Completed ground recovery should move terminal guidance into launch prep");
        helper.succeed();
    }

    private static void earlyLaunchToOrbit(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        placeLaunchComplex(helper, player, new BlockPos(6, 1, 6));
        giveAssemblyParts(player);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        player.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));

        InteractionResult result = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        EmergencyRocketEntity rocket = firstRocketNear(player);
        helper.assertTrue(rocket != null, "Prepared Emergency Rocket use should place a launch vehicle");
        helper.assertTrue(countRocketsNear(player) == 1, "Prepared Emergency Rocket use should place exactly one launch vehicle");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Placing the rocket should consume the held rocket item");
        helper.assertFalse(EchoTerminalProgress.get(player).lowOrbitReached(), "Placing the rocket should not mark Low Earth Orbit before countdown");
        EchoTerminalSnapshot stagedSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(stagedSnapshot.rocketStaged(), "Terminal snapshot should detect a nearby staged rocket");
        helper.assertTrue("VEHICLE STAGED".equals(stagedSnapshot.rocketLaunchStatus()),
                "Terminal launch status should report a staged empty vehicle");
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(player.getVehicle() == rocket, "Right-clicking an empty rocket should board the player");
        EchoTerminalSnapshot occupiedSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(occupiedSnapshot.rocketOccupied(), "Terminal snapshot should detect an occupied cabin");
        helper.assertTrue("CABIN OCCUPIED".equals(occupiedSnapshot.rocketLaunchStatus()),
                "Terminal launch status should report the occupied cabin");
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(rocket.launchState() == EmergencyRocketEntity.LaunchState.COUNTDOWN,
                "Right-clicking while riding should start the launch countdown");
        EchoTerminalSnapshot countdownSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(countdownSnapshot.rocketCountingDown(), "Terminal snapshot should report countdown state");
        helper.assertTrue(countdownSnapshot.rocketCountdownSeconds() == 5, "Countdown should begin at five seconds");
        for (int i = 0; i < 20; i++) {
            rocket.tick();
        }
        helper.assertTrue(EchoTerminalSnapshot.from(player).rocketCountdownSeconds() == 4,
                "Terminal countdown seconds should decrease after one second");
        for (int i = 0; i < 150; i++) {
            rocket.tick();
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertTrue(result == InteractionResult.SUCCESS_SERVER, "Prepared Emergency Rocket placement should succeed");
        helper.assertTrue(progress.lowOrbitReached(), "Emergency Rocket launch should mark Low Earth Orbit reached");
        helper.assertTrue(progress.hasEarthReturnPoint(), "Emergency Rocket launch should save an Earth return vector");
        helper.succeed();
    }

    private static void rocketVehiclePlacement(GameTestHelper helper) {
        var unprepared = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(unprepared);
        unprepared.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));
        InteractionResult unpreparedResult = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), unprepared, InteractionHand.MAIN_HAND);
        helper.assertTrue(unpreparedResult == InteractionResult.CONSUME, "Unprepared rocket placement should be blocked");
        helper.assertTrue(countRocketsNear(unprepared) == 0, "Blocked placement should not spawn a rocket");
        helper.assertTrue(count(unprepared.getInventory(), ModItems.EMERGENCY_ROCKET.get()) == 1,
                "Blocked placement should not consume the rocket item");

        var partialPad = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(partialPad);
        BlockPos center = new BlockPos(6, 1, 6);
        placeLaunchComplex(helper, partialPad, center);
        helper.setBlock(center.offset(2, 0, 2), Blocks.AIR);
        partialPad.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        partialPad.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        partialPad.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        partialPad.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        partialPad.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));
        partialPad.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));
        InteractionResult partialPadResult = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), partialPad, InteractionHand.MAIN_HAND);
        helper.assertTrue(partialPadResult == InteractionResult.CONSUME, "Incomplete 5x5 launch platform should block placement");
        helper.assertTrue(countRocketsNear(partialPad) == 0, "Incomplete launch platform should not spawn a rocket");
        helper.assertTrue(count(partialPad.getInventory(), ModItems.EMERGENCY_ROCKET.get()) == 1,
                "Incomplete launch platform should not consume the rocket item");

        var prepared = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(prepared);
        placeLaunchComplex(helper, prepared, center);
        prepared.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        prepared.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        prepared.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        prepared.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        prepared.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));
        EmergencyRocketEntity staged = ModEntities.EMERGENCY_ROCKET_VEHICLE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(staged != null, "Emergency Rocket vehicle should be spawnable for duplicate placement testing");
        BlockPos absolute = helper.absolutePos(center);
        staged.setLaunchPadPosition(absolute.getX() + 0.5D, absolute.getY() + 2.0D, absolute.getZ() + 0.5D, prepared.getYRot());
        helper.getLevel().addFreshEntity(staged);
        int stagedBefore = countRocketsNear(prepared);
        prepared.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));
        InteractionResult duplicatePlacement = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), prepared, InteractionHand.MAIN_HAND);
        helper.assertTrue(duplicatePlacement == InteractionResult.CONSUME, "Duplicate rocket placement should be blocked");
        helper.assertTrue(countRocketsNear(prepared) == stagedBefore, "Duplicate rocket placement should not spawn another staged vehicle");
        helper.assertTrue(prepared.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                "Duplicate rocket placement should not consume the replacement rocket item");
        staged.discard();

        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        EchoTerminalProgress.reset(creative);
        BlockPos creativeLocal = center.offset(0, 2, 0);
        clearRocketVolume(helper, creativeLocal);
        BlockPos creativePos = helper.absolutePos(creativeLocal);
        if (creative instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(helper.getLevel(), creativePos.getX() + 0.5D, creativePos.getY(), creativePos.getZ() + 0.5D,
                    Set.of(), creative.getYRot(), creative.getXRot(), false);
        } else {
            creative.setPos(creativePos.getX() + 0.5D, creativePos.getY(), creativePos.getZ() + 0.5D);
        }
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_ROCKET.get()));
        int creativeRocketsBefore = countRocketsNear(helper.getLevel(), creative, 16.0D);
        InteractionResult creativePlacement = ModItems.EMERGENCY_ROCKET.get().use(helper.getLevel(), creative, InteractionHand.MAIN_HAND);
        helper.assertTrue(creativePlacement == InteractionResult.SUCCESS_SERVER,
                "Creative/infinite materials should bypass launch readiness for staging, got " + creativePlacement);
        int creativeRocketsAfter = countRocketsNear(helper.getLevel(), creative, 16.0D);
        helper.assertTrue(creativeRocketsAfter == creativeRocketsBefore + 1,
                "Creative bypass should spawn one rocket vehicle, found " + creativeRocketsBefore
                        + " before and " + creativeRocketsAfter + " after within placement range");
        helper.assertTrue(creative.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                "Creative bypass should not consume the rocket item");
        helper.succeed();
    }

    private static void rocketCountdownAbort(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        BlockPos center = new BlockPos(6, 1, 6);
        placeLaunchComplex(helper, player, center);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        player.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));
        EmergencyRocketEntity rocket = ModEntities.EMERGENCY_ROCKET_VEHICLE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(rocket != null, "Emergency Rocket vehicle should be spawnable for abort testing");
        BlockPos absolute = helper.absolutePos(center);
        rocket.setLaunchPadPosition(absolute.getX() + 0.5D, absolute.getY() + 2.0D, absolute.getZ() + 0.5D, player.getYRot());
        helper.getLevel().addFreshEntity(rocket);
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(rocket.launchState() == EmergencyRocketEntity.LaunchState.COUNTDOWN,
                "Rocket should be counting down before abort");
        for (int i = 0; i < 20; i++) {
            rocket.tick();
        }
        helper.assertTrue(EchoTerminalSnapshot.from(player).rocketCountdownSeconds() == 4,
                "Countdown snapshot should tick down before abort");
        player.stopRiding();
        rocket.tick();
        helper.assertTrue(rocket.launchState() == EmergencyRocketEntity.LaunchState.PLACED,
                "Dismounting before ignition should abort the countdown");
        EchoTerminalSnapshot abortedSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue("VEHICLE STAGED".equals(abortedSnapshot.rocketLaunchStatus()),
                "Aborted countdown should return terminal status to vehicle staged");
        helper.assertFalse(EchoTerminalProgress.get(player).lowOrbitReached(), "Aborted countdown should not mark Low Earth Orbit reached");
        helper.succeed();
    }

    private static void rocketIgnitionCommit(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        BlockPos center = new BlockPos(6, 1, 6);
        placeLaunchComplex(helper, player, center);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURIZED_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURIZED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURIZED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MAGNETIC_BOOTS.get()));
        player.getInventory().add(new ItemStack(ModItems.OXYGEN_TANK.get()));
        EmergencyRocketEntity rocket = ModEntities.EMERGENCY_ROCKET_VEHICLE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(rocket != null, "Emergency Rocket vehicle should be spawnable for ignition commit testing");
        BlockPos absolute = helper.absolutePos(center);
        rocket.setLaunchPadPosition(absolute.getX() + 0.5D, absolute.getY() + 2.0D, absolute.getZ() + 0.5D, player.getYRot());
        helper.getLevel().addFreshEntity(rocket);
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        rocket.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        for (int i = 0; i < EmergencyRocketEntity.COUNTDOWN_TICKS; i++) {
            rocket.tick();
        }
        helper.assertTrue(rocket.launchState() == EmergencyRocketEntity.LaunchState.LAUNCHING,
                "Rocket should enter committed ascent after countdown reaches zero");
        EchoTerminalSnapshot ascentSnapshot = EchoTerminalSnapshot.from(player);
        helper.assertTrue(ascentSnapshot.rocketLaunching(), "Terminal snapshot should report ascent committed");
        helper.assertTrue("ASCENT COMMITTED".equals(ascentSnapshot.rocketLaunchStatus()),
                "Terminal launch status should name committed ascent");
        player.stopRiding();
        for (int i = 0; i < EmergencyRocketEntity.ASCENT_TICKS; i++) {
            rocket.tick();
        }
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.lowOrbitReached(), "Committed ignition should reach Low Earth Orbit even after dismount");
        helper.assertTrue(progress.hasEarthReturnPoint(), "Committed ignition should preserve the Earth return vector");
        helper.succeed();
    }

    private static void rocketDamageDrop(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        BlockPos center = new BlockPos(6, 1, 6);
        placeLaunchComplex(helper, player, center);
        EmergencyRocketEntity rocket = ModEntities.EMERGENCY_ROCKET_VEHICLE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(rocket != null, "Emergency Rocket vehicle should be spawnable for damage testing");
        BlockPos absolute = helper.absolutePos(center);
        rocket.setLaunchPadPosition(absolute.getX() + 0.5D, absolute.getY() + 2.0D, absolute.getZ() + 0.5D, player.getYRot());
        helper.getLevel().addFreshEntity(rocket);
        int dropsBefore = countRocketItemDropsNear(player);
        rocket.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 4.0F);
        helper.assertTrue(rocket.isRemoved(), "Damaging a placed rocket before launch should remove the vehicle");
        helper.assertTrue(countRocketItemDropsNear(player) == dropsBefore + 1,
                "Damaging a placed rocket before launch should drop one Emergency Rocket item");
        helper.succeed();
    }

    private static void betaFactionContracts(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress.get(player).markLowOrbitReached(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOID_SALVAGER_MARKER.get()));
        ModItems.VOID_SALVAGER_MARKER.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.prepareFactionContract(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.voidSalvagerStanding() == FactionStanding.ALIGNED,
                "Crashbreak pledge should persist aligned standing");
        helper.assertTrue(progress.factionContractStatus().contains("Crashbreak"),
                "Aligned faction should expose an active terminal contract");

        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        player.getInventory().add(new ItemStack(ModItems.ORBITAL_ALLOY.get()));
        player.getInventory().add(new ItemStack(ModItems.VACUUM_CIRCUIT.get()));
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.completedFactionContractCount() == 1,
                "Completing a faction contract should persist exactly one completion");
        helper.assertTrue(progress.lastTerminalReport().contains("Crashbreak Orbital Salvage Manifest complete"),
                "Terminal report should describe the completed faction contract");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.NAVIGATION_CHIP.get())),
                "Faction contract should grant a useful mission reward");

        int completed = progress.completedFactionContractCount();
        int chips = count(player.getInventory(), ModItems.NAVIGATION_CHIP.get());
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).completedFactionContractCount() == completed,
                "Cooldown scan should not double-complete faction contracts");
        helper.assertTrue(count(player.getInventory(), ModItems.NAVIGATION_CHIP.get()) == chips,
                "Cooldown scan should not double-grant faction contract rewards");
        helper.assertTrue(EchoTerminalSnapshot.from(player).factionContract().contains("cooling down")
                        || EchoTerminalSnapshot.from(player).factionContract().contains("Crashbreak"),
                "Terminal snapshot should expose faction contract state");
        helper.succeed();
    }

    private static void betaRadwardenContract(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress.get(player).markLowOrbitReached(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ORBITAL_REMNANT_BADGE.get()));
        ModItems.ORBITAL_REMNANT_BADGE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        EchoTerminalProgress.get(player).prepareFactionContract(player);
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).lastTerminalReport().contains("wrong dimension"),
                "Radwarden contract should explain wrong-dimension blocking");
        player.getInventory().add(new ItemStack(ModItems.ORBIT_SURVEY_DATA.get()));
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).completedFactionContractCount() == 1,
                "Radwarden contract should complete from Orbit Survey Data proof");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.OXYGEN_CANISTER.get())),
                "Radwarden contract should grant oxygen support rewards");
        helper.succeed();
    }

    private static void betaCrashbreakContract(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress.get(player).markLowOrbitReached(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOID_SALVAGER_MARKER.get()));
        ModItems.VOID_SALVAGER_MARKER.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        EchoTerminalProgress.get(player).prepareFactionContract(player);
        player.getInventory().clearContent();
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).lastTerminalReport().contains("Orbital Alloy"),
                "Crashbreak contract should name missing proof items");
        player.getInventory().add(new ItemStack(ModItems.ORBITAL_ALLOY.get()));
        player.getInventory().add(new ItemStack(ModItems.VACUUM_CIRCUIT.get()));
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).completedFactionContractCount() == 1,
                "Crashbreak contract should complete from salvage proof items");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.NAVIGATION_CHIP.get())),
                "Crashbreak contract should grant navigation support rewards");
        helper.succeed();
    }

    private static void betaSporeboundContract(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        EchoTerminalProgress.get(player).markLowOrbitReached(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NEXUS_CHOIR_SIGIL.get()));
        ModItems.NEXUS_CHOIR_SIGIL.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        EchoTerminalProgress.get(player).prepareFactionContract(player);
        player.setPos(player.getX(), Config.ORBITAL_ALTITUDE.get(), player.getZ());
        player.getInventory().add(new ItemStack(ModItems.NEXUS_STABILIZER_SHARD.get()));
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).lastTerminalReport().contains("ECHO-0"),
                "Sporebound contract should stay locked before ECHO-0");
        helper.assertTrue(EchoTerminalProgress.get(player).completedFactionContractCount() == 0,
                "Sporebound contract should not complete before ECHO-0");
        EchoTerminalProgress.get(player).markEchoZeroEncountered(player);
        com.knoxhack.echoorbitalremnants.item.EchoTerminalItem.performScan(player);
        helper.assertTrue(EchoTerminalProgress.get(player).completedFactionContractCount() == 1,
                "Sporebound contract should complete from stabilizer shard proof after ECHO-0");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.CRYO_BATTERY.get())),
                "Sporebound contract should grant late-route support rewards");
        helper.succeed();
    }

    private static void outpostNpcRegistration(GameTestHelper helper) {
        OrbitalFactionNpcEntity npc = ModEntities.ORBITAL_FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(npc != null, "Orbital faction NPC entity should be registered and spawnable");
        npc.configure(FactionPledgeItem.Faction.ORBITAL_REMNANT, OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.ORBITAL_REMNANT));
        helper.assertTrue(npc.faction() == FactionPledgeItem.Faction.ORBITAL_REMNANT,
                "Orbital faction NPC should retain synced faction identity");
        helper.assertTrue(npc.roleId().equals(OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.ORBITAL_REMNANT)),
                "Orbital faction NPC should retain synced role identity");
        helper.assertTrue(npc.getCustomName() != null && npc.getCustomName().getString().contains("Radwarden"),
                "Orbital faction NPC nameplate should expose the faction");
        helper.succeed();
    }

    private static void outpostCharterGating(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        completeFinalSurveyPrerequisites(player);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        helper.assertTrue(!progress.finalNetworkSealed() && !progress.canSealFinalNetwork(),
                "Final seal should remain blocked before outpost charters");
        progress.acceptOutpostCharter(player, FactionPledgeItem.Faction.VOID_SALVAGERS);
        EchoTerminalProgress.get(player).completeOutpostCharter(player, FactionPledgeItem.Faction.VOID_SALVAGERS);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.completedOutpostCharterCount() == 1 && !progress.canSealFinalNetwork(),
                "One Tier I charter should not unlock the final seal");
        progress.acceptOutpostCharter(player, FactionPledgeItem.Faction.ORBITAL_REMNANT);
        EchoTerminalProgress.get(player).completeOutpostCharter(player, FactionPledgeItem.Faction.ORBITAL_REMNANT);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.completedOutpostCharterCount() == 2 && !progress.canSealFinalNetwork(),
                "Two Tier I charters should not unlock the final seal");
        progress.acceptOutpostCharter(player, FactionPledgeItem.Faction.NEXUS_CHOIR);
        EchoTerminalProgress.get(player).completeOutpostCharter(player, FactionPledgeItem.Faction.NEXUS_CHOIR);
        progress = EchoTerminalProgress.get(player);
        helper.assertTrue(progress.completedOutpostCharterCount() == 3 && progress.finalNetworkSealed(),
                "All three Tier I charters should seal the final network when surveys and ECHO-0 are complete");
        helper.succeed();
    }

    private static void outpostLegacyMigration(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress.reset(player);
        CompoundTag legacy = new CompoundTag();
        legacy.putString("completedFactionContracts", "legacy:a|legacy:b|legacy:c");
        player.getPersistentData().put("echoorbitalremnants_progress", legacy);
        EchoTerminalProgress migrated = EchoTerminalProgress.get(player);
        helper.assertTrue(migrated.allOutpostChartersComplete(),
                "Old saves with three completed faction contracts should migrate to all Tier I outpost charters");
        helper.assertTrue(migrated.completedOutpostCharterCount() == 3,
                "Legacy faction contract migration should expose three completed outpost charters");
        EchoTerminalProgress.reset(player);
        CompoundTag sealed = new CompoundTag();
        sealed.putBoolean("final_network_sealed", true);
        player.getPersistentData().put("echoorbitalremnants_progress", sealed);
        helper.assertTrue(EchoTerminalProgress.get(player).allOutpostChartersComplete(),
                "Already sealed old saves should stay eligible under the new outpost gate");
        helper.succeed();
    }

    private static void outpostActionValidation(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        EchoTerminalProgress.reset(player);
        OrbitalFactionNpcEntity npc = ModEntities.ORBITAL_FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(npc != null, "Orbital faction NPC should be spawnable for action validation");
        npc.configure(FactionPledgeItem.Faction.VOID_SALVAGERS, OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.VOID_SALVAGERS));
        npc.setPos(player.getX() + 1.0D, player.getY(), player.getZ());
        helper.getLevel().addFreshEntity(npc);
        OrbitalFactionDialogueService.handleAction(
                new OrbitalFactionNpcActionPayload(npc.getId(), OrbitalFactionDialogueService.ACTION_ACCEPT_CHARTER,
                        OrbitalOutpostProfiles.contractId(FactionPledgeItem.Faction.VOID_SALVAGERS)),
                player);
        helper.assertTrue(EchoTerminalProgress.get(player).activeOutpostCharterId().isBlank(),
                "Outpost NPC actions should reject wrong-dimension charter attempts server-side");
        OrbitalFactionDialogueService.handleAction(
                new OrbitalFactionNpcActionPayload(npc.getId() + 999, OrbitalFactionDialogueService.ACTION_ACCEPT_CHARTER,
                        OrbitalOutpostProfiles.contractId(FactionPledgeItem.Faction.VOID_SALVAGERS)),
                player);
        helper.assertTrue(EchoTerminalProgress.get(player).activeOutpostCharterId().isBlank(),
                "Outpost NPC actions should reject invalid entity ids server-side");
        helper.succeed();
    }

    private static void outpostNpcPersistence(GameTestHelper helper) {
        OrbitalFactionNpcEntity npc = ModEntities.ORBITAL_FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(npc != null, "Orbital faction NPC should be spawnable for persistence testing");
        npc.configure(FactionPledgeItem.Faction.NEXUS_CHOIR, OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.NEXUS_CHOIR));
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        npc.saveWithoutId(output);
        CompoundTag saved = output.buildResult();

        OrbitalFactionNpcEntity loaded = ModEntities.ORBITAL_FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(loaded != null, "Orbital faction NPC should be reloadable from saved data");
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(loaded.faction() == FactionPledgeItem.Faction.NEXUS_CHOIR,
                "Orbital faction NPC should persist faction identity");
        helper.assertTrue(loaded.roleId().equals(OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.NEXUS_CHOIR)),
                "Orbital faction NPC should persist role identity");
        helper.succeed();
    }

    private static void outpostSpawnCaps(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.teleportTo(helper.getLevel(), playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D,
                Set.of(), player.getYRot(), player.getXRot(), false);
        int totalCap = outpostSpawnerIntField("MAX_NEARBY_TOTAL");
        int factionCap = outpostSpawnerIntField("MAX_NEARBY_FACTION");
        for (int i = 0; i < totalCap; i++) {
            FactionPledgeItem.Faction faction = i == 0
                    ? FactionPledgeItem.Faction.VOID_SALVAGERS
                    : i == 1 ? FactionPledgeItem.Faction.ORBITAL_REMNANT : FactionPledgeItem.Faction.NEXUS_CHOIR;
            OrbitalFactionNpcEntity npc = ModEntities.ORBITAL_FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(npc != null, "Orbital faction NPC should be spawnable for cap testing");
            npc.configure(faction, OrbitalOutpostProfiles.roleId(faction));
            npc.setPos(player.getX() + i + 1.0D, player.getY(), player.getZ());
            helper.assertTrue(helper.getLevel().addFreshEntity(npc),
                    "Orbital faction NPC should be added to the loaded cap-test area");
        }
        helper.assertTrue(outpostNearbyCount(helper.getLevel(), player, null) >= totalCap,
                "Outpost spawner should count nearby NPCs against the total cap");
        helper.assertTrue(outpostNearbyCount(helper.getLevel(), player, FactionPledgeItem.Faction.VOID_SALVAGERS) >= factionCap,
                "Outpost spawner should count nearby NPCs against the faction cap");
        helper.succeed();
    }

    private static void outpostBarterBehavior(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.SATURN_RING_FRAGMENT.get()));
        OrbitalFactionNpcEntity npc = ModEntities.ORBITAL_FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(npc != null, "Orbital faction NPC should be spawnable for barter testing");
        npc.configure(FactionPledgeItem.Faction.VOID_SALVAGERS, OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.VOID_SALVAGERS));
        npc.setPos(player.getX() + 1.0D, player.getY(), player.getZ());
        helper.getLevel().addFreshEntity(npc);

        invokeOutpostBarter(player, npc, FactionPledgeItem.Faction.VOID_SALVAGERS, "barter_crashbreak_navigation");
        helper.assertTrue(count(player.getInventory(), ModItems.SATURN_RING_FRAGMENT.get()) == 0,
                "Outpost barter should consume the offered cost item");
        helper.assertTrue(count(player.getInventory(), ModItems.NAVIGATION_CHIP.get()) == 1,
                "Outpost barter should grant the configured reward item");
        helper.succeed();
    }

    private static void midGameObjectiveChain(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.repairStationRelay(player, "orbit:relay:a");
        EchoTerminalProgress.RouteObjectiveResult duplicate = EchoTerminalProgress.get(player).repairStationRelay(player, "orbit:relay:a");
        helper.assertTrue(duplicate.duplicate(), "Station relay repairs should reject duplicate site ids");
        EchoTerminalProgress.get(player).repairStationRelay(player, "orbit:relay:b");
        helper.assertFalse(EchoTerminalProgress.get(player).stationNetworkRestored(), "Two station relay repairs should not restore the network");
        EchoTerminalProgress.get(player).repairStationRelay(player, "orbit:relay:c");
        helper.assertTrue(EchoTerminalProgress.get(player).stationNetworkRestored(), "Three station relay repairs should restore the network");

        EchoTerminalProgress.get(player).repairLunarExtractor(player, "moon:extractor:a");
        EchoTerminalProgress.get(player).repairLunarExtractor(player, "moon:extractor:b");
        EchoTerminalProgress.get(player).repairLunarExtractor(player, "moon:extractor:c");
        helper.assertTrue(EchoTerminalProgress.get(player).lunarExtractorOnline(), "Three lunar extractor repairs should bring the network online");
        helper.assertTrue(EchoTerminalProgress.get(player).marsRouteUnlocked(), "Lunar extractor completion should unlock Mars reliability");

        EchoTerminalProgress.get(player).repairMarsPressureConsole(player, "mars:console:a");
        EchoTerminalProgress.get(player).repairMarsPressureConsole(player, "mars:console:b");
        EchoTerminalProgress.get(player).repairMarsPressureConsole(player, "mars:console:c");
        helper.assertTrue(EchoTerminalProgress.get(player).marsHabitatsPressurized(), "Three Mars pressure repairs should pressurize habitats");
        helper.assertTrue(EchoTerminalProgress.get(player).europaRouteUnlocked(), "Mars habitat completion should unlock Europa prep");

        EchoTerminalProgress.get(player).repairEuropaThermalArray(player, "europa:array:a");
        EchoTerminalProgress.get(player).repairEuropaThermalArray(player, "europa:array:b");
        EchoTerminalProgress.get(player).repairEuropaThermalArray(player, "europa:array:c");
        helper.assertTrue(EchoTerminalProgress.get(player).europaArrayCalibrated(), "Three Europa array repairs should calibrate Saturn prep");
        helper.assertTrue(EchoTerminalProgress.get(player).saturnRouteUnlocked(), "Europa array completion should unlock Saturn routing");
        helper.assertFalse(EchoTerminalProgress.get(player).deepSpaceProtocolUnlocked(), "Europa array completion should not skip Saturn and Titan prep");

        EchoTerminalProgress.get(player).repairSaturnRingRelay(player, "saturn:relay:a");
        EchoTerminalProgress.get(player).repairSaturnRingRelay(player, "saturn:relay:b");
        EchoTerminalProgress.get(player).repairSaturnRingRelay(player, "saturn:relay:c");
        helper.assertTrue(EchoTerminalProgress.get(player).saturnRelaysRestored(), "Three Saturn relay repairs should stabilize Titan descent");
        helper.assertTrue(EchoTerminalProgress.get(player).titanRouteUnlocked(), "Saturn relay completion should unlock Titan routing");

        EchoTerminalProgress.get(player).repairTitanMethanePump(player, "titan:pump:a");
        EchoTerminalProgress.get(player).repairTitanMethanePump(player, "titan:pump:b");
        EchoTerminalProgress.get(player).repairTitanMethanePump(player, "titan:pump:c");
        helper.assertTrue(EchoTerminalProgress.get(player).titanPumpsPressurized(), "Three Titan pump repairs should pressurize methane telemetry");
        helper.assertTrue(EchoTerminalProgress.get(player).deepSpaceProtocolUnlocked(), "Titan pump completion should unlock Deep Space Protocol");
        helper.assertTrue(EchoTerminalProgress.get(player).allMidGameObjectivesComplete(), "All six mid-game route chains should complete together");
        helper.succeed();
    }

    private static void midGameRouteGates(GameTestHelper helper) {
        var shuttlePlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        shuttlePlayer.setPos(shuttlePlayer.getX(), Config.ORBITAL_ALTITUDE.get(), shuttlePlayer.getZ());
        EchoTerminalProgress shuttleProgress = EchoTerminalProgress.get(shuttlePlayer);
        shuttleProgress.restoreStationLifeSupport(shuttlePlayer);
        shuttlePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ORBITAL_SHUTTLE.get()));
        InteractionResult lockedShuttle = ModItems.ORBITAL_SHUTTLE.get().use(helper.getLevel(), shuttlePlayer, InteractionHand.MAIN_HAND);
        helper.assertTrue(lockedShuttle == InteractionResult.CONSUME, "Orbital Shuttle should require Station Network restoration");
        EchoTerminalProgress.get(shuttlePlayer).repairStationRelay(shuttlePlayer, "orbit:a");
        EchoTerminalProgress.get(shuttlePlayer).repairStationRelay(shuttlePlayer, "orbit:b");
        EchoTerminalProgress.get(shuttlePlayer).repairStationRelay(shuttlePlayer, "orbit:c");
        InteractionResult openShuttle = ModItems.ORBITAL_SHUTTLE.get().use(helper.getLevel(), shuttlePlayer, InteractionHand.MAIN_HAND);
        helper.assertTrue(openShuttle == InteractionResult.SUCCESS_SERVER, "Restored Station Network should open the Lunar route gate");

        var marsPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        marsPlayer.setPos(marsPlayer.getX(), Config.ORBITAL_ALTITUDE.get(), marsPlayer.getZ());
        EchoTerminalProgress.get(marsPlayer).unlockMarsRoute(marsPlayer);
        marsPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MARS_TRANSFER_WINDOW.get()));
        InteractionResult lockedMars = ModItems.MARS_TRANSFER_WINDOW.get().use(helper.getLevel(), marsPlayer, InteractionHand.MAIN_HAND);
        helper.assertTrue(lockedMars == InteractionResult.CONSUME, "Mars route should require Helium Extractor restoration when mid-game objectives are enabled");
        EchoTerminalProgress.get(marsPlayer).repairLunarExtractor(marsPlayer, "moon:a");
        EchoTerminalProgress.get(marsPlayer).repairLunarExtractor(marsPlayer, "moon:b");
        EchoTerminalProgress.get(marsPlayer).repairLunarExtractor(marsPlayer, "moon:c");
        InteractionResult openMars = ModItems.MARS_TRANSFER_WINDOW.get().use(helper.getLevel(), marsPlayer, InteractionHand.MAIN_HAND);
        helper.assertTrue(openMars == InteractionResult.SUCCESS_SERVER, "Restored Helium Extractors should open Mars transfer");
        helper.succeed();
    }

    private static void midGameRecipesAndSites(GameTestHelper helper) {
        assertRecipe(helper, "machine_station_relay_fuse");
        assertRecipe(helper, "machine_helium_extractor_core");
        assertRecipe(helper, "machine_pressure_regulator");
        assertRecipe(helper, "machine_europa_probe_array");
        helper.assertTrue(RouteTerrainGenerator.routeObjectiveBlock(RouteTerrainGenerator.Route.ORBIT).is(ModBlocks.STATION_RELAY_NODE.get()),
                "Orbit deep sites should use Station Relay Nodes");
        helper.assertTrue(RouteTerrainGenerator.routeObjectiveBlock(RouteTerrainGenerator.Route.MOON).is(ModBlocks.HELIUM_EXTRACTOR_NODE.get()),
                "Moon deep sites should use Helium Extractor Nodes");
        helper.assertTrue(RouteTerrainGenerator.routeObjectiveBlock(RouteTerrainGenerator.Route.MARS).is(ModBlocks.MARS_PRESSURE_CONSOLE.get()),
                "Mars deep sites should use pressure consoles");
        helper.assertTrue(RouteTerrainGenerator.routeObjectiveBlock(RouteTerrainGenerator.Route.EUROPA).is(ModBlocks.EUROPA_THERMAL_ARRAY.get()),
                "Europa deep sites should use thermal arrays");
        helper.assertTrue(RouteTerrainGenerator.cacheItems(RouteTerrainGenerator.Route.MOON, 1).stream().anyMatch(stack -> stack.is(ModItems.HELIUM_EXTRACTOR_CORE.get())),
                "Moon cache variants should include mid-game repair resources");
        helper.succeed();
    }

    private static void europaCryoWarden(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var warden = ModEntities.EUROPA_CRYO_WARDEN.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(warden != null, "Europa Cryo Warden should be registered and spawnable");
        if (warden != null) {
            warden.die(player.damageSources().playerAttack(player));
        }
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.THERMAL_STABILIZER.get())),
                "Europa Cryo Warden should grant Thermal Stabilizer reward once through its defeat hook");
        helper.assertFalse(EchoTerminalProgress.get(player).echoZeroEncountered(),
                "Europa Cryo Warden defeat should not affect ECHO-0 completion");
        helper.succeed();
    }

    private static void bossIdentity(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos playerPos = helper.absolutePos(new BlockPos(8, 2, 8));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        CorruptedDockingAiEntity dockingAi = ModEntities.CORRUPTED_DOCKING_AI.get()
                .create(helper.getLevel(), EntitySpawnReason.EVENT);
        AbandonedCaptainEntity captain = ModEntities.ABANDONED_CAPTAIN.get()
                .create(helper.getLevel(), EntitySpawnReason.EVENT);
        EuropaCryoWardenEntity warden = ModEntities.EUROPA_CRYO_WARDEN.get()
                .create(helper.getLevel(), EntitySpawnReason.EVENT);
        EchoZeroEntity echoZero = ModEntities.ECHO_ZERO.get()
                .create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(dockingAi != null, "Corrupted Docking AI should be spawnable");
        helper.assertTrue(captain != null, "Abandoned Captain should be spawnable");
        helper.assertTrue(warden != null, "Europa Cryo Warden should be spawnable");
        helper.assertTrue(echoZero != null, "ECHO-0 should be spawnable");

        if (dockingAi == null || captain == null || warden == null || echoZero == null) {
            helper.assertTrue(false, "Boss identity test requires all orbital boss entities");
            return;
        }

        placeBossForIdentityTest(dockingAi, player, 2.0D, 0.0D);
        placeBossForIdentityTest(captain, player, -2.0D, 0.0D);
        placeBossForIdentityTest(warden, player, 0.0D, 2.0D);
        placeBossForIdentityTest(echoZero, player, 0.0D, -2.0D);
        helper.getLevel().addFreshEntity(dockingAi);
        helper.getLevel().addFreshEntity(captain);
        helper.getLevel().addFreshEntity(warden);
        helper.getLevel().addFreshEntity(echoZero);

        dockingAi.setHealth(dockingAi.getMaxHealth() * 0.30F);
        captain.setHealth(captain.getMaxHealth() * 0.30F);
        warden.setHealth(warden.getMaxHealth() * 0.30F);
        echoZero.setHealth(echoZero.getMaxHealth() * 0.30F);
        for (int i = 0; i < 150; i++) {
            dockingAi.tick();
            captain.tick();
            warden.tick();
            echoZero.tick();
        }

        helper.assertTrue(dockingAi.getEncounterPhase() >= 3, "Docking AI should expose later-phase airlock pressure");
        helper.assertTrue(captain.getEncounterPhase() >= 3, "Abandoned Captain should expose later-phase crew pressure");
        helper.assertTrue(warden.getEncounterPhase() >= 3, "Europa Cryo Warden should expose later-phase thermal pressure");
        helper.assertTrue(echoZero.getEncounterPhase() >= 3, "ECHO-0 should expose later-phase quarantine pressure");
        SuitState state = SuitState.get(player);
        helper.assertTrue(state.oxygen() < 100 || state.pressure() < 100 || state.radiation() > 0,
                "Orbital boss identity pulses should pressure at least one suit system");

        dockingAi.die(player.damageSources().playerAttack(player));
        captain.die(player.damageSources().playerAttack(player));
        warden.die(player.damageSources().playerAttack(player));
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.NAVIGATION_CHIP.get())),
                "Corrupted Docking AI should reward navigation recovery");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.MARTIAN_SILICA.get())),
                "Abandoned Captain should reward Mars transfer lore/materials");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.THERMAL_STABILIZER.get())),
                "Europa Cryo Warden should reward thermal stabilization");
        int blackBoxCount = count(player.getInventory(), ModItems.ORBITAL_BLACK_BOX.get());
        helper.assertTrue(blackBoxCount == 1,
                "Orbital boss rewards should grant exactly one duplicate-safe Orbital Black Box");

        int nexusCoreCount = count(player.getInventory(), ModItems.NEXUS_DRIVE_CORE.get());
        helper.assertTrue(EchoZeroEntity.completeEncounter(player), "ECHO-0 should complete the final orbital protocol once");
        helper.assertFalse(EchoZeroEntity.completeEncounter(player), "ECHO-0 should not duplicate the final protocol reward");
        helper.assertTrue(count(player.getInventory(), ModItems.NEXUS_DRIVE_CORE.get()) == nexusCoreCount + 1,
                "ECHO-0 should grant exactly one Nexus Drive Core");
        helper.assertTrue(count(player.getInventory(), ModItems.ORBITAL_BLACK_BOX.get()) == blackBoxCount,
                "ECHO-0 completion should not duplicate an existing Orbital Black Box");
        echoZero.discard();
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoOrbitalRemnants.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, path);
    }

    private static boolean hasDiagnostic(List<EchoDiagnosticBlocker> diagnostics, String path) {
        Identifier diagnosticId = id(path);
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.id().equals(diagnosticId));
    }

    private static List<EchoRouteRecord> orbitalRouteRecords(net.minecraft.world.entity.player.Player player) {
        return EchoCoreServices.routeRecords(player).stream()
                .filter(record -> "orbital_remnants".equals(record.chapterId()))
                .toList();
    }

    private static EchoRouteRecord routeRecord(List<EchoRouteRecord> records, String path) {
        Identifier routeId = id(path);
        return records.stream()
                .filter(record -> record.id().equals(routeId))
                .findFirst()
                .orElse(null);
    }

    private static void assertOrbitalRoutePlacement(GameTestHelper helper,
            net.minecraft.world.entity.player.Player player,
            List<TerminalMissionDefinition> missions,
            String path,
            int expectedPhase,
            int expectedOrder) {
        TerminalMissionDefinition definition = missionDefinition(missions, path);
        helper.assertTrue(definition != null, "Orbital mission should exist for route placement: " + path);
        TerminalMissionSnapshot snapshot = OrbitalMissionProvider.INSTANCE.snapshot(player, id(path));
        TerminalMissionRoutePlacement placement = OrbitalMissionProvider.INSTANCE
                .routePlacement(player, definition, snapshot, TerminalMissionRole.MAIN)
                .orElse(null);
        helper.assertTrue(placement != null, "Orbital mission should publish Survival Route placement: " + path);
        helper.assertTrue(placement.phaseOrder() == expectedPhase && placement.missionOrder() == expectedOrder
                        && placement.role() == TerminalMissionRole.MAIN && placement.includeInSurvivalRoute(),
                "Orbital mission should publish expected Survival Route phase/order: " + path);
    }

    private static void assertSurvivalRoutePhase(GameTestHelper helper,
            List<TerminalMissionDefinition> missions,
            String path,
            int expectedPhase) {
        TerminalMissionDefinition definition = missionDefinition(missions, path);
        helper.assertTrue(definition != null, "Survival Route should include Orbital mission: " + path);
        helper.assertTrue(definition.phaseOrder() == expectedPhase
                        && definition.phaseId().equals(String.format(java.util.Locale.ROOT, "phase_%02d", expectedPhase))
                        && definition.phaseTitle().equals(orbitalSurvivalRouteStageTitle(expectedPhase)),
                "Survival Route should place Orbital mission in expected phase: " + path);
    }

    private static String orbitalSurvivalRouteStageTitle(int phaseOrder) {
        return switch (phaseOrder) {
            case 10 -> "Deep Extraction";
            case 13 -> "Cryogenic Route";
            case 14 -> "Nexus Decision";
            case 15 -> "Aftermath";
            default -> String.format(java.util.Locale.ROOT, "phase_%02d", phaseOrder);
        };
    }

    private static TerminalMissionDefinition missionDefinition(List<TerminalMissionDefinition> missions, String path) {
        Identifier missionId = id(path);
        return missions.stream()
                .filter(definition -> definition.id().equals(missionId))
                .findFirst()
                .orElse(null);
    }

    private static void markAshfallNexusChoice(GameTestHelper helper, net.minecraft.world.entity.player.Player player) {
        if (!net.neoforged.fml.ModList.get().isLoaded("echoashfallprotocol")) {
            return;
        }
        try {
            Class<?> postClass = Class.forName("com.knoxhack.echoashfallprotocol.endgame.PostNexusData");
            Class<?> pathClass = Class.forName("com.knoxhack.echoashfallprotocol.endgame.PostNexusData$NexusPath");
            Object restorePath = enumValue(pathClass, "RESTORE");
            Object postNexus = postClass.getMethod("get", net.minecraft.world.entity.player.Player.class)
                    .invoke(null, player);
            postClass.getMethod("setSelectedPath", pathClass).invoke(postNexus, restorePath);
            setAshfallPostNexusAttachment(player, postNexus);
            if (player instanceof ServerPlayer serverPlayer) {
                postClass.getMethod("saveAndSync", ServerPlayer.class, postClass).invoke(null, serverPlayer, postNexus);
            }

            Class<?> worldClass = Class.forName("com.knoxhack.echoashfallprotocol.world.NexusWorldData");
            Class<?> stateClass = Class.forName("com.knoxhack.echoashfallprotocol.world.NexusWorldData$WorldState");
            Object restoredState = enumValue(stateClass, "RESTORED");
            Object worldData = worldClass.getMethod("get", net.minecraft.server.level.ServerLevel.class)
                    .invoke(null, helper.getLevel().getServer().overworld());
            worldClass.getMethod("setChoice", stateClass, BlockPos.class, String.class)
                    .invoke(worldData, restoredState, helper.absolutePos(BlockPos.ZERO), "GameTest");
        } catch (ReflectiveOperationException | RuntimeException error) {
            helper.assertTrue(false, "GameTest could not seed Ashfall Nexus choice: " + error.getMessage());
        }
    }

    private static void setAshfallPostNexusAttachment(net.minecraft.world.entity.player.Player player, Object postNexus)
            throws ReflectiveOperationException {
        Class<?> attachmentsClass = Class.forName("com.knoxhack.echoashfallprotocol.registry.ModAttachments");
        Object supplier = attachmentsClass.getField("POST_NEXUS_DATA").get(null);
        Object attachmentType = supplier.getClass().getMethod("get").invoke(supplier);
        for (Method method : player.getClass().getMethods()) {
            if (!method.getName().equals("setData") || method.getParameterCount() != 2) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(attachmentType.getClass())) {
                method.invoke(player, attachmentType, postNexus);
                return;
            }
        }
        throw new NoSuchMethodException("Player#setData for Ashfall post-Nexus data");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
    }

    private static String snapshotStatus(Object snapshot) throws ReflectiveOperationException {
        Object status = snapshot.getClass().getMethod("status").invoke(snapshot);
        return status == null ? "" : status.toString();
    }

    private static boolean hasTerminalActionHandler(Map<?, ?> handlers, Identifier tabId, Identifier actionId)
            throws ReflectiveOperationException {
        for (Object key : handlers.keySet()) {
            Identifier keyTabId = terminalActionKeyId(key, "tabId");
            Identifier keyActionId = terminalActionKeyId(key, "actionId");
            if (tabId.equals(keyTabId) && actionId.equals(keyActionId)) {
                return true;
            }
        }
        return false;
    }

    private static Identifier terminalActionKeyId(Object key, String methodName) throws ReflectiveOperationException {
        Method method = key.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (Identifier) method.invoke(key);
    }

    private static void assertRecipe(GameTestHelper helper, String path) {
        Identifier recipeId = id(path);
        boolean exists = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.id().identifier().equals(recipeId));
        helper.assertTrue(exists, "Required survival recipe missing: " + recipeId);
    }

    private static void assertNoRecipe(GameTestHelper helper, String path) {
        Identifier recipeId = id(path);
        boolean exists = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.id().identifier().equals(recipeId));
        helper.assertFalse(exists, "Recipe should not be available through normal survival crafting: " + recipeId);
    }

    private static String expectedSiteName(RouteTerrainGenerator.Route route, int variant) {
        int normalized = Math.floorMod(variant, 3);
        return switch (route) {
            case ORBIT -> switch (normalized) {
                case 1 -> "docking rib corridor";
                case 2 -> "solar breaker yard";
                default -> "station relay spine";
            };
            case MOON -> switch (normalized) {
                case 1 -> "scar drill cairn";
                case 2 -> "Nexus impact survey pit";
                default -> "helium extractor camp";
            };
            case MARS -> switch (normalized) {
                case 1 -> "pressure pipe yard";
                case 2 -> "dust-shield pylon";
                default -> "buried habitat wing";
            };
            case EUROPA -> switch (normalized) {
                case 1 -> "frozen cable substation";
                case 2 -> "cryo vault vent";
                default -> "thermal array lab";
            };
            case SATURN -> switch (normalized) {
                case 1 -> "ring relay rib";
                case 2 -> "salvager trade spine";
                default -> "Saturn ice breaker yard";
            };
            case TITAN -> switch (normalized) {
                case 1 -> "methane pump field";
                case 2 -> "tholin survey dome";
                default -> "Titan pressure shelf";
            };
            case NEXUS -> switch (normalized) {
                case 1 -> "folded station bridge";
                case 2 -> "Nexus growth cluster";
                default -> "Nexus anchor island";
            };
        };
    }

    private static BlockPos placeLaunchComplex(GameTestHelper helper, net.minecraft.world.entity.player.Player player, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(center.offset(x, 0, z), ModBlocks.LAUNCH_PLATFORM.get());
            }
        }
        BlockPos framePos = center.above();
        helper.setBlock(framePos, ModBlocks.ROCKET_ASSEMBLY_FRAME.get());
        helper.setBlock(center.offset(3, 1, 0), ModBlocks.FUEL_REFINERY.get());
        helper.setBlock(center.offset(-3, 1, 0), ModBlocks.OXYGEN_COMPRESSOR.get());
        helper.setBlock(center.offset(0, 1, 3), ModBlocks.NAVIGATION_CONSOLE.get());
        BlockPos absolute = helper.absolutePos(center);
        double playerX = absolute.getX() + 0.5D;
        double playerY = absolute.getY() + 2.0D;
        double playerZ = absolute.getZ() + 0.5D;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(helper.getLevel(), playerX, playerY, playerZ, Set.of(),
                    player.getYRot(), player.getXRot(), false);
        } else {
            player.setPos(playerX, playerY, playerZ);
        }
        return framePos;
    }

    private static void clearRocketVolume(GameTestHelper helper, BlockPos localBase) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    helper.setBlock(localBase.offset(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void giveAssemblyParts(net.minecraft.world.entity.player.Player player) {
        player.getInventory().add(new ItemStack(ModItems.ROCKET_NOSE_CONE.get()));
        player.getInventory().add(new ItemStack(ModItems.FUEL_TANK.get()));
        player.getInventory().add(new ItemStack(ModItems.SALVAGED_ENGINE.get()));
        player.getInventory().add(new ItemStack(ModItems.LANDING_GEAR.get()));
        player.getInventory().add(new ItemStack(ModItems.ECHO_FLIGHT_CORE.get()));
        player.getInventory().add(new ItemStack(ModItems.NAVIGATION_COMPUTER.get()));
    }

    private static void placeBossForIdentityTest(Mob boss, net.minecraft.world.entity.player.Player player, double xOffset, double zOffset) {
        boss.setPos(player.getX() + xOffset, player.getY(), player.getZ() + zOffset);
        boss.setTarget(player);
    }

    private static void completeFinalSurveyPrerequisites(net.minecraft.world.entity.player.Player player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.markEchoZeroEncountered(player);
        progress = EchoTerminalProgress.get(player);
        for (int i = 1; i <= 3; i++) {
            progress.recordOrbitSurvey(player, "outpost:orbit:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordMoonSurvey(player, "outpost:moon:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordMarsSurvey(player, "outpost:mars:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordEuropaSurvey(player, "outpost:europa:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordSaturnSurvey(player, "outpost:saturn:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordTitanSurvey(player, "outpost:titan:" + i);
            progress = EchoTerminalProgress.get(player);
            progress.recordNexusStabilization(player, "outpost:nexus:" + i);
            progress = EchoTerminalProgress.get(player);
        }
    }

    private static void completeAllOutpostCharters(net.minecraft.world.entity.player.Player player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.acceptOutpostCharter(player, FactionPledgeItem.Faction.VOID_SALVAGERS);
        progress = EchoTerminalProgress.get(player);
        progress.completeOutpostCharter(player, FactionPledgeItem.Faction.VOID_SALVAGERS);
        progress = EchoTerminalProgress.get(player);
        progress.acceptOutpostCharter(player, FactionPledgeItem.Faction.ORBITAL_REMNANT);
        progress = EchoTerminalProgress.get(player);
        progress.completeOutpostCharter(player, FactionPledgeItem.Faction.ORBITAL_REMNANT);
        progress = EchoTerminalProgress.get(player);
        progress.acceptOutpostCharter(player, FactionPledgeItem.Faction.NEXUS_CHOIR);
        progress = EchoTerminalProgress.get(player);
        progress.completeOutpostCharter(player, FactionPledgeItem.Faction.NEXUS_CHOIR);
    }

    private static int outpostSpawnerIntField(String fieldName) {
        try {
            java.lang.reflect.Field field = OrbitalOutpostSpawner.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException("Unable to read outpost spawner cap " + fieldName, error);
        }
    }

    private static int outpostNearbyCount(net.minecraft.server.level.ServerLevel level, ServerPlayer player,
            FactionPledgeItem.Faction faction) {
        try {
            Method method = OrbitalOutpostSpawner.class.getDeclaredMethod("countNearby",
                    net.minecraft.server.level.ServerLevel.class,
                    ServerPlayer.class,
                    FactionPledgeItem.Faction.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, level, player, faction);
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException("Unable to invoke outpost nearby NPC count", error);
        }
    }

    private static void invokeOutpostBarter(ServerPlayer player, OrbitalFactionNpcEntity npc,
            FactionPledgeItem.Faction faction, String actionId) {
        try {
            Method method = OrbitalFactionDialogueService.class.getDeclaredMethod("barter",
                    ServerPlayer.class,
                    OrbitalFactionNpcEntity.class,
                    FactionPledgeItem.Faction.class,
                    String.class);
            method.setAccessible(true);
            method.invoke(null, player, npc, faction, actionId);
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException("Unable to invoke outpost barter behavior", error);
        }
    }

    private static int count(net.minecraft.world.entity.player.Inventory inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void removeOne(net.minecraft.world.entity.player.Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    private static boolean moveOneToHand(net.minecraft.world.entity.player.Player player, Item item) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(item)) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                return true;
            }
        }
        return false;
    }

    private static int countRocketsNear(net.minecraft.world.entity.player.Player player) {
        return countRocketsNear(player.level(), player);
    }

    private static int countRocketsNear(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player) {
        return rocketsNear(level, player).size();
    }

    private static int countRocketsNear(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, double radius) {
        return rocketsNear(level, player, radius).size();
    }

    private static EmergencyRocketEntity firstRocketNear(net.minecraft.world.entity.player.Player player) {
        return rocketsNear(player).stream()
                .filter(EmergencyRocketEntity.class::isInstance)
                .map(EmergencyRocketEntity.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static EmergencyRocketEntity firstRocketNear(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player) {
        return rocketsNear(level, player).stream()
                .filter(EmergencyRocketEntity.class::isInstance)
                .map(EmergencyRocketEntity.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static List<Entity> rocketsNear(net.minecraft.world.entity.player.Player player) {
        return rocketsNear(player.level(), player);
    }

    private static List<Entity> rocketsNear(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player) {
        return rocketsNear(level, player, 4.0D);
    }

    private static List<Entity> rocketsNear(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, double radius) {
        AABB area = new AABB(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius);
        return level.getEntities((Entity) null, area, entity -> entity.getType() == ModEntities.EMERGENCY_ROCKET_VEHICLE.get());
    }

    private static int countRocketItemDropsNear(net.minecraft.world.entity.player.Player player) {
        AABB area = new AABB(player.getX() - 4.0D, player.getY() - 4.0D, player.getZ() - 4.0D,
                player.getX() + 4.0D, player.getY() + 4.0D, player.getZ() + 4.0D);
        return player.level().getEntities((Entity) null, area,
                entity -> entity instanceof ItemEntity item && item.getItem().is(ModItems.EMERGENCY_ROCKET.get())).size();
    }

    private static int countItemDropsNear(GameTestHelper helper, BlockPos center, Item item) {
        AABB area = new AABB(center.getX() - 4.0D, center.getY() - 4.0D, center.getZ() - 4.0D,
                center.getX() + 4.0D, center.getY() + 4.0D, center.getZ() + 4.0D);
        int total = 0;
        for (Entity entity : helper.getLevel().getEntities((Entity) null, area,
                candidate -> candidate instanceof ItemEntity itemEntity && itemEntity.getItem().is(item))) {
            total += ((ItemEntity) entity).getItem().getCount();
        }
        return total;
    }

    private static int countChestsNear(net.minecraft.server.level.ServerLevel level, BlockPos center, int radius) {
        int total = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -8, -radius), center.offset(radius, 8, radius))) {
            if (level.getBlockState(pos).is(Blocks.CHEST)) {
                total++;
            }
        }
        return total;
    }

    private static int countOrbitalThreatsNear(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, double radius) {
        return countOrbitalThreatsNear(level, player.blockPosition(), radius);
    }

    private static int countOrbitalThreatsNear(net.minecraft.world.level.Level level, BlockPos center, double radius) {
        AABB area = new AABB(center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius, center.getY() + radius, center.getZ() + radius);
        return level.getEntities((Entity) null, area, ModGameTests::isOrbitalThreat).size();
    }

    private static boolean isOrbitalThreat(Entity entity) {
        return entity.getType() == ModEntities.ECHO_DEFENSE_DRONE.get()
                || entity.getType() == ModEntities.VACUUM_WRAITH.get()
                || entity.getType() == ModEntities.BROKEN_ASTRONAUT.get()
                || entity.getType() == ModEntities.NEXUS_HUSK.get()
                || entity.getType() == ModEntities.LUNAR_NEXUS_HUSK.get()
                || entity.getType() == ModEntities.EUROPA_CRYO_WARDEN.get()
                || entity.getType() == ModEntities.SATURN_RELAY_SENTINEL.get()
                || entity.getType() == ModEntities.TITAN_METHANE_STALKER.get();
    }

    private static BlockPos sitePos(List<GroundRecoverySite> sites, GroundRecoverySiteType type) {
        return sites.stream()
                .filter(site -> site.type() == type)
                .findFirst()
                .map(GroundRecoverySite::pos)
                .orElse(BlockPos.ZERO);
    }

    private static boolean hasCacheWith(net.minecraft.server.level.ServerLevel level, BlockPos center, Item item) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-18, -8, -18), center.offset(18, 8, 18))) {
            if (level.getBlockState(pos).is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof Container container
                    && container.hasAnyMatching(stack -> stack.is(item))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCacheCount(net.minecraft.server.level.ServerLevel level, BlockPos center, Item item, int minCount) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-18, -8, -18), center.offset(18, 8, 18))) {
            if (level.getBlockState(pos).is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof Container container) {
                int total = 0;
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.is(item)) {
                        total += stack.getCount();
                    }
                }
                if (total >= minCount) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertArrivalCadence(GameTestHelper helper, net.minecraft.server.level.ServerLevel level,
            BlockPos center, RouteTerrainGenerator.Route route) {
        List<ItemStack> stacks = cacheStacksNear(level, center);
        helper.assertTrue(!stacks.isEmpty(), route.getSerializedName() + " arrival should place a cache");
        helper.assertTrue(hasProgressionValue(route, stacks),
                route.getSerializedName() + " arrival should include progression value");
        helper.assertTrue(hasCraftingSupport(stacks),
                route.getSerializedName() + " arrival should include crafting support");
        helper.assertTrue(hasRecoverabilitySupport(stacks),
                route.getSerializedName() + " arrival should include survival recovery");
    }

    private static List<ItemStack> cacheStacksNear(net.minecraft.server.level.ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-18, -8, -18), center.offset(18, 8, 18))) {
            if (level.getBlockState(pos).is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof Container container) {
                java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!stack.isEmpty()) {
                        stacks.add(stack.copy());
                    }
                }
                return List.copyOf(stacks);
            }
        }
        return List.of();
    }

    private static boolean hasProgressionValue(RouteTerrainGenerator.Route route, List<ItemStack> stacks) {
        return switch (route) {
            case ORBIT -> hasAny(stacks, ModItems.ORBIT_SURVEY_DATA.get(), ModItems.STATION_RELAY_FUSE.get());
            case MOON -> hasAny(stacks, ModItems.LUNAR_CORE_SAMPLE.get(), ModItems.HELIUM_EXTRACTOR_CORE.get());
            case MARS -> hasAny(stacks, ModItems.MARTIAN_PRESSURE_VALVE.get(), ModItems.PRESSURE_REGULATOR.get());
            case EUROPA -> hasAny(stacks, ModItems.EUROPA_THERMAL_PROBE.get(), ModItems.EUROPA_PROBE_ARRAY.get());
            case SATURN -> hasAny(stacks, ModItems.SATURN_RING_FRAGMENT.get(), ModItems.SATURN_RELAY_LENS.get(), ModItems.TITAN_TRANSFER_WINDOW.get());
            case TITAN -> hasAny(stacks, ModItems.TITAN_METHANE_CELL.get(), ModItems.TITAN_SURVEY_CORE.get(), ModItems.NEXUS_DRIVE_CORE.get());
            case NEXUS -> hasAny(stacks, ModItems.NEXUS_STABILIZER_SHARD.get(), ModItems.LUNAR_CORE_FRAGMENT.get());
        };
    }

    private static boolean hasCraftingSupport(List<ItemStack> stacks) {
        return hasAny(stacks,
                ModItems.NAVIGATION_CHIP.get(),
                ModItems.VACUUM_CIRCUIT.get(),
                ModItems.ORBITAL_ALLOY.get(),
                ModItems.LUNAR_TITANIUM.get(),
                ModItems.HELIUM_3_CELL.get(),
                ModItems.NEXUS_DUST.get(),
                ModItems.MARTIAN_SILICA.get(),
                ModItems.CRYO_CRYSTAL.get(),
                ModItems.CRYO_BATTERY.get(),
                ModItems.THERMAL_STABILIZER.get(),
                ModItems.NEXUS_STABILIZER_SHARD.get())
                || stacks.stream().anyMatch(stack -> stack.is(ModBlocks.BROKEN_SOLAR_PANEL.get().asItem())
                        || stack.is(ModBlocks.OXYGEN_PIPE.get().asItem()));
    }

    private static boolean hasAny(List<ItemStack> stacks, Item... items) {
        for (Item item : items) {
            if (stacks.stream().anyMatch(stack -> stack.is(item))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRecoverabilitySupport(List<ItemStack> stacks) {
        return stacks.stream().anyMatch(stack -> stack.is(ModItems.EMERGENCY_OXYGEN_CELL.get())
                || stack.is(ModItems.SUIT_SEALANT_PATCH.get())
                || stack.is(ModItems.OXYGEN_CANISTER.get()));
    }

}
