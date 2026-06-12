package com.knoxhack.echologisticsnetwork.registry;

import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.knoxhack.echoindustrialnexus.integration.IndustrialMachineCoreRuntimeProvider;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpoint;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock.LogisticsKind;
import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.content.FactionDepotOffer;
import com.knoxhack.echologisticsnetwork.content.FactoryRestockPolicy;
import com.knoxhack.echologisticsnetwork.content.LoadoutPreset;
import com.knoxhack.echologisticsnetwork.content.LoadoutRequirement;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.content.LogisticsJsonReloadListener;
import com.knoxhack.echologisticsnetwork.content.LoadoutCardSelection;
import com.knoxhack.echologisticsnetwork.content.RemoteRequestSelection;
import com.knoxhack.echologisticsnetwork.content.RouteManifestSelection;
import com.knoxhack.echologisticsnetwork.content.SupplyCategory;
import com.knoxhack.echologisticsnetwork.content.SupplyTagSelection;
import com.knoxhack.echologisticsnetwork.entity.CourierDroneEntity;
import com.knoxhack.echologisticsnetwork.integration.LogisticsCoreIntegration;
import com.knoxhack.echologisticsnetwork.integration.LogisticsIndustrialMachineEndpointProvider;
import com.knoxhack.echologisticsnetwork.integration.LogisticsMissionCoreIntegration;
import com.knoxhack.echologisticsnetwork.integration.LogisticsTerminalCommonIntegration;
import com.knoxhack.echologisticsnetwork.integration.LogisticsTerminalIds;
import com.knoxhack.echologisticsnetwork.integration.MachineCoreLogisticsEndpointProvider;
import com.knoxhack.echologisticsnetwork.item.LogisticsToolItem;
import com.knoxhack.echologisticsnetwork.menu.LogisticsMenu;
import com.knoxhack.echologisticsnetwork.api.LogisticsNetworkStatus;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryCategory;
import com.echoplatform.echocore.api.EchoDiscoveryEntry;
import com.echoplatform.echocore.api.EchoDiscoveryProvider;
import com.echoplatform.echocore.api.EchoDiscoveryState;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.TerminalRewardService;
import com.echoplatform.echocore.api.mission.InMemoryMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, EchoLogisticsNetwork.MODID);
   // Empty-structure tests place blocks manually; padding must exceed terminal scan radius to avoid cross-test networks.
   private static final int TEST_PADDING = 48;

   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODULE_REGISTRATION =
      TEST_FUNCTIONS.register("module_registration", () -> ModGameTests::moduleRegistration);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATA_PARSERS =
      TEST_FUNCTIONS.register("data_parsers", () -> ModGameTests::dataParsers);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATA_BACKED_TOOLS =
      TEST_FUNCTIONS.register("data_backed_tools", () -> ModGameTests::dataBackedTools);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BLOCK_STATE_PERSISTENCE =
      TEST_FUNCTIONS.register("block_state_persistence", () -> ModGameTests::blockStatePersistence);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTAINER_BREAK_DROPS_INVENTORY =
      TEST_FUNCTIONS.register("container_break_drops_inventory", () -> ModGameTests::containerBreakDropsInventory);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COURIER_PAYLOAD_PERSISTENCE =
      TEST_FUNCTIONS.register("courier_payload_persistence", () -> ModGameTests::courierPayloadPersistence);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STOCK_AND_READINESS =
      TEST_FUNCTIONS.register("stock_counting_and_readiness", () -> ModGameTests::stockCountingAndReadiness);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ITEM_RESERVATION =
      TEST_FUNCTIONS.register("item_reservation_extraction", () -> ModGameTests::itemReservationExtraction);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TARGET_CAPACITY =
      TEST_FUNCTIONS.register("target_capacity_precheck", () -> ModGameTests::targetCapacityPrecheck);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TARGET_BLOCK_TYPES =
      TEST_FUNCTIONS.register("target_block_type_enforcement", () -> ModGameTests::targetBlockTypeEnforcement);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEPOT_SELECTION =
      TEST_FUNCTIONS.register("depot_offer_selection_cooldown", () -> ModGameTests::depotOfferSelectionCooldown);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REMOTE_TABLET_LOOP =
      TEST_FUNCTIONS.register("remote_tablet_route_requester_loop", () -> ModGameTests::remoteTabletRouteRequesterLoop);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTO_RESTOCK_LOOP =
      TEST_FUNCTIONS.register("auto_restock_station_loop", () -> ModGameTests::autoRestockStationLoop);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DASHBOARD_SNAPSHOT =
      TEST_FUNCTIONS.register("dashboard_snapshot", () -> ModGameTests::dashboardSnapshot);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PUBLIC_NETWORK_STATUS =
      TEST_FUNCTIONS.register("public_network_status", () -> ModGameTests::publicNetworkStatus);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_DISCOVERY =
      TEST_FUNCTIONS.register("route_discovery", () -> ModGameTests::routeDiscovery);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRONE_DELIVERY =
      TEST_FUNCTIONS.register("courier_drone_delivery", () -> ModGameTests::courierDroneDelivery);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FAILURE_RECOVERY =
      TEST_FUNCTIONS.register("courier_failure_recovery", () -> ModGameTests::courierFailureRecovery);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_WIRING =
      TEST_FUNCTIONS.register("core_wiring", () -> ModGameTests::coreWiring);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_CROSS_ADDON =
      TEST_FUNCTIONS.register("core_cross_addon_integration", () -> ModGameTests::coreCrossAddonIntegration);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OFFLINE_DOCK_DIAGNOSTICS =
      TEST_FUNCTIONS.register("offline_dock_diagnostics", () -> ModGameTests::offlineDockDiagnostics);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELAY_IDEMPOTENCY =
      TEST_FUNCTIONS.register("relay_idempotency_edges", () -> ModGameTests::relayIdempotencyEdges);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INDUSTRIAL_DUCT_REACH =
      TEST_FUNCTIONS.register("industrial_duct_reach", () -> ModGameTests::industrialDuctReach);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INDUSTRIAL_DUCT_LIMIT =
      TEST_FUNCTIONS.register("industrial_duct_graph_limit", () -> ModGameTests::industrialDuctGraphLimit);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INDUSTRIAL_MACHINE_ENDPOINT =
      TEST_FUNCTIONS.register("industrial_machine_external_endpoint", () -> ModGameTests::industrialMachineExternalEndpoint);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_LEGACY_ENDPOINT_DEDUPE =
      TEST_FUNCTIONS.register("machinecore_legacy_endpoint_dedupe", () -> ModGameTests::machineCoreLegacyEndpointDedupe);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_LOADOUT =
      TEST_FUNCTIONS.register("factory_loadout_to_industrial_input_depot", () -> ModGameTests::factoryLoadoutToIndustrialInputDepot);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_AUTO_RESTOCK =
      TEST_FUNCTIONS.register("factory_auto_restock_to_industrial_input_depot", () -> ModGameTests::factoryAutoRestockToIndustrialInputDepot);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ROUTE_REFRESH =
      TEST_FUNCTIONS.register("terminal_route_refresh", () -> ModGameTests::terminalRouteRefresh);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECOVERY_NO_DUPLICATION =
      TEST_FUNCTIONS.register("recovery_no_duplication", () -> ModGameTests::recoveryNoDuplication);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ACTIONS =
      TEST_FUNCTIONS.register("terminal_actions", () -> ModGameTests::terminalActions);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
      TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      if (!shouldRegisterTests()) {
         return;
      }
      register(event, "module_registration", MODULE_REGISTRATION.getId());
      register(event, "data_parsers", DATA_PARSERS.getId());
      register(event, "data_backed_tools", DATA_BACKED_TOOLS.getId());
      register(event, "block_state_persistence", BLOCK_STATE_PERSISTENCE.getId());
      register(event, "container_break_drops_inventory", CONTAINER_BREAK_DROPS_INVENTORY.getId());
      register(event, "courier_payload_persistence", COURIER_PAYLOAD_PERSISTENCE.getId());
      register(event, "stock_counting_and_readiness", STOCK_AND_READINESS.getId());
      register(event, "item_reservation_extraction", ITEM_RESERVATION.getId());
      register(event, "target_capacity_precheck", TARGET_CAPACITY.getId());
      register(event, "target_block_type_enforcement", TARGET_BLOCK_TYPES.getId());
      register(event, "depot_offer_selection_cooldown", DEPOT_SELECTION.getId());
      register(event, "remote_tablet_route_requester_loop", REMOTE_TABLET_LOOP.getId(), 1200);
      register(event, "auto_restock_station_loop", AUTO_RESTOCK_LOOP.getId(), 800);
      register(event, "dashboard_snapshot", DASHBOARD_SNAPSHOT.getId());
      register(event, "public_network_status", PUBLIC_NETWORK_STATUS.getId());
      register(event, "route_discovery", ROUTE_DISCOVERY.getId());
      register(event, "courier_drone_delivery", DRONE_DELIVERY.getId());
      register(event, "courier_failure_recovery", FAILURE_RECOVERY.getId());
      register(event, "core_wiring", CORE_WIRING.getId());
      register(event, "core_cross_addon_integration", CORE_CROSS_ADDON.getId());
      register(event, "offline_dock_diagnostics", OFFLINE_DOCK_DIAGNOSTICS.getId());
      register(event, "relay_idempotency_edges", RELAY_IDEMPOTENCY.getId());
      register(event, "industrial_duct_reach", INDUSTRIAL_DUCT_REACH.getId());
      register(event, "industrial_duct_graph_limit", INDUSTRIAL_DUCT_LIMIT.getId());
      register(event, "industrial_machine_external_endpoint", INDUSTRIAL_MACHINE_ENDPOINT.getId());
      register(event, "machinecore_legacy_endpoint_dedupe", MACHINECORE_LEGACY_ENDPOINT_DEDUPE.getId());
      register(event, "factory_loadout_to_industrial_input_depot", FACTORY_LOADOUT.getId(), 400);
      register(event, "factory_auto_restock_to_industrial_input_depot", FACTORY_AUTO_RESTOCK.getId(), 400);
      register(event, "terminal_route_refresh", TERMINAL_ROUTE_REFRESH.getId());
      register(event, "recovery_no_duplication", RECOVERY_NO_DUPLICATION.getId());
      register(event, "terminal_actions", TERMINAL_ACTIONS.getId());
      register(event, "missioncore_content_registration", MISSION_CORE_CONTENT.getId());
   }

   private static void moduleRegistration(GameTestHelper helper) {
      helper.assertTrue(ModBlocks.LOGISTICS_TERMINAL.get() != Blocks.AIR, "Logistics Terminal should be registered");
      helper.assertTrue(ModItems.REMOTE_REQUEST_TABLET.get() != Items.AIR, "Remote Request Tablet should be registered");
      helper.assertTrue(ModEntities.COURIER_DRONE.get() != null, "Courier drone entity should be registered");
      helper.succeed();
   }

   private static void dataParsers(GameTestHelper helper) {
      SupplyCategory category = LogisticsJsonReloadListener.parseCategoryForTests(id("water_parser"),
         JsonParser.parseString("{\"title\":\"Water\",\"order\":1,\"accentColor\":\"0xFF66E8FF\",\"lowStockTarget\":4,\"tagId\":\"echologisticsnetwork:echo_logistics/water\"}").getAsJsonObject());
      helper.assertTrue(category.lowStockTarget() == 4, "Category parser should load low stock targets");
      helper.assertTrue(category.accentColor() == 0xFF66E8FF, "Category parser should accept ARGB color strings");
      LoadoutPreset preset = LogisticsJsonReloadListener.parseLoadoutForTests(id("test_loadout"),
         JsonParser.parseString("{\"title\":\"Kit\",\"requirements\":[{\"item\":\"minecraft:apple\",\"count\":2}],\"targetBlockTypes\":[\"echologisticsnetwork:loadout_locker\"],\"deliveryTicks\":80}").getAsJsonObject());
      helper.assertTrue(preset.requirements().size() == 1 && preset.deliveryTicks() == 80, "Loadout parser should load requirements");
      helper.assertTrue(preset.targetBlockTypes().contains(id("loadout_locker")), "Loadout parser should load target block restrictions");
      LoadoutPreset restockPreset = LogisticsJsonReloadListener.parseLoadoutForTests(id("factory_restock_parser"),
         JsonParser.parseString("{\"requirements\":[{\"item\":\"minecraft:apple\",\"count\":2}],\"factoryTaskId\":\"echoindustrialnexus:weld_reinforced_machine_frame\",\"restockTargetRuns\":5,\"restockMinRuns\":1,\"restockMaxInFlight\":2,\"restockCooldownTicks\":240}").getAsJsonObject());
      helper.assertTrue(restockPreset.restockPolicy().enabled()
         && restockPreset.restockPolicy().targetRuns() == 5
         && restockPreset.restockPolicy().maxInFlight() == 2,
         "Loadout parser should load optional factory restock policy metadata");
      FactionDepotOffer offer = LogisticsJsonReloadListener.parseOfferForTests(id("offer"),
         JsonParser.parseString("{\"factionId\":\"echoashfallprotocol:crashbreak_salvage\",\"input\":{\"item\":\"minecraft:emerald\",\"count\":1},\"output\":{\"item\":\"minecraft:bread\",\"count\":2},\"cooldownTicks\":77}").getAsJsonObject());
      helper.assertTrue(offer.output().getCount() == 2, "Faction offer parser should load output stacks");
      helper.assertTrue(offer.cooldownTicks() == 77, "Faction offer parser should load cooldown ticks");
      assertJsonParseFails(helper, () -> LogisticsJsonReloadListener.parseLoadoutForTests(id("bad_loadout"),
         JsonParser.parseString("{\"requirements\":[{\"count\":1}]}").getAsJsonObject()), "Loadout requirements should reject missing item/tag/category selectors");
      assertJsonParseFails(helper, () -> LogisticsJsonReloadListener.parseOfferForTests(id("bad_offer"),
         JsonParser.parseString("{\"input\":{\"item\":\"minecraft:emerald\",\"count\":1},\"output\":{\"count\":1}}").getAsJsonObject()), "Faction offers should reject output stacks without item ids");
      helper.succeed();
   }

   private static void dataBackedTools(GameTestHelper helper) {
      BlockPos lockerPos = new BlockPos(1, 1, 1);
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      LogisticsBlockEntity logistics = helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);

      ItemStack tag = new ItemStack(ModItems.SUPPLY_TAG.get());
      tag.set(ModDataComponents.SUPPLY_TAG_SELECTION.get(), new SupplyTagSelection("echologisticsnetwork:water"));
      InteractionResult tagResult = ((LogisticsToolItem)tag.getItem()).applyToBlock(tag, helper.getLevel(), helper.absolutePos(lockerPos), player);
      helper.assertTrue(tagResult == InteractionResult.SUCCESS_SERVER, "Supply Tag should apply to logistics blocks");
      helper.assertTrue("echologisticsnetwork:water".equals(logistics.categoryId()), "Supply Tag should persist selected category on the block");

      ItemStack card = new ItemStack(ModItems.LOADOUT_CARD.get());
      card.set(ModDataComponents.LOADOUT_CARD_SELECTION.get(), new LoadoutCardSelection("echologisticsnetwork:radiation_expedition_kit"));
      InteractionResult cardResult = ((LogisticsToolItem)card.getItem()).applyToBlock(card, helper.getLevel(), helper.absolutePos(lockerPos), player);
      helper.assertTrue(cardResult == InteractionResult.SUCCESS_SERVER, "Loadout Card should apply to logistics blocks");
      helper.assertTrue("echologisticsnetwork:radiation_expedition_kit".equals(logistics.loadoutId()), "Loadout Card should persist selected loadout on the block");

      ItemStack manifest = new ItemStack(ModItems.ROUTE_MANIFEST.get());
      manifest.set(ModDataComponents.ROUTE_MANIFEST_SELECTION.get(), new RouteManifestSelection("Cargo Alpha"));
      InteractionResult manifestResult = ((LogisticsToolItem)manifest.getItem()).applyToBlock(manifest, helper.getLevel(), helper.absolutePos(lockerPos), player);
      helper.assertTrue(manifestResult == InteractionResult.SUCCESS_SERVER, "Route Manifest should apply to logistics blocks");
      helper.assertTrue("cargo_alpha".equals(logistics.networkId()), "Route Manifest should persist a sanitized network id on the block");
      helper.assertTrue("cargo_alpha".equals(manifest.get(ModDataComponents.ROUTE_MANIFEST_SELECTION.get()).networkId()), "Route Manifest should keep its stored network data");

      ItemStack tablet = new ItemStack(ModItems.REMOTE_REQUEST_TABLET.get());
      InteractionResult bindResult = ((LogisticsToolItem)tablet.getItem()).applyToBlock(tablet, helper.getLevel(), helper.absolutePos(lockerPos), player);
      helper.assertTrue(bindResult == InteractionResult.SUCCESS_SERVER, "Remote Request Tablet should bind to logistics blocks");
      RemoteRequestSelection captured = tablet.get(ModDataComponents.REMOTE_REQUEST_SELECTION.get());
      helper.assertTrue(captured != null && "cargo_alpha".equals(captured.networkId()), "Remote Request Tablet should capture network data");
      helper.assertTrue(captured != null && "echologisticsnetwork:radiation_expedition_kit".equals(captured.loadoutId()), "Remote Request Tablet should capture loadout data");
      helper.assertTrue(captured != null && captured.targetPos().filter(pos -> pos.equals(helper.absolutePos(lockerPos))).isPresent(), "Remote Request Tablet should capture target position data");

      String categoryBeforeScan = logistics.categoryId();
      String networkBeforeScan = logistics.networkId();
      player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
      InteractionResult scanResult = ((LogisticsToolItem)tablet.getItem()).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(scanResult == InteractionResult.SUCCESS_SERVER, "Remote Request Tablet should handle its bound route");
      helper.assertTrue(categoryBeforeScan.equals(logistics.categoryId()) && networkBeforeScan.equals(logistics.networkId()), "Remote tablet use should not mutate bound block state");
      helper.succeed();
   }

   private static void blockStatePersistence(GameTestHelper helper) {
      BlockPos depotPos = new BlockPos(1, 1, 1);
      helper.setBlock(depotPos, (Block)ModBlocks.FACTION_TRADE_DEPOT.get());
      LogisticsBlockEntity depot = helper.getBlockEntity(depotPos, LogisticsBlockEntity.class);
      depot.setNetworkId("Persist Net 01");
      depot.setCategoryId("echologisticsnetwork:faction_goods");
      depot.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      depot.setDepotOfferId("echologisticsnetwork:salvage_water_exchange");
      depot.setCooldownTicks(73);
      depot.recordManifest("Persisted manifest for release smoke");
      depot.setItem(0, new ItemStack(Items.EMERALD, 3));
      depot.setItem(1, new ItemStack(Items.COPPER_INGOT, 7));

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      depot.saveWithoutMetadata(output);
      CompoundTag saved = output.buildResult();
      LogisticsBlockEntity loaded = new LogisticsBlockEntity(helper.absolutePos(depotPos), depot.getBlockState());
      loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      helper.assertTrue("persist_net_01".equals(loaded.networkId()), "Logistics block should persist sanitized network ids");
      helper.assertTrue("echologisticsnetwork:faction_goods".equals(loaded.categoryId()), "Logistics block should persist category labels");
      helper.assertTrue("echologisticsnetwork:toxic_expedition_kit".equals(loaded.loadoutId()), "Logistics block should persist selected loadouts");
      helper.assertTrue("echologisticsnetwork:salvage_water_exchange".equals(loaded.depotOfferId()), "Logistics block should persist selected depot offers");
      helper.assertTrue(loaded.cooldownTicks() == 73, "Logistics block should persist cooldowns");
      helper.assertTrue(countItem(loaded, Items.EMERALD) == 3 && countItem(loaded, Items.COPPER_INGOT) == 7,
         "Logistics block should persist inventory contents without changing counts");
      helper.assertTrue(loaded.statusLine().contains("Persisted manifest"), "Logistics block should persist the last manifest/status line");
      helper.succeed();
   }

   private static void containerBreakDropsInventory(GameTestHelper helper) {
      BlockPos cratePos = new BlockPos(1, 1, 1);
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setItem(0, new ItemStack(Items.EMERALD, 3));
      crate.setItem(1, new ItemStack(Items.COPPER_INGOT, 7));
      crate.setItem(2, new ItemStack(Items.APPLE, 2));
      int emeraldDropsBefore = countDroppedItems(helper, cratePos, Items.EMERALD);
      int copperDropsBefore = countDroppedItems(helper, cratePos, Items.COPPER_INGOT);
      int appleDropsBefore = countDroppedItems(helper, cratePos, Items.APPLE);

      helper.getLevel().destroyBlock(helper.absolutePos(cratePos), false);

      int emeraldDelta = countDroppedItems(helper, cratePos, Items.EMERALD) - emeraldDropsBefore;
      int copperDelta = countDroppedItems(helper, cratePos, Items.COPPER_INGOT) - copperDropsBefore;
      int appleDelta = countDroppedItems(helper, cratePos, Items.APPLE) - appleDropsBefore;
      helper.assertTrue(emeraldDelta == 3 && copperDelta == 7 && appleDelta == 2,
         "Breaking a stocked Logistics container should drop exact stored contents once; emerald="
            + emeraldDelta + ", copper=" + copperDelta + ", apple=" + appleDelta);
      helper.assertTrue(countItem(crate, Items.EMERALD) == 0 && countItem(crate, Items.COPPER_INGOT) == 0 && countItem(crate, Items.APPLE) == 0,
         "Dropped Logistics contents should be cleared from the removed block entity to prevent duplicate drops");

      crate.preRemoveSideEffects(helper.absolutePos(cratePos), crate.getBlockState());
      helper.assertTrue(countDroppedItems(helper, cratePos, Items.EMERALD) - emeraldDropsBefore == 3
            && countDroppedItems(helper, cratePos, Items.COPPER_INGOT) - copperDropsBefore == 7
            && countDroppedItems(helper, cratePos, Items.APPLE) - appleDropsBefore == 2,
         "Repeated removal side effects should not duplicate already-dropped Logistics contents");
      helper.succeed();
   }

   private static void courierPayloadPersistence(GameTestHelper helper) {
      UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000123");
      UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000456");
      BlockPos sourceDock = helper.absolutePos(new BlockPos(1, 1, 1));
      BlockPos target = helper.absolutePos(new BlockPos(6, 1, 1));
      CourierDroneEntity drone = ModEntities.COURIER_DRONE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(drone != null, "Courier drone should instantiate for persistence");
      drone.configureDelivery(jobId, owner, "persist-net", sourceDock, target, id("toxic_expedition_kit"),
         List.of(new ItemStack(Items.POTION, 5), new ItemStack(Items.APPLE, 3)), 90);

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      drone.saveWithoutId(output);
      CompoundTag saved = output.buildResult();
      CourierDroneEntity loaded = ModEntities.COURIER_DRONE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(loaded != null, "Reloaded courier drone should instantiate");
      loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      LogisticsNetworkService.DeliveryJob job = loaded.deliveryJob();
      helper.assertTrue(job.id().equals(jobId), "Courier should persist its job id");
      helper.assertTrue(owner.equals(job.owner()), "Courier should persist its owner id");
      helper.assertTrue("persist-net".equals(loaded.networkId()), "Courier should persist its network id");
      helper.assertTrue(sourceDock.equals(job.sourceDock()) && target.equals(job.targetPos()), "Courier should persist source and target positions");
      helper.assertTrue(id("toxic_expedition_kit").equals(job.presetId()), "Courier should persist the requested preset id");
      helper.assertTrue(job.etaTick() - job.createdTick() == 90, "Courier should persist its delivery timing");
      helper.assertTrue("in_transit".equals(job.status()), "Courier should reload as an in-transit job");
      helper.assertTrue(countPayload(job, Items.POTION) == 5 && countPayload(job, Items.APPLE) == 3,
         "Courier payload should survive save/load without changing item counts");
      helper.succeed();
   }

   private static void stockCountingAndReadiness(GameTestHelper helper) {
      BlockPos cratePos = new BlockPos(1, 1, 1);
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("stock-test");
      crate.setItem(0, new ItemStack(Items.POTION));
      crate.setItem(1, new ItemStack(Items.POTION));
      crate.setItem(2, new ItemStack(Items.POTION));
      crate.setItem(3, new ItemStack(Items.POTION));
      crate.setItem(4, new ItemStack(Items.APPLE, 4));
      crate.setItem(5, new ItemStack(Items.PAPER, 4));
      crate.setItem(6, new ItemStack(Items.GOLDEN_APPLE, 2));
      helper.runAfterDelay(2L, () -> {
         LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(cratePos), "stock-test", helper.makeMockPlayer(GameType.CREATIVE));
         int water = snapshot.stockRows().stream().filter(row -> row.categoryId().getPath().equals("water")).findFirst().map(LogisticsNetworkService.StockRow::count).orElse(-1);
         helper.assertTrue(water >= 4, "Water stock should count tagged potions");
         helper.assertTrue(snapshot.loadoutReadiness().stream().anyMatch(LogisticsNetworkService.LoadoutReadiness::ready), "At least one default loadout should be ready");
         helper.succeed();
      });
   }

   private static void itemReservationExtraction(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos lockerPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("reservation-test");
      helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class).setNetworkId("reservation-test");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("reservation-test");
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.runAfterDelay(2L, () -> {
         boolean requested = LogisticsNetworkService.requestLoadout(player, helper.absolutePos(dockPos), helper.absolutePos(lockerPos), "echologisticsnetwork:toxic_expedition_kit");
         helper.assertTrue(requested, "Loadout request should reserve and extract supplies");
         helper.assertTrue(countItem(crate, Items.POTION) < 3, "Reserved payload should be removed from storage before drone launch");
         LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(dockPos), "reservation-test", player);
         helper.assertTrue(snapshot.activeDeliveries() == 1, "Dispatched courier should be tracked as an active delivery");
         helper.assertTrue(snapshot.deliveryJobs().size() == 1 && "in_transit".equals(snapshot.deliveryJobs().get(0).status()), "Active delivery should expose a tracked job");
         helper.succeed();
      });
   }

   private static void targetCapacityPrecheck(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos lockerPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("capacity-test");
      helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class).setNetworkId("capacity-test");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("capacity-test");
      fillBasicKit(crate);
      LogisticsBlockEntity locker = helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class);
      for (int slot = 0; slot < locker.getContainerSize(); slot++) {
         locker.setItem(slot, new ItemStack(Items.DIRT, 64));
      }
      int potionsBefore = countItem(crate, Items.POTION);
      int applesBefore = countItem(crate, Items.APPLE);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.runAfterDelay(2L, () -> {
         boolean requested = LogisticsNetworkService.requestLoadout(player, helper.absolutePos(dockPos), helper.absolutePos(lockerPos), "echologisticsnetwork:toxic_expedition_kit");
         helper.assertFalse(requested, "Full target inventory should block loadout dispatch before extraction");
         helper.assertTrue(countItem(crate, Items.POTION) == potionsBefore && countItem(crate, Items.APPLE) == applesBefore,
            "Capacity precheck should leave source storage unchanged");
         LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(dockPos), "capacity-test", player);
         helper.assertTrue(snapshot.activeDeliveries() == 0, "Capacity precheck should not spawn a courier for the blocked network");
         helper.succeed();
      });
   }

   private static void targetBlockTypeEnforcement(GameTestHelper helper) {
      Identifier presetId = id("target_lock_test");
      LogisticsContent.replaceJsonContent(new LogisticsContent.LoadedContent(Map.of(), Map.of(presetId, new LoadoutPreset(
         presetId,
         "Target Lock Test",
         500,
         Identifier.withDefaultNamespace("apple"),
         List.of(new LoadoutRequirement(LoadoutRequirement.Kind.ITEM, Identifier.withDefaultNamespace("apple"), 1, false)),
         List.of(id("loadout_locker")),
         40
      )), Map.of()));
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos depotPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(depotPos, (Block)ModBlocks.FACTION_TRADE_DEPOT.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("target-type-test");
      helper.getBlockEntity(depotPos, LogisticsBlockEntity.class).setNetworkId("target-type-test");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("target-type-test");
      crate.setItem(0, new ItemStack(Items.APPLE, 2));
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.runAfterDelay(2L, () -> {
         try {
            boolean requested = LogisticsNetworkService.requestLoadout(player, helper.absolutePos(dockPos), helper.absolutePos(depotPos), presetId.toString());
            helper.assertFalse(requested, "Preset targetBlockTypes should reject a disallowed but inventory-capable target block");
            helper.assertTrue(countItem(crate, Items.APPLE) == 2, "Target block rejection should happen before extraction");
            LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(dockPos), "target-type-test", player);
            helper.assertTrue(snapshot.activeDeliveries() == 0, "Target block rejection should not spawn a courier for the blocked network");
         } finally {
            LogisticsContent.clearJsonForTests();
         }
         helper.succeed();
      });
   }

   private static void depotOfferSelectionCooldown(GameTestHelper helper) {
      Identifier cheapOfferId = id("cheap_copper_exchange");
      Identifier selectedOfferId = id("selected_emerald_exchange");
      FactionDepotOffer cheapOffer = new FactionDepotOffer(cheapOfferId, id("test_faction"),
         new FactionDepotOffer.StackSpec(Identifier.withDefaultNamespace("copper_ingot"), 2),
         new FactionDepotOffer.StackSpec(Identifier.withDefaultNamespace("potion"), 1), 0, 0, 20);
      FactionDepotOffer selectedOffer = new FactionDepotOffer(selectedOfferId, id("test_faction"),
         new FactionDepotOffer.StackSpec(Identifier.withDefaultNamespace("emerald"), 1),
         new FactionDepotOffer.StackSpec(Identifier.withDefaultNamespace("bread"), 2), 0, 0, 40);
      LogisticsContent.replaceJsonContent(new LogisticsContent.LoadedContent(Map.of(), Map.of(), Map.of(
         cheapOfferId, cheapOffer,
         selectedOfferId, selectedOffer
      )));
      BlockPos depotPos = new BlockPos(1, 1, 1);
      helper.setBlock(depotPos, (Block)ModBlocks.FACTION_TRADE_DEPOT.get());
      LogisticsBlockEntity depot = helper.getBlockEntity(depotPos, LogisticsBlockEntity.class);
      depot.setDepotOfferId(selectedOfferId.toString());
      depot.setItem(0, new ItemStack(Items.COPPER_INGOT, 2));
      depot.setItem(1, new ItemStack(Items.EMERALD, 1));
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      try {
         boolean exchanged = LogisticsNetworkService.performDepotExchange(player, depot);
         helper.assertTrue(exchanged, "Selected depot offer should exchange when inputs and reputation match");
         helper.assertTrue(countItem(depot, Items.EMERALD) == 0 && countItem(depot, Items.BREAD) == 2, "Selected offer should consume emeralds and insert bread");
         helper.assertTrue(countItem(depot, Items.COPPER_INGOT) == 2, "Selected offer should not consume inputs from another matching offer");
         helper.assertTrue(depot.cooldownTicks() == selectedOffer.cooldownTicks(), "Completed offer should apply its data-driven cooldown");
         int breadBeforeCooldown = countItem(depot, Items.BREAD);
         boolean blocked = LogisticsNetworkService.performDepotExchange(player, depot);
         helper.assertFalse(blocked, "Depot cooldown should block immediate repeated exchanges");
         helper.assertTrue(countItem(depot, Items.BREAD) == breadBeforeCooldown && countItem(depot, Items.COPPER_INGOT) == 2,
            "Cooldown rejection should not mutate depot inventory");
      } finally {
         LogisticsContent.clearJsonForTests();
      }
      helper.succeed();
   }

   private static void remoteTabletRouteRequesterLoop(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos requesterPos = new BlockPos(2, 1, 1);
      BlockPos cratePos = new BlockPos(4, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(requesterPos, (Block)ModBlocks.ROUTE_REQUESTER.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("tablet-loop");
      LogisticsBlockEntity requester = helper.getBlockEntity(requesterPos, LogisticsBlockEntity.class);
      requester.setNetworkId("tablet-loop");
      requester.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("tablet-loop");
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(requesterPos).getCenter());
      ItemStack tablet = new ItemStack(ModItems.REMOTE_REQUEST_TABLET.get());
      InteractionResult bindResult = ((LogisticsToolItem)tablet.getItem()).applyToBlock(tablet, helper.getLevel(), helper.absolutePos(requesterPos), player);
      helper.assertTrue(bindResult == InteractionResult.SUCCESS_SERVER, "Tablet should bind to Route Requester");
      player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
      helper.runAfterDelay(2L, () -> {
         InteractionResult requestResult = ((LogisticsToolItem)tablet.getItem()).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
         helper.assertTrue(requestResult == InteractionResult.SUCCESS_SERVER, "Tablet should dispatch a bound Route Requester loadout");
         LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "tablet-loop", player);
         helper.assertTrue(snapshot.activeDeliveries() == 1, "Remote tablet dispatch should create a tracked drone delivery");
         helper.assertTrue(countItem(crate, Items.POTION) < 3, "Remote tablet dispatch should reserve supplies before delivery");
      });
      helper.succeedWhen(() -> {
         tickNetworkDrones(helper, requesterPos, "tablet-loop");
         helper.assertTrue(countItem(requester, Items.POTION) > 0 && countItem(requester, Items.APPLE) > 0,
            "Route Requester should receive the remote tablet loadout payload");
      });
   }

   private static void autoRestockStationLoop(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos restockPos = new BlockPos(2, 1, 1);
      BlockPos cratePos = new BlockPos(4, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(restockPos, (Block)ModBlocks.AUTO_RESTOCK_STATION.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("restock-loop");
      LogisticsBlockEntity restock = helper.getBlockEntity(restockPos, LogisticsBlockEntity.class);
      restock.setNetworkId("restock-loop");
      restock.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("restock-loop");
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(restockPos).getCenter());
      helper.runAfterDelay(2L, () -> {
         boolean dispatched = restock.tryAutoRestock(player);
         helper.assertTrue(dispatched, "Auto-Restock Station should dispatch when empty and selected loadout is ready");
         LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(restockPos), "restock-loop", player);
         helper.assertTrue(snapshot.activeDeliveries() == 1, "Auto-restock dispatch should create a tracked drone delivery");
      });
      helper.succeedWhen(() -> {
         tickNetworkDrones(helper, restockPos, "restock-loop");
         helper.assertTrue(countItem(restock, Items.POTION) > 0 && countItem(restock, Items.APPLE) > 0,
            "Auto-Restock Station should receive the selected loadout payload; restock="
               + totals(countItem(restock, Items.POTION), countItem(restock, Items.APPLE), countItem(restock, Items.PAPER), countItem(restock, Items.GOLDEN_APPLE))
               + ", crate="
               + totals(countItem(crate, Items.POTION), countItem(crate, Items.APPLE), countItem(crate, Items.PAPER), countItem(crate, Items.GOLDEN_APPLE))
               + ", active="
               + LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(restockPos), "restock-loop", player).activeDeliveries());
         });
   }

   private static void dashboardSnapshot(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos requesterPos = new BlockPos(5, 1, 1);
      BlockPos relayPos = new BlockPos(1, 1, 4);
      BlockPos depotPos = new BlockPos(2, 1, 4);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(requesterPos, (Block)ModBlocks.ROUTE_REQUESTER.get());
      helper.setBlock(relayPos, (Block)ModBlocks.REMOTE_REWARD_RELAY.get());
      helper.setBlock(depotPos, (Block)ModBlocks.FACTION_TRADE_DEPOT.get());
      for (BlockPos pos : List.of(dockPos, cratePos, requesterPos, relayPos, depotPos)) {
         helper.getBlockEntity(pos, LogisticsBlockEntity.class).setNetworkId("dashboard-net");
      }
      LogisticsBlockEntity requester = helper.getBlockEntity(requesterPos, LogisticsBlockEntity.class);
      requester.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      LogisticsBlockEntity depot = helper.getBlockEntity(depotPos, LogisticsBlockEntity.class);
      depot.setCooldownTicks(81);
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(requesterPos.above()).getCenter());

      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "dashboard-net", player);
      helper.assertTrue(snapshot.blockCount() == 5, "Dashboard snapshot should count all Logistics blocks on the network; counted " + snapshot.blockCount());
      helper.assertTrue(snapshot.endpointCount() == 1, "Dashboard snapshot should count request endpoints; counted " + snapshot.endpointCount());
      helper.assertTrue(snapshot.dockOnline() && snapshot.relayOnline() && snapshot.depotOnline(),
         "Dashboard snapshot should report dock, relay, and depot online");
      helper.assertTrue(snapshot.depotCooldown() == 81, "Dashboard snapshot should expose depot cooldowns");
      helper.assertTrue(snapshot.selectedEndpoint() != null && snapshot.selectedEndpoint().kind() == LogisticsKind.ROUTE_REQUESTER,
         "Dashboard snapshot should choose the nearby Route Requester as the selected endpoint");
      helper.assertTrue("echologisticsnetwork:toxic_expedition_kit".equals(snapshot.selectedLoadoutId()) && snapshot.selectedReady(),
         "Dashboard snapshot should expose selected loadout readiness");
      helper.assertTrue("dashboard-net|echologisticsnetwork:toxic_expedition_kit".equals(snapshot.requestPayload()),
         "Dashboard snapshot should expose the terminal/block request payload");

      boolean dispatched = LogisticsNetworkService.requestDashboardLoadout(player, helper.absolutePos(dockPos), "dashboard-net");
      helper.assertTrue(dispatched, "Dashboard dispatch should use the selected endpoint/loadout");
      LogisticsNetworkService.LogisticsSnapshot active = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "dashboard-net", player);
      helper.assertTrue(active.activeDeliveries() == 1 && active.deliveryJobs().size() == 1,
         "Dashboard snapshot should expose a bounded active delivery row after dispatch");
      helper.assertTrue(active.deliveryJobs().getFirst().targetPos().equals(helper.absolutePos(requesterPos)),
         "Dashboard delivery row should target the selected endpoint");
      helper.succeed();
   }

   private static void publicNetworkStatus(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos requesterPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(requesterPos, (Block)ModBlocks.ROUTE_REQUESTER.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("status-net");
      helper.getBlockEntity(cratePos, LogisticsBlockEntity.class).setNetworkId("status-net");
      LogisticsBlockEntity requester = helper.getBlockEntity(requesterPos, LogisticsBlockEntity.class);
      requester.setNetworkId("status-net");
      requester.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      fillBasicKit(helper.getBlockEntity(cratePos, LogisticsBlockEntity.class));
      Player player = helper.makeMockPlayer(GameType.CREATIVE);

      LogisticsNetworkStatus status = LogisticsNetworkService.networkStatus(helper.getLevel(), helper.absolutePos(requesterPos), "status-net", player);
      helper.assertTrue("status-net".equals(status.networkId()), "Public Logistics status should expose normalized network id");
      helper.assertTrue(status.blockCount() == 3 && status.endpointCount() == 1, "Public Logistics status should expose block and endpoint counts");
      helper.assertTrue(status.dockOnline(), "Public Logistics status should expose dock readiness");
      helper.assertTrue(status.selectedReady() && "echologisticsnetwork:toxic_expedition_kit".equals(status.selectedLoadoutId()),
         "Public Logistics status should expose selected loadout readiness without mutable dashboard internals");
      LogisticsNetworkService.LogisticsSnapshot readySnapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "status-net", player);
      helper.assertTrue(readySnapshot.canDispatch() && readySnapshot.dispatchBlockedReason().isBlank(),
         "Ready Logistics snapshot should not report a dispatch blocker");
      LogisticsNetworkService.LogisticsSnapshot emptySnapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(new BlockPos(18, 1, 1)), "missing-status-net", player, true);
      helper.assertTrue(!emptySnapshot.canDispatch() && emptySnapshot.dispatchBlockedReason().contains("No eligible"),
         "Blocked Logistics snapshot should explain missing endpoints");
      helper.succeed();
   }

   private static void routeDiscovery(GameTestHelper helper) {
      resetCoreServicesForLogisticsTests();
      registerRouteDiscoveryProviderForTests();
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos lockerPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(dockPos).getCenter());
      Identifier presetId = id("toxic_expedition_kit");
      Identifier discoveryId = EchoCoreServices.routeDiscoveryId(LogisticsTerminalIds.id("route/" + presetId.getPath()));

      helper.runAfterDelay(2L, () -> {
         helper.assertFalse(EchoCoreServices.hasDiscoveredFeature(player, discoveryId),
            "Fresh logistics player should not start with route discovery");
         TerminalActionRegistry.withClearedForTests(() -> {
            LogisticsTerminalCommonIntegration.register();
            helper.assertTrue(TerminalActionRegistry.handle(null, LogisticsTerminalIds.LOGISTICS_TAB,
                  LogisticsTerminalIds.SCAN_ACTION, ""),
               "Logistics scan action should register safely without a server player");
         });
         helper.assertTrue(EchoCoreServices.routeRecords(player).stream().anyMatch(record -> record.id().equals(LogisticsTerminalIds.id("route/" + presetId.getPath())) && "READY".equals(record.status())),
            "Scanning-ready logistics networks should expose ready route records");

         Player blockedPlayer = helper.makeMockPlayer(GameType.CREATIVE);
         blockedPlayer.setPos(helper.absolutePos(new BlockPos(8, 1, 1)).getCenter());
         boolean blocked = LogisticsNetworkService.requestLoadout(blockedPlayer, helper.absolutePos(new BlockPos(8, 1, 1)),
            helper.absolutePos(new BlockPos(8, 1, 1)), presetId.toString());
         helper.assertFalse(blocked, "Blocked loadout requests should fail");
         helper.assertFalse(EchoCoreServices.hasDiscoveredFeature(blockedPlayer, discoveryId),
            "Blocked loadout requests should not persist route discovery");

         boolean requested = LogisticsNetworkService.requestLoadout(player, helper.absolutePos(dockPos),
            helper.absolutePos(lockerPos), presetId.toString());
         helper.assertTrue(requested, "Successful logistics dispatch should still work after scan discovery");
         helper.assertFalse(EchoCoreServices.hasDiscoveredFeature(player, discoveryId),
            "Mock-player dispatch should not require server-player discovery persistence");

         resetCoreServicesForLogisticsTests();
         helper.succeed();
      });
   }

   private static void courierDroneDelivery(GameTestHelper helper) {
      BlockPos targetPos = new BlockPos(3, 1, 1);
      helper.setBlock(targetPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      CourierDroneEntity drone = ModEntities.COURIER_DRONE.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
      helper.assertTrue(drone != null, "Courier drone should spawn");
      drone.configureDelivery(helper.makeMockPlayer(GameType.CREATIVE).getUUID(), helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(targetPos),
         id("test_payload"), List.of(new ItemStack(Items.APPLE, 2)), 40);
      drone.setPos(helper.absolutePos(new BlockPos(2, 2, 1)).getCenter());
      helper.getLevel().addFreshEntity(drone);
      helper.succeedWhen(() -> {
         drone.tick();
         LogisticsBlockEntity locker = helper.getBlockEntity(targetPos, LogisticsBlockEntity.class);
         helper.assertTrue(locker.getItem(0).is(Items.APPLE), "Courier drone should deposit payload into target locker");
      });
   }

   private static void courierFailureRecovery(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      CourierDroneEntity drone = ModEntities.COURIER_DRONE.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
      helper.assertTrue(drone != null, "Courier drone should spawn for recovery");
      drone.configureDelivery(helper.makeMockPlayer(GameType.CREATIVE).getUUID(), helper.absolutePos(dockPos), helper.absolutePos(new BlockPos(8, 1, 1)),
         id("bad_target"), List.of(new ItemStack(Items.BREAD, 3)), 40);
      drone.setPos(helper.absolutePos(new BlockPos(7, 2, 1)).getCenter());
      helper.getLevel().addFreshEntity(drone);
      for (int i = 0; i < 170; i++) {
         drone.tick();
      }
      LogisticsBlockEntity dock = helper.getBlockEntity(dockPos, LogisticsBlockEntity.class);
      helper.assertTrue(dock.getItem(0).is(Items.BREAD) || !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(dockPos)).inflate(2)).isEmpty(),
         "Failed courier should recover payload to dock or recoverable item drop");
      helper.succeed();
   }

   private static void coreWiring(GameTestHelper helper) {
      LogisticsCoreIntegration.registerAddonChapter();
      helper.assertTrue(com.echoplatform.echocore.api.EchoAddonRegistry.isRegistered(LogisticsCoreIntegration.CHAPTER_ID),
         "Logistics should register as an ECHO addon chapter");
      helper.succeed();
   }

   private static void coreCrossAddonIntegration(GameTestHelper helper) {
      LogisticsCoreIntegration.registerAddonChapter();
      BlockPos cratePos = new BlockPos(1, 1, 1);
      BlockPos lockerPos = new BlockPos(4, 1, 1);
      BlockPos dockPos = new BlockPos(7, 1, 1);
      BlockPos relayPos = new BlockPos(4, 1, 3);
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      LogisticsBlockEntity locker = helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class);
      crate.setNetworkId("core-layer7");
      locker.setNetworkId("core-layer7");
      locker.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(lockerPos.above()).getCenter());

      EchoRouteRecord offlineRoute = logisticsRoute(player, "toxic_expedition_kit");
      helper.assertTrue(offlineRoute != null && "DOCK OFFLINE".equals(offlineRoute.status()) && !offlineRoute.complete(),
         "Core route records should distinguish supply readiness from offline dock readiness");
      helper.assertTrue(hasLogisticsDiagnostic(player, "offline_dock"),
         "Core diagnostics should report an offline Drone Delivery Dock");

      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("core-layer7");
      EchoRouteRecord readyRoute = logisticsRoute(player, "toxic_expedition_kit");
      helper.assertTrue(readyRoute != null && "READY".equals(readyRoute.status()) && readyRoute.complete(),
         "Core route records should mark a stocked network with dock and endpoint as dispatch-ready");

      EchoServiceRegistry.withClearedForTests(() -> {
         int[] pending = {2};
         EchoCoreServices.registerTerminalRewardService(new TerminalRewardService() {
            @Override
            public boolean storeRewards(ServerPlayer serverPlayer, String missionId, List<ItemStack> rewards) {
               return false;
            }

            @Override
            public boolean claimRewards(ServerPlayer serverPlayer) {
               if (pending[0] <= 0) {
                  return false;
               }
               pending[0] = 0;
               return true;
            }

            @Override
            public int pendingRewardCount(Player rewardPlayer) {
               return pending[0];
            }
         });
         ServerPlayer serverPlayer = makeServerPlayer(helper);
         serverPlayer.setPos(helper.absolutePos(lockerPos.above()).getCenter());
         helper.assertFalse(LogisticsNetworkService.claimRelayRewards(serverPlayer, null),
            "Reward relay claiming should be guarded when the relay block is offline");
         helper.assertTrue(pending[0] == 2, "Offline relay claim should not mutate pending rewards");
         helper.setBlock(relayPos, (Block)ModBlocks.REMOTE_REWARD_RELAY.get());
         LogisticsBlockEntity relay = helper.getBlockEntity(relayPos, LogisticsBlockEntity.class);
         relay.setNetworkId("core-layer7");
         helper.assertTrue(LogisticsNetworkService.claimRelayRewards(serverPlayer, relay),
            "Online Remote Reward Relay should claim through the shared ECHO reward service");
         helper.assertTrue(pending[0] == 0, "Relay claim should be idempotent and clear pending rewards once");
         helper.assertFalse(LogisticsNetworkService.claimRelayRewards(serverPlayer, relay),
            "Second relay claim should not claim already-transferred rewards");
         helper.getLevel().getServer().getPlayerList().remove(serverPlayer);
      });
      helper.succeed();
   }

   private static void offlineDockDiagnostics(GameTestHelper helper) {
      resetCoreServicesForLogisticsTests();
      BlockPos cratePos = new BlockPos(1, 1, 1);
      BlockPos lockerPos = new BlockPos(4, 1, 1);
      BlockPos dockPos = new BlockPos(7, 1, 1);
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      LogisticsBlockEntity locker = helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class);
      crate.setNetworkId("offline-dock");
      locker.setNetworkId("offline-dock");
      locker.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      fillBasicKit(crate);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(lockerPos.above()).getCenter());

      EchoRouteRecord blockedRoute = logisticsRoute(player, "toxic_expedition_kit");
      helper.assertTrue(blockedRoute != null && "DOCK OFFLINE".equals(blockedRoute.status()) && !blockedRoute.complete(),
         "Stocked logistics routes should remain blocked while the drone dock is offline");
      helper.assertTrue(hasLogisticsDiagnostic(player, "offline_dock"),
         "Offline dock diagnostic should appear when a stocked network has no Drone Delivery Dock");

      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("offline-dock");
      EchoRouteRecord readyRoute = logisticsRoute(player, "toxic_expedition_kit");
      helper.assertTrue(readyRoute != null && "READY".equals(readyRoute.status()) && readyRoute.complete(),
         "Adding an online Drone Delivery Dock should refresh the route to READY");
      helper.assertFalse(hasLogisticsDiagnostic(player, "offline_dock"),
         "Offline dock diagnostic should disappear after a same-network dock comes online");
      resetCoreServicesForLogisticsTests();
      helper.succeed();
   }

   private static void relayIdempotencyEdges(GameTestHelper helper) {
      BlockPos cratePos = new BlockPos(1, 1, 1);
      BlockPos relayPos = new BlockPos(3, 1, 1);
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(relayPos, (Block)ModBlocks.REMOTE_REWARD_RELAY.get());
      LogisticsBlockEntity wrongBlock = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      LogisticsBlockEntity relay = helper.getBlockEntity(relayPos, LogisticsBlockEntity.class);
      EchoServiceRegistry.withClearedForTests(() -> {
         int[] pending = {3};
         int[] claims = {0};
         EchoCoreServices.registerTerminalRewardService(new TerminalRewardService() {
            @Override
            public boolean storeRewards(ServerPlayer serverPlayer, String missionId, List<ItemStack> rewards) {
               return false;
            }

            @Override
            public boolean claimRewards(ServerPlayer serverPlayer) {
               claims[0]++;
               if (pending[0] <= 0) {
                  return false;
               }
               pending[0] = 0;
               return true;
            }

            @Override
            public int pendingRewardCount(Player rewardPlayer) {
               return pending[0];
            }
         });
         ServerPlayer player = makeServerPlayer(helper);
         helper.assertFalse(LogisticsNetworkService.claimRelayRewards(player, null),
            "Null relay claims should be rejected before calling the reward claim service");
         helper.assertFalse(LogisticsNetworkService.claimRelayRewards(player, wrongBlock),
            "Non-relay logistics blocks should not be accepted as reward relays");
         helper.assertTrue(pending[0] == 3 && claims[0] == 0,
            "Rejected relay claims must not mutate pending reward state");
         helper.assertTrue(LogisticsNetworkService.claimRelayRewards(player, relay),
            "Online Remote Reward Relay should claim pending rewards exactly once");
         helper.assertTrue(pending[0] == 0 && claims[0] == 1,
            "Successful relay claim should clear pending rewards with one service claim");
         helper.assertFalse(LogisticsNetworkService.claimRelayRewards(player, relay),
            "Second relay claim should be idempotent when no rewards remain");
         helper.assertTrue(claims[0] == 1, "No-pending relay claim should not call the reward claim service again");
         helper.getLevel().getServer().getPlayerList().remove(player);
      });
      helper.succeed();
   }

   private static void industrialDuctReach(GameTestHelper helper) {
      Block duct = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", "scrap_duct"));
      helper.assertTrue(duct != Blocks.AIR, "Industrial Nexus Scrap Duct should be present for duct reach integration");
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos ductA = new BlockPos(2, 1, 1);
      BlockPos ductB = new BlockPos(3, 1, 1);
      BlockPos ductC = new BlockPos(4, 1, 1);
      BlockPos chestPos = new BlockPos(5, 1, 1);
      BlockPos lockerPos = new BlockPos(1, 1, 3);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(ductA, duct);
      helper.setBlock(ductB, duct);
      helper.setBlock(ductC, duct);
      helper.setBlock(chestPos, Blocks.CHEST);
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("duct-reach");
      helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class).setNetworkId("duct-reach");
      net.minecraft.world.level.block.entity.BlockEntity chestEntity = helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
      helper.assertTrue(chestEntity instanceof Container, "Duct reach test chest should expose an inventory");
      Container chest = (Container)chestEntity;
      fillBasicKit(chest);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(dockPos.above()).getCenter());
      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(dockPos), "duct-reach", player);
      helper.assertTrue(snapshot.loadoutReadiness().stream().anyMatch(row -> row.presetId().equals(id("toxic_expedition_kit")) && row.ready()),
         "Connected Industrial item ducts should extend Logistics stock counting to remote inventories");
      boolean requested = LogisticsNetworkService.requestLoadout(player, helper.absolutePos(dockPos), helper.absolutePos(lockerPos), "echologisticsnetwork:toxic_expedition_kit");
      helper.assertTrue(requested, "Connected Industrial item ducts should participate in loadout reservation/extraction");
      helper.assertTrue(countItem(chest, Items.POTION) < 3, "Duct-reached source inventory should be extracted safely before dispatch");
      helper.succeed();
   }

   private static void industrialDuctGraphLimit(GameTestHelper helper) {
      Block duct = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", "scrap_duct"));
      helper.assertTrue(duct != Blocks.AIR, "Industrial Nexus Scrap Duct should be present for duct graph limit coverage");
      BlockPos dockPos = new BlockPos(1, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("duct-limit");
      for (int y = 1; y <= 100; y++) {
         helper.setBlock(new BlockPos(2, y, 1), duct);
      }
      BlockPos reachableChestPos = new BlockPos(3, 10, 1);
      BlockPos beyondLimitChestPos = new BlockPos(3, 100, 1);
      helper.setBlock(reachableChestPos, Blocks.CHEST);
      helper.setBlock(beyondLimitChestPos, Blocks.CHEST);
      Container reachableChest = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(reachableChestPos));
      Container beyondLimitChest = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(beyondLimitChestPos));
      reachableChest.setItem(0, new ItemStack(Items.POTION));
      reachableChest.setItem(1, new ItemStack(Items.POTION));
      reachableChest.setItem(2, new ItemStack(Items.POTION));
      for (int slot = 0; slot < 7; slot++) {
         beyondLimitChest.setItem(slot, new ItemStack(Items.POTION));
      }
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(dockPos.above()).getCenter());

      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(dockPos), "duct-limit", player);
      helper.assertTrue(stockCount(snapshot, "water") == 3,
         "Industrial duct reach should be bounded so inventories beyond the graph limit are not counted; counted " + stockCount(snapshot, "water"));
      helper.succeed();
   }

   private static void industrialMachineExternalEndpoint(GameTestHelper helper) {
      Block machineBlock = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", "water_purifier"));
      helper.assertTrue(machineBlock != Blocks.AIR, "Industrial Water Purifier should be present for machine endpoint integration");
      LogisticsIndustrialMachineEndpointProvider.register();

      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos machinePos = new BlockPos(4, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(machinePos, machineBlock);
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("industrial-machine-endpoint");
      net.minecraft.world.level.block.entity.BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(machinePos));
      helper.assertTrue(blockEntity instanceof Container, "Industrial machine endpoint should expose a container inventory");
      Container machine = (Container)blockEntity;
      machine.setItem(0, new ItemStack(Items.POTION));
      machine.setItem(1, new ItemStack(Items.POTION));

      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      LogisticsNetworkService.invalidateSnapshots();
      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(
         helper.getLevel(),
         helper.absolutePos(dockPos),
         "industrial-machine-endpoint",
         player,
         true
      );
      helper.assertTrue(snapshot.blockCount() == 1, "Industrial machine endpoint scan should keep normal Logistics block counting stable");
      helper.assertTrue(snapshot.endpointCount() >= 1, "Industrial machine endpoint should be counted as a Logistics delivery endpoint");
      helper.assertTrue(stockCount(snapshot, "water") == 2,
         "Industrial machine endpoint should contribute stocked machine inventory to Logistics water counts");
      boolean accepted = LogisticsNetworkService.depositPayload(
         helper.getLevel(),
         helper.absolutePos(dockPos),
         "industrial-machine-endpoint",
         List.of(new ItemStack(Items.BREAD))
      );
      helper.assertTrue(accepted && countItem(machine, Items.BREAD) == 1,
         "Industrial machine endpoint should accept Logistics payload deposits through the shared container contract");
      helper.succeed();
   }

   private static void machineCoreLegacyEndpointDedupe(GameTestHelper helper) {
      Block machineBlock = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", "water_purifier"));
      helper.assertTrue(machineBlock != Blocks.AIR, "Industrial Water Purifier should be present for MachineCore endpoint dedupe");
      IndustrialMachineCoreRuntimeProvider.register();
      MachineCoreLogisticsEndpointProvider.register();
      LogisticsIndustrialMachineEndpointProvider.register();

      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos machinePos = new BlockPos(4, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(machinePos, machineBlock);
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("machinecore-dedupe");
      net.minecraft.world.level.block.entity.BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(machinePos));
      helper.assertTrue(blockEntity instanceof Container, "MachineCore endpoint dedupe should use a real container-backed machine");
      Container machine = (Container)blockEntity;
      machine.setItem(0, new ItemStack(Items.POTION));
      machine.setItem(1, new ItemStack(Items.POTION));

      List<LogisticsExternalEndpoint> machineCoreEndpoints = LogisticsNetworkService.externalEndpointsFromProvider(
         helper.getLevel(),
         helper.absolutePos(dockPos),
         "machinecore-dedupe",
         Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "machinecore_machine_endpoints")
      );
      List<LogisticsExternalEndpoint> legacyEndpoints = LogisticsNetworkService.externalEndpointsFromProvider(
         helper.getLevel(),
         helper.absolutePos(dockPos),
         "machinecore-dedupe",
         Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "industrial_machine_endpoints")
      );
      helper.assertTrue(machineCoreEndpoints.size() == 1,
         "MachineCore Logistics provider should see the Industrial machine through the neutral runtime snapshot");
      helper.assertTrue(legacyEndpoints.size() == 1,
         "Legacy Industrial Logistics provider should remain callable for compatibility diagnostics");

      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      LogisticsNetworkService.invalidateSnapshots();
      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(
         helper.getLevel(),
         helper.absolutePos(dockPos),
         "machinecore-dedupe",
         player,
         true
      );
      helper.assertTrue(snapshot.blockCount() == 1, "MachineCore endpoint dedupe should keep normal Logistics block counting stable");
      helper.assertTrue(snapshot.endpointCount() == 1,
         "Shared Logistics snapshots should count a MachineCore-published machine once when the legacy Industrial provider also sees it; counted "
            + snapshot.endpointCount());
      helper.assertTrue(stockCount(snapshot, "water") == 2,
         "Deduped endpoint scans should count the machine inventory once as water stock");
      boolean accepted = LogisticsNetworkService.depositPayload(
         helper.getLevel(),
         helper.absolutePos(dockPos),
         "machinecore-dedupe",
         List.of(new ItemStack(Items.BREAD))
      );
      helper.assertTrue(accepted && countItem(machine, Items.BREAD) == 1,
         "Deduped MachineCore endpoint should still accept Logistics payload deposits through the shared container contract");
      helper.succeed();
   }

   private static void factoryLoadoutToIndustrialInputDepot(GameTestHelper helper) {
      Block inputDepotBlock = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", "input_depot_crate"));
      helper.assertTrue(inputDepotBlock != Blocks.AIR, "Industrial input depot should be present for factory loadout integration");
      Identifier presetId = id("factory_apple_test");
      LogisticsContent.replaceJsonContent(new LogisticsContent.LoadedContent(Map.of(), Map.of(presetId, new LoadoutPreset(
         presetId,
         "Factory Apple Test",
         700,
         Identifier.withDefaultNamespace("apple"),
         List.of(new LoadoutRequirement(LoadoutRequirement.Kind.ITEM, Identifier.withDefaultNamespace("apple"), 1, false)),
         List.of(Identifier.fromNamespaceAndPath("echoindustrialnexus", "input_depot_crate")),
         40
      )), Map.of()));

      BlockPos origin = new BlockPos(5, 1, 1);
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos sourcePos = new BlockPos(2, 1, 1);
      BlockPos labelPos = new BlockPos(5, 1, 2);
      BlockPos depotPos = new BlockPos(5, 1, 3);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(sourcePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(labelPos, (Block)ModBlocks.SMART_STORAGE_LABEL.get());
      helper.setBlock(depotPos, inputDepotBlock);
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("factory-test");
      helper.getBlockEntity(sourcePos, LogisticsBlockEntity.class).setNetworkId("factory-test");
      helper.getBlockEntity(labelPos, LogisticsBlockEntity.class).setNetworkId("factory-test");
      LogisticsBlockEntity source = helper.getBlockEntity(sourcePos, LogisticsBlockEntity.class);
      source.setItem(0, new ItemStack(Items.APPLE, 1));
      ServerPlayer player = makeServerPlayer(helper);
      try {
         player.setPos(helper.absolutePos(origin).getCenter());
         LogisticsNetworkService.FactoryDispatchResult result =
            LogisticsNetworkService.requestFactoryLoadout(player, helper.absolutePos(origin), presetId.toString());
         helper.assertTrue(result.dispatched(), "Factory bridge should dispatch a loadout to the connected Industrial input depot: " + result.message());
         helper.assertTrue(result.targetPos().equals(helper.absolutePos(depotPos)), "Factory bridge should choose the Industrial input depot as delivery target");
         helper.assertTrue(countItem(source, Items.APPLE) == 0, "Factory bridge should reserve source stock before dispatch");
      } finally {
         LogisticsContent.clearJsonForTests();
         helper.getLevel().getServer().getPlayerList().remove(player);
      }
      helper.succeed();
   }

   private static void factoryAutoRestockToIndustrialInputDepot(GameTestHelper helper) {
      Block inputDepotBlock = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("echoindustrialnexus", "input_depot_crate"));
      helper.assertTrue(inputDepotBlock != Blocks.AIR, "Industrial input depot should be present for factory auto-restock integration");
      Identifier presetId = id("factory_restock_apple_test");
      LogisticsContent.replaceJsonContent(new LogisticsContent.LoadedContent(Map.of(), Map.of(presetId, new LoadoutPreset(
         presetId,
         "Factory Restock Apple Test",
         710,
         Identifier.withDefaultNamespace("apple"),
         List.of(new LoadoutRequirement(LoadoutRequirement.Kind.ITEM, Identifier.withDefaultNamespace("apple"), 1, false)),
         List.of(Identifier.fromNamespaceAndPath("echoindustrialnexus", "input_depot_crate")),
         40,
          new FactoryRestockPolicy(Identifier.fromNamespaceAndPath("echoindustrialnexus", "weld_reinforced_machine_frame"), 3, 2, 1, 120)
      )), Map.of()));

      BlockPos origin = new BlockPos(5, 1, 1);
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos sourcePos = new BlockPos(2, 1, 1);
      BlockPos labelPos = new BlockPos(5, 1, 2);
      BlockPos depotPos = new BlockPos(5, 1, 3);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(sourcePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(labelPos, (Block)ModBlocks.SMART_STORAGE_LABEL.get());
      helper.setBlock(depotPos, inputDepotBlock);
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("factory-restock-test");
      helper.getBlockEntity(labelPos, LogisticsBlockEntity.class).setNetworkId("factory-restock-test");
      LogisticsBlockEntity source = helper.getBlockEntity(sourcePos, LogisticsBlockEntity.class);
      source.setNetworkId("factory-restock-test");
      source.setItem(0, new ItemStack(Items.APPLE, 2));
      ServerPlayer player = makeServerPlayer(helper);
      try {
         player.setPos(helper.absolutePos(origin).getCenter());
         LogisticsNetworkService.FactoryRestockStatus status =
            LogisticsNetworkService.requestFactoryAutoRestock(helper.getLevel(), player.getUUID(), player,
               helper.absolutePos(origin), "factory-restock-test", presetId.toString());
         helper.assertTrue(status.dispatched(), "Factory auto-restock should dispatch to the Industrial input depot: " + status.message());
         helper.assertTrue(status.targetPos().equals(helper.absolutePos(depotPos)), "Factory auto-restock should choose the Industrial input depot as delivery target");
         helper.assertTrue(countItem(source, Items.APPLE) == 1, "Factory auto-restock should reserve one loadout run before dispatch");
         LogisticsNetworkService.FactoryRestockStatus duplicate =
            LogisticsNetworkService.requestFactoryAutoRestock(helper.getLevel(), player.getUUID(), player,
               helper.absolutePos(origin), "factory-restock-test", presetId.toString());
         helper.assertTrue(!duplicate.dispatched() && duplicate.message().contains("in-flight"),
            "Factory auto-restock should respect the in-flight cap instead of duplicate-dispatching: " + duplicate.message());
      } finally {
         LogisticsContent.clearJsonForTests();
         helper.getLevel().getServer().getPlayerList().remove(player);
      }
      helper.succeed();
   }

   private static void terminalRouteRefresh(GameTestHelper helper) {
      resetCoreServicesForLogisticsTests();
      registerRouteDiscoveryProviderForTests();
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos requesterPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(requesterPos, (Block)ModBlocks.ROUTE_REQUESTER.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("terminal-refresh");
      LogisticsBlockEntity requester = helper.getBlockEntity(requesterPos, LogisticsBlockEntity.class);
      requester.setNetworkId("terminal-refresh");
      requester.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("terminal-refresh");
      fillBasicKit(crate);
      crate.setItem(6, new ItemStack(Items.POTION));
      crate.setItem(7, new ItemStack(Items.POTION));
      crate.setItem(8, new ItemStack(Items.POTION));
      crate.setItem(9, new ItemStack(Items.APPLE, 3));
      crate.setItem(10, new ItemStack(Items.PAPER, 2));
      crate.setItem(11, new ItemStack(Items.GOLDEN_APPLE));

      TerminalActionRegistry.withClearedForTests(() -> {
         LogisticsTerminalCommonIntegration.register();
         ServerPlayer player = makeServerPlayer(helper);
         player.setPos(helper.absolutePos(requesterPos.above()).getCenter());
         Identifier routeRecordId = LogisticsTerminalIds.id("route/toxic_expedition_kit");
         Identifier discoveryId = EchoCoreServices.routeDiscoveryId(routeRecordId);
         helper.assertFalse(EchoCoreServices.hasDiscoveredFeature(player, discoveryId),
            "Terminal route refresh test should start with an undiscovered logistics route");
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.SCAN_ACTION, ""),
            "Terminal scan action should run against the real logistics network");
         helper.assertTrue(EchoCoreServices.hasDiscoveredFeature(player, discoveryId),
            "Terminal scan should discover visible logistics route records through ECHO Core");
         helper.assertTrue(logisticsRoute(player, "toxic_expedition_kit") != null
               && "READY".equals(logisticsRoute(player, "toxic_expedition_kit").status()),
            "Terminal scan should leave a ready route visible through Core records");
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REQUEST_ACTION,
               "terminal-refresh|echologisticsnetwork:toxic_expedition_kit"),
            "Terminal request should dispatch the selected loadout");
         EchoRouteRecord inTransit = logisticsRoute(player, "toxic_expedition_kit");
         helper.assertTrue(inTransit != null && "READY / IN TRANSIT".equals(inTransit.status()),
            "Core route records should refresh to an in-transit status while a delivery is active and stock remains ready; actual="
               + (inTransit == null ? "missing" : inTransit.status()));
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.CANCEL_ACTION, ""),
            "Terminal cancel should recover active deliveries");
         EchoRouteRecord readyAgain = logisticsRoute(player, "toxic_expedition_kit");
         helper.assertTrue(readyAgain != null && "READY".equals(readyAgain.status()),
            "Core route records should refresh back to READY after terminal cancellation recovers the payload");
         helper.getLevel().getServer().getPlayerList().remove(player);
      });
      resetCoreServicesForLogisticsTests();
      helper.succeed();
   }

   private static void recoveryNoDuplication(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos lockerPos = new BlockPos(5, 1, 1);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(lockerPos, (Block)ModBlocks.LOADOUT_LOCKER.get());
      LogisticsBlockEntity dock = helper.getBlockEntity(dockPos, LogisticsBlockEntity.class);
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      LogisticsBlockEntity locker = helper.getBlockEntity(lockerPos, LogisticsBlockEntity.class);
      dock.setNetworkId("recovery-exact");
      crate.setNetworkId("recovery-exact");
      locker.setNetworkId("recovery-exact");
      fillBasicKit(crate);
      int potionsBefore = countItemAcross(Items.POTION, crate, dock, locker);
      int applesBefore = countItemAcross(Items.APPLE, crate, dock, locker);
      int paperBefore = countItemAcross(Items.PAPER, crate, dock, locker);
      int medicineBefore = countItemAcross(Items.GOLDEN_APPLE, crate, dock, locker);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(helper.absolutePos(dockPos.above()).getCenter());

      boolean requested = LogisticsNetworkService.requestLoadout(player, helper.absolutePos(dockPos), helper.absolutePos(lockerPos), "echologisticsnetwork:toxic_expedition_kit");
      helper.assertTrue(requested, "Recovery no-duplication test should dispatch a sealed payload");
      helper.assertTrue(LogisticsNetworkService.cancelActiveDeliveries(player, helper.absolutePos(dockPos), "recovery-exact") == 1,
         "Cancelling the active delivery should recover exactly one sealed payload");
      int potionsAfter = countItemAcross(Items.POTION, crate, dock, locker);
      int applesAfter = countItemAcross(Items.APPLE, crate, dock, locker);
      int paperAfter = countItemAcross(Items.PAPER, crate, dock, locker);
      int medicineAfter = countItemAcross(Items.GOLDEN_APPLE, crate, dock, locker);
      helper.assertTrue(potionsAfter == potionsBefore
            && applesAfter == applesBefore
            && paperAfter == paperBefore
            && medicineAfter == medicineBefore,
         "Cancel recovery should preserve exact loadout item totals without duplication or loss: before="
            + totals(potionsBefore, applesBefore, paperBefore, medicineBefore)
            + ", after="
            + totals(potionsAfter, applesAfter, paperAfter, medicineAfter)
            + ", crate="
            + totals(countItem(crate, Items.POTION), countItem(crate, Items.APPLE), countItem(crate, Items.PAPER), countItem(crate, Items.GOLDEN_APPLE))
            + ", dock="
            + totals(countItem(dock, Items.POTION), countItem(dock, Items.APPLE), countItem(dock, Items.PAPER), countItem(dock, Items.GOLDEN_APPLE))
            + ", locker="
            + totals(countItem(locker, Items.POTION), countItem(locker, Items.APPLE), countItem(locker, Items.PAPER), countItem(locker, Items.GOLDEN_APPLE)));
      helper.assertTrue(countDroppedItems(helper, dockPos, Items.POTION) == 0
            && LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(dockPos), "recovery-exact", player).activeDeliveries() == 0,
         "Cancel recovery should clear the drone job and avoid unnecessary item drops when the dock has space");

      BlockPos failureDockPos = new BlockPos(1, 1, 5);
      helper.setBlock(failureDockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      LogisticsBlockEntity failureDock = helper.getBlockEntity(failureDockPos, LogisticsBlockEntity.class);
      CourierDroneEntity drone = ModEntities.COURIER_DRONE.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
      helper.assertTrue(drone != null, "Courier drone should spawn for exact failure recovery");
      drone.configureDelivery(player.getUUID(), helper.absolutePos(failureDockPos), helper.absolutePos(new BlockPos(8, 1, 5)),
         id("bad_target"), List.of(new ItemStack(Items.BREAD, 3)), 40);
      drone.setPos(helper.absolutePos(new BlockPos(7, 2, 5)).getCenter());
      helper.getLevel().addFreshEntity(drone);
      for (int i = 0; i < 170; i++) {
         drone.tick();
      }
      int recoveredBread = countItem(failureDock, Items.BREAD) + countDroppedItems(helper, failureDockPos, Items.BREAD);
      helper.assertTrue(recoveredBread == 3,
         "Failed courier recovery should make the exact payload recoverable once, either in the dock or as item drops");
      helper.succeed();
   }

   private static void terminalActions(GameTestHelper helper) {
      BlockPos dockPos = new BlockPos(1, 1, 1);
      BlockPos cratePos = new BlockPos(2, 1, 1);
      BlockPos requesterPos = new BlockPos(5, 1, 1);
      BlockPos relayPos = new BlockPos(1, 1, 4);
      BlockPos depotPos = new BlockPos(2, 1, 4);
      helper.setBlock(dockPos, (Block)ModBlocks.DRONE_DELIVERY_DOCK.get());
      helper.setBlock(cratePos, (Block)ModBlocks.SUPPLY_CRATE.get());
      helper.setBlock(requesterPos, (Block)ModBlocks.ROUTE_REQUESTER.get());
      helper.setBlock(relayPos, (Block)ModBlocks.REMOTE_REWARD_RELAY.get());
      helper.setBlock(depotPos, (Block)ModBlocks.FACTION_TRADE_DEPOT.get());
      helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).setNetworkId("terminal-loop");
      helper.getBlockEntity(relayPos, LogisticsBlockEntity.class).setNetworkId("terminal-loop");
      helper.getBlockEntity(depotPos, LogisticsBlockEntity.class).setNetworkId("terminal-loop");
      LogisticsBlockEntity requester = helper.getBlockEntity(requesterPos, LogisticsBlockEntity.class);
      requester.setNetworkId("terminal-loop");
      requester.setLoadoutId("echologisticsnetwork:toxic_expedition_kit");
      LogisticsBlockEntity crate = helper.getBlockEntity(cratePos, LogisticsBlockEntity.class);
      crate.setNetworkId("terminal-loop");
      fillBasicKit(crate);

      TerminalActionRegistry.withClearedForTests(() -> {
         LogisticsTerminalCommonIntegration.register();
         boolean handled = TerminalActionRegistry.handle(null, com.knoxhack.echologisticsnetwork.integration.LogisticsTerminalIds.LOGISTICS_TAB,
            com.knoxhack.echologisticsnetwork.integration.LogisticsTerminalIds.SCAN_ACTION, "");
         helper.assertTrue(handled, "Logistics terminal scan action should be registered");
         ServerPlayer player = makeServerPlayer(helper);
         player.setPos(helper.absolutePos(requesterPos.above()).getCenter());
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.SCAN_ACTION, ""),
            "Logistics terminal scan should read a nearby logistics network");
         LogisticsNetworkService.LogisticsSnapshot dashboard = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "terminal-loop", player);
         helper.assertTrue(dashboard.blockCount() == 5 && dashboard.endpointCount() == 1 && dashboard.dockOnline()
               && dashboard.relayOnline() && dashboard.depotOnline() && dashboard.canDispatch(),
            "Terminal action loop should start from a complete shared dashboard snapshot");
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REFRESH_OFFERS_ACTION, ""),
            "Logistics terminal offer refresh action should be handled when the depot is online");
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REQUEST_ACTION,
               "terminal-loop|echologisticsnetwork:toxic_expedition_kit"),
            "Logistics terminal request action should be handled");
         LogisticsNetworkService.LogisticsSnapshot active = LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "terminal-loop", player);
         helper.assertTrue(active.activeDeliveries() == 1, "Terminal request action should dispatch a tracked courier delivery");
         helper.assertTrue(helper.getBlockEntity(dockPos, LogisticsBlockEntity.class).handleMenuButton(player, LogisticsMenu.BUTTON_CANCEL_DELIVERIES),
            "Block dashboard cancel action should be handled");
         helper.assertTrue(LogisticsNetworkService.snapshot(helper.getLevel(), helper.absolutePos(requesterPos), "terminal-loop", player).activeDeliveries() == 0,
            "Block dashboard cancel action should recover the active terminal delivery");
         fillBasicKit(crate);
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REQUEST_ACTION,
               "terminal-loop|echologisticsnetwork:toxic_expedition_kit"),
            "Logistics terminal request action should dispatch again after block cancellation");
         helper.assertTrue(TerminalActionRegistry.handle(player, LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.CANCEL_ACTION, ""),
            "Logistics terminal cancel action should be handled");
         helper.assertTrue(countItem(helper.getBlockEntity(dockPos, LogisticsBlockEntity.class), Items.POTION) > 0,
            "Terminal cancel action should recover the sealed payload to the dock");
         helper.getLevel().getServer().getPlayerList().remove(player);
      });
      helper.succeed();
   }

   private static EchoRouteRecord logisticsRoute(Player player, String path) {
      Identifier routeId = LogisticsTerminalIds.id("route/" + path);
      return EchoCoreServices.routeRecords(player).stream()
         .filter(record -> LogisticsCoreIntegration.CHAPTER_ID.equals(record.chapterId()) && record.id().equals(routeId))
         .findFirst()
         .orElse(null);
   }

   private static boolean hasLogisticsDiagnostic(Player player, String path) {
      Identifier diagnosticId = LogisticsTerminalIds.id("diagnostic/" + path);
      return EchoCoreServices.diagnostics(player).stream()
         .anyMatch(blocker -> LogisticsCoreIntegration.CHAPTER_ID.equals(blocker.chapterId()) && blocker.id().equals(diagnosticId));
   }

   private static void resetCoreServicesForLogisticsTests() {
      EchoCoreServices.clearPlatformServicesForTests();
      LogisticsCoreIntegration.registerAddonChapter();
   }

   private static void registerRouteDiscoveryProviderForTests() {
      EchoCoreServices.registerDiscoveryProvider(new EchoDiscoveryProvider() {
         @Override
         public List<EchoDiscoveryEntry> entries(Player player) {
            return EchoCoreServices.routeRecords(player).stream()
               .map(record -> new EchoDiscoveryEntry(
                  EchoCoreServices.routeDiscoveryId(record.id()),
                  id("logistics_network"),
                  EchoDiscoveryCategory.STRUCTURE,
                  record.title(),
                  "Unmapped Logistics Signal",
                  "Scan an online logistics network to reveal this route record.",
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

   private static ServerPlayer makeServerPlayer(GameTestHelper helper) {
      CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
      ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
         @Override
         public GameType gameMode() {
            return GameType.CREATIVE;
         }
      };
      Connection connection = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(connection);
      helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      return player;
   }

   private static void fillBasicKit(Container container) {
      container.setItem(0, new ItemStack(Items.POTION));
      container.setItem(1, new ItemStack(Items.POTION));
      container.setItem(2, new ItemStack(Items.POTION));
      container.setItem(3, new ItemStack(Items.APPLE, 4));
      container.setItem(4, new ItemStack(Items.PAPER, 4));
      container.setItem(5, new ItemStack(Items.GOLDEN_APPLE, 2));
   }

   private static int stockCount(LogisticsNetworkService.LogisticsSnapshot snapshot, String categoryPath) {
      return snapshot.stockRows().stream()
         .filter(row -> row.categoryId().getPath().equals(categoryPath))
         .findFirst()
         .map(LogisticsNetworkService.StockRow::count)
         .orElse(0);
   }

   private static int countItem(Container container, Item item) {
      int count = 0;
      for (int slot = 0; slot < container.getContainerSize(); slot++) {
         ItemStack stack = container.getItem(slot);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private static int countPayload(LogisticsNetworkService.DeliveryJob job, Item item) {
      int count = 0;
      for (ItemStack stack : job.payload()) {
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private static int countItemAcross(Item item, Container... containers) {
      int count = 0;
      for (Container container : containers) {
         count += countItem(container, item);
      }
      return count;
   }

   private static String totals(int potions, int apples, int paper, int medicine) {
      return "potion " + potions + ", apple " + apples + ", paper " + paper + ", golden_apple " + medicine;
   }

   private static int countDroppedItems(GameTestHelper helper, BlockPos localOrigin, Item item) {
      int count = 0;
      for (net.minecraft.world.entity.item.ItemEntity entity : helper.getLevel().getEntitiesOfClass(
         net.minecraft.world.entity.item.ItemEntity.class,
         new net.minecraft.world.phys.AABB(helper.absolutePos(localOrigin)).inflate(3.0D))) {
         if (entity.getItem().is(item)) {
            count += entity.getItem().getCount();
         }
      }
      return count;
   }

   private static void tickNetworkDrones(GameTestHelper helper, BlockPos localOrigin, String networkId) {
      for (CourierDroneEntity drone : helper.getLevel().getEntitiesOfClass(
         CourierDroneEntity.class,
         new net.minecraft.world.phys.AABB(helper.absolutePos(localOrigin)).inflate(16.0D))) {
         if (networkId.equals(drone.networkId())) {
            drone.tick();
         }
      }
   }

   private static void assertJsonParseFails(GameTestHelper helper, Runnable parse, String message) {
      try {
         parse.run();
         helper.fail(message);
      } catch (JsonParseException expected) {
         // Expected validation failure.
      }
   }

   private static void missionCoreContentRegistration(GameTestHelper helper) {
      InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
      LogisticsMissionCoreIntegration.registerContent(registry);
      helper.assertTrue(registry.chapter(id("logistics")).isPresent(), "Logistics MissionCore chapter should be owned by Logistics.");
      assertMission(helper, registry, "network_online", "route", MissionObjectiveType.ESTABLISH_ROUTE);
      assertMission(helper, registry, "label_supplies", "label", MissionObjectiveType.CUSTOM);
      assertMission(helper, registry, "request_loadout", "request", MissionObjectiveType.ESTABLISH_ROUTE);
      assertMission(helper, registry, "courier_delivery", "deliver", MissionObjectiveType.DELIVER_ITEM);
      assertMission(helper, registry, "depot_exchange", "exchange", MissionObjectiveType.DELIVER_ITEM);
      assertMission(helper, registry, "industrial_auto_restock", "restock", MissionObjectiveType.ESTABLISH_ROUTE);
      helper.succeed();
   }

   private static void assertMission(
      GameTestHelper helper,
      InMemoryMissionRegistry registry,
      String missionPath,
      String objectiveKey,
      MissionObjectiveType type
   ) {
      Identifier missionId = id(missionPath);
      MissionDefinition mission = registry.missionDefinition(missionId)
         .orElseThrow(() -> new AssertionError("Missing MissionCore mission: " + missionId));
      helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "Logistics MissionCore missions should be side ops.");
      helper.assertTrue(!mission.rewards().isEmpty(), "Logistics MissionCore mission should have a claimable reward: " + missionId);
      helper.assertTrue(mission.objectives().size() == 1, "Logistics MissionCore mission should have one direct objective: " + missionId);
      helper.assertTrue(mission.objectives().getFirst().type() == type, "Logistics objective type should stay stable: " + missionId);
      String target = mission.objectives().getFirst().criteria().get("target");
      helper.assertTrue(MissionHookTargets.objectiveTarget(EchoLogisticsNetwork.MODID, missionId, objectiveKey).toString().equals(target),
         "Logistics MissionCore objective target should use MissionHookTargets: " + missionId);
   }

   private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("logistics_network_" + testName));
      register(event, environment, testName, functionId);
   }

   private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId, int maxTicks) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("logistics_network_" + testName));
      register(event, environment, testName, functionId, maxTicks);
   }

   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
      register(event, environment, testName, functionId, 400);
   }

   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId, int maxTicks) {
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
         environment, Identifier.withDefaultNamespace("empty"), maxTicks, 0, true, Rotation.NONE, false, 1, 1, false, TEST_PADDING
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, path);
   }

   private static boolean shouldRegisterTests() {
      String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
      if (namespaces == null || namespaces.isBlank()) {
         return true;
      }
      for (String namespace : namespaces.split(",")) {
         String normalized = namespace.trim();
         if (normalized.equals(EchoLogisticsNetwork.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
            return true;
         }
      }
      return false;
   }
}
