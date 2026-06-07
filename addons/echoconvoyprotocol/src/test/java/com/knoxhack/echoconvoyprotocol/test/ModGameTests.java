package com.knoxhack.echoconvoyprotocol.test;

import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.Config;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyFacilityState;
import com.knoxhack.echoconvoyprotocol.content.ConvoyFieldOperationPhase;
import com.knoxhack.echoconvoyprotocol.content.ConvoyFieldOperationState;
import com.knoxhack.echoconvoyprotocol.content.ConvoyIncidentJsonReloadListener;
import com.knoxhack.echoconvoyprotocol.content.ConvoyIncidentProfile;
import com.knoxhack.echoconvoyprotocol.content.ConvoyJsonReloadListener;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyCoreIntegration;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyMissionProvider;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyRenderCoreVisuals;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyTerminalCommonIntegration;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyTerminalIds;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyStationMenu;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyUpgradeMenu;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalClientState;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalStatePacket;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.registry.ModEntities;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.knoxhack.echoconvoyprotocol.task.ConvoyMultiblockTasks;
import com.knoxhack.echoconvoyprotocol.upgrade.ConvoyUpgradeSlot;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiscoveryCategory;
import com.knoxhack.echocore.api.EchoDiscoveryEntry;
import com.knoxhack.echocore.api.EchoDiscoveryProvider;
import com.knoxhack.echocore.api.EchoDiscoveryState;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echomultiblockcore.api.AutomationIngredient;
import com.knoxhack.echomultiblockcore.api.AutomationEffectHandlers;
import com.knoxhack.echomultiblockcore.api.AutomationEffectInvocation;
import com.knoxhack.echomultiblockcore.api.AutomationEffectResult;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.WorkcellType;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import com.knoxhack.echomultiblockcore.item.ToolHeadItem;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
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
   private static final String PROGRESS_ROOT = "echoconvoyprotocol";
   private static final int TEST_PADDING = 48;

   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, EchoConvoyProtocol.MODID);

   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODULE_REGISTRATION =
      TEST_FUNCTIONS.register("module_registration", () -> ModGameTests::moduleRegistration);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_PARSER =
      TEST_FUNCTIONS.register("route_parser", () -> ModGameTests::routeParser);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_PARSER_VALIDATION =
      TEST_FUNCTIONS.register("route_parser_validation", () -> ModGameTests::routeParserValidation);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INCIDENT_PARSER_VALIDATION =
      TEST_FUNCTIONS.register("incident_parser_validation", () -> ModGameTests::incidentParserValidation);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONFIG_SURFACE =
      TEST_FUNCTIONS.register("config_surface", () -> ModGameTests::configSurface);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VEHICLE_STATE =
      TEST_FUNCTIONS.register("vehicle_state", () -> ModGameTests::vehicleState);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VEHICLE_DIMENSIONS =
      TEST_FUNCTIONS.register("vehicle_dimensions", () -> ModGameTests::vehicleDimensions);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VEHICLE_KIT_DEPLOYMENT =
      TEST_FUNCTIONS.register("vehicle_kit_deployment", () -> ModGameTests::vehicleKitDeployment);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FUEL_CONSUMPTION =
      TEST_FUNCTIONS.register("fuel_consumption", () -> ModGameTests::fuelConsumption);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATION_REPAIR =
      TEST_FUNCTIONS.register("station_repair", () -> ModGameTests::stationRepair);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_COMPLETION =
      TEST_FUNCTIONS.register("route_completion", () -> ModGameTests::routeCompletion);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_DISCOVERY =
      TEST_FUNCTIONS.register("route_discovery", () -> ModGameTests::routeDiscovery);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHECKPOINT_GATING =
      TEST_FUNCTIONS.register("checkpoint_gating", () -> ModGameTests::checkpointGating);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHECKPOINT_PROGRESS =
      TEST_FUNCTIONS.register("checkpoint_progress", () -> ModGameTests::checkpointProgress);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MARKER_MISMATCH =
      TEST_FUNCTIONS.register("marker_mismatch", () -> ModGameTests::markerMismatch);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATION_OWNERSHIP =
      TEST_FUNCTIONS.register("station_ownership", () -> ModGameTests::stationOwnership);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CARGO_ANCHOR_NO_DELETION =
      TEST_FUNCTIONS.register("cargo_anchor_no_deletion", () -> ModGameTests::cargoAnchorNoDeletion);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_WIRING =
      TEST_FUNCTIONS.register("core_wiring", () -> ModGameTests::coreWiring);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MULTIBLOCK_LOGISTICS_UPGRADE =
      TEST_FUNCTIONS.register("multiblock_logistics_upgrade", () -> ModGameTests::multiblockLogisticsUpgrade);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MULTIBLOCK_TASK_INVENTORY_CONSUMPTION =
      TEST_FUNCTIONS.register("multiblock_task_inventory_consumption", () -> ModGameTests::multiblockTaskInventoryConsumption);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ACTIONS =
      TEST_FUNCTIONS.register("terminal_actions", () -> ModGameTests::terminalActions);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SNAPSHOT =
      TEST_FUNCTIONS.register("terminal_snapshot", () -> ModGameTests::terminalSnapshot);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ROUTE_SELECTION =
      TEST_FUNCTIONS.register("terminal_route_selection", () -> ModGameTests::terminalRouteSelection);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CALLSIGN_SHIELDING_PERSISTENCE =
      TEST_FUNCTIONS.register("callsign_shielding_persistence", () -> ModGameTests::callsignShieldingPersistence);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCANNER_ROUTE_UTILITY =
      TEST_FUNCTIONS.register("scanner_route_utility", () -> ModGameTests::scannerRouteUtility);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELAY_DEPLOYMENT =
      TEST_FUNCTIONS.register("relay_deployment", () -> ModGameTests::relayDeployment);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UPGRADE_BAY_LINKING =
      TEST_FUNCTIONS.register("upgrade_bay_linking", () -> ModGameTests::upgradeBayLinking);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UPGRADE_SLOT_VALIDATION =
      TEST_FUNCTIONS.register("upgrade_slot_validation", () -> ModGameTests::upgradeSlotValidation);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UPGRADE_PERSISTENCE =
      TEST_FUNCTIONS.register("upgrade_persistence", () -> ModGameTests::upgradePersistence);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UPGRADE_EFFECTIVE_STATS =
      TEST_FUNCTIONS.register("upgrade_effective_stats", () -> ModGameTests::upgradeEffectiveStats);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UPGRADE_REMOVAL_GUARDS =
      TEST_FUNCTIONS.register("upgrade_removal_guards", () -> ModGameTests::upgradeRemovalGuards);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_ROUTE_TELEMETRY =
      TEST_FUNCTIONS.register("core_route_telemetry", () -> ModGameTests::coreRouteTelemetry);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDGEN_RESOURCE_COVERAGE =
      TEST_FUNCTIONS.register("worldgen_resource_coverage", () -> ModGameTests::worldgenResourceCoverage);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNTIME_ASSET_COVERAGE =
      TEST_FUNCTIONS.register("runtime_asset_coverage", () -> ModGameTests::runtimeAssetCoverage);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PRODUCTION_RELOAD_SOAK =
      TEST_FUNCTIONS.register("production_reload_soak", () -> ModGameTests::productionReloadSoak);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RENDERCORE_ROVER_VISUALS =
      TEST_FUNCTIONS.register("rendercore_rover_visuals", () -> ModGameTests::renderCoreRoverVisuals);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_OPS_ROUTE_PARSER =
      TEST_FUNCTIONS.register("field_ops_route_parser", () -> ModGameTests::fieldOpsRouteParser);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_OPERATION_LIFECYCLE =
      TEST_FUNCTIONS.register("field_operation_lifecycle", () -> ModGameTests::fieldOperationLifecycle);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_OPERATION_INCIDENT =
      TEST_FUNCTIONS.register("field_operation_incident", () -> ModGameTests::fieldOperationIncident);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_OPERATION_POLICY =
      TEST_FUNCTIONS.register("field_operation_policy", () -> ModGameTests::fieldOperationPolicy);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_OPERATION_PERSISTENCE =
      TEST_FUNCTIONS.register("field_operation_persistence", () -> ModGameTests::fieldOperationPersistence);

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("convoy_protocol"));
      register(event, environment, "module_registration", MODULE_REGISTRATION.getId());
      register(event, environment, "route_parser", ROUTE_PARSER.getId());
      register(event, environment, "route_parser_validation", ROUTE_PARSER_VALIDATION.getId());
      register(event, environment, "incident_parser_validation", INCIDENT_PARSER_VALIDATION.getId());
      register(event, environment, "config_surface", CONFIG_SURFACE.getId());
      register(event, environment, "vehicle_state", VEHICLE_STATE.getId());
      register(event, environment, "vehicle_dimensions", VEHICLE_DIMENSIONS.getId());
      register(event, environment, "vehicle_kit_deployment", VEHICLE_KIT_DEPLOYMENT.getId());
      register(event, environment, "fuel_consumption", FUEL_CONSUMPTION.getId());
      register(event, environment, "station_repair", STATION_REPAIR.getId());
      register(event, environment, "route_completion", ROUTE_COMPLETION.getId());
      register(event, environment, "route_discovery", ROUTE_DISCOVERY.getId());
      register(event, environment, "checkpoint_gating", CHECKPOINT_GATING.getId());
      register(event, environment, "checkpoint_progress", CHECKPOINT_PROGRESS.getId());
      register(event, environment, "marker_mismatch", MARKER_MISMATCH.getId());
      register(event, environment, "station_ownership", STATION_OWNERSHIP.getId());
      register(event, environment, "cargo_anchor_no_deletion", CARGO_ANCHOR_NO_DELETION.getId());
      register(event, environment, "core_wiring", CORE_WIRING.getId());
      register(event, environment, "multiblock_logistics_upgrade", MULTIBLOCK_LOGISTICS_UPGRADE.getId());
      register(event, environment, "multiblock_task_inventory_consumption", MULTIBLOCK_TASK_INVENTORY_CONSUMPTION.getId());
      register(event, environment, "terminal_actions", TERMINAL_ACTIONS.getId());
      register(event, environment, "terminal_snapshot", TERMINAL_SNAPSHOT.getId());
      register(event, environment, "terminal_route_selection", TERMINAL_ROUTE_SELECTION.getId());
      register(event, environment, "callsign_shielding_persistence", CALLSIGN_SHIELDING_PERSISTENCE.getId());
      register(event, environment, "scanner_route_utility", SCANNER_ROUTE_UTILITY.getId());
      register(event, environment, "relay_deployment", RELAY_DEPLOYMENT.getId());
      register(event, environment, "upgrade_bay_linking", UPGRADE_BAY_LINKING.getId());
      register(event, environment, "upgrade_slot_validation", UPGRADE_SLOT_VALIDATION.getId());
      register(event, environment, "upgrade_persistence", UPGRADE_PERSISTENCE.getId());
      register(event, environment, "upgrade_effective_stats", UPGRADE_EFFECTIVE_STATS.getId());
      register(event, environment, "upgrade_removal_guards", UPGRADE_REMOVAL_GUARDS.getId());
      register(event, environment, "core_route_telemetry", CORE_ROUTE_TELEMETRY.getId());
      register(event, environment, "worldgen_resource_coverage", WORLDGEN_RESOURCE_COVERAGE.getId());
      register(event, environment, "runtime_asset_coverage", RUNTIME_ASSET_COVERAGE.getId());
      register(event, environment, "production_reload_soak", PRODUCTION_RELOAD_SOAK.getId());
      register(event, environment, "rendercore_rover_visuals", RENDERCORE_ROVER_VISUALS.getId());
      register(event, environment, "field_ops_route_parser", FIELD_OPS_ROUTE_PARSER.getId());
      register(event, environment, "field_operation_lifecycle", FIELD_OPERATION_LIFECYCLE.getId());
      register(event, environment, "field_operation_incident", FIELD_OPERATION_INCIDENT.getId());
      register(event, environment, "field_operation_policy", FIELD_OPERATION_POLICY.getId());
      register(event, environment, "field_operation_persistence", FIELD_OPERATION_PERSISTENCE.getId());
   }

   private static void moduleRegistration(GameTestHelper helper) {
      helper.assertTrue(ModBlocks.VEHICLE_WORKBENCH.get() != Blocks.AIR, "Vehicle Workbench should be registered");
      helper.assertTrue(ModBlocks.VEHICLE_UPGRADE_BAY.get() != Blocks.AIR, "Vehicle Upgrade Bay should be registered");
      helper.assertTrue(ModItems.SCRAP_BIKE_KIT.get() != Items.AIR, "Scrap Bike Kit should be registered");
      helper.assertTrue(ModItems.ARMORED_RELAY_TRUCK_REACTIVE_ARMOR_KIT.get() != Items.AIR, "Vehicle upgrade kits should be registered");
      helper.assertTrue(ModEntities.SCRAP_BIKE.get() != null, "Scrap Bike entity should be registered");
      helper.assertTrue(ModEntities.ARMORED_RELAY_TRUCK.get() != null, "Armored Relay Truck entity should be registered");
      helper.succeed();
   }

   private static void renderCoreRoverVisuals(GameTestHelper helper) {
      helper.assertTrue("DAMAGED".equals(ConvoyRenderCoreVisuals.roverVisualStateName(true, true, true, true)),
         "Damaged rovers should use DAMAGED visuals first.");
      helper.assertTrue("OFFLINE".equals(ConvoyRenderCoreVisuals.roverVisualStateName(false, false, false, false)),
         "Unpowered rovers should use OFFLINE visuals.");
      helper.assertTrue("ACTIVE".equals(ConvoyRenderCoreVisuals.roverVisualStateName(false, true, true, true)),
         "Driven rovers should use ACTIVE visuals.");
      helper.assertTrue("ONLINE".equals(ConvoyRenderCoreVisuals.roverVisualStateName(false, true, false, false)),
         "Powered idle rovers should use ONLINE visuals.");
      helper.succeed();
   }

   private static void routeParser(GameTestHelper helper) {
      ConvoyRouteDefinition route = ConvoyJsonReloadListener.parseRouteForTests(id("parser_route"),
         JsonParser.parseString("{\"title\":\"Parser Route\",\"requiredVehicle\":\"scrap_bike\",\"minFuel\":7,\"requiredCargo\":[{\"item\":\"minecraft:apple\",\"count\":2}],\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":1}],\"threatLevel\":2,\"legs\":[{\"id\":\"checkpoint\",\"title\":\"Parser Checkpoint\",\"minDistanceFromStart\":16,\"requiresCheckpoint\":true,\"roadsideStructure\":\"echoconvoyprotocol:roadside/cargo_checkpoint\"}]}").getAsJsonObject());
      helper.assertTrue(route.minFuel() == 7, "Route parser should load minFuel");
      helper.assertTrue(route.requiredCargo().size() == 1 && route.requiredCargo().get(0).count() == 2, "Route parser should load required cargo");
      helper.assertTrue(route.rewards().size() == 1 && route.threatLevel() == 2, "Route parser should load rewards and threats");
      helper.assertTrue(route.legs().size() == 1 && route.requiredSignalMarkers() == 1, "Route parser should derive signal markers from legs");
      helper.assertTrue(route.leg(0).requiresCheckpoint() && route.leg(0).minDistanceFromStart() == 16, "Route parser should load checkpoint leg constraints");
      helper.assertTrue(route.leg(0).roadsideStructure().equals(id("roadside/cargo_checkpoint")), "Route parser should load roadside structure ids");
      helper.succeed();
   }

   private static void routeParserValidation(GameTestHelper helper) {
      assertParserRejects(helper, "missing_item",
         "{\"title\":\"Bad Route\",\"requiredVehicle\":\"scrap_bike\",\"rewards\":[{\"item\":\"missing:nope\",\"count\":1}]}");
      assertParserRejects(helper, "negative_count",
         "{\"title\":\"Bad Route\",\"requiredVehicle\":\"scrap_bike\",\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":0}]}");
      assertParserRejects(helper, "unknown_vehicle",
         "{\"title\":\"Bad Route\",\"requiredVehicle\":\"hovercraft\",\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":1}]}");
      assertParserRejects(helper, "impossible_fuel",
         "{\"title\":\"Bad Route\",\"requiredVehicle\":\"scrap_bike\",\"minFuel\":999,\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":1}]}");
      assertParserRejects(helper, "duplicate_leg",
         "{\"title\":\"Bad Route\",\"requiredVehicle\":\"scrap_bike\",\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":1}],\"legs\":[{\"id\":\"dup\"},{\"id\":\"dup\"}]}");
      helper.succeed();
   }

   private static void incidentParserValidation(GameTestHelper helper) {
      ConvoyIncidentProfile profile = ConvoyIncidentJsonReloadListener.parseProfileForTests(id("parser_incidents"),
         JsonParser.parseString("{\"incidents\":[{\"id\":\"echoconvoyprotocol:parser_incident\",\"stage\":2,\"displayText\":\"Parser incident\",\"readinessThreshold\":72,\"delayTicks\":40,\"requiredResponseTask\":\"echoconvoyprotocol:resolve_field_incident\"}]}").getAsJsonObject());
      helper.assertTrue(profile.incidents().size() == 1, "Incident parser should load authored incidents");
      helper.assertTrue(profile.firstBlockedIncident(1, 60, Set.of()).isPresent(), "Incident parser should respect one-based stage mapping");
      assertIncidentRejects(helper, "missing_incidents", "{\"title\":\"Bad Profile\"}");
      assertIncidentRejects(helper, "negative_delay", "{\"incidents\":[{\"displayText\":\"Bad\",\"delayTicks\":-1}]}");
      assertIncidentRejects(helper, "bad_threshold", "{\"incidents\":[{\"displayText\":\"Bad\",\"readinessThreshold\":101}]}");
      helper.succeed();
   }

   private static void configSurface(GameTestHelper helper) {
      helper.assertTrue(Config.vehicleJoinPolicy() == Config.VehicleJoinPolicy.HYBRID, "Convoy should default to hybrid vehicle/depot Field Ops");
      helper.assertTrue(Config.routeDifficultyMultiplier() == 1.0D, "Route difficulty default should preserve authored data");
      helper.assertTrue(Config.fieldOpsDurationScale() == 1.0D, "Field Ops duration scale default should preserve authored data");
      helper.assertTrue(Config.incidentFrequencyPercent() == 100, "Incident frequency default should preserve authored data");
      helper.assertTrue(Config.preciseHoloMapMarkers(), "Precise HoloMap markers should be enabled by default");
      helper.assertFalse(Config.debugDiagnostics(), "Debug diagnostics should be opt-in");
      helper.assertTrue(EchoConfigRegistry.snapshot(EchoConvoyProtocol.MODID, EchoConfigSide.COMMON)
         .filter(snapshot -> snapshot.categories().stream().anyMatch(category -> "field_ops".equals(category.categoryId())))
         .isPresent(), "Convoy config should publish Field Ops settings through Echo Core");
      helper.assertTrue(EchoConfigRegistry.snapshot(EchoConvoyProtocol.MODID, EchoConfigSide.COMMON)
         .filter(snapshot -> snapshot.categories().stream().anyMatch(category -> "diagnostics".equals(category.categoryId())))
         .isPresent(), "Convoy config should publish diagnostics settings through Echo Core");
      helper.succeed();
   }

   private static void vehicleState(GameTestHelper helper) {
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(1, 2, 1));
      vehicle.insertCargo(new ItemStack(Items.APPLE, 3));
      vehicle.applyVehicleDamage(12);
      helper.assertTrue(vehicle.cargoItemCount(Items.APPLE) == 3, "Cargo should be retained in vehicle inventory");
      helper.assertTrue(vehicle.damage() == 12, "Vehicle damage should increase");
      vehicle.repair(5);
      helper.assertTrue(vehicle.damage() == 7, "Vehicle repair should reduce damage");
      helper.succeed();
   }

   private static void vehicleDimensions(GameTestHelper helper) {
      assertVehicleFootprint(helper, ModEntities.SCRAP_BIKE.get(), ConvoyVehicleKind.SCRAP_BIKE, 0.95D, 1.35D, 0.95D);
      assertVehicleFootprint(helper, ModEntities.WASTELAND_ROVER.get(), ConvoyVehicleKind.WASTELAND_ROVER, 2.25D, 1.75D, 1.35D);
      assertVehicleFootprint(helper, ModEntities.CARGO_CRAWLER.get(), ConvoyVehicleKind.CARGO_CRAWLER, 3.05D, 1.85D, 1.45D);
      assertVehicleFootprint(helper, ModEntities.ARMORED_RELAY_TRUCK.get(), ConvoyVehicleKind.ARMORED_RELAY_TRUCK, 3.10D, 2.15D, 1.65D);
      helper.succeed();
   }

   private static void vehicleKitDeployment(GameTestHelper helper) {
      for (int x = -2; x <= 6; x++) {
         for (int z = 1; z <= 9; z++) {
            helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
         }
      }
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
      player.setYRot(0.0F);
      ItemStack kit = new ItemStack(ModItems.WASTELAND_ROVER_KIT.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, kit);

      InteractionResult result = ModItems.WASTELAND_ROVER_KIT.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      List<ConvoyVehicleEntity> vehicles = helper.getLevel().getEntitiesOfClass(
         ConvoyVehicleEntity.class,
         new AABB(player.position(), player.position()).inflate(8.0D)
      ).stream().filter(vehicle -> vehicle.kind() == ConvoyVehicleKind.WASTELAND_ROVER).toList();

      helper.assertTrue(result == InteractionResult.SUCCESS_SERVER, "Wasteland Rover Kit should deploy from a clear staging area");
      helper.assertTrue(kit.isEmpty(), "Successful vehicle deployment should consume the kit outside creative mode");
      helper.assertTrue(vehicles.size() == 1, "Exactly one Wasteland Rover should deploy");
      ConvoyVehicleEntity vehicle = vehicles.getFirst();
      helper.assertTrue(vehicle.getZ() > player.getZ() + 1.0D, "Vehicle should stage in front of the player");
      helper.assertFalse(vehicle.getBoundingBox().intersects(player.getBoundingBox()), "Vehicle deployment should not overlap the player");
      helper.assertTrue(helper.getLevel().noBlockCollision(vehicle, vehicle.getBoundingBox()), "Deployed vehicle should have clear block collision");
      helper.succeed();
   }

   private static void fuelConsumption(GameTestHelper helper) {
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(1, 2, 1));
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      helper.assertTrue(player.startRiding(vehicle, true, true), "Mock player should mount the vehicle");
      int fuel = vehicle.fuel();
      for (int i = 0; i < 25; i++) {
         player.zza = 1.0F;
         if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setLastClientInput(new Input(true, false, false, false, false, false, false));
         }
         vehicle.tick();
      }
      helper.assertTrue(vehicle.fuel() < fuel, "Driving should consume fuel");
      helper.succeed();
   }

   private static void stationRepair(GameTestHelper helper) {
      BlockPos stationPos = new BlockPos(1, 1, 1);
      BlockPos vehiclePos = new BlockPos(2, 2, 1);
      helper.setBlock(stationPos, (Block)ModBlocks.FIELD_REPAIR_STATION.get());
      helper.setBlock(vehiclePos.below(), Blocks.STONE);
      ConvoyStationBlockEntity station = helper.getBlockEntity(stationPos, ConvoyStationBlockEntity.class);
      station.setItem(ConvoyStationBlockEntity.INPUT_SLOT, new ItemStack(ModItems.CONVOY_REPAIR_KIT.get()));
      ConvoyVehicleEntity vehicle = spawnBike(helper, vehiclePos);
      vehicle.applyVehicleDamage(30);
      helper.succeedWhen(() -> {
         tickStation(helper, stationPos, station);
         helper.assertTrue(vehicle.damage() < 30, "Repair station should repair nearby vehicle");
      });
   }

   private static void routeCompletion(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(1, 2, 1));
      boolean activated = ConvoyRouteService.activateRoute(player, vehicle, id("northern_route"));
      boolean completed = ConvoyRouteService.advanceRouteAtSignal(player, vehicle, helper.absolutePos(new BlockPos(40, 2, 1)));
      boolean claimed = ConvoyRouteService.claimRouteRewards(player, id("northern_route"));
      boolean duplicateClaim = ConvoyRouteService.claimRouteRewards(player, id("northern_route"));
      helper.assertTrue(activated && completed && claimed, "Northern route should activate, complete, and claim rewards");
      helper.assertTrue(!duplicateClaim, "Route rewards should not duplicate");
      helper.succeed();
   }

   private static void routeDiscovery(GameTestHelper helper) {
      EchoCoreServices.clearPlatformServicesForTests();
      ConvoyCoreIntegration.registerAddonChapter();
      registerRouteDiscoveryProviderForTests();
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(1, 2, 1));
      Identifier routeId = id("northern_route");
      Identifier discoveryId = EchoCoreServices.routeDiscoveryId(ConvoyTerminalIds.id("route/" + routeId.getPath()));

      helper.assertFalse(ConvoyRouteService.activateRoute(player, null, routeId),
         "Blocked route starts should fail before discovery is recorded");
      helper.assertFalse(EchoCoreServices.hasDiscoveredFeature(player, discoveryId),
         "Blocked route starts should not persist route discovery");

      helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, routeId),
         "Valid route starts should succeed");
      helper.assertTrue(EchoCoreServices.hasDiscoveredFeature(player, discoveryId),
         "Valid route starts should persist the matching route discovery id");
      helper.assertFalse(EchoCoreServices.discoverFeature(player, discoveryId),
         "Duplicate route discovery should not fire a second time");

      helper.assertTrue(ConvoyRouteService.advanceRouteAtSignal(player, vehicle, helper.absolutePos(new BlockPos(40, 2, 1))),
         "Completing a discovered route at a distant route signal should succeed");
      helper.assertTrue(ConvoyRouteService.claimRouteRewards(player, routeId),
         "Claiming completed route rewards should succeed once");
      helper.assertFalse(ConvoyRouteService.claimRouteRewards(player, routeId),
         "Duplicate route reward claims should stay blocked");
      helper.assertFalse(EchoCoreServices.discoverFeature(player, discoveryId),
         "Completion and claim hooks should not duplicate discovery state");

      helper.getLevel().getServer().getPlayerList().remove(player);
      EchoCoreServices.clearPlatformServicesForTests();
      helper.succeed();
   }

   private static void checkpointGating(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(1, 2, 1));
      ConvoyRouteDefinition gated = new ConvoyRouteDefinition(id("gated"), "Gated", "", 0, "scrap_bike", 0, List.of(), List.of(), 0,
         Identifier.fromNamespaceAndPath("echocore", "survivors"), 50, "");
      helper.assertTrue(!ConvoyRouteService.readiness(player, vehicle, gated).ready(), "Faction checkpoint should block low reputation");
      helper.succeed();
   }

   private static void checkpointProgress(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(1, 2, 1));
      vehicle.insertCargo(new ItemStack(ModItems.ENGINE_CORE.get()));
      Identifier routeId = id("salvager_escort");
      helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, routeId, helper.absolutePos(new BlockPos(-32, 1, 1))),
         "Checkpoint route should activate");
      helper.assertTrue(ConvoyRouteService.advanceRouteAtSignal(player, vehicle, helper.absolutePos(new BlockPos(3, 2, 1))),
         "Checkpoint leg should complete");
      ConvoyProgress progress = ConvoyProgress.get(player);
      helper.assertTrue(progress.checkpointCleared(routeId), "Checkpoint clear state should persist");
      helper.assertTrue(progress.activeRouteLeg() == 1, "Checkpoint route should advance to the destination leg");
      helper.assertFalse(progress.completed(routeId), "Checkpoint leg alone should not complete the full route");
      helper.succeed();
   }

   private static void markerMismatch(GameTestHelper helper) {
      BlockPos markerPos = new BlockPos(2, 1, 2);
      helper.setBlock(markerPos, (Block)ModBlocks.ROADSIDE_SIGNAL_MARKER.get());
      ConvoyStationBlockEntity marker = helper.getBlockEntity(markerPos, ConvoyStationBlockEntity.class);
      marker.setMarkerMetadata(null, null, id("roadside/cargo_checkpoint"));

      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(1, 2, 1));
      Identifier routeId = id("northern_route");
      helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, routeId), "Northern route should activate");
      helper.assertFalse(ConvoyRouteService.advanceRouteAtSignal(player, vehicle, helper.absolutePos(new BlockPos(40, 2, 1)), marker),
         "Cargo checkpoint marker should not satisfy a signal marker leg");
      ConvoyProgress progress = ConvoyProgress.get(player);
      helper.assertFalse(progress.completed(routeId), "Mismatched marker should not complete the route");
      helper.assertTrue(progress.activeRouteLeg() == 0, "Mismatched marker should not advance the route leg");
      helper.succeed();
   }

   private static void stationOwnership(GameTestHelper helper) {
      BlockPos stationPos = new BlockPos(1, 1, 1);
      BlockPos vehiclePos = new BlockPos(2, 2, 1);
      helper.setBlock(stationPos, (Block)ModBlocks.FIELD_REPAIR_STATION.get());
      helper.setBlock(vehiclePos.below(), Blocks.STONE);
      ConvoyStationBlockEntity station = helper.getBlockEntity(stationPos, ConvoyStationBlockEntity.class);
      station.setItem(ConvoyStationBlockEntity.INPUT_SLOT, new ItemStack(ModItems.CONVOY_REPAIR_KIT.get()));

      ServerPlayer owner = helper.makeMockServerPlayerInLevel();
      ServerPlayer other = helper.makeMockServerPlayerInLevel();
      ConvoyVehicleEntity vehicle = spawnBike(helper, vehiclePos);
      owner.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      other.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      vehicle.interact(owner, InteractionHand.MAIN_HAND, vehicle.position());
      helper.assertTrue(vehicle.isOwner(owner), "Claimed vehicle should accept its owner");
      helper.assertFalse(vehicle.isOwner(other), "Claimed vehicle should reject a different operator");

      vehicle.applyVehicleDamage(30);
      station.linkOwner(other);
      helper.runAfterDelay(45L, () -> {
         helper.assertTrue(vehicle.damage() == 30, "Station linked to another operator should not repair the vehicle");
         helper.assertTrue(station.getItem(ConvoyStationBlockEntity.INPUT_SLOT).is(ModItems.CONVOY_REPAIR_KIT.get()),
            "Owner-locked station should not consume the repair kit");
         station.linkOwner(owner);
         helper.succeedWhen(() -> {
            tickStation(helper, stationPos, station);
            helper.assertTrue(vehicle.damage() < 30, "Station linked to owner should repair the vehicle");
            helper.getLevel().getServer().getPlayerList().remove(owner);
            helper.getLevel().getServer().getPlayerList().remove(other);
         });
      });
   }

   private static void cargoAnchorNoDeletion(GameTestHelper helper) {
      BlockPos anchorPos = new BlockPos(1, 1, 1);
      helper.setBlock(anchorPos, (Block)ModBlocks.CARGO_ANCHOR.get());
      ConvoyStationBlockEntity anchor = helper.getBlockEntity(anchorPos, ConvoyStationBlockEntity.class);
      for (int slot = ConvoyStationBlockEntity.STORAGE_START; slot < ConvoyStationBlockEntity.SLOT_COUNT; slot++) {
         anchor.setItem(slot, new ItemStack(Items.STONE, 64));
      }

      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(2, 2, 1));
      player.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      vehicle.interact(player, InteractionHand.MAIN_HAND, vehicle.position());
      vehicle.insertCargo(new ItemStack(Items.APPLE, 4));

      boolean unloaded = anchor.handleMenuButton(player, ConvoyStationMenu.BUTTON_UNLOAD);
      helper.assertFalse(unloaded, "Cargo Anchor unload should report failure when storage is full");
      helper.assertTrue(vehicle.cargoItemCount(Items.APPLE) == 4, "Rejected cargo should be restored to the vehicle");
      for (int slot = ConvoyStationBlockEntity.STORAGE_START; slot < ConvoyStationBlockEntity.SLOT_COUNT; slot++) {
         helper.assertTrue(anchor.getItem(slot).is(Items.STONE), "Full anchor storage should not receive rejected cargo");
      }
      helper.succeed();
   }

   private static void coreWiring(GameTestHelper helper) {
      ConvoyCoreIntegration.registerAddonChapter();
      helper.assertTrue(com.knoxhack.echocore.api.EchoAddonRegistry.isRegistered(ConvoyCoreIntegration.CHAPTER_ID),
         "Convoy should register as an ECHO addon chapter");
      helper.succeed();
   }

   private static void multiblockLogisticsUpgrade(GameTestHelper helper) {
      helper.assertTrue(ModBlocks.CONVOY_DEPOT_CONTROLLER.get() != Blocks.AIR, "Convoy Depot Controller should be registered");
      helper.assertTrue(ModBlocks.CARGO_INPUT_CRATE.get() != Blocks.AIR, "Cargo Input Crate should be registered");
      helper.assertTrue(ModItems.CONVOY_DEPOT_BLUEPRINT.get() != Items.AIR, "Convoy Depot Blueprint should be registered");
      helper.assertTrue(ModItems.CARGO_CLAMP_HEAD.get() instanceof ToolHeadItem, "Cargo Clamp Head should use MultiblockCore tool head behavior");
      helper.assertTrue(AutomationRecipeRegistry.byId(ConvoyMultiblockTasks.LOAD_FIELD_SUPPLY_CRATE).isPresent(),
         "Load Field Supply Crate automation recipe should be registered");
      helper.assertTrue(AutomationRecipeRegistry.byId(ConvoyMultiblockTasks.REFUEL_CONVOY)
            .map(recipe -> recipe.requiredWorkcell() == WorkcellType.FUEL_INJECTION)
            .orElse(false),
         "Refuel Convoy automation recipe should require the Fuel Injection workcell");

      ConvoyFacilityState state = new ConvoyFacilityState();
      state.loadCargo();
      state.refuel();
      state.refuel();
      state.prepareRoute(id("echo_7_ruined_highway").toString());
      helper.assertTrue(state.readiness().dispatchReady(), "Loaded cargo, fuel, and route data should produce dispatch readiness");
      helper.assertTrue(state.dispatch() && !state.activeRouteId().isBlank(), "Dispatch should move the prepared route into active state");
      state.completeActiveRoute();
      helper.assertTrue(state.completedMissions() == 1, "Completing an active route should increment mission history");

      ConvoyRouteDefinition route = ConvoyJsonReloadListener.parseRouteForTests(id("extended_route"),
         JsonParser.parseString("{\"title\":\"Extended Route\",\"requiredVehicle\":\"any\",\"minFuel\":4,\"missionType\":\"Recovery\",\"distance\":320,\"biomeTheme\":\"ash_wastes\",\"fuelCost\":12,\"requiredReadiness\":61,\"requiredCargo\":[{\"item\":\"echoconvoyprotocol:field_supply_crate\",\"count\":1}],\"rewards\":[{\"item\":\"echoconvoyprotocol:fuel_cell\",\"count\":1}],\"possibleHazards\":[\"ash storm\"],\"holomapMarker\":{\"icon\":\"failed_convoy_signal\",\"color\":\"ff5533\"}}").getAsJsonObject());
      helper.assertTrue("Recovery".equals(route.missionType()), "Extended route parser should load mission type");
      helper.assertTrue(route.distance() == 320 && route.fuelCost() == 12 && route.requiredReadiness() == 61,
         "Extended route parser should load distance, fuel cost, and readiness");
      helper.assertTrue(route.possibleHazards().size() == 1 && "failed_convoy_signal".equals(route.holomapIcon()),
         "Extended route parser should load hazards and HoloMap marker metadata");

      assertClasspathResource(helper, "data/echoconvoyprotocol/echo_multiblocks/convoy_depot.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/echo_multiblocks/vehicle_repair_gantry.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/echo_multiblocks/cargo_loading_bay.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/echo_multiblocks/fuel_refinery_pad.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/echo_multiblocks/route_dispatch_tower.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/convoy_routes/echo_7_ruined_highway.json");
      helper.succeed();
   }

   private static void multiblockTaskInventoryConsumption(GameTestHelper helper) {
      BlockPos controllerPos = new BlockPos(2, 1, 2);
      BlockPos cratePos = new BlockPos(3, 1, 2);
      helper.setBlock(controllerPos, (Block)ModBlocks.CONVOY_DEPOT_CONTROLLER.get());
      helper.setBlock(cratePos, (Block)ModBlocks.CARGO_INPUT_CRATE.get());

      ConvoyMultiblockControllerBlockEntity controller =
         helper.getBlockEntity(controllerPos, ConvoyMultiblockControllerBlockEntity.class);
      MultiblockCrateBlockEntity crate = helper.getBlockEntity(cratePos, MultiblockCrateBlockEntity.class);
      crate.setItem(0, new ItemStack(ModItems.CONVOY_REPAIR_KIT.get(), 2));
      controller.convoyState().applyFieldOperationEffect(0, -45, 0);

      ConvoyMultiblockTasks.register();
      AutomationEffectResult result = AutomationEffectHandlers.onComplete(new AutomationEffectInvocation(
         helper.getLevel(),
         controller,
         controller.getBlockPos(),
         null,
         ConvoyMultiblockTasks.REPAIR_CONVOY_VEHICLE,
         null,
         null,
         null,
         "complete"
      ));

      helper.assertTrue(result.allowed(), "Convoy repair automation effect should allow completion");
      helper.assertTrue(crate.getItem(0).getCount() == 1, "Repair automation should consume one nearby repair kit when present");
      helper.assertTrue(controller.convoyState().readiness().vehicleIntegrity() >= 60,
         "Repair automation should still apply the depot repair effect after consuming inventory");

      crate.setItem(0, new ItemStack(ModItems.CONVOY_REPAIR_KIT.get(), 2));
      controller.convoyState().applyFieldOperationEffect(0, -45, 0);
      MultiblockAutomationRecipe recipeChargedOnStart = new MultiblockAutomationRecipe(
         ConvoyMultiblockTasks.REPAIR_CONVOY_VEHICLE,
         "Repair Convoy Vehicle",
         id("field_operations"),
         List.of(),
         WorkcellType.REPAIR,
         List.of(),
         List.of(AutomationIngredient.item(id("convoy_repair_kit"), 1)),
         List.of(),
         true,
         20,
         0,
         "repair",
         0,
         List.of(),
         List.of(ConvoyMultiblockTasks.REPAIR_CONVOY_VEHICLE)
      );
      AutomationEffectHandlers.onComplete(new AutomationEffectInvocation(
         helper.getLevel(),
         controller,
         controller.getBlockPos(),
         null,
         ConvoyMultiblockTasks.REPAIR_CONVOY_VEHICLE,
         recipeChargedOnStart,
         null,
         null,
         "complete"
      ));
      helper.assertTrue(crate.getItem(0).getCount() == 2,
         "Completion effect should not double-consume an input already charged by the task recipe");
      helper.succeed();
   }

   private static void fieldOpsRouteParser(GameTestHelper helper) {
      ConvoyRouteDefinition route = ConvoyJsonReloadListener.parseRouteForTests(id("field_ops_parser"),
         JsonParser.parseString("{\"title\":\"Field Ops Parser\",\"requiredVehicle\":\"any\",\"minFuel\":0,\"distance\":180,\"requiredCargo\":[{\"item\":\"minecraft:apple\",\"count\":1}],\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":1}],\"fieldOps\":{\"durationTicks\":420,\"stageCount\":3,\"incidentProfile\":\"echoconvoyprotocol:test_profile\",\"vehicleJoinPolicy\":\"optional\",\"completionMode\":\"depot_return\"}}").getAsJsonObject());
      helper.assertTrue(route.fieldOps().durationTicks(route) == 420, "Route parser should load field operation duration");
      helper.assertTrue(route.fieldOps().stageCount(route) == 3, "Route parser should load field operation stages");
      helper.assertTrue(route.fieldOps().incidentProfile().equals(id("test_profile")), "Route parser should load incident profile id");
      helper.assertTrue(route.fieldOps().effectiveVehicleJoinPolicy() == Config.VehicleJoinPolicy.HYBRID,
         "Route parser should normalize optional vehicle policy to hybrid behavior");
      assertParserRejects(helper, "bad_field_ops_policy",
         "{\"title\":\"Bad Field Ops\",\"requiredVehicle\":\"any\",\"fieldOps\":{\"vehicleJoinPolicy\":\"teleport_only\"}}");
      assertParserRejects(helper, "bad_field_ops_completion",
         "{\"title\":\"Bad Field Ops\",\"requiredVehicle\":\"any\",\"fieldOps\":{\"completionMode\":\"instant_win\"}}");

      ConvoyIncidentProfile profile = ConvoyIncidentJsonReloadListener.parseProfileForTests(id("test_profile"),
         JsonParser.parseString("{\"incidents\":[{\"id\":\"echoconvoyprotocol:test_incident\",\"stageId\":\"stage_2\",\"displayText\":\"Test field blocker\",\"readinessThreshold\":75,\"fuelEffect\":-5,\"integrityEffect\":-10,\"cargoEffect\":-1,\"delayTicks\":60,\"requiredResponseTask\":\"echoconvoyprotocol:resolve_field_incident\",\"holomapMarkerHint\":\"test_warning\"}]}").getAsJsonObject());
      helper.assertTrue(profile.firstBlockedIncident(1, 60, Set.of()).isPresent(),
         "Incident profiles should choose deterministic incidents below threshold");
      helper.assertTrue(profile.firstBlockedIncident(1, 90, Set.of()).isEmpty(),
         "Incident profiles should not block high-readiness operations");
      assertClasspathResource(helper, "data/echoconvoyprotocol/convoy_incidents/standard.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/convoy_incidents/ruined_highway.json");
      helper.succeed();
   }

   private static void fieldOperationLifecycle(GameTestHelper helper) {
      ConvoyRouteDefinition route = fieldOpsRoute("field_ops_lifecycle", 220, 2, id("standard"));
      ConvoyFacilityState depot = preparedDepot(route);
      ConvoyFieldOperationState operation = new ConvoyFieldOperationState();
      helper.assertTrue(operation.stage(route, helper.getLevel().getGameTime()), "Field operation should stage");
      helper.assertTrue(operation.launch(route, depot, helper.getLevel().getGameTime()), "Prepared depot should launch field operation");
      helper.assertTrue(operation.phase() == ConvoyFieldOperationPhase.EN_ROUTE, "Launched operation should be en route");
      for (int i = 0; i < 16; i++) {
         operation.tick(helper.getLevel(), route, null, depot);
      }
      helper.assertTrue(operation.phase() == ConvoyFieldOperationPhase.COMPLETE, "Operation should complete after route duration plus return leg");
      helper.assertTrue(depot.completedMissions() == 1, "Completed field operation should increment depot mission history");
      helper.assertTrue(operation.completionRewardReady(), "Completed operation should expose salvage/reward readiness");
      helper.succeed();
   }

   private static void fieldOperationIncident(GameTestHelper helper) {
      ConvoyRouteDefinition route = fieldOpsRoute("field_ops_incident", 240, 2, id("test_incidents"));
      ConvoyIncidentProfile profile = ConvoyIncidentJsonReloadListener.parseProfileForTests(id("test_incidents"),
         JsonParser.parseString("{\"incidents\":[{\"id\":\"echoconvoyprotocol:low_readiness_roadblock\",\"stageId\":0,\"displayText\":\"Roadside bridge washout\",\"readinessThreshold\":98,\"fuelEffect\":-5,\"integrityEffect\":-5,\"delayTicks\":40,\"requiredResponseTask\":\"echoconvoyprotocol:resolve_field_incident\",\"holomapMarkerHint\":\"bridge_warning\"}]}").getAsJsonObject());
      ConvoyFacilityState depot = preparedDepot(route);
      ConvoyFieldOperationState operation = new ConvoyFieldOperationState();
      operation.stage(route, helper.getLevel().getGameTime());
      helper.assertTrue(operation.launch(route, depot, helper.getLevel().getGameTime()), "Incident test operation should launch");
      operation.tick(helper.getLevel(), route, profile, depot);
      helper.assertTrue(operation.phase() == ConvoyFieldOperationPhase.INCIDENT_BLOCKED,
         "Low operation score should deterministically trigger the configured incident");
      helper.assertTrue(operation.incidentId().contains("low_readiness_roadblock"), "Incident id should persist on the operation");
      helper.assertTrue(operation.resolveIncident(depot), "Resolve incident task should unblock the field operation");
      helper.assertTrue(operation.phase() == ConvoyFieldOperationPhase.EN_ROUTE, "Resolved incident should resume the operation");
      helper.succeed();
   }

   private static void fieldOperationPolicy(GameTestHelper helper) {
      ConvoyRouteDefinition requiredRoute = fieldOpsRoute(
         "field_ops_required_vehicle",
         240,
         2,
         id("standard"),
         "vehicle_required"
      );
      ConvoyFacilityState depot = preparedDepot(requiredRoute);
      ConvoyFieldOperationState operation = new ConvoyFieldOperationState();
      helper.assertTrue(operation.stage(requiredRoute, helper.getLevel().getGameTime()), "Vehicle-required operation should stage");
      helper.assertFalse(operation.launch(requiredRoute, depot, helper.getLevel().getGameTime()),
         "Route-level vehicle-required policy should block launch without a joined physical vehicle");
      helper.assertTrue(operation.lastDiagnostic().contains("requires a paired physical convoy vehicle"),
         "Vehicle-required blocker should explain the missing joined vehicle");
      UUID vehicleId = UUID.randomUUID();
      helper.assertTrue(operation.joinVehicle(vehicleId), "Staged operation should accept a joined physical vehicle id");
      helper.assertTrue(operation.launch(requiredRoute, depot, helper.getLevel().getGameTime()),
         "Vehicle-required operation should launch after a physical vehicle joins");
      helper.assertTrue(operation.phase() == ConvoyFieldOperationPhase.EN_ROUTE,
         "Joined vehicle policy launch should enter en-route phase");

      ConvoyRouteDefinition depotOnlyRoute = fieldOpsRoute(
         "field_ops_depot_only",
         220,
         2,
         id("standard"),
         "depot_only"
      );
      ConvoyFieldOperationState depotOnly = new ConvoyFieldOperationState();
      ConvoyFacilityState depotOnlyState = preparedDepot(depotOnlyRoute);
      depotOnly.stage(depotOnlyRoute, helper.getLevel().getGameTime());
      helper.assertTrue(depotOnly.launch(depotOnlyRoute, depotOnlyState, helper.getLevel().getGameTime()),
         "Depot-only route policy should preserve autonomous Field Ops launch");
      helper.succeed();
   }

   private static void fieldOperationPersistence(GameTestHelper helper) {
      ConvoyRouteDefinition route = fieldOpsRoute("field_ops_persist", 260, 3, id("standard"));
      ConvoyFacilityState depot = preparedDepot(route);
      ConvoyFieldOperationState operation = new ConvoyFieldOperationState();
      operation.stage(route, helper.getLevel().getGameTime());
      helper.assertTrue(operation.launch(route, depot, helper.getLevel().getGameTime()), "Persistence operation should launch");
      UUID vehicleId = UUID.randomUUID();
      helper.assertTrue(operation.joinVehicle(vehicleId), "Physical vehicle id should join field operation state");
      helper.assertTrue(operation.advanceFromSignal(route.id(), 2, helper.absolutePos(new BlockPos(6, 2, 6)), vehicleId),
         "Roadside signal should advance the operation stage");

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      operation.save(output);
      CompoundTag saved = output.buildResult();
      ConvoyFieldOperationState loaded = new ConvoyFieldOperationState();
      loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      helper.assertTrue(route.id().toString().equals(loaded.routeId()), "Saved operation should retain route id");
      helper.assertTrue(loaded.phase() == ConvoyFieldOperationPhase.EN_ROUTE, "Saved operation should retain phase");
      helper.assertTrue(loaded.currentStage() == 2, "Saved operation should retain signal-advanced stage");
      helper.assertTrue(vehicleId.toString().equals(loaded.joinedVehicleUuid()), "Saved operation should retain joined vehicle id");
      helper.succeed();
   }

   private static void terminalActions(GameTestHelper helper) {
      if (!terminalApiAvailable()) {
         helper.succeed();
         return;
      }
      TerminalActionRegistry.withClearedForTests(() -> {
         ConvoyTerminalCommonIntegration.register();
         boolean handled = TerminalActionRegistry.handle(null, ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.SCAN_ACTION, "");
         helper.assertTrue(handled, "Convoy terminal scan action should be registered");
         ServerPlayer player = helper.makeMockServerPlayerInLevel();
         ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(3, 2, 3));
         BlockPos markerPos = new BlockPos(4, 1, 3);
         helper.setBlock(markerPos, (Block)ModBlocks.ROADSIDE_SIGNAL_MARKER.get());
         player.setPos(helper.absolutePos(markerPos.above()).getCenter());
         vehicle.setPos(helper.absolutePos(markerPos.above().east()).getCenter());
         Identifier routeId = id("northern_route");
         helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, routeId, helper.absolutePos(new BlockPos(-32, 1, 3))),
            "Terminal action test route should activate");
         helper.assertTrue(TerminalActionRegistry.handle(player, ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.COMPLETE_ACTION, ""),
            "Convoy terminal signal action should be registered");
         helper.assertTrue(ConvoyProgress.get(player).completed(routeId), "Terminal signal action should complete at nearby marker");
         helper.getLevel().getServer().getPlayerList().remove(player);
      });
      TerminalMissionRegistry.withClearedForTests(() -> TerminalActionRegistry.withClearedForTests(() -> {
         ConvoyTerminalCommonIntegration.register();
         boolean terminalProvider = TerminalMissionRegistry.provider(ConvoyTerminalIds.CONVOY_TAB).isPresent();
         boolean missionCoreProvider = EchoCoreServices.missionService()
            .missionDefinition(ConvoyMissionProvider.PREP_VEHICLE)
            .isPresent();
         helper.assertTrue(terminalProvider || missionCoreProvider,
            "Convoy Field Assistant mission provider should be registered through Terminal or MissionCore");
         ServerPlayer player = helper.makeMockServerPlayerInLevel();
         var prep = ConvoyMissionProvider.INSTANCE.snapshot(player, ConvoyMissionProvider.PREP_VEHICLE);
         helper.assertTrue(prep.actionHint().contains("Vehicle Workbench"),
            "Convoy Field Assistant should explain starter vehicle acquisition");
         var startDefinition = ConvoyMissionProvider.INSTANCE.missions(player).stream()
            .filter(mission -> mission.id().equals(ConvoyMissionProvider.START_ROUTE))
            .findFirst()
            .orElseThrow();
         var startSnapshot = ConvoyMissionProvider.INSTANCE.snapshot(player, ConvoyMissionProvider.START_ROUTE);
         var startPresentation = ConvoyMissionProvider.INSTANCE.presentation(player, startDefinition, startSnapshot);
         var startVisuals = ConvoyMissionProvider.INSTANCE.visuals(player, startDefinition, startSnapshot);
         helper.assertTrue(startDefinition.icon().is(ModItems.SCRAP_BIKE_KIT.get()),
            "Vehicle-missing route blocker should use the required vehicle kit icon");
         helper.assertTrue("warning".equals(startVisuals.visualTone()),
            "Vehicle-missing route blocker should expose warning tone");
         helper.assertTrue(startPresentation.tags().contains("Vehicle Missing"),
            "Field Assistant presentation should tag the current blocker");
         helper.getLevel().getServer().getPlayerList().remove(player);
      }));
      helper.succeed();
   }

   private static void terminalSnapshot(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(2, 2, 2));
      vehicle.setCustomName(Component.literal("Route Fox"));
      vehicle.insertCargo(new ItemStack(ModItems.ENGINE_CORE.get()));
      helper.setBlock(new BlockPos(3, 1, 2), (Block)ModBlocks.ROADSIDE_SIGNAL_MARKER.get());
      player.setPos(helper.absolutePos(new BlockPos(2, 2, 3)).getCenter());
      helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, id("salvager_escort"), helper.absolutePos(new BlockPos(-32, 1, 2))),
         "Snapshot route should activate");

      ConvoyTerminalStatePacket snapshot = ConvoyTerminalStatePacket.from(player);
      ConvoyTerminalClientState.apply(snapshot);
      helper.assertTrue(ConvoyTerminalClientState.snapshot().vehicleTitle().contains("Route Fox"), "Terminal snapshot should include vehicle callsign");
      helper.assertTrue(snapshot.vehicleStatus().contains("Damage 0/80"), "Terminal snapshot should include vehicle damage");
      helper.assertTrue(snapshot.vehicleCargo().contains("Cargo 1/18"), "Terminal snapshot should include cargo slot use");
      helper.assertTrue(snapshot.cargoLines().stream().anyMatch(line -> line.contains("Engine Core")), "Terminal snapshot should include cargo item lines");
      helper.assertTrue(snapshot.activeLegStatus().contains("Salvager Checkpoint"), "Terminal snapshot should include active route leg");
      helper.assertTrue(snapshot.assistantStatus().contains("Active route"), "Terminal snapshot should include Field Assistant status");
      helper.assertTrue(snapshot.assistantLines().stream().anyMatch(line -> line.contains("SIGNAL")),
         "Field Assistant should explain route signal closeout");
      helper.assertTrue(snapshot.nearbyPoiLines().stream().anyMatch(line -> line.contains("Roadside Signal Marker")), "Terminal snapshot should include nearby POIs");
      helper.getLevel().getServer().getPlayerList().remove(player);
      helper.succeed();
   }

   private static void terminalRouteSelection(GameTestHelper helper) {
      if (!terminalApiAvailable()) {
         helper.succeed();
         return;
      }
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(2, 2, 2));
      player.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      Identifier routeId = id("salvager_escort");

      TerminalActionRegistry.withClearedForTests(() -> {
         ConvoyTerminalCommonIntegration.register();
         helper.assertTrue(TerminalActionRegistry.handle(player, ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.START_ACTION, id("missing_route").toString()),
            "Terminal should handle invalid selected-route payloads");
         helper.assertTrue(ConvoyProgress.get(player).activeRouteId().isBlank(), "Invalid route selection should not start the recommended route");
      });

      ConvoyTerminalStatePacket blockedSnapshot = ConvoyTerminalStatePacket.from(player);
      int blockedIndex = blockedSnapshot.routeBoardRouteIds().indexOf(routeId.toString());
      helper.assertTrue(blockedIndex >= 0, "Terminal snapshot should expose route ids for selectable rows");
      helper.assertTrue("blocked".equals(blockedSnapshot.routeBoardActions().get(blockedIndex)),
         "Terminal snapshot should mark missing-cargo routes as blocked");
      helper.assertTrue(blockedSnapshot.assistantLines().stream().anyMatch(line ->
            line.contains("Load") || line.contains("Bring") || line.contains("Craft") || line.contains("Fuel")),
         "Field Assistant should explain the current route blocker");

      TerminalActionRegistry.withClearedForTests(() -> {
         ConvoyTerminalCommonIntegration.register();
         helper.assertTrue(TerminalActionRegistry.handle(player, ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.START_ACTION, routeId.toString()),
            "Terminal should handle selected blocked route payloads");
         helper.assertTrue(ConvoyProgress.get(player).activeRouteId().isBlank(), "Blocked selected routes should not activate");
      });

      vehicle.insertCargo(new ItemStack(ModItems.ENGINE_CORE.get()));
      ConvoyTerminalStatePacket readySnapshot = ConvoyTerminalStatePacket.from(player);
      int readyIndex = readySnapshot.routeBoardRouteIds().indexOf(routeId.toString());
      helper.assertTrue(readyIndex >= 0 && "start".equals(readySnapshot.routeBoardActions().get(readyIndex)),
         "Terminal snapshot should mark prepared selected routes as startable");
      helper.assertTrue(routeId.toString().equals(readySnapshot.recommendedStartRouteId()),
         "Prepared selected route should become the terminal start payload when it is first startable");
      helper.assertTrue(readySnapshot.assistantStatus().contains("Ready"),
         "Field Assistant should surface start-ready route state");

      TerminalActionRegistry.withClearedForTests(() -> {
         ConvoyTerminalCommonIntegration.register();
         helper.assertTrue(TerminalActionRegistry.handle(player, ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.START_ACTION, routeId.toString()),
            "Terminal should start the selected prepared route");
         helper.assertTrue(routeId.toString().equals(ConvoyProgress.get(player).activeRouteId()),
            "Terminal route selection should activate the selected route, not the default route");
      });
      helper.getLevel().getServer().getPlayerList().remove(player);
      helper.succeed();
   }

   private static void callsignShieldingPersistence(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(1, 2, 1));
      player.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());

      ItemStack nameTag = new ItemStack(Items.NAME_TAG);
      nameTag.set(DataComponents.CUSTOM_NAME, Component.literal("Dustline"));
      player.setItemInHand(InteractionHand.MAIN_HAND, nameTag);
      vehicle.interact(player, InteractionHand.MAIN_HAND, vehicle.position());
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIATION_SHIELDING_PLATE.get()));
      vehicle.interact(player, InteractionHand.MAIN_HAND, vehicle.position());
      vehicle.insertCargo(new ItemStack(Items.APPLE, 4));

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      vehicle.saveWithoutId(output);
      CompoundTag saved = output.buildResult();
      ConvoyVehicleEntity loaded = ModEntities.WASTELAND_ROVER.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(loaded != null, "Reloaded vehicle should instantiate");
      loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      helper.assertTrue("Dustline".equals(loaded.callsign()), "Vehicle callsign should survive save/load");
      helper.assertTrue(loaded.shieldingPlates() == 1, "Vehicle shielding should survive save/load");
      helper.assertTrue(loaded.cargoItemCount(Items.APPLE) == 4, "Vehicle cargo should survive save/load");
      helper.succeed();
   }

   private static void scannerRouteUtility(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(1, 2, 1));
      player.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      ItemStack beacon = new ItemStack(ModItems.ROUTE_BEACON.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, beacon);
      vehicle.interact(player, InteractionHand.MAIN_HAND, vehicle.position());
      helper.assertTrue(beacon.getCount() == 1, "Route Beacon scanner sweep should not consume the beacon without an active route");
      helper.assertTrue(vehicle.activeRouteId().isBlank(), "Scanner sweep without active route should not pair a route");

      vehicle.insertCargo(new ItemStack(ModItems.ENGINE_CORE.get()));
      helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, id("salvager_escort"), helper.absolutePos(new BlockPos(-64, 1, 1))),
         "Scanner route should activate");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ROUTE_BEACON.get()));
      vehicle.interact(player, InteractionHand.MAIN_HAND, vehicle.position());
      ConvoyProgress progress = ConvoyProgress.get(player);
      helper.assertTrue(progress.activeRouteVehicle().isPresent() && progress.activeRouteVehicle().get().equals(vehicle.getUUID()),
         "Route Beacon should pair the active route vehicle");
      helper.assertTrue(ConvoyTerminalStatePacket.from(player).activeLegStatus().contains("within scanner range"),
         "Terminal snapshot should expose scanner range status");
      helper.getLevel().getServer().getPlayerList().remove(player);
      helper.succeed();
   }

   private static void relayDeployment(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPos relayPos = new BlockPos(2, 2, 2);
      ConvoyVehicleEntity relay = spawnVehicle(helper, ModEntities.ARMORED_RELAY_TRUCK.get(), relayPos);
      player.setPos(relay.getX(), relay.getY(), relay.getZ());
      for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
         helper.setBlock(relayPos.relative(direction).below(), Blocks.STONE);
      }
      int battery = relay.battery();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.FIELD_REPAIR_STATION.get().asItem()));
      relay.interact(player, InteractionHand.MAIN_HAND, relay.position());
      helper.assertTrue(hasAdjacentFieldRepairStation(helper, relayPos),
         "Armored Relay Truck should deploy a real adjacent Field Repair Station");
      helper.assertTrue(relay.battery() == battery - 20, "Field station deployment should consume relay battery");
      helper.succeed();
   }

   private static void upgradeBayLinking(GameTestHelper helper) {
      BlockPos bayPos = new BlockPos(1, 1, 1);
      helper.setBlock(bayPos, (Block)ModBlocks.VEHICLE_UPGRADE_BAY.get());
      ConvoyStationBlockEntity bay = helper.getBlockEntity(bayPos, ConvoyStationBlockEntity.class);
      ServerPlayer owner = helper.makeMockServerPlayerInLevel();
      ServerPlayer other = helper.makeMockServerPlayerInLevel();
      owner.setPos(helper.absolutePos(bayPos.above()).getCenter());
      other.setPos(helper.absolutePos(bayPos.above()).getCenter());

      ConvoyUpgradeMenu emptyMenu = new ConvoyUpgradeMenu(1, owner.getInventory(), bay);
      helper.assertFalse(emptyMenu.hasVehicle(), "Upgrade Bay should open disabled with no owned vehicle nearby");

      ConvoyVehicleEntity vehicle = spawnBike(helper, new BlockPos(2, 2, 1));
      owner.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      other.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
      vehicle.interact(owner, InteractionHand.MAIN_HAND, vehicle.position());
      bay.linkOwner(owner);

      helper.assertTrue(bay.nearestUpgradeVehicle(owner) == vehicle, "Upgrade Bay should link the nearest owned vehicle within five blocks");
      helper.assertTrue(bay.nearestUpgradeVehicle(other) == null, "Upgrade Bay should not expose another operator's claimed vehicle");
      helper.assertTrue(new ConvoyUpgradeMenu(2, owner.getInventory(), bay).hasVehicle(), "Upgrade Bay menu should report linked vehicle state");
      helper.getLevel().getServer().getPlayerList().remove(owner);
      helper.getLevel().getServer().getPlayerList().remove(other);
      helper.succeed();
   }

   private static void upgradeSlotValidation(GameTestHelper helper) {
      ConvoyVehicleEntity bike = spawnBike(helper, new BlockPos(1, 2, 1));
      ItemStack tunedChain = new ItemStack(ModItems.SCRAP_BIKE_TUNED_CHAIN_KIT.get());
      ItemStack crashCage = new ItemStack(ModItems.SCRAP_BIKE_CRASH_CAGE_KIT.get());
      ItemStack roverSuspension = new ItemStack(ModItems.WASTELAND_ROVER_SUSPENSION_KIT.get());

      helper.assertTrue(bike.canInstallUpgrade(ConvoyUpgradeSlot.MOBILITY, tunedChain),
         "Matching vehicle-kind Mobility kit should be accepted");
      helper.assertFalse(bike.canInstallUpgrade(ConvoyUpgradeSlot.DEFENSE, tunedChain),
         "Matching vehicle-kind kit should be rejected from the wrong slot");
      helper.assertFalse(bike.canInstallUpgrade(ConvoyUpgradeSlot.MOBILITY, crashCage),
         "Defense kit should be rejected from Mobility slot");
      helper.assertFalse(bike.canInstallUpgrade(ConvoyUpgradeSlot.MOBILITY, roverSuspension),
         "Wrong vehicle-kind upgrade should be rejected");
      helper.assertFalse(bike.canInstallUpgrade(ConvoyUpgradeSlot.MOBILITY, new ItemStack(ModItems.SCRAP_BIKE_TUNED_CHAIN_KIT.get(), 2)),
         "Stacked upgrade kits should be rejected");
      helper.assertTrue(bike.setUpgrade(ConvoyUpgradeSlot.MOBILITY, tunedChain),
         "Valid upgrade should install into an empty matching slot");
      helper.assertFalse(bike.canInstallUpgrade(ConvoyUpgradeSlot.MOBILITY, new ItemStack(ModItems.SCRAP_BIKE_TUNED_CHAIN_KIT.get())),
         "Occupied upgrade slots should reject duplicates");
      helper.succeed();
   }

   private static void upgradePersistence(GameTestHelper helper) {
      ConvoyVehicleEntity rover = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(1, 2, 1));
      helper.assertTrue(rover.setUpgrade(ConvoyUpgradeSlot.UTILITY, new ItemStack(ModItems.WASTELAND_ROVER_SCANNER_ARRAY_KIT.get())),
         "Rover scanner upgrade should install");
      helper.assertTrue(rover.setUpgrade(ConvoyUpgradeSlot.DEFENSE, new ItemStack(ModItems.WASTELAND_ROVER_REINFORCED_PANEL_KIT.get())),
         "Rover panel upgrade should install");

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      rover.saveWithoutId(output);
      CompoundTag saved = output.buildResult();
      ConvoyVehicleEntity loaded = ModEntities.WASTELAND_ROVER.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(loaded != null, "Reloaded upgraded rover should instantiate");
      loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      helper.assertTrue(loaded.upgradeStack(ConvoyUpgradeSlot.UTILITY).is(ModItems.WASTELAND_ROVER_SCANNER_ARRAY_KIT.get()),
         "Utility upgrade stack should survive save/load");
      helper.assertTrue(loaded.upgradeStack(ConvoyUpgradeSlot.DEFENSE).is(ModItems.WASTELAND_ROVER_REINFORCED_PANEL_KIT.get()),
         "Defense upgrade stack should survive save/load");
      helper.assertTrue(loaded.maxBattery() == ConvoyVehicleKind.WASTELAND_ROVER.maxBattery() + 40,
         "Loaded scanner array should increase battery capacity");
      helper.assertTrue(loaded.scannerRange() == ConvoyVehicleKind.WASTELAND_ROVER.scannerRange() + 48,
         "Loaded scanner array should increase scanner range");
      helper.assertTrue(loaded.maxDamage() == ConvoyVehicleKind.WASTELAND_ROVER.maxDamage() + 25,
         "Loaded reinforced panels should increase max damage");
      helper.succeed();
   }

   private static void upgradeEffectiveStats(GameTestHelper helper) {
      ConvoyVehicleEntity bike = spawnBike(helper, new BlockPos(1, 2, 1));
      bike.setUpgrade(ConvoyUpgradeSlot.MOBILITY, new ItemStack(ModItems.SCRAP_BIKE_TUNED_CHAIN_KIT.get()));
      bike.setUpgrade(ConvoyUpgradeSlot.UTILITY, new ItemStack(ModItems.SCRAP_BIKE_SADDLEBAG_FRAME_KIT.get()));
      bike.setUpgrade(ConvoyUpgradeSlot.DEFENSE, new ItemStack(ModItems.SCRAP_BIKE_CRASH_CAGE_KIT.get()));
      helper.assertTrue(bike.speed() > ConvoyVehicleKind.SCRAP_BIKE.speed(), "Bike Mobility upgrade should increase speed");
      helper.assertTrue(bike.turnRate() > ConvoyVehicleKind.SCRAP_BIKE.turnRate(), "Bike Mobility upgrade should increase turn rate");
      helper.assertTrue(bike.cargoSlots() == ConvoyVehicleKind.SCRAP_BIKE.cargoSlots() + 4, "Bike Utility upgrade should add cargo slots");
      helper.assertTrue(bike.maxDamage() == ConvoyVehicleKind.SCRAP_BIKE.maxDamage() + 15, "Bike Defense upgrade should add max damage");
      helper.assertTrue(bike.hazardDamageReduction() > ConvoyVehicleKind.SCRAP_BIKE.armor(), "Bike Defense upgrade should add armor");

      ConvoyVehicleEntity rover = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(2, 2, 1));
      rover.setUpgrade(ConvoyUpgradeSlot.UTILITY, new ItemStack(ModItems.WASTELAND_ROVER_SCANNER_ARRAY_KIT.get()));
      helper.assertTrue(rover.maxBattery() == ConvoyVehicleKind.WASTELAND_ROVER.maxBattery() + 40, "Rover Utility upgrade should add battery capacity");
      helper.assertTrue(rover.scannerRange() == ConvoyVehicleKind.WASTELAND_ROVER.scannerRange() + 48, "Rover Utility upgrade should add scanner range");

      ConvoyVehicleEntity crawler = spawnVehicle(helper, ModEntities.CARGO_CRAWLER.get(), new BlockPos(3, 2, 1));
      crawler.setUpgrade(ConvoyUpgradeSlot.UTILITY, new ItemStack(ModItems.CARGO_CRAWLER_EXPANDED_BAY_KIT.get()));
      crawler.setUpgrade(ConvoyUpgradeSlot.MOBILITY, new ItemStack(ModItems.CARGO_CRAWLER_LOW_GEAR_DRIVE_KIT.get()));
      helper.assertTrue(crawler.cargoSlots() == ConvoyVehicleKind.CARGO_CRAWLER.cargoSlots() + 12, "Crawler Utility upgrade should add cargo slots");
      helper.assertTrue(crawler.cargoWeightPenalty() < ConvoyVehicleKind.CARGO_CRAWLER.cargoWeightPenalty(), "Crawler Mobility upgrade should reduce cargo penalty");

      ConvoyVehicleEntity relay = spawnVehicle(helper, ModEntities.ARMORED_RELAY_TRUCK.get(), new BlockPos(4, 2, 1));
      relay.setUpgrade(ConvoyUpgradeSlot.UTILITY, new ItemStack(ModItems.ARMORED_RELAY_TRUCK_RELAY_ARRAY_KIT.get()));
      relay.setUpgrade(ConvoyUpgradeSlot.DEFENSE, new ItemStack(ModItems.ARMORED_RELAY_TRUCK_REACTIVE_ARMOR_KIT.get()));
      helper.assertTrue(relay.maxBattery() == ConvoyVehicleKind.ARMORED_RELAY_TRUCK.maxBattery() + 60, "Relay Utility upgrade should add battery capacity");
      helper.assertTrue(relay.scannerRange() == ConvoyVehicleKind.ARMORED_RELAY_TRUCK.scannerRange() + 64, "Relay Utility upgrade should add scanner range");
      helper.assertTrue(relay.maxDamage() == ConvoyVehicleKind.ARMORED_RELAY_TRUCK.maxDamage() + 40, "Relay Defense upgrade should add max damage");
      helper.succeed();
   }

   private static void upgradeRemovalGuards(GameTestHelper helper) {
      ConvoyVehicleEntity bike = spawnBike(helper, new BlockPos(1, 2, 1));
      helper.assertTrue(bike.setUpgrade(ConvoyUpgradeSlot.UTILITY, new ItemStack(ModItems.SCRAP_BIKE_SADDLEBAG_FRAME_KIT.get())),
         "Bike saddlebag upgrade should install");
      for (Item item : List.of(Items.APPLE, Items.BREAD, Items.CARROT, Items.POTATO, Items.STICK, Items.DIRT, Items.STONE)) {
         bike.insertCargo(new ItemStack(item));
      }
      helper.assertFalse(bike.canRemoveUpgrade(ConvoyUpgradeSlot.UTILITY),
         "Cargo capacity upgrade removal should be blocked when cargo would be orphaned");
      bike.clearCargo();
      helper.assertTrue(bike.setUpgrade(ConvoyUpgradeSlot.UTILITY, ItemStack.EMPTY),
         "Cargo capacity upgrade should remove after overflow cargo is cleared");

      helper.assertTrue(bike.setUpgrade(ConvoyUpgradeSlot.DEFENSE, new ItemStack(ModItems.SCRAP_BIKE_CRASH_CAGE_KIT.get())),
         "Bike crash cage should install");
      bike.applyVehicleDamage(ConvoyVehicleKind.SCRAP_BIKE.maxDamage() + 5);
      helper.assertFalse(bike.canRemoveUpgrade(ConvoyUpgradeSlot.DEFENSE),
         "Damage cap upgrade removal should be blocked while current damage exceeds the post-removal cap");
      bike.repair(20);
      helper.assertTrue(bike.setUpgrade(ConvoyUpgradeSlot.DEFENSE, ItemStack.EMPTY),
         "Damage cap upgrade should remove after repairs bring damage under the base cap");
      helper.succeed();
   }

   private static void coreRouteTelemetry(GameTestHelper helper) {
      EchoCoreServices.clearPlatformServicesForTests();
      ConvoyCoreIntegration.registerAddonChapter();
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(1, 2, 1));
      vehicle.setCustomName(Component.literal("Core Rover"));
      vehicle.insertCargo(new ItemStack(ModItems.ENGINE_CORE.get()));
      helper.assertTrue(ConvoyRouteService.activateRoute(player, vehicle, id("salvager_escort"), helper.absolutePos(new BlockPos(-32, 1, 1))),
         "Core telemetry route should activate");

      EchoRouteRecord record = EchoCoreServices.routeRecords(player).stream()
         .filter(route -> route.id().equals(ConvoyTerminalIds.id("route/salvager_escort")))
         .findFirst()
         .orElseThrow();
      helper.assertTrue(record.status().contains("ACTIVE LEG"), "Core route record should expose active leg status");
      helper.assertTrue(record.summary().contains("Core Rover"), "Core route record should expose vehicle callsign");
      helper.assertTrue(record.summary().contains("Next leg"), "Core route record should expose next leg telemetry");
      helper.assertTrue(EchoCoreServices.diagnostics(player).stream().anyMatch(diagnostic -> diagnostic.id().equals(ConvoyTerminalIds.id("diagnostic/active_route"))),
         "Core diagnostics should expose active convoy route");
      EchoCoreServices.clearPlatformServicesForTests();
      helper.succeed();
   }

   private static void worldgenResourceCoverage(GameTestHelper helper) {
      assertResource(helper, "worldgen/structure/roadside_corridors.json");
      assertResource(helper, "worldgen/template_pool/roadside_corridors.json");
      assertResource(helper, "worldgen/structure_set/roadside_corridors.json");
      for (String route : List.of("northern_route", "salvager_escort", "northern_freight")) {
         assertResource(helper, "worldgen/structure/route/" + route + "_corridor.json");
         assertResource(helper, "worldgen/template_pool/route/" + route + "_corridor.json");
         assertResource(helper, "worldgen/structure_set/route/" + route + "_corridor.json");
      }
      for (String structure : List.of(
         "roadside/signal_marker.nbt",
         "roadside/cargo_checkpoint.nbt",
         "roadside/fuel_cache.nbt",
         "roadside/wreck_ambush.nbt",
         "roadside/repair_pullout.nbt",
         "roadside/routes/northern_route_destination.nbt",
         "roadside/routes/salvager_escort_checkpoint.nbt",
         "roadside/routes/salvager_escort_destination.nbt",
         "roadside/routes/northern_freight_checkpoint.nbt",
         "roadside/routes/northern_freight_destination.nbt"
      )) {
         assertResource(helper, "structures/" + structure);
      }
      helper.succeed();
   }

   private static void runtimeAssetCoverage(GameTestHelper helper) {
      for (String block : List.of(
         "vehicle_workbench",
         "fuel_still",
         "battery_charging_pad",
         "vehicle_dock",
         "vehicle_upgrade_bay",
         "convoy_beacon",
         "roadside_signal_marker",
         "cargo_anchor",
         "field_repair_station"
      )) {
         assertClasspathResource(helper, "assets/echoconvoyprotocol/blockstates/" + block + ".json");
         assertClasspathResource(helper, "assets/echoconvoyprotocol/items/" + block + ".json");
         assertClasspathResource(helper, "assets/echoconvoyprotocol/models/block/" + block + ".json");
         assertClasspathResource(helper, "assets/echoconvoyprotocol/models/item/" + block + ".json");
         assertClasspathResource(helper, "assets/echoconvoyprotocol/textures/block/" + block + ".png");
      }
      for (String item : List.of(
         "scrap_tire",
         "armored_tire",
         "vehicle_frame",
         "fuel_canister",
         "battery_cell",
         "engine_core",
         "radiation_shielding_plate",
         "convoy_repair_kit",
         "cargo_net",
         "route_beacon",
         "scrap_bike_kit",
         "wasteland_rover_kit",
         "cargo_crawler_kit",
         "armored_relay_truck_kit",
         "scrap_bike_tuned_chain_kit",
         "scrap_bike_saddlebag_frame_kit",
         "scrap_bike_crash_cage_kit",
         "wasteland_rover_suspension_kit",
         "wasteland_rover_scanner_array_kit",
         "wasteland_rover_reinforced_panel_kit",
         "cargo_crawler_low_gear_drive_kit",
         "cargo_crawler_expanded_bay_kit",
         "cargo_crawler_track_skirt_kit",
         "armored_relay_truck_torque_axle_kit",
         "armored_relay_truck_relay_array_kit",
         "armored_relay_truck_reactive_armor_kit"
      )) {
         assertClasspathResource(helper, "assets/echoconvoyprotocol/items/" + item + ".json");
         assertClasspathResource(helper, "assets/echoconvoyprotocol/models/item/" + item + ".json");
         assertClasspathResource(helper, "assets/echoconvoyprotocol/textures/item/" + item + ".png");
      }
      for (String kit : List.of("scrap_bike_kit", "wasteland_rover_kit", "cargo_crawler_kit", "armored_relay_truck_kit")) {
         assertPngDimensions(helper, "assets/echoconvoyprotocol/textures/item/" + kit + ".png", 16, 16);
      }
      for (String upgrade : List.of(
         "scrap_bike_tuned_chain_kit",
         "scrap_bike_saddlebag_frame_kit",
         "scrap_bike_crash_cage_kit",
         "wasteland_rover_suspension_kit",
         "wasteland_rover_scanner_array_kit",
         "wasteland_rover_reinforced_panel_kit",
         "cargo_crawler_low_gear_drive_kit",
         "cargo_crawler_expanded_bay_kit",
         "cargo_crawler_track_skirt_kit",
         "armored_relay_truck_torque_axle_kit",
         "armored_relay_truck_relay_array_kit",
         "armored_relay_truck_reactive_armor_kit"
      )) {
         assertPngDimensions(helper, "assets/echoconvoyprotocol/textures/item/" + upgrade + ".png", 16, 16);
         assertClasspathResource(helper, "data/echoconvoyprotocol/recipe/station_" + upgrade + ".json");
      }
      assertClasspathResource(helper, "data/echoconvoyprotocol/recipe/vehicle_upgrade_bay.json");
      assertClasspathResource(helper, "data/echoconvoyprotocol/loot_table/blocks/vehicle_upgrade_bay.json");
      assertClasspathResource(helper, "assets/echoconvoyprotocol/lang/en_us.json");
      for (String entity : List.of("scrap_bike", "wasteland_rover", "cargo_crawler", "armored_relay_truck")) {
         assertClasspathResource(helper, "assets/echoconvoyprotocol/textures/entity/" + entity + ".png");
         assertPngDimensions(helper, "assets/echoconvoyprotocol/textures/entity/" + entity + ".png", 256, 256);
         assertClasspathResource(helper, "assets/echoconvoyprotocol/textures/entity/rendercore_echo_mobs/" + entity + ".png");
         assertPngDimensions(helper, "assets/echoconvoyprotocol/textures/entity/rendercore_echo_mobs/" + entity + ".png", 256, 256);
      }
      for (String overlay : List.of("glow", "active_overlay", "damage_overlay", "corrupted_overlay")) {
         for (String entity : List.of("scrap_bike", "wasteland_rover", "cargo_crawler", "armored_relay_truck")) {
            assertClasspathResource(helper, "assets/echoconvoyprotocol/textures/entity/rendercore_echo_mobs/" + entity + "_" + overlay + ".png");
            assertPngDimensions(helper, "assets/echoconvoyprotocol/textures/entity/rendercore_echo_mobs/" + entity + "_" + overlay + ".png", 256, 256);
         }
      }
      helper.succeed();
   }

   private static void productionReloadSoak(GameTestHelper helper) {
      Identifier completedRoute = id("northern_route");
      Identifier activeRoute = id("salvager_escort");
      ServerPlayer player = helper.makeMockServerPlayerInLevel();

      ConvoyVehicleEntity bike = spawnBike(helper, new BlockPos(1, 2, 1));
      helper.assertTrue(ConvoyRouteService.activateRoute(player, bike, completedRoute, helper.absolutePos(new BlockPos(-40, 1, 1))),
         "Soak setup should activate a completion route");
      helper.assertTrue(ConvoyRouteService.advanceRouteAtSignal(player, bike, helper.absolutePos(new BlockPos(1, 2, 1))),
         "Soak setup should complete the first route");
      helper.assertTrue(ConvoyRouteService.claimRouteRewards(player, completedRoute),
         "Initial reward claim should succeed before reload");
      helper.assertFalse(ConvoyRouteService.claimRouteRewards(player, completedRoute),
         "Duplicate reward claim should be rejected before reload");

      ConvoyVehicleEntity rover = spawnVehicle(helper, ModEntities.WASTELAND_ROVER.get(), new BlockPos(2, 2, 2));
      ItemStack nameTag = new ItemStack(Items.NAME_TAG);
      nameTag.set(DataComponents.CUSTOM_NAME, Component.literal("Soak Rover"));
      player.setItemInHand(InteractionHand.MAIN_HAND, nameTag);
      rover.interact(player, InteractionHand.MAIN_HAND, rover.position());
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIATION_SHIELDING_PLATE.get()));
      rover.interact(player, InteractionHand.MAIN_HAND, rover.position());
      rover.insertCargo(new ItemStack(ModItems.ENGINE_CORE.get()));
      rover.insertCargo(new ItemStack(Items.APPLE, 3));
      rover.applyVehicleDamage(19);

      helper.assertTrue(ConvoyRouteService.activateRoute(player, rover, activeRoute, helper.absolutePos(new BlockPos(-40, 1, 2))),
         "Soak setup should activate an in-progress route");
      helper.assertTrue(ConvoyRouteService.advanceRouteAtSignal(player, rover, helper.absolutePos(new BlockPos(2, 2, 2))),
         "First salvager route leg should advance before reload");

      TagValueOutput vehicleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      rover.saveWithoutId(vehicleOutput);
      CompoundTag savedVehicle = vehicleOutput.buildResult();
      CompoundTag savedPlayerData = player.getPersistentData().copy();

      bike.discard();
      rover.discard();
      player.getPersistentData().put(PROGRESS_ROOT, new CompoundTag());
      player.getPersistentData().put(PROGRESS_ROOT, savedPlayerData.getCompoundOrEmpty(PROGRESS_ROOT).copy());

      ConvoyVehicleEntity loadedRover = ModEntities.WASTELAND_ROVER.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(loadedRover != null, "Reloaded rover should instantiate");
      loadedRover.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), savedVehicle));
      loadedRover.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
      helper.getLevel().addFreshEntity(loadedRover);
      player.setPos(loadedRover.getX(), loadedRover.getY(), loadedRover.getZ());

      ConvoyProgress reloadedProgress = ConvoyProgress.get(player);
      helper.assertTrue(loadedRover.isOwner(player), "Vehicle owner UUID should survive reload");
      helper.assertTrue("Soak Rover".equals(loadedRover.callsign()), "Vehicle callsign should survive reload");
      helper.assertTrue(loadedRover.shieldingPlates() == 1, "Shielding plates should survive reload");
      helper.assertTrue(loadedRover.damage() == 19, "Vehicle damage should survive reload");
      helper.assertTrue(loadedRover.cargoItemCount(ModItems.ENGINE_CORE.get()) == 1, "Route cargo should survive reload");
      helper.assertTrue(loadedRover.cargoItemCount(Items.APPLE) == 3, "General cargo should survive reload");
      helper.assertTrue(activeRoute.toString().equals(loadedRover.activeRouteId()), "Vehicle active route should survive reload");
      helper.assertTrue(activeRoute.toString().equals(reloadedProgress.activeRouteId()),
         "Player active route should recover after reload");
      helper.assertTrue(reloadedProgress.activeRouteLeg() == 1, "Active route leg should recover after reload");
      helper.assertTrue(reloadedProgress.activeRouteVehicle().filter(loadedRover.getUUID()::equals).isPresent(),
         "Active route vehicle binding should recover after reload");
      helper.assertTrue(reloadedProgress.completed(completedRoute), "Completed route should survive reload");
      helper.assertTrue(reloadedProgress.claimed(completedRoute), "Claimed reward state should survive reload");
      helper.assertFalse(ConvoyRouteService.claimRouteRewards(player, completedRoute),
         "Duplicate reward claim should remain rejected after reload");

      ConvoyTerminalStatePacket snapshot = ConvoyTerminalStatePacket.from(player);
      ConvoyTerminalClientState.apply(snapshot);
      helper.assertTrue(snapshot.vehicleTitle().contains("Soak Rover"), "Terminal snapshot should recover loaded vehicle callsign");
      helper.assertTrue(snapshot.activeRouteTitle().contains("Escort"), "Terminal snapshot should recover active route title");
      helper.assertTrue(snapshot.activeRouteStatus().contains("Leg 2/2"), "Terminal snapshot should recover active leg progress");
      int completedIndex = snapshot.routeBoardRouteIds().indexOf(completedRoute.toString());
      int activeIndex = snapshot.routeBoardRouteIds().indexOf(activeRoute.toString());
      helper.assertTrue(completedIndex >= 0 && "claimed".equals(snapshot.routeBoardActions().get(completedIndex)),
         "Terminal route board should recover claimed route state");
      helper.assertTrue(activeIndex >= 0 && "active".equals(snapshot.routeBoardActions().get(activeIndex)),
         "Terminal route board should recover active route state");

      loadedRover.discard();
      helper.getLevel().getServer().getPlayerList().remove(player);
      helper.succeed();
   }

   private static boolean terminalApiAvailable() {
      try {
         ClassLoader loader = ModGameTests.class.getClassLoader();
         Class.forName("com.knoxhack.echoterminal.api.TerminalActionRegistry", false, loader);
         Class.forName("com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry", false, loader);
         return true;
      } catch (ClassNotFoundException | LinkageError exception) {
         return false;
      }
   }

   private static void registerRouteDiscoveryProviderForTests() {
      EchoCoreServices.registerDiscoveryProvider(new EchoDiscoveryProvider() {
         @Override
         public List<EchoDiscoveryEntry> entries(Player player) {
            return EchoCoreServices.routeRecords(player).stream()
               .map(record -> new EchoDiscoveryEntry(
                  EchoCoreServices.routeDiscoveryId(record.id()),
                  id("convoy_protocol"),
                  EchoDiscoveryCategory.STRUCTURE,
                  record.title(),
                  "Unmapped Convoy Signal",
                  "Find and activate this convoy route to reveal its record.",
                  record.summary(),
                  null,
                  null,
                  0xFF66E8FF,
                  record.id(),
                  10))
               .toList();
         }

         @Override
         public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
            return EchoDiscoveryState.LOCKED;
         }
      });
   }

   private static void assertParserRejects(GameTestHelper helper, String path, String json) {
      try {
         ConvoyJsonReloadListener.parseRouteForTests(id(path), JsonParser.parseString(json).getAsJsonObject());
         helper.fail("Route parser should reject invalid convoy route " + path);
      } catch (JsonParseException expected) {
         helper.assertTrue(expected.getMessage() != null && expected.getMessage().contains(path),
         "Rejected route should include the route id in its error message");
      }
   }

   private static void assertIncidentRejects(GameTestHelper helper, String path, String json) {
      try {
         ConvoyIncidentJsonReloadListener.parseProfileForTests(id(path), JsonParser.parseString(json).getAsJsonObject());
         helper.fail("Incident parser should reject invalid convoy incident profile " + path);
      } catch (JsonParseException expected) {
         helper.assertTrue(expected.getMessage() != null && expected.getMessage().contains(path),
         "Rejected incident profile should include the profile id in its error message");
      }
   }

   private static ConvoyRouteDefinition fieldOpsRoute(String path, int durationTicks, int stageCount, Identifier incidentProfile) {
      return fieldOpsRoute(path, durationTicks, stageCount, incidentProfile, "optional");
   }

   private static ConvoyRouteDefinition fieldOpsRoute(String path, int durationTicks, int stageCount, Identifier incidentProfile, String vehicleJoinPolicy) {
      return ConvoyJsonReloadListener.parseRouteForTests(id(path), JsonParser.parseString(
         "{\"title\":\"" + path + "\",\"requiredVehicle\":\"any\",\"minFuel\":0,\"distance\":120,"
            + "\"requiredCargo\":[{\"item\":\"minecraft:apple\",\"count\":1}],"
            + "\"rewards\":[{\"item\":\"minecraft:bread\",\"count\":1}],"
            + "\"fieldOps\":{\"durationTicks\":" + durationTicks
            + ",\"stageCount\":" + stageCount
            + ",\"incidentProfile\":\"" + incidentProfile + "\""
            + ",\"vehicleJoinPolicy\":\"" + vehicleJoinPolicy + "\"}}"
      ).getAsJsonObject());
   }

   private static ConvoyFacilityState preparedDepot(ConvoyRouteDefinition route) {
      ConvoyFacilityState depot = new ConvoyFacilityState();
      depot.loadCargo();
      depot.refuel();
      depot.refuel();
      depot.prepareRoute(route);
      return depot;
   }

   private static void assertResource(GameTestHelper helper, String path) {
      Identifier resourceId = id(path);
      helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(resourceId).isPresent(),
         "Expected resource " + resourceId);
   }

   private static void assertClasspathResource(GameTestHelper helper, String path) {
      helper.assertTrue(ModGameTests.class.getClassLoader().getResource(path) != null,
         "Expected packaged resource " + path);
   }

   private static void assertPngDimensions(GameTestHelper helper, String path, int expectedWidth, int expectedHeight) {
      try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
         helper.assertTrue(stream != null, "Expected packaged PNG " + path);
         byte[] header = stream.readNBytes(24);
         helper.assertTrue(header.length == 24 && header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E
               && header[3] == 0x47,
            "Expected valid PNG signature for " + path);
         int width = readPngInt(header, 16);
         int height = readPngInt(header, 20);
         helper.assertTrue(width == expectedWidth && height == expectedHeight,
            path + " should be " + expectedWidth + "x" + expectedHeight + " but was " + width + "x" + height);
      } catch (IOException exception) {
         helper.fail("Could not read PNG header for " + path + ": " + exception.getMessage());
      }
   }

   private static int readPngInt(byte[] header, int offset) {
      return ((header[offset] & 0xFF) << 24)
         | ((header[offset + 1] & 0xFF) << 16)
         | ((header[offset + 2] & 0xFF) << 8)
         | (header[offset + 3] & 0xFF);
   }

   private static void assertVehicleFootprint(
      GameTestHelper helper,
      EntityType<? extends ConvoyVehicleEntity> type,
      ConvoyVehicleKind kind,
      double expectedWidth,
      double expectedHeight,
      double expectedPassengerHeight
   ) {
      ConvoyVehicleEntity vehicle = spawnVehicle(helper, type, new BlockPos(kind.ordinal() * 4 + 1, 2, 1));
      helper.assertTrue(vehicle.kind() == kind, kind.displayName() + " should keep its registered vehicle kind");
      helper.assertTrue(closeTo(vehicle.getBoundingBox().getXsize(), expectedWidth, 0.04D),
         kind.displayName() + " hitbox width should match the production vehicle footprint");
      helper.assertTrue(closeTo(vehicle.getBoundingBox().getYsize(), expectedHeight, 0.04D),
         kind.displayName() + " hitbox height should match the production vehicle footprint");
      helper.assertTrue(closeTo(vehicle.kind().passengerHeight(), expectedPassengerHeight, 0.001D),
         kind.displayName() + " passenger height should place riders on the larger body");
      vehicle.discard();
   }

   private static boolean closeTo(double actual, double expected, double tolerance) {
      return Math.abs(actual - expected) <= tolerance;
   }

   private static ConvoyVehicleEntity spawnVehicle(GameTestHelper helper, EntityType<? extends ConvoyVehicleEntity> type, BlockPos relativePos) {
      ConvoyVehicleEntity vehicle = type.create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(vehicle != null, "Convoy vehicle should spawn");
      vehicle.setPos(helper.absolutePos(relativePos).getCenter());
      helper.getLevel().addFreshEntity(vehicle);
      return vehicle;
   }

   private static ConvoyVehicleEntity spawnBike(GameTestHelper helper, BlockPos relativePos) {
      return spawnVehicle(helper, ModEntities.SCRAP_BIKE.get(), relativePos);
   }

   private static boolean hasAdjacentFieldRepairStation(GameTestHelper helper, BlockPos localOrigin) {
      for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
         if (helper.getLevel().getBlockState(helper.absolutePos(localOrigin.relative(direction))).is(ModBlocks.FIELD_REPAIR_STATION.get())) {
            return true;
         }
      }
      return false;
   }

   private static void tickStation(GameTestHelper helper, BlockPos localPos, ConvoyStationBlockEntity station) {
      BlockPos worldPos = helper.absolutePos(localPos);
      ConvoyStationBlockEntity.tick(helper.getLevel(), worldPos, helper.getLevel().getBlockState(worldPos), station);
   }

   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData(
         environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, TEST_PADDING
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, path);
   }
}
