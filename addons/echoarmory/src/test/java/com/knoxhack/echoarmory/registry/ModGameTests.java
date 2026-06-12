package com.knoxhack.echoarmory.registry;

import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.InMemoryMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.api.ArmoryReports;
import com.knoxhack.echoarmory.api.StationOperationPreview;
import com.knoxhack.echoarmory.block.entity.ArmoryStationBlockEntity;
import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.ArmoryJsonReloadListener;
import com.knoxhack.echoarmory.content.ArmoryLoadoutDefinition;
import com.knoxhack.echoarmory.content.BossRecommendationDefinition;
import com.knoxhack.echoarmory.content.FiringModeDefinition;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.content.RouteProfileDefinition;
import com.knoxhack.echoarmory.content.StationRecipeDefinition;
import com.knoxhack.echoarmory.data.ArmoryLoadout;
import com.knoxhack.echoarmory.data.ArmoryStance;
import com.knoxhack.echoarmory.data.CosmeticTrim;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.EquipmentTier;
import com.knoxhack.echoarmory.data.InstabilityState;
import com.knoxhack.echoarmory.data.InstalledModules;
import com.knoxhack.echoarmory.integration.ArmoryCoreIntegration;
import com.knoxhack.echoarmory.integration.ArmoryMissionCoreIntegration;
import com.knoxhack.echoarmory.integration.ArmoryTerminalCommonIntegration;
import com.knoxhack.echoarmory.integration.ArmoryTerminalIds;
import com.knoxhack.echoarmory.entity.ArmoryProjectileEntity;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.menu.ArmoryStationMenu;
import com.knoxhack.echoarmory.service.ArmoryReadinessService;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, EchoArmory.MODID);
   private static final int TEST_PADDING = 24;

   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODULE_REGISTRATION =
      TEST_FUNCTIONS.register("module_registration", () -> ModGameTests::moduleRegistration);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATA_COMPONENTS =
      TEST_FUNCTIONS.register("data_components", () -> ModGameTests::dataComponents);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATA_PARSERS =
      TEST_FUNCTIONS.register("data_parsers", () -> ModGameTests::dataParsers);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODULE_INSTALL =
      TEST_FUNCTIONS.register("module_install_swap", () -> ModGameTests::moduleInstall);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OVERLOAD_AND_PROTECTION =
      TEST_FUNCTIONS.register("overload_and_protection", () -> ModGameTests::overloadAndProtection);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_WIRING =
      TEST_FUNCTIONS.register("core_wiring", () -> ModGameTests::coreWiring);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ACTIONS =
      TEST_FUNCTIONS.register("terminal_actions", () -> ModGameTests::terminalActions);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INSTALL_SAFETY =
      TEST_FUNCTIONS.register("install_safety", () -> ModGameTests::installSafety);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RANGED_RESOURCE_SAFETY =
      TEST_FUNCTIONS.register("ranged_resource_safety", () -> ModGameTests::rangedResourceSafety);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECHARGE_COSTS_FUEL =
      TEST_FUNCTIONS.register("recharge_costs_fuel", () -> ModGameTests::rechargeCostsFuel);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTION_LOCK_SAFETY =
      TEST_FUNCTIONS.register("faction_lock_safety", () -> ModGameTests::factionLockSafety);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STACK_COMPONENT_PERSISTENCE =
      TEST_FUNCTIONS.register("stack_component_round_trip_persists_armory_state", () -> ModGameTests::stackComponentRoundTripPersistsArmoryState);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATION_INVENTORY_PERSISTENCE =
      TEST_FUNCTIONS.register("station_inventory_round_trip_persists_items_and_gear_state", () -> ModGameTests::stationInventoryRoundTripPersistsItemsAndGearState);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATION_SLOT_SAFETY =
      TEST_FUNCTIONS.register("station_slot_and_transfer_safety", () -> ModGameTests::stationSlotAndTransferSafety);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SELECTED_ACTIONS =
      TEST_FUNCTIONS.register("terminal_selected_action_paths", () -> ModGameTests::terminalSelectedActionPaths);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
      TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REQUIRED_PROTECTION_PARSER =
      TEST_FUNCTIONS.register("required_protection_parser", () -> ModGameTests::requiredProtectionParser);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOADOUT_READINESS_STATES =
      TEST_FUNCTIONS.register("loadout_readiness_states", () -> ModGameTests::loadoutReadinessStates);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTENT_V2_CONTRACTS =
      TEST_FUNCTIONS.register("content_v2_contracts", () -> ModGameTests::contentV2Contracts);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> READINESS_REPORT_V13 =
      TEST_FUNCTIONS.register("readiness_report_v13", () -> ModGameTests::readinessReportV13);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATION_PREVIEW_CONTRACT =
      TEST_FUNCTIONS.register("station_preview_contract", () -> ModGameTests::stationPreviewContract);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STAGED_RACK_READINESS =
      TEST_FUNCTIONS.register("staged_rack_readiness", () -> ModGameTests::stagedRackReadiness);

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
      register(event, "data_components", DATA_COMPONENTS.getId());
      register(event, "data_parsers", DATA_PARSERS.getId());
      register(event, "module_install_swap", MODULE_INSTALL.getId());
      register(event, "overload_and_protection", OVERLOAD_AND_PROTECTION.getId());
      register(event, "core_wiring", CORE_WIRING.getId());
      register(event, "terminal_actions", TERMINAL_ACTIONS.getId());
      register(event, "install_safety", INSTALL_SAFETY.getId());
      register(event, "ranged_resource_safety", RANGED_RESOURCE_SAFETY.getId());
      register(event, "recharge_costs_fuel", RECHARGE_COSTS_FUEL.getId());
      register(event, "faction_lock_safety", FACTION_LOCK_SAFETY.getId());
      register(event, "stack_component_round_trip_persists_armory_state", STACK_COMPONENT_PERSISTENCE.getId());
      register(event, "station_inventory_round_trip_persists_items_and_gear_state", STATION_INVENTORY_PERSISTENCE.getId());
      register(event, "station_slot_and_transfer_safety", STATION_SLOT_SAFETY.getId());
      register(event, "terminal_selected_action_paths", TERMINAL_SELECTED_ACTIONS.getId());
      register(event, "missioncore_content_registration", MISSION_CORE_CONTENT.getId());
      register(event, "required_protection_parser", REQUIRED_PROTECTION_PARSER.getId());
      register(event, "loadout_readiness_states", LOADOUT_READINESS_STATES.getId());
      register(event, "content_v2_contracts", CONTENT_V2_CONTRACTS.getId());
      register(event, "readiness_report_v13", READINESS_REPORT_V13.getId());
      register(event, "station_preview_contract", STATION_PREVIEW_CONTRACT.getId());
      register(event, "staged_rack_readiness", STAGED_RACK_READINESS.getId());
   }

   private static void moduleRegistration(GameTestHelper helper) {
      helper.assertTrue(ModBlocks.ARMORY_BENCH.get() != Blocks.AIR, "Armory Bench should be registered");
      helper.assertTrue(ModBlocks.CONSTRUCT_DOCK.get() != Blocks.AIR, "Construct Dock should be registered");
      helper.assertTrue(ModItems.FROST_BLADE.get() != Items.AIR, "Frost Blade should be registered");
      helper.assertTrue(ModItems.AMMO_CRYSTALS.get() != Items.AIR, "Ammo Crystals should be registered");
      helper.assertTrue(ModDataComponents.INSTALLED_MODULES.get() != null, "Installed module component should be registered");
      helper.succeed();
   }

   private static void dataComponents(GameTestHelper helper) {
      ItemStack weapon = new ItemStack(ModItems.FROST_BLADE.get());
      ArmoryData.initialize(weapon);
      helper.assertTrue(ArmoryData.modules(weapon).equals(InstalledModules.EMPTY), "Gear should initialize installed modules");
      EnergyState energy = weapon.get(ModDataComponents.ENERGY_STATE.get());
      helper.assertTrue(energy != null && energy.capacity() == 180 && energy.stored() == 180, "Energy state should initialize from gear definition");
      helper.assertTrue(weapon.get(ModDataComponents.EQUIPMENT_TIER.get()).tier() == 2, "Equipment tier should initialize from gear definition");
      helper.assertTrue(weapon.get(ModDataComponents.STANCE.get()) != null, "Stance component should initialize");
      helper.succeed();
   }

   private static void dataParsers(GameTestHelper helper) {
      GearDefinition gear = ArmoryJsonReloadListener.parseGearForTests(id("parser_blade"),
         JsonParser.parseString("{\"title\":\"Parser Blade\",\"baseType\":\"melee\",\"tier\":2,\"moduleSlots\":3,\"baseDamage\":7.5,\"energyCapacity\":150,\"allowedSlots\":[\"elemental\"],\"tags\":[\"parser\"]}").getAsJsonObject());
      helper.assertTrue(gear.tier() == 2 && gear.moduleSlots() == 3 && gear.allowedSlots().contains("elemental"), "Gear parser should load modular fields");
      ModuleDefinition module = ArmoryJsonReloadListener.parseModuleForTests(id("parser_core"),
         JsonParser.parseString("{\"title\":\"Parser Core\",\"slotType\":\"elemental\",\"effectType\":\"frost\",\"damageBonus\":2.0,\"energyCost\":10,\"instability\":4,\"compatibleTypes\":[\"melee\"],\"synergyTags\":[\"frost\"]}").getAsJsonObject());
      helper.assertTrue(module.damageBonus() == 2.0F && module.compatibleTypes().contains("melee"), "Module parser should load stats and compatibility");
      ArmoryLoadoutDefinition loadout = ArmoryJsonReloadListener.parseLoadoutForTests(id("parser_kit"),
         JsonParser.parseString("{\"title\":\"Parser Kit\",\"order\":5,\"icon\":\"echoarmory:frost_blade\",\"weapon\":\"echoarmory:frost_blade\",\"armor\":[\"echoarmory:thermal_chestplate\"],\"modules\":[\"echoarmory:frost_core\"],\"minTier\":2,\"minProtection\":30,\"logisticsPreset\":\"echoarmory:parser_kit\"}").getAsJsonObject());
      helper.assertTrue(loadout.minTier() == 2 && loadout.modules().size() == 1, "Loadout parser should load readiness and module data");
      helper.assertTrue(loadout.requiredProtection(ArmoryData.ProtectionType.FRACTURE) == 30,
         "Legacy minProtection should map to fracture when requiredProtections is absent");
      helper.assertTrue(ArmoryContent.validationErrors().isEmpty(), "Bundled Armory gameplay data should validate cleanly: " + ArmoryContent.validationErrors());
      helper.succeed();
   }

   private static void requiredProtectionParser(GameTestHelper helper) {
      ArmoryLoadoutDefinition loadout = ArmoryJsonReloadListener.parseLoadoutForTests(id("hazard_parser_kit"),
         JsonParser.parseString("{\"title\":\"Hazard Parser Kit\",\"weapon\":\"echoarmory:alloy_sword\",\"requiredProtections\":{\"toxic\":55,\"heat\":40},\"minProtection\":30}").getAsJsonObject());
      helper.assertTrue(loadout.requiredProtection(ArmoryData.ProtectionType.TOXIC) == 55, "Loadout parser should load toxic protection");
      helper.assertTrue(loadout.requiredProtection(ArmoryData.ProtectionType.HEAT) == 40, "Loadout parser should load heat protection");
      helper.assertTrue(loadout.requiredProtection(ArmoryData.ProtectionType.FRACTURE) == 0,
         "Explicit requiredProtections should replace the legacy fracture fallback");
      helper.succeed();
   }

   private static void contentV2Contracts(GameTestHelper helper) {
      StationRecipeDefinition recipe = ArmoryJsonReloadListener.parseStationRecipeForTests(id("parser_station_recipe"),
         JsonParser.parseString("{\"schemaVersion\":2,\"title\":\"Parser Station\",\"stationKind\":\"weapon_forge\",\"operation\":\"upgrade_weapon\",\"gearTags\":[\"ranged\"],\"moduleTags\":[],\"auxItems\":[\"echoarmory:resonance_shard\"],\"result\":\"upgrade\",\"order\":1}").getAsJsonObject());
      helper.assertTrue(recipe.stationKind().getSerializedName().equals("weapon_forge"), "Station recipe v2 parser should load station kind");
      FiringModeDefinition mode = ArmoryJsonReloadListener.parseFiringModeForTests(id("parser_firing_mode"),
         JsonParser.parseString("{\"schemaVersion\":2,\"title\":\"Parser Bolt\",\"family\":\"veil\",\"projectileKind\":\"veil_arrow\",\"ammoCost\":1,\"energyCost\":12,\"cooldownTicks\":20,\"range\":16,\"velocity\":1.0,\"damageScale\":1.1,\"gearTags\":[\"veil\"]}").getAsJsonObject());
      helper.assertTrue(mode.projectileKind() == FiringModeDefinition.ProjectileKind.VEIL_ARROW, "Firing mode v2 parser should load projectile kind");
      RouteProfileDefinition profile = ArmoryJsonReloadListener.parseRouteProfileForTests(id("parser_route"),
         JsonParser.parseString("{\"schemaVersion\":2,\"title\":\"Parser Route\",\"routeFamily\":\"parser\",\"loadoutId\":\"echoarmory:toxic_breach_kit\",\"requiredProtections\":{\"toxic\":55},\"recommendedTags\":[\"toxic\"]}").getAsJsonObject());
      helper.assertTrue(profile.requiredProtections().getOrDefault(ArmoryData.ProtectionType.TOXIC, 0) == 55,
         "Route profile v2 parser should load protection requirements");
      boolean rejected = false;
      try {
         ArmoryJsonReloadListener.parseLoadoutForTests(id("bad_v2_loadout"),
            JsonParser.parseString("{\"schemaVersion\":2,\"title\":\"Bad Kit\",\"weapon\":\"echoarmory:alloy_sword\",\"minProtection\":30}").getAsJsonObject());
      } catch (RuntimeException expected) {
         rejected = true;
      }
      helper.assertTrue(rejected, "Loadout schemaVersion 2 should reject removed beta minProtection");
      helper.assertTrue(!ArmoryContent.firingModes().isEmpty(), "Bundled v2 firing modes should be available");
      helper.assertTrue(!ArmoryContent.stationRecipes().isEmpty(), "Bundled v2 station recipes should be available");
      helper.assertTrue(!ArmoryContent.routeProfiles().isEmpty(), "Bundled v2 route profiles should be available");
      helper.assertTrue(ArmoryContent.validationErrors().isEmpty(), "Bundled v2 Armory data should validate cleanly: " + ArmoryContent.validationErrors());
      helper.succeed();
   }

   private static void readinessReportV13(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      player.getAbilities().instabuild = false;
      ItemStack bow = new ItemStack(ModItems.VEIL_BOW.get());
      ArmoryData.initialize(bow);
      player.setItemInHand(InteractionHand.MAIN_HAND, bow);
      ArmoryLoadoutDefinition rangedRoute = new ArmoryLoadoutDefinition(
         id("test_veil_route"),
         "Test Veil Route",
         0,
         id("veil_bow"),
         "echoarmory:veil_bow",
         java.util.List.of(),
         java.util.List.of(),
         2,
         0,
         Map.of(),
         ""
      );
      ArmoryReadinessService.Report fallback = ArmoryReadinessService.report(player, rangedRoute);
      helper.assertTrue(fallback.score() > 0, "Readiness report should expose a nonzero score for equipped gear");
      helper.assertTrue("ENERGY_FALLBACK".equals(fallback.ammoState()), "Ranged readiness should report energy fallback when ammo is missing");
      helper.assertTrue(!fallback.nextAction().isBlank(), "Readiness report should expose a next action");
      player.getInventory().add(new ItemStack(ModItems.AMMO_CRYSTALS.get()));
      ArmoryReadinessService.Report ammoReady = ArmoryReadinessService.report(player, rangedRoute);
      helper.assertTrue("READY".equals(ammoReady.ammoState()), "Ranged readiness should report ammo ready when crystals are present");
      helper.assertTrue(ArmoryReports.bestReadiness(player).isPresent(), "Public Armory report API should expose best readiness safely");
      helper.assertTrue(!ArmoryReports.routeProfiles().isEmpty(), "Public Armory report API should expose route profiles");
      helper.succeed();
   }

   private static void stationPreviewContract(GameTestHelper helper) {
      ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
      ItemStack frost = new ItemStack(ModItems.FROST_CORE.get());
      ArmoryData.initialize(blade);
      StationOperationPreview install = ArmoryReports.stationPreview(
         com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind.MODULE_UPGRADE_TABLE, blade, frost, ItemStack.EMPTY);
      helper.assertTrue(install.accepted() && "install_module".equals(install.operation()), "Module Table preview should accept compatible module install");
      helper.assertTrue(ArmoryData.installModule(blade, frost), "Preview fixture should install Frost Core");
      ItemStack duplicateFrost = new ItemStack(ModItems.FROST_CORE.get());
      StationOperationPreview duplicate = ArmoryReports.stationPreview(
         com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind.MODULE_UPGRADE_TABLE, blade, duplicateFrost, ItemStack.EMPTY);
      helper.assertFalse(duplicate.accepted(), "Module Table preview should reject duplicate modules before APPLY");
      ItemStack gasMask = new ItemStack(ModItems.GAS_MASK_FILTER.get());
      StationOperationPreview rejected = ArmoryReports.stationPreview(
         com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind.MODULE_UPGRADE_TABLE, blade, gasMask, ItemStack.EMPTY);
      helper.assertFalse(rejected.accepted(), "Module Table preview should reject incompatible modules without consuming them");
      helper.assertTrue(gasMask.getCount() == 1, "Preview-only rejection should not consume a caller's module stack");

      ItemStack freshRifle = new ItemStack(ModItems.ENERGY_RIFLE.get());
      StationOperationPreview freshRecharge = ArmoryReports.stationPreview(
         com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind.ENERGY_CORE_CHARGING_STATION,
         freshRifle, ItemStack.EMPTY, new ItemStack(ModItems.VEIL_CRYSTAL.get()));
      helper.assertFalse(freshRecharge.accepted(), "Fresh energy gear should preview from definition defaults instead of missing its core");
      helper.assertTrue(freshRecharge.blocker().contains("already full"), "Fresh energy gear should start with a full default core preview");

      ItemStack rifle = new ItemStack(ModItems.ENERGY_RIFLE.get());
      ArmoryData.initialize(rifle);
      helper.assertTrue(ArmoryData.spendEnergy(rifle, 80), "Preview fixture should spend energy first");
      StationOperationPreview recharge = ArmoryReports.stationPreview(
         com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind.ENERGY_CORE_CHARGING_STATION,
         rifle, ItemStack.EMPTY, new ItemStack(ModItems.VEIL_CRYSTAL.get()));
      helper.assertTrue(recharge.accepted() && "recharge".equals(recharge.operation()), "Energy station preview should accept valid fuel");
      helper.succeed();
   }

   private static void stagedRackReadiness(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      Vec3 playerPos = helper.absoluteVec(new Vec3(2.5D, 2.0D, 2.5D));
      player.setPos(playerPos.x, playerPos.y, playerPos.z);
      BlockPos rackPos = new BlockPos(3, 1, 2);
      helper.setBlock(rackPos, (Block)ModBlocks.WEAPON_RACK.get());
      ArmoryStationBlockEntity rack = helper.getBlockEntity(rackPos, ArmoryStationBlockEntity.class);
      rack.setItem(ArmoryStationBlockEntity.GEAR_SLOT, new ItemStack(ModItems.ALLOY_SWORD.get()));
      ArmoryLoadoutDefinition stagedRoute = new ArmoryLoadoutDefinition(
         id("rack_staged_route"),
         "Rack Staged Route",
         0,
         id("alloy_sword"),
         "echoarmory:alloy_sword",
         java.util.List.of(),
         java.util.List.of(),
         1,
         0,
         Map.of(),
         ""
      );
      ArmoryReadinessService.Report report = ArmoryReadinessService.report(player, stagedRoute);
      helper.assertTrue(report.state() == ArmoryReadinessService.State.STAGED, "Nearby rack gear should satisfy readiness as staged");
      helper.assertTrue(report.staged().stream().anyMatch(line -> line.contains("Alloy Sword")), "Staged report should name rack gear blocker");
      helper.succeed();
   }

   private static void loadoutReadinessStates(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      player.getAbilities().instabuild = false;
      ArmoryLoadoutDefinition toxicRoute = new ArmoryLoadoutDefinition(
         id("test_toxic_route"),
         "Test Toxic Route",
         0,
         id("gas_mask_filter"),
         "echoarmory:alloy_sword",
         java.util.List.of("echoarmory:thermal_chestplate"),
         java.util.List.of("echoarmory:gas_mask_filter"),
         1,
         0,
         Map.of(ArmoryData.ProtectionType.TOXIC, 55),
         ""
      );

      ArmoryReadinessService.Report missing = ArmoryReadinessService.report(player, toxicRoute);
      helper.assertTrue(missing.state() == ArmoryReadinessService.State.MISSING, "Empty operator should miss the toxic route kit");

      player.getInventory().add(new ItemStack(ModItems.ALLOY_SWORD.get()));
      player.getInventory().add(new ItemStack(ModItems.THERMAL_CHESTPLATE.get()));
      player.getInventory().add(new ItemStack(ModItems.GAS_MASK_FILTER.get()));
      ArmoryReadinessService.Report staged = ArmoryReadinessService.report(player, toxicRoute);
      helper.assertTrue(staged.state() == ArmoryReadinessService.State.STAGED, "Inventory-only toxic kit should be staged");

      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ALLOY_SWORD.get()));
      ItemStack chest = new ItemStack(ModItems.THERMAL_CHESTPLATE.get());
      ItemStack filter = new ItemStack(ModItems.GAS_MASK_FILTER.get());
      ArmoryData.initialize(chest);
      helper.assertTrue(ArmoryData.installModule(player, chest, filter), "Gas Mask Module should install into Thermal Chestplate");
      player.setItemSlot(EquipmentSlot.CHEST, chest);
      ArmoryReadinessService.Report ready = ArmoryReadinessService.report(player, toxicRoute);
      helper.assertTrue(ready.state() == ArmoryReadinessService.State.READY, "Equipped filtered toxic kit should be ready");

      ArmoryLoadoutDefinition fractureGuardian = ArmoryContent.loadouts().stream()
         .filter(loadout -> "fracture_guardian_kit".equals(loadout.id().getPath()))
         .findFirst()
         .orElseThrow(() -> new AssertionError("Missing bundled Fracture Guardian Kit"));
      ArmoryReadinessService.Report locked = ArmoryReadinessService.report(player, fractureGuardian);
      helper.assertTrue(locked.state() == ArmoryReadinessService.State.LOCKED, "Faction-gated guardian kit should report locked without reputation");
      helper.succeed();
   }

   private static void moduleInstall(GameTestHelper helper) {
      ItemStack weapon = new ItemStack(ModItems.FROST_BLADE.get());
      ItemStack module = new ItemStack(ModItems.FROST_CORE.get());
      ArmoryData.initialize(weapon);
      helper.assertTrue(ArmoryData.installModule(weapon, module), "Compatible Frost Core should install into Frost Blade");
      helper.assertTrue(ArmoryData.modules(weapon).contains("echoarmory:frost_core"), "Installed modules should persist module ids");
      helper.assertTrue(module.isEmpty(), "Installing a module should consume one module item");
      helper.assertTrue(ArmoryData.damageBonus(weapon) > 1.5F, "Installed modules and tier should affect damage");

      ItemStack rejected = new ItemStack(ModItems.GAS_MASK_FILTER.get());
      helper.assertFalse(ArmoryData.installModule(weapon, rejected), "Incompatible survival module should not install into melee weapon");
      helper.succeed();
   }

   private static void overloadAndProtection(GameTestHelper helper) {
      ItemStack rifle = new ItemStack(ModItems.ENERGY_RIFLE.get());
      ArmoryData.initialize(rifle);
      helper.assertTrue(ArmoryData.spendEnergy(rifle, 80), "Energy gear should spend stored energy");
      helper.assertFalse(ArmoryData.spendEnergy(rifle, 999), "Energy gear should reject over-budget attacks");
      ArmoryData.recharge(rifle);
      helper.assertTrue(rifle.get(ModDataComponents.ENERGY_STATE.get()).stored() == 320, "Energy charging should refill capacity");

      ItemStack armor = new ItemStack(ModItems.THERMAL_CHESTPLATE.get());
      ItemStack regulator = new ItemStack(ModItems.THERMAL_REGULATOR.get());
      ArmoryData.initialize(armor);
      helper.assertTrue(ArmoryData.installModule(armor, regulator), "Thermal Regulator should install into Thermal Chestplate");
      helper.assertTrue(ArmoryData.protection(armor, ArmoryData.ProtectionType.COLD) >= 40, "Armor modules should summarize cold protection");
      helper.assertTrue(ArmoryData.protection(armor, ArmoryData.ProtectionType.HEAT) >= 40, "Armor modules should summarize heat protection");
      helper.succeed();
   }

   private static void coreWiring(GameTestHelper helper) {
      EchoCoreServices.clearPlatformServicesForTests();
      ArmoryCoreIntegration.registerAddonChapter();
      helper.assertTrue(EchoCoreServices.routeRecords(null).stream()
         .anyMatch(record -> ArmoryCoreIntegration.CHAPTER_ID.equals(record.chapterId())), "Armory should publish route records through ECHO Core");
      helper.assertTrue(EchoCoreServices.diagnostics(helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE)).stream()
         .anyMatch(blocker -> ArmoryCoreIntegration.CHAPTER_ID.equals(blocker.chapterId())), "Armory should publish diagnostics through ECHO Core");
      EchoCoreServices.clearPlatformServicesForTests();
      helper.succeed();
   }

   private static void terminalActions(GameTestHelper helper) {
      TerminalActionRegistry.withClearedForTests(() -> {
         ArmoryTerminalCommonIntegration.resetForTests();
         ArmoryTerminalCommonIntegration.register();
         helper.assertTrue(TerminalActionRegistry.handle(null, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.SCAN_ACTION, ""),
            "Armory scan action should be registered");
         helper.assertTrue(TerminalActionRegistry.handle(null, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.PREVIEW_ACTION, ""),
            "Armory preview action should be registered");
         ServerPlayer player = helper.makeMockServerPlayerInLevel();
         player.getInventory().add(new ItemStack(ModItems.ALLOY_SWORD.get()));
         player.getInventory().add(new ItemStack(ModItems.THERMAL_CHESTPLATE.get()));
         player.getInventory().add(new ItemStack(ModItems.STABILITY_RUNE.get()));
         helper.assertTrue(TerminalActionRegistry.handle(player, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.EQUIP_ACTION, "toxic_breach_kit"),
            "Armory equip action should run with a player context");
         helper.assertTrue(player.getMainHandItem().is(ModItems.ALLOY_SWORD.get()), "Terminal equip should move loadout weapon into main hand");
         helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.THERMAL_CHESTPLATE.get()), "Terminal equip should move loadout armor into its slot");
         helper.assertTrue(TerminalActionRegistry.handle(player, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.INSTALL_ACTION, "echoarmory:stability_rune"),
            "Armory install action should run with a player context");
         helper.assertTrue(ArmoryData.modules(player.getMainHandItem()).contains("echoarmory:stability_rune"),
            "Terminal install should consume a compatible inventory module into equipped gear");
      });
      helper.succeed();
   }

   private static void installSafety(GameTestHelper helper) {
      ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
      ItemStack firstCore = new ItemStack(ModItems.FROST_CORE.get());
      ItemStack duplicateCore = new ItemStack(ModItems.FROST_CORE.get());
      ArmoryData.initialize(blade);
      helper.assertTrue(ArmoryData.installModule(blade, firstCore), "First Frost Core should install");
      helper.assertTrue(firstCore.isEmpty(), "Successful install should consume exactly one module");
      helper.assertFalse(ArmoryData.installModule(blade, duplicateCore), "Duplicate module should be rejected");
      helper.assertTrue(duplicateCore.getCount() == 1, "Duplicate rejection must not consume the module");

      ItemStack incompatible = new ItemStack(ModItems.GAS_MASK_FILTER.get());
      helper.assertFalse(ArmoryData.installModule(blade, incompatible), "Incompatible module should be rejected");
      helper.assertTrue(incompatible.getCount() == 1, "Incompatible rejection must not consume the module");

      ItemStack chakram = new ItemStack(ModItems.SIGIL_CHAKRAM.get());
      ItemStack fire = new ItemStack(ModItems.FIRE_CORE.get());
      ItemStack frost = new ItemStack(ModItems.FROST_CORE.get());
      ItemStack lightning = new ItemStack(ModItems.LIGHTNING_CORE.get());
      ArmoryData.initialize(chakram);
      helper.assertTrue(ArmoryData.installModule(chakram, fire), "First module should fill Sigil Chakram slot one");
      helper.assertTrue(ArmoryData.installModule(chakram, frost), "Second module should fill Sigil Chakram slot two");
      helper.assertFalse(ArmoryData.installModule(chakram, lightning), "Full module slots should reject another compatible module");
      helper.assertTrue(lightning.getCount() == 1, "Full-slot rejection must not consume the module");
      helper.succeed();
   }

   private static void rangedResourceSafety(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      player.getAbilities().instabuild = false;
      Vec3 playerPos = helper.absoluteVec(new Vec3(4.5D, 2.0D, 1.5D));
      player.setPos(playerPos.x, playerPos.y, playerPos.z);
      player.setYRot(0.0F);
      player.setXRot(0.0F);
      ItemStack bow = new ItemStack(ModItems.VEIL_BOW.get());
      ArmoryData.initialize(bow);
      ArmoryData.spendEnergy(bow, 40);
      player.setItemInHand(InteractionHand.MAIN_HAND, bow);
      player.getInventory().add(new ItemStack(ModItems.AMMO_CRYSTALS.get(), 2));
      int ammoBeforeNoTarget = count(player, ModItems.AMMO_CRYSTALS.get());
      int energyBeforeNoTarget = player.getMainHandItem().get(ModDataComponents.ENERGY_STATE.get()).stored();
      InteractionResult noTarget = player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(noTarget == InteractionResult.CONSUME, "No-target ranged use should fail visibly");
      helper.assertTrue(count(player, ModItems.AMMO_CRYSTALS.get()) == ammoBeforeNoTarget, "No-target ranged use must not consume ammo");
      helper.assertTrue(player.getMainHandItem().get(ModDataComponents.ENERGY_STATE.get()).stored() == energyBeforeNoTarget,
         "No-target ranged use must not consume energy");

      for (int i = 0; i < 20; i++) {
         player.getCooldowns().tick();
      }
      LivingEntity target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 6));
      helper.assertTrue(target != null, "Zombie target should be spawnable");
      int ammoBeforeTarget = count(player, ModItems.AMMO_CRYSTALS.get());
      int energyBeforeTarget = player.getMainHandItem().get(ModDataComponents.ENERGY_STATE.get()).stored();
      int projectilesBefore = countArmoryProjectiles(helper);
      InteractionResult targetShot = player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(targetShot == InteractionResult.SUCCESS_SERVER || count(player, ModItems.AMMO_CRYSTALS.get()) == ammoBeforeTarget - 1,
         "Targeted ranged shot should fire");
      helper.assertTrue(count(player, ModItems.AMMO_CRYSTALS.get()) == ammoBeforeTarget - 1, "Targeted ranged shot should spend one Ammo Crystal first");
      helper.assertTrue(player.getMainHandItem().get(ModDataComponents.ENERGY_STATE.get()).stored() == energyBeforeTarget,
         "Targeted ranged shot with ammo must not also spend energy");
      helper.assertTrue(countArmoryProjectiles(helper) == projectilesBefore + 1, "Accepted ranged shot should spawn an Armory projectile entity");
      helper.succeed();
   }

   private static void rechargeCostsFuel(GameTestHelper helper) {
      ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
      ArmoryData.initialize(blade);
      helper.assertTrue(ArmoryData.spendEnergy(blade, 60), "Energy test gear should spend energy");
      ItemStack fuel = new ItemStack(ModItems.RESONANCE_SHARD.get(), 2);
      helper.assertTrue(ArmoryData.rechargeWithFuel(blade, fuel), "Recharge should accept Resonance Shard fuel");
      helper.assertTrue(fuel.getCount() == 1, "Recharge should consume one fuel item");
      helper.assertTrue(blade.get(ModDataComponents.ENERGY_STATE.get()).stored() == blade.get(ModDataComponents.ENERGY_STATE.get()).capacity(),
         "Recharge should refill to capacity");
      ArmoryData.spendEnergy(blade, 20);
      ItemStack noFuel = ItemStack.EMPTY;
      helper.assertFalse(ArmoryData.rechargeWithFuel(blade, noFuel), "Recharge without fuel should fail");
      helper.assertTrue(blade.get(ModDataComponents.ENERGY_STATE.get()).stored() < blade.get(ModDataComponents.ENERGY_STATE.get()).capacity(),
         "Failed recharge should leave energy unchanged below capacity");
      helper.succeed();
   }

   private static void factionLockSafety(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      player.getAbilities().instabuild = false;
      ItemStack rifle = new ItemStack(ModItems.ENERGY_RIFLE.get());
      ArmoryData.initialize(rifle);
      player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
      helper.assertFalse(ArmoryData.factionGateSatisfied(player, rifle), "Survival player without reputation should not satisfy Energy Rifle gate");
      InteractionResult lockedUse = player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(lockedUse == InteractionResult.CONSUME, "Locked gear should fail use with feedback");
      helper.assertTrue(player.getMainHandItem().is(ModItems.ENERGY_RIFLE.get()) && player.getMainHandItem().getCount() == 1,
         "Locked gear must remain in the player's hand and never be deleted");
      helper.succeed();
   }

   private static void stackComponentRoundTripPersistsArmoryState(GameTestHelper helper) {
      ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
      ItemStack module = new ItemStack(ModItems.FROST_CORE.get());
      ArmoryData.initialize(blade);
      helper.assertTrue(ArmoryData.installModule(blade, module), "Persistence fixture should install a Frost Core");
      helper.assertTrue(ArmoryData.spendEnergy(blade, 72), "Persistence fixture should spend energy before save");
      blade.set(ModDataComponents.EQUIPMENT_TIER.get(), new EquipmentTier(3, "Industrial upgraded"));
      blade.set(ModDataComponents.STANCE.get(), ArmoryStance.HEAVY);
      blade.set(ModDataComponents.COSMETIC_TRIM.get(), new CosmeticTrim("release-proof", 0xFF66E8FF));
      blade.set(ModDataComponents.INSTABILITY_STATE.get(), new InstabilityState(37, 120));
      blade.set(ModDataComponents.ARMORY_LOADOUT.get(), new ArmoryLoadout("echoarmory:toxic_breach_kit", "Toxic Breach Kit"));

      ItemStack loaded = roundTripStack(helper, blade);

      helper.assertTrue(loaded.is(ModItems.FROST_BLADE.get()), "Round-tripped stack should remain the same Armory item");
      helper.assertTrue(ArmoryData.modules(loaded).contains("echoarmory:frost_core"), "Installed module ids should survive stack save/load");
      EnergyState energy = loaded.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
      helper.assertTrue(energy.capacity() == 180 && energy.stored() == 108, "Energy stored/capacity should survive stack save/load");
      helper.assertTrue(loaded.getOrDefault(ModDataComponents.EQUIPMENT_TIER.get(), EquipmentTier.TIER_1).tier() == 3,
         "Equipment tier should survive stack save/load");
      helper.assertTrue(loaded.getOrDefault(ModDataComponents.STANCE.get(), ArmoryStance.BALANCED) == ArmoryStance.HEAVY,
         "Stance should survive stack save/load");
      helper.assertTrue("release-proof".equals(loaded.getOrDefault(ModDataComponents.COSMETIC_TRIM.get(), CosmeticTrim.EMPTY).sigil()),
         "Cosmetic trim should survive stack save/load");
      helper.assertTrue(loaded.getOrDefault(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE).instability() == 37,
         "Instability should survive stack save/load");
      helper.assertTrue("echoarmory:toxic_breach_kit".equals(loaded.getOrDefault(ModDataComponents.ARMORY_LOADOUT.get(), ArmoryLoadout.EMPTY).loadoutId()),
         "Loadout marker should survive stack save/load");
      helper.succeed();
   }

   private static void stationInventoryRoundTripPersistsItemsAndGearState(GameTestHelper helper) {
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, (Block)ModBlocks.MODULE_UPGRADE_TABLE.get());
      ArmoryStationBlockEntity station = helper.getBlockEntity(pos, ArmoryStationBlockEntity.class);
      ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
      ArmoryData.initialize(blade);
      helper.assertTrue(ArmoryData.spendEnergy(blade, 40), "Station persistence fixture should spend gear energy");
      station.setItem(ArmoryStationBlockEntity.GEAR_SLOT, blade);
      station.setItem(ArmoryStationBlockEntity.MODULE_SLOT, new ItemStack(ModItems.FROST_CORE.get()));
      station.setItem(ArmoryStationBlockEntity.AUX_SLOT, new ItemStack(ModItems.RESONANCE_SHARD.get(), 2));
      ServerPlayer player = helper.makeMockServerPlayerInLevel();

      helper.assertTrue(station.handleMenuButton(player, ArmoryStationMenu.BUTTON_APPLY), "Station apply should run for persistence fixture");
      helper.assertTrue(station.isOperationActive(), "Successful station operation should enter active progress");
      helper.assertTrue(ArmoryData.modules(station.getItem(ArmoryStationBlockEntity.GEAR_SLOT)).contains("echoarmory:frost_core"),
         "Station operation should install the module before save");

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      station.saveWithoutMetadata(output);
      CompoundTag saved = output.buildResult();
      ArmoryStationBlockEntity loaded = new ArmoryStationBlockEntity(helper.absolutePos(pos), station.getBlockState());
      loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

      ItemStack loadedGear = loaded.getItem(ArmoryStationBlockEntity.GEAR_SLOT);
      helper.assertTrue(loadedGear.is(ModItems.FROST_BLADE.get()), "Station gear slot should survive block entity save/load");
      helper.assertTrue(ArmoryData.modules(loadedGear).contains("echoarmory:frost_core"),
         "Station-stored gear components should survive block entity save/load");
      helper.assertTrue(loadedGear.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY).stored() == 140,
         "Station-stored gear energy should survive block entity save/load");
      helper.assertTrue(loaded.getItem(ArmoryStationBlockEntity.MODULE_SLOT).isEmpty(), "Consumed station module slot should reload empty");
      helper.assertTrue(loaded.getItem(ArmoryStationBlockEntity.AUX_SLOT).is(ModItems.RESONANCE_SHARD.get())
            && loaded.getItem(ArmoryStationBlockEntity.AUX_SLOT).getCount() == 2,
         "Station AUX inventory should survive save/load without count changes");
      helper.assertTrue(loaded.data().get(ArmoryStationBlockEntity.DATA_PROGRESS) > 0, "Station operation progress should survive save/load");
      helper.assertTrue(loaded.statusLine().contains("module install"), "Station last action should survive save/load");
      helper.succeed();
   }

   private static void stationSlotAndTransferSafety(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      BlockPos pos = new BlockPos(1, 1, 1);
      helper.setBlock(pos, (Block)ModBlocks.MODULE_UPGRADE_TABLE.get());
      ArmoryStationBlockEntity station = helper.getBlockEntity(pos, ArmoryStationBlockEntity.class);
      ArmoryStationMenu menu = new ArmoryStationMenu(1, player.getInventory(), station, station.data());

      helper.assertFalse(menu.getSlot(ArmoryStationBlockEntity.GEAR_SLOT).mayPlace(new ItemStack(ModItems.FROST_CORE.get())),
         "Gear slot should reject modules");
      helper.assertFalse(menu.getSlot(ArmoryStationBlockEntity.MODULE_SLOT).mayPlace(new ItemStack(ModItems.ALLOY_SWORD.get())),
         "Module slot should reject weapons");
      helper.assertFalse(menu.getSlot(ArmoryStationBlockEntity.AUX_SLOT).mayPlace(new ItemStack(Items.DIRT)),
         "AUX slot should reject non-material junk");
      helper.assertFalse(station.canPlaceItemThroughFace(ArmoryStationBlockEntity.AUX_SLOT, new ItemStack(Items.DIRT), Direction.UP),
         "Automation insertion should obey AUX slot rules");

      player.getInventory().setItem(9, new ItemStack(ModItems.ALLOY_SWORD.get()));
      helper.assertTrue(!menu.quickMoveStack(player, ArmoryStationBlockEntity.SLOT_COUNT).isEmpty(),
         "Shift-clicking valid gear should move it into the protected gear slot");
      helper.assertTrue(station.getItem(ArmoryStationBlockEntity.GEAR_SLOT).is(ModItems.ALLOY_SWORD.get()),
         "Quick-moved gear should land in the gear slot");
      player.getInventory().setItem(10, new ItemStack(ModItems.STABILITY_RUNE.get()));
      helper.assertTrue(!menu.quickMoveStack(player, ArmoryStationBlockEntity.SLOT_COUNT + 1).isEmpty(),
         "Shift-clicking a valid module should move it into the module slot");
      helper.assertTrue(station.getItem(ArmoryStationBlockEntity.MODULE_SLOT).is(ModItems.STABILITY_RUNE.get()),
         "Quick-moved module should land in the module slot");
      player.getInventory().setItem(11, new ItemStack(ModItems.VEIL_CRYSTAL.get()));
      helper.assertTrue(!menu.quickMoveStack(player, ArmoryStationBlockEntity.SLOT_COUNT + 2).isEmpty(),
         "Shift-clicking recharge fuel should move it into AUX");
      helper.assertTrue(station.getItem(ArmoryStationBlockEntity.AUX_SLOT).is(ModItems.VEIL_CRYSTAL.get()),
         "Quick-moved fuel should land in AUX");

      BlockPos activePos = new BlockPos(3, 1, 1);
      helper.setBlock(activePos, (Block)ModBlocks.MODULE_UPGRADE_TABLE.get());
      ArmoryStationBlockEntity active = helper.getBlockEntity(activePos, ArmoryStationBlockEntity.class);
      ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
      ArmoryData.initialize(blade);
      active.setItem(ArmoryStationBlockEntity.GEAR_SLOT, blade);
      active.setItem(ArmoryStationBlockEntity.MODULE_SLOT, new ItemStack(ModItems.FROST_CORE.get()));
      helper.assertTrue(active.handleMenuButton(player, ArmoryStationMenu.BUTTON_APPLY), "Active station fixture should apply successfully");
      ArmoryStationMenu activeMenu = new ArmoryStationMenu(2, player.getInventory(), active, active.data());
      helper.assertTrue(active.isOperationActive(), "Station should be active after successful apply");
      helper.assertFalse(activeMenu.getSlot(ArmoryStationBlockEntity.GEAR_SLOT).mayPickup(player),
         "Protected gear slot should block manual pickup during active operation");
      helper.assertTrue(activeMenu.quickMoveStack(player, ArmoryStationBlockEntity.GEAR_SLOT).isEmpty(),
         "Protected gear slot should block quick-move extraction during active operation");
      helper.assertTrue(active.getItem(ArmoryStationBlockEntity.GEAR_SLOT).is(ModItems.FROST_BLADE.get()),
         "Blocked quick-move extraction should leave gear in the station");
      helper.assertFalse(active.canTakeItemThroughFace(ArmoryStationBlockEntity.GEAR_SLOT,
            active.getItem(ArmoryStationBlockEntity.GEAR_SLOT), Direction.UP),
         "Automation extraction should not pull protected gear during active operation");
      helper.succeed();
   }

   private static void terminalSelectedActionPaths(GameTestHelper helper) {
      TerminalActionRegistry.withClearedForTests(() -> {
         ArmoryTerminalCommonIntegration.resetForTests();
         ArmoryTerminalCommonIntegration.register();
         ServerPlayer player = helper.makeMockServerPlayerInLevel();
         player.getAbilities().instabuild = false;
         ItemStack blade = new ItemStack(ModItems.FROST_BLADE.get());
         ArmoryData.initialize(blade);
         helper.assertTrue(ArmoryData.spendEnergy(blade, 60), "Terminal recharge fixture should spend energy first");
         player.setItemInHand(InteractionHand.MAIN_HAND, blade);
         player.getInventory().add(new ItemStack(ModItems.RESONANCE_SHARD.get(), 2));
         int fuelBefore = count(player, ModItems.RESONANCE_SHARD.get());
         helper.assertTrue(TerminalActionRegistry.handle(player, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.RECHARGE_ACTION, ""),
            "Terminal recharge action should handle selected main-hand gear");
         helper.assertTrue(count(player, ModItems.RESONANCE_SHARD.get()) == fuelBefore - 1,
            "Terminal recharge should consume exactly one inventory recharge material");
         EnergyState recharged = player.getMainHandItem().getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
         helper.assertTrue(recharged.capacity() > 0 && recharged.stored() == recharged.capacity(),
            "Terminal recharge should refill stored energy to capacity");

         BossRecommendationDefinition selectedBoss = selectedBossForTests("veilbound_guardian")
            .orElseThrow(() -> new AssertionError("Expected selected boss recommendation to resolve"));
         helper.assertTrue("Veilbound Guardian".equals(selectedBoss.bossName()),
            "Selected boss payload should resolve the requested recommendation");
         helper.assertTrue(TerminalActionRegistry.handle(player, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.PREVIEW_ACTION, "veilbound_guardian"),
            "Terminal preview should handle an explicit selected boss payload");

         player.getInventory().add(new ItemStack(ModItems.AMMO_CRYSTALS.get(), 3));
         int ammoBefore = count(player, ModItems.AMMO_CRYSTALS.get());
         helper.assertTrue(TerminalActionRegistry.handle(player, ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.LOGISTICS_ACTION, "toxic_breach_kit"),
            "Terminal Logistics action should handle a selected loadout even when no endpoint is nearby");
         helper.assertTrue(count(player, ModItems.AMMO_CRYSTALS.get()) == ammoBefore,
            "Blocked Logistics dispatch should not consume inventory items");
         helper.assertTrue(player.getMainHandItem().is(ModItems.FROST_BLADE.get()),
            "Blocked Logistics dispatch should not alter equipped gear");
      });
      helper.succeed();
   }

   private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
      int total = 0;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.is(item)) {
            total += stack.getCount();
         }
      }
      return total;
   }

   private static int countArmoryProjectiles(GameTestHelper helper) {
      Vec3 center = helper.absoluteVec(new Vec3(8.0D, 2.0D, 8.0D));
      return helper.getLevel().getEntitiesOfClass(ArmoryProjectileEntity.class,
         new AABB(center.x - 64, center.y - 64, center.z - 64, center.x + 64, center.y + 64, center.z + 64)).size();
   }

   private static void missionCoreContentRegistration(GameTestHelper helper) {
      InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
      ArmoryMissionCoreIntegration.registerContent(registry);
      helper.assertTrue(registry.chapter(id("armory")).isPresent(), "Armory MissionCore chapter should be owned by Armory.");
      assertMission(helper, registry, "inspect_loadout", "scan", MissionObjectiveType.SCAN_ENTITY);
      assertMission(helper, registry, "forge_upgrade", "upgrade", MissionObjectiveType.CRAFT_ITEM);
      assertMission(helper, registry, "install_module", "module", MissionObjectiveType.REPAIR_MACHINE);
      assertMission(helper, registry, "recharge_core", "recharge", MissionObjectiveType.REPAIR_MACHINE);
      assertMission(helper, registry, "bind_loadout", "bind", MissionObjectiveType.SCAN_ENTITY);
      assertMission(helper, registry, "prepare_route_kit", "prepare", MissionObjectiveType.CUSTOM);
      assertMission(helper, registry, "dispatch_route_kit", "dispatch", MissionObjectiveType.ESTABLISH_ROUTE);
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
      helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "Armory MissionCore missions should be side ops.");
      helper.assertTrue(!mission.rewards().isEmpty(), "Armory MissionCore mission should have a claimable reward: " + missionId);
      helper.assertTrue(mission.objectives().size() == 1, "Armory MissionCore mission should have one direct objective: " + missionId);
      helper.assertTrue(mission.objectives().getFirst().type() == type, "Armory objective type should stay stable: " + missionId);
      String target = mission.objectives().getFirst().criteria().get("target");
      helper.assertTrue(MissionHookTargets.objectiveTarget(EchoArmory.MODID, missionId, objectiveKey).toString().equals(target),
         "Armory MissionCore objective target should use MissionHookTargets: " + missionId);
   }

   private static ItemStack roundTripStack(GameTestHelper helper, ItemStack stack) {
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      output.store("stack", ItemStack.OPTIONAL_CODEC, stack);
      CompoundTag saved = output.buildResult();
      return TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved)
         .read("stack", ItemStack.OPTIONAL_CODEC)
         .orElse(ItemStack.EMPTY);
   }

   @SuppressWarnings("unchecked")
   private static Optional<BossRecommendationDefinition> selectedBossForTests(String payload) {
      try {
         Method method = ArmoryTerminalCommonIntegration.class.getDeclaredMethod("selectedBoss", String.class);
         method.setAccessible(true);
         return (Optional<BossRecommendationDefinition>)method.invoke(null, payload);
      } catch (ReflectiveOperationException exception) {
         throw new AssertionError("Unable to resolve Armory Terminal boss selection for tests", exception);
      }
   }

   private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("armory_" + testName));
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
         environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, TEST_PADDING
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoArmory.MODID, path);
   }

   private static boolean shouldRegisterTests() {
      String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
      if (namespaces == null || namespaces.isBlank()) {
         return true;
      }
      for (String namespace : namespaces.split(",")) {
         String normalized = namespace.trim();
         if (normalized.equals(EchoArmory.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
            return true;
         }
      }
      return false;
   }
}
