package com.knoxhack.echonexusprotocol.test;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoHandoffs;
import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.ProtocolSealBlock;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.compat.jei.NexusJeiRecipeCatalog;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.entity.CorruptionWardenEntity;
import com.knoxhack.echonexusprotocol.entity.NexusGuardianEntity;
import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.event.NexusArmorEvents;
import com.knoxhack.echonexusprotocol.event.NexusWorldEvents;
import com.knoxhack.echonexusprotocol.integration.NexusFieldMapPlanner;
import com.knoxhack.echonexusprotocol.integration.NexusMachineCoreAdapter;
import com.knoxhack.echonexusprotocol.integration.NexusMachineCoreRuntimeProvider;
import com.knoxhack.echonexusprotocol.integration.NexusTerminalIds;
import com.knoxhack.echonexusprotocol.integration.NexusTerminalMissionProvider;
import com.knoxhack.echonexusprotocol.integration.NexusProgression;
import com.knoxhack.echonexusprotocol.item.NexusFieldChargeItem;
import com.knoxhack.echonexusprotocol.item.NexusScannerVisorItem;
import com.knoxhack.echonexusprotocol.item.NexusUtilityItem;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModEntities;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echonexusprotocol.world.ModDimensions;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import java.util.Set;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final String[] NEXUS_POIS = {
      "abandoned_nexus_field_station", "signal_relay_tower", "data_vault", "corruption_containment_lab", "blackbox_monolith", "nexus_core_chamber"
   };
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, "echonexusprotocol");
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY = TEST_FUNCTIONS.register(
      "energy_storage_transfer", () -> ModGameTests::energyStorageTransfer
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECYCLER = TEST_FUNCTIONS.register(
      "recycler_dirty_corruption", () -> ModGameTests::recyclerDirtyCorruption
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_RECIPES = TEST_FUNCTIONS.register(
      "machine_recipe_paths", () -> ModGameTests::machineRecipePaths
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STABILIZER = TEST_FUNCTIONS.register(
      "field_stabilizer", () -> ModGameTests::fieldStabilizer
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEAL = TEST_FUNCTIONS.register(
      "protocol_seal_modes", () -> ModGameTests::protocolSealModes
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION = TEST_FUNCTIONS.register(
      "progression_and_endings", () -> ModGameTests::progressionAndEndings
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL = TEST_FUNCTIONS.register(
      "terminal_mission_snapshots", () -> ModGameTests::terminalMissionSnapshots
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARMOR_LOCK = TEST_FUNCTIONS.register(
      "armor_emergency_field_lock", () -> ModGameTests::armorEmergencyFieldLock
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOBS = TEST_FUNCTIONS.register(
      "mob_signature_behaviors", () -> ModGameTests::mobSignatureBehaviors
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCANNER = TEST_FUNCTIONS.register(
      "scanner_research_unlocks", () -> ModGameTests::scannerResearchUnlocks
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_CHARGES = TEST_FUNCTIONS.register(
      "field_charges_rewrite_blocks", () -> ModGameTests::fieldChargesRewriteBlocks
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_STATE = TEST_FUNCTIONS.register(
      "world_quarantine_storm_tears", () -> ModGameTests::worldQuarantineStormTears
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERRAIN_SITES = TEST_FUNCTIONS.register(
      "terrain_landmark_coverage", () -> ModGameTests::terrainLandmarkCoverage
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UTILITY_GEAR = TEST_FUNCTIONS.register(
      "utility_gear_behaviors", () -> ModGameTests::utilityGearBehaviors
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POST_RELEASE_POLISH = TEST_FUNCTIONS.register(
      "post_release_polish_coverage", () -> ModGameTests::postReleasePolishCoverage
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_TELEMETRY = TEST_FUNCTIONS.register(
      "field_terminal_telemetry_sources", () -> ModGameTests::fieldTerminalTelemetrySources
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_MAP = TEST_FUNCTIONS.register(
      "field_map_telemetry_sources", () -> ModGameTests::fieldMapTelemetrySources
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECOVERY_TOOLS = TEST_FUNCTIONS.register(
      "collapsed_recovery_tools", () -> ModGameTests::collapsedRecoveryTools
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENDING_EFFECTS = TEST_FUNCTIONS.register(
      "ending_world_effects", () -> ModGameTests::endingWorldEffects
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELEASE_RISK_FIXES = TEST_FUNCTIONS.register(
      "release_risk_fixes", () -> ModGameTests::releaseRiskFixes
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESOURCE_COMPLETENESS = TEST_FUNCTIONS.register(
      "resource_completeness_release_gate", () -> ModGameTests::resourceCompletenessReleaseGate
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTER_PATH = TEST_FUNCTIONS.register(
      "terminal_starter_path", () -> ModGameTests::terminalStarterPath
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ONE_TIME_REWARDS = TEST_FUNCTIONS.register(
      "one_time_boss_reward_gates", () -> ModGameTests::oneTimeBossRewardGates
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STORM_SPAWN_CAP = TEST_FUNCTIONS.register(
      "storm_spawn_cap", () -> ModGameTests::stormSpawnCap
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JEI_JSON_PARITY = TEST_FUNCTIONS.register(
      "jei_json_recipe_parity", () -> ModGameTests::jeiJsonRecipeParity
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_DATA_CODEC = TEST_FUNCTIONS.register(
      "world_data_codec_round_trip", () -> ModGameTests::worldDataCodecRoundTrip
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRICT_PLAYABLE_STARTER_LOOP = TEST_FUNCTIONS.register(
      "strict_playable_starter_loop", () -> ModGameTests::strictPlayableStarterLoop
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLAYER_DATA_PERSISTENCE = TEST_FUNCTIONS.register(
      "nexus_player_data_persistence_round_trip", () -> ModGameTests::nexusPlayerDataPersistenceRoundTrip
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_PERSISTENCE = TEST_FUNCTIONS.register(
      "nexus_machine_persistence_round_trip", () -> ModGameTests::nexusMachinePersistenceRoundTrip
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_ACCESS_ROUTE = TEST_FUNCTIONS.register(
      "core_access_key_route", () -> ModGameTests::coreAccessKeyRoute
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_PLACEMENT = TEST_FUNCTIONS.register(
      "terminal_route_placement", () -> ModGameTests::terminalRoutePlacement
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_STATUS_STATES = TEST_FUNCTIONS.register(
      "machine_status_polish_states", () -> ModGameTests::machineStatusPolishStates
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_RUNTIME = TEST_FUNCTIONS.register("nexus_machinecore_runtime_snapshot_contract",
      () -> ModGameTests::nexusMachineCoreRuntimeSnapshotContract
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOSS_TELEGRAPHS = TEST_FUNCTIONS.register(
      "boss_telegraph_polish", () -> ModGameTests::bossTelegraphPolish
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOOT_POLISH = TEST_FUNCTIONS.register(
      "route_loot_polish_invariants", () -> ModGameTests::routeLootPolishInvariants
   );

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      if (!shouldRegisterTests()) {
         return;
      }
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("nexus_release_v1"), new TestEnvironmentDefinition[0]);
      register(event, environment, "energy_storage_transfer", ENERGY.getId());
      register(event, environment, "recycler_dirty_corruption", RECYCLER.getId());
      register(event, environment, "machine_recipe_paths", MACHINE_RECIPES.getId());
      register(event, environment, "field_stabilizer", STABILIZER.getId());
      register(event, environment, "protocol_seal_modes", SEAL.getId());
      register(event, environment, "progression_and_endings", PROGRESSION.getId());
      register(event, environment, "terminal_mission_snapshots", TERMINAL.getId());
      register(event, environment, "armor_emergency_field_lock", ARMOR_LOCK.getId());
      register(event, environment, "mob_signature_behaviors", MOBS.getId());
      register(event, environment, "scanner_research_unlocks", SCANNER.getId());
      register(event, environment, "field_charges_rewrite_blocks", FIELD_CHARGES.getId());
      register(event, environment, "world_quarantine_storm_tears", WORLD_STATE.getId());
      register(event, environment, "terrain_landmark_coverage", TERRAIN_SITES.getId());
      register(event, environment, "utility_gear_behaviors", UTILITY_GEAR.getId());
      register(event, environment, "post_release_polish_coverage", POST_RELEASE_POLISH.getId());
      register(event, environment, "field_terminal_telemetry_sources", FIELD_TELEMETRY.getId());
      register(event, environment, "field_map_telemetry_sources", FIELD_MAP.getId());
      register(event, environment, "collapsed_recovery_tools", RECOVERY_TOOLS.getId());
      register(event, environment, "ending_world_effects", ENDING_EFFECTS.getId());
      register(event, environment, "release_risk_fixes", RELEASE_RISK_FIXES.getId());
      register(event, environment, "resource_completeness_release_gate", RESOURCE_COMPLETENESS.getId());
      register(event, environment, "terminal_starter_path", STARTER_PATH.getId());
      register(event, environment, "one_time_boss_reward_gates", ONE_TIME_REWARDS.getId());
      register(event, environment, "storm_spawn_cap", STORM_SPAWN_CAP.getId());
      register(event, environment, "jei_json_recipe_parity", JEI_JSON_PARITY.getId());
      register(event, environment, "world_data_codec_round_trip", WORLD_DATA_CODEC.getId());
      register(event, environment, "strict_playable_starter_loop", STRICT_PLAYABLE_STARTER_LOOP.getId());
      register(event, environment, "nexus_player_data_persistence_round_trip", PLAYER_DATA_PERSISTENCE.getId());
      register(event, environment, "nexus_machine_persistence_round_trip", MACHINE_PERSISTENCE.getId());
      register(event, environment, "core_access_key_route", CORE_ACCESS_ROUTE.getId());
      register(event, environment, "terminal_route_placement", ROUTE_PLACEMENT.getId());
      register(event, environment, "machine_status_polish_states", MACHINE_STATUS_STATES.getId());
      register(event, environment, "nexus_machinecore_runtime_snapshot_contract", MACHINECORE_RUNTIME.getId());
      register(event, environment, "boss_telegraph_polish", BOSS_TELEGRAPHS.getId());
      register(event, environment, "route_loot_polish_invariants", LOOT_POLISH.getId());
   }

   private static void energyStorageTransfer(GameTestHelper helper) {
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, (Block)ModBlocks.NEXUS_CHARGE_TANK.get());
      NexusMachineBlockEntity tank = (NexusMachineBlockEntity)helper.getBlockEntity(pos, NexusMachineBlockEntity.class);
      tank.receiveCharge(1000);
      helper.assertTrue(tank.energyStored() == 1000, "Charge Tank should accept FE-backed Nexus Charge");
      helper.setBlock(new BlockPos(2, 1, 1), (Block)ModBlocks.NEXUS_CHARGE_TANK.get());
      NexusMachineBlockEntity.tick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
      helper.assertTrue(tank.energyStored() <= 1000, "Charge Tank transfer tick should preserve or move stored charge safely");
      helper.succeed();
   }

   private static void recyclerDirtyCorruption(GameTestHelper helper) {
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, (Block)ModBlocks.NEXUS_RECYCLER.get());
      NexusMachineBlockEntity recycler = (NexusMachineBlockEntity)helper.getBlockEntity(pos, NexusMachineBlockEntity.class);
      recycler.setItem(0, new ItemStack((ItemLike)ModItems.STATIC_FLUID.get()));

      for (int i = 0; i < 180; i++) {
         NexusMachineBlockEntity.tick(helper.getLevel(), recycler.getBlockPos(), recycler.getBlockState(), recycler);
      }

      helper.assertTrue(recycler.energyStored() > 0, "Recycler should create Nexus Charge from dirty Nexus inputs");
      helper.assertTrue(recycler.contamination() > 0, "Dirty input should contaminate the machine");
      helper.succeed();
   }

   private static void machineRecipePaths(GameTestHelper helper) {
      NexusMachineBlockEntity recycler = placeMachine(helper, new BlockPos(1, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      recycler.setItem(0, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get()));
      tickMachine(helper, recycler, 130);
      helper.assertTrue(recycler.energyStored() >= 700, "Recycler recipe should produce Nexus Charge");
      helper.assertTrue(recycler.getItem(1).is(ModItems.REALITY_DUST.get()), "Recycler should output Reality Dust from Nexus Shards");

      NexusMachineBlockEntity infuser = placeMachine(helper, new BlockPos(4, 1, 1), (Block)ModBlocks.NEXUS_INFUSER.get());
      infuser.receiveCharge(800);
      infuser.setItem(0, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get()));
      tickMachine(helper, infuser, 190);
      helper.assertTrue(infuser.getItem(1).is(ModItems.STABLE_NEXUS_CORE.get()), "Infuser should craft Stable Nexus Cores");
      helper.assertTrue(infuser.energyStored() < 800, "Infuser should spend charge on core infusion");

      NexusMachineBlockEntity decoder = placeMachine(helper, new BlockPos(7, 1, 1), (Block)ModBlocks.MEMORY_DECODER.get());
      decoder.receiveCharge(300);
      decoder.setItem(0, new ItemStack((ItemLike)ModItems.BLACKBOX_FRAGMENT.get()));
      tickMachine(helper, decoder, 170);
      helper.assertTrue(decoder.getItem(1).is(ModItems.MEMORY_SHARD.get()) && decoder.getItem(1).getCount() >= 2, "Memory Decoder should decode Blackbox Fragments");

      NexusMachineBlockEntity forge = placeMachine(helper, new BlockPos(10, 1, 1), (Block)ModBlocks.REALITY_FORGE.get());
      forge.receiveCharge(400);
      forge.setItem(0, new ItemStack((ItemLike)ModBlocks.DATA_CRACKED_STONE.get()));
      tickMachine(helper, forge, 210);
      helper.assertTrue(forge.getItem(1).is(ModBlocks.BLACKBOX_PLATE.get().asItem()), "Reality Forge should transmute Data-Cracked Stone");

      NexusMachineBlockEntity reactor = placeMachine(helper, new BlockPos(13, 1, 1), (Block)ModBlocks.CORRUPTION_REACTOR.get());
      reactor.setItem(0, new ItemStack((ItemLike)ModItems.STATIC_FLUID.get()));
      ChunkPos reactorChunk = new ChunkPos(reactor.getBlockPos().getX() >> 4, reactor.getBlockPos().getZ() >> 4);
      int pressureBefore = NexusWorldData.get(helper.getLevel()).corruptionPressure(reactorChunk);
      tickMachine(helper, reactor, 130);
      helper.assertTrue(reactor.energyStored() >= 900, "Corruption Reactor should generate high Nexus Charge from Static Fluid");
      helper.assertTrue(
         reactor.contamination() > 0 || NexusWorldData.get(helper.getLevel()).corruptionPressure(reactorChunk) > pressureBefore,
         "Corruption Reactor fuel should add machine contamination or local corruption pressure"
      );
      helper.succeed();
   }

   private static void nexusMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
      NexusMachineBlockEntity recycler = placeMachine(helper, new BlockPos(1, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      recycler.receiveCharge(128);
      recycler.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get(), 2));
      NexusMachineBlockEntity.tick(helper.getLevel(), recycler.getBlockPos(), recycler.getBlockState(), recycler);

      EchoMachineRuntimeSnapshot direct = NexusMachineCoreAdapter.runtimeSnapshot(recycler);
      helper.assertTrue(EchoNexusProtocol.id("nexus_recycler").toString().equals(direct.id().value()), "Snapshot id should use the real Nexus Recycler block id");
      helper.assertTrue(EchoNexusProtocol.MODID.equals(direct.ownerModule().value()), "Snapshot owner should be Nexus Protocol");
      helper.assertTrue(direct.kind() == EchoMachineKind.REFINERY, "Nexus Recycler should project as a refinery-style machine");
      helper.assertTrue(direct.state() == EchoMachineState.ACTIVE, "Recycler should be active after one processing tick");
      helper.assertTrue(direct.energy().stored() == 128 && direct.energy().capacity() == 16000, "Nexus Charge contract should expose stored charge and capacity");
      helper.assertTrue("Nexus Charge".equals(direct.energy().unit()), "Nexus energy unit should remain chapter-specific");
      helper.assertTrue(direct.inventory().totalSlots() == 2 && direct.inventory().occupiedSlots() == 1, "Nexus machine inventory should expose two slots with one occupied input");
      helper.assertTrue("input".equals(direct.inventory().slots().get(0).role()), "Input slot role should be explicit");
      helper.assertTrue(direct.process().active() && direct.process().progressTicks() > 0 && direct.process().maxProgressTicks() > 0, "Process contract should expose live progress");
      helper.assertTrue("echonexusprotocol:recycler_nexus_shard".equals(direct.process().recipeContract()), "Process contract should bind the real recycler recipe");
      helper.assertTrue("700".equals(direct.process().attributes().get("recipeChargeOutput")), "Recipe attributes should expose charge output");
      helper.assertTrue("1".equals(direct.process().attributes().get("recipeCorruptionDelta")), "Recipe attributes should expose corruption delta");
      helper.assertTrue(direct.savedState().persistedKeys().contains("nexusCharge"), "Saved state should include Nexus Charge");
      helper.assertTrue(direct.savedState().persistedKeys().contains("contamination"), "Saved state should include contamination");
      helper.assertTrue(EchoMachineUiBridge.hasAutomationSurface(direct), "Nexus machine should expose an automation surface");
      helper.assertTrue(direct.attributes().containsKey("position") && direct.attributes().containsKey("dimension"), "Snapshot should include world position metadata");

      NexusMachineCoreRuntimeProvider.register();
      EchoMachineRuntimeSnapshot registered = EchoMachineRuntimeRegistry.snapshot(helper.getLevel(), recycler.getBlockPos())
         .orElseThrow(() -> new AssertionError("Registered MachineCore snapshot missing for Nexus Recycler"));
      helper.assertTrue(registered.process().recipeContract().equals(direct.process().recipeContract()), "Registered provider snapshot should preserve recipe binding");
      helper.assertTrue(EchoMachineUiBridge.energyLine(registered).contains("Nexus Charge"), "UI bridge should render Nexus Charge units");

      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      BlockPos absolute = recycler.getBlockPos();
      player.teleportTo(helper.getLevel(), absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D, Set.of(), 0.0F, 0.0F, true);
      helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
         .anyMatch(snapshot -> EchoNexusProtocol.id("nexus_recycler").toString().equals(snapshot.id().value())), "Nearby player scan should discover Nexus Recycler snapshots");
      helper.assertTrue(EchoMachineRuntimeRegistry.profiles(player).stream()
         .flatMap(profile -> profile.recipeBindings().stream())
         .anyMatch(binding -> "echonexusprotocol:recycler_nexus_shard".equals(binding.recipeId().value())), "Nearby player profiles should expose the recycler recipe binding");
      helper.succeed();
   }

   private static void fieldStabilizer(GameTestHelper helper) {
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, (Block)ModBlocks.NEXUS_FIELD_STABILIZER.get());
      NexusMachineBlockEntity stabilizer = (NexusMachineBlockEntity)helper.getBlockEntity(pos, NexusMachineBlockEntity.class);
      stabilizer.receiveCharge(200);
      NexusWorldData data = NexusWorldData.get(helper.getLevel());
      ChunkPos chunk = new ChunkPos(stabilizer.getBlockPos().getX() >> 4, stabilizer.getBlockPos().getZ() >> 4);
      data.setFieldValue(chunk, 42);
      stabilizer.stabilizeField(helper.getLevel());
      helper.assertTrue(data.fieldValue(chunk) > 42, "Field Stabilizer should raise chunk field stability");
      helper.succeed();
   }

   private static void protocolSealModes(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
      NexusWorldData data = NexusWorldData.get(helper.getLevel());
      ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
      data.setFieldValue(chunk, 50);
      data.setCorruptionPressure(chunk, 20);
      BlockPos tankLocal = new BlockPos(2, 1, 1);
      BlockPos sourceLocal = new BlockPos(3, 1, 1);
      BlockPos chestLocal = new BlockPos(0, 1, 0);
      NexusMachineBlockEntity tank = placeMachine(helper, tankLocal, (Block)ModBlocks.NEXUS_CHARGE_TANK.get());
      NexusMachineBlockEntity sourceTank = placeMachine(helper, sourceLocal, (Block)ModBlocks.NEXUS_CHARGE_TANK.get());

      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.EXTRACT);
      helper.assertTrue(tank.energyStored() > 0 || sourceTank.energyStored() > 0, "Extract seal should move corruption into nearby tanks as charge");
      helper.assertTrue(data.corruptionPressure(chunk) < 20, "Extract seal should reduce local corruption pressure");

      tank.addContamination(12);
      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.REPAIR);
      helper.assertTrue(tank.contamination() < 12, "Repair seal should reduce nearby machine contamination");

      tank.receiveCharge(220);
      int targetBefore = sourceTank.energyStored();
      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.RELAY);
      helper.assertTrue(sourceTank.energyStored() > targetBefore, "Relay seal should move charge from a fuller machine to an emptier one");

      helper.setBlock(chestLocal, Blocks.CHEST);
      ItemEntity item = new ItemEntity(helper.getLevel(), pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, new ItemStack(Items.REDSTONE, 3));
      helper.getLevel().addFreshEntity(item);
      helper.runAfterDelay(3L, () -> {
         ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.COLLECT);
         Container chest = (Container)helper.getBlockEntity(chestLocal, net.minecraft.world.level.block.entity.BlockEntity.class);
         helper.assertTrue(containerHasAtLeast(chest, Items.REDSTONE, 3), "Collect seal should insert nearby item drops into inventories");
         finishProtocolSealModes(helper, pos, tank, data, chunk);
      });
   }

   private static boolean containerHasAtLeast(Container container, ItemLike item, int count) {
      int found = 0;
      for (int slot = 0; slot < container.getContainerSize(); slot++) {
         ItemStack stack = container.getItem(slot);
         if (stack.is(item.asItem())) {
            found += stack.getCount();
         }
      }
      return found >= count;
   }

   private static void finishProtocolSealModes(GameTestHelper helper, BlockPos pos, NexusMachineBlockEntity tank, NexusWorldData data, ChunkPos chunk) {
      BlockPos rewriteLocal = new BlockPos(1, 1, 2);
      helper.setBlock(rewriteLocal, (Block)ModBlocks.DATA_CRACKED_STONE.get());
      tank.receiveCharge(100);
      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.REWRITE);
      helper.assertBlockPresent((Block)ModBlocks.BLACKBOX_PLATE.get(), rewriteLocal);

      NexusMobEntity husk = (NexusMobEntity)((EntityType)ModEntities.NEXUS_HUSK.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(husk != null, "Nexus Husk should create for Defense seal test");
      if (husk != null) {
         husk.setPos(pos.getX() + 1.5D, pos.getY(), pos.getZ() + 1.5D);
         husk.setNoAi(true);
         helper.getLevel().addFreshEntity(husk);
         float health = husk.getHealth();
         ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.DEFENSE);
         helper.assertTrue(husk.getHealth() < health, "Defense seal should prioritize Nexus mobs");
      }

      data.setCorruptionPressure(chunk, 20);
      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.PURIFY);
      helper.assertTrue(data.corruptionPressure(chunk) < 20, "Purify seal should reduce corruption");
      data.setCorruptionPressure(chunk, 20);
      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.COLLAPSE);
      helper.assertTrue(data.corruptionPressure(chunk) >= 20, "Collapse seal should increase corruption for power");
      helper.succeed();
   }

   private static void progressionAndEndings(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.assertFalse(NexusProgression.isNexusUnlocked(player), "Nexus should be gated before Stationfall/dev unlock");
      if (player instanceof ServerPlayer serverPlayer) {
         EchoCoreServices.recordMilestone(serverPlayer, "stationfall.blackbox_retrieved");
         helper.assertTrue(NexusProgression.isNexusUnlocked(player), "Legacy Stationfall blackbox milestone should satisfy Nexus gate");
         NexusProgression.grantDevelopmentUnlock(serverPlayer);
         helper.assertTrue(NexusProgression.isNexusUnlocked(player), "Development unlock should satisfy Nexus gate");
      }

      NexusPlayerData data = NexusPlayerData.get(player);
      data.activateBlackboxMonolith();
      data.setEndingPath("restore");
      helper.assertTrue(data.blackboxMonolithActivated(), "Blackbox Monolith activation should persist in player data");
      helper.assertTrue(data.hasEndingPath(), "Ending path should persist in player data");
      helper.succeed();
   }

   private static void terminalMissionSnapshots(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      var locked = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("the_signal_beneath"));
      helper.assertTrue(locked.status() == TerminalMissionStatus.LOCKED, "Signal mission should explain the Stationfall handoff before Nexus unlock");
      helper.assertTrue(locked.unlockReason().contains(EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED), "Locked signal mission should name the required handoff milestone");
      if (player instanceof ServerPlayer serverPlayer) {
         NexusProgression.grantDevelopmentUnlock(serverPlayer);
      }

      NexusPlayerData data = NexusPlayerData.get(player);
      data.markGearUsed("nexus_scanner_visor");
      TerminalMissionStatus signalStatus = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("the_signal_beneath")).status();
      helper.assertTrue(
         signalStatus == TerminalMissionStatus.CLAIMABLE || signalStatus == TerminalMissionStatus.CLAIMED,
         "Signal mission should complete from an actual scanner use"
      );

      data.markMachineUsed("nexus_recycler");
      data.markMachineUsed("nexus_field_stabilizer");
      data.markMachineUsed("corruption_filter");
      data.markMachineUsed("memory_decoder");
      data.addBlackboxFragment();
      data.addBlackboxFragment();
      data.addBlackboxFragment();
      data.markWardenDefeated();
      data.activateBlackboxMonolith();
      data.markMachineUsed("reality_forge");
      data.markCoreEntered();
      data.markGuardianDefeated();
      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("what_rebuilds_the_world")).status() == TerminalMissionStatus.UNLOCKED,
         "Final mission should expose ending actions after Guardian defeat but before a path is committed"
      );
      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("what_rebuilds_the_world")).actions().stream().filter(action -> action.enabled() && action.id().startsWith("choose_")).count() == 4L,
         "Final mission should expose all four ending actions after Guardian defeat"
      );

      if (player instanceof ServerPlayer serverPlayer) {
         helper.assertTrue(
            NexusTerminalMissionProvider.INSTANCE.handleAction(serverPlayer, NexusTerminalIds.id("what_rebuilds_the_world"), "choose_restore"),
            "Final mission action should commit a Nexus path"
         );
         helper.assertTrue(
            EchoCoreServices.progressLedger(serverPlayer).hasMilestone(EchoHandoffs.NEXUS_PROTOCOL_COMPLETE),
            "Final Nexus path should record the canonical Nexus completion handoff"
         );
      } else {
         data.setEndingPath("restore");
      }
      var completed = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("what_rebuilds_the_world"));
      helper.assertTrue(
         completed.status() == TerminalMissionStatus.CLAIMABLE,
         "Final mission should complete after exactly one ending path is stored"
      );
      helper.assertTrue(completed.actionHint().contains("Claim"), "Completed final mission should direct the player to claim the support cache");
      helper.succeed();
   }

   private static void terminalRoutePlacement(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      if (player instanceof ServerPlayer serverPlayer) {
         NexusProgression.grantDevelopmentUnlock(serverPlayer);
      }

      var missions = NexusTerminalMissionProvider.INSTANCE.missions(player);
      helper.assertTrue(missions.size() == 10, "Nexus route placement should cover every Nexus mission");
      for (var definition : missions) {
         var snapshot = NexusTerminalMissionProvider.INSTANCE.snapshot(player, definition.id());
         var role = NexusTerminalMissionProvider.INSTANCE.role(player, definition, snapshot);
         TerminalMissionRoutePlacement placement = NexusTerminalMissionProvider.INSTANCE.routePlacement(player, definition, snapshot, role).orElse(null);
         helper.assertTrue(placement != null, "Nexus mission should expose Survival Route placement: " + definition.id());
         int expectedPhase = definition.id().getPath().equals("the_core_door") || definition.id().getPath().equals("what_rebuilds_the_world") ? 14 : 13;
         helper.assertTrue(placement.phaseOrder() == expectedPhase, "Nexus mission should map into the expected late shared route phase: " + definition.id());
         helper.assertTrue(placement.includeInSurvivalRoute(), "Nexus mission should be visible in the shared Survival Route: " + definition.id());
      }
      helper.succeed();
   }

   private static void armorEmergencyFieldLock(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      if (!(player instanceof ServerPlayer serverPlayer)) {
         helper.succeed();
         return;
      }

      serverPlayer.setItemSlot(EquipmentSlot.HEAD, new ItemStack((ItemLike)ModItems.NEXUS_HELMET.get()));
      serverPlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack((ItemLike)ModItems.NEXUS_CHESTPLATE.get()));
      serverPlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack((ItemLike)ModItems.NEXUS_LEGGINGS.get()));
      serverPlayer.setItemSlot(EquipmentSlot.FEET, new ItemStack((ItemLike)ModItems.NEXUS_BOOTS.get()));
      serverPlayer.getInventory().add(new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get()));
      serverPlayer.setHealth(1.0F);

      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      ChunkPos chunk = serverPlayer.chunkPosition();
      worldData.setFieldValue(chunk, 42);
      worldData.setCorruptionPressure(chunk, 30);
      helper.assertTrue(NexusArmorEvents.tryEmergencyFieldLock(serverPlayer), "Full Nexus armor should spend a shard to prevent lethal corruption damage");
      NexusPlayerData data = NexusPlayerData.get(player);
      helper.assertTrue(data.armorLockCooldown() == 2400, "Emergency Field Lock should set the configured cooldown");
      helper.assertTrue(data.hasUsedGear("nexus_armor_emergency_lock"), "Emergency Field Lock should record gear mission progress");
      helper.assertTrue(worldData.fieldValue(chunk) > 42, "Emergency Field Lock should stabilize the local field");
      helper.assertFalse(NexusArmorEvents.tryEmergencyFieldLock(serverPlayer), "Emergency Field Lock should not fire while on cooldown");
      helper.succeed();
   }

   private static void mobSignatureBehaviors(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.getInventory().add(new ItemStack((ItemLike)ModItems.MEMORY_SHARD.get()));
      // Keep the Warden pulse away from neighboring full-stack mob-cap tests.
      BlockPos pos = helper.absolutePos(new BlockPos(2, 32, 2));

      NexusMobEntity archiveSeeker = (NexusMobEntity)((EntityType)ModEntities.ARCHIVE_SEEKER.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(archiveSeeker != null, "Archive Seeker should create");
      if (archiveSeeker != null) {
         int before = countItem(player, (ItemLike)ModItems.MEMORY_SHARD.get());
         helper.assertFalse(com.knoxhack.echonexusprotocol.entity.ArchiveSeekerEntity.stealMemoryItem(player).isEmpty(), "Archive Seeker should find memory items");
         helper.assertTrue(countItem(player, (ItemLike)ModItems.MEMORY_SHARD.get()) == before - 1, "Archive Seeker should steal memory items");
      }

      NexusMobEntity warden = (NexusMobEntity)((EntityType)ModEntities.CORRUPTION_WARDEN.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(warden != null, "Corruption Warden should create");
      if (warden != null) {
         ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
         NexusWorldData data = NexusWorldData.get(helper.getLevel());
         data.setCorruptionPressure(chunk, 0);
         warden.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
         warden.tickCount = 99;
         helper.getLevel().addFreshEntity(warden);
         if (warden instanceof com.knoxhack.echonexusprotocol.entity.CorruptionWardenEntity corruptionWarden) {
            corruptionWarden.pulse(helper.getLevel());
         }
         helper.assertTrue(data.corruptionPressure(warden.chunkPosition()) >= 4, "Corruption Warden should pulse corruption into the chunk");
      }

      NexusMobEntity guardian = (NexusMobEntity)((EntityType)ModEntities.NEXUS_GUARDIAN.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(guardian instanceof com.knoxhack.echonexusprotocol.entity.NexusGuardianEntity, "Nexus Guardian should use the role-specific class");
      if (guardian instanceof com.knoxhack.echonexusprotocol.entity.NexusGuardianEntity nexusGuardian) {
         nexusGuardian.setHealth(nexusGuardian.getMaxHealth() * 0.2F);
         helper.assertTrue(nexusGuardian.phase() == 4, "Nexus Guardian should enter phase 4 below 25% health");
      }
      helper.succeedWhen(() -> {
         helper.assertTrue(
            !helper.getLevel().getEntitiesOfClass(NexusMobEntity.class, new AABB(pos).inflate(5.0D), mob -> mob.getType() == ModEntities.STATIC_CRAWLER.get()).isEmpty(),
            "Corruption Warden should summon Static Crawlers after pulsing"
         );
      });
   }

   private static void scannerResearchUnlocks(GameTestHelper helper) {
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, (Block)ModBlocks.REALITY_FORGE.get());
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      NexusScannerVisorItem.scanBlock(player, helper.absolutePos(pos));
      NexusPlayerData data = NexusPlayerData.get(player);
      helper.assertTrue(data.hasResearch("nexus_theory"), "Scanner should unlock Nexus Theory");
      helper.assertTrue(data.hasResearch("matter_rewriting"), "Scanning Reality Forge should unlock Matter Rewriting");
      helper.assertTrue(data.scanCount() > 0, "Scanner should record scanned block ids");
      helper.succeed();
   }

   private static void bossTelegraphPolish(GameTestHelper helper) {
      CorruptionWardenEntity warden = ModEntities.CORRUPTION_WARDEN.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      NexusGuardianEntity guardian = ModEntities.NEXUS_GUARDIAN.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(warden != null, "Corruption Warden should spawn for telegraph coverage");
      helper.assertTrue(guardian != null, "Nexus Guardian should spawn for telegraph coverage");
      if (warden == null || guardian == null) {
         helper.fail("Boss telegraph test entities failed to spawn");
         return;
      }

      BlockPos wardenPos = helper.absolutePos(new BlockPos(1, 1, 1));
      BlockPos guardianPos = helper.absolutePos(new BlockPos(4, 1, 1));
      warden.setPos(wardenPos.getX() + 0.5D, wardenPos.getY(), wardenPos.getZ() + 0.5D);
      guardian.setPos(guardianPos.getX() + 0.5D, guardianPos.getY(), guardianPos.getZ() + 0.5D);
      helper.getLevel().addFreshEntity(warden);
      helper.getLevel().addFreshEntity(guardian);
      guardian.setHealth(guardian.getMaxHealth() * 0.2F);
      helper.runAfterDelay(5L, () -> {
         // GameTest servers can cull hostile mobs before long waits; call the existing boss feedback routines directly.
         warden.tickCount = 80;
         invokeBossFeedbackMethod(helper, warden, "telegraphPulse", new Class<?>[] {net.minecraft.server.level.ServerLevel.class}, helper.getLevel());
         warden.pulse(helper.getLevel());
         guardian.tickCount = 20;
         invokeBossFeedbackMethod(helper, guardian, "telegraphPhaseIfChanged", new Class<?>[] {net.minecraft.server.level.ServerLevel.class, int.class}, helper.getLevel(), 4);
         helper.assertTrue(warden.pulseCount() > 0, "Corruption Warden should still pulse corruption pressure");
         helper.assertTrue(warden.telegraphCount() > 0 && warden.lastTelegraphTick() >= 80, "Corruption Warden should telegraph before a major pulse");
         helper.assertTrue(guardian.lastTelegraphPhase() == 4, "Nexus Guardian should telegraph the current low-health phase");
         helper.assertTrue(guardian.phaseTelegraphCount() > 0, "Nexus Guardian phase changes should be visibly telegraphed");
         helper.assertTrue(guardian.lastHazardPulseTick() > 0, "Nexus Guardian should record hazard pulses for phase readability");
         helper.succeed();
      });
   }

   private static void invokeBossFeedbackMethod(GameTestHelper helper, Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) {
      try {
         var method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
         method.setAccessible(true);
         method.invoke(target, arguments);
      } catch (ReflectiveOperationException exception) {
         helper.fail("Boss telegraph test could not invoke " + methodName + ": " + exception.getMessage());
      }
   }

   private static void fieldChargesRewriteBlocks(GameTestHelper helper) {
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, Blocks.DIRT);
      ChunkPos chunk = new ChunkPos(helper.absolutePos(pos).getX() >> 4, helper.absolutePos(pos).getZ() >> 4);
      NexusWorldData data = NexusWorldData.get(helper.getLevel());
      data.setFieldValue(chunk, 70);
      data.setCorruptionPressure(chunk, 0);
      NexusFieldChargeItem.apply(helper.getLevel(), helper.absolutePos(pos), NexusFieldChargeItem.Mode.COLLAPSE);
      helper.assertBlockPresent((Block)ModBlocks.FRAGMENTED_SOIL.get(), pos);
      helper.assertTrue(data.corruptionPressure(chunk) >= 18, "Collapse Charge should raise corruption pressure");
      NexusFieldChargeItem.apply(helper.getLevel(), helper.absolutePos(pos), NexusFieldChargeItem.Mode.PURITY);
      helper.assertBlockPresent(Blocks.DIRT, pos);
      helper.assertTrue(data.fieldValue(chunk) >= 68, "Purity Charge should restore field stability after a collapse pulse");
      data.setFieldValue(chunk, 10);
      data.setCorruptionPressure(chunk, 80);
      helper.setBlock(pos, (Block)ModBlocks.FRAGMENTED_SOIL.get());
      NexusFieldChargeItem.apply(helper.getLevel(), helper.absolutePos(pos), NexusFieldChargeItem.Mode.STABILIZED_PURITY);
      helper.assertBlockPresent(Blocks.DIRT, pos);
      helper.assertTrue(data.fieldValue(chunk) >= 26, "Stabilized Purity Charge should recover collapsed field stability");
      helper.assertTrue(data.corruptionPressure(chunk) <= 48, "Stabilized Purity Charge should sharply reduce corruption pressure");
      helper.succeed();
   }

   private static void worldQuarantineStormTears(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
      ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
      NexusWorldData data = NexusWorldData.get(helper.getLevel());
      data.setFieldValue(chunk, 30);
      data.setCorruptionPressure(chunk, 40);
      ProtocolSealBlock.applySeal(helper.getLevel(), pos, ProtocolSealBlock.SealMode.QUARANTINE);
      helper.assertTrue(data.isQuarantined(chunk), "Quarantine seal should mark the chunk as quarantined");
      data.tickAffectedChunk(helper.getLevel(), chunk);
      helper.assertFalse(data.hasActiveStorm(chunk, helper.getLevel().getGameTime(), 200L), "Quarantined chunks should not start anomaly storms");
      data.quarantineChunk(chunk, 0);
      data.setFieldValue(chunk, 12);
      data.setCorruptionPressure(chunk, 80);
      data.tickAffectedChunk(helper.getLevel(), chunk);
      helper.assertTrue(data.hasActiveStorm(chunk, helper.getLevel().getGameTime(), 200L), "Collapsed chunks should start anomaly storms");
      helper.assertTrue(data.realityTearCount(chunk) > 0, "Collapsed chunks should register reality tear activity");
      helper.succeed();
   }

   private static void terrainLandmarkCoverage(GameTestHelper helper) {
      for (String site : NEXUS_POIS) {
         assertResourcePresent(helper, "data/echonexusprotocol/worldgen/structure/" + site + ".json");
         assertResourcePresent(helper, "data/echonexusprotocol/worldgen/structure_set/" + site + ".json");
         assertResourcePresent(helper, "data/echonexusprotocol/worldgen/template_pool/" + site + ".json");
         assertResourcePresent(helper, "data/echonexusprotocol/structure/" + site + ".nbt");
         assertResourceMinSize(helper, "data/echonexusprotocol/structure/" + site + ".nbt", site.contains("core") || site.contains("monolith") ? 9000 : 3000);
      }
      helper.succeed();
   }

   private static void assertResourcePresent(GameTestHelper helper, String path) {
      helper.assertTrue(ModGameTests.class.getClassLoader().getResource(path) != null, "Missing Nexus structure resource: " + path);
   }

   private static void assertResourceMinSize(GameTestHelper helper, String path, int minBytes) {
      try (java.io.InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
         helper.assertTrue(input != null, "Missing Nexus resource: " + path);
         helper.assertTrue(input.readAllBytes().length >= minBytes, "Expanded Nexus resource is too small: " + path);
      } catch (java.io.IOException exception) {
         helper.fail("Unable to read Nexus resource: " + path + " / " + exception.getMessage());
      }
   }

   private static NexusMachineBlockEntity placeMachine(GameTestHelper helper, BlockPos pos, Block block) {
      helper.setBlock(pos, block);
      return (NexusMachineBlockEntity)helper.getBlockEntity(pos, NexusMachineBlockEntity.class);
   }

   private static void tickMachine(GameTestHelper helper, NexusMachineBlockEntity machine, int ticks) {
      for (int i = 0; i < ticks; i++) {
         NexusMachineBlockEntity.tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
      }
   }

   private static NexusMachineBlockEntity.MachineStatus status(NexusMachineBlockEntity machine) {
      return NexusMachineBlockEntity.MachineStatus.byId(machine.data().get(NexusMachineBlockEntity.DATA_STATUS));
   }

   private static int countItem(Player player, ItemLike item) {
      int count = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item.asItem())) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private static void postReleasePolishCoverage(GameTestHelper helper) {
      helper.assertTrue(NexusJeiRecipeCatalog.all().size() >= 18, "JEI catalog should cover every Nexus processing recipe path");
      helper.assertTrue((Integer)Config.MACHINE_DURATION_PERCENT.get() <= 100, "Machine duration tuning should remain responsive for beta");
      helper.assertTrue((Integer)Config.MACHINE_CHARGE_COST_PERCENT.get() <= 100, "Machine charge costs should stay approachable for beta onboarding");
      helper.assertTrue((Integer)Config.STABILIZER_FIELD_GAIN.get() >= 4, "Field Stabilizer should visibly improve a base chunk during beta play");
      helper.assertTrue((Integer)Config.SEAL_RADIUS.get() >= 5, "Protocol Seal radius config should expose beta tuning");
      helper.assertTrue((Integer)Config.SEAL_TICK_INTERVAL.get() <= 40, "Protocol Seals should tick fast enough to feel useful in beta");
      helper.assertTrue((Integer)Config.BOSS_HEALTH_PERCENT.get() >= 100, "Boss health multiplier should keep beta bosses threatening");
      helper.assertTrue((Integer)Config.BOSS_DAMAGE_PERCENT.get() >= 100, "Boss damage multiplier should keep beta bosses threatening");
      helper.assertTrue((Integer)Config.DUNGEON_LOOT_SCALE_PERCENT.get() >= 100, "Dungeon loot scale should make Nexus POIs feel worth entering");
      helper.assertTrue("standard".equals(Config.BALANCE_PRESET.get()), "Default public balance preset should be standard");
      helper.assertTrue((Integer)Config.FIELD_MAP_RADIUS.get() == 2, "Field map should show the public-release 5x5 grid by default");
      helper.assertTrue((Integer)Config.STABILIZED_PURITY_FIELD_GAIN.get() >= 12, "Stabilized Purity Charge should be strong enough for collapsed chunk recovery");
      helper.assertTrue((Integer)Config.FIELD_ANCHOR_TICKS.get() >= 1200, "Field Anchor should provide a meaningful recovery window");
      for (String sound : new String[]{"machine_process", "seal_activate", "field_stabilize", "corruption_leak", "monolith_activate", "reality_tear_pulse", "warden_pulse", "guardian_phase", "ending_choice"}) {
         assertResourcePresent(helper, "assets/echonexusprotocol/sounds/" + sound + ".ogg");
      }
      assertResourcePresent(helper, "assets/echonexusprotocol/sounds.json");
      assertResourcePresent(helper, "assets/echonexusprotocol/textures/entity/nexus_guardian.png");
      assertResourcePresent(helper, "assets/echonexusprotocol/textures/block/blackbox_monolith_core.png");
      helper.succeed();
   }

   private static void machineStatusPolishStates(GameTestHelper helper) {
      NexusMachineBlockEntity badInput = placeMachine(helper, new BlockPos(1, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      badInput.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack(Items.DIRT));
      tickMachine(helper, badInput, 1);
      helper.assertTrue(status(badInput) == NexusMachineBlockEntity.MachineStatus.BAD_INPUT, "Invalid machine input should expose BAD_INPUT status");

      NexusMachineBlockEntity noCharge = placeMachine(helper, new BlockPos(3, 1, 1), (Block)ModBlocks.NEXUS_INFUSER.get());
      noCharge.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get()));
      tickMachine(helper, noCharge, 1);
      helper.assertTrue(status(noCharge) == NexusMachineBlockEntity.MachineStatus.CHARGING, "Charge-gated machine should expose CHARGING status");

      NexusMachineBlockEntity blocked = placeMachine(helper, new BlockPos(5, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      blocked.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get()));
      blocked.setItem(NexusMachineBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIRT));
      tickMachine(helper, blocked, 1);
      helper.assertTrue(status(blocked) == NexusMachineBlockEntity.MachineStatus.OUTPUT_BLOCKED, "Blocked machine output should expose OUTPUT_BLOCKED status");

      NexusMachineBlockEntity complete = placeMachine(helper, new BlockPos(7, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      complete.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get()));
      int recyclerTicks = Math.max(1, Math.round(120 * (Integer)Config.MACHINE_DURATION_PERCENT.get() / 100.0F));
      tickMachine(helper, complete, recyclerTicks);
      helper.assertTrue(status(complete) == NexusMachineBlockEntity.MachineStatus.COMPLETE, "Completed machine cycle should expose COMPLETE status before the next cycle");

      NexusMachineBlockEntity leaking = placeMachine(helper, new BlockPos(9, 1, 1), (Block)ModBlocks.CORRUPTION_FILTER.get());
      leaking.addContamination(80);
      helper.runAfterDelay(45L, () -> {
         helper.assertTrue(status(leaking) == NexusMachineBlockEntity.MachineStatus.LEAKING, "Dirty unpowered filter should expose LEAKING status");
         helper.succeed();
      });
   }

   private static void fieldMapTelemetrySources(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      if (!(player instanceof ServerPlayer serverPlayer)) {
         helper.succeed();
         return;
      }

      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      NexusPlayerData playerData = NexusPlayerData.get(player);
      ChunkPos center = serverPlayer.chunkPosition();
      int[] values = {86, 65, 45, 25, 12};
      for (int i = 0; i < values.length; i++) {
         worldData.setFieldValue(new ChunkPos(center.x() - 2 + i, center.z()), values[i]);
      }
      worldData.setCorruptionPressure(new ChunkPos(center.x(), center.z()), 44);
      worldData.startAnomalyStorm(new ChunkPos(center.x() + 2, center.z()), helper.getLevel().getGameTime());
      playerData.refreshFieldTelemetry(serverPlayer);
      int rowStart = NexusPlayerData.FIELD_MAP_RADIUS * NexusPlayerData.FIELD_MAP_DIAMETER;
      for (int i = 0; i < values.length; i++) {
         helper.assertTrue(playerData.telemetryMapField(rowStart + i) == values[i], "Field map should sync nearby chunk field values");
      }
      helper.assertTrue(playerData.telemetryMapCorruption(rowStart + NexusPlayerData.FIELD_MAP_RADIUS) >= 44, "Field map should sync chunk corruption pressure");
      helper.assertTrue(playerData.telemetryMapStorm(rowStart + 4), "Field map should mark active storms");
      helper.assertTrue(playerData.telemetryMapTears(rowStart + 4) > 0, "Field map should mark reality tears");
      int centerIndex = rowStart + NexusPlayerData.FIELD_MAP_RADIUS;
      int stormIndex = rowStart + 4;
      NexusFieldMapPlanner.Analysis analysis = NexusFieldMapPlanner.analyze(playerData);
      helper.assertTrue(analysis.center().index() == centerIndex, "Field map planner should identify the center chunk deterministically");
      int expectedCenterRisk = (100 - playerData.telemetryMapField(centerIndex)) + playerData.telemetryMapCorruption(centerIndex);
      helper.assertTrue(analysis.center().risk() == expectedCenterRisk, "Field map planner should score local field and corruption risk");
      int expectedStormRisk = (100 - playerData.telemetryMapField(stormIndex))
         + playerData.telemetryMapCorruption(stormIndex)
         + NexusFieldMapPlanner.STORM_RISK_PENALTY
         + playerData.telemetryMapTears(stormIndex) * NexusFieldMapPlanner.TEAR_RISK_PENALTY;
      helper.assertTrue(analysis.cell(stormIndex).risk() == expectedStormRisk, "Field map planner should score storm and tear risk");
      helper.assertTrue(analysis.highestRisk().index() == stormIndex, "Field map planner should identify the highest-risk recovery cell");
      helper.assertTrue(
         analysis.safestAdjacent().index() == centerIndex - NexusPlayerData.FIELD_MAP_DIAMETER,
         "Field map planner should tie-break equally safe cardinal cells toward north"
      );
      helper.assertTrue(
         analysis.collapsedCells() >= 1 && analysis.stormCells() == 1 && analysis.tearCells() == 1,
         "Field map planner should summarize collapsed, storm, and tear cells"
      );
      helper.assertTrue(
         analysis.safestAdjacentGuidance().contains("Field Stabilizer"),
         "Safest adjacent guidance should name a route-safe recovery tool"
      );
      helper.assertTrue(
         analysis.priorityRecoveryGuidance().contains("Field Anchor") && analysis.priorityRecoveryGuidance().contains("Stabilized Purity Charge"),
         "Priority recovery guidance should name collapsed-chunk recovery tools"
      );
      helper.succeed();
   }

   private static void collapsedRecoveryTools(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      BlockPos absolute = helper.absolutePos(local);
      helper.setBlock(local, (Block)ModBlocks.FRAGMENTED_SOIL.get());
      ChunkPos chunk = new ChunkPos(absolute.getX() >> 4, absolute.getZ() >> 4);
      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      worldData.setFieldValue(chunk, 8);
      worldData.setCorruptionPressure(chunk, 90);
      NexusFieldChargeItem.apply(helper.getLevel(), absolute, NexusFieldChargeItem.Mode.STABILIZED_PURITY);
      helper.assertBlockPresent(Blocks.DIRT, local);
      helper.assertTrue(worldData.fieldValue(chunk) > 8, "Stabilized Purity Charge should raise collapsed field value");
      helper.assertTrue(worldData.corruptionPressure(chunk) < 90, "Stabilized Purity Charge should lower collapsed corruption pressure");
      NexusUtilityItem.anchorField(helper.getLevel(), absolute);
      helper.assertTrue(worldData.isQuarantined(chunk), "Field Anchor should quarantine collapsed chunks");
      helper.assertTrue(worldData.fieldValue(chunk) >= 28, "Field Anchor should make collapsed recovery visibly safer");
      helper.succeed();
   }

   private static void endingWorldEffects(GameTestHelper helper) {
      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      worldData.commitEndingState("");
      ChunkPos destroyChunk = new ChunkPos(4, 4);
      worldData.setFieldValue(destroyChunk, 20);
      worldData.setCorruptionPressure(destroyChunk, 70);
      worldData.startAnomalyStorm(destroyChunk, helper.getLevel().getGameTime());
      worldData.setEndingState("destroy");
      helper.assertTrue(worldData.corruptionPressure(destroyChunk) == 0, "Destroy ending should stop tracked corruption pressure");
      helper.assertFalse(worldData.hasActiveStorm(destroyChunk, helper.getLevel().getGameTime(), 400L), "Destroy ending should clear active storms");
      helper.assertTrue(worldData.realityTearCount(destroyChunk) == 0, "Destroy ending should close tracked tears");
      int fieldAfterDestroy = worldData.fieldValue(destroyChunk);
      worldData.setEndingState("destroy");
      helper.assertTrue(worldData.fieldValue(destroyChunk) == fieldAfterDestroy, "Ending world feedback should not stack when repeated");

      ChunkPos mergeChunk = new ChunkPos(5, 5);
      worldData.setEndingState("");
      worldData.setFieldValue(mergeChunk, 70);
      worldData.setCorruptionPressure(mergeChunk, 5);
      worldData.setEndingState("merge");
      helper.assertTrue(worldData.realityTearCount(mergeChunk) > 0, "Merge ending should make tears usable as a persistent world effect");
      helper.assertTrue(worldData.corruptionPressure(mergeChunk) >= 13, "Merge ending should embrace extra corruption pressure");
      helper.succeed();
   }

   private static void resourceCompletenessReleaseGate(GameTestHelper helper) {
      String lang = readResource("assets/echonexusprotocol/lang/en_us.json");
      String[] blocks = {
         "fragmented_soil",
         "data_cracked_stone",
         "signal_burned_grass",
         "glassed_dust",
         "hollow_signal_wood",
         "dead_signal_leaves",
         "corrupted_ferrite_ore",
         "blackbox_deepslate",
         "static_fluid_block",
         "nexus_crystal_cluster",
         "blackbox_plate",
         "signal_glass",
         "core_brick",
         "core_glass_block",
         "white_signal_log",
         "white_signal_leaves",
         "blackbox_monolith_core",
         "reality_tear",
         "nexus_recycler",
         "nexus_charge_tank",
         "corruption_filter",
         "nexus_field_stabilizer",
         "nexus_infuser",
         "memory_decoder",
         "reality_forge",
         "corruption_reactor",
         "protocol_seal"
      };
      for (String block : blocks) {
         assertResourcePresent(helper, "assets/echonexusprotocol/blockstates/" + block + ".json");
         assertResourcePresent(helper, "assets/echonexusprotocol/items/" + block + ".json");
         assertResourcePresent(helper, "assets/echonexusprotocol/models/block/" + block + ".json");
         assertResourcePresent(helper, "assets/echonexusprotocol/textures/block/" + block + ".png");
         assertResourcePresent(helper, "data/echonexusprotocol/loot_table/blocks/" + block + ".json");
         helper.assertTrue(lang.contains("\"block.echonexusprotocol." + block + "\""), "Missing block lang key: " + block);
      }

      String[] items = {
         "nexus_shard",
         "stable_nexus_core",
         "blackbox_fragment",
         "corrupted_ferrite",
         "static_fluid",
         "white_signal_bark",
         "nexus_gel",
         "reality_dust",
         "field_membrane",
         "core_glass",
         "signal_wire",
         "filter_membrane",
         "stabilized_alloy",
         "echo_crystal_dust",
         "clean_resonance_battery",
         "memory_shard",
         "data_fragment",
         "reactor_core",
         "core_access_key",
         "core_key_assembly",
         "nexus_scanner_visor",
         "nexus_pickaxe",
         "signal_blade",
         "reality_anchor",
         "field_anchor",
         "purity_charge",
         "stabilized_purity_charge",
         "collapse_charge",
         "nexus_helmet",
         "nexus_chestplate",
         "nexus_leggings",
         "nexus_boots"
      };
      for (String item : items) {
         assertResourcePresent(helper, "assets/echonexusprotocol/items/" + item + ".json");
         assertResourcePresent(helper, "assets/echonexusprotocol/models/item/" + item + ".json");
         assertResourcePresent(helper, "assets/echonexusprotocol/textures/item/" + item + ".png");
         helper.assertTrue(lang.contains("\"item.echonexusprotocol." + item + "\""), "Missing item lang key: " + item);
      }

      for (String entity : new String[]{"nexus_husk", "data_wraith", "static_crawler", "core_soldier", "archive_seeker", "corruption_warden", "nexus_guardian"}) {
         assertResourcePresent(helper, "assets/echonexusprotocol/textures/entity/" + entity + ".png");
         assertResourcePresent(helper, "data/echonexusprotocol/loot_table/entities/" + entity + ".json");
         helper.assertTrue(lang.contains("\"entity.echonexusprotocol." + entity + "\""), "Missing entity lang key: " + entity);
      }
      helper.succeed();
   }

   private static void terminalStarterPath(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      if (!(player instanceof ServerPlayer serverPlayer)) {
         helper.succeed();
         return;
      }

      var locked = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("the_signal_beneath"));
      helper.assertTrue(locked.status() == TerminalMissionStatus.LOCKED, "First Nexus mission should be locked before Stationfall/dev unlock");
      NexusProgression.grantDevelopmentUnlock(serverPlayer);
      var unlocked = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("the_signal_beneath"));
      helper.assertTrue(unlocked.status() == TerminalMissionStatus.UNLOCKED, "First Nexus mission should unlock from development handoff");
      helper.assertTrue(
         unlocked.actions().stream().anyMatch(action -> action.enabled() && "scan".equals(action.id())),
         "Unlocked first mission should expose exactly the starter scan route"
      );

      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.handleAction(serverPlayer, NexusTerminalIds.id("the_signal_beneath"), "scan"),
         "SCAN FIELD should be a handled starter action"
      );
      NexusPlayerData data = NexusPlayerData.get(player);
      helper.assertTrue(data.hasResearch(NexusPlayerData.RESEARCH_NEXUS_THEORY), "SCAN FIELD should unlock Nexus Theory");
      helper.assertTrue(data.scanCount() > 0, "SCAN FIELD should record real scan progress");
      var claimable = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("the_signal_beneath"));
      helper.assertTrue(claimable.status() == TerminalMissionStatus.CLAIMABLE, "SCAN FIELD should make the starter cache claimable");

      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.handleAction(serverPlayer, NexusTerminalIds.id("the_signal_beneath"), "claim_cache"),
         "Starter cache claim should be handled"
      );
      helper.assertTrue(data.isTerminalCacheClaimed("the_signal_beneath"), "Starter cache claim should persist on player data");
      int pendingAfterClaim = EchoCoreServices.pendingTerminalRewardCount(serverPlayer);
      int shardsAfterClaim = countItem(player, (ItemLike)ModItems.NEXUS_SHARD.get());
      int wireAfterClaim = countItem(player, (ItemLike)ModItems.SIGNAL_WIRE.get());
      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.handleAction(serverPlayer, NexusTerminalIds.id("the_signal_beneath"), "claim_cache"),
         "Repeated starter cache claim should be safely handled"
      );
      helper.assertTrue(
         EchoCoreServices.pendingTerminalRewardCount(serverPlayer) == pendingAfterClaim
            && countItem(player, (ItemLike)ModItems.NEXUS_SHARD.get()) == shardsAfterClaim
            && countItem(player, (ItemLike)ModItems.SIGNAL_WIRE.get()) == wireAfterClaim,
         "Repeated starter cache claim should not duplicate pending rewards or fallback inventory rewards"
      );
      helper.succeed();
   }

   private static void strictPlayableStarterLoop(GameTestHelper helper) {
      Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
      if (!(mockPlayer instanceof ServerPlayer player)) {
         helper.succeed();
         return;
      }
      BlockPos playerLocal = new BlockPos(2, 1, 2);
      BlockPos playerPos = helper.absolutePos(playerLocal);
      player.teleportTo(helper.getLevel(), playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D, Set.of(), 0.0F, 0.0F, true);

      Identifier signalMission = NexusTerminalIds.id("the_signal_beneath");
      var locked = NexusTerminalMissionProvider.INSTANCE.snapshot(player, signalMission);
      helper.assertTrue(locked.status() == TerminalMissionStatus.LOCKED, "Starter Nexus mission should be locked before the Stationfall/dev handoff");
      helper.assertTrue(!locked.actionHint().isBlank(), "Locked starter mission should explain the missing handoff");
      helper.assertTrue(
         locked.actions().stream().anyMatch(action -> !action.enabled() && !action.disabledReason().isBlank()),
         "Locked starter mission should expose a disabled next action with a reason"
      );

      NexusProgression.grantDevelopmentUnlock(player);
      var unlocked = NexusTerminalMissionProvider.INSTANCE.snapshot(player, signalMission);
      helper.assertTrue(unlocked.status() == TerminalMissionStatus.UNLOCKED, "Starter Nexus mission should unlock after the handoff");
      helper.assertTrue(
         unlocked.actions().stream().anyMatch(action -> action.enabled() && "scan".equals(action.id()) && "SCAN FIELD".equals(action.label())),
         "Unlocked starter mission should expose the SCAN FIELD player action"
      );
      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.handleAction(player, signalMission, "scan"),
         "SCAN FIELD should be handled from the Terminal"
      );

      NexusPlayerData data = NexusPlayerData.get(player);
      helper.assertTrue(data.hasResearch(NexusPlayerData.RESEARCH_NEXUS_THEORY), "Terminal scan should unlock Nexus Theory");
      helper.assertTrue(data.scanCount() > 0, "Terminal scan should create real server-side scan state");
      var claimable = NexusTerminalMissionProvider.INSTANCE.snapshot(player, signalMission);
      helper.assertTrue(claimable.status() == TerminalMissionStatus.CLAIMABLE, "Terminal scan should make the starter cache claimable");
      helper.assertTrue(claimable.actionHint().contains("Claim"), "Claimable starter mission should point to the support cache");
      helper.assertTrue(
         claimable.actions().stream().anyMatch(action -> action.enabled() && "claim_cache".equals(action.id())),
         "Claimable starter mission should expose a cache claim action"
      );

      int pendingBeforeClaim = EchoCoreServices.pendingTerminalRewardCount(player);
      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.handleAction(player, signalMission, "claim_cache"),
         "Starter support cache should be claimable"
      );
      helper.assertTrue(data.isTerminalCacheClaimed("the_signal_beneath"), "Starter cache claim should persist on player state");
      EchoCoreServices.claimTerminalRewards(player);
      int starterShards = countItem(player, (ItemLike)ModItems.NEXUS_SHARD.get());
      int starterWire = countItem(player, (ItemLike)ModItems.SIGNAL_WIRE.get());
      helper.assertTrue(starterShards >= 2 && starterWire >= 2, "Starter cache should grant enough Nexus materials for scanner/recycler onboarding");
      assertRecipe(helper, "nexus_scanner_visor");
      assertRecipe(helper, "nexus_recycler");

      int pendingAfterClaim = EchoCoreServices.pendingTerminalRewardCount(player);
      int shardsAfterClaim = countItem(player, (ItemLike)ModItems.NEXUS_SHARD.get());
      int wireAfterClaim = countItem(player, (ItemLike)ModItems.SIGNAL_WIRE.get());
      helper.assertTrue(
         NexusTerminalMissionProvider.INSTANCE.handleAction(player, signalMission, "claim_cache"),
         "Repeated starter cache claim should be safely handled"
      );
      helper.assertTrue(
         EchoCoreServices.pendingTerminalRewardCount(player) == pendingAfterClaim
            && countItem(player, (ItemLike)ModItems.NEXUS_SHARD.get()) == shardsAfterClaim
            && countItem(player, (ItemLike)ModItems.SIGNAL_WIRE.get()) == wireAfterClaim,
         "Repeated starter cache claim should not duplicate pending rewards or fallback inventory rewards"
      );
      helper.assertTrue(pendingAfterClaim <= pendingBeforeClaim + 1, "Starter cache should create at most one pending reward bundle");

      NexusMachineBlockEntity recycler = placeMachine(helper, new BlockPos(4, 1, 2), (Block)ModBlocks.NEXUS_RECYCLER.get());
      recycler.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get(), 2));
      tickMachine(helper, recycler, 130);
      helper.assertTrue(recycler.getItem(NexusMachineBlockEntity.INPUT_SLOT).getCount() == 1, "Recycler should consume exactly one Nexus Shard per process");
      helper.assertTrue(
         recycler.getItem(NexusMachineBlockEntity.OUTPUT_SLOT).is(ModItems.REALITY_DUST.get())
            && recycler.getItem(NexusMachineBlockEntity.OUTPUT_SLOT).getCount() == 1,
         "Recycler should output exactly one Reality Dust for the first process"
      );
      helper.assertTrue(recycler.energyStored() >= 700, "Recycler should produce Nexus Charge from the first craftable input");
      helper.assertTrue(data.hasUsedMachine("nexus_recycler"), "Recycler completion should mark player progression near the machine");
      helper.assertFalse(recycler.acceptsInput(new ItemStack(Items.DIRT)), "Recycler should reject invalid input instead of silently eating it");

      var dirtyCharge = NexusTerminalMissionProvider.INSTANCE.snapshot(player, NexusTerminalIds.id("dirty_charge"));
      helper.assertTrue(dirtyCharge.status() == TerminalMissionStatus.CLAIMABLE, "Using the recycler should advance the Dirty Charge Terminal objective");
      helper.assertTrue(dirtyCharge.actionHint().contains("Claim"), "Completed Dirty Charge mission should direct the player to claim the next cache");
      helper.succeed();
   }

   private static void nexusPlayerDataPersistenceRoundTrip(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      BlockPos returnPos = helper.absolutePos(new BlockPos(3, 1, 3));
      player.teleportTo(helper.getLevel(), returnPos.getX() + 0.25D, returnPos.getY(), returnPos.getZ() + 0.75D, Set.of(), 123.0F, -12.0F, true);
      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      ChunkPos center = new ChunkPos(returnPos.getX() >> 4, returnPos.getZ() >> 4);
      worldData.setFieldValue(center, 64);
      worldData.setCorruptionPressure(center, 37);
      worldData.quarantineChunk(center, 222);
      worldData.startAnomalyStorm(center, helper.getLevel().getGameTime());
      worldData.activateBlackboxMonolith();
      worldData.markWardenDefeated();
      worldData.markGuardianDefeated();
      worldData.commitEndingState("");
      worldData.commitEndingState("restore");

      NexusPlayerData original = new NexusPlayerData();
      original.unlockResearch(NexusPlayerData.RESEARCH_NEXUS_THEORY);
      original.unlockResearch(NexusPlayerData.RESEARCH_MEMORY_RECOVERY);
      original.markScanned(id("scan/persistence"));
      original.markTerminalCacheClaimed("the_signal_beneath");
      original.markMachineUsed("nexus_recycler");
      original.markMachineUsed("memory_decoder");
      original.markSealUsed("repair");
      original.markGearUsed("nexus_scanner_visor");
      original.addBlackboxFragment();
      original.addBlackboxFragment();
      original.addBlackboxFragment();
      original.activateBlackboxMonolith();
      original.markWardenDefeated();
      original.markGuardianDefeated();
      original.markCoreEntered();
      original.setEndingPath("merge");
      original.setArmorLockCooldown(37);
      original.setNexusReturn(player);
      original.setFinalChoiceState("committed");
      original.refreshFieldTelemetry(player);
      int expectedField = original.telemetryFieldValue();
      int expectedCorruption = original.telemetryCorruptionPressure();
      int expectedQuarantine = original.telemetryQuarantineTicks();
      boolean expectedStorm = original.telemetryActiveStorm();
      int expectedTears = original.telemetryRealityTears();
      int centerIndex = NexusPlayerData.FIELD_MAP_RADIUS * NexusPlayerData.FIELD_MAP_DIAMETER + NexusPlayerData.FIELD_MAP_RADIUS;
      int expectedMapField = original.telemetryMapField(centerIndex);
      int expectedMapCorruption = original.telemetryMapCorruption(centerIndex);
      boolean expectedMapStorm = original.telemetryMapStorm(centerIndex);
      int expectedMapTears = original.telemetryMapTears(centerIndex);

      NexusPlayerData restored = roundTripNexusPlayerData(helper, original);
      helper.assertTrue(restored.hasResearch(NexusPlayerData.RESEARCH_NEXUS_THEORY), "Research unlocks should survive player-data serialization");
      helper.assertTrue(restored.hasResearch(NexusPlayerData.RESEARCH_MEMORY_RECOVERY), "Secondary research unlocks should survive player-data serialization");
      helper.assertTrue(restored.scannedIds().contains(id("scan/persistence").toString()), "Scanned ids should survive player-data serialization");
      helper.assertTrue(restored.isTerminalCacheClaimed("the_signal_beneath"), "Claimed Terminal caches should survive player-data serialization");
      helper.assertTrue(restored.hasUsedMachine("nexus_recycler") && restored.hasUsedMachine("memory_decoder"), "Used machines should survive player-data serialization");
      helper.assertTrue(restored.hasUsedSeal("repair"), "Used Protocol Seal modes should survive player-data serialization");
      helper.assertTrue(restored.hasUsedGear("nexus_scanner_visor"), "Used Nexus gear should survive player-data serialization");
      helper.assertTrue(restored.blackboxFragments() == 3, "Blackbox fragment count should survive player-data serialization");
      helper.assertTrue(restored.blackboxMonolithActivated(), "Monolith player state should survive player-data serialization");
      helper.assertTrue(restored.wardenDefeated() && restored.guardianDefeated(), "Boss defeat state should survive player-data serialization");
      helper.assertTrue(restored.coreEntered(), "Core entry state should survive player-data serialization");
      helper.assertTrue("merge".equals(restored.endingPath()) && "committed".equals(restored.finalChoiceState()), "Final path state should survive player-data serialization");
      helper.assertTrue(restored.armorLockCooldown() == 37, "Armor lock cooldown should survive player-data serialization");
      helper.assertTrue(restored.hasNexusReturn(), "Return-vector presence should survive player-data serialization");
      helper.assertTrue("minecraft:overworld".equals(restored.nexusReturnDimension()), "Return dimension should survive player-data serialization");
      helper.assertTrue(Math.abs(restored.nexusReturnX() - (returnPos.getX() + 0.25D)) < 0.01D, "Return X should survive player-data serialization");
      helper.assertTrue(Math.abs(restored.nexusReturnZ() - (returnPos.getZ() + 0.75D)) < 0.01D, "Return Z should survive player-data serialization");
      helper.assertTrue(Math.abs(restored.nexusReturnYRot() - 123.0F) < 0.01F, "Return yaw should survive player-data serialization");
      helper.assertTrue(Math.abs(restored.nexusReturnXRot() - -12.0F) < 0.01F, "Return pitch should survive player-data serialization");
      helper.assertTrue(restored.telemetryFieldValue() == expectedField, "Field telemetry should survive player-data serialization");
      helper.assertTrue(restored.telemetryCorruptionPressure() == expectedCorruption, "Corruption telemetry should survive player-data serialization");
      helper.assertTrue(restored.telemetryQuarantineTicks() == expectedQuarantine, "Quarantine telemetry should survive player-data serialization");
      helper.assertTrue(restored.telemetryActiveStorm() == expectedStorm, "Storm telemetry should survive player-data serialization");
      helper.assertTrue(restored.telemetryRealityTears() == expectedTears, "Reality tear telemetry should survive player-data serialization");
      helper.assertTrue(restored.telemetryFieldMapSize() == NexusPlayerData.FIELD_MAP_SIZE, "Field map telemetry should remain a stable 5x5 sync shape");
      helper.assertTrue(restored.telemetryMapField(centerIndex) == expectedMapField, "Field map values should survive player-data serialization");
      helper.assertTrue(restored.telemetryMapCorruption(centerIndex) == expectedMapCorruption, "Field map corruption should survive player-data serialization");
      helper.assertTrue(restored.telemetryMapStorm(centerIndex) == expectedMapStorm, "Field map storm markers should survive player-data serialization");
      helper.assertTrue(restored.telemetryMapTears(centerIndex) == expectedMapTears, "Field map tear markers should survive player-data serialization");
      helper.assertTrue("restore".equals(restored.telemetryWorldEndingState()), "World ending telemetry should survive player-data serialization");
      helper.succeed();
   }

   private static void nexusMachinePersistenceRoundTrip(GameTestHelper helper) {
      NexusMachineBlockEntity machine = placeMachine(helper, new BlockPos(1, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      machine.setItem(NexusMachineBlockEntity.INPUT_SLOT, new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get(), 3));
      machine.setItem(NexusMachineBlockEntity.OUTPUT_SLOT, new ItemStack((ItemLike)ModItems.REALITY_DUST.get(), 2));
      machine.receiveCharge(444);
      machine.addContamination(27);
      machine.data().set(NexusMachineBlockEntity.DATA_PROGRESS, 41);
      machine.data().set(NexusMachineBlockEntity.DATA_MAX_PROGRESS, 120);
      machine.data().set(NexusMachineBlockEntity.DATA_STATUS, NexusMachineBlockEntity.MachineStatus.PROCESSING.ordinal());

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      machine.saveWithoutMetadata(output);
      CompoundTag saved = output.buildResult();
      NexusMachineBlockEntity restored = new NexusMachineBlockEntity(machine.getBlockPos(), machine.getBlockState());
      restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      helper.assertTrue(
         restored.getItem(NexusMachineBlockEntity.INPUT_SLOT).is(ModItems.NEXUS_SHARD.get())
            && restored.getItem(NexusMachineBlockEntity.INPUT_SLOT).getCount() == 3,
         "Machine input inventory should survive block-entity serialization without item loss"
      );
      helper.assertTrue(
         restored.getItem(NexusMachineBlockEntity.OUTPUT_SLOT).is(ModItems.REALITY_DUST.get())
            && restored.getItem(NexusMachineBlockEntity.OUTPUT_SLOT).getCount() == 2,
         "Machine output inventory should survive block-entity serialization without duplication"
      );
      helper.assertTrue(restored.energyStored() == 444, "Machine charge should survive block-entity serialization");
      helper.assertTrue(restored.contamination() == 27, "Machine contamination should survive block-entity serialization");
      helper.assertTrue(restored.data().get(NexusMachineBlockEntity.DATA_PROGRESS) == 41, "Machine progress should survive block-entity serialization");
      helper.assertTrue(restored.data().get(NexusMachineBlockEntity.DATA_MAX_PROGRESS) == 120, "Machine max progress should survive block-entity serialization");
      helper.assertTrue(
         restored.data().get(NexusMachineBlockEntity.DATA_STATUS) == NexusMachineBlockEntity.MachineStatus.PROCESSING.ordinal(),
         "Machine status should survive block-entity serialization"
      );
      helper.succeed();
   }

   private static void coreAccessKeyRoute(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      ResourceKey<net.minecraft.world.level.Level> originDimension = helper.getLevel().dimension();
      BlockPos origin = helper.absolutePos(new BlockPos(5, 1, 5));
      player.teleportTo(helper.getLevel(), origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D, Set.of(), 45.0F, 5.0F, true);
      NexusProgression.grantDevelopmentUnlock(player);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)ModItems.CORE_ACCESS_KEY.get()));

      InteractionResult openResult = player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      if (helper.getLevel().getServer().getLevel(ModDimensions.NEXUS) == null) {
         helper.assertTrue(openResult == InteractionResult.CONSUME, "Core Access Key should fail safely when the Nexus dimension is absent");
         NexusPlayerData failed = NexusPlayerData.get(player);
         helper.assertFalse(failed.coreEntered(), "Missing Nexus dimension should not mark Core entry");
         helper.assertFalse(failed.hasNexusReturn(), "Missing Nexus dimension should not save a stale return vector");
         helper.succeed();
         return;
      }
      helper.assertTrue(openResult == InteractionResult.SUCCESS_SERVER, "Core Access Key should open the Nexus route after unlock");
      NexusPlayerData entered = NexusPlayerData.get(player);
      helper.assertTrue(player.level().dimension() == ModDimensions.NEXUS, "Core Access Key should move the player into the Nexus dimension");
      helper.assertTrue(entered.coreEntered(), "Core Access Key should mark Core entry on player data");
      helper.assertTrue(entered.hasNexusReturn(), "Core Access Key should write a return vector before dimension transfer");
      helper.assertTrue(entered.hasUsedGear("core_access_key"), "Core Access Key should mark gear use for Terminal progression");

      NexusPlayerData restored = roundTripNexusPlayerData(helper, entered);
      helper.assertTrue(restored.hasNexusReturn(), "Core return vector should survive player-data serialization");
      helper.assertTrue(originDimension.identifier().toString().equals(restored.nexusReturnDimension()), "Core return dimension should survive player-data serialization");
      helper.assertTrue(Math.abs(restored.nexusReturnX() - (origin.getX() + 0.5D)) < 0.01D, "Core return X should survive player-data serialization");
      helper.assertTrue(Math.abs(restored.nexusReturnZ() - (origin.getZ() + 0.5D)) < 0.01D, "Core return Z should survive player-data serialization");

      InteractionResult returnResult = player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(returnResult == InteractionResult.SUCCESS_SERVER, "Core Access Key should return the player from the Nexus dimension");
      NexusPlayerData returned = NexusPlayerData.get(player);
      helper.assertTrue(player.level().dimension().equals(originDimension), "Core Access Key should restore the saved origin dimension");
      helper.assertFalse(returned.hasNexusReturn(), "Returning from Nexus should clear the saved return vector to avoid stale re-use");
      helper.succeed();
   }

   private static void oneTimeBossRewardGates(GameTestHelper helper) {
      NexusWorldData data = new NexusWorldData();
      helper.assertTrue(data.markWardenDefeated(), "First Warden defeat should open the unique reward gate");
      helper.assertFalse(data.markWardenDefeated(), "Repeated Warden defeat should not reopen the unique reward gate");
      helper.assertTrue(data.markGuardianDefeated(), "First Guardian defeat should open the unique reward gate");
      helper.assertFalse(data.markGuardianDefeated(), "Repeated Guardian defeat should not reopen the unique reward gate");
      helper.assertTrue(data.commitEndingState("restore"), "First ending path should commit");
      helper.assertFalse(data.commitEndingState("destroy"), "A different ending path should not overwrite the committed path");
      helper.succeed();
   }

   private static void stormSpawnCap(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      if (!(player instanceof ServerPlayer serverPlayer)) {
         helper.succeed();
         return;
      }

      BlockPos center = helper.absolutePos(new BlockPos(2, 1, 2));
      serverPlayer.teleportTo(helper.getLevel(), center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, Set.of(), 0.0F, 0.0F, true);
      for (int i = 0; i < NexusWorldEvents.STORM_MOB_CAP; i++) {
         NexusMobEntity mob = (NexusMobEntity)((EntityType)ModEntities.STATIC_CRAWLER.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
         helper.assertTrue(mob != null, "Storm cap test mob should create");
         if (mob != null) {
            mob.setNoAi(true);
            mob.setPos(center.getX() + 0.5D + i, center.getY(), center.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(mob);
         }
      }
      helper.assertTrue(
         NexusWorldEvents.nearbyStormMobCount(helper.getLevel(), center) >= NexusWorldEvents.STORM_MOB_CAP,
         "Storm cap test should place enough Nexus mobs near the player"
      );
      helper.assertFalse(NexusWorldEvents.canSpawnStormMob(helper.getLevel(), center), "Anomaly storms should stop spawning local mobs at the cap");
      helper.succeed();
   }

   private static void jeiJsonRecipeParity(GameTestHelper helper) {
      java.util.Set<String> ids = new java.util.HashSet<>();
      for (var recipe : NexusJeiRecipeCatalog.all()) {
         String path = recipe.id().getPath();
         helper.assertTrue(ids.add(path), "JEI recipe ids should be unique: " + path);
         assertResourcePresent(helper, "data/echonexusprotocol/recipe/" + path + ".json");
      }
      helper.assertTrue(ids.size() >= 18, "JEI catalog should stay in parity with the data-driven Nexus processing recipes");
      helper.succeed();
   }

   private static void worldDataCodecRoundTrip(GameTestHelper helper) {
      NexusWorldData original = new NexusWorldData();
      ChunkPos chunk = new ChunkPos(12, -4);
      original.setFieldValue(chunk, 31);
      original.setCorruptionPressure(chunk, 77);
      original.quarantineChunk(chunk, 240);
      original.startAnomalyStorm(chunk, 1234L);
      original.activateBlackboxMonolith();
      original.markWardenDefeated();
      original.markGuardianDefeated();
      original.commitEndingState("merge");
      int expectedField = original.fieldValue(chunk);
      int expectedCorruption = original.corruptionPressure(chunk);
      int expectedQuarantine = original.quarantineTicks(chunk);
      int expectedTears = original.realityTearCount(chunk);
      var encoded = NexusWorldData.CODEC.encodeStart(JsonOps.INSTANCE, original).result();
      helper.assertTrue(encoded.isPresent(), "NexusWorldData should encode through its SavedData codec");
      JsonElement json = encoded.get();
      var decoded = NexusWorldData.CODEC.parse(JsonOps.INSTANCE, json).result();
      helper.assertTrue(decoded.isPresent(), "NexusWorldData should decode through its SavedData codec");
      NexusWorldData copy = decoded.get();
      helper.assertTrue(copy.fieldValue(chunk) == expectedField, "World-data codec should preserve field map telemetry");
      helper.assertTrue(copy.corruptionPressure(chunk) == expectedCorruption, "World-data codec should preserve corruption pressure");
      helper.assertTrue(copy.quarantineTicks(chunk) == expectedQuarantine, "World-data codec should preserve quarantine ticks");
      helper.assertTrue(copy.realityTearCount(chunk) == expectedTears, "World-data codec should preserve reality tear telemetry");
      helper.assertTrue(copy.hasActiveStorm(chunk, 1234L, 400L), "World-data codec should preserve storm telemetry");
      helper.assertTrue(copy.blackboxMonolithActivated(), "World-data codec should preserve Monolith activation");
      helper.assertTrue(copy.wardenDefeated(), "World-data codec should preserve Warden defeat state");
      helper.assertTrue(copy.guardianDefeated(), "World-data codec should preserve Guardian defeat state");
      helper.assertTrue("merge".equals(copy.endingState()), "World-data codec should preserve ending state");
      helper.assertTrue(copy.lastStormTick() == 1234L, "World-data codec should preserve last storm tick");
      helper.succeed();
   }

   private static void releaseRiskFixes(GameTestHelper helper) {
      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      worldData.commitEndingState("");
      ChunkPos stormChunk = new ChunkPos(8, 8);
      worldData.startAnomalyStorm(stormChunk, helper.getLevel().getGameTime() - 1000L);
      worldData.pruneExpiredStorms(helper.getLevel().getGameTime(), 80L);
      helper.assertFalse(worldData.hasActiveStorm(stormChunk, helper.getLevel().getGameTime(), 80L), "Expired storms should be pruned from telemetry");

      NexusMachineBlockEntity recycler = placeMachine(helper, new BlockPos(1, 1, 1), (Block)ModBlocks.NEXUS_RECYCLER.get());
      helper.assertTrue(recycler.acceptsInput(new ItemStack((ItemLike)ModItems.NEXUS_SHARD.get())), "Machine input should accept known Nexus recipes");
      helper.assertFalse(recycler.acceptsInput(new ItemStack(Items.DIRT)), "Machine input should reject invalid recipe inputs");

      BlockPos relaySealPos = helper.absolutePos(new BlockPos(20, 1, 1));
      NexusMachineBlockEntity relaySource = placeMachine(helper, new BlockPos(21, 1, 1), (Block)ModBlocks.NEXUS_CHARGE_TANK.get());
      NexusMachineBlockEntity relayTarget = placeMachine(helper, new BlockPos(22, 1, 1), (Block)ModBlocks.NEXUS_CHARGE_TANK.get());
      relaySource.receiveCharge(relaySource.energyStorage().getCapacityAsInt());
      relayTarget.receiveCharge(relayTarget.energyStorage().getCapacityAsInt() - 10);
      int relayTotalBefore = relaySource.energyStored() + relayTarget.energyStored();
      ProtocolSealBlock.applySeal(helper.getLevel(), relaySealPos, ProtocolSealBlock.SealMode.RELAY);
      helper.assertTrue(relaySource.energyStored() + relayTarget.energyStored() == relayTotalBefore, "Relay seal should not delete charge when the target is nearly full");
      helper.assertTrue(relayTarget.energyStored() == relayTarget.energyStorage().getCapacityAsInt(), "Relay seal should fill the remaining target capacity exactly");

      for (String entityLoot : new String[]{"nexus_husk", "data_wraith", "static_crawler", "core_soldier", "archive_seeker", "corruption_warden", "nexus_guardian"}) {
         assertResourcePresent(helper, "data/echonexusprotocol/loot_table/entities/" + entityLoot + ".json");
      }
      for (String chestLoot : new String[]{"field_station_supply", "data_vault_memory", "containment_lab_reactor", "core_chamber_final_prep"}) {
         assertResourcePresent(helper, "data/echonexusprotocol/loot_table/chests/" + chestLoot + ".json");
      }
      for (String feature : new String[]{"white_signal_tree", "nexus_crystal_cluster", "static_fluid_pool", "dead_signal_leaf_patch", "hollow_signal_log_patch", "corrupted_ferrite_pocket", "blackbox_debris_cluster", "lab_pipe_remnant", "reality_tear_hotspot"}) {
         assertResourcePresent(helper, "data/echonexusprotocol/worldgen/configured_feature/" + feature + ".json");
      }
      for (String placed : new String[]{"white_signal_trees", "nexus_crystal_clusters", "static_fluid_pools", "dead_signal_leaf_patches", "hollow_signal_log_patches", "corrupted_ferrite_pockets", "blackbox_debris_clusters", "lab_pipe_remnants", "reality_tear_hotspots"}) {
         assertResourcePresent(helper, "data/echonexusprotocol/worldgen/placed_feature/" + placed + ".json");
      }

      String coreBiome = readResource("data/echonexusprotocol/worldgen/biome/core_exclusion_zone.json");
      helper.assertFalse(coreBiome.contains("echonexusprotocol:corruption_warden"), "Corruption Warden should not naturally spawn from biomes");
      helper.assertFalse(coreBiome.contains("echonexusprotocol:nexus_guardian"), "Nexus Guardian should not naturally spawn from biomes");
      helper.assertTrue(coreBiome.contains("echonexusprotocol:reality_tear_hotspots"), "Core Exclusion Zone should include reality tear exploration dressing");

      ChunkPos endingChunk = new ChunkPos(9, 9);
      worldData.commitEndingState("");
      worldData.setFieldValue(endingChunk, 40);
      worldData.setCorruptionPressure(endingChunk, 40);
      helper.assertTrue(worldData.commitEndingState("restore"), "First ending commit should apply world feedback");
      helper.assertFalse(worldData.commitEndingState("destroy"), "Different ending commits should be rejected after a path is set");
      helper.assertTrue("restore".equals(worldData.endingState()), "World ending should remain the first committed path");
      helper.succeed();
   }

   private static void routeLootPolishInvariants(GameTestHelper helper) {
      String fieldStation = readResource("data/echonexusprotocol/loot_table/chests/field_station_supply.json");
      helper.assertTrue(
         fieldStation.contains("echonexusprotocol:signal_wire")
            && fieldStation.contains("echonexusprotocol:filter_membrane")
            && fieldStation.contains("echonexusprotocol:field_membrane"),
         "Field station loot should support early machine and filter setup"
      );

      String dataVault = readResource("data/echonexusprotocol/loot_table/chests/data_vault_memory.json");
      helper.assertTrue(
         dataVault.contains("echonexusprotocol:data_fragment")
            && dataVault.contains("echonexusprotocol:memory_shard")
            && dataVault.contains("echonexusprotocol:blackbox_fragment"),
         "Data vault loot should support memory and Blackbox fragment progress"
      );

      String containment = readResource("data/echonexusprotocol/loot_table/chests/containment_lab_reactor.json");
      helper.assertTrue(
         containment.contains("echonexusprotocol:field_anchor")
            && containment.contains("echonexusprotocol:stabilized_purity_charge"),
         "Containment lab loot should support collapsed-field recovery"
      );

      String wardenLoot = readResource("data/echonexusprotocol/loot_table/entities/corruption_warden.json");
      String guardianLoot = readResource("data/echonexusprotocol/loot_table/entities/nexus_guardian.json");
      helper.assertTrue(wardenLoot.contains("echonexusprotocol:field_membrane"), "Warden repeat loot should include route support materials");
      helper.assertFalse(wardenLoot.contains("echonexusprotocol:reactor_core"), "Warden Reactor Core should remain guarded by one-time defeat code");
      helper.assertTrue(guardianLoot.contains("echonexusprotocol:stabilized_purity_charge"), "Guardian repeat loot should include final recovery support");
      helper.assertFalse(guardianLoot.contains("echonexusprotocol:core_key_assembly"), "Guardian Core Key Assembly should remain guarded by one-time defeat code");
      helper.succeed();
   }

   private static String readResource(String path) {
      try (java.io.InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
         return input == null ? "" : new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      } catch (java.io.IOException exception) {
         return "";
      }
   }

   private static void fieldTerminalTelemetrySources(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      if (!(player instanceof ServerPlayer serverPlayer)) {
         helper.succeed();
         return;
      }

      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      NexusPlayerData playerData = NexusPlayerData.get(player);
      ChunkPos chunk = serverPlayer.chunkPosition();
      int[] values = {86, 65, 45, 25, 12};
      NexusWorldData.FieldState[] states = {
         NexusWorldData.FieldState.STABLE,
         NexusWorldData.FieldState.UNSTABLE,
         NexusWorldData.FieldState.FRACTURED,
         NexusWorldData.FieldState.CRITICAL,
         NexusWorldData.FieldState.COLLAPSED
      };
      for (int i = 0; i < values.length; i++) {
         worldData.setFieldValue(chunk, values[i]);
         playerData.refreshFieldTelemetry(serverPlayer);
         helper.assertTrue(playerData.telemetryFieldValue() == values[i], "Field telemetry should sync the local chunk field value");
         helper.assertTrue(NexusWorldData.FieldState.fromValue(playerData.telemetryFieldValue()) == states[i], "Field telemetry should preserve field state thresholds");
      }

      worldData.setCorruptionPressure(chunk, 37);
      worldData.quarantineChunk(chunk, 200);
      worldData.startAnomalyStorm(chunk, helper.getLevel().getGameTime());
      worldData.activateBlackboxMonolith();
      worldData.markWardenDefeated();
      worldData.markGuardianDefeated();
      worldData.setEndingState("restore");
      playerData.refreshFieldTelemetry(serverPlayer);
      helper.assertTrue(playerData.telemetryCorruptionPressure() >= 37, "Field tab telemetry should expose corruption pressure");
      helper.assertTrue(playerData.telemetryQuarantineTicks() > 0, "Field tab telemetry should expose quarantine duration");
      helper.assertTrue(playerData.telemetryActiveStorm(), "Field tab telemetry should expose active storm state");
      helper.assertTrue(playerData.telemetryRealityTears() > 0, "Field tab telemetry should expose reality tear count");
      helper.assertTrue(playerData.telemetryWorldMonolithActivated(), "Field tab telemetry should expose Monolith state");
      helper.assertTrue(playerData.telemetryWorldWardenDefeated(), "Field tab telemetry should expose Warden state");
      helper.assertTrue(playerData.telemetryWorldGuardianDefeated(), "Field tab telemetry should expose Guardian state");
      helper.assertTrue("restore".equals(playerData.telemetryWorldEndingState()), "Field tab telemetry should expose ending state");
      helper.succeed();
   }

   private static void utilityGearBehaviors(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      BlockPos absolute = helper.absolutePos(local);
      helper.setBlock(local, Blocks.STONE);
      ChunkPos chunk = new ChunkPos(absolute.getX() >> 4, absolute.getZ() >> 4);
      NexusWorldData worldData = NexusWorldData.get(helper.getLevel());
      worldData.setFieldValue(chunk, 45);
      worldData.setCorruptionPressure(chunk, 24);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      int pulseScans = NexusUtilityItem.fieldPulse(helper.getLevel(), player, absolute);
      NexusPlayerData playerData = NexusPlayerData.get(player);
      helper.assertTrue(pulseScans > 0, "Nexus Pickaxe Field Pulse should reveal or scan Nexus signatures");
      helper.assertBlockPresent((Block)ModBlocks.NEXUS_CRYSTAL_CLUSTER.get(), local);
      helper.assertTrue(playerData.hasResearch("matter_rewriting"), "Field Pulse should teach Matter Rewriting from crystal signatures");
      NexusUtilityItem.anchorReality(helper.getLevel(), absolute);
      helper.assertTrue(worldData.isQuarantined(chunk), "Reality Anchor should quarantine the current chunk");
      int corruptionAfterAnchor = worldData.corruptionPressure(chunk);
      NexusMobEntity husk = helper.spawnWithNoFreeWill(ModEntities.NEXUS_HUSK.get(), new BlockPos(2, 1, 1));
      helper.runAfterDelay(3L, () -> {
         float beforeHealth = husk.getHealth();
         int hit = NexusUtilityItem.signalBladePulse(helper.getLevel(), absolute, player);
         helper.assertTrue(hit > 0, "Signal Blade should hit nearby Nexus mobs");
         helper.assertTrue(husk.getHealth() < beforeHealth, "Signal Blade should damage Nexus mobs");
         helper.assertTrue(worldData.corruptionPressure(chunk) < corruptionAfterAnchor, "Signal Blade pulse should lower local corruption pressure");
         helper.succeed();
      });
   }

   private static void assertRecipe(GameTestHelper helper, String path) {
      Identifier recipeId = id(path);
      boolean exists = helper.getLevel().getServer().getRecipeManager().getRecipes().stream().anyMatch(holder -> holder.id().identifier().equals(recipeId));
      helper.assertTrue(exists, "Required Nexus survival recipe missing: " + recipeId);
   }

   private static NexusPlayerData roundTripNexusPlayerData(GameTestHelper helper, NexusPlayerData original) {
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      original.serialize(output);
      CompoundTag tag = output.buildResult();
      NexusPlayerData restored = new NexusPlayerData();
      restored.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
      return restored;
   }

   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData(
         environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, 24
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static boolean shouldRegisterTests() {
      String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
      if (namespaces == null || namespaces.isBlank()) {
         return true;
      }
      for (String namespace : namespaces.split(",")) {
         String normalized = namespace.trim();
         if (normalized.equals(EchoNexusProtocol.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
            return true;
         }
      }
      return false;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, path);
   }
}
