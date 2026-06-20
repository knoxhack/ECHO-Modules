package com.knoxhack.echomultiblockcore.registry;

import com.google.gson.JsonParser;
import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationEffectHandler;
import com.knoxhack.echomultiblockcore.api.AutomationEffectHandlers;
import com.knoxhack.echomultiblockcore.api.AutomationEffectInvocation;
import com.knoxhack.echomultiblockcore.api.AutomationEffectResult;
import com.knoxhack.echomultiblockcore.api.AutomationIngredient;
import com.knoxhack.echomultiblockcore.api.AutomationOutput;
import com.knoxhack.echomultiblockcore.api.AutoBuilderPlan;
import com.knoxhack.echomultiblockcore.api.AutoBuilderResult;
import com.knoxhack.echomultiblockcore.api.AutoBuilderStep;
import com.knoxhack.echomultiblockcore.api.BuildAssistGeometry;
import com.knoxhack.echomultiblockcore.api.BuildAssistMaterialChecklist;
import com.knoxhack.echomultiblockcore.api.BuildAssistTransform;
import com.knoxhack.echomultiblockcore.api.ConstructionPermissionPolicy;
import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipeParseResult;
import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockDataCoreProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinitionParseResult;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockMaterialSummary;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionParseResult;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockPowerProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockRole;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.api.MultiblockTerminalProvider;
import com.knoxhack.echomultiblockcore.api.StructureBlockRequirement;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import com.knoxhack.echomultiblockcore.api.WorkcellType;
import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echomultiblockcore.content.AutomationRecipeJsonReloadListener;
import com.knoxhack.echomultiblockcore.content.MultiblockJsonReloadListener;
import com.knoxhack.echomultiblockcore.content.MultiblockProgressionJsonReloadListener;
import com.knoxhack.echomultiblockcore.event.MultiblockBrokenEvent;
import com.knoxhack.echomultiblockcore.event.MultiblockDamagedEvent;
import com.knoxhack.echomultiblockcore.event.MultiblockFormedEvent;
import com.knoxhack.echomultiblockcore.event.RoboticTaskCompletedEvent;
import com.knoxhack.echomultiblockcore.integration.DefaultMultiblockIntegrationProvider;
import com.knoxhack.echomultiblockcore.integration.MultiblockMachineCoreAdapter;
import com.knoxhack.echomultiblockcore.integration.MultiblockMachineCoreRuntimeProvider;
import com.knoxhack.echomultiblockcore.integration.MultiblockMissionCoreIntegration;
import com.knoxhack.echomultiblockcore.item.ToolHeadItem;
import com.knoxhack.echomultiblockcore.network.AutomationRecipeMetadataPacket;
import com.knoxhack.echomultiblockcore.runtime.AutoBuilderService;
import com.knoxhack.echomultiblockcore.runtime.MultiblockSavedData;
import com.knoxhack.echomultiblockcore.task.AutomationTransaction;
import com.knoxhack.echomultiblockcore.task.MultiblockTaskQueue;
import com.knoxhack.echomultiblockcore.validation.MultiblockValidationEngine;
import com.echoplatform.echocore.api.mission.InMemoryMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoMultiblockCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_PARSE =
            TEST_FUNCTIONS.register("json_definition_parse", () -> ModGameTests::jsonDefinitionParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VALIDATION_SUCCESS =
            TEST_FUNCTIONS.register("structure_validation_success", () -> ModGameTests::structureValidationSuccess);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSING_DIAGNOSTIC =
            TEST_FUNCTIONS.register("structure_validation_missing_block", () -> ModGameTests::structureValidationMissingBlock);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROTATION_MATCH =
            TEST_FUNCTIONS.register("structure_validation_rotation_match", () -> ModGameTests::structureValidationRotationMatch);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROBOT_TOOL =
            TEST_FUNCTIONS.register("robotic_arm_tool_requirement", () -> ModGameTests::roboticArmToolRequirement);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TASK_ITEMS =
            TEST_FUNCTIONS.register("task_queue_consume_produce", () -> ModGameTests::taskQueueConsumeProduce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_BAD_PALETTE =
            TEST_FUNCTIONS.register("json_definition_bad_palette_isolated", () -> ModGameTests::jsonBadPaletteIsolated);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_WARNINGS =
            TEST_FUNCTIONS.register("json_definition_warnings", () -> ModGameTests::jsonDefinitionWarnings);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_RECIPE_PARSE =
            TEST_FUNCTIONS.register("automation_recipe_parse", () -> ModGameTests::automationRecipeParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_RECIPE_BAD_ITEM =
            TEST_FUNCTIONS.register("automation_recipe_bad_item_isolated", () -> ModGameTests::automationRecipeBadItemIsolated);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_RECIPE_EFFECTS =
            TEST_FUNCTIONS.register("automation_recipe_effects_parse", () -> ModGameTests::automationRecipeEffectsParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_RECIPE_EFFECT_ONLY =
            TEST_FUNCTIONS.register("automation_recipe_effect_only_no_empty_warning", () -> ModGameTests::automationRecipeEffectOnlyNoEmptyWarning);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_RECIPE_BAD_EFFECT =
            TEST_FUNCTIONS.register("automation_recipe_bad_effect_warning", () -> ModGameTests::automationRecipeBadEffectWarning);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_EFFECT_DEDUPE =
            TEST_FUNCTIONS.register("automation_effect_provider_dedupe", () -> ModGameTests::automationEffectProviderDedupe);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_EFFECT_FAILURE =
            TEST_FUNCTIONS.register("automation_effect_failure_isolated", () -> ModGameTests::automationEffectFailureIsolated);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_EFFECT_NO_HANDLER =
            TEST_FUNCTIONS.register("automation_effect_no_handler_noop", () -> ModGameTests::automationEffectNoHandlerNoop);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_EFFECT_COMPLETE_ONCE =
            TEST_FUNCTIONS.register("automation_effect_completion_once", () -> ModGameTests::automationEffectCompletionOnce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TASK_BLOCKED_OUTPUT =
            TEST_FUNCTIONS.register("task_transaction_full_output_no_loss", () -> ModGameTests::taskTransactionFullOutputNoLoss);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TASK_QUEUE_STATES =
            TEST_FUNCTIONS.register("task_queue_state_snapshots", () -> ModGameTests::taskQueueStateSnapshots);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTEGRATION_DEDUPE =
            TEST_FUNCTIONS.register("integration_provider_deduplicates", () -> ModGameTests::integrationProviderDeduplicates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTEGRATION_FAILURE =
            TEST_FUNCTIONS.register("integration_provider_failure_isolated", () -> ModGameTests::integrationProviderFailureIsolated);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POWER_PROVIDER_DRAW =
            TEST_FUNCTIONS.register("power_provider_draw_and_failure_isolation", () -> ModGameTests::powerProviderDrawAndFailureIsolation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTEGRATION_SAVED_SNAPSHOTS =
            TEST_FUNCTIONS.register("integration_saved_data_snapshots", () -> ModGameTests::integrationSavedDataSnapshots);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTEGRATION_SCAN_ROBOT =
            TEST_FUNCTIONS.register("integration_scan_robotic_arm", () -> ModGameTests::integrationScanRoboticArm);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTEGRATION_MAP_MARKERS =
            TEST_FUNCTIONS.register("integration_map_marker_snapshots", () -> ModGameTests::integrationMapMarkerSnapshots);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTEGRATION_EVENT_SNAPSHOTS =
            TEST_FUNCTIONS.register("integration_event_snapshot_constructors", () -> ModGameTests::integrationEventSnapshotConstructors);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_MATERIALS =
            TEST_FUNCTIONS.register("build_assist_material_summary", () -> ModGameTests::buildAssistMaterialSummary);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_TRANSFORM =
            TEST_FUNCTIONS.register("build_assist_transform", () -> ModGameTests::buildAssistTransform);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_CONTROLLER_ANCHOR =
            TEST_FUNCTIONS.register("build_assist_controller_anchor", () -> ModGameTests::buildAssistControllerAnchor);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_OVERSIZED =
            TEST_FUNCTIONS.register("build_assist_oversized_snapshot", () -> ModGameTests::buildAssistOversizedSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_GEOMETRY_PARITY =
            TEST_FUNCTIONS.register("build_assist_geometry_parity", () -> ModGameTests::buildAssistGeometryParity);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_LAYER_WRAP =
            TEST_FUNCTIONS.register("build_assist_layer_wrap", () -> ModGameTests::buildAssistLayerWrap);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILD_ASSIST_MATERIAL_CHECKLIST =
            TEST_FUNCTIONS.register("build_assist_material_checklist", () -> ModGameTests::buildAssistMaterialChecklist);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION_JSON_PARSE =
            TEST_FUNCTIONS.register("progression_json_parse", () -> ModGameTests::progressionJsonParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION_BAD_FILE =
            TEST_FUNCTIONS.register("progression_bad_file_isolated", () -> ModGameTests::progressionBadFileIsolated);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION_CONTENT_COVERAGE =
            TEST_FUNCTIONS.register("progression_content_coverage", () -> ModGameTests::progressionContentCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHOWCASE_FACILITY_IDENTITY =
            TEST_FUNCTIONS.register("showcase_facility_identity_coverage", () -> ModGameTests::showcaseFacilityIdentityCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_METADATA_ALLOWED =
            TEST_FUNCTIONS.register("automation_metadata_allowed_multiblocks", () -> ModGameTests::automationMetadataAllowedMultiblocks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHOWCASE_FACILITY_FORMATION =
            TEST_FUNCTIONS.register("showcase_facility_formation_coverage", () -> ModGameTests::showcaseFacilityFormationCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FEATURED_RECIPE_TRANSACTIONS =
            TEST_FUNCTIONS.register("featured_recipe_transaction_coverage", () -> ModGameTests::featuredRecipeTransactionCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTO_BUILDER_REFUSAL_NO_LOSS =
            TEST_FUNCTIONS.register("autobuilder_occupied_refusal_no_loss", () -> ModGameTests::autoBuilderOccupiedRefusalNoLoss);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION_PREREQ_DIAGNOSTIC =
            TEST_FUNCTIONS.register("progression_prerequisite_diagnostics", () -> ModGameTests::progressionPrerequisiteDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROGRESSION_SNAPSHOT_FIELDS =
            TEST_FUNCTIONS.register("progression_snapshot_fields", () -> ModGameTests::progressionSnapshotFields);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
            TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_RUNTIME =
            TEST_FUNCTIONS.register("multiblock_machinecore_runtime_snapshot_contract", () -> ModGameTests::multiblockMachineCoreRuntimeSnapshotContract);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "json_definition_parse", JSON_PARSE.getId());
        register(event, "structure_validation_success", VALIDATION_SUCCESS.getId());
        register(event, "structure_validation_missing_block", MISSING_DIAGNOSTIC.getId());
        register(event, "structure_validation_rotation_match", ROTATION_MATCH.getId());
        register(event, "robotic_arm_tool_requirement", ROBOT_TOOL.getId());
        register(event, "task_queue_consume_produce", TASK_ITEMS.getId());
        register(event, "json_definition_bad_palette_isolated", JSON_BAD_PALETTE.getId());
        register(event, "json_definition_warnings", JSON_WARNINGS.getId());
        register(event, "automation_recipe_parse", AUTOMATION_RECIPE_PARSE.getId());
        register(event, "automation_recipe_bad_item_isolated", AUTOMATION_RECIPE_BAD_ITEM.getId());
        register(event, "automation_recipe_effects_parse", AUTOMATION_RECIPE_EFFECTS.getId());
        register(event, "automation_recipe_effect_only_no_empty_warning", AUTOMATION_RECIPE_EFFECT_ONLY.getId());
        register(event, "automation_recipe_bad_effect_warning", AUTOMATION_RECIPE_BAD_EFFECT.getId());
        register(event, "automation_effect_provider_dedupe", AUTOMATION_EFFECT_DEDUPE.getId());
        register(event, "automation_effect_failure_isolated", AUTOMATION_EFFECT_FAILURE.getId());
        register(event, "automation_effect_no_handler_noop", AUTOMATION_EFFECT_NO_HANDLER.getId());
        register(event, "automation_effect_completion_once", AUTOMATION_EFFECT_COMPLETE_ONCE.getId());
        register(event, "task_transaction_full_output_no_loss", TASK_BLOCKED_OUTPUT.getId());
        register(event, "task_queue_state_snapshots", TASK_QUEUE_STATES.getId());
        register(event, "integration_provider_deduplicates", INTEGRATION_DEDUPE.getId());
        register(event, "integration_provider_failure_isolated", INTEGRATION_FAILURE.getId());
        register(event, "power_provider_draw_and_failure_isolation", POWER_PROVIDER_DRAW.getId());
        register(event, "integration_saved_data_snapshots", INTEGRATION_SAVED_SNAPSHOTS.getId());
        register(event, "integration_scan_robotic_arm", INTEGRATION_SCAN_ROBOT.getId());
        register(event, "integration_map_marker_snapshots", INTEGRATION_MAP_MARKERS.getId());
        register(event, "integration_event_snapshot_constructors", INTEGRATION_EVENT_SNAPSHOTS.getId());
        register(event, "build_assist_material_summary", BUILD_ASSIST_MATERIALS.getId());
        register(event, "build_assist_transform", BUILD_ASSIST_TRANSFORM.getId());
        register(event, "build_assist_controller_anchor", BUILD_ASSIST_CONTROLLER_ANCHOR.getId());
        register(event, "build_assist_oversized_snapshot", BUILD_ASSIST_OVERSIZED.getId());
        register(event, "build_assist_geometry_parity", BUILD_ASSIST_GEOMETRY_PARITY.getId());
        register(event, "build_assist_layer_wrap", BUILD_ASSIST_LAYER_WRAP.getId());
        register(event, "build_assist_material_checklist", BUILD_ASSIST_MATERIAL_CHECKLIST.getId());
        register(event, "progression_json_parse", PROGRESSION_JSON_PARSE.getId());
        register(event, "progression_bad_file_isolated", PROGRESSION_BAD_FILE.getId());
        register(event, "progression_content_coverage", PROGRESSION_CONTENT_COVERAGE.getId());
        register(event, "showcase_facility_identity_coverage", SHOWCASE_FACILITY_IDENTITY.getId());
        register(event, "automation_metadata_allowed_multiblocks", AUTOMATION_METADATA_ALLOWED.getId());
        register(event, "showcase_facility_formation_coverage", SHOWCASE_FACILITY_FORMATION.getId());
        register(event, "featured_recipe_transaction_coverage", FEATURED_RECIPE_TRANSACTIONS.getId());
        register(event, "autobuilder_occupied_refusal_no_loss", AUTO_BUILDER_REFUSAL_NO_LOSS.getId());
        register(event, "progression_prerequisite_diagnostics", PROGRESSION_PREREQ_DIAGNOSTIC.getId());
        register(event, "progression_snapshot_fields", PROGRESSION_SNAPSHOT_FIELDS.getId());
        register(event, "missioncore_content_registration", MISSION_CORE_CONTENT.getId());
        register(event, "multiblock_machinecore_runtime_snapshot_contract", MACHINECORE_RUNTIME.getId());
    }

    private static void jsonDefinitionParse(GameTestHelper helper) {
        MultiblockDefinition definition = parse(twoBlockJson());
        helper.assertTrue(definition.width() == 2 && definition.height() == 1 && definition.depth() == 1,
                "Definition size should parse.");
        helper.assertTrue(definition.controllerLocalPosition().orElse(BlockPos.ZERO).equals(BlockPos.ZERO),
                "Controller slot should parse.");
        helper.succeed();
    }

    private static void structureValidationSuccess(GameTestHelper helper) {
        MultiblockDefinition definition = parse(twoBlockJson());
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MULTIBLOCK_CONTROLLER.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.REINFORCED_FRAME.get());
        ValidationResult result = MultiblockValidationEngine.validate(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), definition);
        helper.assertTrue(result.valid(), "Validation should succeed for exact two-block structure.");
        helper.succeed();
    }

    private static void structureValidationMissingBlock(GameTestHelper helper) {
        MultiblockDefinition definition = parse(twoBlockJson());
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MULTIBLOCK_CONTROLLER.get());
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.AIR);
        ValidationResult result = MultiblockValidationEngine.validate(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), definition);
        helper.assertTrue(!result.valid(), "Validation should fail when frame is missing.");
        helper.assertTrue(result.missingBlocks().size() == 1, "Missing diagnostics should include the frame slot.");
        helper.succeed();
    }

    private static void structureValidationRotationMatch(GameTestHelper helper) {
        MultiblockDefinition definition = parse(twoBlockJson());
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MULTIBLOCK_CONTROLLER.get());
        helper.setBlock(new BlockPos(1, 1, 2), ModBlocks.REINFORCED_FRAME.get());
        ValidationResult result = MultiblockValidationEngine.validate(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), definition);
        helper.assertTrue(result.valid(), "Validation should match a rotated structure.");
        helper.assertTrue(result.matchedRotation() == Rotation.CLOCKWISE_90 || result.matchedRotation() == Rotation.COUNTERCLOCKWISE_90,
                "A quarter rotation should be selected.");
        helper.succeed();
    }

    private static void roboticArmToolRequirement(GameTestHelper helper) {
        RoboticArmBlockEntity arm = new RoboticArmBlockEntity(BlockPos.ZERO, ModBlocks.ROBOTIC_ARM.get().defaultBlockState());
        MultiblockAutomationRecipe recipe = assemblyRecipe();
        helper.assertTrue(!arm.canPerform(recipe), "Arm without tool should not perform assembly.");
        arm.installTool(new ItemStack(ModItems.WELDER_HEAD.get()), null);
        helper.assertTrue(arm.canPerform(recipe), "Welder head should satisfy assembly task.");
        helper.assertTrue(arm.installedTool().getItem() instanceof ToolHeadItem, "Installed tool should persist as a tool head.");
        helper.succeed();
    }

    private static void taskQueueConsumeProduce(GameTestHelper helper) {
        MultiblockCrateBlockEntity input = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.INPUT_CRATE.get().defaultBlockState());
        MultiblockCrateBlockEntity output = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.OUTPUT_CRATE.get().defaultBlockState());
        helper.assertTrue(input.kind() == MultiblockCrateBlock.CrateKind.INPUT, "Input crate kind should be retained.");
        input.insertStack(new ItemStack(ModBlocks.REINFORCED_FRAME.asItem(), 4));
        input.insertStack(new ItemStack(ModBlocks.SIGNAL_CONDUIT.asItem(), 1));
        helper.assertTrue(input.consume(ModBlocks.REINFORCED_FRAME.asItem(), 4), "Frame input should be consumed.");
        helper.assertTrue(input.consume(ModBlocks.SIGNAL_CONDUIT.asItem(), 1), "Conduit input should be consumed.");
        output.insertStack(new ItemStack(ModBlocks.REINFORCED_MACHINE_FRAME.asItem(), 1));
        helper.assertTrue(output.countItem(ModBlocks.REINFORCED_MACHINE_FRAME.asItem()) == 1, "Output frame should be produced.");
        helper.succeed();
    }

    private static void jsonBadPaletteIsolated(GameTestHelper helper) {
        MultiblockDefinitionParseResult result = parseResult("""
                {
                  "id": "echomultiblockcore:bad_palette",
                  "display_name": "Bad Palette",
                  "size": [1, 1, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "layers": [["C"]],
                  "palette": {
                    "CTRL": {"block": "echomultiblockcore:multiblock_controller", "controller": true}
                  }
                }
                """);
        helper.assertTrue(!result.valid(), "Bad palette keys should be isolated as parse errors.");
        helper.assertTrue(result.definition() == null, "Invalid definitions should not be exposed.");
        helper.succeed();
    }

    private static void jsonDefinitionWarnings(GameTestHelper helper) {
        MultiblockDefinitionParseResult result = parseResult("""
                {
                  "id": "echomultiblockcore:warning_structure",
                  "display_name": "Warning Structure",
                  "role": "not-a-role",
                  "size": [1, 1, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "preview": {"color": "zzzzzz"},
                  "layers": [["C"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "X": {"block": "echomultiblockcore:reinforced_frame"}
                  },
                  "workcells": [
                    {"id": "echomultiblockcore:dup", "type": "assembly", "pos": [0,0,0]},
                    {"id": "echomultiblockcore:dup", "type": "bad-type", "pos": [0,0,0]}
                  ]
                }
                """);
        helper.assertTrue(!result.valid(), "Duplicate workcell ids should reject the definition.");
        helper.assertTrue(result.warnings().size() >= 3, "Invalid enum, color, and unused palette warnings should be reported.");
        helper.succeed();
    }

    private static void taskTransactionFullOutputNoLoss(GameTestHelper helper) {
        MultiblockCrateBlockEntity input = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.INPUT_CRATE.get().defaultBlockState());
        MultiblockCrateBlockEntity output = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.OUTPUT_CRATE.get().defaultBlockState());
        input.insertStack(new ItemStack(ModBlocks.REINFORCED_FRAME.asItem(), 4));
        input.insertStack(new ItemStack(ModBlocks.SIGNAL_CONDUIT.asItem(), 1));
        for (int i = 0; i < MultiblockCrateBlockEntity.SLOT_COUNT; i++) {
            output.insertStack(new ItemStack(Blocks.STONE, 64));
        }
        AutomationTransaction transaction = new AutomationTransaction(assemblyRecipe().inputs(), assemblyRecipe().outputs());
        AutomationTransaction.Commit commit = transaction.commit(input, output);
        helper.assertTrue(!commit.completed(), "Full output should block the transaction.");
        helper.assertTrue(input.countItem(ModBlocks.REINFORCED_FRAME.asItem()) == 4, "Frame input should not be lost.");
        helper.assertTrue(input.countItem(ModBlocks.SIGNAL_CONDUIT.asItem()) == 1, "Conduit input should not be lost.");
        helper.succeed();
    }

    private static void automationRecipeParse(GameTestHelper helper) {
        MultiblockAutomationRecipe recipe = AutomationRecipeJsonReloadListener.parseRecipeForTests(EchoMultiblockCore.id("test_recipe"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:test_recipe",
                          "display_name": "Test Recipe",
                          "category": "assembly",
                          "required_workcell": "ASSEMBLY",
                          "required_tools": ["WELDER"],
                          "duration_ticks": 80,
                          "inputs": [{"item": "echomultiblockcore:reinforced_frame", "count": 1}],
                          "outputs": [{"item": "echomultiblockcore:machine_casing", "count": 1}]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(recipe.inputs().size() == 1 && recipe.outputs().size() == 1, "Automation recipe IO should parse.");
        helper.assertTrue(recipe.requiredTools().contains(RobotToolType.WELDER), "Required tool should parse.");
        helper.succeed();
    }

    private static void automationRecipeBadItemIsolated(GameTestHelper helper) {
        MultiblockAutomationRecipeParseResult result = AutomationRecipeJsonReloadListener.parseRecipeResultForTests(EchoMultiblockCore.id("bad_recipe"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:bad_recipe",
                          "display_name": "Bad Recipe",
                          "required_workcell": "bad_workcell",
                          "required_tools": ["bad_tool"],
                          "inputs": [{"item": "echomultiblockcore:not_real", "count": 1}],
                          "outputs": [{"item": "echomultiblockcore:machine_casing", "count": 1}]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(!result.valid(), "Unknown recipe items should reject only the bad recipe.");
        helper.assertTrue(!result.warnings().isEmpty(), "Invalid enum fallbacks should be reported as warnings.");
        helper.succeed();
    }

    private static void automationRecipeEffectsParse(GameTestHelper helper) {
        MultiblockAutomationRecipe recipe = AutomationRecipeJsonReloadListener.parseRecipeForTests(EchoMultiblockCore.id("effect_recipe"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:effect_recipe",
                          "display_name": "Effect Recipe",
                          "required_workcell": "ASSEMBLY",
                          "required_tools": ["WELDER"],
                          "outputs": [{"item": "echomultiblockcore:machine_casing", "count": 1}],
                          "effects": ["echomultiblockcore:test_effect"]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(recipe.effects().equals(List.of(EchoMultiblockCore.id("test_effect"))), "Automation effects should parse additively.");
        helper.succeed();
    }

    private static void automationRecipeEffectOnlyNoEmptyWarning(GameTestHelper helper) {
        MultiblockAutomationRecipeParseResult result = AutomationRecipeJsonReloadListener.parseRecipeResultForTests(EchoMultiblockCore.id("effect_only_recipe"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:effect_only_recipe",
                          "display_name": "Effect Only Recipe",
                          "required_workcell": "ASSEMBLY",
                          "required_tools": ["WELDER"],
                          "effects": ["echomultiblockcore:test_effect"]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(result.valid(), "Effect-only recipes should parse.");
        helper.assertTrue(result.recipe().effects().equals(List.of(EchoMultiblockCore.id("test_effect"))),
                "Effect-only recipes should keep declared effects.");
        helper.assertTrue(result.warnings().stream().noneMatch(warning -> warning.contains("recipe has no inputs")),
                "Effect-only recipes should not be reported as empty.");
        helper.succeed();
    }

    private static void automationRecipeBadEffectWarning(GameTestHelper helper) {
        MultiblockAutomationRecipeParseResult result = AutomationRecipeJsonReloadListener.parseRecipeResultForTests(EchoMultiblockCore.id("bad_effect_recipe"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:bad_effect_recipe",
                          "display_name": "Bad Effect Recipe",
                          "outputs": [{"item": "echomultiblockcore:machine_casing", "count": 1}],
                          "effects": ["not_namespaced", "bad id"]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(result.valid(), "Malformed optional effects should not reject an otherwise valid recipe.");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("invalid automation effect id")),
                "Malformed effect ids should be reported as warnings.");
        helper.assertTrue(result.recipe().effects().isEmpty(), "Malformed effects should be ignored.");
        helper.succeed();
    }

    private static void automationEffectProviderDedupe(GameTestHelper helper) {
        AutomationEffectHandlers.withClearedForTests(() -> {
            AutomationEffectHandler first = new AutomationEffectHandler() {
                @Override
                public Identifier providerId() {
                    return EchoMultiblockCore.id("effect_test_provider");
                }
            };
            AutomationEffectHandler duplicate = new AutomationEffectHandler() {
                @Override
                public Identifier providerId() {
                    return EchoMultiblockCore.id("effect_test_provider");
                }
            };
            helper.assertTrue(AutomationEffectHandlers.register(first), "First effect provider should register.");
            helper.assertTrue(!AutomationEffectHandlers.register(duplicate), "Duplicate effect provider ids should be ignored.");
            helper.assertTrue(AutomationEffectHandlers.providerCount() == 1, "Only one provider should remain registered.");
        });
        helper.succeed();
    }

    private static void automationEffectFailureIsolated(GameTestHelper helper) {
        Identifier effectId = EchoMultiblockCore.id("throwing_effect");
        AutomationEffectHandlers.withClearedForTests(() -> {
            AutomationEffectHandlers.register(new AutomationEffectHandler() {
                @Override
                public Identifier providerId() {
                    return EchoMultiblockCore.id("throwing_provider");
                }

                @Override
                public boolean handles(Identifier candidate) {
                    return effectId.equals(candidate);
                }

                @Override
                public AutomationEffectResult beforeStart(AutomationEffectInvocation invocation) {
                    throw new IllegalStateException("boom");
                }
            });
            AutomationEffectResult result = AutomationEffectHandlers.beforeStart(effectInvocation(effectId, assemblyRecipe(), "before_start"));
            helper.assertTrue(result.failed(), "Throwing effect providers should fail only this invocation.");
            helper.assertTrue(result.reason().contains("throwing_effect"), "Failure diagnostics should name the effect.");
        });
        helper.succeed();
    }

    private static void automationEffectNoHandlerNoop(GameTestHelper helper) {
        AutomationEffectHandlers.withClearedForTests(() -> {
            AutomationEffectResult result = AutomationEffectHandlers.beforeStart(
                    effectInvocation(EchoMultiblockCore.id("missing_handler"), assemblyRecipe(), "before_start"));
            helper.assertTrue(result.allowed(), "Missing effect handlers should be a no-op.");
            helper.assertTrue(result.reason().contains("No automation effect handler registered"),
                    "Missing handlers should still leave a diagnostic.");
        });
        helper.succeed();
    }

    private static void automationEffectCompletionOnce(GameTestHelper helper) {
        Identifier effectId = EchoMultiblockCore.id("complete_once");
        AtomicInteger completions = new AtomicInteger();
        AutomationEffectHandlers.withClearedForTests(() -> {
            AutomationEffectHandlers.register(new AutomationEffectHandler() {
                @Override
                public Identifier providerId() {
                    return EchoMultiblockCore.id("complete_once_provider");
                }

                @Override
                public boolean handles(Identifier candidate) {
                    return effectId.equals(candidate);
                }

                @Override
                public AutomationEffectResult onComplete(AutomationEffectInvocation invocation) {
                    completions.incrementAndGet();
                    return AutomationEffectResult.allow();
                }
            });
            AutomationEffectResult result = AutomationEffectHandlers.onComplete(effectInvocation(effectId, assemblyRecipe(), "complete"));
            helper.assertTrue(result.allowed(), "Completion effect should allow the task.");
            helper.assertTrue(completions.get() == 1, "Completion effect should run exactly once for one invocation.");
        });
        helper.succeed();
    }

    private static void taskQueueStateSnapshots(GameTestHelper helper) {
        MultiblockTaskQueue queue = new MultiblockTaskQueue();
        queue.enqueue(EchoMultiblockCore.id("assemble_reinforced_machine_frame"), 42L)
                .orElseThrow()
                .block("Missing input.");
        helper.assertTrue(queue.nextRunnable().isPresent(), "Blocked tasks should remain runnable for retry.");
        helper.assertTrue(queue.snapshots().get(0).state() == MultiblockTaskState.BLOCKED, "Snapshot should expose blocked state.");
        helper.succeed();
    }

    private static void integrationProviderDeduplicates(GameTestHelper helper) {
        Identifier providerId = EchoMultiblockCore.id("test_terminal_provider");
        Identifier definitionId = EchoMultiblockCore.id("integration_test");
        MultiblockTerminalProvider first = new MultiblockTerminalProvider() {
            @Override
            public Identifier providerId() {
                return providerId;
            }

            @Override
            public List<MultiblockStatusSnapshot> snapshots(Player player) {
                return List.of(statusSnapshot(definitionId, BlockPos.ZERO));
            }
        };
        MultiblockTerminalProvider duplicate = new MultiblockTerminalProvider() {
            @Override
            public Identifier providerId() {
                return providerId;
            }

            @Override
            public List<MultiblockStatusSnapshot> snapshots(Player player) {
                return List.of(statusSnapshot(definitionId, new BlockPos(1, 0, 0)));
            }
        };
        MultiblockIntegrationServices.withClearedForTests(() -> {
            helper.assertTrue(MultiblockIntegrationServices.registerTerminalProvider(first), "First provider should register.");
            helper.assertTrue(!MultiblockIntegrationServices.registerTerminalProvider(duplicate), "Duplicate provider id should be rejected.");
            helper.assertTrue(MultiblockIntegrationServices.terminalProviderCount() == 1, "Only one terminal provider should remain registered.");
            helper.assertTrue(MultiblockIntegrationServices.terminalSnapshots(null).size() == 1, "Duplicate provider output should not be aggregated.");
        });
        helper.succeed();
    }

    private static void integrationProviderFailureIsolated(GameTestHelper helper) {
        MultiblockTerminalProvider failingTerminal = new MultiblockTerminalProvider() {
            @Override
            public Identifier providerId() {
                return EchoMultiblockCore.id("failing_terminal_provider");
            }

            @Override
            public List<MultiblockStatusSnapshot> snapshots(Player player) {
                throw new IllegalStateException("terminal failure");
            }
        };
        MultiblockDataCoreProvider failingData = new MultiblockDataCoreProvider() {
            @Override
            public Identifier providerId() {
                return EchoMultiblockCore.id("failing_data_provider");
            }

            @Override
            public List<MultiblockRuntimeSnapshot> snapshots(Player player) {
                throw new IllegalStateException("data failure");
            }
        };
        MultiblockIntegrationServices.withClearedForTests(() -> {
            helper.assertTrue(MultiblockIntegrationServices.registerTerminalProvider(failingTerminal), "Failing terminal provider should register.");
            helper.assertTrue(MultiblockIntegrationServices.registerDataCoreProvider(failingData), "Failing data provider should register.");
            helper.assertTrue(MultiblockIntegrationServices.terminalSnapshots(null).isEmpty(), "Failing terminal provider should aggregate as empty.");
            helper.assertTrue(MultiblockIntegrationServices.dataSnapshots(null).isEmpty(), "Failing data provider should aggregate as empty.");
        });
        helper.succeed();
    }

    private static void powerProviderDrawAndFailureIsolation(GameTestHelper helper) {
        long[] reserve = { 200L };
        MultiblockPowerProvider working = new MultiblockPowerProvider() {
            @Override
            public Identifier providerId() {
                return EchoMultiblockCore.id("working_power_provider");
            }

            @Override
            public long availablePower(net.minecraft.world.level.Level level, BlockPos controllerPos) {
                return reserve[0];
            }

            @Override
            public long drawPower(net.minecraft.world.level.Level level, BlockPos controllerPos, long ep, boolean simulate) {
                long drawn = Math.min(Math.max(0L, ep), reserve[0]);
                if (!simulate) {
                    reserve[0] -= drawn;
                }
                return drawn;
            }
        };
        MultiblockPowerProvider failing = new MultiblockPowerProvider() {
            @Override
            public Identifier providerId() {
                return EchoMultiblockCore.id("failing_power_provider");
            }

            @Override
            public long availablePower(net.minecraft.world.level.Level level, BlockPos controllerPos) {
                throw new IllegalStateException("availability failure");
            }

            @Override
            public long drawPower(net.minecraft.world.level.Level level, BlockPos controllerPos, long ep, boolean simulate) {
                throw new IllegalStateException("draw failure");
            }
        };
        MultiblockIntegrationServices.withClearedForTests(() -> {
            helper.assertTrue(MultiblockIntegrationServices.registerPowerProvider(failing), "Failing power provider should register.");
            helper.assertTrue(MultiblockIntegrationServices.registerPowerProvider(working), "Working power provider should register.");
            helper.assertTrue(MultiblockIntegrationServices.powerProviderCount() == 2, "Both power providers should be tracked.");
            helper.assertTrue(MultiblockIntegrationServices.availablePower(helper.getLevel(), BlockPos.ZERO) == 200L,
                    "Power availability should ignore failing providers and keep working output.");
            helper.assertTrue(MultiblockIntegrationServices.drawPower(helper.getLevel(), BlockPos.ZERO, 120L, true) == 120L,
                    "Simulated draw should report working provider output.");
            helper.assertTrue(reserve[0] == 200L, "Simulated draw should not mutate provider reserve.");
            helper.assertTrue(MultiblockIntegrationServices.drawPower(helper.getLevel(), BlockPos.ZERO, 80L, false) == 80L,
                    "Committed draw should debit working provider output.");
            helper.assertTrue(reserve[0] == 120L, "Committed draw should reduce provider reserve.");
        });
        helper.succeed();
    }

    private static void integrationSavedDataSnapshots(GameTestHelper helper) {
        Identifier definitionId = EchoMultiblockCore.id("signal_tower_tier_1");
        BlockPos controllerPos = helper.absolutePos(new BlockPos(2, 1, 2));
        MultiblockSavedData.get(helper.getLevel()).record(definitionId, controllerPos, 87.0F, "DAMAGED");

        List<MultiblockStatusSnapshot> terminalSnapshots = DefaultMultiblockIntegrationProvider.statusSnapshots(helper.getLevel());
        List<MultiblockRuntimeSnapshot> dataSnapshots = DefaultMultiblockIntegrationProvider.runtimeSnapshots(helper.getLevel());

        helper.assertTrue(terminalSnapshots.stream().anyMatch(snapshot ->
                definitionId.equals(snapshot.definitionId()) && snapshot.controllerPos().equals(controllerPos)
                        && snapshot.state() == MultiblockState.DAMAGED),
                "Default terminal provider should expose formed saved-data entries.");
        helper.assertTrue(dataSnapshots.stream().anyMatch(snapshot ->
                definitionId.equals(snapshot.definitionId()) && snapshot.controllerPos().equals(controllerPos)
                        && snapshot.dimension().equals(helper.getLevel().dimension())),
                "Default DataCore provider should expose formed saved-data entries with dimension.");
        helper.succeed();
    }

    private static void integrationScanRoboticArm(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        BlockPos worldPos = helper.absolutePos(relativePos);
        helper.setBlock(relativePos, ModBlocks.ROBOTIC_ARM.get());

        Optional<LensMultiblockScan> scan = DefaultMultiblockIntegrationProvider.SCAN.scan(null, helper.getLevel(), worldPos);
        helper.assertTrue(scan.isPresent(), "Default scan provider should scan robotic arms.");
        helper.assertTrue(scan.get().targetId().getNamespace().equals(EchoMultiblockCore.MODID)
                        && scan.get().targetId().getPath().startsWith("robotic_arm/"),
                "Robotic arm scan should use a stable runtime target id.");
        helper.assertTrue(!scan.get().roboticStatus().isEmpty(), "Robotic arm scan should include status text.");
        helper.succeed();
    }

    private static void integrationMapMarkerSnapshots(GameTestHelper helper) {
        Identifier definitionId = EchoMultiblockCore.id("industrial_assembly_line");
        BlockPos controllerPos = helper.absolutePos(new BlockPos(3, 1, 3));
        MultiblockSavedData.get(helper.getLevel()).record(definitionId, controllerPos, 96.0F, "ACTIVE");

        List<MultiblockMapMarkerSnapshot> markers = DefaultMultiblockIntegrationProvider.markerSnapshots(helper.getLevel());
        Optional<MultiblockMapMarkerSnapshot> marker = markers.stream()
                .filter(snapshot -> definitionId.equals(snapshot.definitionId()) && controllerPos.equals(snapshot.position()))
                .findFirst();

        helper.assertTrue(marker.isPresent(), "Default map provider should emit markers for saved runtimes.");
        helper.assertTrue(marker.get().markerId() != null, "Map marker should expose a stable marker id.");
        helper.assertTrue(marker.get().dimension().equals(helper.getLevel().dimension()), "Map marker should include dimension.");
        helper.assertTrue(marker.get().state() == MultiblockState.ACTIVE, "Map marker should include state.");
        helper.assertTrue(marker.get().color() != 0, "Map marker should include a display color.");
        helper.assertTrue(!marker.get().title().isBlank(), "Map marker should include a title.");
        helper.succeed();
    }

    private static void integrationEventSnapshotConstructors(GameTestHelper helper) {
        Identifier definitionId = EchoMultiblockCore.id("integration_test");
        Identifier taskId = EchoMultiblockCore.id("assemble_reinforced_machine_frame");
        MultiblockRuntimeSnapshot snapshot = runtimeSnapshot(definitionId, BlockPos.ZERO);
        TaskExecutionSnapshot taskSnapshot = new TaskExecutionSnapshot(taskId, "Assemble", MultiblockTaskState.ACTIVE,
                10, 20, BlockPos.ZERO, "", 0);

        RoboticTaskCompletedEvent legacyTask = new RoboticTaskCompletedEvent(null, definitionId, taskId, BlockPos.ZERO, BlockPos.ZERO);
        RoboticTaskCompletedEvent richTask = new RoboticTaskCompletedEvent(null, definitionId, taskId, BlockPos.ZERO, BlockPos.ZERO,
                taskSnapshot, snapshot, snapshot);
        MultiblockFormedEvent legacyFormed = new MultiblockFormedEvent(null, definitionId, BlockPos.ZERO, null);
        MultiblockFormedEvent richFormed = new MultiblockFormedEvent(null, definitionId, BlockPos.ZERO, null, snapshot);
        MultiblockBrokenEvent richBroken = new MultiblockBrokenEvent(null, definitionId, BlockPos.ZERO, snapshot);
        MultiblockDamagedEvent richDamaged = new MultiblockDamagedEvent(null, definitionId, BlockPos.ZERO, 55.0F,
                "integration_test", snapshot, snapshot);

        helper.assertTrue(legacyTask.beforeSnapshot == null && legacyTask.afterSnapshot == null,
                "Legacy task constructors should preserve null snapshots.");
        helper.assertTrue(richTask.beforeSnapshot == snapshot && richTask.afterSnapshot == snapshot,
                "Rich task constructors should expose before/after snapshots.");
        helper.assertTrue(legacyFormed.snapshot() != null, "Legacy formed constructor should still expose a snapshot.");
        helper.assertTrue(richFormed.snapshot() == snapshot, "Rich formed constructor should expose the supplied snapshot.");
        helper.assertTrue(richBroken.beforeSnapshot() == snapshot, "Broken events should expose the before snapshot.");
        helper.assertTrue(richDamaged.beforeSnapshot() == snapshot && richDamaged.afterSnapshot() == snapshot,
                "Damaged events should expose before/after snapshots.");
        helper.succeed();
    }

    private static void buildAssistMaterialSummary(GameTestHelper helper) {
        MultiblockMaterialSummary summary = MultiblockMaterialSummary.from(parse("""
                {
                  "id": "echomultiblockcore:assist_materials",
                  "display_name": "Assist Materials",
                  "size": [5, 1, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "layers": [["CTOAW"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "T": {"tag": "echomultiblockcore:reinforced_frames"},
                    "O": {"block": "echomultiblockcore:signal_conduit", "optional": true},
                    "A": {"air": true},
                    "W": {"wildcard": true}
                  }
                }
                """));
        helper.assertTrue(summary.entries().stream().anyMatch(entry -> entry.expected().equals("echomultiblockcore:multiblock_controller")),
                "Material summary should include exact/controller blocks.");
        helper.assertTrue(summary.entries().stream().anyMatch(entry -> entry.expected().equals("#echomultiblockcore:reinforced_frames")),
                "Material summary should include block tags.");
        helper.assertTrue(summary.entries().stream().anyMatch(entry -> entry.optional() && entry.expected().equals("echomultiblockcore:signal_conduit")),
                "Material summary should include optional slots.");
        helper.assertTrue(summary.entries().stream().anyMatch(entry -> entry.expected().equals("Air") && !entry.placeable()),
                "Material summary should include air slots as non-placeable.");
        helper.assertTrue(summary.entries().stream().anyMatch(entry -> entry.expected().equals("Any block") && !entry.placeable()),
                "Material summary should include wildcard slots as non-placeable.");
        helper.succeed();
    }

    private static void buildAssistTransform(GameTestHelper helper) {
        MultiblockDefinition definition = parse("""
                {
                  "id": "echomultiblockcore:assist_transform",
                  "display_name": "Assist Transform",
                  "size": [3, 1, 2],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "allowed_rotations": true,
                  "mirrorable": true,
                  "layers": [["CFF", "FSF"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "F": {"block": "echomultiblockcore:reinforced_frame"},
                    "S": {"block": "echomultiblockcore:signal_conduit"}
                  }
                }
                """);
        BlockPos controller = new BlockPos(10, 64, 10);
        BuildAssistTransform rotated = new BuildAssistTransform(Rotation.CLOCKWISE_90, false, -1);
        helper.assertTrue(rotated.localToWorld(definition, controller, new BlockPos(1, 0, 0)).equals(new BlockPos(10, 64, 11)),
                "Clockwise rotation should transform local +X into world +Z from the controller anchor.");
        BuildAssistTransform mirrored = new BuildAssistTransform(Rotation.NONE, true, -1);
        helper.assertTrue(mirrored.localToWorld(definition, controller, new BlockPos(2, 0, 0)).equals(new BlockPos(8, 64, 10)),
                "Mirrored transform should flip the local X axis around the anchored controller slot.");
        helper.succeed();
    }

    private static void buildAssistControllerAnchor(GameTestHelper helper) {
        MultiblockDefinition definition = parse("""
                {
                  "id": "echomultiblockcore:assist_anchor",
                  "display_name": "Assist Anchor",
                  "size": [3, 2, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "layers": [["FCF"], ["FFF"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "F": {"block": "echomultiblockcore:reinforced_frame"}
                  }
                }
                """);
        helper.assertTrue(definition.controllerLocalPosition().orElse(BlockPos.ZERO).equals(new BlockPos(1, 0, 0)),
                "Controller local-position should come from the JSON controller slot.");
        BuildAssistTransform transform = BuildAssistTransform.DEFAULT;
        helper.assertTrue(transform.localToWorld(definition, new BlockPos(20, 70, 20), new BlockPos(0, 0, 0))
                        .equals(new BlockPos(19, 70, 20)),
                "Build assist anchor should resolve other cells relative to the controller local slot.");
        helper.succeed();
    }

    private static void buildAssistOversizedSnapshot(GameTestHelper helper) {
        MultiblockDefinition definition = parse("""
                {
                  "id": "echomultiblockcore:assist_big",
                  "display_name": "Assist Big",
                  "size": [3, 3, 3],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "layers": [["CCC","CCC","CCC"],["CCC","CCC","CCC"],["CCC","CCC","CCC"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true}
                  }
                }
                """);
        MultiblockBuildAssistSnapshot snapshot = MultiblockBuildAssistSnapshot.from(definition, 8);
        helper.assertTrue(!snapshot.complete(), "Oversized build-assist snapshots should be marked incomplete.");
        helper.assertTrue(snapshot.cells().isEmpty(), "Oversized build-assist snapshots should not sync cell data.");
        helper.assertTrue(!snapshot.warning().isBlank(), "Oversized build-assist snapshots should include a warning.");
        helper.succeed();
    }

    private static void buildAssistGeometryParity(GameTestHelper helper) {
        MultiblockDefinition definition = parse("""
                {
                  "id": "echomultiblockcore:assist_geometry_parity",
                  "display_name": "Assist Geometry Parity",
                  "size": [3, 1, 2],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "allowed_rotations": true,
                  "mirrorable": true,
                  "layers": [["CFF", "FSF"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "F": {"block": "echomultiblockcore:reinforced_frame"},
                    "S": {"block": "echomultiblockcore:signal_conduit"}
                  }
                }
                """);
        MultiblockBuildAssistSnapshot snapshot = MultiblockBuildAssistSnapshot.from(definition, 64);
        BuildAssistTransform transform = new BuildAssistTransform(Rotation.CLOCKWISE_90, true, -1);
        BlockPos controller = new BlockPos(10, 64, 10);
        BlockPos local = new BlockPos(2, 0, 1);
        helper.assertTrue(BuildAssistGeometry.localToWorld(snapshot, transform, controller, local)
                        .equals(transform.localToWorld(definition, controller, local)),
                "Snapshot build-assist geometry should match definition-backed transforms.");
        helper.assertTrue(BuildAssistGeometry.localToWorld(snapshot, transform, controller, snapshot.controllerLocalPos()).equals(controller),
                "The transformed controller cell should remain anchored to the controller world position.");
        helper.succeed();
    }

    private static void buildAssistLayerWrap(GameTestHelper helper) {
        MultiblockDefinition definition = parse("""
                {
                  "id": "echomultiblockcore:assist_layers",
                  "display_name": "Assist Layers",
                  "size": [1, 3, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "layers": [["C"], ["C"], ["C"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true}
                  }
                }
                """);
        MultiblockBuildAssistSnapshot snapshot = MultiblockBuildAssistSnapshot.from(definition, 64);
        helper.assertTrue(BuildAssistGeometry.layerDelta(snapshot, BuildAssistTransform.DEFAULT, 1).layer() == 0,
                "Layer-up from all layers should start at layer zero.");
        helper.assertTrue(BuildAssistGeometry.layerDelta(snapshot, BuildAssistTransform.DEFAULT, -1).layer() == 2,
                "Layer-down from all layers should start at the top layer.");
        helper.assertTrue(BuildAssistGeometry.normalize(snapshot, new BuildAssistTransform(Rotation.NONE, false, 99)).layer() == 2,
                "Out-of-range layers should clamp to the final layer.");
        helper.assertTrue(BuildAssistGeometry.layerDelta(snapshot, new BuildAssistTransform(Rotation.NONE, false, 2), 1).layer() == -1,
                "Layer-up from the final layer should wrap back to all layers.");
        helper.succeed();
    }

    private static void buildAssistMaterialChecklist(GameTestHelper helper) {
        MultiblockMaterialSummary summary = MultiblockMaterialSummary.from(parse("""
                {
                  "id": "echomultiblockcore:assist_checklist",
                  "display_name": "Assist Checklist",
                  "size": [4, 1, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "layers": [["CFFT"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "F": {"block": "echomultiblockcore:reinforced_frame"},
                    "T": {"tag": "echomultiblockcore:reinforced_frames"}
                  }
                }
                """));
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().add(new ItemStack(ModBlocks.REINFORCED_FRAME.asItem(), 2));
        BuildAssistMaterialChecklist checklist = BuildAssistMaterialChecklist.from(summary, player.getInventory());
        helper.assertTrue(checklist.entries().stream().anyMatch(entry ->
                        entry.expected().equals("echomultiblockcore:reinforced_frame") && entry.available() == 2 && entry.missing() == 0),
                "Exact block requirements should count matching player inventory stacks.");
        helper.assertTrue(checklist.entries().stream().anyMatch(entry ->
                        entry.expected().equals("#echomultiblockcore:reinforced_frames") && !entry.counted()),
                "Tag requirements should remain requirement-only in the client checklist.");
        helper.succeed();
    }

    private static void progressionJsonParse(GameTestHelper helper) {
        MultiblockProgressionParseResult result = MultiblockProgressionJsonReloadListener.parseProgressionResultForTests(
                EchoMultiblockCore.id("progression_parse_fixture"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:progression_parse_fixture",
                          "facility": "echomultiblockcore:signal_tower_tier_1",
                          "tier": 1,
                          "featured_recipes": ["echomultiblockcore:tune_signal_beacon"],
                          "reward_items": ["echomultiblockcore:signal_circuit"],
                          "advancement": "echomultiblockcore:multiblock/signal_tower_tier_1",
                          "title": "Parse Fixture",
                          "guide": "A compact progression parser fixture."
                        }
                        """).getAsJsonObject());
        helper.assertTrue(result.valid(), "Well-formed progression JSON should parse.");
        helper.assertTrue(result.definition().tier() == 1, "Progression tier should parse.");
        helper.assertTrue(result.definition().featuredRecipes().contains(EchoMultiblockCore.id("tune_signal_beacon")),
                "Featured recipes should parse.");
        helper.succeed();
    }

    private static void progressionBadFileIsolated(GameTestHelper helper) {
        MultiblockProgressionParseResult result = MultiblockProgressionJsonReloadListener.parseProgressionResultForTests(
                EchoMultiblockCore.id("bad_progression_fixture"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:bad_progression_fixture",
                          "facility": "echomultiblockcore:signal_tower_tier_1",
                          "tier": 1,
                          "prerequisites": ["echomultiblockcore:signal_tower_tier_1"]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(!result.valid(), "Invalid progression data should be isolated.");
        helper.assertTrue(result.definition() == null, "Invalid progression definitions should not be exposed.");
        helper.assertTrue(result.errors().stream().anyMatch(error -> error.contains("cannot require itself")),
                "Self-prerequisites should produce a clear error.");
        helper.succeed();
    }

    private static void progressionContentCoverage(GameTestHelper helper) {
        List<Identifier> facilities = showcaseFacilities();
        List<Identifier> blueprints = List.of(
                EchoMultiblockCore.id("signal_tower_blueprint"),
                EchoMultiblockCore.id("industrial_assembly_line_blueprint"),
                EchoMultiblockCore.id("logistics_depot_blueprint"),
                EchoMultiblockCore.id("scanner_array_blueprint"),
                EchoMultiblockCore.id("vehicle_repair_gantry_blueprint"),
                EchoMultiblockCore.id("orbital_launch_platform_blueprint"),
                EchoMultiblockCore.id("archive_data_chamber_blueprint"),
                EchoMultiblockCore.id("agriculture_dome_blueprint"),
                EchoMultiblockCore.id("nexus_stabilizer_blueprint"),
                EchoMultiblockCore.id("armory_fabricator_blueprint"),
                EchoMultiblockCore.id("auto_builder_yard_blueprint"));

        for (Identifier facility : facilities) {
            Optional<MultiblockProgressionDefinition> progression = MultiblockProgressionRegistry.byFacility(facility);
            helper.assertTrue(progression.isPresent(), "Missing progression entry for " + facility + ".");
            helper.assertTrue(!progression.get().featuredRecipes().isEmpty(), "Progression entry needs a featured recipe: " + facility + ".");
            for (Identifier recipe : progression.get().featuredRecipes()) {
                helper.assertTrue(com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry.byId(recipe).isPresent(),
                        "Featured recipe should be loaded: " + recipe + ".");
            }
        }
        for (Identifier blueprint : blueprints) {
            helper.assertTrue(BuiltInRegistries.ITEM.getOptional(blueprint).isPresent(), "Missing blueprint item " + blueprint + ".");
        }
        helper.succeed();
    }

    private static void showcaseFacilityIdentityCoverage(GameTestHelper helper) {
        for (Identifier facility : showcaseFacilities()) {
            MultiblockDefinition definition = MultiblockContent.definition(facility)
                    .orElseThrow(() -> new AssertionError("Missing showcase definition " + facility + "."));
            helper.assertTrue(!definition.capabilityRequirements().isEmpty(),
                    "Showcase facility should declare capability requirements: " + facility + ".");
            helper.assertTrue(definition.upgradeSlotRules().size() >= 3,
                    "Showcase facility should expose at least three upgrade slots: " + facility + ".");
            helper.assertTrue(!definition.workcells().isEmpty(),
                    "Showcase facility should expose workcell metadata: " + facility + ".");
            helper.assertTrue(!definition.facilityStage().isBlank() && !definition.facilityRoute().isBlank()
                            && !definition.primaryUse().isBlank() && !definition.integrationHints().isEmpty(),
                    "Showcase facility should expose 1.3 route/stage/use/hint metadata: " + facility + ".");
            for (var workcell : definition.workcells()) {
                for (Identifier taskId : workcell.allowedTaskTypes()) {
                    MultiblockAutomationRecipe recipe = com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry.byId(taskId)
                            .orElseThrow(() -> new AssertionError("Workcell references missing recipe " + taskId + "."));
                    helper.assertTrue(recipe.allowsMultiblock(facility),
                            "Workcell recipe should allow its facility: " + taskId + " -> " + facility + ".");
                }
            }
            MultiblockProgressionDefinition progression = MultiblockProgressionRegistry.byFacility(facility)
                    .orElseThrow(() -> new AssertionError("Missing progression entry " + facility + "."));
            for (Identifier recipeId : progression.featuredRecipes()) {
                MultiblockAutomationRecipe recipe = com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry.byId(recipeId)
                        .orElseThrow(() -> new AssertionError("Missing featured recipe " + recipeId + "."));
                helper.assertTrue(recipe.allowsMultiblock(facility),
                        "Featured recipe should allow its progression facility: " + recipeId + " -> " + facility + ".");
            }
        }
        helper.succeed();
    }

    private static void automationMetadataAllowedMultiblocks(GameTestHelper helper) {
        AutomationRecipeMetadataPacket packet = AutomationRecipeMetadataPacket.current();
        AutomationRecipeMetadataPacket.Entry signalRecipe = packet.entries().stream()
                .filter(entry -> entry.id().equals(EchoMultiblockCore.id("tune_signal_beacon")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing signal recipe metadata."));
        helper.assertTrue(signalRecipe.allows(EchoMultiblockCore.id("signal_tower_tier_1")),
                "Recipe metadata should allow its owning facility.");
        helper.assertTrue(!signalRecipe.allows(EchoMultiblockCore.id("industrial_assembly_line")),
                "Recipe metadata should reject unrelated facilities.");
        helper.assertTrue(signalRecipe.allowedMultiblocks().contains("echomultiblockcore:signal_tower_tier_1"),
                "Recipe metadata should carry allowed multiblock ids for the controller UI.");
        helper.succeed();
    }

    private static void showcaseFacilityFormationCoverage(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(6, 2, 6);
        for (Identifier facility : showcaseFacilities()) {
            clearTestVolume(helper);
            MultiblockDefinition definition = MultiblockContent.definition(facility)
                    .orElseThrow(() -> new AssertionError("Missing showcase definition " + facility + "."));
            placeDefinition(helper, definition, anchor);

            ValidationResult result = MultiblockValidationEngine.validate(helper.getLevel(), helper.absolutePos(anchor), definition);
            helper.assertTrue(result.valid(), "Showcase facility should validate in-world: " + facility + " -> " + result.summaryLine());

            BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(anchor));
            helper.assertTrue(blockEntity instanceof MultiblockControllerBlockEntity,
                    "Placed showcase facility should create a controller block entity: " + facility + ".");
            MultiblockControllerBlockEntity controller = (MultiblockControllerBlockEntity) blockEntity;
            controller.setDefinitionId(facility, null);
            controller.onStructureFormed();
            helper.assertTrue(controller.getRuntime().isPresent(), "Formed showcase facility should expose runtime: " + facility + ".");
            helper.assertTrue(controller.runtimeSnapshot().definitionId().equals(facility),
                    "Runtime snapshot should retain the formed facility id: " + facility + ".");
        }
        helper.succeed();
    }

    private static void multiblockMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        clearTestVolume(helper);
        Identifier facility = EchoMultiblockCore.id("signal_tower_tier_1");
        MultiblockDefinition definition = MultiblockContent.definition(facility)
                .orElseThrow(() -> new AssertionError("Missing MachineCore test facility definition: " + facility));
        BlockPos anchor = new BlockPos(6, 2, 6);
        placeDefinition(helper, definition, anchor);

        ValidationResult result = MultiblockValidationEngine.validate(helper.getLevel(), helper.absolutePos(anchor), definition);
        helper.assertTrue(result.valid(), "MachineCore proof facility should validate before forming: " + result.summaryLine());

        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(anchor));
        helper.assertTrue(blockEntity instanceof MultiblockControllerBlockEntity,
                "MachineCore proof should place a real Multiblock controller block entity.");
        MultiblockControllerBlockEntity controller = (MultiblockControllerBlockEntity) blockEntity;
        controller.setDefinitionId(facility, null);
        controller.onStructureFormed();
        helper.assertTrue(controller.getRuntime().isPresent(), "Formed MachineCore proof facility should expose runtime.");

        EchoMachineRuntimeSnapshot direct = MultiblockMachineCoreAdapter.runtimeSnapshot(controller);
        helper.assertTrue(facility.toString().equals(direct.id().value())
                        && EchoMultiblockCore.MODID.equals(direct.ownerModule().value())
                        && EchoMultiblockCore.id("signal_tower_core").toString().equals(direct.machineBlockId())
                        && direct.kind() == EchoMachineKind.MULTIBLOCK,
                "Multiblock MachineCore adapter should publish the formed facility identity and real controller block id.");
        helper.assertTrue(direct.state() != EchoMachineState.LOCKED
                        && direct.state() != EchoMachineState.OFFLINE
                        && direct.state() != EchoMachineState.UNKNOWN,
                "Formed signal tower should publish a non-locked MachineCore state.");
        helper.assertTrue(direct.inventory().totalSlots() == 4
                        && direct.inventory().occupiedSlots() >= 1
                        && direct.inventory().slots().stream().anyMatch(slot -> "task_queue".equals(slot.role()))
                        && EchoMachineUiBridge.hasAutomationSurface(direct),
                "Multiblock MachineCore snapshot should expose controller, task, upgrade, and robotics lanes.");
        helper.assertTrue("echomultiblockcore:tune_signal_beacon".equals(direct.process().recipeContract())
                        && direct.process().attributes().containsKey("capabilitySummary")
                        && direct.process().attributes().containsKey("featuredRecipeSummary"),
                "Multiblock process contract should expose the facility's real featured automation recipe.");
        helper.assertTrue(direct.savedState().persistedKeys().contains("multiblock_id")
                        && direct.savedState().persistedKeys().contains("task_queue_size")
                        && direct.savedState().persistedKeys().contains("integrity")
                        && "no energy buffer".equals(EchoMachineUiBridge.energyLine(direct))
                        && EchoMachineUiBridge.position(direct).filter(controller.getBlockPos()::equals).isPresent(),
                "Multiblock snapshot should expose persisted runtime keys, no-buffer energy UI, and world position metadata.");

        MultiblockMachineCoreRuntimeProvider.register();
        EchoMachineRuntimeSnapshot registered = EchoMachineRuntimeRegistry.snapshot(helper.getLevel(), controller.getBlockPos())
                .orElseThrow(() -> new AssertionError("Registered MachineCore snapshot missing for Multiblock controller"));
        helper.assertTrue(registered.id().value().equals(direct.id().value())
                        && registered.process().recipeContract().equals(direct.process().recipeContract()),
                "MachineCore registry should discover the placed Multiblock controller through its runtime provider.");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(controller.getBlockPos().above().getCenter());
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> facility.toString().equals(snapshot.id().value())
                                && EchoMachineUiBridge.hasAutomationSurface(snapshot)),
                "Nearby player scan should discover Multiblock controller snapshots.");
        List<EchoMachineProfile> profiles = EchoMachineRuntimeRegistry.profiles(player);
        helper.assertTrue(profiles.stream().anyMatch(profile -> profile.id().value().equals(facility.toString())
                        && profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                                && "echomultiblockcore:tune_signal_beacon".equals(binding.recipeId().value()))),
                "MachineCore profiles should expose the Multiblock featured automation recipe binding.");
        helper.succeed();
    }

    private static void featuredRecipeTransactionCoverage(GameTestHelper helper) {
        for (Identifier facility : showcaseFacilities()) {
            MultiblockProgressionDefinition progression = MultiblockProgressionRegistry.byFacility(facility)
                    .orElseThrow(() -> new AssertionError("Missing progression entry " + facility + "."));
            Identifier recipeId = progression.featuredRecipes().stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing featured recipe for " + facility + "."));
            MultiblockAutomationRecipe recipe = com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry.byId(recipeId)
                    .orElseThrow(() -> new AssertionError("Missing featured automation recipe " + recipeId + "."));

            MultiblockCrateBlockEntity input = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.INPUT_CRATE.get().defaultBlockState());
            MultiblockCrateBlockEntity output = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.OUTPUT_CRATE.get().defaultBlockState());
            seedInputs(helper, input, recipe);

            AutomationTransaction.Commit commit = new AutomationTransaction(recipe.inputs(), recipe.outputs()).commit(input, output);
            helper.assertTrue(commit.completed(), "Featured recipe transaction should complete: " + facility + " -> " + recipeId
                    + " (" + commit.reason() + ").");
            for (AutomationIngredient ingredient : recipe.inputs()) {
                helper.assertTrue(input.countMatching(ingredient::matches) == 0,
                        "Featured recipe inputs should be consumed: " + recipeId + " / " + ingredient.summary() + ".");
            }
            for (AutomationOutput recipeOutput : recipe.outputs()) {
                ItemStack stack = recipeOutput.stack();
                helper.assertTrue(!stack.isEmpty(), "Featured recipe output item should resolve: " + recipeId + ".");
                helper.assertTrue(output.countItem(stack.getItem()) >= recipeOutput.count(),
                        "Featured recipe output should be produced: " + recipeId + " / " + recipeOutput.summary() + ".");
            }
        }
        helper.succeed();
    }

    private static void autoBuilderOccupiedRefusalNoLoss(GameTestHelper helper) {
        BlockPos relativeTarget = new BlockPos(2, 1, 2);
        BlockPos target = helper.absolutePos(relativeTarget);
        AutoBuilderPlan plan = new AutoBuilderPlan(
                EchoMultiblockCore.id("autobuilder_refusal_fixture"),
                helper.absolutePos(new BlockPos(1, 1, 1)),
                List.of(new AutoBuilderStep(target, EchoMultiblockCore.id("reinforced_frame"), "Reinforced Frame", false)),
                1,
                ConstructionPermissionPolicy.OPERATOR_ONLY);
        MultiblockCrateBlockEntity input = new MultiblockCrateBlockEntity(BlockPos.ZERO, ModBlocks.INPUT_CRATE.get().defaultBlockState());
        input.insertStack(new ItemStack(ModBlocks.REINFORCED_FRAME.asItem(), 1));

        helper.setBlock(relativeTarget, Blocks.STONE);
        AutoBuilderResult blocked = AutoBuilderService.execute(helper.getLevel(), plan, input, null, 1);
        helper.assertTrue(!blocked.success(), "Auto Builder should refuse occupied/wrong cells.");
        helper.assertTrue(input.countItem(ModBlocks.REINFORCED_FRAME.asItem()) == 1,
                "Auto Builder should not consume materials when refusing an occupied/wrong cell.");

        helper.setBlock(relativeTarget, Blocks.AIR);
        AutoBuilderResult placed = AutoBuilderService.execute(helper.getLevel(), plan, input, null, 1);
        helper.assertTrue(placed.success() && placed.placedBlocks() == 1, "Auto Builder should place the exact planned block.");
        helper.assertTrue(helper.getLevel().getBlockState(target).is(ModBlocks.REINFORCED_FRAME.get()),
                "Auto Builder should place the reinforced frame at the planned position.");
        helper.assertTrue(input.countItem(ModBlocks.REINFORCED_FRAME.asItem()) == 0,
                "Auto Builder should consume exactly one material after successful placement.");
        helper.succeed();
    }

    private static void progressionPrerequisiteDiagnostics(GameTestHelper helper) {
        MultiblockProgressionParseResult result = MultiblockProgressionJsonReloadListener.parseProgressionResultForTests(
                EchoMultiblockCore.id("progression_missing_prereq_fixture"),
                JsonParser.parseString("""
                        {
                          "id": "echomultiblockcore:progression_missing_prereq_fixture",
                          "facility": "echomultiblockcore:signal_tower_tier_1",
                          "tier": 99,
                          "prerequisites": ["echomultiblockcore:not_real_facility"],
                          "featured_recipes": ["echomultiblockcore:not_real_recipe"],
                          "reward_items": ["echomultiblockcore:not_real_reward"]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(result.valid(), "Missing optional progression links should warn, not reject the file.");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("$.prerequisites")),
                "Missing prerequisite diagnostics should be reported.");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("$.featured_recipes")),
                "Missing featured recipe diagnostics should be reported.");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("$.reward_items")),
                "Missing reward item diagnostics should be reported.");
        helper.succeed();
    }

    private static void progressionSnapshotFields(GameTestHelper helper) {
        MultiblockProgressionDefinition progression = MultiblockProgressionRegistry
                .byFacility(EchoMultiblockCore.id("signal_tower_tier_1"))
                .orElseThrow();
        MultiblockProgressionSnapshot progressionSnapshot = MultiblockProgressionSnapshot.from(progression, true, "Featured path ready.");
        helper.assertTrue(progressionSnapshot.tier() == progression.tier(), "Progression snapshot should expose tier.");
        helper.assertTrue(progressionSnapshot.featuredRecipes().equals(progression.featuredRecipes()),
                "Progression snapshot should expose featured recipes.");

        MultiblockStatusSnapshot status = new MultiblockStatusSnapshot(
                progression.facilityId(),
                progression.title(),
                MultiblockState.FORMED,
                100.0F,
                1.0D,
                BlockPos.ZERO,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                progression.title(),
                progressionSnapshot.completionHint());
        MultiblockRuntimeSnapshot runtime = new MultiblockRuntimeSnapshot(
                progression.facilityId(),
                BlockPos.ZERO,
                MultiblockState.FORMED,
                100.0F,
                1.0D,
                4,
                1,
                List.of(),
                List.of(),
                1L,
                net.minecraft.world.level.Level.OVERWORLD,
                progression.title(),
                "progression",
                MultiblockRole.INFRASTRUCTURE,
                0xFF00D8FF,
                0,
                0,
                com.knoxhack.echomultiblockcore.api.MultiblockCapabilityRuntime.EMPTY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                progression.id(),
                progression.tier(),
                progression.title(),
                progression.featuredRecipeSummary());
        helper.assertTrue(status.progressionTitle().equals(progression.title()), "Status snapshots should expose progression titles.");
        helper.assertTrue(runtime.progressionId().equals(progression.id()), "Runtime snapshots should expose progression ids.");
        helper.assertTrue(runtime.progressionTier() == progression.tier(), "Runtime snapshots should expose progression tier.");
        helper.assertTrue(runtime.facilityTier() == progression.tier(), "Runtime snapshots should mirror facility tier.");
        helper.assertTrue(!runtime.featuredRecipeSummary().isBlank(), "Runtime snapshots should expose featured recipe summaries.");
        MultiblockDefinition signal = MultiblockContent.definition(EchoMultiblockCore.id("signal_tower_tier_1")).orElseThrow();
        MultiblockRuntimeSnapshot routeRuntime = new MultiblockRuntimeSnapshot(
                signal.id(), BlockPos.ZERO, MultiblockState.FORMED, 100.0F, 1.0D, 4, 1,
                List.of(), List.of(), 1L, net.minecraft.world.level.Level.OVERWORLD, signal.displayName(),
                signal.category(), signal.role(), signal.previewColor(), 0, 0,
                com.knoxhack.echomultiblockcore.api.MultiblockCapabilityRuntime.EMPTY, List.of(), List.of(),
                List.of(), List.of(), "", progression.id(), progression.tier(), progression.title(),
                progression.featuredRecipeSummary(), progression.tier(), signal.facilityStage(),
                signal.facilityRoute(), signal.primaryUse(), "structure",
                EchoMultiblockCore.id("tune_signal_beacon"), "BLOCKED", signal.integrationHints());
        helper.assertTrue(routeRuntime.facilityRoute().equals(signal.facilityRoute()),
                "Runtime snapshots should expose facility route metadata.");
        helper.assertTrue(routeRuntime.blockedReasonCode().equals("structure"),
                "Runtime snapshots should expose blocked reason codes.");
        helper.assertTrue(routeRuntime.lastTaskId().equals(EchoMultiblockCore.id("tune_signal_beacon")),
                "Runtime snapshots should expose last task ids.");
        helper.assertTrue(!routeRuntime.integrationHints().isEmpty(),
                "Runtime snapshots should expose integration hints.");
        helper.succeed();
    }

    private static AutomationEffectInvocation effectInvocation(Identifier effectId, MultiblockAutomationRecipe recipe, String phase) {
        return new AutomationEffectInvocation(
                null,
                null,
                BlockPos.ZERO,
                null,
                effectId,
                recipe,
                null,
                recipe == null ? null : new TaskExecutionSnapshot(
                        recipe.id(),
                        recipe.displayName(),
                        MultiblockTaskState.WAITING,
                        0,
                        recipe.durationTicks(),
                        BlockPos.ZERO,
                        "",
                        0,
                        recipe.category().toString(),
                        null,
                        null,
                        recipe.inputSummary(),
                        recipe.outputSummary(),
                        recipe.effects(),
                        ""),
                phase);
    }

    private static MultiblockAutomationRecipe assemblyRecipe() {
        return new MultiblockAutomationRecipe(
                EchoMultiblockCore.id("assemble_reinforced_machine_frame"),
                "Assemble Reinforced Machine Frame",
                EchoMultiblockCore.id("assembly"),
                List.of(EchoMultiblockCore.id("industrial_assembly_line")),
                WorkcellType.ASSEMBLY,
                List.of(RobotToolType.WELDER),
                List.of(
                        AutomationIngredient.item(EchoMultiblockCore.id("reinforced_frame"), 4),
                        AutomationIngredient.item(EchoMultiblockCore.id("signal_conduit"), 1)),
                List.of(new AutomationOutput(EchoMultiblockCore.id("reinforced_machine_frame"), 1, "")),
                false,
                120,
                2,
                "assemble",
                0,
                List.of("GameTest fixture recipe"));
    }

    private static MultiblockDefinition parse(String json) {
        return MultiblockJsonReloadListener.parseDefinitionForTests(EchoMultiblockCore.id("test_structure"),
                JsonParser.parseString(json).getAsJsonObject());
    }

    private static void missionCoreContentRegistration(GameTestHelper helper) {
        InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
        MultiblockMissionCoreIntegration.registerContent(registry);
        helper.assertTrue(registry.chapter(EchoMultiblockCore.id("multiblock_core")).isPresent(),
                "MultiblockCore MissionCore chapter should be owned by MultiblockCore.");
        assertMission(helper, registry, "validate_first_structure", "validate", MissionObjectiveType.BUILD_MULTIBLOCK);
        assertMission(helper, registry, "install_robot_tool", "tool", MissionObjectiveType.REPAIR_MACHINE);
        assertMission(helper, registry, "complete_automation_task", "task", MissionObjectiveType.CUSTOM);
        assertMission(helper, registry, "repair_integrity", "repair", MissionObjectiveType.REPAIR_MACHINE);
        assertMission(helper, registry, "use_auto_builder", "builder", MissionObjectiveType.BUILD_MULTIBLOCK);
        helper.succeed();
    }

    private static void assertMission(
            GameTestHelper helper,
            InMemoryMissionRegistry registry,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type) {
        Identifier missionId = EchoMultiblockCore.id(missionPath);
        MissionDefinition mission = registry.missionDefinition(missionId)
                .orElseThrow(() -> new AssertionError("Missing MissionCore mission: " + missionId));
        helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "MultiblockCore MissionCore missions should be side ops.");
        helper.assertTrue(!mission.rewards().isEmpty(), "MultiblockCore MissionCore mission should have a claimable reward: " + missionId);
        helper.assertTrue(mission.objectives().size() == 1, "MultiblockCore MissionCore mission should have one direct objective: " + missionId);
        helper.assertTrue(mission.objectives().getFirst().type() == type, "MultiblockCore objective type should stay stable: " + missionId);
        String target = mission.objectives().getFirst().criteria().get("target");
        helper.assertTrue(MissionHookTargets.objectiveTarget(EchoMultiblockCore.MODID, missionId, objectiveKey).toString().equals(target),
                "MultiblockCore MissionCore objective target should use MissionHookTargets: " + missionId);
    }

    private static MultiblockStatusSnapshot statusSnapshot(Identifier definitionId, BlockPos pos) {
        return new MultiblockStatusSnapshot(definitionId, "Integration Test", MultiblockState.FORMED, 100.0F,
                1.0D, pos, List.of(), List.of(), List.of(), List.of());
    }

    private static MultiblockRuntimeSnapshot runtimeSnapshot(Identifier definitionId, BlockPos pos) {
        return new MultiblockRuntimeSnapshot(
                definitionId,
                pos,
                MultiblockState.FORMED,
                91.0F,
                1.0D,
                4,
                1,
                List.of(),
                List.of("integration warning"),
                123L,
                net.minecraft.world.level.Level.OVERWORLD,
                "Integration Test",
                "integration",
                MultiblockRole.INFRASTRUCTURE,
                0xFF00D8FF,
                0,
                1);
    }

    private static void clearTestVolume(GameTestHelper helper) {
        for (int x = 0; x <= 13; x++) {
            for (int y = 0; y <= 6; y++) {
                for (int z = 0; z <= 13; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void placeDefinition(GameTestHelper helper, MultiblockDefinition definition, BlockPos controllerAnchor) {
        BlockPos controllerLocal = definition.controllerLocalPosition().orElse(BlockPos.ZERO);
        for (int y = 0; y < definition.height(); y++) {
            for (int z = 0; z < definition.depth(); z++) {
                for (int x = 0; x < definition.width(); x++) {
                    StructureBlockRequirement requirement = definition.requirementAt(x, y, z);
                    Block block = blockForRequirement(requirement);
                    BlockPos relative = controllerAnchor.offset(x - controllerLocal.getX(), y - controllerLocal.getY(),
                            z - controllerLocal.getZ());
                    helper.setBlock(relative, block);
                }
            }
        }
    }

    private static Block blockForRequirement(StructureBlockRequirement requirement) {
        if (requirement == null) {
            return Blocks.AIR;
        }
        return switch (requirement.kind()) {
            case AIR, WILDCARD -> Blocks.AIR;
            case BLOCK_TAG -> ModBlocks.REINFORCED_FRAME.get();
            case BLOCK_LIST -> requirement.blocks().stream()
                    .map(BuiltInRegistries.BLOCK::getOptional)
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElse(Blocks.AIR);
            case EXACT_BLOCK, CONTROLLER, COMPONENT, ROBOTICS, UPGRADE -> requirement.block() == null
                    ? Blocks.AIR
                    : BuiltInRegistries.BLOCK.getOptional(requirement.block()).orElse(Blocks.AIR);
        };
    }

    private static void seedInputs(GameTestHelper helper, MultiblockCrateBlockEntity input, MultiblockAutomationRecipe recipe) {
        for (AutomationIngredient ingredient : recipe.inputs()) {
            List<ItemStack> examples = ingredient.exampleStacks();
            helper.assertTrue(!examples.isEmpty(), "Featured recipe ingredients must have concrete examples for smoke tests: "
                    + recipe.id() + " / " + ingredient.summary() + ".");
            ItemStack stack = examples.getFirst().copy();
            stack.setCount(ingredient.count());
            int inserted = input.insertStack(stack);
            helper.assertTrue(inserted == ingredient.count(), "Featured recipe input should fit in the test crate: "
                    + recipe.id() + " / " + ingredient.summary() + ".");
        }
    }

    private static MultiblockDefinitionParseResult parseResult(String json) {
        return MultiblockJsonReloadListener.parseDefinitionResultForTests(EchoMultiblockCore.id("test_structure"),
                JsonParser.parseString(json).getAsJsonObject());
    }

    private static String twoBlockJson() {
        return """
                {
                  "id": "echomultiblockcore:test_structure",
                  "display_name": "Test Structure",
                  "role": "INFRASTRUCTURE",
                  "size": [2, 1, 1],
                  "controller": "echomultiblockcore:multiblock_controller",
                  "allowed_rotations": true,
                  "layers": [["CF"]],
                  "palette": {
                    "C": {"block": "echomultiblockcore:multiblock_controller", "controller": true},
                    "F": {"block": "echomultiblockcore:reinforced_frame"}
                  }
                }
                """;
    }

    private static List<Identifier> showcaseFacilities() {
        return List.of(
                EchoMultiblockCore.id("signal_tower_tier_1"),
                EchoMultiblockCore.id("industrial_assembly_line"),
                EchoMultiblockCore.id("logistics_depot"),
                EchoMultiblockCore.id("scanner_array"),
                EchoMultiblockCore.id("vehicle_repair_gantry"),
                EchoMultiblockCore.id("orbital_launch_platform"),
                EchoMultiblockCore.id("archive_data_chamber"),
                EchoMultiblockCore.id("agriculture_dome"),
                EchoMultiblockCore.id("nexus_stabilizer"),
                EchoMultiblockCore.id("armory_fabricator"),
                EchoMultiblockCore.id("auto_builder_yard"));
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(EchoMultiblockCore.id("multiblock_" + testName));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1,
                false, 16);
        event.registerTest(EchoMultiblockCore.id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoMultiblockCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }
}
