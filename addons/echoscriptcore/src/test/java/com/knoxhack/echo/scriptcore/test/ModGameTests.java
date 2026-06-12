package com.knoxhack.echo.scriptcore.test;

import com.google.gson.JsonParser;
import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.api.EchoActionResult;
import com.knoxhack.echo.scriptcore.api.EchoConditionResult;
import com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptExecutionContext;
import com.knoxhack.echo.scriptcore.api.EchoScriptLoadResult;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeMigrationReport;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeSnapshot;
import com.knoxhack.echo.scriptcore.client.screencore.ScriptCoreScreenCoreBridge;
import com.knoxhack.echo.scriptcore.client.screencore.ScriptCoreScreenCoreClientState;
import com.knoxhack.echo.scriptcore.client.terminal.ScriptCoreTerminalTab;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreRuntimeStateService;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataService;
import com.echoplatform.echocore.api.NoOpDataService;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echo.scriptcore.loader.EchoScriptLoader;
import com.knoxhack.echo.scriptcore.loader.EchoScriptParser;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloader;
import com.knoxhack.echo.scriptcore.model.EchoArchiveEntryDefinition;
import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echo.scriptcore.util.EchoJson;
import com.knoxhack.echo.scriptcore.validation.EchoScriptValidator;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreUiExecutionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoScriptCore.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VALID_PARSE =
            TEST_FUNCTIONS.register("valid_definition_parse", () -> ModGameTests::validDefinitionParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOADER_DIAGNOSTICS =
            TEST_FUNCTIONS.register("loader_diagnostics", () -> ModGameTests::loaderDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VALIDATOR_DIAGNOSTICS =
            TEST_FUNCTIONS.register("validator_diagnostics", () -> ModGameTests::validatorDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ADAPTER_AND_RELOAD =
            TEST_FUNCTIONS.register("adapter_and_scoped_reload", () -> ModGameTests::adapterAndScopedReload);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSIONCORE_LIFECYCLE =
            TEST_FUNCTIONS.register("missioncore_source_lifecycle", () -> ModGameTests::missionCoreSourceLifecycle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNTIME_STORAGE =
            TEST_FUNCTIONS.register("runtime_storage_roundtrip", () -> ModGameTests::runtimeStorageRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCREENCORE_UI_ACTIONS =
            TEST_FUNCTIONS.register("screencore_ui_action_execution", () -> ModGameTests::screenCoreUiActionExecution);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_AND_DIAGNOSTICS =
            TEST_FUNCTIONS.register("terminal_and_diagnostics", () -> ModGameTests::terminalAndDiagnostics);

    private static final EchoScriptParser PARSER = new EchoScriptParser();

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("scriptcore_framework"));
        register(event, environment, "valid_definition_parse", VALID_PARSE.getId());
        register(event, environment, "loader_diagnostics", LOADER_DIAGNOSTICS.getId());
        register(event, environment, "validator_diagnostics", VALIDATOR_DIAGNOSTICS.getId());
        register(event, environment, "adapter_and_scoped_reload", ADAPTER_AND_RELOAD.getId());
        register(event, environment, "missioncore_source_lifecycle", MISSIONCORE_LIFECYCLE.getId());
        register(event, environment, "runtime_storage_roundtrip", RUNTIME_STORAGE.getId());
        register(event, environment, "screencore_ui_action_execution", SCREENCORE_UI_ACTIONS.getId());
        register(event, environment, "terminal_and_diagnostics", TERMINAL_AND_DIAGNOSTICS.getId());
    }

    private static void validDefinitionParse(GameTestHelper helper) {
        EchoScriptDefinitionView mission = parse("""
                {
                  "schema_version": 1,
                  "pack": "scriptcore_test",
                  "id": "scriptcore_test:repair_radio",
                  "type": "mission",
                  "title": "Repair Radio",
                  "objectives": [
                    { "id": "iron", "type": "collect_item", "item": "minecraft:iron_ingot", "count": 2 }
                  ],
                  "on_complete": [{ "type": "noop" }]
                }
                """);
        helper.assertTrue(mission instanceof EchoMissionDefinition parsedMission
                        && parsedMission.objectives().size() == 1
                        && parsedMission.objectives().getFirst().item().orElseThrow().equals(Identifier.withDefaultNamespace("iron_ingot")),
                "Valid mission JSON should parse typed mission data.");

        EchoScriptDefinitionView archive = parse("""
                {
                  "schema_version": 1,
                  "pack": "scriptcore_test",
                  "id": "scriptcore_test:first_signal",
                  "type": "archive_entry",
                  "title": "First Signal",
                  "content": ["A readable archive line."]
                }
                """);
        helper.assertTrue(archive instanceof EchoArchiveEntryDefinition parsedArchive
                        && parsedArchive.content().size() == 1,
                "Valid archive JSON should parse typed archive data.");
        helper.succeed();
    }

    private static void loaderDiagnostics(GameTestHelper helper) {
        try {
            Path root = Files.createTempDirectory("scriptcore_loader");
            write(root.resolve("pack/missions/valid.json"), """
                    { "schema_version": 1, "id": "loader_test:valid", "type": "generic" }
                    """);
            write(root.resolve("pack/missions/missing_id.json"), """
                    { "schema_version": 1, "type": "generic" }
                    """);
            write(root.resolve("pack/missions/invalid_id.json"), """
                    { "schema_version": 1, "id": "Bad Id", "type": "generic" }
                    """);
            write(root.resolve("pack/missions/malformed.json"), "{ this is not json");
            write(root.resolve("pack/missions/duplicate_a.json"), """
                    { "schema_version": 1, "id": "loader_test:duplicate", "type": "generic" }
                    """);
            write(root.resolve("pack/missions/duplicate_b.json"), """
                    { "schema_version": 1, "id": "loader_test:duplicate", "type": "generic" }
                    """);

            EchoScriptLoadResult result = EchoScriptLoader.INSTANCE.load(root);
            helper.assertTrue(hasCode(result.diagnostics(), "SCRIPTCORE_MISSING_REQUIRED_FIELD"), "Missing id should be diagnosed.");
            helper.assertTrue(hasCode(result.diagnostics(), "SCRIPTCORE_INVALID_ID"), "Invalid id should be diagnosed.");
            helper.assertTrue(hasCode(result.diagnostics(), "SCRIPTCORE_JSON_PARSE_ERROR"), "Malformed JSON should be diagnosed.");
            helper.assertTrue(hasCode(result.diagnostics(), "SCRIPTCORE_DUPLICATE_ID"), "Duplicate ids should be diagnosed.");
            helper.succeed();
        } catch (IOException exception) {
            helper.fail("Loader diagnostics setup failed: " + exception.getMessage());
        }
    }

    private static void validatorDiagnostics(GameTestHelper helper) {
        List<EchoScriptDefinitionView> definitions = List.of(
                parse("""
                        {
                          "schema_version": 1,
                          "pack": "scriptcore_test",
                          "id": "scriptcore_test:a",
                          "type": "mission",
                          "title": "A",
                          "objectives": [{ "id": "missing_item", "type": "collect_item" }],
                          "prerequisites": [{ "type": "mission_complete", "mission": "scriptcore_test:b" }],
                          "actions": [{ "type": "mystery_action" }],
                          "conditions": [{ "type": "mystery_condition" }]
                        }
                        """),
                parse("""
                        {
                          "schema_version": 1,
                          "pack": "scriptcore_test",
                          "id": "scriptcore_test:b",
                          "type": "mission",
                          "title": "B",
                          "objectives": [{ "id": "ok", "type": "collect_item", "item": "minecraft:redstone" }],
                          "prerequisites": [{ "type": "mission_complete", "mission": "scriptcore_test:a" }]
                        }
                        """),
                parse("""
                        {
                          "schema_version": 1,
                          "pack": "scriptcore_test",
                          "id": "scriptcore_test:faction",
                          "type": "faction",
                          "ranks": [
                            { "name": "High", "min": 10, "max": 5 },
                            { "name": "Low", "min": 4 }
                          ]
                        }
                        """),
                parse("""
                        {
                          "schema_version": 1,
                          "pack": "scriptcore_test",
                          "id": "scriptcore_test:bad_weather",
                          "type": "weather_event",
                          "duration_ticks": 0
                        }
                        """));
        List<EchoScriptDiagnostic> diagnostics = EchoScriptValidator.INSTANCE.validate(definitions);
        helper.assertTrue(hasCode(diagnostics, "SCRIPTCORE_INVALID_OBJECTIVE"), "Invalid objective should be diagnosed.");
        helper.assertTrue(hasCode(diagnostics, "SCRIPTCORE_CIRCULAR_PREREQUISITE"), "Circular prerequisites should be diagnosed.");
        helper.assertTrue(hasCode(diagnostics, "SCRIPTCORE_UNKNOWN_ACTION"), "Unknown actions should warn.");
        helper.assertTrue(hasCode(diagnostics, "SCRIPTCORE_UNKNOWN_CONDITION"), "Unknown conditions should warn.");
        helper.assertTrue(hasCode(diagnostics, "SCRIPTCORE_INVALID_FACTION_RANKS"), "Invalid faction ranks should be diagnosed.");
        helper.assertTrue(hasCode(diagnostics, "SCRIPTCORE_INVALID_WEATHER_DURATION"), "Invalid weather duration should be diagnosed.");
        helper.succeed();
    }

    private static void adapterAndScopedReload(GameTestHelper helper) {
        try {
            EchoScriptAdapterRegistry.INSTANCE.registerDefaults();
            EchoActionResult result = EchoScriptAdapterRegistry.INSTANCE.executeAction(
                    EchoJson.action(JsonParser.parseString("""
                            { "type": "trigger_weather", "weather": "scriptcore_test:storm" }
                            """).getAsJsonObject()),
                    EchoScriptExecutionContext.empty());
            helper.assertFalse(result.supported(), "Unavailable or stubbed adapters should return unsupported without crashing.");

            Path root = Files.createTempDirectory("scriptcore_scoped");
            write(root.resolve("pack_a/missions/new.json"), """
                    {
                      "schema_version": 1,
                      "pack": "pack_a",
                      "id": "pack_a:new",
                      "type": "mission",
                      "title": "New",
                      "objectives": [{ "id": "ok", "type": "collect_item", "item": "minecraft:redstone" }]
                    }
                    """);
            EchoScriptRegistry.INSTANCE.replaceAll(List.of(
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "pack_a",
                              "id": "pack_a:old",
                              "type": "mission",
                              "title": "Old",
                              "objectives": [{ "id": "ok", "type": "collect_item", "item": "minecraft:iron_ingot" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "pack_b",
                              "id": "pack_b:keep",
                              "type": "archive_entry",
                              "title": "Keep",
                              "content": ["Preserved."]
                            }
                            """)));
            EchoScriptReloader.INSTANCE.reloadPack(root, "pack_a");
            helper.assertTrue(EchoScriptRegistry.INSTANCE.get(id("missing")).isEmpty(), "Missing ids should remain absent.");
            helper.assertTrue(EchoScriptRegistry.INSTANCE.get(Identifier.fromNamespaceAndPath("pack_a", "new")).isPresent(),
                    "Scoped pack reload should load new pack definitions.");
            helper.assertTrue(EchoScriptRegistry.INSTANCE.get(Identifier.fromNamespaceAndPath("pack_a", "old")).isEmpty(),
                    "Scoped pack reload should remove deleted definitions from that pack.");
            helper.assertTrue(EchoScriptRegistry.INSTANCE.get(Identifier.fromNamespaceAndPath("pack_b", "keep")).isPresent(),
                    "Scoped pack reload should preserve unrelated packs.");

            EchoScriptReloader.INSTANCE.reloadType(root, "mission");
            helper.assertTrue(EchoScriptRegistry.INSTANCE.get(Identifier.fromNamespaceAndPath("pack_b", "keep")).isPresent(),
                    "Scoped type reload should preserve unrelated definition types.");
            EchoScriptRegistry.INSTANCE.clear();
            helper.succeed();
        } catch (IOException exception) {
            helper.fail("Scoped reload setup failed: " + exception.getMessage());
        }
    }

    private static void missionCoreSourceLifecycle(GameTestHelper helper) {
        MissionCoreService.INSTANCE.clearForTests();
        EchoCoreServices.registerMissionService(MissionCoreService.INSTANCE);
        EchoScriptAdapterRegistry.INSTANCE.registerDefaults();

        Identifier otherChapter = Identifier.fromNamespaceAndPath("scriptcore_other", "chapter");
        Identifier otherMission = Identifier.fromNamespaceAndPath("scriptcore_other", "mission");
        MissionCoreService.INSTANCE.registerChapter("other_source",
                new MissionChapterDefinition(otherChapter, "Other", "Other source", 1, 0xFFFFFF));
        MissionCoreService.INSTANCE.registerMission("other_source",
                MissionDefinition.builder(otherMission, otherChapter).text("Other", "", "").build());

        EchoScriptRegistry.INSTANCE.replaceAll(List.of(parse("""
                {
                  "schema_version": 1,
                  "pack": "scriptcore_live",
                  "id": "scriptcore_live:first",
                  "type": "mission",
                  "title": "First",
                  "objectives": [{ "id": "ok", "type": "collect_item", "item": "minecraft:redstone" }]
                }
                """)));
        EchoScriptAdapterRegistry.INSTANCE.registerDefinitions(EchoScriptRegistry.INSTANCE, diagnostic -> { });
        helper.assertTrue(MissionCoreService.INSTANCE.missionDefinition(Identifier.fromNamespaceAndPath("scriptcore_live", "first")).isPresent(),
                "ScriptCore mission should publish into MissionCore.");
        helper.assertTrue(MissionCoreService.INSTANCE.missionDefinition(otherMission).isPresent(),
                "MissionCore replacement should preserve non-ScriptCore missions.");

        EchoScriptRegistry.INSTANCE.replaceAll(List.of(parse("""
                {
                  "schema_version": 1,
                  "pack": "scriptcore_live",
                  "id": "scriptcore_live:second",
                  "type": "mission",
                  "title": "Second",
                  "objectives": [{ "id": "ok", "type": "collect_item", "item": "minecraft:iron_ingot" }]
                }
                """)));
        EchoScriptAdapterRegistry.INSTANCE.registerDefinitions(EchoScriptRegistry.INSTANCE, diagnostic -> { });
        helper.assertTrue(MissionCoreService.INSTANCE.missionDefinition(Identifier.fromNamespaceAndPath("scriptcore_live", "first")).isEmpty(),
                "Deleted ScriptCore mission should be unregistered from MissionCore.");
        helper.assertTrue(MissionCoreService.INSTANCE.missionDefinition(Identifier.fromNamespaceAndPath("scriptcore_live", "second")).isPresent(),
                "Updated ScriptCore mission snapshot should publish into MissionCore.");
        helper.assertTrue(MissionCoreService.INSTANCE.missionDefinition(otherMission).isPresent(),
                "MissionCore source replacement should still preserve unrelated mission source.");
        EchoScriptRegistry.INSTANCE.clear();
        MissionCoreService.INSTANCE.clearForTests();
        helper.succeed();
    }

    private static void runtimeStorageRoundTrip(GameTestHelper helper) {
        EchoCoreServices.registerDataService(DataCoreDataService.INSTANCE);
        EchoScriptAdapterRegistry.INSTANCE.registerDefaults();
        var player = helper.makeMockServerPlayerInLevel();
        EchoScriptExecutionContext context = new EchoScriptExecutionContext(
                Optional.of(player),
                Optional.of(helper.getLevel().getServer()),
                "gametest",
                Map.of());
        Identifier state = Identifier.fromNamespaceAndPath("scriptcore_live", "radio_restored");
        Identifier faction = Identifier.fromNamespaceAndPath("scriptcore_live", "settlers");

        EchoActionResult world = EchoScriptAdapterRegistry.INSTANCE.executeAction(
                EchoJson.action(JsonParser.parseString("""
                        { "type": "set_world_state", "state": "scriptcore_live:radio_restored" }
                        """).getAsJsonObject()),
                context);
        helper.assertTrue(world.supported() && world.success(), "set_world_state should be handled by DataCore runtime.");
        helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.worldState(helper.getLevel(), state),
                "World state should persist through DataCore.");

        EchoScriptAdapterRegistry.INSTANCE.executeAction(EchoJson.action(JsonParser.parseString("""
                { "type": "change_reputation", "faction": "scriptcore_live:settlers", "amount": 7 }
                """).getAsJsonObject()), context);
        helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.factionReputation(player, faction) == 7L,
                "Faction reputation should persist through DataCore.");

        EchoScriptAdapterRegistry.INSTANCE.executeAction(EchoJson.action(JsonParser.parseString("""
                { "type": "set_custom_metric", "metric": "generator_parts", "value": "4" }
                """).getAsJsonObject()), context);
        EchoScriptAdapterRegistry.INSTANCE.executeAction(EchoJson.action(JsonParser.parseString("""
                { "type": "change_custom_metric", "metric": "generator_parts", "amount": 2 }
                """).getAsJsonObject()), context);
        helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.customMetric(player, "generator_parts") == 6L,
                "Custom metric should support set and change actions.");

        EchoScriptAdapterRegistry.INSTANCE.executeAction(EchoJson.action(JsonParser.parseString("""
                { "type": "set_branch_marker", "value": "route_a_unlocked" }
                """).getAsJsonObject()), context);
        EchoConditionResult branch = EchoScriptAdapterRegistry.INSTANCE.evaluateCondition(
                EchoJson.condition(JsonParser.parseString("""
                        { "type": "branch_marker_set", "value": "route_a_unlocked" }
                        """).getAsJsonObject()),
                context);
        helper.assertTrue(branch.supported() && branch.matched(), "Branch marker condition should round trip.");

        EchoScriptAdapterRegistry.INSTANCE.executeAction(EchoJson.action(JsonParser.parseString("""
                { "type": "record_dialogue_choice", "entry": "scriptcore_live:intro", "value": "help_settlers" }
                """).getAsJsonObject()), context);
        helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.dialogueChoiceMade(player,
                        Identifier.fromNamespaceAndPath("scriptcore_live", "intro"), "help_settlers"),
                "Dialogue choice marker should persist.");

        EchoScriptRuntimeMigrationReport playerPreview = EchoScriptCoreApi.get().runtimeMigrations()
                .previewPlayer(player, "scriptcore_live:settlers", "scriptcore_live:survivors");
        helper.assertTrue(playerPreview.supported() && playerPreview.candidates() == 1 && playerPreview.copied() == 0,
                "Runtime player migration preview should find faction rename candidates without copying.");
        helper.assertTrue(playerPreview.entries().stream()
                        .allMatch(entry -> !entry.copied() && entry.note().contains("preview")),
                "Runtime migration preview entries should be marked as non-copied preview notes.");
        EchoScriptRuntimeMigrationReport playerApply = EchoScriptCoreApi.get().runtimeMigrations()
                .applyPlayer(player, "scriptcore_live:settlers", "scriptcore_live:survivors");
        Identifier renamedFaction = Identifier.fromNamespaceAndPath("scriptcore_live", "survivors");
        helper.assertTrue(playerApply.supported() && playerApply.copied() == 1
                        && ScriptCoreRuntimeStateService.INSTANCE.factionReputation(player, renamedFaction) == 7L
                        && ScriptCoreRuntimeStateService.INSTANCE.factionReputation(player, faction) == 7L,
                "Runtime player migration should copy faction reputation and preserve the source.");
        helper.assertTrue(playerApply.entries().stream()
                        .anyMatch(entry -> entry.copied() && entry.note().contains("copied")),
                "Runtime migration apply entries should be marked copied only after a successful copy.");

        EchoScriptRuntimeMigrationReport metricApply = EchoScriptCoreApi.get().runtimeMigrations()
                .applyPlayer(player, "generator_parts", "generator_components");
        helper.assertTrue(metricApply.supported()
                        && ScriptCoreRuntimeStateService.INSTANCE.customMetric(player, "generator_components") == 6L
                        && ScriptCoreRuntimeStateService.INSTANCE.customMetric(player, "generator_parts") == 6L,
                "Runtime migration should copy custom metrics and preserve old metric values.");

        EchoScriptRuntimeMigrationReport branchApply = EchoScriptCoreApi.get().runtimeMigrations()
                .applyPlayer(player, "route_a_unlocked", "route_b_unlocked");
        helper.assertTrue(branchApply.supported()
                        && ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "route_b_unlocked")
                        && ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "route_a_unlocked"),
                "Runtime migration should copy branch markers without deleting the original.");

        EchoScriptRuntimeMigrationReport worldApply = EchoScriptCoreApi.get().runtimeMigrations()
                .applyWorld(helper.getLevel(), "scriptcore_live:radio_restored", "scriptcore_live:radio_repaired");
        helper.assertTrue(worldApply.supported()
                        && ScriptCoreRuntimeStateService.INSTANCE.worldState(helper.getLevel(),
                        Identifier.fromNamespaceAndPath("scriptcore_live", "radio_repaired"))
                        && ScriptCoreRuntimeStateService.INSTANCE.worldState(helper.getLevel(), state),
                "Runtime world migration should copy world states and preserve old flags.");
        EchoScriptRuntimeSnapshot playerSnapshot = EchoScriptCoreApi.get().runtimeMigrations().snapshotPlayer(player);
        helper.assertTrue(playerSnapshot.available() && playerSnapshot.values().stream()
                        .anyMatch(value -> EchoScriptCore.id("faction_reputation/scriptcore_live/settlers").equals(value.key()))
                        && playerSnapshot.values().stream()
                        .anyMatch(value -> EchoScriptCore.id("custom_metric/generator_parts").equals(value.key()))
                        && playerSnapshot.values().stream()
                        .anyMatch(value -> EchoScriptCore.id("branch/route_a_unlocked").equals(value.key())),
                "Runtime snapshots should include registered ScriptCore runtime keys, not only raw debug entries.");

        EchoCoreServices.registerDataService(NoOpDataService.INSTANCE);
        EchoScriptRuntimeMigrationReport unavailable = EchoScriptCoreApi.get().runtimeMigrations()
                .previewPlayer(player, "old", "new");
        helper.assertFalse(unavailable.supported(), "Runtime migration should report unsupported when DataCore is unavailable.");
        EchoCoreServices.registerDataService(DataCoreDataService.INSTANCE);
        helper.succeed();
    }

    private static void screenCoreUiActionExecution(GameTestHelper helper) {
        boolean originalEnabled = ScriptCoreConfig.ENABLED.get();
        boolean originalReadOnly = ScriptCoreConfig.READ_ONLY_MODE.get();
        boolean originalUiActions = ScriptCoreConfig.ALLOW_SCREENCORE_UI_ACTIONS.get();
        IDataService originalDataService = EchoCoreServices.dataService();
        try {
            ScriptCoreConfig.ENABLED.set(true);
            ScriptCoreConfig.READ_ONLY_MODE.set(false);
            EchoCoreServices.registerDataService(DataCoreDataService.INSTANCE);
            EchoScriptAdapterRegistry.INSTANCE.registerDefaults();
            var player = helper.makeMockServerPlayerInLevel();
            Identifier executable = Identifier.fromNamespaceAndPath("scriptcore_ui", "set_branch");
            Identifier custom = Identifier.fromNamespaceAndPath("scriptcore_ui", "custom_action");
            Identifier unknown = Identifier.fromNamespaceAndPath("scriptcore_ui", "unknown_action");
            Identifier partialCustom = Identifier.fromNamespaceAndPath("scriptcore_ui", "partial_custom_action");
            Identifier unmet = Identifier.fromNamespaceAndPath("scriptcore_ui", "unmet_condition");
            Identifier capped = Identifier.fromNamespaceAndPath("scriptcore_ui", "too_many_actions");
            Identifier worldState = Identifier.fromNamespaceAndPath("scriptcore_ui", "radio_restored");
            Identifier dialogue = Identifier.fromNamespaceAndPath("scriptcore_ui", "dialogue");
            Identifier typed = Identifier.fromNamespaceAndPath("generic_survival", "radio_choice");
            Identifier unresolved = Identifier.fromNamespaceAndPath("scriptcore_ui", "unresolved_param");
            Identifier embedded = Identifier.fromNamespaceAndPath("scriptcore_ui", "embedded_param");
            EchoScriptRegistry.INSTANCE.replaceAll(List.of(
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:set_branch",
                              "type": "generic",
                              "actions": [{ "type": "set_branch_marker", "value": "ui_clicked" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:custom_action",
                              "type": "generic",
                              "actions": [{ "type": "custom", "id": "unsafe" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:unknown_action",
                              "type": "generic",
                              "actions": [{ "type": "made_up_action" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:partial_custom_action",
                              "type": "generic",
                              "actions": [
                                { "type": "set_branch_marker", "value": "partial_should_not_set" },
                                { "type": "custom", "id": "unsafe" }
                              ]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "generic_survival",
                              "id": "generic_survival:radio_choice",
                              "type": "generic",
                              "metadata": {
                                "screencore_ui": {
                                  "params": [
                                    { "id": "choice_id", "type": "string", "required": true, "pattern": "[a-z0-9_]+" },
                                    { "id": "amount", "type": "int", "required": true, "min": 1, "max": 64 },
                                    { "id": "state_id", "type": "identifier", "required": true },
                                    { "id": "flag", "type": "boolean" },
                                    { "id": "route", "type": "enum", "values": ["route_a", "route_b"] }
                                  ]
                                }
                              },
                              "actions": [
                                { "type": "set_branch_marker", "value": "{param.choice_id}" },
                                { "type": "set_custom_metric", "metric": "ui_amount", "value": "{param.amount}" },
                                { "type": "set_world_state", "state": "{param.state_id}" },
                                { "type": "set_branch_marker", "value": "{param.route}" }
                              ]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:unresolved_param",
                              "type": "generic",
                              "metadata": {
                                "screencore_ui": {
                                  "params": [
                                    { "id": "choice_id", "type": "string", "required": true }
                                  ]
                                }
                              },
                              "actions": [{ "type": "set_branch_marker", "value": "{param.missing}" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:embedded_param",
                              "type": "generic",
                              "metadata": {
                                "screencore_ui": {
                                  "params": [
                                    { "id": "choice_id", "type": "string", "required": true }
                                  ]
                                }
                              },
                              "actions": [{ "type": "set_branch_marker", "value": "prefix_{param.choice_id}" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:unmet_condition",
                              "type": "generic",
                              "conditions": [{ "type": "never" }],
                              "actions": [{ "type": "noop" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:too_many_actions",
                              "type": "generic",
                              "actions": [
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" },
                                { "type": "noop" }
                              ]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:radio_restored",
                              "type": "world_state",
                              "effects": [{ "type": "set_world_state", "state": "scriptcore_ui:radio_restored" }]
                            }
                            """),
                    parse("""
                            {
                              "schema_version": 1,
                              "pack": "scriptcore_ui",
                              "id": "scriptcore_ui:dialogue",
                              "type": "dialogue",
                              "choices": [
                                {
                                  "id": "help",
                                  "conditions": [{ "type": "always" }],
                                  "actions": [
                                    { "type": "record_dialogue_choice", "entry": "scriptcore_ui:dialogue", "value": "help" }
                                  ]
                                }
                              ]
                            }
                            """)));

            ScriptCoreConfig.ALLOW_SCREENCORE_UI_ACTIONS.set(false);
            var disabled = ScriptCoreUiExecutionService.INSTANCE.execute(player, executable, "actions",
                    "echoscreencore:test", "run", executable.toString());
            helper.assertFalse(disabled.success(), "Disabled ScreenCore UI action bridge should reject execution.");
            helper.assertFalse(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "ui_clicked"),
                    "Disabled UI action execution must not mutate runtime state.");

            ScriptCoreConfig.ALLOW_SCREENCORE_UI_ACTIONS.set(true);
            ScriptCoreConfig.READ_ONLY_MODE.set(true);
            var readOnly = ScriptCoreUiExecutionService.INSTANCE.execute(player, executable, "actions",
                    "echoscreencore:test", "read-only", executable.toString());
            helper.assertFalse(readOnly.success(), "ScriptCore read_only_mode should reject UI action execution.");
            helper.assertFalse(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "ui_clicked"),
                    "Read-only UI action rejection must not mutate runtime state.");
            ScriptCoreConfig.READ_ONLY_MODE.set(false);
            ScriptCoreConfig.ENABLED.set(false);
            var globallyDisabled = ScriptCoreUiExecutionService.INSTANCE.execute(player, executable, "actions",
                    "echoscreencore:test", "globally-disabled", executable.toString());
            helper.assertFalse(globallyDisabled.success(),
                    "ScriptCore enabled=false should reject UI action execution.");
            ScriptCoreConfig.ENABLED.set(true);
            var ok = ScriptCoreUiExecutionService.INSTANCE.execute(player, executable, "actions",
                    "echoscreencore:test", "run", executable.toString());
            helper.assertTrue(ok.success() && ok.executedActions() == 1,
                    "Enabled ScreenCore UI action bridge should execute known ScriptCore actions.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "ui_clicked"),
                    "Known UI-triggered action should mutate DataCore runtime state.");
            var effects = ScriptCoreUiExecutionService.INSTANCE.execute(player, worldState, "effects",
                    "echoscreencore:test", "run", worldState.toString());
            helper.assertTrue(effects.success() && effects.executedActions() == 1,
                    "World-state effects slot should be executable from trusted ScreenCore UI requests.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.worldState(helper.getLevel(), worldState),
                    "World-state effects should mutate DataCore world runtime state.");
            var choice = ScriptCoreUiExecutionService.INSTANCE.execute(player, dialogue, "choice:help",
                    "echoscreencore:test", "choice-help", dialogue.toString());
            helper.assertTrue(choice.success() && choice.executedActions() == 1,
                    "Dialogue choice slot should execute matching choice actions.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.dialogueChoiceMade(player, dialogue, "help"),
                    "Dialogue choice slot should record the chosen branch marker.");
            Map<String, String> typedParams = Map.of(
                    "choice_id", "ui_param_clicked",
                    "amount", "7",
                    "state_id", "scriptcore_ui:param_state",
                    "flag", "true",
                    "route", "route_a");
            var preview = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-preview", typed.toString(), typedParams);
            helper.assertTrue(preview.success() && preview.actionCount() == 4 && preview.executedActions() == 0,
                    "Preview should validate typed params and known actions without executing them.");
            helper.assertFalse(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "ui_param_clicked"),
                    "Preview must not mutate branch marker state.");
            var typedOk = ScriptCoreUiExecutionService.INSTANCE.execute(player, typed, "actions",
                    "echoscreencore:test", "typed-execute", typed.toString(), typedParams);
            helper.assertTrue(typedOk.success() && typedOk.actionCount() == 4 && typedOk.executedActions() == 4,
                    "Execute should resolve declared typed params into preloaded ScriptCore actions.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "ui_param_clicked"),
                    "String param should resolve into set_branch_marker value.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.customMetric(player, "ui_amount") == 7L,
                    "Int param should resolve into set_custom_metric value.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.worldState(helper.getLevel(),
                            Identifier.fromNamespaceAndPath("scriptcore_ui", "param_state")),
                    "Identifier param should resolve into set_world_state state.");
            helper.assertTrue(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "route_a"),
                    "Enum param should resolve into a known branch marker value.");

            var missing = ScriptCoreUiExecutionService.INSTANCE.execute(player, id("missing"), "actions",
                    "echoscreencore:test", "run", "missing");
            helper.assertTrue(!missing.success() && "missing-definition".equals(missing.code()),
                    "Missing ScriptCore definition should be rejected.");
            var invalidSlot = ScriptCoreUiExecutionService.INSTANCE.execute(player, executable, "effects",
                    "echoscreencore:test", "run", executable.toString());
            helper.assertTrue(!invalidSlot.success() && "invalid-slot".equals(invalidSlot.code()),
                    "Unsupported UI action slot should be rejected.");
            var unmetCondition = ScriptCoreUiExecutionService.INSTANCE.execute(player, unmet, "actions",
                    "echoscreencore:test", "run", unmet.toString());
            helper.assertTrue(!unmetCondition.success() && "condition-unmet".equals(unmetCondition.code()),
                    "Unmet ScriptCore conditions should reject UI execution.");
            var customAction = ScriptCoreUiExecutionService.INSTANCE.execute(player, custom, "actions",
                    "echoscreencore:test", "run", custom.toString());
            helper.assertTrue(!customAction.success() && "unsupported-action".equals(customAction.code()),
                    "Custom actions should remain diagnostic-only from ScreenCore UI execution.");
            var unknownAction = ScriptCoreUiExecutionService.INSTANCE.execute(player, unknown, "actions",
                    "echoscreencore:test", "run", unknown.toString());
            helper.assertTrue(!unknownAction.success() && "unsupported-action".equals(unknownAction.code()),
                    "Unknown actions should be rejected before UI execution.");
            var partial = ScriptCoreUiExecutionService.INSTANCE.execute(player, partialCustom, "actions",
                    "echoscreencore:test", "run", partialCustom.toString());
            helper.assertTrue(!partial.success() && partial.executedActions() == 0,
                    "UI preflight should reject custom actions before executing earlier actions.");
            helper.assertFalse(ScriptCoreRuntimeStateService.INSTANCE.branchMarker(player, "partial_should_not_set"),
                    "Rejected UI preflight must not partially mutate runtime state.");
            var tooMany = ScriptCoreUiExecutionService.INSTANCE.execute(player, capped, "actions",
                    "echoscreencore:test", "run", capped.toString());
            helper.assertTrue(!tooMany.success() && "too-many-actions".equals(tooMany.code()),
                    "UI execution should reject action lists over the cap.");
            var extraParam = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-extra", typed.toString(),
                    Map.of("choice_id", "extra_test", "amount", "1", "state_id", "scriptcore_ui:x",
                            "route", "route_a", "extra", "nope"));
            helper.assertTrue(!extraParam.success() && "undeclared-param".equals(extraParam.code()),
                    "Undeclared ScreenCore UI params should be rejected.");
            var missingParam = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-missing", typed.toString(),
                    Map.of("choice_id", "missing_amount", "state_id", "scriptcore_ui:x", "route", "route_a"));
            helper.assertTrue(!missingParam.success() && "missing-param".equals(missingParam.code()),
                    "Required ScreenCore UI params should be enforced.");
            var badInt = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-bad-int", typed.toString(),
                    Map.of("choice_id", "bad_int", "amount", "abc", "state_id", "scriptcore_ui:x", "route", "route_a"));
            helper.assertTrue(!badInt.success() && "invalid-param".equals(badInt.code()),
                    "Malformed int params should be rejected.");
            var badRange = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-bad-range", typed.toString(),
                    Map.of("choice_id", "bad_range", "amount", "99", "state_id", "scriptcore_ui:x", "route", "route_a"));
            helper.assertTrue(!badRange.success() && "invalid-param".equals(badRange.code()),
                    "Out-of-range int params should be rejected.");
            var badIdentifier = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-bad-id", typed.toString(),
                    Map.of("choice_id", "bad_id", "amount", "1", "state_id", "Bad Id", "route", "route_a"));
            helper.assertTrue(!badIdentifier.success() && "invalid-param".equals(badIdentifier.code()),
                    "Malformed identifier params should be rejected.");
            var badEnum = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-bad-enum", typed.toString(),
                    Map.of("choice_id", "bad_enum", "amount", "1", "state_id", "scriptcore_ui:x", "route", "route_c"));
            helper.assertTrue(!badEnum.success() && "invalid-param".equals(badEnum.code()),
                    "Unknown enum params should be rejected.");
            LinkedHashMap<String, String> tooManyParams = new LinkedHashMap<>();
            for (int i = 0; i <= ScriptCoreUiExecutionService.MAX_PARAMS_PER_TRIGGER; i++) {
                tooManyParams.put("p" + i, "v");
            }
            var tooManyParamResult = ScriptCoreUiExecutionService.INSTANCE.preview(player, typed, "actions",
                    "echoscreencore:test", "typed-too-many-params", typed.toString(), tooManyParams);
            helper.assertTrue(!tooManyParamResult.success() && "too-many-params".equals(tooManyParamResult.code()),
                    "UI param maps over the cap should be rejected.");
            var unresolvedParam = ScriptCoreUiExecutionService.INSTANCE.execute(player, unresolved, "actions",
                    "echoscreencore:test", "typed-unresolved", unresolved.toString(),
                    Map.of("choice_id", "safe"));
            helper.assertTrue(!unresolvedParam.success() && "unresolved-param".equals(unresolvedParam.code()),
                    "Unresolved exact placeholders should be rejected before mutation.");
            var embeddedParam = ScriptCoreUiExecutionService.INSTANCE.execute(player, embedded, "actions",
                    "echoscreencore:test", "typed-embedded", embedded.toString(),
                    Map.of("choice_id", "safe"));
            helper.assertTrue(!embeddedParam.success() && "invalid-placeholder".equals(embeddedParam.code()),
                    "Embedded placeholders should be rejected before mutation.");
            var resultPacket = com.knoxhack.echo.scriptcore.network.ScriptCoreUiResultPacket.from(preview);
            helper.assertTrue(resultPacket.success() && "preview".equals(resultPacket.mode().wireName())
                            && "typed-preview".equals(resultPacket.componentId())
                            && resultPacket.actionCount() == 4 && resultPacket.executedActions() == 0,
                    "Result packet should preserve mode, context, success, action count, and executed count.");
            EchoActionContext actionContext = new EchoActionContext(
                    Identifier.fromNamespaceAndPath("echoscreencore", "test"),
                    "typed-preview",
                    EchoDataContext.empty(),
                    null,
                    "scriptcore.preview",
                    typed.toString(),
                    typed.toString(),
                    Map.of("param-choice_id", "screen_choice", "param-amount", "3", "slot", "actions"),
                    "click",
                    null);
            helper.assertTrue("screen_choice".equals(ScriptCoreScreenCoreBridge.uiParamsForTests(actionContext).get("choice_id"))
                            && ScriptCoreScreenCoreBridge.uiParamsForTests(actionContext).size() == 2,
                    "ScreenCore bridge should extract only action-param-param-* values as UI params.");
            ScriptCoreScreenCoreClientState.resetForTests();
            ScriptCoreScreenCoreClientState.markBridgeRegistered();
            ScriptCoreScreenCoreClientState.apply(resultPacket);
            helper.assertTrue(Boolean.TRUE.equals(ScriptCoreScreenCoreClientState.resolveForTests("bridge.clientRegistered")),
                    "ScriptCore ScreenCore client data provider should expose bridge registration.");
            helper.assertTrue(Boolean.TRUE.equals(ScriptCoreScreenCoreClientState.resolveForTests("last.success"))
                            && "ready".equals(ScriptCoreScreenCoreClientState.resolveForTests("last.status"))
                            && "preview".equals(ScriptCoreScreenCoreClientState.resolveForTests("results.typed-preview.mode"))
                            && "ready".equals(ScriptCoreScreenCoreClientState.resolveForTests("results.typed-preview.status")),
                    "ScriptCore ScreenCore client data provider should expose latest and component-scoped results.");

            helper.succeed();
        } finally {
            ScriptCoreConfig.ENABLED.set(originalEnabled);
            ScriptCoreConfig.READ_ONLY_MODE.set(originalReadOnly);
            ScriptCoreConfig.ALLOW_SCREENCORE_UI_ACTIONS.set(originalUiActions);
            EchoScriptRegistry.INSTANCE.clear();
            EchoCoreServices.registerDataService(originalDataService);
        }
    }

    private static void terminalAndDiagnostics(GameTestHelper helper) {
        EchoScriptAdapterRegistry.INSTANCE.registerDefaults();
        TerminalArchiveRegistry.clearForTests();
        Identifier otherId = Identifier.fromNamespaceAndPath("other_pack", "manual_entry");
        Identifier staleId = Identifier.fromNamespaceAndPath("scriptcore_live", "stale_signal");
        Identifier freshId = Identifier.fromNamespaceAndPath("scriptcore_live", "fresh_signal");
        TerminalArchiveRegistry.register("other_source", new TerminalArchiveEntry(
                otherId, "manual", "Manual Entry", "common", List.of("Keep me."), false));
        TerminalArchiveRegistry.replaceSource("echoscriptcore", List.of(new TerminalArchiveEntry(
                staleId, "scriptcore", "Stale Signal", "common", List.of("Remove me."), false)));
        TerminalArchiveRegistry.replaceSource("echoscriptcore", List.of(new TerminalArchiveEntry(
                freshId, "scriptcore", "Fresh Signal", "common", List.of("Keep me too."), false)));
        helper.assertTrue(TerminalArchiveRegistry.entries().stream().anyMatch(entry -> entry.id().equals(otherId)),
                "Terminal archive source replacement should preserve non-ScriptCore entries.");
        helper.assertTrue(TerminalArchiveRegistry.entries().stream().noneMatch(entry -> entry.id().equals(staleId)),
                "Terminal archive source replacement should remove stale entries for the replaced source.");
        helper.assertTrue(TerminalArchiveRegistry.sourceOf(freshId).filter("echoscriptcore"::equals).isPresent(),
                "Terminal archive source tracking should expose source ownership.");

        EchoScriptRegistry.INSTANCE.replaceAll(List.of(parse("""
                {
                  "schema_version": 1,
                  "pack": "scriptcore_live",
                  "id": "scriptcore_live:first_signal",
                  "type": "archive_entry",
                  "title": "First Signal",
                  "content": ["A terminal-readable archive entry."]
                }
                """)));
        EchoScriptAdapterRegistry.INSTANCE.registerDefinitions(EchoScriptRegistry.INSTANCE, diagnostic -> { });
        helper.assertTrue(TerminalArchiveRegistry.entries().stream()
                        .anyMatch(entry -> entry.id().equals(Identifier.fromNamespaceAndPath("scriptcore_live", "first_signal"))),
                "Terminal adapter should publish ScriptCore archive entries when Terminal is available.");
        EchoScriptRegistry.INSTANCE.replaceAll(List.of());
        EchoScriptAdapterRegistry.INSTANCE.registerDefinitions(EchoScriptRegistry.INSTANCE, diagnostic -> { });
        helper.assertTrue(TerminalArchiveRegistry.entries().stream()
                        .noneMatch(entry -> entry.id().equals(Identifier.fromNamespaceAndPath("scriptcore_live", "first_signal"))),
                "Terminal adapter should unregister deleted ScriptCore archive entries on empty replacement.");

        List<EchoScriptDiagnostic> diagnostics = EchoScriptValidator.INSTANCE.validate(List.of(parse("""
                {
                  "schema_version": 1,
                  "pack": "scriptcore_live",
                  "id": "scriptcore_live:bad",
                  "type": "mission",
                  "title": "Bad",
                  "objectives": [{ "id": "missing_item", "type": "collect_item" }]
                }
                """)));
        EchoScriptLoadResult synthetic = new EchoScriptLoadResult(
                1, 0, 0, 0, EchoScriptRegistry.INSTANCE.all(), diagnostics, List.of(), List.of(), 1);
        helper.assertTrue(diagnostics.stream().anyMatch(d -> "SCRIPTCORE_INVALID_OBJECTIVE".equals(d.code())),
                "Diagnostics fixture should include invalid objective.");
        helper.assertTrue(EchoScriptCoreApi.get().diagnosticsSummary().missingAdapters() != null,
                "Diagnostics summary API should be callable for UI integrations.");
        helper.assertTrue(synthetic.diagnostics().size() == diagnostics.size(),
                "Synthetic load result sanity check should preserve diagnostics.");
        List<EchoScriptDefinitionView> filterFixture = List.of(
                parse("""
                        { "schema_version": 1, "pack": "alpha", "id": "alpha:radio", "type": "mission", "title": "Repair Radio" }
                        """),
                parse("""
                        { "schema_version": 1, "pack": "beta", "id": "beta:signal", "type": "archive_entry", "title": "Signal Log", "content": ["Log"] }
                        """));
        helper.assertTrue(ScriptCoreTerminalTab.filterDefinitionsForTests(filterFixture, "radio", "mission", "alpha").size() == 1,
                "Terminal ScriptCore definition filters should combine search, type, and pack.");
        helper.assertTrue(ScriptCoreTerminalTab.filterDiagnosticsForTests(diagnostics, "objective", EchoScriptDiagnostic.Severity.ERROR)
                        .stream().anyMatch(d -> "SCRIPTCORE_INVALID_OBJECTIVE".equals(d.code())),
                "Terminal ScriptCore diagnostic filters should combine search and severity.");
        EchoScriptRegistry.INSTANCE.clear();
        TerminalArchiveRegistry.clearForTests();
        helper.succeed();
    }

    private static EchoScriptDefinitionView parse(String json) {
        return PARSER.parse(JsonParser.parseString(json).getAsJsonObject(), Path.of("scriptcore_test.json"), "scriptcore_test");
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static boolean hasCode(List<EchoScriptDiagnostic> diagnostics, String code) {
        return diagnostics.stream().anyMatch(diagnostic -> code.equals(diagnostic.code()));
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true,
                net.minecraft.world.level.block.Rotation.NONE,
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
            if (normalized.equals(EchoScriptCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoScriptCore.MODID, path);
    }
}
