package com.knoxhack.echostationfall.registry;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoHandoffs;
import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.integration.StationfallTerminalCommonIntegration;
import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationfallObjective;
import com.knoxhack.echostationfall.progression.StationfallCooldown;
import com.knoxhack.echostationfall.progression.StationfallRouteTracker;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.progression.StationLore;
import com.knoxhack.echostationfall.integration.StationfallIndustrialCompat;
import com.knoxhack.echostationfall.integration.StationfallOrbitalCompat;
import com.knoxhack.echostationfall.integration.StationfallSuitState;
import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallRouteService;
import com.knoxhack.echostationfall.world.StationfallStationGenerator;
import com.knoxhack.echostationfall.world.StationfallStationState;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoStationfall.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODULE_REGISTRATION =
            TEST_FUNCTIONS.register("module_registration", () -> ModGameTests::moduleRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORBITAL_GATE =
            TEST_FUNCTIONS.register("orbital_gate", () -> ModGameTests::orbitalGate);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESS_AND_PANIC =
            TEST_FUNCTIONS.register("progress_and_panic", () -> ModGameTests::progressAndPanic);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATION_STATE =
            TEST_FUNCTIONS.register("station_state", () -> ModGameTests::stationState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GENERATOR_LAYOUT =
            TEST_FUNCTIONS.register("generator_layout", () -> ModGameTests::generatorLayout);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GENERATOR_SALVAGE =
            TEST_FUNCTIONS.register("generator_salvage", () -> ModGameTests::generatorSalvage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_SERVICE =
            TEST_FUNCTIONS.register("route_service", () -> ModGameTests::routeService);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_COMPAT =
            TEST_FUNCTIONS.register("terminal_compat", () -> ModGameTests::terminalCompat);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOSS_REWARDS =
            TEST_FUNCTIONS.register("boss_rewards", () -> ModGameTests::bossRewards);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SURVIVAL_ITEMS =
            TEST_FUNCTIONS.register("survival_items", () -> ModGameTests::survivalItems);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COOLDOWNS_AND_GUARDS =
            TEST_FUNCTIONS.register("cooldowns_and_guards", () -> ModGameTests::cooldownsAndGuards);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_TRACKER_OBJECTIVES =
            TEST_FUNCTIONS.register("route_tracker_objectives", () -> ModGameTests::routeTrackerObjectives);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("stationfall_release"));
        register(event, environment, "module_registration", MODULE_REGISTRATION.getId());
        register(event, environment, "orbital_gate", ORBITAL_GATE.getId());
        register(event, environment, "progress_and_panic", PROGRESS_AND_PANIC.getId());
        register(event, environment, "station_state", STATION_STATE.getId());
        register(event, environment, "generator_layout", GENERATOR_LAYOUT.getId());
        register(event, environment, "generator_salvage", GENERATOR_SALVAGE.getId());
        register(event, environment, "route_service", ROUTE_SERVICE.getId());
        register(event, environment, "terminal_compat", TERMINAL_COMPAT.getId());
        register(event, environment, "boss_rewards", BOSS_REWARDS.getId());
        register(event, environment, "survival_items", SURVIVAL_ITEMS.getId());
        register(event, environment, "cooldowns_and_guards", COOLDOWNS_AND_GUARDS.getId());
        register(event, environment, "route_tracker_objectives", ROUTE_TRACKER_OBJECTIVES.getId());
    }

    private static void moduleRegistration(GameTestHelper helper) {
        helper.assertTrue(ModItems.creativeItems().contains(ModItems.STATIONFALL_BLACKBOX),
                "Stationfall Blackbox should be exposed in the creative tab item list");
        helper.assertTrue(ModItems.creativeItems().contains(ModItems.STATION_ACCESS_CARD),
                "Station Access Card should be exposed in the creative tab item list");
        helper.assertTrue(ModBlocks.ALL_BLOCKS.contains(ModBlocks.STATION_POWER_NODE),
                "Station Power Node should be part of Stationfall block registration");
        helper.assertTrue(ModBlocks.ALL_BLOCKS.contains(ModBlocks.COMMAND_CONSOLE),
                "Command Console should be part of Stationfall block registration");

        ServerLevel level = helper.getLevel();
        helper.assertTrue(ModEntities.HOLLOW_CREWMAN.get().create(level, EntitySpawnReason.EVENT) instanceof Mob,
                "Hollow Crewman should spawn as a mob");
        helper.assertTrue(ModEntities.STATION_MOTHER.get().create(level, EntitySpawnReason.EVENT)
                        instanceof ModEntities.StationMotherEntity,
                "Station Mother should spawn as the boss entity");
        helper.succeed();
    }

    private static void orbitalGate(GameTestHelper helper) {
        Player survival = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(StationfallProgress.get(survival).canBoard(survival),
                "Survival players should be gated before Stationfall coordinates/network progress");

        if (StationfallOrbitalCompat.orbitalLoaded()) {
            StationfallOrbitalCompat.markLowOrbitReached(survival);
            helper.assertTrue(StationfallProgress.get(survival).canBoard(survival),
                    "Station coordinates from Orbital Remnants should unlock Stationfall boarding when Orbital is installed");
        } else {
            StationfallProgress.get(survival).unlockCoordinates(survival);
            helper.assertTrue(StationfallProgress.get(survival).canBoard(survival),
                    "Standalone Stationfall coordinates should unlock boarding without Orbital Remnants");
        }

        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        helper.assertTrue(StationfallProgress.get(creative).canBoard(creative),
                "Creative players should bypass Stationfall route gates");
        helper.succeed();
    }

    private static void progressAndPanic(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StationfallProgress progress = StationfallProgress.get(player);
        progress.setSectionPower(player, StationSection.CREW_QUARTERS, StationPowerState.STABLE);
        progress.decodeLog(player, StationSection.CREW_QUARTERS);
        progress.markAiOverrideObtained(player);
        progress.markBlackboxRetrieved(player);
        progress.markTerminalRewardClaimed(player, "crew_quarters_cache");

        StationfallProgress restored = StationfallProgress.get(player);
        helper.assertTrue(restored.powerState(StationSection.CREW_QUARTERS) == StationPowerState.STABLE,
                "Section power state should persist on player data");
        helper.assertTrue(restored.doorUnlocked(StationSection.CREW_QUARTERS),
                "Stable power should unlock the powered section door");
        helper.assertTrue(restored.logDecoded(StationSection.CREW_QUARTERS),
                "Decoded crew logs should persist on player data");
        helper.assertTrue(restored.aiOverrideObtained(), "AI Override milestone should persist");
        helper.assertTrue(restored.blackboxRetrieved(), "Stationfall Blackbox retrieval should persist");
        helper.assertTrue(restored.terminalRewardClaimed("crew_quarters_cache"),
                "Terminal claim state should persist without duplication");
        if (player instanceof ServerPlayer serverPlayer) {
            helper.assertTrue(EchoCoreServices.isArchiveUnlocked(serverPlayer, StationLore.crewLogId(StationSection.CREW_QUARTERS)),
                    "Decoding a crew log should unlock the matching archive record");
            helper.assertTrue(EchoCoreServices.progressLedger(serverPlayer).hasMilestone(EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED),
                    "Retrieving the Stationfall Blackbox should record the canonical saga handoff milestone");
        }

        SignalPanicState panic = SignalPanicState.get(player);
        panic.gain(player, 50);
        helper.assertTrue(SignalPanicState.get(player).high(), "Signal Panic should enter the high threshold");
        panic = SignalPanicState.get(player);
        panic.gain(player, 60);
        helper.assertTrue(SignalPanicState.get(player).critical(), "Signal Panic should cap into the critical threshold");
        panic = SignalPanicState.get(player);
        panic.decay(player, 100);
        helper.assertTrue(SignalPanicState.get(player).value() == 0, "Signal Panic decay should clamp at zero");
        helper.succeed();
    }

    private static void stationState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StationfallStationState state = StationfallStationState.get(level);
        state.setPower(StationSection.ENGINEERING_DECK, StationPowerState.OVERLOADED);
        state.setLogDecoded(StationSection.ENGINEERING_DECK, true);
        state.setBreachRepaired(StationSection.ENGINEERING_DECK, true);
        state.startBoss();
        helper.assertTrue(state.bossActive(), "World state should expose an active Station Mother encounter");
        state.defeatBoss();
        state.markBlackboxRewarded();

        helper.assertTrue(state.powerState(StationSection.ENGINEERING_DECK) == StationPowerState.OVERLOADED,
                "World state should persist section overload state");
        helper.assertTrue(state.doorUnlocked(StationSection.ENGINEERING_DECK),
                "World state should unlock doors for powered sections");
        helper.assertTrue(state.logDecoded(StationSection.ENGINEERING_DECK),
                "World state should persist decoded logs");
        helper.assertTrue(state.breachRepaired(StationSection.ENGINEERING_DECK),
                "World state should persist repaired hull breaches");
        helper.assertTrue(state.bossDefeated(), "World state should persist Station Mother defeat");
        helper.assertTrue(state.blackboxRewarded(), "World state should persist duplicate-safe blackbox rewards");

        BlockPos fixture = StationfallStationGenerator.sectionLightPos(StationSection.DOCKING_RING);
        state.ensureLighting(level);
        helper.assertTrue(state.lightingVersion() == 1,
                "Legacy Stationfall state should advance to the current lighting version");
        helper.assertTrue(level.getBlockState(fixture).is(Blocks.SEA_LANTERN),
                "Legacy Stationfall lighting repair should place deterministic fixture blocks");
        level.setBlock(fixture, Blocks.AIR.defaultBlockState(), 3);
        state.ensureLighting(level);
        helper.assertTrue(level.getBlockState(fixture).isAir(),
                "Stationfall lighting repair should not rerun after the version is current");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StationfallProgress progress = StationfallProgress.get(player);
        progress.setSectionPower(player, StationSection.DATA_CORE, StationPowerState.STABLE);
        progress.decodeLog(player, StationSection.DATA_CORE);
        state.mergeFromProgress(StationfallProgress.get(player));
        helper.assertTrue(state.powerState(StationSection.DATA_CORE) == StationPowerState.STABLE,
                "World state should merge newer player section power");
        helper.assertTrue(state.logDecoded(StationSection.DATA_CORE),
                "World state should merge player log progress");
        progress.markObjectiveComplete(player, StationfallObjective.OBSERVATION_ANTENNA);
        state.mergeFromProgress(StationfallProgress.get(player));
        helper.assertTrue(state.objectiveComplete(StationfallObjective.OBSERVATION_ANTENNA),
                "World state should merge player section objective progress");
        helper.succeed();
    }

    private static void generatorLayout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StationfallStationGenerator.generate(level);

        for (StationSection section : StationSection.values()) {
            helper.assertTrue(level.getBlockState(section.powerNodePos()).is(ModBlocks.STATION_POWER_NODE.get()),
                    section.displayName() + " should contain a power node");
            helper.assertTrue(level.getBlockState(section.logTerminalPos()).is(ModBlocks.CREW_LOG_TERMINAL.get()),
                    section.displayName() + " should contain a crew log terminal");
            helper.assertTrue(level.getBlockState(StationfallStationGenerator.sectionLightPos(section))
                            .is(Blocks.SEA_LANTERN),
                    section.displayName() + " should contain station ceiling light fixtures");
            if (section.next() != null) {
                helper.assertTrue(level.getBlockState(section.doorPos()).is(ModBlocks.PRESSURE_DOOR.get()),
                        section.displayName() + " connector should contain a pressure door");
                helper.assertTrue(level.getBlockState(StationfallStationGenerator.connectorLightPos(section))
                                .is(Blocks.SEA_LANTERN),
                        section.displayName() + " connector should contain a station light fixture");
            }
        }

        helper.assertTrue(level.getBlockState(new BlockPos(StationSection.DATA_CORE.centerX(), 96, 6))
                        .is(ModBlocks.DATA_CORE_TERMINAL.get()),
                "Data Core should contain the AI override terminal");
        helper.assertTrue(level.getBlockState(new BlockPos(StationSection.COMMAND_MODULE.centerX(), 96, 0))
                        .is(ModBlocks.COMMAND_CONSOLE.get()),
                "Command Module should contain the final command console");
        helper.assertTrue(level.getBlockState(new BlockPos(StationSection.OBSERVATION_DECK.centerX(), 97, 10))
                        .is(ModBlocks.CRACKED_OBSERVATION_GLASS.get()),
                "Observation Deck should expose cracked observation glass");
        helper.succeed();
    }

    private static void routeTrackerObjectives(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StationfallProgress progress = StationfallProgress.get(player);
        progress.markBoarded(player);

        helper.assertTrue(StationfallRouteTracker.status(player).contains("objectives 0/5"),
                "Route tracker should expose objective progress");
        helper.assertTrue(progress.recordObjectiveStep(player, StationfallObjective.HYDROPONICS_PURGE, "cluster_a"),
                "Hydroponics should record the first growth cluster purge");
        helper.assertTrue(progress.recordObjectiveStep(player, StationfallObjective.HYDROPONICS_PURGE, "cluster_b"),
                "Hydroponics should record the second growth cluster purge");
        helper.assertFalse(progress.objectiveComplete(StationfallObjective.HYDROPONICS_PURGE),
                "Hydroponics should require multiple clusters before completion");
        helper.assertTrue(progress.recordObjectiveStep(player, StationfallObjective.HYDROPONICS_PURGE, "cluster_c"),
                "Hydroponics should record the final growth cluster purge");
        helper.assertTrue(progress.objectiveComplete(StationfallObjective.HYDROPONICS_PURGE),
                "Hydroponics should complete after the required cluster count");
        helper.assertFalse(progress.recordObjectiveStep(player, StationfallObjective.HYDROPONICS_PURGE, "cluster_c"),
                "Objective step keys should prevent duplicate interaction credit");

        for (StationfallObjective objective : StationfallObjective.values()) {
            if (objective != StationfallObjective.HYDROPONICS_PURGE) {
                helper.assertTrue(progress.markObjectiveComplete(player, objective),
                        objective.title() + " should complete once");
            }
            helper.assertFalse(StationfallProgress.get(player).markObjectiveComplete(player, objective),
                    objective.title() + " should be duplicate-safe");
        }
        StationfallProgress restored = StationfallProgress.get(player);
        helper.assertTrue(restored.objectiveCount() == StationfallObjective.values().length,
                "Section objectives should persist on player progress");
        helper.assertTrue(restored.objectiveStepCount(StationfallObjective.HYDROPONICS_PURGE)
                        == StationfallObjective.HYDROPONICS_PURGE.targetSteps(),
                "Multi-step section objective progress should persist on player progress");
        helper.assertTrue(StationfallRouteTracker.status(player).contains("objectives 5/5"),
                "Route tracker should reflect completed section objectives");

        StationfallSuitState suit = StationfallSuitState.get(player);
        suit.drainOxygen(90);
        suit.compromisePressure(90);
        suit.save(player);
        StationfallIndustrialCompat.supportSuit(player, 40, 40, 0);
        helper.assertTrue(StationfallSuitState.get(player).oxygen() > 10,
                "Industrial compat should support Stationfall suit recovery");
        StationfallIndustrialCompat.recordIndustrialComponent(player, "ai_override_chip_casing");
        helper.assertTrue(StationfallProgress.get(player).aiOverrideObtained(),
                "Industrial compat should expose the AI override support path");
        helper.succeed();
    }

    private static void generatorSalvage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StationfallStationGenerator.generate(level);

        int powerCaches = 0;
        int commonCaches = 0;
        for (StationSection section : StationSection.values()) {
            BlockPos powerCache = StationfallStationGenerator.powerCachePos(section);
            BlockPos commonCache = StationfallStationGenerator.commonCachePos(section);
            helper.assertTrue(level.getBlockState(powerCache).is(Blocks.BARREL),
                    section.displayName() + " should contain a Station Battery power cache");
            helper.assertTrue(level.getBlockState(commonCache).is(Blocks.BARREL),
                    section.displayName() + " should contain a survival support cache");
            helper.assertTrue(powerCache.distManhattan(section.powerNodePos()) <= 8,
                    section.displayName() + " power cache should be reachable beside its power node");
            powerCaches++;
            commonCaches++;
        }

        helper.assertTrue(powerCaches == StationSection.values().length,
                "Stationfall should generate one guaranteed power cache per section");
        helper.assertTrue(commonCaches == StationSection.values().length,
                "Stationfall should generate one common survival cache per section");
        helper.assertTrue(level.getBlockState(StationfallStationGenerator.commandCachePos()).is(Blocks.BARREL),
                "Command Module should contain an endgame support cache");
        helper.succeed();
    }

    private static void routeService(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.assertTrue(StationfallProgress.get(player).canBoard(player),
                    "Creative route gate should be open even when the GameTest harness uses a lightweight player");
            helper.succeed();
            return;
        }
        player.setPos(8, 96, 8);
        helper.assertTrue(StationfallRouteService.board(serverPlayer, "gametest"),
                "Creative route service boarding should succeed through the shared service");
        helper.assertTrue(StationfallDimensions.isStation(player.level()),
                "Route service should teleport the player into the Stationfall dimension");
        helper.assertFalse(StationfallRouteService.board(serverPlayer, "gametest"),
                "Route service should refuse to overwrite the return vector while already aboard");
        helper.assertTrue(StationfallProgress.get(player).boarded(),
                "Boarding through the route service should persist boarded progress");
        helper.assertTrue(StationfallProgress.get(player).hasReturnPoint(),
                "Boarding should save a return vector");
        helper.assertTrue(StationfallRouteService.returnFromStation(serverPlayer),
                "Shared route service should return the player to the saved vector");
        helper.assertFalse(StationfallDimensions.isStation(player.level()),
                "Return action should leave the Stationfall dimension");
        helper.succeed();
    }

    private static void cooldownsAndGuards(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.tickCount = 100;
        helper.assertTrue(StationfallCooldown.ready(player, "test.message", 80),
                "Stationfall cooldown should allow the first event");
        helper.assertFalse(StationfallCooldown.ready(player, "test.message", 80),
                "Stationfall cooldown should block repeated events in the cooldown window");
        player.tickCount = 181;
        helper.assertTrue(StationfallCooldown.ready(player, "test.message", 80),
                "Stationfall cooldown should allow events after the cooldown expires");

        StationfallSuitState suit = StationfallSuitState.get(player);
        suit.drainOxygen(10);
        suit.save(player);
        int oxygenBefore = StationfallSuitState.get(player).oxygen();
        helper.assertTrue(ModEntities.pulseReady(player, "gametest"),
                "Entity pulse cooldown should allow the first hazard pulse");
        ModEntities.pulse(player, 5, 0, 4);
        helper.assertFalse(ModEntities.pulseReady(player, "gametest"),
                "Entity pulse cooldown should block stacked same-source pulses");
        helper.assertTrue(StationfallSuitState.get(player).oxygen() == oxygenBefore - 5,
                "Only the allowed entity pulse should drain suit oxygen");

        player.tickCount = 260;
        helper.assertTrue(ModEntities.hazardPulseReady(player, "global_a"),
                "Global entity hazard budget should allow the first nearby mob pulse");
        helper.assertFalse(ModEntities.hazardPulseReady(player, "global_b"),
                "Global entity hazard budget should block immediate pulses from different mob types");
        player.tickCount = 281;
        helper.assertTrue(ModEntities.hazardPulseReady(player, "global_b"),
                "Global entity hazard budget should reopen after its short fairness window");
        helper.succeed();
    }

    private static void terminalCompat(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        StationfallTerminalCommonIntegration.Provider provider = StationfallTerminalCommonIntegration.Provider.INSTANCE;

        TerminalMissionSnapshot board = provider.snapshot(player, StationfallTerminalCommonIntegration.id("board_station"));
        helper.assertTrue(board.status() == TerminalMissionStatus.UNLOCKED,
                "Stationfall terminal boarding mission should be unlocked for creative gate bypass");
        helper.assertTrue(hasAction(board, StationfallTerminalCommonIntegration.ACTION_BOARD),
                "Boarding snapshot should expose BOARD STATION action");

        StationfallProgress progress = StationfallProgress.get(player);
        progress.markBoarded(player);
        for (StationSection section : StationSection.values()) {
            progress.setSectionPower(player, section, StationPowerState.STABLE);
            progress.decodeLog(player, section);
        }
        TerminalMissionSnapshot stabilize = provider.snapshot(player, StationfallTerminalCommonIntegration.id("stabilize_sections"));
        helper.assertTrue(stabilize.status() == TerminalMissionStatus.UNLOCKED,
                "Stationfall stabilization mission should unlock after power and logs");
        for (StationfallObjective objective : StationfallObjective.values()) {
            progress.markObjectiveComplete(player, objective);
        }
        helper.assertTrue(provider.snapshot(player, StationfallTerminalCommonIntegration.id("stabilize_sections")).status()
                        == TerminalMissionStatus.CLAIMABLE,
                "Completed section objectives should make stabilization mission claimable");
        progress.markAiOverrideObtained(player);
        progress.markBlackboxRetrieved(player);

        TerminalMissionSnapshot blackbox = provider.snapshot(player, StationfallTerminalCommonIntegration.id("blackbox"));
        helper.assertTrue(blackbox.status() == TerminalMissionStatus.CLAIMABLE,
                "Completed Stationfall Blackbox mission should become claimable");
        if (player instanceof ServerPlayer serverPlayer) {
            helper.assertTrue(provider.handleAction(
                            serverPlayer,
                            StationfallTerminalCommonIntegration.id("blackbox"),
                            StationfallTerminalCommonIntegration.ACTION_CLAIM),
                    "Terminal CLAIM CACHE action should be handled by Stationfall");
        } else {
            StationfallProgress.get(player).markTerminalRewardClaimed(player, "blackbox");
        }
        helper.assertTrue(StationfallProgress.get(player).terminalRewardClaimed("blackbox"),
                "Terminal claim state should persist on Stationfall progress");
        helper.assertTrue(provider.snapshot(player, StationfallTerminalCommonIntegration.id("blackbox")).status()
                        == TerminalMissionStatus.CLAIMED,
                "Claimed Stationfall Blackbox mission should remain claimed");
        helper.assertTrue(TerminalArchiveRegistry.entries().stream()
                        .anyMatch(entry -> entry.id().equals(StationfallTerminalCommonIntegration.BLACKBOX_ARCHIVE_ID)
                                && entry.locked()),
                "Stationfall should register the locked Blackbox archive entry");
        helper.succeed();
    }

    private static void bossRewards(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ServerLevel level = helper.getLevel();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.assertFalse(ModEntities.StationMotherEntity.completeEncounter(level, List.of()),
                    "Completing Station Mother without server players should not grant inventory rewards");
            StationfallProgress.get(player).markBlackboxRetrieved(player);
            helper.assertTrue(StationfallProgress.get(player).blackboxRetrieved(),
                    "Blackbox progress should persist for lightweight GameTest players");
            helper.assertTrue(StationfallStationState.get(level).blackboxRewarded(),
                    "Station Mother completion should still mark world reward state");
            helper.succeed();
            return;
        }
        helper.assertTrue(ModEntities.StationMotherEntity.completeEncounter(level, List.of(serverPlayer)),
                "First Station Mother completion should grant rewards");
        helper.assertTrue(StationfallProgress.get(player).blackboxRetrieved(),
                "Station Mother completion should mark blackbox retrieval");
        helper.assertTrue(StationfallStationState.get(level).blackboxRewarded(),
                "Station Mother completion should mark world reward state");
        helper.assertTrue(count(serverPlayer, ModItems.STATIONFALL_BLACKBOX.get()) == 1,
                "Station Mother should grant one Stationfall Blackbox");
        helper.assertFalse(ModEntities.StationMotherEntity.completeEncounter(level, List.of(serverPlayer)),
                "Repeated Station Mother completion should not grant duplicate rewards");
        helper.assertTrue(count(serverPlayer, ModItems.STATIONFALL_BLACKBOX.get()) == 1,
                "Repeated Station Mother completion should not duplicate the blackbox");
        helper.succeed();
    }

    private static void survivalItems(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StationfallSuitState suit = StationfallSuitState.get(player);
        suit.drainOxygen(80);
        suit.compromisePressure(70);
        suit.save(player);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMERGENCY_OXYGEN_PACK.get()));
        ModItems.EMERGENCY_OXYGEN_PACK.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(StationfallSuitState.get(player).oxygen() > 20,
                "Emergency Oxygen Pack should restore suit oxygen");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PRESSURE_SEAL_KIT.get()));
        ModItems.PRESSURE_SEAL_KIT.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(StationfallSuitState.get(player).pressure() > 30,
                "Pressure Seal Kit should restore suit pressure");

        SignalPanicState panic = SignalPanicState.get(player);
        panic.gain(player, 90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SIGNAL_PANIC_DAMPENER.get()));
        ModItems.SIGNAL_PANIC_DAMPENER.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SignalPanicState.get(player).value() < 90,
                "Signal Panic Dampener should reduce panic");
        helper.succeed();
    }

    private static boolean hasAction(TerminalMissionSnapshot snapshot, String action) {
        for (TerminalMissionAction terminalAction : snapshot.actions()) {
            if (terminalAction.id().equals(action) && terminalAction.enabled()) {
                return true;
            }
        }
        return false;
    }

    private static int count(Player player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String testName,
            Identifier functionId
    ) {
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
                2
        );
        event.registerTest(
                id(testName),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data)
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoStationfall.MODID, path);
    }
}
