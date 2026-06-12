package com.knoxhack.echomissioncore.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryCategory;
import com.echoplatform.echocore.api.EchoDiscoveryEntry;
import com.echoplatform.echocore.api.EchoRuntimeSpineBus;
import com.echoplatform.echocore.api.EchoRuntimeSpineEvent;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.EchoWorldRuntimeBus;
import com.echoplatform.echocore.api.IDataKey;
import com.echoplatform.echocore.api.WorldDiscoverySource;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import com.echoplatform.echocore.api.WorldRegionInstance;
import com.echoplatform.echocore.api.WorldRegionType;
import com.echoplatform.echocore.api.mission.IMissionProgressView;
import com.echoplatform.echocore.api.mission.MissionActionView;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.MissionRuntimeBus;
import com.echoplatform.echocore.api.mission.MissionRuntimeEvent;
import com.echoplatform.echocore.api.mission.MissionStatus;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echomissioncore.content.MissionCoreJsonReloadListener;
import com.knoxhack.echomissioncore.integration.MissionCoreRuntimeSpineConsumer;
import com.knoxhack.echomissioncore.integration.MissionCoreTerminalProvider;
import com.knoxhack.echomissioncore.integration.MissionCoreWorldCoreConsumer;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import com.knoxhack.echomissioncore.storage.MissionPlayerData;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelKind;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoMissionCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NO_OP_FALLBACK =
            TEST_FUNCTIONS.register("no_op_fallback", () -> ModGameTests::noOpFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_VALIDATION =
            TEST_FUNCTIONS.register("json_validation", () -> ModGameTests::jsonValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OBJECTIVE_REWARD_FLOW =
            TEST_FUNCTIONS.register("objective_reward_flow", () -> ModGameTests::objectiveRewardFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLAYER_DATA_ROUND_TRIP =
            TEST_FUNCTIONS.register("player_data_round_trip", () -> ModGameTests::playerDataRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_PROVIDER =
            TEST_FUNCTIONS.register("terminal_provider_snapshot", () -> ModGameTests::terminalProviderSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CUSTOM_ACTIONS =
            TEST_FUNCTIONS.register("custom_action_bridge", () -> ModGameTests::customActionBridge);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLDCORE_CONSUMER =
            TEST_FUNCTIONS.register("worldcore_consumer", () -> ModGameTests::worldCoreConsumer);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOOK_COVERAGE =
            TEST_FUNCTIONS.register("hook_coverage", () -> ModGameTests::hookCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REPEATABLE_RESET =
            TEST_FUNCTIONS.register("repeatable_reset", () -> ModGameTests::repeatableReset);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_RELOAD_REPLACEMENT =
            TEST_FUNCTIONS.register("json_reload_replacement", () -> ModGameTests::jsonReloadReplacement);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> IMMEDIATE_REWARD =
            TEST_FUNCTIONS.register("immediate_reward_once", () -> ModGameTests::immediateRewardOnce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTERNAL_STATUS_PREREQUISITES =
            TEST_FUNCTIONS.register("external_status_prerequisites", () -> ModGameTests::externalStatusPrerequisites);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ROUTE_METADATA =
            TEST_FUNCTIONS.register("terminal_route_metadata", () -> ModGameTests::terminalRouteMetadata);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNTIME_SPINE_FOUNDATION_FLOW =
            TEST_FUNCTIONS.register("runtime_spine_foundation_flow", () -> ModGameTests::runtimeSpineFoundationFlow);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("missioncore_framework"));
        register(event, environment, "no_op_fallback", NO_OP_FALLBACK.getId());
        register(event, environment, "json_validation", JSON_VALIDATION.getId());
        register(event, environment, "objective_reward_flow", OBJECTIVE_REWARD_FLOW.getId());
        register(event, environment, "player_data_round_trip", PLAYER_DATA_ROUND_TRIP.getId());
        register(event, environment, "terminal_provider_snapshot", TERMINAL_PROVIDER.getId());
        register(event, environment, "custom_action_bridge", CUSTOM_ACTIONS.getId());
        register(event, environment, "worldcore_consumer", WORLDCORE_CONSUMER.getId());
        register(event, environment, "hook_coverage", HOOK_COVERAGE.getId());
        register(event, environment, "repeatable_reset", REPEATABLE_RESET.getId());
        register(event, environment, "json_reload_replacement", JSON_RELOAD_REPLACEMENT.getId());
        register(event, environment, "immediate_reward_once", IMMEDIATE_REWARD.getId());
        register(event, environment, "external_status_prerequisites", EXTERNAL_STATUS_PREREQUISITES.getId());
        register(event, environment, "terminal_route_metadata", TERMINAL_ROUTE_METADATA.getId());
        register(event, environment, "runtime_spine_foundation_flow", RUNTIME_SPINE_FOUNDATION_FLOW.getId());
    }

    private static void noOpFallback(GameTestHelper helper) {
        AtomicBoolean checked = new AtomicBoolean(false);
        EchoServiceRegistry.withClearedForTests(() -> {
            helper.assertFalse(EchoCoreServices.missionService().available(), "Mission service should no-op when MissionCore is absent");
            helper.assertTrue(EchoCoreServices.missionService().missionDefinitions().isEmpty(), "No-op mission definitions should be empty");
            checked.set(!EchoCoreServices.startMission(null, id("missing")));
        });
        helper.assertTrue(checked.get(), "No-op mission start should fail safely");
        helper.succeed();
    }

    private static void jsonValidation(GameTestHelper helper) {
        JsonObject chapter = JsonParser.parseString("{\"title\":\"Tests\",\"order\":1}").getAsJsonObject();
        helper.assertTrue(
                MissionCoreJsonReloadListener.parseChapterForTests(id("json_chapter"), chapter).id().equals(id("json_chapter")),
                "Chapter JSON should default id from resource location");

        JsonObject mission = JsonParser.parseString("""
                {"chapter":"echomissioncore:json_chapter","title":"JSON Test","objectives":[{"type":"obtain_item","target":"minecraft:apple"}],"rewards":[{"item":"minecraft:emerald","claimMode":"claimable"}]}
                """).getAsJsonObject();
        helper.assertTrue(
                MissionCoreJsonReloadListener.parseMissionForTests(id("json_mission"), mission).objectives().getFirst().type() == MissionObjectiveType.OBTAIN_ITEM,
                "Mission JSON should parse objective type");
        MissionDefinition parsed = MissionCoreJsonReloadListener.parseMissionForTests(id("json_metadata"), JsonParser.parseString("""
                {"chapter":"echomissioncore:json_chapter","kind":"side","terminal_route_phase":"7","terminal_route_anchor":"echomissioncore:anchor","terminal_intel_archives":"echomissioncore:archive/log","terminal_intel_discoveries":"echomissioncore:discovery/cache","terminal_intel_factions":"echomissioncore:faction/test","terminal_intel_pois":"echomissioncore:structure/test","metadata":{"terminal_route_order":"77","terminal_intel_routes":"echomissioncore:test_route"},"rewards":[{"item":"minecraft:emerald","mode":"immediate","xp":25,"reputation":3}]}
                """).getAsJsonObject());
        helper.assertTrue(parsed.kind() == com.echoplatform.echocore.api.mission.MissionKind.SIDE_OP,
                "Mission JSON should accept side as a side-op alias");
        helper.assertTrue("7".equals(parsed.metadata().get("terminal_route_phase")),
                "Mission JSON should copy Terminal route metadata");
        helper.assertTrue("77".equals(parsed.metadata().get("terminal_route_order")),
                "Mission JSON should copy explicit metadata");
        helper.assertTrue("echomissioncore:anchor".equals(parsed.metadata().get("terminal_route_anchor")),
                "Mission JSON should parse terminal route anchors");
        helper.assertTrue("echomissioncore:archive/log".equals(parsed.metadata().get("terminal_intel_archives"))
                        && "echomissioncore:test_route".equals(parsed.metadata().get("terminal_intel_routes"))
                        && "echomissioncore:discovery/cache".equals(parsed.metadata().get("terminal_intel_discoveries"))
                        && "echomissioncore:faction/test".equals(parsed.metadata().get("terminal_intel_factions"))
                        && "echomissioncore:structure/test".equals(parsed.metadata().get("terminal_intel_pois")),
                "Mission JSON should preserve terminal intel unlock metadata");
        RewardDefinition reward = parsed.rewards().getFirst();
        helper.assertTrue(reward.claimMode() == MissionRewardClaimMode.IMMEDIATE,
                "Mission JSON should parse reward mode alias");
        helper.assertTrue("25".equals(reward.metadata().get("xp")) && "3".equals(reward.metadata().get("reputation")),
                "Mission JSON should preserve reward metadata fields");

        boolean failed = false;
        try {
            MissionCoreJsonReloadListener.parseMissionForTests(id("bad_json"), JsonParser.parseString("""
                    {"chapter":"echomissioncore:json_chapter","objectives":[{"type":"not_real"}]}
                    """).getAsJsonObject());
        } catch (RuntimeException exception) {
            failed = true;
        }
        helper.assertTrue(failed, "Invalid objective type should fail validation");
        helper.succeed();
    }

    private static void repeatableReset(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        Identifier chapterId = id("repeatable_chapter");
        Identifier missionId = id("repeatable_mission");
        Identifier objectiveId = id("repeatable_mission/objective");
        service.registerChapter("gametest", new MissionChapterDefinition(chapterId, "Repeatable", "Repeatable tests", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("Repeat", "Complete this more than once.", "GameTest")
                .repeatPolicy(com.echoplatform.echocore.api.mission.MissionRepeatPolicy.REPEATABLE)
                .objective(ObjectiveDefinition.simple(objectiveId, MissionObjectiveType.CUSTOM, "Repeat", "", ItemStack.EMPTY, 1))
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(service.startMission(player, missionId), "Repeatable mission should start.");
        helper.assertTrue(service.forceProgress(player, missionId, objectiveId, 1), "Forced progress should complete repeatable mission.");
        helper.assertTrue(service.mission(player, missionId).orElseThrow().status() == MissionStatus.COMPLETED,
                "Repeatable mission should complete after progress.");
        helper.assertTrue(service.startMission(player, missionId), "Completed repeatable mission should restart.");
        IMissionProgressView restarted = service.mission(player, missionId).orElseThrow();
        helper.assertTrue(restarted.status() == MissionStatus.ACTIVE, "Restarted repeatable mission should be active.");
        helper.assertTrue(restarted.objectives().getFirst().progress() == 0, "Restarted repeatable mission should clear objective progress.");
        helper.succeed();
    }

    private static void jsonReloadReplacement(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        Identifier javaChapter = id("java_chapter");
        Identifier javaMission = id("java_mission");
        Identifier jsonChapter = id("json_chapter");
        Identifier firstJsonMission = id("json_one");
        Identifier secondJsonMission = id("json_two");
        service.registerChapter("gametest", new MissionChapterDefinition(javaChapter, "Java", "Persistent java content", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(javaMission, javaChapter)
                .text("Java Mission", "Should survive JSON reload.", "GameTest")
                .objective(ObjectiveDefinition.simple(id("java_mission/objective"), MissionObjectiveType.CUSTOM, "Java", "", ItemStack.EMPTY, 1))
                .build());
        service.replaceJsonContent(
                List.of(new MissionChapterDefinition(jsonChapter, "JSON", "First JSON content", 1, 0x55FFDD)),
                List.of(MissionDefinition.builder(firstJsonMission, jsonChapter)
                        .text("JSON One", "Should be replaced.", "GameTest")
                        .objective(ObjectiveDefinition.simple(id("json_one/objective"), MissionObjectiveType.CUSTOM, "JSON", "", ItemStack.EMPTY, 1))
                        .build()));
        helper.assertTrue(service.missionDefinition(firstJsonMission).isPresent(), "First JSON mission should load.");
        service.replaceJsonContent(
                List.of(new MissionChapterDefinition(jsonChapter, "JSON", "Second JSON content", 1, 0x55FFDD)),
                List.of(MissionDefinition.builder(secondJsonMission, jsonChapter)
                        .text("JSON Two", "Should replace prior JSON content.", "GameTest")
                        .objective(ObjectiveDefinition.simple(id("json_two/objective"), MissionObjectiveType.CUSTOM, "JSON", "", ItemStack.EMPTY, 1))
                        .build()));
        helper.assertTrue(service.missionDefinition(javaMission).isPresent(), "Java mission should survive JSON replacement.");
        helper.assertFalse(service.missionDefinition(firstJsonMission).isPresent(), "Old JSON mission should be removed.");
        helper.assertTrue(service.missionDefinition(secondJsonMission).isPresent(), "New JSON mission should load.");
        helper.succeed();
    }

    private static void immediateRewardOnce(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        Identifier chapterId = id("immediate_reward_chapter");
        Identifier missionId = id("immediate_reward_mission");
        Identifier objectiveId = id("immediate_reward_mission/objective");
        Identifier rewardId = id("immediate_reward_mission/reward");
        service.registerChapter("gametest", new MissionChapterDefinition(chapterId, "Immediate", "Immediate reward tests", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("Immediate Reward", "Complete for an immediate reward.", "GameTest")
                .objective(ObjectiveDefinition.simple(objectiveId, MissionObjectiveType.CUSTOM, "Immediate", "", ItemStack.EMPTY, 1))
                .reward(RewardDefinition.item(rewardId, MissionRewardClaimMode.IMMEDIATE, new ItemStack(Items.EMERALD)))
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(service.startMission(player, missionId), "Immediate reward mission should start.");
        helper.assertTrue(service.forceProgress(player, missionId, objectiveId, 1), "Forced progress should complete immediate reward mission.");
        IMissionProgressView view = service.mission(player, missionId).orElseThrow();
        helper.assertTrue(view.status() == MissionStatus.CLAIMED, "Immediate reward mission should move to claimed.");
        helper.assertTrue(view.rewards().getFirst().claimed(), "Immediate reward should be marked claimed.");
        helper.assertFalse(service.completeMission(player, missionId), "Already completed once-only mission should not grant again.");
        helper.succeed();
    }

    private static void externalStatusPrerequisites(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        AtomicBoolean gateComplete = new AtomicBoolean(false);
        Identifier chapterId = id("external_status_chapter");
        Identifier gateId = id("external_status_gate");
        Identifier childId = id("external_status_child");
        service.registerChapter("gametest", new MissionChapterDefinition(
                chapterId, "External Status", "Imported provider status gating tests", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(gateId, chapterId)
                .text("Gate", "Complete the gate first.", "GameTest")
                .statusRule((player, mission) -> java.util.Optional.of(gateComplete.get()
                        ? MissionStatus.COMPLETED
                        : MissionStatus.UNLOCKED))
                .build());
        service.registerMission("gametest", MissionDefinition.builder(childId, chapterId)
                .text("Imported Child", "External provider reports complete.", "GameTest")
                .prerequisite(gateId)
                .statusRule((player, mission) -> java.util.Optional.of(MissionStatus.COMPLETED))
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(service.mission(player, childId).orElseThrow().status() == MissionStatus.LOCKED,
                "Imported provider status must not bypass incomplete prerequisites.");
        gateComplete.set(true);
        helper.assertTrue(service.mission(player, childId).orElseThrow().status() == MissionStatus.COMPLETED,
                "Imported provider status should apply once prerequisites are complete.");
        helper.succeed();
    }

    private static void terminalRouteMetadata(GameTestHelper helper) {
        if (!ModList.get().isLoaded("echoterminal")) {
            helper.succeed();
            return;
        }
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        Identifier chapterId = id("terminal_route_metadata_chapter");
        Identifier missionId = id("terminal_route_metadata_mission");
        Identifier contractMainId = id("terminal_route_contract_main");
        Identifier previewMainId = id("terminal_route_preview_main");
        Identifier arcanaAggregateId = Identifier.fromNamespaceAndPath("echoarcanacore", "arcana_route_optional_main");
        service.registerChapter("gametest", new MissionChapterDefinition(
                chapterId, "Route Metadata", "MissionCore Terminal route metadata tests", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("Route Metadata", "Preserve aggregate route metadata.", "GameTest")
                .metadata("terminal_route_phase", "14")
                .metadata("terminal_route_order", "77")
                .metadata("terminal_route_role", "optional")
                .metadata("terminal_route_visible", "false")
                .metadata("terminal_route_prerequisites", "echoashfallprotocol:expedition_readiness")
                .metadata("terminal_route_anchor", "echoashfallprotocol:secure_crash_outpost")
                .metadata("terminal_intel_archives", "echomissioncore:archive/route_metadata")
                .metadata("terminal_intel_routes", "echomissioncore:route_metadata_route")
                .metadata("terminal_intel_discoveries", "echomissioncore:discovery/route_metadata")
                .metadata("terminal_intel_factions", "echomissioncore:faction/test_faction")
                .metadata("terminal_intel_pois", "echomissioncore:structure/test_poi")
                .build());
        service.registerMission("gametest", MissionDefinition.builder(contractMainId, chapterId)
                .text("Contract Main", "Contract records can block aggregate route phases.", "GameTest")
                .kind(MissionKind.CONTRACT)
                .metadata("terminal_route_role", "MAIN")
                .build());
        service.registerMission("gametest", MissionDefinition.builder(previewMainId, chapterId)
                .text("Preview Main", "View-only branches stay informational.", "GameTest")
                .kind(MissionKind.MAIN)
                .metadata("terminal_route_role", "MAIN")
                .statusRule((player, definition) -> java.util.Optional.of(MissionStatus.VIEW_ONLY))
                .build());
        service.registerMission("gametest", MissionDefinition.builder(arcanaAggregateId, chapterId)
                .text("Arcana Optional Campaign", "Arcana remains ordered in its own chapter.", "GameTest")
                .kind(MissionKind.MAIN)
                .metadata("terminal_route_role", "OPTIONAL")
                .metadata("terminal_route_visible", "false")
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TerminalMissionDefinition definition = MissionCoreTerminalProvider.INSTANCE.missions(player).stream()
                .filter(candidate -> candidate.id().equals(missionId))
                .findFirst()
                .orElseThrow();
        TerminalMissionSnapshot snapshot = MissionCoreTerminalProvider.INSTANCE.snapshot(player, missionId);
        TerminalMissionRole role = MissionCoreTerminalProvider.INSTANCE.role(player, definition, snapshot);
        TerminalMissionRoutePlacement placement = MissionCoreTerminalProvider.INSTANCE
                .routePlacement(player, definition, snapshot, role)
                .orElseThrow();
        helper.assertTrue(role == TerminalMissionRole.OPTIONAL && placement.role() == TerminalMissionRole.OPTIONAL,
                "MissionCore Terminal import should preserve explicit aggregate route roles.");
        helper.assertTrue(placement.phaseOrder() == 14 && placement.missionOrder() == 77,
                "MissionCore Terminal import should preserve route placement above phase 9.");
        helper.assertFalse(placement.includeInSurvivalRoute(),
                "MissionCore Terminal import should preserve hidden aggregate route visibility.");
        helper.assertTrue(MissionCoreTerminalProvider.INSTANCE
                        .routePrerequisites(player, definition, snapshot, role)
                        .contains(Identifier.fromNamespaceAndPath("echoashfallprotocol", "expedition_readiness")),
                "MissionCore Terminal import should preserve aggregate route prerequisites.");
        helper.assertTrue(MissionCoreTerminalProvider.INSTANCE
                        .routeAnchor(player, definition, snapshot, role)
                        .filter(Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_crash_outpost")::equals)
                        .isPresent(),
                "MissionCore Terminal import should preserve side-card route anchors.");
        List<TerminalMissionIntelUnlock> intelUnlocks =
                MissionCoreTerminalProvider.INSTANCE.intelUnlocks(player, definition, snapshot, role);
        helper.assertTrue(intelUnlocks.stream().anyMatch(unlock ->
                        unlock.kind() == TerminalMissionIntelKind.ARCHIVE
                                && unlock.id().equals(Identifier.fromNamespaceAndPath(
                                        "echomissioncore", "archive/route_metadata")))
                        && intelUnlocks.stream().anyMatch(unlock ->
                                unlock.kind() == TerminalMissionIntelKind.DISCOVERY)
                        && intelUnlocks.stream().anyMatch(unlock ->
                                unlock.kind() == TerminalMissionIntelKind.ROUTE)
                        && intelUnlocks.stream().anyMatch(unlock ->
                                unlock.kind() == TerminalMissionIntelKind.FACTION)
                        && intelUnlocks.stream().anyMatch(unlock ->
                                unlock.kind() == TerminalMissionIntelKind.POI),
                "MissionCore Terminal import should expose archive, discovery, faction, and POI unlocks.");
        Identifier routeRecordId = Identifier.fromNamespaceAndPath("echomissioncore", "route_metadata_route");
        Identifier routeDiscoveryId = EchoCoreServices.routeDiscoveryId(routeRecordId);
        Identifier discoveryId = Identifier.fromNamespaceAndPath("echomissioncore", "discovery/route_metadata");
        Identifier factionId = Identifier.fromNamespaceAndPath("echomissioncore", "faction/test_faction");
        Identifier poiId = Identifier.fromNamespaceAndPath("echomissioncore", "structure/test_poi");
        EchoCoreServices.registerDiscoveryProvider(discoveryPlayer -> List.of(
                discoveryEntry(routeDiscoveryId, EchoDiscoveryCategory.STRUCTURE, "Route Metadata Route", 10),
                discoveryEntry(discoveryId, EchoDiscoveryCategory.STRUCTURE, "Route Metadata Discovery", 11),
                discoveryEntry(factionId, EchoDiscoveryCategory.FACTION, "Test Faction", 12),
                discoveryEntry(poiId, EchoDiscoveryCategory.STRUCTURE, "Test POI", 13)));
        helper.assertTrue(service.completeMission(player, missionId),
                "Completing MissionCore side intel should succeed once.");
        helper.assertTrue(EchoCoreServices.isArchiveUnlocked(player, "echomissioncore:archive/route_metadata"),
                "MissionCore completion should persist archive intel unlocks.");
        helper.assertTrue(EchoCoreServices.hasDiscoveredFeature(player, routeDiscoveryId)
                        && EchoCoreServices.hasDiscoveredFeature(player, discoveryId)
                        && EchoCoreServices.hasDiscoveredFeature(player, factionId)
                        && EchoCoreServices.hasDiscoveredFeature(player, poiId),
                "MissionCore completion should persist route, discovery, faction, and POI unlocks.");

        TerminalMissionDefinition contractDefinition = MissionCoreTerminalProvider.INSTANCE.missions(player).stream()
                .filter(candidate -> candidate.id().equals(contractMainId))
                .findFirst()
                .orElseThrow();
        TerminalMissionSnapshot contractSnapshot =
                MissionCoreTerminalProvider.INSTANCE.snapshot(player, contractMainId);
        TerminalMissionRole contractRole =
                MissionCoreTerminalProvider.INSTANCE.role(player, contractDefinition, contractSnapshot);
        TerminalMissionRoutePlacement contractPlacement = MissionCoreTerminalProvider.INSTANCE
                .routePlacement(player, contractDefinition, contractSnapshot, contractRole)
                .orElseThrow();
        helper.assertTrue(contractRole == TerminalMissionRole.MAIN
                        && contractPlacement.role() == TerminalMissionRole.MAIN,
                "MissionCore Terminal import should let contract missions explicitly block aggregate phases.");

        TerminalMissionDefinition previewDefinition = MissionCoreTerminalProvider.INSTANCE.missions(player).stream()
                .filter(candidate -> candidate.id().equals(previewMainId))
                .findFirst()
                .orElseThrow();
        TerminalMissionSnapshot previewSnapshot =
                MissionCoreTerminalProvider.INSTANCE.snapshot(player, previewMainId);
        TerminalMissionRole previewRole =
                MissionCoreTerminalProvider.INSTANCE.role(player, previewDefinition, previewSnapshot);
        TerminalMissionRoutePlacement previewPlacement = MissionCoreTerminalProvider.INSTANCE
                .routePlacement(player, previewDefinition, previewSnapshot, previewRole)
                .orElseThrow();
        helper.assertTrue(previewSnapshot.status()
                        == com.knoxhack.echoterminal.api.mission.TerminalMissionStatus.VIEW_ONLY
                        && previewRole == TerminalMissionRole.REFERENCE
                        && previewPlacement.role() == TerminalMissionRole.REFERENCE,
                "MissionCore Terminal import should keep view-only route branches from blocking aggregate phases.");
        TerminalMissionDefinition arcanaDefinition = MissionCoreTerminalProvider.INSTANCE.missions(player).stream()
                .filter(candidate -> candidate.id().equals(arcanaAggregateId))
                .findFirst()
                .orElseThrow();
        TerminalMissionSnapshot arcanaSnapshot =
                MissionCoreTerminalProvider.INSTANCE.snapshot(player, arcanaAggregateId);
        TerminalMissionRole arcanaRole =
                MissionCoreTerminalProvider.INSTANCE.role(player, arcanaDefinition, arcanaSnapshot);
        TerminalMissionRoutePlacement arcanaPlacement = MissionCoreTerminalProvider.INSTANCE
                .routePlacement(player, arcanaDefinition, arcanaSnapshot, arcanaRole)
                .orElseThrow();
        helper.assertTrue(arcanaRole == TerminalMissionRole.OPTIONAL
                        && arcanaPlacement.role() == TerminalMissionRole.OPTIONAL
                        && !arcanaPlacement.includeInSurvivalRoute(),
                "Arcana aggregate campaigns can stay internally ordered MAIN missions without blocking Ashfall Survival Route.");
        helper.succeed();
    }

    private static void objectiveRewardFlow(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        Identifier chapterId = id("test_chapter");
        Identifier missionId = id("test_mission");
        Identifier objectiveId = id("test_mission/apple");
        Identifier rewardId = id("test_mission/reward");
        service.registerChapter("gametest", new MissionChapterDefinition(chapterId, "Tests", "MissionCore tests", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("Apple Test", "Obtain an apple.", "GameTest")
                .objective(new ObjectiveDefinition(objectiveId, MissionObjectiveType.OBTAIN_ITEM, "Apple", "", new ItemStack(Items.APPLE), 1, true, Map.of("target", "minecraft:apple")))
                .reward(RewardDefinition.item(rewardId, MissionRewardClaimMode.CLAIMABLE, new ItemStack(Items.EMERALD)))
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<MissionRuntimeEvent> events = new ArrayList<>();
        MissionRuntimeBus.clearForTests();
        try {
            AutoCloseable listener = MissionRuntimeBus.register(events::add);
            try {
                helper.assertTrue(service.startMission(player, missionId), "Mission should start");
                IMissionProgressView activeView = service.mission(player, missionId).orElseThrow();
                helper.assertTrue(activeView.actions().stream().anyMatch(action ->
                                "complete".equals(action.id())
                                        && !action.enabled()
                                        && action.disabledReason().contains("Requirements incomplete")),
                        "Incomplete active missions should expose a disabled Turn In action with a clear blocker reason");
                helper.assertFalse(service.handleAction(player, missionId, "complete"),
                        "MissionCore action handler should reject incomplete Turn In actions server-side");
                helper.assertTrue(service.recordObjective(player, MissionObjectiveType.OBTAIN_ITEM, Identifier.withDefaultNamespace("apple"), 1, Map.of()), "Objective should record");
                IMissionProgressView view = service.mission(player, missionId).orElseThrow();
                helper.assertTrue(view.status() == MissionStatus.CLAIMABLE, "Mission should become claimable");
                helper.assertTrue(view.actions().stream().anyMatch(action ->
                                "claim".equals(action.id()) && action.enabled()),
                        "Claimable missions should expose a valid enabled claim action");
                helper.assertTrue(view.objectives().stream().anyMatch(objective -> objective.id().equals(objectiveId) && objective.complete()),
                        "Hidden objective should be revealed once complete");
                helper.assertTrue(events.stream().anyMatch(event -> MissionRuntimeEvent.OBJECTIVE_PROGRESSED.equals(event.eventType())), "Objective event should fire");
                helper.assertTrue(events.stream().anyMatch(event -> MissionRuntimeEvent.MISSION_COMPLETED.equals(event.eventType())), "Completion event should fire");
                helper.assertTrue(service.handleAction(player, missionId, "claim"), "Claim action should succeed once through MissionCore handler");
                helper.assertFalse(service.handleAction(player, missionId, "claim"), "Claim action should be idempotent");
                helper.assertTrue(events.stream().anyMatch(event -> MissionRuntimeEvent.REWARD_CLAIMED.equals(event.eventType())), "Reward event should fire");
            } finally {
                listener.close();
            }
        } catch (Exception exception) {
            helper.fail("MissionCore listener cleanup failed: " + exception.getMessage());
        } finally {
            MissionRuntimeBus.clearForTests();
        }
        helper.succeed();
    }

    private static void playerDataRoundTrip(GameTestHelper helper) {
        Identifier missionId = id("persisted");
        Identifier chapterId = id("persisted_chapter");
        Identifier objectiveId = id("persisted/objective");
        Identifier rewardId = id("persisted/reward");
        MissionPlayerData data = new MissionPlayerData();
        data.trackMission(missionId);
        data.markMigrated("gametest");
        data.markUnlockedChapter(chapterId);
        MissionPlayerData.MissionState state = data.state(missionId);
        state.status(MissionStatus.CLAIMED);
        state.setObjectiveProgress(objectiveId, 3);
        state.claimReward(rewardId);
        state.revealObjective(objectiveId);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        data.serialize(output);
        CompoundTag tag = output.buildResult();
        MissionPlayerData restored = new MissionPlayerData();
        restored.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        MissionPlayerData.MissionState restoredState = restored.stateIfPresent(missionId);
        helper.assertTrue(restoredState != null && restoredState.status() == MissionStatus.CLAIMED, "Mission status should round trip");
        helper.assertTrue(restoredState.objectiveProgress(objectiveId) == 3, "Objective progress should round trip");
        helper.assertTrue(restoredState.isRewardClaimed(rewardId), "Reward claim should round trip");
        helper.assertTrue(restored.hasUnlockedChapter(chapterId), "Unlocked chapter should round trip");
        helper.succeed();
    }

    private static void terminalProviderSnapshot(GameTestHelper helper) {
        if (!ModList.get().isLoaded("echoterminal")) {
            helper.succeed();
            return;
        }
        try {
            MissionCoreService service = MissionCoreService.INSTANCE;
            if (service.missionDefinitions().isEmpty()) {
                Identifier chapterId = id("terminal_smoke_chapter");
                Identifier missionId = id("terminal_smoke_mission");
                service.registerChapter("gametest", new MissionChapterDefinition(chapterId, "Terminal Smoke", "Terminal provider smoke test", 99, 0x55FFDD));
                service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                        .text("Terminal Smoke", "Expose one MissionCore mission to Terminal.", "GameTest")
                        .objective(ObjectiveDefinition.simple(id("terminal_smoke_mission/objective"),
                                MissionObjectiveType.CUSTOM, "Smoke", "Provider row", ItemStack.EMPTY, 1))
                        .build());
            }
            Object provider = Class.forName("com.knoxhack.echomissioncore.integration.MissionCoreTerminalProvider")
                    .getField("INSTANCE")
                    .get(null);
            Method missions = provider.getClass().getMethod("missions", Player.class);
            Object result = missions.invoke(provider, helper.makeMockServerPlayerInLevel());
            helper.assertTrue(result instanceof List<?> list && !list.isEmpty(), "MissionCore Terminal provider should expose missions");
            helper.succeed();
        } catch (ReflectiveOperationException exception) {
            helper.fail("MissionCore Terminal provider reflection failed: " + exception.getMessage());
        }
    }

    private static void customActionBridge(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        Identifier chapterId = id("custom_action_chapter");
        Identifier missionId = id("custom_action_mission");
        AtomicBoolean handled = new AtomicBoolean(false);
        service.registerChapter("gametest", new MissionChapterDefinition(chapterId, "Actions", "Custom action bridge tests", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("Custom Action", "Expose a Java-only action.", "GameTest")
                .objective(ObjectiveDefinition.simple(id("custom_action_mission/objective"),
                        MissionObjectiveType.CUSTOM, "Bridge", "Custom action is visible.", ItemStack.EMPTY, 1))
                .actionProvider((player, mission, status, completeNow) ->
                        List.of(MissionActionView.enabled("custom_ping", "Ping Relay")))
                .actionHandler((player, mission, actionId) -> {
                    if ("custom_ping".equals(actionId)) {
                        handled.set(true);
                        return true;
                    }
                    return false;
                })
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        IMissionProgressView view = service.mission(player, missionId).orElseThrow();
        helper.assertTrue(view.actions().stream().anyMatch(action -> "custom_ping".equals(action.id())),
                "MissionCore view should merge Java custom actions into the action list.");
        helper.assertTrue(service.handleAction(player, missionId, "custom_ping"),
                "MissionCore should delegate unknown action ids to the Java action handler.");
        helper.assertTrue(handled.get(), "Custom action handler should receive delegated action id.");
        helper.succeed();
    }

    private static void worldCoreConsumer(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        EchoWorldRuntimeBus.clearForTests();
        MissionCoreWorldCoreConsumer.registerForTests();

        Identifier chapterId = id("worldcore_chapter");
        Identifier missionId = id("worldcore_bridge");
        Identifier regionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "crash_zone_wasteland");
        Identifier markerId = id("worldcore/marker");
        Identifier hazardId = id("worldcore/hazard");
        Identifier scanObjective = id("worldcore_bridge/scan");
        service.registerChapter("gametest", new MissionChapterDefinition(chapterId, "World", "WorldCore bridge tests", 0, 0x66E8FF));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("WorldCore Bridge", "Respond to shared world events.", "GameTest")
                .objective(new ObjectiveDefinition(id("worldcore_bridge/enter"),
                        MissionObjectiveType.ENTER_REGION, "Enter", "Enter the test region.", ItemStack.EMPTY,
                        1, true, Map.of("target", regionId.toString())))
                .objective(new ObjectiveDefinition(scanObjective,
                        MissionObjectiveType.DISCOVER_STRUCTURE, "Scan", "Scan the test marker.", ItemStack.EMPTY,
                        1, true, Map.of("target", regionId.toString())))
                .objective(new ObjectiveDefinition(id("worldcore_bridge/marker"),
                        MissionObjectiveType.CUSTOM, "Marker", "Reveal the test marker.", ItemStack.EMPTY,
                        1, true, Map.of("target", markerId.toString())))
                .objective(new ObjectiveDefinition(id("worldcore_bridge/hazard"),
                        MissionObjectiveType.CUSTOM, "Hazard", "Detect the test hazard.", ItemStack.EMPTY,
                        1, true, Map.of("target", hazardId.toString())))
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(service.startMission(player, missionId), "WorldCore bridge mission should start.");
        WorldRegionInstance region = new WorldRegionInstance(id("worldcore/region"), regionId,
                WorldRegionType.CRASH_ZONE, "Crash Zone", player.level().dimension(),
                BlockPos.ZERO, 96, List.of(), true);
        WorldMarker marker = new WorldMarker(markerId, regionId, WorldMarkerType.CRASH_SITE,
                "Crash Site", "Scanned crash site.", player.level().dimension(),
                BlockPos.ZERO, 64, true, player.level().getGameTime());

        EchoWorldRuntimeBus.fireRegionEntered(new EchoWorldRuntimeBus.RegionEntered(player, region));
        EchoWorldRuntimeBus.fireRegionDiscovered(new EchoWorldRuntimeBus.RegionDiscovered(
                player, region, WorldDiscoverySource.ENTER, true));
        EchoWorldRuntimeBus.fireRegionScanned(new EchoWorldRuntimeBus.RegionScanned(player, region, marker));
        EchoWorldRuntimeBus.fireMarkerRevealed(new EchoWorldRuntimeBus.MarkerRevealed(player, marker));
        EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(player,
                WorldHazardSnapshot.nominal(),
                new WorldHazardSnapshot(List.of(regionId), List.of(hazardId), 25, false, "Test hazard")));

        IMissionProgressView view = service.mission(player, missionId).orElseThrow();
        helper.assertTrue(view.objectives().stream().allMatch(objective -> objective.progress() >= objective.required()),
                "WorldCore runtime events should progress MissionCore region and scan objectives.");
        helper.assertTrue(view.status() == MissionStatus.COMPLETED,
                "WorldCore bridge mission should complete after matching events.");
        EchoWorldRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void runtimeSpineFoundationFlow(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("echodatacore"),
                "Foundation runtime-spine proof requires DataCore in the live GameTest runtime.");
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();
        EchoRuntimeSpineBus.clearForTests();
        MissionRuntimeBus.clearForTests();
        MissionCoreRuntimeSpineConsumer.registerForTests();
        registerDataCoreRuntimeSpineConsumerForTests(helper);

        Identifier chapterId = id("runtime_spine_chapter");
        Identifier missionId = id("runtime_spine_foundation_flow");
        Identifier targetId = id("runtime_spine/foundation_target");
        service.registerChapter("gametest", new MissionChapterDefinition(
                chapterId, "Runtime Spine", "Foundation spine live event proof.", 0, 0x55FFDD));
        service.registerMission("gametest", MissionDefinition.builder(missionId, chapterId)
                .text("Runtime Spine Flow", "Consume a shared module event.", "GameTest")
                .objective(new ObjectiveDefinition(id("runtime_spine_foundation_flow/objective"),
                        MissionObjectiveType.CUSTOM, "Shared Event", "Consume the shared runtime-spine event.",
                        ItemStack.EMPTY, 1, true, Map.of("target", targetId.toString())))
                .build());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(service.startMission(player, missionId), "Runtime-spine mission should start.");
        List<MissionRuntimeEvent> missionEvents = new ArrayList<>();
        Identifier eventId = id("gametest/runtime_spine_event");
        String sourceModule = "echoashfallprotocol";
        long worldEventsBefore = EchoCoreServices.worldData(player.level()).get(runtimeSpineWorldEventsKey());
        try {
            AutoCloseable listener = MissionRuntimeBus.register(missionEvents::add);
            try {
                boolean published = EchoRuntimeSpineBus.publish(EchoRuntimeSpineEvent.of(
                        sourceModule,
                        eventId,
                        player,
                        targetId,
                        1,
                        Map.of("objective_type", "custom", "proof", "foundation_spine_live")));
                helper.assertTrue(published, "Runtime-spine bus should publish the GameTest event.");
            } finally {
                listener.close();
            }
        } catch (Exception exception) {
            helper.fail("Runtime-spine listener cleanup failed: " + exception.getMessage());
        }

        IMissionProgressView view = service.mission(player, missionId).orElseThrow();
        helper.assertTrue(view.objectives().stream().allMatch(objective -> objective.progress() >= objective.required()),
                "Runtime-spine event should progress MissionCore objective state.");
        helper.assertTrue(view.status() == MissionStatus.COMPLETED,
                "Runtime-spine mission should complete from the shared event.");
        helper.assertTrue(missionEvents.stream().anyMatch(event -> MissionRuntimeEvent.OBJECTIVE_PROGRESSED.equals(event.eventType())
                        && event.context().containsKey("runtime_spine_event")
                        && sourceModule.equals(event.context().get("runtime_spine_source"))),
                "MissionCore runtime event should carry runtime-spine context.");

        helper.assertTrue(eventId.toString().equals(EchoCoreServices.playerData(player).get(runtimeSpineLastEventKey())),
                "DataCore should save the last runtime-spine event id.");
        helper.assertTrue(sourceModule.equals(EchoCoreServices.playerData(player).get(runtimeSpineLastSourceKey())),
                "DataCore should save the last runtime-spine source module.");
        helper.assertTrue(EchoCoreServices.playerData(player).get(runtimeSpineEventsKey()) >= 1L,
                "DataCore should increment the player runtime-spine event counter.");
        helper.assertTrue(EchoCoreServices.worldData(player.level()).get(runtimeSpineWorldEventsKey()) > worldEventsBefore,
                "DataCore should increment the world runtime-spine event counter.");

        EchoRuntimeSpineBus.clearForTests();
        MissionRuntimeBus.clearForTests();
        helper.succeed();
    }

    private static void registerDataCoreRuntimeSpineConsumerForTests(GameTestHelper helper) {
        try {
            Class.forName("com.knoxhack.echodatacore.integration.DataCoreRuntimeSpineConsumer")
                    .getMethod("registerForTests")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            helper.fail("DataCore runtime-spine test consumer registration failed: " + exception.getMessage());
        }
    }

    private static IDataKey<String> runtimeSpineLastEventKey() {
        return IDataKey.string(Identifier.fromNamespaceAndPath("echodatacore", "runtime_spine/last_event"),
                DataScope.PLAYER, "", true);
    }

    private static IDataKey<String> runtimeSpineLastSourceKey() {
        return IDataKey.string(Identifier.fromNamespaceAndPath("echodatacore", "runtime_spine/last_source"),
                DataScope.PLAYER, "", true);
    }

    private static IDataKey<Long> runtimeSpineEventsKey() {
        return IDataKey.counter(Identifier.fromNamespaceAndPath("echodatacore", "runtime_spine/events"),
                DataScope.PLAYER, 0L, true);
    }

    private static IDataKey<Long> runtimeSpineWorldEventsKey() {
        return IDataKey.counter(Identifier.fromNamespaceAndPath("echodatacore", "runtime_spine/world_events"),
                DataScope.WORLD, 0L, true);
    }

    private static void hookCoverage(GameTestHelper helper) {
        MissionCoreService service = MissionCoreService.INSTANCE;
        service.clearForTests();

        String directSource = "echoblackboxprotocol";
        Identifier directChapter = Identifier.fromNamespaceAndPath(directSource, "hook_chapter");
        Identifier directMission = Identifier.fromNamespaceAndPath(directSource, "decode_cache");
        Identifier directTarget = MissionHookTargets.objectiveTarget(directSource, directMission, 0);
        service.registerChapter(directSource, new MissionChapterDefinition(
                directChapter, "Hooks", "Direct hook coverage.", 0, 0x55FFDD));
        service.registerMission(directSource, MissionDefinition.builder(directMission, directChapter)
                .text("Decode Cache", "Decode the cache.", "Hook proof")
                .objective(new ObjectiveDefinition(Identifier.fromNamespaceAndPath(directSource, "decode_cache/objective"),
                        MissionObjectiveType.CUSTOM, "Decode", "Decode the cache.", ItemStack.EMPTY,
                        1, false, Map.of("target", directTarget.toString())))
                .build());
        EchoCoreServices.registerMissionHookCoverage(directSource, directMission, directTarget);
        EchoCoreServices.registerMissionHookCoverage(directSource, directMission, directTarget);

        String mixedSource = "echoconvoyprotocol";
        Identifier mixedChapter = Identifier.fromNamespaceAndPath(mixedSource, "hook_chapter");
        Identifier mixedMission = Identifier.fromNamespaceAndPath(mixedSource, "route_alpha");
        Identifier mixedTarget0 = MissionHookTargets.objectiveTarget(mixedSource, mixedMission, 0);
        Identifier mixedTarget1 = MissionHookTargets.objectiveTarget(mixedSource, mixedMission, 1);
        service.registerChapter(mixedSource, new MissionChapterDefinition(
                mixedChapter, "Convoy Hooks", "Mixed hook coverage.", 0, 0x55FFDD));
        service.registerMission(mixedSource, MissionDefinition.builder(mixedMission, mixedChapter)
                .text("Route Alpha", "Complete two route milestones.", "Hook proof")
                .objective(new ObjectiveDefinition(Identifier.fromNamespaceAndPath(mixedSource, "route_alpha/one"),
                        MissionObjectiveType.ESTABLISH_ROUTE, "Activate", "Activate the route.", ItemStack.EMPTY,
                        1, false, Map.of("target", mixedTarget0.toString())))
                .objective(new ObjectiveDefinition(Identifier.fromNamespaceAndPath(mixedSource, "route_alpha/two"),
                        MissionObjectiveType.ESTABLISH_ROUTE, "Complete", "Complete the route.", ItemStack.EMPTY,
                        1, false, Map.of("target", mixedTarget1.toString())))
                .build());
        EchoCoreServices.registerMissionHookCoverage(mixedSource, mixedMission, mixedTarget0);

        String adapterSource = "echoorbitalremnants";
        Identifier adapterChapter = Identifier.fromNamespaceAndPath(adapterSource, "hook_chapter");
        Identifier adapterMission = Identifier.fromNamespaceAndPath(adapterSource, "orbital_scan");
        Identifier adapterTarget = MissionHookTargets.objectiveTarget(adapterSource, adapterMission, 0);
        service.registerChapter(adapterSource, new MissionChapterDefinition(
                adapterChapter, "Orbital Hooks", "Adapter fallback coverage.", 0, 0x55FFDD));
        service.registerMission(adapterSource, MissionDefinition.builder(adapterMission, adapterChapter)
                .text("Orbital Scan", "Legacy scan state still imports.", "Hook proof")
                .objective(new ObjectiveDefinition(Identifier.fromNamespaceAndPath(adapterSource, "orbital_scan/objective"),
                        MissionObjectiveType.COMPLETE_ORBITAL_SCAN, "Scan", "Complete scan.", ItemStack.EMPTY,
                        1, false, Map.of("target", adapterTarget.toString())))
                .completionRule((player, mission) -> true)
                .build());

        Map<String, String> coverage = EchoCoreServices.missionHookCoverageSummary();
        helper.assertTrue("direct-hooks".equals(coverage.get(directSource)), "Full target coverage should report direct-hooks.");
        helper.assertTrue("mixed".equals(coverage.get(mixedSource)), "Partial target coverage should report mixed.");
        helper.assertTrue("adapter-state".equals(coverage.get(adapterSource)), "Missing hook coverage should report adapter-state.");
        helper.assertTrue(service.validateContent().stream().anyMatch(warning -> warning.contains("adapter-state")),
                "Validation should warn about adapter-state migrated sources.");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(service.startMission(player, mixedMission), "Mixed coverage mission should start.");
        helper.assertTrue(EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.ESTABLISH_ROUTE, mixedTarget0, 1,
                        MissionHookTargets.context(mixedSource, mixedMission, "route", "alpha")),
                "Direct route activation hook should progress.");
        helper.assertTrue(EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.ESTABLISH_ROUTE, mixedTarget1, 1,
                        MissionHookTargets.context(mixedSource, mixedMission, "route", "alpha")),
                "Direct route completion hook should progress.");
        helper.assertTrue(service.mission(player, mixedMission).orElseThrow().status() == MissionStatus.COMPLETED,
                "Direct hooks should complete the mission without Terminal state.");
        helper.assertFalse(EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.ESTABLISH_ROUTE, mixedTarget0, 1,
                        MissionHookTargets.context(mixedSource, mixedMission, "route", "alpha")),
                "Once-only completed missions should ignore duplicate hook progress.");
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoMissionCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static EchoDiscoveryEntry discoveryEntry(
            Identifier id,
            EchoDiscoveryCategory category,
            String title,
            int sortOrder) {
        return new EchoDiscoveryEntry(
                id,
                Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, "missioncore_tests"),
                category,
                title,
                "Locked " + title,
                "Complete the mission to reveal this signal.",
                title + " revealed.",
                null,
                null,
                0xFF55FFDD,
                null,
                sortOrder);
    }
}
