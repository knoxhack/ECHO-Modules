package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echoblockworks.Config;
import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.block.BlockworksStateUtil;
import com.knoxhack.echoblockworks.block.entity.BlockworksTableBlockEntity;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksPaletteKit;
import com.knoxhack.echoblockworks.content.BlockworksShapeKind;
import com.knoxhack.echoblockworks.content.BlockworksWorldgenSite;
import com.knoxhack.echoblockworks.integration.BlockworksMissionCoreIntegration;
import com.knoxhack.echoblockworks.menu.BlockworksTableMenu;
import com.knoxhack.echocore.api.mission.InMemoryMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameType;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, EchoBlockworks.MODID);

   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CATALOG_TEST =
      TEST_FUNCTIONS.register("catalog_has_required_mvp_content", () -> ModGameTests::catalogHasRequiredMvpContent);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TABLE_CONVERSION =
      TEST_FUNCTIONS.register("table_conversion_consumes_one_input", () -> ModGameTests::tableConversionConsumesOneInput);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TABLE_MENU_SHIFT_CLICK =
      TEST_FUNCTIONS.register("table_menu_shift_click_and_invalid_inputs", () -> ModGameTests::tableMenuShiftClickAndInvalidInputs);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CUTTER_STATE_COPY =
      TEST_FUNCTIONS.register("cutter_preserves_shape_state", () -> ModGameTests::cutterPreservesShapeState);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDGEN_RESOURCE_COVERAGE =
      TEST_FUNCTIONS.register("worldgen_resource_coverage", () -> ModGameTests::worldgenResourceCoverage);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONFIG_DEFAULTS =
      TEST_FUNCTIONS.register("config_defaults", () -> ModGameTests::configDefaults);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TABLE_KIT_MODE =
      TEST_FUNCTIONS.register("table_palette_kit_mode_and_stack_conversion", () -> ModGameTests::tablePaletteKitModeAndStackConversion);
   @SuppressWarnings("unused")
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
      TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("blockworks_catalog"));
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
         environment, Identifier.withDefaultNamespace("empty"), 100, 0, true, Rotation.NONE, false, 1, 1, false, 2);
      register(event, data, "catalog_has_required_mvp_content", CATALOG_TEST.getId());
      register(event, data, "table_conversion_consumes_one_input", TABLE_CONVERSION.getId());
      register(event, data, "table_menu_shift_click_and_invalid_inputs", TABLE_MENU_SHIFT_CLICK.getId());
      register(event, data, "cutter_preserves_shape_state", CUTTER_STATE_COPY.getId());
      register(event, data, "worldgen_resource_coverage", WORLDGEN_RESOURCE_COVERAGE.getId());
      register(event, data, "config_defaults", CONFIG_DEFAULTS.getId());
      register(event, data, "table_palette_kit_mode_and_stack_conversion", TABLE_KIT_MODE.getId());
      register(event, data, "missioncore_content_registration", MISSION_CORE_CONTENT.getId());
   }

   private static void register(RegisterGameTestsEvent event, TestData<Holder<TestEnvironmentDefinition<?>>> data, String name, Identifier functionId) {
      event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static void catalogHasRequiredMvpContent(GameTestHelper helper) {
      helper.assertTrue(BlockworksCatalog.families().size() == 10, "Expected 10 Blockworks families.");
      long fullBlocks = BlockworksCatalog.blockInfos().stream()
         .filter(info -> info.shape() == BlockworksShapeKind.FULL)
         .count();
      helper.assertTrue(fullBlocks >= 80, "Expected at least 80 full decorative blocks.");
      helper.assertTrue(BlockworksCatalog.details().size() == 16, "Expected 16 detail blocks.");
      helper.assertTrue(BlockworksCatalog.worldgenSites().size() == 8, "Expected 8 v2 worldgen showcase sites.");
      helper.assertTrue(BlockworksCatalog.paletteKits().size() == 11, "Expected 11 v3 palette kits.");
      helper.assertTrue(BlockworksCatalog.paletteKit("cyberglass_control_room")
         .map(kit -> kit.theme().name().equals("CYBERGLASS"))
         .orElse(false), "CyberGlass palette kit should be present.");
      for (BlockworksPaletteKit kit : BlockworksCatalog.paletteKits()) {
         helper.assertFalse(kit.familyIds().isEmpty(), "Palette kit should reference at least one family: " + kit.id());
         kit.familyIds().forEach(family -> helper.assertTrue(BlockworksCatalog.family(family).isPresent(),
            "Palette kit family should exist: " + kit.id() + "/" + family));
         java.util.stream.Stream.concat(kit.featuredBlockIds().stream(), kit.accentBlockIds().stream())
            .filter(blockId -> !BlockworksCatalog.blockInfo(blockId).isPresent() && ModBlocks.blockForId(blockId).isEmpty())
            .findFirst()
            .ifPresent(blockId -> helper.fail("Palette kit block should exist: " + kit.id() + "/" + blockId));
      }
      helper.assertTrue(BlockworksCatalog.blockInfo("reinforced_metal_panel")
         .map(BlockworksCatalog::conversionTargets)
         .map(targets -> targets.size() == 8)
         .orElse(false), "Reinforced Metal Panel should convert across 8 variants.");
      helper.succeed();
   }

   private static void missionCoreContentRegistration(GameTestHelper helper) {
      InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
      BlockworksMissionCoreIntegration.registerContent(registry);
      helper.assertTrue(registry.chapter(id("blockworks")).isPresent(), "Blockworks MissionCore chapter should be owned by Blockworks.");
      assertMission(helper, registry, "use_table", "table", MissionObjectiveType.CUSTOM);
      assertMission(helper, registry, "convert_variant", "convert", MissionObjectiveType.CRAFT_ITEM);
      assertMission(helper, registry, "use_pattern_cutter", "cutter", MissionObjectiveType.PLACE_BLOCK);
      assertMission(helper, registry, "discover_showcase_site", "showcase", MissionObjectiveType.DISCOVER_STRUCTURE);
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
      helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "Blockworks MissionCore missions should be side ops.");
      helper.assertTrue(!mission.rewards().isEmpty(), "Blockworks MissionCore mission should have a claimable reward: " + missionId);
      helper.assertTrue(mission.objectives().size() == 1, "Blockworks MissionCore mission should have one direct objective: " + missionId);
      helper.assertTrue(mission.objectives().getFirst().type() == type, "Blockworks objective type should stay stable: " + missionId);
      String target = mission.objectives().getFirst().criteria().get("target");
      helper.assertTrue(MissionHookTargets.objectiveTarget(EchoBlockworks.MODID, missionId, objectiveKey).toString().equals(target),
         "Blockworks MissionCore objective target should use MissionHookTargets: " + missionId);
   }

   private static void tablePaletteKitModeAndStackConversion(GameTestHelper helper) {
      BlockworksTableBlockEntity table = new BlockworksTableBlockEntity(BlockPos.ZERO, ModBlocks.BLOCKWORKS_TABLE.get().defaultBlockState());
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      table.setItem(BlockworksTableBlockEntity.INPUT_SLOT, new ItemStack(ModBlocks.blockForId("reinforced_metal_panel").orElseThrow().get(), 5));
      BlockworksTableMenu menu = new BlockworksTableMenu(2, player.getInventory(), table, table.data());
      helper.assertTrue(menu.targets().size() == 8, "All mode should expose all same-family full variants.");

      table.setSelectedViewMode(BlockworksTableBlockEntity.VIEW_KIT);
      table.setSelectedKitIndex(9);
      helper.assertTrue(menu.activeKit().map(kit -> kit.id().equals("industrial_factory")).orElse(false),
         "Selected kit should be Industrial Factory.");
      List<BlockworksBlockInfo> kitTargets = menu.targets();
      helper.assertTrue(kitTargets.size() == 4, "Industrial Factory should filter Reinforced Metal to four featured variants.");
      helper.assertTrue(kitTargets.stream().anyMatch(info -> info.blockId().equals("reinforced_metal_hazard_stripe")),
         "Kit mode should include kit-specific Reinforced Metal variants.");

      table.setSelectedVariant(1);
      ItemStack moved = menu.quickMoveStack(player, BlockworksTableBlockEntity.OUTPUT_SLOT);
      helper.assertTrue(moved.getCount() == 5, "Shift-clicking output should convert the full input stack when inventory has room.");
      helper.assertTrue(table.getItem(BlockworksTableBlockEntity.INPUT_SLOT).isEmpty(), "Stack conversion should consume matching input count.");
      int converted = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(ModBlocks.blockForId("reinforced_metal_riveted").orElseThrow().get().asItem())) {
            converted += stack.getCount();
         }
      }
      helper.assertTrue(converted == 5, "Player inventory should receive exactly five converted blocks.");

      table.setItem(BlockworksTableBlockEntity.INPUT_SLOT, new ItemStack(ModBlocks.blockForId("ashstone_raw").orElseThrow().get()));
      helper.assertTrue(menu.kitFallbackActive(), "Kit mode should report fallback when the selected kit has no matching family variants.");
      helper.assertTrue(menu.targets().size() == 8, "Fallback should show all same-family variants.");
      helper.succeed();
   }

   private static void tableConversionConsumesOneInput(GameTestHelper helper) {
      BlockPos local = new BlockPos(1, 1, 1);
      helper.setBlock(local, ModBlocks.BLOCKWORKS_TABLE.get());
      BlockworksTableBlockEntity table = helper.getBlockEntity(local, BlockworksTableBlockEntity.class);
      table.setItem(BlockworksTableBlockEntity.INPUT_SLOT, new ItemStack(ModBlocks.blockForId("reinforced_metal_panel").orElseThrow().get(), 3));
      table.setSelectedVariant(1);
      helper.assertTrue(table.getItem(BlockworksTableBlockEntity.OUTPUT_SLOT).is(ModBlocks.blockForId("reinforced_metal_riveted").orElseThrow().get().asItem()),
         "Table output should preview the selected target.");
      table.completeConversion(helper.makeMockPlayer(GameType.SURVIVAL));
      helper.assertTrue(table.getItem(BlockworksTableBlockEntity.INPUT_SLOT).getCount() == 2,
         "Taking table output should consume exactly one input.");
      helper.assertTrue(table.getItem(BlockworksTableBlockEntity.OUTPUT_SLOT).getCount() == 1,
         "Output slot should remain a one-item preview after conversion.");

      table.setSelectedVariant(4);
      table.setItem(BlockworksTableBlockEntity.INPUT_SLOT, new ItemStack(ModBlocks.blockForId("ashstone_raw").orElseThrow().get()));
      helper.assertTrue(table.selectedTarget()
         .map(info -> info.blockId().equals("ashstone_raw"))
         .orElse(false), "Changing family/shape should reset selected target to the first variant.");
      helper.succeed();
   }

   private static void tableMenuShiftClickAndInvalidInputs(GameTestHelper helper) {
      BlockworksTableBlockEntity table = new BlockworksTableBlockEntity(BlockPos.ZERO, ModBlocks.BLOCKWORKS_TABLE.get().defaultBlockState());
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.getInventory().setItem(9, new ItemStack(ModBlocks.blockForId("charred_concrete_smooth").orElseThrow().get(), 2));
      BlockworksTableMenu menu = new BlockworksTableMenu(1, player.getInventory(), table, table.data());
      int sourceSlot = -1;
      for (int i = 2; i < menu.slots.size(); i++) {
         if (menu.slots.get(i).getItem().is(ModBlocks.blockForId("charred_concrete_smooth").orElseThrow().get().asItem())) {
            sourceSlot = i;
            break;
         }
      }
      helper.assertTrue(sourceSlot >= 0, "Menu should expose the player's Blockworks stack.");
      helper.assertTrue(!menu.quickMoveStack(player, sourceSlot).isEmpty(), "Shift-click should move Blockworks blocks into the table input.");
      helper.assertTrue(table.getItem(BlockworksTableBlockEntity.INPUT_SLOT).is(ModBlocks.blockForId("charred_concrete_smooth").orElseThrow().get().asItem()),
         "Shift-clicked Blockworks stack should land in the input slot.");
      helper.assertFalse(menu.getSlot(BlockworksTableBlockEntity.INPUT_SLOT).mayPlace(new ItemStack(Blocks.DIRT)),
         "Input slot should reject non-Blockworks blocks.");
      helper.succeed();
   }

   private static void cutterPreservesShapeState(GameTestHelper helper) {
      BlockState sourceSlab = ModBlocks.blockForId("reinforced_metal_panel_slab").orElseThrow().get().defaultBlockState()
         .setValue(SlabBlock.TYPE, SlabType.TOP);
      BlockState targetSlab = BlockworksStateUtil.copySharedProperties(sourceSlab, ModBlocks.blockForId("reinforced_metal_riveted_slab").orElseThrow().get().defaultBlockState());
      helper.assertTrue(targetSlab.getValue(SlabBlock.TYPE) == SlabType.TOP, "Cutter should preserve slab type.");

      BlockState sourceStairs = ModBlocks.blockForId("reinforced_metal_panel_stairs").orElseThrow().get().defaultBlockState()
         .setValue(StairBlock.FACING, net.minecraft.core.Direction.SOUTH)
         .setValue(StairBlock.HALF, Half.TOP)
         .setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT);
      BlockState targetStairs = BlockworksStateUtil.copySharedProperties(sourceStairs, ModBlocks.blockForId("reinforced_metal_riveted_stairs").orElseThrow().get().defaultBlockState());
      helper.assertTrue(targetStairs.getValue(StairBlock.FACING) == net.minecraft.core.Direction.SOUTH
         && targetStairs.getValue(StairBlock.HALF) == Half.TOP
         && targetStairs.getValue(StairBlock.SHAPE) == StairsShape.INNER_LEFT, "Cutter should preserve stair facing, half, and shape.");

      BlockState sourceWall = ModBlocks.blockForId("reinforced_metal_panel_wall").orElseThrow().get().defaultBlockState()
         .setValue(WallBlock.NORTH, WallSide.TALL)
         .setValue(WallBlock.UP, true)
         .setValue(BlockStateProperties.WATERLOGGED, false);
      BlockState targetWall = BlockworksStateUtil.copySharedProperties(sourceWall, ModBlocks.blockForId("reinforced_metal_riveted_wall").orElseThrow().get().defaultBlockState());
      helper.assertTrue(targetWall.getValue(WallBlock.NORTH) == WallSide.TALL && targetWall.getValue(WallBlock.UP),
         "Cutter should preserve wall side and post state.");
      helper.assertTrue(BlockworksCatalog.blockInfo("minecraft:stone").isEmpty(), "Catalog lookup should not treat non-Blockworks ids as cuttable.");
      helper.succeed();
   }

   private static void worldgenResourceCoverage(GameTestHelper helper) {
      assertResource(helper, "worldgen/structure/blockworks_showcase_site.json");
      assertResource(helper, "worldgen/template_pool/blockworks_showcase_sites.json");
      assertResource(helper, "worldgen/structure_set/blockworks_showcase_sites.json");
      assertResource(helper, "tags/worldgen/biome/has_structure/blockworks_showcase_site.json");
      for (BlockworksWorldgenSite site : BlockworksCatalog.worldgenSites()) {
         assertResource(helper, "structures/" + site.structureTemplate() + ".nbt");
         assertResource(helper, "palettes/" + site.paletteId() + ".json");
      }
      for (BlockworksPaletteKit kit : BlockworksCatalog.paletteKits()) {
         assertResource(helper, "palette_kits/" + kit.id() + ".json");
      }
      assertClasspathResourceContains(helper, "assets/echoblockworks/blockstates/reinforced_metal_panel.json", "\"weight\"");
      for (String texture : List.of(
         "flickering_warning_light",
         "steam_vent",
         "sparking_cable_panel",
         "hologram_floor_projector",
         "reinforced_metal_lit_panel",
         "orbital_hull_lit_strip"
      )) {
         assertClasspathResource(helper, "assets/echoblockworks/textures/block/" + texture + ".png.mcmeta");
      }
      helper.succeed();
   }

   private static void configDefaults(GameTestHelper helper) {
      helper.assertTrue(Config.PROCEDURAL_SCATTER_ENABLED.get(), "Procedural scatter should default to enabled.");
      helper.assertTrue(Config.SCATTER_SPACING_CHUNKS.get() == 32, "Scatter spacing should default to 32 chunks.");
      helper.assertTrue(Config.SCATTER_SEARCH_RADIUS.get() == 10, "Scatter search radius should default to 10.");
      helper.assertTrue(Config.SCATTER_MAX_PIECES.get() == 7, "Scatter max pieces should default to 7.");
      helper.succeed();
   }

   private static void assertResource(GameTestHelper helper, String path) {
      Identifier resourceId = id(path);
      helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(resourceId).isPresent(),
         "Expected resource " + resourceId);
   }

   private static void assertClasspathResource(GameTestHelper helper, String path) {
      try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
         helper.assertTrue(stream != null, "Expected packaged resource " + path);
      } catch (IOException exception) {
         helper.fail("Could not read packaged resource " + path + ": " + exception.getMessage());
      }
   }

   private static void assertClasspathResourceContains(GameTestHelper helper, String path, String needle) {
      try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
         helper.assertTrue(stream != null, "Expected packaged resource " + path);
         String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         helper.assertTrue(text.contains(needle), "Expected packaged resource " + path + " to contain " + needle);
      } catch (IOException exception) {
         helper.fail("Could not read packaged resource " + path + ": " + exception.getMessage());
      }
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, path);
   }
}
