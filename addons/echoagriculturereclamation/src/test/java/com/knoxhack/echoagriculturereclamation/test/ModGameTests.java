package com.knoxhack.echoagriculturereclamation.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoFactionDefinition;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldQuery;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldSnapshot;
import com.knoxhack.echoagriculturereclamation.api.ReclamationIntegrationServices;
import com.knoxhack.echoagriculturereclamation.block.ReclamationMachineBlock;
import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationCropBlockEntity;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationMachineBlockEntity;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCoreIntegration;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration.FactionPreference;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationMapDataProvider;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationTerminalIds;
import com.knoxhack.echoagriculturereclamation.menu.ReclamationMachineMenu;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationRestoration;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationWorldData;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, EchoAgricultureReclamation.MODID);

   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEED_CAPSULE =
      TEST_FUNCTIONS.register("seed_capsule_analysis", () -> ModGameTests::seedCapsuleAnalysis);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SOIL_CONVERSION =
      TEST_FUNCTIONS.register("soil_state_conversion", () -> ModGameTests::soilStateConversion);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HYDROPONIC_TRAY =
      TEST_FUNCTIONS.register("hydroponic_tray_persistence", () -> ModGameTests::hydroponicTrayPersistence);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GREENHOUSE =
      TEST_FUNCTIONS.register("greenhouse_safety_scoring", () -> ModGameTests::greenhouseSafetyScoring);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POLLINATOR_DRONE =
      TEST_FUNCTIONS.register("pollinator_drone_system", () -> ModGameTests::pollinatorDroneSystem);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GROWTH_GREENHOUSE_CACHE =
      TEST_FUNCTIONS.register("growth_greenhouse_cache_regression", () -> ModGameTests::growthGreenhouseCacheRegression);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEED_STABILIZATION =
      TEST_FUNCTIONS.register("seed_stabilization_profile", () -> ModGameTests::seedStabilizationProfile);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL =
      TEST_FUNCTIONS.register("terminal_status_metrics", () -> ModGameTests::terminalStatusMetrics);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESTORATION =
      TEST_FUNCTIONS.register("restoration_score_conversion", () -> ModGameTests::restorationScoreConversion);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_WIRING =
      TEST_FUNCTIONS.register("core_route_records", () -> ModGameTests::coreRouteRecords);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CROSS_ADDON_SOIL =
      TEST_FUNCTIONS.register("cross_addon_soil_compatibility", () -> ModGameTests::crossAddonSoilCompatibility);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_MILESTONES =
      TEST_FUNCTIONS.register("core_milestone_recording", () -> ModGameTests::coreMilestoneRecording);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTION_SEED_BIAS =
      TEST_FUNCTIONS.register("faction_biased_seed_recovery", () -> ModGameTests::factionBiasedSeedRecovery);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MAIN_LOOP =
      TEST_FUNCTIONS.register("main_progression_loop_regression", () -> ModGameTests::mainProgressionLoopRegression);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BIO_REACTOR_UTILITY =
      TEST_FUNCTIONS.register("bio_reactor_utility_outputs", () -> ModGameTests::bioReactorUtilityOutputs);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COMPOST_RECYCLER_UTILITY =
      TEST_FUNCTIONS.register("compost_recycler_utility_outputs", () -> ModGameTests::compostRecyclerUtilityOutputs);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_DATA_CODEC =
      TEST_FUNCTIONS.register("world_data_codec_round_trip", () -> ModGameTests::worldDataCodecRoundTrip);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BLOCK_ENTITY_PERSISTENCE =
      TEST_FUNCTIONS.register("block_entity_persistence_round_trip", () -> ModGameTests::blockEntityPersistenceRoundTrip);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_REWARD_ONCE =
      TEST_FUNCTIONS.register("terminal_reward_claim_once", () -> ModGameTests::terminalRewardClaimOnce);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOOT_MODIFIER_PACKAGING =
      TEST_FUNCTIONS.register("loot_modifier_packaging", () -> ModGameTests::lootModifierPackaging);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STANDALONE_RECIPE_CHAIN =
      TEST_FUNCTIONS.register("standalone_recipe_chain", () -> ModGameTests::standaloneRecipeChain);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_RECOVERY_ONCE =
      TEST_FUNCTIONS.register("core_recovery_once", () -> ModGameTests::coreRecoveryOnce);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLAYER_PROGRESS_NBT =
      TEST_FUNCTIONS.register("player_progress_nbt_round_trip", () -> ModGameTests::playerProgressNbtRoundTrip);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_SNAPSHOT_API =
      TEST_FUNCTIONS.register("field_snapshot_api", () -> ModGameTests::fieldSnapshotApi);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROCESS_DEFINITIONS =
      TEST_FUNCTIONS.register("process_definitions_indexable", () -> ModGameTests::processDefinitionsIndexable);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_DIAGNOSTICS =
      TEST_FUNCTIONS.register("machine_entity_diagnostics", () -> ModGameTests::machineEntityDiagnostics);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MAP_PROVIDER =
      TEST_FUNCTIONS.register("map_provider_snapshots", () -> ModGameTests::mapProviderSnapshots);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_MENU_PROCESS =
      TEST_FUNCTIONS.register("machine_menu_process_completion", () -> ModGameTests::machineMenuProcessCompletion);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_MENU_BLOCKED_OUTPUT =
      TEST_FUNCTIONS.register("machine_menu_blocked_output", () -> ModGameTests::machineMenuBlockedOutput);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_QUICK_INSERT =
      TEST_FUNCTIONS.register("machine_quick_insert_fallback", () -> ModGameTests::machineQuickInsertFallback);

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("agriculture_reclamation"));
      register(event, environment, "seed_capsule_analysis", SEED_CAPSULE.getId());
      register(event, environment, "soil_state_conversion", SOIL_CONVERSION.getId());
      register(event, environment, "hydroponic_tray_persistence", HYDROPONIC_TRAY.getId());
      register(event, environment, "greenhouse_safety_scoring", GREENHOUSE.getId());
      register(event, environment, "pollinator_drone_system", POLLINATOR_DRONE.getId());
      register(event, environment, "growth_greenhouse_cache_regression", GROWTH_GREENHOUSE_CACHE.getId());
      register(event, environment, "seed_stabilization_profile", SEED_STABILIZATION.getId());
      register(event, environment, "terminal_status_metrics", TERMINAL.getId());
      register(event, environment, "restoration_score_conversion", RESTORATION.getId());
      register(event, environment, "core_route_records", CORE_WIRING.getId());
      register(event, environment, "cross_addon_soil_compatibility", CROSS_ADDON_SOIL.getId());
      register(event, environment, "core_milestone_recording", CORE_MILESTONES.getId());
      register(event, environment, "faction_biased_seed_recovery", FACTION_SEED_BIAS.getId());
      register(event, environment, "main_progression_loop_regression", MAIN_LOOP.getId());
      register(event, environment, "bio_reactor_utility_outputs", BIO_REACTOR_UTILITY.getId());
      register(event, environment, "compost_recycler_utility_outputs", COMPOST_RECYCLER_UTILITY.getId());
      register(event, environment, "world_data_codec_round_trip", WORLD_DATA_CODEC.getId());
      register(event, environment, "block_entity_persistence_round_trip", BLOCK_ENTITY_PERSISTENCE.getId());
      register(event, environment, "terminal_reward_claim_once", TERMINAL_REWARD_ONCE.getId());
      register(event, environment, "loot_modifier_packaging", LOOT_MODIFIER_PACKAGING.getId());
      register(event, environment, "standalone_recipe_chain", STANDALONE_RECIPE_CHAIN.getId());
      register(event, environment, "core_recovery_once", CORE_RECOVERY_ONCE.getId());
      register(event, environment, "player_progress_nbt_round_trip", PLAYER_PROGRESS_NBT.getId());
      register(event, environment, "field_snapshot_api", FIELD_SNAPSHOT_API.getId());
      register(event, environment, "process_definitions_indexable", PROCESS_DEFINITIONS.getId());
      register(event, environment, "machine_entity_diagnostics", MACHINE_DIAGNOSTICS.getId());
      register(event, environment, "map_provider_snapshots", MAP_PROVIDER.getId());
      register(event, environment, "machine_menu_process_completion", MACHINE_MENU_PROCESS.getId());
      register(event, environment, "machine_menu_blocked_output", MACHINE_MENU_BLOCKED_OUTPUT.getId());
      register(event, environment, "machine_quick_insert_fallback", MACHINE_QUICK_INSERT.getId());
   }

   private static void seedCapsuleAnalysis(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack capsule = new ItemStack(ModItems.RECOVERED_SEED_CAPSULE.get(), 2);
      player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
      InteractionResult result = ModItems.RECOVERED_SEED_CAPSULE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(result == InteractionResult.SUCCESS_SERVER, "Recovered seed capsule should analyze on use");
      helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "Capsule use should consume exactly one capsule");
      helper.assertTrue(countProfiledSeeds(player) == 1, "Capsule use should create exactly one profiled contaminated seed");
      helper.assertTrue(!ReclamationProgress.knownSeeds(player).isEmpty(), "Capsule use should record a known seed");
      helper.assertTrue(ReclamationProgress.flag(player, "seed_recovered"), "Capsule use should mark seed recovery");
      helper.succeed();
   }

   private static void soilStateConversion(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, (Block)ModBlocks.CONTAMINATED_SOIL.get());
      int changed = ReclamationRestoration.purifyArea(helper.getLevel(), helper.absolutePos(local), 1, 1);
      helper.assertTrue(changed == 1, "Soil purifier should convert at least one contaminated soil block");
      helper.assertBlockPresent(ModBlocks.PURIFIED_SOIL.get(), local);
      helper.succeed();
   }

   private static void hydroponicTrayPersistence(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, (Block)ModBlocks.HYDROPONIC_TRAY.get());
      HydroponicTrayBlockEntity tray = helper.getBlockEntity(local, HydroponicTrayBlockEntity.class);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ItemStack seed = new ItemStack(ModItems.STABILIZED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), new SeedProfile("clean_corn", 0, 100));
      helper.assertTrue(tray.insertSeed(player, seed), "Hydroponic tray should accept a profiled seed");
      for (int i = 0; i < 1800; i++) {
         HydroponicTrayBlockEntity.tick(helper.getLevel(), tray.getBlockPos(), tray.getBlockState(), tray);
      }
      helper.assertTrue(tray.profile() != null, "Hydroponic tray should persist seed profile");
      helper.assertTrue(tray.age() > 0, "Hydroponic tray should tick crop age");
      helper.succeed();
   }

   private static void greenhouseSafetyScoring(GameTestHelper helper) {
      BlockPos openCenter = new BlockPos(4, 2, 4);
      helper.setBlock(openCenter, (Block)ModBlocks.GREENHOUSE_CONTROLLER.get());
      for (int x = 1; x <= 6; x++) {
         helper.setBlock(new BlockPos(x, 3, 1), (Block)ModBlocks.GREENHOUSE_GLASS.get());
         helper.setBlock(new BlockPos(x, 3, 6), (Block)ModBlocks.GREENHOUSE_GLASS.get());
      }
      helper.setBlock(new BlockPos(2, 2, 2), (Block)ModBlocks.SPORE_FILTER.get());
      helper.setBlock(new BlockPos(3, 2, 2), (Block)ModBlocks.POLLINATOR_DRONE_DOCK.get());
      ReclamationProgress.GreenhouseScan open = ReclamationProgress.scanGreenhouse(helper.getLevel(), helper.absolutePos(openCenter));
      helper.assertTrue(open.score() >= 30, "Open greenhouse support should still provide partial safety");
      helper.assertTrue(open.score() < ReclamationContent.progression().greenhouseSafeThreshold(), "Open support should not reach safe greenhouse threshold");
      helper.assertFalse(open.enclosed(), "Open greenhouse support should report a leaking enclosure");
      ReclamationProgress.GreenhouseContext outdoor = ReclamationProgress.GreenhouseScan.empty().asContext();
      helper.assertFalse(outdoor.established(), "Outdoor crops should not be treated as a saved greenhouse zone");
      helper.assertTrue(outdoor.growthPenalty() == 0, "Outdoor crops without an established zone should not receive greenhouse penalties");
      ReclamationProgress.recordGreenhouseZone(helper.getLevel(), helper.absolutePos(openCenter), open);
      ReclamationProgress.GreenhouseContext unsafe = ReclamationProgress.greenhouseContext(helper.getLevel(), helper.absolutePos(openCenter));
      helper.assertTrue(unsafe.established(), "Controller scan should establish a saved greenhouse zone");
      helper.assertTrue(unsafe.growthPenalty() > 0, "Unsafe established greenhouse zone should apply a soft growth penalty");
      SeedProfile testProfile = new SeedProfile("clean_corn", 1, 70);
      helper.assertTrue(
         ReclamationCropLogic.canGrow(CropSpec.byPath("clean_corn"), SoilState.STABILIZED, testProfile, unsafe),
         "Unsafe greenhouse zone should not hard-block crops on valid soil"
      );
      helper.assertTrue(
         ReclamationCropLogic.growthChance(CropSpec.byPath("clean_corn"), SoilState.STABILIZED, testProfile, unsafe, 0)
            < ReclamationCropLogic.growthChance(CropSpec.byPath("clean_corn"), SoilState.STABILIZED, testProfile, unsafe.score(), 0),
         "Unsafe greenhouse zone should reduce growth chance softly"
      );

      BlockPos sealedController = new BlockPos(16, 2, 14);
      buildSealedGreenhouse(helper, sealedController);
      ReclamationProgress.GreenhouseScan sealed = ReclamationProgress.scanGreenhouse(helper.getLevel(), helper.absolutePos(sealedController));
      helper.assertTrue(sealed.enclosed(), "Complete Greenhouse Glass shell should report a sealed enclosure");
      helper.assertTrue(sealed.greenhouseRoof(), "Sealed greenhouse should detect Greenhouse Glass overhead");
      helper.assertTrue(sealed.activeDocks() >= 1, "Pollinator Dock should become active when crops or trays are nearby");
      helper.assertTrue(sealed.score() >= ReclamationContent.progression().greenhouseSafeThreshold(), "Sealed greenhouse should reach safe threshold");
      ReclamationWorldData.GreenhouseZoneProfile profile = ReclamationProgress.recordGreenhouseZone(helper.getLevel(), helper.absolutePos(sealedController), sealed);
      helper.assertTrue(profile.score() == sealed.score(), "Saved greenhouse profile should preserve the controller scan score");
      ReclamationProgress.GreenhouseContext safe = ReclamationProgress.greenhouseContext(helper.getLevel(), helper.absolutePos(sealedController));
      helper.assertTrue(safe.established(), "Sealed controller scan should establish a greenhouse zone");
      helper.assertTrue(safe.quality() == ReclamationProgress.GreenhouseZoneQuality.SAFE, "Sealed greenhouse zone should be safe");
      helper.assertTrue(safe.growthPenalty() == 0, "Safe greenhouse zone should not apply growth penalties");
      helper.setBlock(sealedController, Blocks.AIR);
      ReclamationProgress.GreenhouseContext stale = ReclamationProgress.greenhouseContext(helper.getLevel(), helper.absolutePos(sealedController));
      helper.assertTrue(stale.score() == 0, "Removed controller should prevent stale greenhouse profiles from over-crediting safety");
      helper.assertTrue(stale.nextAction().contains("missing"), "Stale greenhouse profiles should explain that the controller or structure is missing");
      helper.succeed();
   }

   private static void growthGreenhouseCacheRegression(GameTestHelper helper) {
      ReclamationProgress.clearGrowthGreenhouseSafetyCacheForTests();
      ServerLevel level = helper.getLevel();
      BlockPos controller = new BlockPos(8, 2, 8);
      buildSealedGreenhouse(helper, controller);
      ReclamationProgress.recordGreenhouseZone(level, helper.absolutePos(controller), ReclamationProgress.scanGreenhouse(level, helper.absolutePos(controller)));
      int exact = ReclamationProgress.greenhouseContext(level, helper.absolutePos(controller)).score();
      int cached = ReclamationProgress.growthGreenhouseSafety(level, helper.absolutePos(controller));
      helper.assertTrue(cached == exact, "Growth greenhouse cache should match exact scoring for the first section scan");

      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      List<HydroponicTrayBlockEntity> trays = new java.util.ArrayList<>();
      for (int x = controller.getX() - 1; x <= controller.getX() + 1; x++) {
         for (int z = controller.getZ() - 1; z <= controller.getZ() + 1; z++) {
            BlockPos trayLocal = new BlockPos(x, controller.getY() + 1, z);
            helper.setBlock(trayLocal, (Block)ModBlocks.HYDROPONIC_TRAY.get());
            HydroponicTrayBlockEntity tray = helper.getBlockEntity(trayLocal, HydroponicTrayBlockEntity.class);
            ItemStack seed = new ItemStack(ModItems.STABILIZED_SEED.get());
            seed.set(ModItems.seedProfileComponent(), new SeedProfile("clean_corn", 0, 100));
            helper.assertTrue(tray.insertSeed(player, seed), "Growth cache regression tray should accept a profiled seed");
            tray.addNutrient(player, new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get()));
            trays.add(tray);
         }
      }

      for (int ticks = 0; ticks < ReclamationContent.machines().hydroponicGrowthTicks() * 10; ticks++) {
         for (HydroponicTrayBlockEntity tray : trays) {
            HydroponicTrayBlockEntity.tick(level, tray.getBlockPos(), tray.getBlockState(), tray);
         }
      }
      int agedTrays = 0;
      for (HydroponicTrayBlockEntity tray : trays) {
         if (tray.age() > 0) {
            agedTrays++;
         }
      }
      helper.assertTrue(agedTrays > 0, "Cached greenhouse growth checks should still let seeded trays advance");
      helper.assertTrue(ReclamationProgress.growthGreenhouseSafetyCacheSizeForTests() == 1,
         "Many growth checks in one block section should share one greenhouse safety cache entry");
      helper.succeed();
   }

   private static void pollinatorDroneSystem(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPos controller = new BlockPos(8, 2, 8);
      buildSealedGreenhouse(helper, controller);
      BlockPos dockLocal = controller.west();
      BlockPos dock = helper.absolutePos(dockLocal);
      BlockPos trayLocal = controller.south();
      BlockPos trayPos = helper.absolutePos(trayLocal);
      HydroponicTrayBlockEntity tray = helper.getBlockEntity(trayLocal, HydroponicTrayBlockEntity.class);
      ItemStack seed = new ItemStack(ModItems.STABILIZED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), new SeedProfile("clean_corn", 0, 100));
      helper.assertTrue(tray.insertSeed(player, seed), "Pollinator test tray should accept a profiled seed");
      tray.addNutrient(player, new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get()));
      ReclamationProgress.recordGreenhouseZone(level, helper.absolutePos(controller), ReclamationProgress.scanGreenhouse(level, helper.absolutePos(controller)));
      ReclamationProgress.GreenhouseContext preDeploy = ReclamationProgress.greenhouseContext(level, dock);
      helper.assertTrue(preDeploy.nextAction().contains("deploy a Pollinator Drone"),
         "Safe greenhouse diagnostics should recommend drone deployment when service targets exist");

      PollinatorDroneEntity drone = PollinatorDroneEntity.deployOrFind(level, dock);
      int deployedCount = PollinatorDroneEntity.boundDroneCount(level, dock);
      helper.assertTrue(deployedCount == 1, "Dock deploy should create exactly one bound Pollinator Drone, found " + deployedCount
         + " (removed=" + drone.isRemoved() + ")");
      List<PollinatorDroneEntity> queriedDrones = PollinatorDroneEntity.boundDrones(level, dock);
      long distinctDroneIds = queriedDrones.stream().map(PollinatorDroneEntity::getUUID).distinct().count();
      helper.assertTrue(queriedDrones.size() == 1 && distinctDroneIds == 1,
         "Bound drone lookup should de-duplicate overlapping entity queries by drone id");
      PollinatorDroneEntity sameDrone = PollinatorDroneEntity.deployOrFind(level, dock);
      helper.assertTrue(sameDrone == drone && PollinatorDroneEntity.boundDroneCount(level, dock) == 1,
         "Repeated dock deploy should reuse the existing bound Pollinator Drone");

      PollinatorDroneEntity duplicate = ModEntities.POLLINATOR_DRONE.get().create(level, EntitySpawnReason.EVENT);
      helper.assertTrue(duplicate != null, "Duplicate Pollinator Drone should instantiate for cleanup test");
      duplicate.configureDock(dock);
      duplicate.setPos(dock.getX() + 0.5D, dock.getY() + 1.25D, dock.getZ() + 0.5D);
      level.addFreshEntity(duplicate);
      PollinatorDroneEntity autoKeeper = PollinatorDroneEntity.cleanupDuplicateDrones(level, dock, null);
      helper.assertTrue(autoKeeper == drone, "Duplicate cleanup without a preferred drone should keep the oldest valid bound drone");
      helper.assertTrue(PollinatorDroneEntity.boundDroneCount(level, dock) == 1 && duplicate.isRemoved(),
         "Duplicate Pollinator Drones for one dock should be recalled");

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
      drone.saveWithoutId(output);
      PollinatorDroneEntity loaded = ModEntities.POLLINATOR_DRONE.get().create(level, EntitySpawnReason.EVENT);
      helper.assertTrue(loaded != null, "Reloaded Pollinator Drone should instantiate");
      loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
      helper.assertTrue(loaded.homeDock().equals(dock), "Pollinator Drone should persist its home dock");
      helper.assertTrue(loaded.targetPos().equals(drone.targetPos()), "Pollinator Drone should persist its current target");
      helper.assertTrue(loaded.serviceCooldown() == drone.serviceCooldown(), "Pollinator Drone should persist its service cooldown");
      helper.assertTrue(loaded.serviceCount() == drone.serviceCount(), "Pollinator Drone should persist its service count");

      for (int ticks = 0; ticks < 120 && drone.serviceCount() == 0; ticks++) {
         drone.tick();
      }
      helper.assertTrue(drone.serviceCount() > 0, "Pollinator Drone should service greenhouse crop/tray targets");
      int ageBefore = tray.age();
      for (int attempts = 0; attempts < 100 && tray.age() == ageBefore; attempts++) {
         ReclamationProgress.servicePollinationTarget(level, trayPos, 200);
      }
      helper.assertTrue(tray.age() > ageBefore, "Safe greenhouse pollinator service should be able to advance tray growth");

      BlockPos openController = new BlockPos(4, 2, 18);
      BlockPos openTrayLocal = openController.south();
      helper.setBlock(openController, (Block)ModBlocks.GREENHOUSE_CONTROLLER.get());
      helper.setBlock(openController.west(), (Block)ModBlocks.POLLINATOR_DRONE_DOCK.get());
      helper.setBlock(openController.east(), (Block)ModBlocks.SPORE_FILTER.get());
      helper.setBlock(openTrayLocal, (Block)ModBlocks.HYDROPONIC_TRAY.get());
      HydroponicTrayBlockEntity openTray = helper.getBlockEntity(openTrayLocal, HydroponicTrayBlockEntity.class);
      ItemStack openSeed = new ItemStack(ModItems.STABILIZED_SEED.get());
      openSeed.set(ModItems.seedProfileComponent(), new SeedProfile("clean_corn", 0, 100));
      helper.assertTrue(openTray.insertSeed(player, openSeed), "Unsafe greenhouse test tray should accept a profiled seed");
      BlockPos openControllerPos = helper.absolutePos(openController);
      int softZoneScore = Math.max(0, ReclamationContent.progression().greenhouseSafeThreshold() - 10);
      ReclamationWorldData.get(level).setGreenhouseZone(new ChunkPos(openControllerPos.getX() >> 4, openControllerPos.getZ() >> 4),
         new ReclamationWorldData.GreenhouseZoneProfile(
            softZoneScore, 24, 0, 0, 1, 1, 0, 1, 0, 1,
            false, false, false, openControllerPos.getX(), openControllerPos.getY(), openControllerPos.getZ(), level.getGameTime()
         ));
      ReclamationProgress.clearGrowthGreenhouseSafetyCacheForTests();
      ReclamationProgress.GreenhouseContext openContext = ReclamationProgress.greenhouseContext(level, helper.absolutePos(openTrayLocal));
      helper.assertTrue(openContext.established() && openContext.quality() != ReclamationProgress.GreenhouseZoneQuality.SAFE,
         "Open greenhouse should establish a strained or unsafe zone for soft service (established=" + openContext.established()
            + ", quality=" + openContext.quality().label() + ", score=" + openContext.score() + ", saved=" + openContext.savedScore()
            + ", live=" + openContext.liveScore() + ", profile=" + softZoneScore + ")");
      helper.assertTrue(openContext.pollinationBonus(12) > 0 && openContext.pollinationBonus(12) < 12,
         "Strained/unsafe greenhouse zones should reduce but not remove pollinator service");
      helper.assertTrue(ReclamationProgress.servicePollinationTarget(level, helper.absolutePos(openTrayLocal), 12),
         "Strained/unsafe established greenhouse service should not hard-block trays");

      BlockPos outdoorTrayLocal = new BlockPos(20, 1, 30);
      helper.setBlock(outdoorTrayLocal, (Block)ModBlocks.HYDROPONIC_TRAY.get());
      HydroponicTrayBlockEntity outdoorTray = helper.getBlockEntity(outdoorTrayLocal, HydroponicTrayBlockEntity.class);
      ItemStack outdoorSeed = new ItemStack(ModItems.STABILIZED_SEED.get());
      outdoorSeed.set(ModItems.seedProfileComponent(), new SeedProfile("clean_corn", 0, 100));
      helper.assertTrue(outdoorTray.insertSeed(player, outdoorSeed), "Outdoor tray should accept a profiled seed");
      helper.assertFalse(ReclamationProgress.servicePollinationTarget(level, helper.absolutePos(outdoorTrayLocal), 200),
         "Outdoor tray without an established greenhouse zone should not receive pollinator service");
      helper.assertTrue(outdoorTray.age() == 0, "Outdoor tray should not advance from pollinator service");

      int recalled = PollinatorDroneEntity.recallDrones(level, dock);
      helper.assertTrue(recalled == 1 && PollinatorDroneEntity.boundDroneCount(level, dock) == 0,
         "Dock recall should remove the bound Pollinator Drone");
      PollinatorDroneEntity orphan = PollinatorDroneEntity.deployOrFind(level, dock);
      helper.setBlock(dockLocal, Blocks.AIR);
      orphan.tick();
      helper.assertTrue(orphan.isRemoved(), "Pollinator Drone should discard itself when its dock is removed");
      helper.succeed();
   }

   private static void seedStabilizationProfile(GameTestHelper helper) {
      SeedProfile contaminated = new SeedProfile("ash_wheat", 3, 22);
      SeedProfile stable = contaminated.stabilized();
      helper.assertTrue(stable.contaminationTier() == 0, "Stabilized seed should clear contamination tier");
      helper.assertTrue(stable.stability() == 100, "Stabilized seed should reach 100 stability");
      helper.succeed();
   }

   private static void terminalStatusMetrics(GameTestHelper helper) {
      if (!ModList.get().isLoaded("echoterminal")) {
         helper.succeed();
         return;
      }
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ReclamationProgress.discoverSeed(player, CropSpec.byPath("ash_wheat"));
      ReclamationProgress.max(player, "crop_stability", 100);
      ReclamationProgress.max(player, "food_security", 30);
      Identifier seedMission = firstTerminalMissionId();
      String status = terminalSnapshotStatus(player, seedMission);
      helper.assertTrue("CLAIMABLE".equals(status) || "UNLOCKED".equals(status),
         "Terminal mission snapshot should render seed recovery state");
      helper.assertTrue("OPTIONAL".equals(terminalMissionRole(player, seedMission)),
         "Agriculture missions should be optional side-route records in the aggregate Terminal route");
      helper.assertTrue("2:0:OPTIONAL:true".equals(terminalRoutePlacement(player, seedMission)),
         "Agriculture missions should publish explicit Phase 02 optional route placement");
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      helper.assertTrue(metrics.knownSeeds() >= 1, "Terminal metrics should include known seeds");
      String greenhouseDetail = terminalSnapshotDetail(player, ReclamationTerminalIds.id("mission/greenhouse_online"));
      helper.assertTrue(greenhouseDetail.contains("greenhouse") || greenhouseDetail.contains("Greenhouse"),
         "Terminal greenhouse mission detail should mention greenhouse state");
      helper.assertTrue(greenhouseDetail.contains("zone"), "Terminal greenhouse mission detail should include zone quality");
      helper.assertTrue(greenhouseDetail.contains("drone") && greenhouseDetail.contains("service target"),
         "Terminal greenhouse mission detail should include drone service diagnostics");
      helper.assertTrue(greenhouseDetail.contains("Scan a Greenhouse Controller"),
         "Terminal greenhouse mission detail should include an actionable greenhouse next step");
      helper.succeed();
   }

   private static void worldDataCodecRoundTrip(GameTestHelper helper) {
      ChunkPos chunk = new ChunkPos(3, -2);
      ReclamationWorldData saved = new ReclamationWorldData();
      saved.setRestorationScore(chunk, 77);
      saved.setGreenhouseSafety(chunk, 64);
      saved.setGreenhouseZone(chunk, new ReclamationWorldData.GreenhouseZoneProfile(
         72, 48, 24, 18, 1, 1, 0, 3, 2, 4, true, true, true, 48, 51, 64, 700L
      ));
      saved.setLastSoilState(chunk, SoilState.STABILIZED.displayName());
      saved.addStat("soil_purified", 5);

      JsonElement encoded = ReclamationWorldData.CODEC.encodeStart(JsonOps.INSTANCE, saved)
         .result()
         .orElseThrow(() -> new IllegalStateException("Reclamation world data should encode"));
      ReclamationWorldData decoded = ReclamationWorldData.CODEC.parse(JsonOps.INSTANCE, encoded)
         .result()
         .orElseThrow(() -> new IllegalStateException("Reclamation world data should decode"));

      helper.assertTrue(decoded.restorationScore(chunk) == 77, "Restoration score should survive saved-data serialization");
      helper.assertTrue(decoded.greenhouseSafety(chunk) == 72, "Greenhouse safety should preserve the latest saved zone score");
      ReclamationWorldData.GreenhouseZoneProfile decodedZone = decoded.greenhouseZone(chunk);
      helper.assertTrue(decodedZone != null, "Greenhouse zone profile should survive saved-data serialization");
      helper.assertTrue(decodedZone.controllerPos().equals(new BlockPos(48, 51, 64)), "Greenhouse zone controller position should survive serialization");
      helper.assertTrue(decodedZone.activeDocks() == 1 && decodedZone.cropTargets() == 3, "Greenhouse zone support details should survive serialization");
      helper.assertTrue(decodedZone.deployedDrones() == 2 && decodedZone.serviceTargets() == 4, "Greenhouse zone drone details should survive serialization");
      helper.assertTrue(decoded.lastSoilState(chunk).equals(SoilState.STABILIZED.displayName()), "Last soil state should survive saved-data serialization");
      helper.assertTrue(decoded.stat("soil_purified") == 5, "World stats should survive saved-data serialization");
      helper.assertTrue(decoded.stat("greenhouse_zone_scans") == 1, "Greenhouse zone scan stats should survive saved-data serialization");
      helper.succeed();
   }

   private static void blockEntityPersistenceRoundTrip(GameTestHelper helper) {
      BlockPos trayLocal = new BlockPos(1, 1, 1);
      helper.setBlock(trayLocal, (Block)ModBlocks.HYDROPONIC_TRAY.get());
      HydroponicTrayBlockEntity tray = helper.getBlockEntity(trayLocal, HydroponicTrayBlockEntity.class);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ItemStack seed = new ItemStack(ModItems.STABILIZED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), new SeedProfile("clean_corn", 0, 100));
      helper.assertTrue(tray.insertSeed(player, seed), "Hydroponic tray should accept seed before persistence test");
      tray.addNutrient(player, new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get()));
      for (int i = 0; i < 1800 && tray.age() == 0; i++) {
         HydroponicTrayBlockEntity.tick(helper.getLevel(), tray.getBlockPos(), tray.getBlockState(), tray);
      }

      TagValueOutput trayOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      tray.saveWithoutMetadata(trayOutput);
      CompoundTag trayTag = trayOutput.buildResult();
      HydroponicTrayBlockEntity loadedTray = new HydroponicTrayBlockEntity(helper.absolutePos(trayLocal), tray.getBlockState());
      loadedTray.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), trayTag));

      helper.assertTrue(loadedTray.profile() != null && loadedTray.profile().cropId().equals("clean_corn"), "Tray seed profile should survive block-entity serialization");
      helper.assertTrue(loadedTray.age() == tray.age(), "Tray crop age should survive block-entity serialization");
      helper.assertTrue(loadedTray.nutrient() == tray.nutrient(), "Tray nutrient buffer should survive block-entity serialization");

      SeedProfile cropProfile = new SeedProfile("clean_corn", 1, 43);
      ReclamationCropBlockEntity crop = new ReclamationCropBlockEntity(
         helper.absolutePos(new BlockPos(2, 1, 1)),
         ModBlocks.CLEAN_CORN_CROP.get().plantedState(cropProfile)
      );
      crop.setProfile(cropProfile);
      TagValueOutput cropOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      crop.saveWithoutMetadata(cropOutput);
      ReclamationCropBlockEntity loadedCrop = new ReclamationCropBlockEntity(crop.getBlockPos(), crop.getBlockState());
      loadedCrop.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), cropOutput.buildResult()));
      helper.assertTrue(loadedCrop.profile() != null && loadedCrop.profile().cropId().equals("clean_corn"), "Crop block entity should preserve crop id");
      helper.assertTrue(loadedCrop.profile().contaminationTier() == 1 && loadedCrop.profile().stability() == 43, "Crop block entity should preserve seed profile values");
      helper.succeed();
   }

   private static void terminalRewardClaimOnce(GameTestHelper helper) {
      if (!ModList.get().isLoaded("echoterminal")) {
         helper.succeed();
         return;
      }
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      ReclamationProgress.discoverSeed(player, CropSpec.byPath("ash_wheat"));
      Identifier missionId = firstTerminalMissionId();
      int nutrientBefore = ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get());
      int enzymeBefore = ReclamationProgress.count(player, ModItems.PURIFICATION_ENZYME.get());

      helper.assertTrue(invokeTerminalAction(player, missionId, "claim_cache"), "Completed terminal mission should claim its cache once");
      int nutrientAfterFirst = ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get());
      int enzymeAfterFirst = ReclamationProgress.count(player, ModItems.PURIFICATION_ENZYME.get());
      helper.assertTrue(nutrientAfterFirst - nutrientBefore == 2, "Recover Seed cache should grant exactly two nutrient mix");
      helper.assertTrue(enzymeAfterFirst - enzymeBefore == 1, "Recover Seed cache should grant exactly one purification enzyme");
      helper.assertTrue(ReclamationProgress.claimed(player, "recover_seed"), "Claim should persist the terminal mission claim flag");

      helper.assertFalse(invokeTerminalAction(player, missionId, "claim_cache"), "Already claimed terminal mission should reject duplicate rewards");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get()) == nutrientAfterFirst, "Duplicate claim should not add nutrient mix");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.PURIFICATION_ENZYME.get()) == enzymeAfterFirst, "Duplicate claim should not add purification enzyme");
      helper.succeed();
   }

   private static void lootModifierPackaging(GameTestHelper helper) {
      ClassLoader loader = ModGameTests.class.getClassLoader();
      helper.assertTrue(
         loader.getResource("data/echoagriculturereclamation/loot_modifiers/seed_vault_capsules.json") != null,
         "Agriculture loot modifiers should be packaged under NeoForge's plural loot_modifiers directory"
      );
      helper.assertTrue(
         loader.getResource("data/echoagriculturereclamation/loot_modifier/seed_vault_capsules.json") == null,
         "Agriculture should not package stale singular loot_modifier resources"
      );
      helper.succeed();
   }

   private static void standaloneRecipeChain(GameTestHelper helper) {
      JsonObject bioReactor = recipe("bio_reactor");
      helper.assertTrue(containsJsonString(bioReactor, "echoagriculturereclamation:soil_nutrient_mix"),
         "Bio-Reactor should be craftable from early soil nutrient mix");
      helper.assertFalse(containsJsonString(bioReactor, "echoagriculturereclamation:bio_gel"),
         "Bio-Reactor recipe should not require Bio-Gel");

      JsonObject compostRecycler = recipe("compost_recycler");
      helper.assertTrue(containsJsonString(compostRecycler, "echoagriculturereclamation:soil_nutrient_mix"),
         "Compost Recycler should be craftable from early soil nutrient mix");
      helper.assertFalse(containsJsonString(compostRecycler, "echoagriculturereclamation:bio_gel"),
         "Compost Recycler recipe should not require Bio-Gel");

      JsonObject geneStabilizer = recipe("gene_stabilizer");
      helper.assertTrue(containsJsonString(geneStabilizer, "echoagriculturereclamation:bio_gel"),
         "Gene Stabilizer should still require player-made Bio-Gel");
      helper.assertTrue(containsJsonString(geneStabilizer, "echoagriculturereclamation:soil_nutrient_mix"),
         "Gene Stabilizer should use early Agriculture material instead of Gene Sample");
      helper.assertFalse(containsJsonString(geneStabilizer, "echoagriculturereclamation:gene_sample"),
         "Gene Stabilizer recipe should not require Gene Sample");
      helper.succeed();
   }

   private static void coreRecoveryOnce(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      int capsulesBefore = ReclamationProgress.count(player, ModItems.RECOVERED_SEED_CAPSULE.get());

      helper.assertTrue(EchoCoreServices.recover(player, "agriculture_seed_cache"), "Core recovery should grant the Agriculture seed cache once");
      helper.assertTrue(
         ReclamationProgress.count(player, ModItems.RECOVERED_SEED_CAPSULE.get()) == capsulesBefore + 2,
         "Core recovery should grant exactly two recovered seed capsules"
      );
      helper.assertTrue(ReclamationProgress.flag(player, "seed_recovered"), "Core recovery should mark the seed recovery route");
      helper.assertTrue(ReclamationProgress.claimed(player, "recovery_agriculture_seed_cache"), "Core recovery should persist a local claim flag");

      helper.assertFalse(EchoCoreServices.recover(player, "agriculture_seed_cache"), "Core recovery should reject duplicate seed cache claims");
      helper.assertTrue(
         ReclamationProgress.count(player, ModItems.RECOVERED_SEED_CAPSULE.get()) == capsulesBefore + 2,
         "Duplicate Core recovery should not grant more capsules"
      );
      helper.succeed();
   }

   private static void playerProgressNbtRoundTrip(GameTestHelper helper) {
      Player original = helper.makeMockPlayer(GameType.SURVIVAL);
      ReclamationProgress.discoverSeed(original, CropSpec.byPath("ash_wheat"));
      ReclamationProgress.mark(original, "first_growth");
      ReclamationProgress.add(original, "crops_grown", 3);
      ReclamationProgress.add(original, "soil_purified", 4);
      ReclamationProgress.claim(original, "recover_seed");
      ReclamationProgress.claim(original, "recovery_agriculture_seed_cache");

      CompoundTag saved = original.getPersistentData().getCompoundOrEmpty(ReclamationProgress.ROOT).copy();
      Player loaded = helper.makeMockPlayer(GameType.SURVIVAL);
      loaded.getPersistentData().put(ReclamationProgress.ROOT, saved);

      helper.assertTrue(ReclamationProgress.knownSeeds(loaded).contains("ash_wheat"), "Known seed list should survive player NBT round trip");
      helper.assertTrue(ReclamationProgress.flag(loaded, "first_growth"), "Progress flags should survive player NBT round trip");
      helper.assertTrue(ReclamationProgress.value(loaded, "crops_grown") == 3, "Progress counters should survive player NBT round trip");
      helper.assertTrue(ReclamationProgress.value(loaded, "soil_purified") == 4, "Soil counters should survive player NBT round trip");
      helper.assertTrue(ReclamationProgress.claimed(loaded, "recover_seed"), "Terminal claim flags should survive player NBT round trip");
      helper.assertTrue(ReclamationProgress.claimed(loaded, "recovery_agriculture_seed_cache"), "Core recovery claim flags should survive player NBT round trip");
      helper.succeed();
   }

   private static void restorationScoreConversion(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      BlockPos local = new BlockPos(1, 1, 1);
      BlockPos absolute = helper.absolutePos(local);
      helper.setBlock(local, (Block)ModBlocks.PURIFIED_SOIL.get());
      ChunkPos chunk = new ChunkPos(absolute.getX() >> 4, absolute.getZ() >> 4);
      ReclamationWorldData.get(level).setRestorationScore(chunk, 100);
      int changed = ReclamationRestoration.stabilizeArea(level, absolute, 1, 1);
      helper.assertTrue(changed >= 1, "Restoration score should be able to stabilize local soil blocks");
      helper.succeed();
   }

   private static void coreRouteRecords(GameTestHelper helper) {
      ReclamationCoreIntegration.registerAddonChapter();
      helper.assertTrue(EchoAddonRegistry.isRegistered(ReclamationCoreIntegration.CHAPTER_ID), "Core chapter should be registered");
      List<EchoRouteRecord> records = EchoCoreServices.routeRecords(helper.makeMockPlayer(GameType.CREATIVE)).stream()
         .filter(record -> ReclamationCoreIntegration.CHAPTER_ID.equals(record.chapterId()))
         .toList();
      helper.assertTrue(!records.isEmpty(), "Core route records should include Agriculture Reclamation");
      helper.succeed();
   }

   private static void crossAddonSoilCompatibility(GameTestHelper helper) {
      helper.assertTrue(
         ReclamationCrossAddonIntegration.externalSoilState(Identifier.fromNamespaceAndPath("echoashfallprotocol", "contaminated_soil")) == SoilState.CONTAMINATED,
         "Ashfall contaminated soil id should map to Agriculture contaminated soil state"
      );
      helper.assertTrue(
         ReclamationCrossAddonIntegration.externalSoilState(Identifier.fromNamespaceAndPath("echoashfallprotocol", "toxic_puddle")) == SoilState.TOXIC_MUD,
         "Ashfall toxic puddle id should map to Agriculture toxic mud state"
      );
      helper.assertTrue(
         ReclamationCrossAddonIntegration.externalSoilState(Identifier.fromNamespaceAndPath("echoashfallprotocol", "irradiated_crust")) == SoilState.IRRADIATED,
         "Ashfall irradiated crust id should map to Agriculture irradiated soil state"
      );
      helper.assertTrue(
         ReclamationCrossAddonIntegration.externalSoilState(Identifier.fromNamespaceAndPath("restoration_project", "restored_soil")) == SoilState.RESTORED,
         "Restoration Project restored soil id should map to Agriculture restored soil state"
      );
      helper.succeed();
   }

   private static void coreMilestoneRecording(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ReclamationProgress.mark(player, "first_growth");
      ReclamationProgress.mark(player, "greenhouse_online");
      ReclamationProgress.mark(player, "restore_chunk");
      helper.assertTrue(
         ReclamationProgress.coreMilestoneForFlag("first_growth").equals("first_growth"),
         "First growth should map to an ECHO Core milestone id"
      );
      helper.assertTrue(
         ReclamationProgress.coreMilestoneForFlag("greenhouse_online").equals("greenhouse_online"),
         "Greenhouse online should map to an ECHO Core milestone id"
      );
      helper.assertTrue(
         ReclamationProgress.coreMilestoneForFlag("restore_chunk").equals("restore_chunk"),
         "Chunk restoration should map to an ECHO Core milestone id"
      );
      helper.succeed();
   }

   private static void factionBiasedSeedRecovery(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      Identifier remnant = Identifier.fromNamespaceAndPath("echoashfallprotocol", "radwarden_compact");
      ensureFaction(remnant, "Radwarden Compact");
      EchoCoreServices.setFactionReputation(player, remnant, 55);

      CropSpec spec = CropSpec.byPath("clean_corn");
      SeedProfile baseline = ReclamationContent.progression().recoveredProfile(spec, RandomSource.create(311L));
      SeedProfile biased = ReclamationCrossAddonIntegration.recoveredProfile(player, spec, RandomSource.create(311L));
      helper.assertTrue(
         ReclamationCrossAddonIntegration.factionPreference(player) == FactionPreference.REMNANT,
         "Trusted Radwarden standing should be detected as Agriculture seed preference"
      );
      helper.assertTrue(biased.stability() > baseline.stability(), "Radwarden seed recovery should improve stability");
      helper.assertTrue(biased.contaminationTier() < baseline.contaminationTier(), "Radwarden seed recovery should reduce contamination");
      helper.succeed();
   }

   private static void mainProgressionLoopRegression(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos soilLocal = new BlockPos(1, 1, 1);
      BlockPos trayLocal = new BlockPos(2, 1, 1);
      helper.setBlock(soilLocal, (Block)ModBlocks.CONTAMINATED_SOIL.get());
      int purified = ReclamationRestoration.purifyArea(level, helper.absolutePos(soilLocal), 1, 1);
      helper.assertTrue(purified == 1, "Main loop should purify contaminated soil before cultivation");

      ItemStack capsule = new ItemStack(ModItems.RECOVERED_SEED_CAPSULE.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
      helper.assertTrue(
         ModItems.RECOVERED_SEED_CAPSULE.get().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS_SERVER,
         "Main loop should open a recovered seed capsule"
      );
      ItemStack seed = firstProfiledSeed(player, ModItems.CONTAMINATED_SEED.get());
      helper.assertFalse(seed.isEmpty(), "Main loop should create a profiled contaminated seed");
      SeedProfile profile = seed.get(ModItems.seedProfileComponent());

      helper.setBlock(trayLocal, (Block)ModBlocks.HYDROPONIC_TRAY.get());
      HydroponicTrayBlockEntity tray = helper.getBlockEntity(trayLocal, HydroponicTrayBlockEntity.class);
      helper.assertTrue(tray.insertSeed(player, seed), "Main loop should insert a profiled seed into hydroponics");
      ItemStack nutrient = new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), 4);
      for (int i = 0; i < 4; i++) {
         tray.addNutrient(player, nutrient);
      }
      for (int ticks = 0; ticks < 12000 && tray.age() < 7; ticks++) {
         HydroponicTrayBlockEntity.tick(level, tray.getBlockPos(), tray.getBlockState(), tray);
      }
      helper.assertTrue(tray.age() >= 7, "Main loop hydroponic crop should reach harvest age");
      helper.assertTrue(tray.harvest(player), "Main loop should harvest hydroponic crop output");
      helper.assertTrue(ReclamationProgress.flag(player, "first_growth"), "Harvest should mark first growth");
      helper.assertTrue(ReclamationProgress.value(player, "crops_grown") >= 1, "Harvest should increment grown crop count");
      ItemStack produce = firstItem(player, ModItems.produceFor(profile.spec()).get());
      helper.assertFalse(produce.isEmpty(), "Harvest should grant the correct produce");
      helper.assertTrue(ReclamationProgress.flag(player, "stabilization_seed_recovered"), "Unstable harvest should return a seed for stabilization");
      ReclamationMachineBlock bioReactor = (ReclamationMachineBlock)ModBlocks.BIO_REACTOR.get();
      helper.assertTrue(runMachine(bioReactor, "runBioReactor", produce, player) == InteractionResult.SUCCESS_SERVER,
         "Main loop should process first harvest in the Bio-Reactor");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.BIO_GEL.get()) > 0,
         "Bio-Reactor should make Bio-Gel from any first crop matter");

      ItemStack stabilizationSeed = firstProfiledSeed(player, ModItems.CONTAMINATED_SEED.get());
      int catalystBefore = ReclamationProgress.count(player, ModItems.BIO_GEL.get()) + ReclamationProgress.count(player, ModItems.GENE_SAMPLE.get());
      int stabilizedBefore = ReclamationProgress.count(player, ModItems.STABILIZED_SEED.get());
      ReclamationMachineBlock stabilizer = (ReclamationMachineBlock)ModBlocks.GENE_STABILIZER.get();
      helper.assertTrue(runMachine(stabilizer, "stabilizeSeed", stabilizationSeed, player) == InteractionResult.SUCCESS_SERVER,
         "Main loop should stabilize one recovered contaminated seed with machine catalyst");
      helper.assertTrue(ReclamationProgress.flag(player, "gene_stabilization"), "Stabilization should update progression");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.STABILIZED_SEED.get()) == stabilizedBefore + 1,
         "Gene Stabilizer should output exactly one stabilized seed");
      int catalystAfter = ReclamationProgress.count(player, ModItems.BIO_GEL.get()) + ReclamationProgress.count(player, ModItems.GENE_SAMPLE.get());
      helper.assertTrue(catalystAfter == catalystBefore - 1, "Gene Stabilizer should consume exactly one machine catalyst");
      helper.assertTrue(ReclamationProgress.value(player, "restoration_score") > 0, "Harvest should add local restoration pressure");
      helper.succeed();
   }

   private static void bioReactorUtilityOutputs(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ReclamationMachineBlock block = (ReclamationMachineBlock)ModBlocks.BIO_REACTOR.get();

      helper.assertTrue(runMachine(block, "runBioReactor", new ItemStack(ModItems.MEDICINAL_ALOE.get()), player) == InteractionResult.SUCCESS_SERVER,
         "Bio-Reactor should process Medicinal Aloe without requiring Ashfall bridge items");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.BIO_GEL.get()) >= 1, "Medicinal Aloe should produce Bio-Gel");

      int bioGelBefore = ReclamationProgress.count(player, ModItems.BIO_GEL.get());
      runMachine(block, "runBioReactor", new ItemStack(ModItems.SIGNAL_FUNGUS.get()), player);
      helper.assertTrue(ReclamationProgress.count(player, ModItems.BIO_GEL.get()) >= bioGelBefore + 2,
         "Signal Fungus should yield increased Bio-Gel");

      runMachine(block, "runBioReactor", new ItemStack(ModItems.CRYO_MOSS.get()), player);
      helper.assertTrue(ReclamationProgress.count(player, ModItems.BIO_GEL.get()) >= bioGelBefore + 3,
         "Cryo Moss should still produce Bio-Gel for the standalone stabilization loop");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.PURIFICATION_ENZYME.get()) >= 1,
         "Cryo Moss should yield Purification Enzyme");

      bioGelBefore = ReclamationProgress.count(player, ModItems.BIO_GEL.get());
      runMachine(block, "runBioReactor", new ItemStack(ModItems.NEXUS_ORCHID.get()), player);
      helper.assertTrue(ReclamationProgress.count(player, ModItems.BIO_GEL.get()) >= bioGelBefore + 1,
         "Nexus Orchid should still produce Bio-Gel for the standalone stabilization loop");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.GENE_SAMPLE.get()) >= 1,
         "Nexus Orchid should yield Gene Sample");
      BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("echonexusprotocol", "nexus_gel")).ifPresent(item ->
         helper.assertTrue(ReclamationProgress.count(player, item) >= 1, "Loaded Nexus bridge should grant nexus_gel")
      );
      helper.succeed();
   }

   private static void compostRecyclerUtilityOutputs(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ReclamationMachineBlock block = (ReclamationMachineBlock)ModBlocks.COMPOST_RECYCLER.get();

      helper.assertTrue(runMachine(block, "runCompostRecycler", new ItemStack(ModItems.FILTER_REED.get()), player) == InteractionResult.SUCCESS_SERVER,
         "Compost Recycler should process Filter Reed without requiring Ashfall bridge items");
      helper.assertTrue(ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get()) >= 3,
         "Filter Reed should yield increased Soil Nutrient Mix");

      int nutrientBefore = ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get());
      runMachine(block, "runCompostRecycler", new ItemStack(ModItems.CRYO_MOSS.get()), player);
      helper.assertTrue(ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get()) >= nutrientBefore + 2,
         "Cryo Moss compost should yield increased Soil Nutrient Mix");

      nutrientBefore = ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get());
      runMachine(block, "runCompostRecycler", new ItemStack(ModItems.SIGNAL_FUNGUS.get()), player);
      helper.assertTrue(ReclamationProgress.count(player, ModItems.SOIL_NUTRIENT_MIX.get()) >= nutrientBefore + 2,
         "Signal Fungus compost should yield increased Soil Nutrient Mix");

      BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("echoashfallprotocol", "plant_fiber")).ifPresent(item ->
         helper.assertTrue(ReclamationProgress.count(player, item) >= 1, "Loaded Ashfall bridge should grant plant_fiber")
      );
      helper.succeed();
   }

   private static void fieldSnapshotApi(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos controller = new BlockPos(8, 2, 8);
      buildSealedGreenhouse(helper, controller);
      ReclamationProgress.GreenhouseScan scan = ReclamationProgress.scanGreenhouse(helper.getLevel(), helper.absolutePos(controller));
      ReclamationProgress.recordGreenhouseZone(helper.getLevel(), helper.absolutePos(controller), scan);
      BlockPos controllerPos = helper.absolutePos(controller);
      ReclamationWorldData.get(helper.getLevel()).setRestorationScore(new ChunkPos(controllerPos.getX() >> 4, controllerPos.getZ() >> 4), 65);
      final int[] observed = {0};
      ReclamationIntegrationServices.registerFieldObserver(snapshot -> observed[0]++);
      ReclamationFieldSnapshot snapshot = ReclamationFieldQuery.at(helper.getLevel(), helper.absolutePos(controller), player);
      helper.assertTrue(snapshot != null, "Field snapshot should be available for a server position");
      helper.assertTrue(snapshot.restorationScore() == 65, "Field snapshot should expose chunk restoration score");
      helper.assertTrue("safe".equals(snapshot.greenhouseQuality()), "Field snapshot should expose saved greenhouse quality");
      helper.assertTrue(snapshot.hasController(), "Field snapshot should expose controller position");
      helper.assertTrue(snapshot.cropTargetCount() > 0, "Field snapshot should expose crop/tray target count");
      helper.assertTrue(observed[0] > 0, "Field snapshot query should notify registered read-only observers");
      ReclamationIntegrationServices.clearObserversForTests();
      helper.succeed();
   }

   private static void processDefinitionsIndexable(GameTestHelper helper) {
      helper.assertTrue(ReclamationContent.processes().containsKey("bio_reactor_biomass"),
         "Default process definitions should include the Bio-Reactor route");
      helper.assertTrue(ReclamationContent.processes().containsKey("gene_stabilization"),
         "Default process definitions should include gene stabilization");
      boolean indexHasProcess = ReclamationCoreIntegration.class != null && ReclamationContent.processes().values().stream()
         .allMatch(process -> process.title() != null && !process.title().isBlank());
      helper.assertTrue(indexHasProcess, "Process definitions should be title-safe for Index recipe cards");
      helper.succeed();
   }

   private static void machineEntityDiagnostics(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, (Block)ModBlocks.BIO_REACTOR.get());
      ReclamationMachineBlockEntity machine = helper.getBlockEntity(local, ReclamationMachineBlockEntity.class);
      machine.recordOperation("process/bio_reactor", 120, "Bio-Reactor action accepted.", "");
      helper.assertTrue(machine.lastOperation().contains("bio_reactor"), "Machine entity should remember the last operation");
      helper.assertTrue(machine.progressMax() > 0, "Machine entity should expose process progress for diagnostics");
      helper.assertTrue(machine.blockedReason().isBlank(), "Valid Bio-Reactor input should not leave a blocked reason");
      helper.succeed();
   }

   private static void mapProviderSnapshots(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos controller = new BlockPos(8, 2, 8);
      buildSealedGreenhouse(helper, controller);
      ReclamationProgress.recordGreenhouseZone(
         helper.getLevel(),
         helper.absolutePos(controller),
         ReclamationProgress.scanGreenhouse(helper.getLevel(), helper.absolutePos(controller))
      );
      List<?> markers = ReclamationMapDataProvider.INSTANCE.markers(player);
      helper.assertTrue(!markers.isEmpty(), "Agriculture map provider should emit player-local field markers");
      helper.assertTrue(ReclamationMapDataProvider.INSTANCE.layers(player).size() >= 3, "Agriculture map provider should expose restoration, greenhouse, and mission layers");
      helper.succeed();
   }

   private static void machineMenuProcessCompletion(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, (Block)ModBlocks.BIO_REACTOR.get());
      ReclamationMachineBlockEntity machine = helper.getBlockEntity(local, ReclamationMachineBlockEntity.class);
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      machine.setItem(ReclamationMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.SIGNAL_FUNGUS.get()));
      ReclamationMachineMenu menu = new ReclamationMachineMenu(1, player.getInventory(), machine, machine.data());
      helper.assertTrue(menu.kind() == ReclamationMachineBlock.MachineKind.BIO_REACTOR, "Machine menu should expose the block's machine kind");
      helper.assertTrue(menu.clickMenuButton(player, ReclamationMachineMenu.BUTTON_RUN), "Run button should start a valid Bio-Reactor process");
      helper.assertTrue(machine.isOperationActive(), "Valid machine process should become active");
      for (int tick = 0; tick <= machine.progressMax() + 2; tick++) {
         ReclamationMachineBlockEntity.tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
      }
      helper.assertTrue(!machine.getItem(ReclamationMachineBlockEntity.OUTPUT_SLOT).isEmpty(), "Completed process should buffer output in the output slot");
      helper.assertTrue(machine.getItem(ReclamationMachineBlockEntity.OUTPUT_SLOT).is(ModItems.BIO_GEL.get()), "Bio-Reactor output should be Bio-Gel");
      helper.assertTrue(!machine.isOperationActive(), "Completed process should clear active progress");
      helper.succeed();
   }

   private static void machineMenuBlockedOutput(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, (Block)ModBlocks.BIO_REACTOR.get());
      ReclamationMachineBlockEntity machine = helper.getBlockEntity(local, ReclamationMachineBlockEntity.class);
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      machine.setItem(ReclamationMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.SIGNAL_FUNGUS.get()));
      machine.setItem(ReclamationMachineBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND));
      helper.assertFalse(machine.startProcess(player), "Machine should reject a process when output cannot fit");
      helper.assertTrue(machine.blockedReason().contains("Output slot"), "Blocked process should expose output-slot diagnostics");
      helper.assertTrue(!machine.isOperationActive(), "Blocked process should not start progress");
      helper.succeed();
   }

   private static void machineQuickInsertFallback(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, (Block)ModBlocks.SEED_VAULT_TERMINAL.get());
      ReclamationMachineBlockEntity machine = helper.getBlockEntity(local, ReclamationMachineBlockEntity.class);
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack capsule = new ItemStack(ModItems.RECOVERED_SEED_CAPSULE.get(), 2);
      player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
      helper.assertTrue(machine.quickUse(player, capsule), "Quick use should insert a valid held capsule and start the shared process path");
      helper.assertTrue(capsule.getCount() == 1, "Quick insert should consume one held item");
      helper.assertTrue(machine.getItem(ReclamationMachineBlockEntity.INPUT_SLOT).is(ModItems.RECOVERED_SEED_CAPSULE.get()), "Quick insert should populate the input slot");
      helper.assertTrue(machine.isOperationActive(), "Quick insert should start the same block-entity process used by the menu");
      helper.succeed();
   }

   private static void buildSealedGreenhouse(GameTestHelper helper, BlockPos controller) {
      for (int x = controller.getX() - 2; x <= controller.getX() + 2; x++) {
         for (int z = controller.getZ() - 2; z <= controller.getZ() + 2; z++) {
            helper.setBlock(new BlockPos(x, controller.getY() - 1, z), (Block)ModBlocks.STABILIZED_SOIL.get());
            helper.setBlock(new BlockPos(x, controller.getY() + 2, z), (Block)ModBlocks.GREENHOUSE_GLASS.get());
         }
      }
      for (int y = controller.getY(); y <= controller.getY() + 1; y++) {
         for (int x = controller.getX() - 2; x <= controller.getX() + 2; x++) {
            helper.setBlock(new BlockPos(x, y, controller.getZ() - 2), (Block)ModBlocks.GREENHOUSE_GLASS.get());
            helper.setBlock(new BlockPos(x, y, controller.getZ() + 2), (Block)ModBlocks.GREENHOUSE_GLASS.get());
         }
         for (int z = controller.getZ() - 1; z <= controller.getZ() + 1; z++) {
            helper.setBlock(new BlockPos(controller.getX() - 2, y, z), (Block)ModBlocks.GREENHOUSE_GLASS.get());
            helper.setBlock(new BlockPos(controller.getX() + 2, y, z), (Block)ModBlocks.GREENHOUSE_GLASS.get());
         }
      }
      helper.setBlock(controller, (Block)ModBlocks.GREENHOUSE_CONTROLLER.get());
      helper.setBlock(controller.west(), (Block)ModBlocks.POLLINATOR_DRONE_DOCK.get());
      helper.setBlock(controller.east(), (Block)ModBlocks.SPORE_FILTER.get());
      helper.setBlock(controller.south(), (Block)ModBlocks.HYDROPONIC_TRAY.get());
   }

   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData(
         environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, 2
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }

   private static InteractionResult runMachine(ReclamationMachineBlock block, String methodName, ItemStack stack, Player player) {
      try {
         Method method = ReclamationMachineBlock.class.getDeclaredMethod(methodName, ItemStack.class, Player.class);
         method.setAccessible(true);
         return (InteractionResult)method.invoke(block, stack, player);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to invoke Agriculture machine test hook " + methodName, exception);
      }
   }

   private static JsonObject recipe(String name) {
      String path = "data/echoagriculturereclamation/recipe/" + name + ".json";
      try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
         if (input == null) {
            throw new AssertionError("Missing recipe resource: " + path);
         }
         try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
         }
      } catch (IOException exception) {
         throw new AssertionError("Unable to read recipe resource: " + path, exception);
      }
   }

   private static boolean containsJsonString(JsonElement element, String expected) {
      if (element == null || element.isJsonNull()) {
         return false;
      }
      if (element.isJsonPrimitive()) {
         return element.getAsJsonPrimitive().isString() && expected.equals(element.getAsString());
      }
      if (element.isJsonArray()) {
         for (JsonElement child : element.getAsJsonArray()) {
            if (containsJsonString(child, expected)) {
               return true;
            }
         }
         return false;
      }
      if (element.isJsonObject()) {
         for (JsonElement child : element.getAsJsonObject().entrySet().stream().map(java.util.Map.Entry::getValue).toList()) {
            if (containsJsonString(child, expected)) {
               return true;
            }
         }
      }
      return false;
   }

   private static ItemStack firstProfiledSeed(Player player, Item item) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty() && stack.is(item) && stack.get(ModItems.seedProfileComponent()) != null) {
            return stack;
         }
      }
      return ItemStack.EMPTY;
   }

   private static ItemStack firstItem(Player player, Item item) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty() && stack.is(item)) {
            return stack;
         }
      }
      return ItemStack.EMPTY;
   }

   private static int countProfiledSeeds(Player player) {
      int total = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(ModItems.CONTAMINATED_SEED.get()) && stack.get(ModItems.seedProfileComponent()) != null) {
            total += stack.getCount();
         }
      }
      return total;
   }

   private static Identifier firstTerminalMissionId() {
      try {
         Class<?> providerClass = Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider");
         List<?> missions = (List<?>)providerClass.getMethod("routeMissions").invoke(null);
         Object first = missions.get(0);
         return (Identifier)first.getClass().getMethod("id").invoke(first);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to inspect Agriculture terminal missions.", exception);
      }
   }

   private static String terminalSnapshotStatus(Player player, Identifier missionId) {
      try {
         Class<?> providerClass = Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider");
         Object provider = providerClass.getField("INSTANCE").get(null);
         Object snapshot = providerClass.getMethod("snapshot", Player.class, Identifier.class).invoke(provider, player, missionId);
         Object status = snapshot.getClass().getMethod("status").invoke(snapshot);
         return status instanceof Enum<?> value ? value.name() : String.valueOf(status);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to inspect Agriculture terminal snapshot.", exception);
      }
   }

   private static String terminalSnapshotDetail(Player player, Identifier missionId) {
      try {
         Class<?> providerClass = Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider");
         Object provider = providerClass.getField("INSTANCE").get(null);
         Object snapshot = providerClass.getMethod("snapshot", Player.class, Identifier.class).invoke(provider, player, missionId);
         return String.valueOf(snapshot.getClass().getMethod("unlockReason").invoke(snapshot));
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to inspect Agriculture terminal snapshot detail.", exception);
      }
   }

   private static String terminalMissionRole(Player player, Identifier missionId) {
      try {
         Class<?> providerClass = Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider");
         Object provider = providerClass.getField("INSTANCE").get(null);
         Object definition = terminalDefinition(providerClass, provider, player, missionId);
         Object snapshot = providerClass.getMethod("snapshot", Player.class, Identifier.class).invoke(provider, player, missionId);
         Object role = providerClass.getMethod("role", Player.class, definition.getClass(), snapshot.getClass())
            .invoke(provider, player, definition, snapshot);
         return role instanceof Enum<?> value ? value.name() : String.valueOf(role);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to inspect Agriculture terminal mission role.", exception);
      }
   }

   private static String terminalRoutePlacement(Player player, Identifier missionId) {
      try {
         Class<?> providerClass = Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider");
         Object provider = providerClass.getField("INSTANCE").get(null);
         Object definition = terminalDefinition(providerClass, provider, player, missionId);
         Object snapshot = providerClass.getMethod("snapshot", Player.class, Identifier.class).invoke(provider, player, missionId);
         Object role = providerClass.getMethod("role", Player.class, definition.getClass(), snapshot.getClass())
            .invoke(provider, player, definition, snapshot);
         Object placement = providerClass.getMethod("routePlacement", Player.class, definition.getClass(), snapshot.getClass(), role.getClass())
            .invoke(provider, player, definition, snapshot, role);
         if (!(placement instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
            return "";
         }
         Object value = optional.get();
         return value.getClass().getMethod("phaseOrder").invoke(value)
            + ":" + value.getClass().getMethod("missionOrder").invoke(value)
            + ":" + value.getClass().getMethod("role").invoke(value)
            + ":" + value.getClass().getMethod("includeInSurvivalRoute").invoke(value);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to inspect Agriculture terminal route placement.", exception);
      }
   }

   private static Object terminalDefinition(Class<?> providerClass, Object provider, Player player, Identifier missionId)
      throws ReflectiveOperationException {
      List<?> definitions = (List<?>)providerClass.getMethod("missions", Player.class).invoke(provider, player);
      for (Object definition : definitions) {
         Object id = definition.getClass().getMethod("id").invoke(definition);
         if (missionId.equals(id)) {
            return definition;
         }
      }
      throw new NoSuchMethodException("Missing Agriculture terminal mission definition " + missionId);
   }

   private static boolean invokeTerminalAction(ServerPlayer player, Identifier missionId, String actionId) {
      try {
         Class<?> providerClass = Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider");
         Object provider = providerClass.getField("INSTANCE").get(null);
         return (Boolean)providerClass.getMethod("handleAction", ServerPlayer.class, Identifier.class, String.class)
            .invoke(provider, player, missionId, actionId);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to invoke Agriculture terminal action.", exception);
      }
   }

   private static void ensureFaction(Identifier factionId, String displayName) {
      if (EchoCoreServices.factionDefinition(factionId).isPresent()) {
         return;
      }
      EchoCoreServices.registerFaction(new EchoFactionDefinition(
         factionId,
         displayName,
         displayName,
         "Test Route",
         "Agriculture Reclamation test faction.",
         "",
         "",
         "",
         0x66E8FF,
         false,
         List.of(),
         List.of(),
         List.of(),
         List.of(),
         null
      ));
   }
}
