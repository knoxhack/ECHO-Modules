package com.knoxhack.signalos.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeSpineBus;
import com.echoplatform.echocore.api.EchoRuntimeSpineEvent;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.TerminalPlacementService;
import com.echoplatform.echocore.api.TerminalRewardService;
import com.echoplatform.echocore.api.mission.InMemoryMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsApi;
import com.knoxhack.signalos.api.SignalOsActionResult;
import com.knoxhack.signalos.api.SignalOsApp;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.api.SignalOsDriveFileSystem;
import com.knoxhack.signalos.api.SignalOsDriveResultCode;
import com.knoxhack.signalos.api.SignalOsDriveWriteResult;
import com.knoxhack.signalos.api.SignalOsDataProvider;
import com.knoxhack.signalos.api.SignalOsNetSite;
import com.knoxhack.signalos.api.SignalOsProviderStatus;
import com.knoxhack.signalos.api.TerminalActionRegistry;
import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalChapter;
import com.knoxhack.signalos.api.TerminalDiagnosticProvider;
import com.knoxhack.signalos.api.TerminalMission;
import com.knoxhack.signalos.block.entity.SignalOsServerRackBlockEntity;
import com.knoxhack.signalos.block.entity.SignalOsTerminalBlockEntity;
import com.knoxhack.signalos.content.SignalOsContentRegistry;
import com.knoxhack.signalos.content.SignalOsJsonContentLoader;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.kubejs.SignalOSKubeBridge;
import com.knoxhack.signalos.kubejs.SignalOSEvents;
import com.knoxhack.signalos.integration.SignalOsMissionCoreIntegration;
import com.knoxhack.signalos.integration.SignalOsRuntimeSpineBridge;
import com.knoxhack.signalos.menu.SignalOsServerRackMenu;
import com.knoxhack.signalos.menu.SignalOsTerminalMenu;
import com.knoxhack.signalos.network.SignalOsActionPacket;
import com.knoxhack.signalos.network.SignalOsRackActionPacket;
import com.knoxhack.signalos.network.SignalOsTerminalStatePacket;
import com.knoxhack.signalos.service.SignalOsBuiltinActions;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import com.knoxhack.signalos.service.SignalOsNetService;
import com.knoxhack.signalos.service.SignalOsPlayerData;
import com.knoxhack.signalos.service.SignalOsRackActions;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, SignalOS.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REGISTRY_SORTING =
            TEST_FUNCTIONS.register("registry_sorting", () -> ModGameTests::registrySorting);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DUPLICATE_ID_HANDLING =
            TEST_FUNCTIONS.register("duplicate_id_handling", () -> ModGameTests::duplicateIdHandling);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_PARSE_FAILURES =
            TEST_FUNCTIONS.register("json_parse_failures", () -> ModGameTests::jsonParseFailures);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_REFERENCE_VALIDATION =
            TEST_FUNCTIONS.register("json_reference_validation", () -> ModGameTests::jsonReferenceValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> KUBEJS_BRIDGE_ABSENCE =
            TEST_FUNCTIONS.register("kubejs_bridge_absence", () -> ModGameTests::kubejsBridgeAbsence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_READ_STATE =
            TEST_FUNCTIONS.register("archive_read_state", () -> ModGameTests::archiveReadState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REWARD_STORAGE_FLOW =
            TEST_FUNCTIONS.register("reward_storage_flow", () -> ModGameTests::rewardStorageFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SERVER_ACTION_VALIDATION =
            TEST_FUNCTIONS.register("server_action_validation", () -> ModGameTests::serverActionValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLAYER_DATA_REWRITES =
            TEST_FUNCTIONS.register("player_data_rewrites", () -> ModGameTests::playerDataRewrites);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OWNED_TERMINAL_REWARD_FLOW =
            TEST_FUNCTIONS.register("owned_terminal_reward_flow", () -> ModGameTests::ownedTerminalRewardFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ECHO_CORE_SERVICE_GUARD =
            TEST_FUNCTIONS.register("echo_core_service_guard", () -> ModGameTests::echoCoreServiceGuard);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_STATE_SNAPSHOT =
            TEST_FUNCTIONS.register("terminal_state_snapshot", () -> ModGameTests::terminalStateSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MENU_VALIDITY =
            TEST_FUNCTIONS.register("terminal_menu_validity", () -> ModGameTests::terminalMenuValidity);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> APP_REGISTRY_AND_DATA =
            TEST_FUNCTIONS.register("app_registry_and_data", () -> ModGameTests::appRegistryAndData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATA_DRIVE_COMPONENT_FLOW =
            TEST_FUNCTIONS.register("data_drive_component_flow", () -> ModGameTests::dataDriveComponentFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COMPUTER_NETWORK_DISCOVERY =
            TEST_FUNCTIONS.register("computer_network_discovery", () -> ModGameTests::computerNetworkDiscovery);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NOTE_EDITING_FLOW =
            TEST_FUNCTIONS.register("note_editing_flow", () -> ModGameTests::noteEditingFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SERVER_RACK_MENU_ACTIONS =
            TEST_FUNCTIONS.register("server_rack_menu_actions", () -> ModGameTests::serverRackMenuActions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CUSTOM_APP_RECORD_VIEW =
            TEST_FUNCTIONS.register("custom_app_record_view", () -> ModGameTests::customAppRecordView);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
            TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> API_CONTEXT_AND_TEMPLATES =
            TEST_FUNCTIONS.register("api_context_and_templates", () -> ModGameTests::apiContextAndTemplates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SIGNALNET_JSON_AND_SEARCH =
            TEST_FUNCTIONS.register("signalnet_json_and_search", () -> ModGameTests::signalNetJsonAndSearch);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SIGNALNET_DRIVE_ACTIONS =
            TEST_FUNCTIONS.register("signalnet_drive_actions", () -> ModGameTests::signalNetDriveActions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNTIME_SPINE_ACTION =
            TEST_FUNCTIONS.register("runtime_spine_action", () -> ModGameTests::runtimeSpineAction);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("signalos_mvp"));
        register(event, environment, "registry_sorting", REGISTRY_SORTING.getId());
        register(event, environment, "duplicate_id_handling", DUPLICATE_ID_HANDLING.getId());
        register(event, environment, "json_parse_failures", JSON_PARSE_FAILURES.getId());
        register(event, environment, "json_reference_validation", JSON_REFERENCE_VALIDATION.getId());
        register(event, environment, "kubejs_bridge_absence", KUBEJS_BRIDGE_ABSENCE.getId());
        register(event, environment, "archive_read_state", ARCHIVE_READ_STATE.getId());
        register(event, environment, "reward_storage_flow", REWARD_STORAGE_FLOW.getId());
        register(event, environment, "server_action_validation", SERVER_ACTION_VALIDATION.getId());
        register(event, environment, "player_data_rewrites", PLAYER_DATA_REWRITES.getId());
        register(event, environment, "owned_terminal_reward_flow", OWNED_TERMINAL_REWARD_FLOW.getId());
        register(event, environment, "echo_core_service_guard", ECHO_CORE_SERVICE_GUARD.getId());
        register(event, environment, "terminal_state_snapshot", TERMINAL_STATE_SNAPSHOT.getId());
        register(event, environment, "terminal_menu_validity", TERMINAL_MENU_VALIDITY.getId());
        register(event, environment, "app_registry_and_data", APP_REGISTRY_AND_DATA.getId());
        register(event, environment, "data_drive_component_flow", DATA_DRIVE_COMPONENT_FLOW.getId());
        register(event, environment, "computer_network_discovery", COMPUTER_NETWORK_DISCOVERY.getId());
        register(event, environment, "note_editing_flow", NOTE_EDITING_FLOW.getId());
        register(event, environment, "server_rack_menu_actions", SERVER_RACK_MENU_ACTIONS.getId());
        register(event, environment, "custom_app_record_view", CUSTOM_APP_RECORD_VIEW.getId());
        register(event, environment, "missioncore_content_registration", MISSION_CORE_CONTENT.getId());
        register(event, environment, "api_context_and_templates", API_CONTEXT_AND_TEMPLATES.getId());
        register(event, environment, "signalnet_json_and_search", SIGNALNET_JSON_AND_SEARCH.getId());
        register(event, environment, "signalnet_drive_actions", SIGNALNET_DRIVE_ACTIONS.getId());
        register(event, environment, "runtime_spine_action", RUNTIME_SPINE_ACTION.getId());
    }

    private static void registrySorting(GameTestHelper helper) {
        SignalOsContentRegistry.withClearedForTests(() -> {
            SignalOsContentRegistry.registerChapter(TerminalChapter.builder("signalos_test:zeta")
                    .title("Zeta")
                    .section("progress")
                    .order(40)
                    .build());
            SignalOsContentRegistry.registerChapter(TerminalChapter.builder("signalos_test:alpha")
                    .title("Alpha")
                    .section("command")
                    .order(100)
                    .build());
            SignalOsContentRegistry.registerChapter(TerminalChapter.builder("signalos_test:beta")
                    .title("Beta")
                    .section("progress")
                    .order(10)
                    .build());
            List<TerminalChapter> chapters = SignalOsContentRegistry.chapters();
            helper.assertTrue(chapters.get(0).id().equals(testId("alpha")),
                    "Command section chapters should sort before progress chapters.");
            helper.assertTrue(chapters.get(1).id().equals(testId("beta")),
                    "Progress chapters should sort by configured order.");
        });
        helper.succeed();
    }

    private static void duplicateIdHandling(GameTestHelper helper) {
        SignalOsContentRegistry.withClearedForTests(() -> {
            SignalOsContentRegistry.registerChapter(TerminalChapter.builder("signalos_test:dupe").title("One").build());
            try {
                SignalOsContentRegistry.registerChapter(TerminalChapter.builder("signalos_test:dupe").title("Two").build());
                helper.fail("Duplicate Java chapter ids should be rejected.");
            } catch (IllegalArgumentException expected) {
                helper.assertTrue(expected.getMessage().contains("Duplicate SignalOS chapter id"),
                        "Duplicate exception should name the conflicting surface.");
            }
        });
        helper.succeed();
    }

    private static void jsonParseFailures(GameTestHelper helper) {
        try {
            SignalOsJsonContentLoader.parseMissionForTests(testId("broken"), new JsonObject());
            helper.fail("Mission JSON without a chapter should fail parsing.");
        } catch (JsonParseException expected) {
            helper.assertTrue(expected.getMessage().contains("chapter"),
                    "Mission JSON failure should mention the missing chapter field.");
        }

        JsonObject badObjectives = new JsonObject();
        badObjectives.addProperty("chapter", testId("json_chapter").toString());
        badObjectives.addProperty("objectives", "not-an-array");
        try {
            SignalOsJsonContentLoader.parseMissionForTests(testId("bad_objectives"), badObjectives);
            helper.fail("Mission JSON with non-array objectives should fail parsing.");
        } catch (JsonParseException expected) {
            helper.assertTrue(expected.getMessage().contains("objectives"),
                    "Mission objective type failures should mention the objectives field.");
        }

        JsonObject badRewards = new JsonObject();
        badRewards.addProperty("chapter", testId("json_chapter").toString());
        JsonArray rewards = new JsonArray();
        rewards.add("minecraft:bread");
        badRewards.add("displayRewards", rewards);
        try {
            SignalOsJsonContentLoader.parseMissionForTests(testId("bad_rewards"), badRewards);
            helper.fail("Mission JSON with non-object rewards should fail parsing.");
        } catch (JsonParseException expected) {
            helper.assertTrue(expected.getMessage().contains("displayRewards[0]"),
                    "Reward type failures should mention the exact reward entry.");
        }

        JsonObject badArchiveLines = new JsonObject();
        badArchiveLines.addProperty("chapter", testId("json_chapter").toString());
        badArchiveLines.addProperty("lines", "not-an-array");
        try {
            SignalOsJsonContentLoader.parseArchiveForTests(testId("bad_archive"), badArchiveLines);
            helper.fail("Archive JSON with non-array lines should fail parsing.");
        } catch (JsonParseException expected) {
            helper.assertTrue(expected.getMessage().contains("lines"),
                    "Archive line type failures should mention the lines field.");
        }

        JsonObject chapter = new JsonObject();
        chapter.addProperty("title", "JSON Chapter");
        TerminalChapter parsed = SignalOsJsonContentLoader.parseChapterForTests(testId("json_chapter"), chapter);
        helper.assertTrue(parsed.id().equals(testId("json_chapter")),
                "Chapter JSON should use its datapack file id.");
        helper.succeed();
    }

    private static void jsonReferenceValidation(GameTestHelper helper) {
        SignalOsContentRegistry.withClearedForTests(() -> {
            Identifier jsonChapter = testId("json_chapter");
            Identifier javaChapter = testId("java_chapter");
            Identifier okMission = testId("ok_mission");
            Identifier javaMission = testId("java_mission");
            Identifier orphanMission = testId("orphan_mission");
            Identifier rewardMission = testId("reward_mission");
            Identifier missingRewardMission = testId("missing_reward_mission");
            Identifier okArchive = testId("ok_archive");
            Identifier orphanArchive = testId("orphan_archive");

            SignalOsContentRegistry.registerChapter(TerminalChapter.builder(javaChapter)
                    .title("Java Chapter")
                    .build());

            SignalOsContentRegistry.LoadedContent loaded = new SignalOsContentRegistry.LoadedContent(
                    Map.of(jsonChapter, TerminalChapter.builder(jsonChapter).title("JSON Chapter").build()),
                    Map.of(
                            okMission, TerminalMission.builder(okMission).chapter(jsonChapter.toString()).build(),
                            javaMission, TerminalMission.builder(javaMission).chapter(javaChapter.toString()).build(),
                            orphanMission, TerminalMission.builder(orphanMission).chapter(testId("missing").toString()).build(),
                            rewardMission, TerminalMission.builder(rewardMission).chapter(jsonChapter.toString()).reward("minecraft:bread", 1).build(),
                            missingRewardMission, TerminalMission.builder(missingRewardMission).chapter(jsonChapter.toString()).reward("signalos_test:missing_reward", 1).build()),
                    Map.of(
                            okArchive, TerminalArchiveRecord.builder(okArchive).chapter(jsonChapter.toString()).build(),
                            orphanArchive, TerminalArchiveRecord.builder(orphanArchive).chapter(testId("missing").toString()).build()),
                    new SignalOsContentRegistry.LoadReport(7, 7, 0, 0, 0));

            SignalOsContentRegistry.LoadedContent validated =
                    SignalOsJsonContentLoader.validateReferencesForTests(loaded);
            helper.assertTrue(validated.missions().containsKey(okMission),
                    "JSON missions should keep references to JSON chapters loaded in the same pass.");
            helper.assertTrue(validated.missions().containsKey(javaMission),
                    "JSON missions should keep references to already-registered Java chapters.");
            helper.assertTrue(validated.missions().containsKey(rewardMission),
                    "JSON missions should keep registered reward item references.");
            helper.assertFalse(validated.missions().containsKey(orphanMission),
                    "JSON missions with missing chapters should be skipped.");
            helper.assertFalse(validated.missions().containsKey(missingRewardMission),
                    "JSON missions with missing reward item ids should be skipped without creating reload-time stacks.");
            helper.assertTrue(validated.archives().containsKey(okArchive),
                    "JSON archives should keep references to JSON chapters loaded in the same pass.");
            helper.assertFalse(validated.archives().containsKey(orphanArchive),
                    "JSON archives with missing chapters should be skipped.");
            helper.assertTrue(validated.report().rejectedReferences() == 3,
                    "JSON load report should count skipped missing references.");
        });
        helper.succeed();
    }

    private static void kubejsBridgeAbsence(GameTestHelper helper) {
        SignalOsContentRegistry.withClearedForTests(() -> {
            SignalOSKubeBridge.clearScriptContent();
            SignalOSEvents.content(event -> {
                event.chapter("signalos_test:script")
                        .title("Script Chapter")
                        .section("intel")
                        .page("missions")
                        .register();
                event.mission("signalos_test:script_mission")
                        .chapter("signalos_test:script")
                        .title("Script Mission")
                        .objective("Reload safely")
                        .reward("minecraft:bread", 1)
                        .register();
            });
            helper.assertTrue(SignalOsContentRegistry.chapters().stream()
                            .anyMatch(chapter -> chapter.id().equals(testId("script"))),
                    "Soft script bridge should register content without KubeJS classes loaded.");
            helper.assertTrue(SignalOsContentRegistry.missionsFor(testId("script")).size() == 1,
                    "Script bridge missions should merge with registry content.");
            SignalOSKubeBridge.clearScriptContent();
            helper.assertTrue(SignalOsContentRegistry.chapters().isEmpty(),
                    "Script bridge clear should be reload-safe.");
        });
        helper.succeed();
    }

    private static void archiveReadState(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Identifier archiveId = testId("archive");
        helper.assertFalse(SignalOsPlayerData.isArchiveRead(player, archiveId),
                "Archives should start unread for a new player.");
        SignalOsPlayerData.markArchiveRead(player, archiveId);
        helper.assertTrue(SignalOsPlayerData.isArchiveRead(player, archiveId),
                "Archive read state should persist in player data.");
        helper.succeed();
    }

    private static void rewardStorageFlow(GameTestHelper helper) {
        SignalOsTerminalBlockEntity terminal = new SignalOsTerminalBlockEntity(
                BlockPos.ZERO,
                ModBlocks.TERMINAL.get().defaultBlockState());
        helper.assertFalse(terminal.storeRewards("signalos_test:empty", List.of()),
                "Terminal should reject empty reward batches.");
        helper.assertFalse(terminal.storeRewards("signalos_test:empty_stack", List.of(ItemStack.EMPTY)),
                "Terminal should reject empty reward stacks.");
        helper.assertTrue(terminal.storeRewards("signalos_test:reward",
                        List.of(new ItemStack(Items.BREAD, 4), new ItemStack(Items.APPLE, 2))),
                "Terminal should store simple reward stacks.");
        helper.assertTrue(terminal.storedRewardCount() == 6,
                "Stored reward count should include every cached item.");
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < terminal.rewardSlotCount() + 1; i++) {
            overflow.add(new ItemStack(Items.WOODEN_SWORD));
        }
        helper.assertFalse(terminal.storeRewards("signalos_test:overflow", overflow),
                "Overflow reward batches should fail before committing partial stacks.");
        helper.assertTrue(terminal.storedRewardCount() == 6,
                "Failed overflow storage should leave existing rewards unchanged.");
        helper.succeed();
    }

    private static void serverActionValidation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SignalOsContentRegistry.withClearedForTests(() -> {
            Identifier lockedArchiveId = testId("locked_archive");
            Identifier openArchiveId = testId("open_archive");
            SignalOsContentRegistry.registerArchive(TerminalArchiveRecord.builder(lockedArchiveId)
                    .chapter(testId("chapter").toString())
                    .title("Locked Archive")
                    .locked(true)
                    .build());
            SignalOsContentRegistry.registerArchive(TerminalArchiveRecord.builder(openArchiveId)
                    .chapter(testId("chapter").toString())
                    .title("Open Archive")
                    .build());

            helper.assertFalse(SignalOsBuiltinActions.markArchiveRead(player, null),
                    "Null archive ids should not be marked read.");
            helper.assertFalse(SignalOsBuiltinActions.markArchiveRead(player, lockedArchiveId),
                    "Locked archive actions should be rejected.");
            helper.assertFalse(SignalOsPlayerData.isArchiveRead(player, lockedArchiveId),
                    "Locked archive records should not be marked read by server actions.");

            helper.assertTrue(SignalOsBuiltinActions.markArchiveRead(player, openArchiveId),
                    "Unlocked archive actions should be accepted.");
            helper.assertTrue(SignalOsPlayerData.isArchiveRead(player, openArchiveId),
                    "Unlocked archive records should be marked read by server actions.");

            Identifier missingMissionId = testId("missing_mission");
            helper.assertFalse(SignalOsPlayerData.isMissionClaimed(player, missingMissionId),
                    "Missing missions should not start marked claimed.");
        });
        helper.succeed();
    }

    private static void playerDataRewrites(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Identifier missionId = testId("mission_data");
        SignalOsPlayerData.markMissionClaimed(player, null);
        helper.assertFalse(SignalOsPlayerData.isMissionClaimed(player, null),
                "Null mission ids should be ignored by player data helpers.");

        CompoundTag signalOs = player.getPersistentData().getCompoundOrEmpty("signalos");
        signalOs.putString("claimed_mission_0", missionId.toString());
        signalOs.putString("claimed_mission_1", "");
        signalOs.putInt("claimed_mission_count", 2);
        player.getPersistentData().put("signalos", signalOs);
        SignalOsPlayerData.markMissionClaimed(player, missionId);
        CompoundTag rewritten = player.getPersistentData().getCompoundOrEmpty("signalos");
        helper.assertTrue(rewritten.getIntOr("claimed_mission_count", -1) == 1,
                "Persistent mission rewrites should de-duplicate values and drop blank stale entries.");

        Identifier archiveId = testId("archive_data");
        SignalOsPlayerData.markArchiveRead(player, archiveId);
        SignalOsPlayerData.markArchiveRead(player, archiveId);
        CompoundTag archiveData = player.getPersistentData().getCompoundOrEmpty("signalos");
        helper.assertTrue(archiveData.getIntOr("read_archive_count", -1) == 1,
                "Persistent archive rewrites should remain stable across repeated writes.");
        helper.succeed();
    }

    private static void ownedTerminalRewardFlow(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos terminalPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(terminalPos, ModBlocks.TERMINAL.get().defaultBlockState(), 3);
        SignalOsTerminalBlockEntity terminal =
                (SignalOsTerminalBlockEntity) helper.getLevel().getBlockEntity(terminalPos);
        helper.assertTrue(terminal != null, "Placed SignalOS terminal should create a block entity.");
        terminal.setOwnerIfMissing(player);
        SignalOsTerminalServices.rememberTerminal(player, terminalPos);

        helper.assertTrue(SignalOsTerminalServices.findOwnedTerminal(player, false) == terminal,
                "Owned terminal cache should resolve the remembered terminal without a broad search.");
        helper.assertTrue(terminal.storeRewards("signalos_test:owned", List.of(new ItemStack(Items.BREAD, 3))),
                "Owned terminal block entity should store valid rewards.");
        helper.assertTrue(SignalOsTerminalServices.pendingRewardCount(player) == 3,
                "Owned terminal cache should report cached reward item counts.");
        helper.assertTrue(terminal.claimAllRewards(player),
                "Owned terminal block entity should claim stored rewards.");
        helper.assertTrue(SignalOsTerminalServices.pendingRewardCount(player) == 0,
                "Claimed reward inbox should be empty on the server.");
        helper.succeed();
    }

    private static void echoCoreServiceGuard(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            boolean registered = SignalOsTerminalServices.registerEchoCoreServices();
            if (ModList.get().isLoaded("echoterminal")) {
                helper.assertFalse(registered,
                        "SignalOS should not claim Echo Core terminal services while ECHO Terminal is loaded.");
                helper.assertTrue(EchoServiceRegistry.find(TerminalPlacementService.class).isEmpty(),
                        "SignalOS should leave placement ownership empty when it defers to ECHO Terminal.");
                helper.assertTrue(EchoServiceRegistry.find(TerminalRewardService.class).isEmpty(),
                        "SignalOS should leave reward ownership empty when it defers to ECHO Terminal.");
                return;
            }

            helper.assertTrue(registered,
                    "SignalOS should claim empty Echo Core terminal services when ECHO Terminal is absent.");
            TerminalPlacementService placement =
                    EchoServiceRegistry.find(TerminalPlacementService.class).orElse(null);
            TerminalRewardService reward =
                    EchoServiceRegistry.find(TerminalRewardService.class).orElse(null);
            helper.assertTrue(placement != null, "SignalOS should register a placement provider.");
            helper.assertTrue(reward != null, "SignalOS should register a reward provider.");
            helper.assertTrue(EchoCoreServices.terminalStructureBlockState().is(ModBlocks.TERMINAL.get()),
                    "Echo Core terminal structure state should resolve to the SignalOS terminal.");
            helper.assertTrue(EchoCoreServices.isTerminalBlock(ModBlocks.TERMINAL.get().defaultBlockState()),
                    "Echo Core terminal block checks should recognize the SignalOS terminal.");
            helper.assertTrue(SignalOsTerminalServices.registerEchoCoreServices(),
                    "Repeated SignalOS provider registration should be a no-op success.");
            helper.assertTrue(EchoServiceRegistry.find(TerminalPlacementService.class).orElse(null) == placement,
                    "Repeated registration should keep the same SignalOS placement provider instance.");
            helper.assertTrue(EchoServiceRegistry.find(TerminalRewardService.class).orElse(null) == reward,
                    "Repeated registration should keep the same SignalOS reward provider instance.");
        });

        EchoServiceRegistry.withClearedForTests(() -> {
            TerminalPlacementService foreignPlacement = new TerminalPlacementService() {
                @Override
                public boolean placeTerminal(Level level, BlockPos pos, Player owner) {
                    return false;
                }

                @Override
                public BlockState structureBlockState() {
                    return Blocks.BARRIER.defaultBlockState();
                }
            };
            TerminalRewardService foreignReward = new TerminalRewardService() {
                @Override
                public boolean storeRewards(net.minecraft.server.level.ServerPlayer player, String missionId,
                        List<ItemStack> rewards) {
                    return false;
                }

                @Override
                public boolean claimRewards(net.minecraft.server.level.ServerPlayer player) {
                    return false;
                }
            };
            EchoCoreServices.registerTerminalPlacementService(foreignPlacement);
            EchoCoreServices.registerTerminalRewardService(foreignReward);
            helper.assertFalse(SignalOsTerminalServices.registerEchoCoreServices(),
                    "SignalOS should not replace an existing Echo Core terminal provider.");
            helper.assertTrue(EchoServiceRegistry.find(TerminalPlacementService.class).orElse(null) == foreignPlacement,
                    "Existing placement provider should remain untouched.");
            helper.assertTrue(EchoServiceRegistry.find(TerminalRewardService.class).orElse(null) == foreignReward,
                    "Existing reward provider should remain untouched.");
        });
        helper.succeed();
    }

    private static void terminalStateSnapshot(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SignalOsContentRegistry.withClearedForTests(() -> {
            Identifier chapterId = testId("state_chapter");
            Identifier completeMissionId = testId("complete_mission");
            Identifier claimedMissionId = testId("claimed_mission");
            Identifier readArchiveId = testId("read_archive");
            Identifier unreadArchiveId = testId("unread_archive");

            SignalOsContentRegistry.registerChapter(TerminalChapter.builder(chapterId)
                    .title("State Chapter")
                    .build());
            SignalOsContentRegistry.registerMission(TerminalMission.builder(completeMissionId)
                    .chapter(chapterId.toString())
                    .title("Complete Mission")
                    .build());
            SignalOsContentRegistry.registerMission(TerminalMission.builder(claimedMissionId)
                    .chapter(chapterId.toString())
                    .title("Claimed Mission")
                    .build());
            SignalOsContentRegistry.registerArchive(TerminalArchiveRecord.builder(readArchiveId)
                    .chapter(chapterId.toString())
                    .title("Read Archive")
                    .build());
            SignalOsContentRegistry.registerArchive(TerminalArchiveRecord.builder(unreadArchiveId)
                    .chapter(chapterId.toString())
                    .title("Unread Archive")
                    .build());

            SignalOsPlayerData.markMissionClaimed(player, claimedMissionId);
            SignalOsPlayerData.markArchiveRead(player, readArchiveId);

            SignalOsTerminalStatePacket state = SignalOsTerminalStatePacket.createForTests(
                    player,
                    mission -> mission.id().equals(completeMissionId),
                    7);

            helper.assertTrue(state.completedMissions().contains(completeMissionId),
                    "Terminal state should include server-resolved completed missions.");
            helper.assertFalse(state.completedMissions().contains(claimedMissionId),
                    "Terminal state should not mark unresolved missions complete just because they are claimed.");
            helper.assertTrue(state.claimedMissions().contains(claimedMissionId),
                    "Terminal state should include persisted claimed missions.");
            helper.assertTrue(state.readArchives().contains(readArchiveId),
                    "Terminal state should include persisted read archives.");
            helper.assertFalse(state.readArchives().contains(unreadArchiveId),
                    "Terminal state should omit unread archives.");
            helper.assertTrue(state.pendingRewardCount() == 7,
                    "Terminal state should carry the authoritative pending reward count.");

            List<SignalOsDataRecord> overflowRecords = new ArrayList<>();
            for (int i = 0; i < 270; i++) {
                overflowRecords.add(new SignalOsDataRecord(
                        testId("record/overflow_" + i),
                        "Overflow " + i,
                        "record",
                        "test",
                        "body",
                        i,
                        false));
            }
            SignalOsTerminalStatePacket overflowState = new SignalOsTerminalStatePacket(
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), 0,
                    "test", true, 0, 0, "", 0, 0, 0, 0, overflowRecords);
            helper.assertTrue(overflowState.dataRecords().size() == 256,
                    "Terminal state should cap oversized data record syncs instead of disconnecting the player.");
            SignalOsDataRecord overflowSummary = overflowState.dataRecords().getLast();
            helper.assertTrue(overflowSummary.id().equals(Identifier.fromNamespaceAndPath(
                            SignalOS.MODID, "terminal_state/overflow_records"))
                            && overflowSummary.body().contains("15 additional"),
                    "Terminal state should replace overflow records with a stable summary record.");
        });
        helper.succeed();
    }

    private static void terminalMenuValidity(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SignalOsTerminalMenu remoteMenu = new SignalOsTerminalMenu(1, player.getInventory());
        helper.assertTrue(remoteMenu.stillValid(player),
                "Key-opened SignalOS terminal menus should use remote access.");

        BlockPos emptyPos = helper.absolutePos(new BlockPos(3, 1, 3));
        SignalOsTerminalMenu missingBlockMenu = new SignalOsTerminalMenu(2, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), emptyPos));
        helper.assertFalse(missingBlockMenu.stillValid(player),
                "Block-opened SignalOS terminal menus should require a valid terminal block.");

        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.TERMINAL.get());
        BlockPos absolute = helper.absolutePos(terminalPos);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.5D, absolute.getZ() + 0.5D);
        SignalOsTerminalMenu blockMenu = new SignalOsTerminalMenu(3, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absolute));
        helper.assertTrue(blockMenu.stillValid(player),
                "Block-opened SignalOS terminal menus should stay valid near their terminal block.");

        BlockPos workstationPos = new BlockPos(2, 1, 1);
        helper.setBlock(workstationPos, ModBlocks.WORKSTATION.get());
        BlockPos workstationAbsolute = helper.absolutePos(workstationPos);
        player.setPos(workstationAbsolute.getX() + 0.5D, workstationAbsolute.getY() + 0.5D, workstationAbsolute.getZ() + 0.5D);
        SignalOsTerminalMenu workstationMenu = new SignalOsTerminalMenu(4, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), workstationAbsolute));
        helper.assertTrue(workstationMenu.stillValid(player),
                "Block-opened SignalOS terminal menus should accept workstation access blocks.");

        SignalOsTerminalBlockEntity workstationTerminal =
                (SignalOsTerminalBlockEntity) helper.getLevel().getBlockEntity(workstationAbsolute);
        ItemStack bootDrive = new ItemStack(ModBlocks.DATA_DRIVE.get());
        player.getInventory().setItem(0, bootDrive);
        SignalOsTerminalMenu driveMenu = new SignalOsTerminalMenu(5, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), workstationAbsolute), workstationTerminal);
        int playerDriveMenuSlot = -1;
        for (int i = SignalOsTerminalMenu.DRIVE_SLOT_COUNT; i < driveMenu.slots.size(); i++) {
            if (driveMenu.slots.get(i).getItem().is(ModBlocks.DATA_DRIVE.get())) {
                playerDriveMenuSlot = i;
                break;
            }
        }
        helper.assertTrue(playerDriveMenuSlot >= 0, "Terminal menu should expose player inventory drive slots.");
        helper.assertTrue(!driveMenu.quickMoveStack(player, playerDriveMenuSlot).isEmpty(),
                "Quick-moving a data drive into the terminal boot slot should succeed.");
        helper.assertTrue(workstationTerminal.activeDriveStack().is(ModBlocks.DATA_DRIVE.get()),
                "Quick-moved data drive should land in the terminal boot slot.");
        helper.assertTrue(workstationTerminal.activeDriveData().isV2Supported(),
                "No-component blank data drives should initialize as V2 when installed.");
        workstationTerminal.extractDrive();
        ItemStack legacyDrive = new ItemStack(ModBlocks.DATA_DRIVE.get());
        legacyDrive.set(ModDataComponents.DRIVE_DATA.get(), new SignalOsDriveData("Legacy", List.of()));
        helper.assertFalse(workstationTerminal.insertDrive(legacyDrive),
                "Legacy V1 data drives should be rejected by terminal boot slots.");
        player.getInventory().setItem(1, new ItemStack(Items.DIRT));
        int dirtMenuSlot = -1;
        for (int i = SignalOsTerminalMenu.DRIVE_SLOT_COUNT; i < driveMenu.slots.size(); i++) {
            if (driveMenu.slots.get(i).getItem().is(Items.DIRT)) {
                dirtMenuSlot = i;
                break;
            }
        }
        helper.assertTrue(driveMenu.quickMoveStack(player, dirtMenuSlot).isEmpty(),
                "Quick-moving non-drive items into the terminal boot slot should be rejected.");
        helper.succeed();
    }

    private static void appRegistryAndData(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SignalOsContentRegistry.withClearedForTests(() -> {
            Identifier appA = testId("app_a");
            Identifier appB = testId("app_b");
            SignalOsContentRegistry.registerApp(SignalOsApp.builder(appB).title("B").order(20).build());
            SignalOsContentRegistry.registerApp(SignalOsApp.builder(appA).title("A").order(10).build());
            helper.assertTrue(SignalOsContentRegistry.apps().getFirst().id().equals(appA),
                    "SignalOS apps should sort by order before id.");
            try {
                SignalOsContentRegistry.registerApp(SignalOsApp.builder(appA).title("Duplicate").build());
                helper.fail("Duplicate SignalOS app ids should be rejected.");
            } catch (IllegalArgumentException expected) {
                helper.assertTrue(expected.getMessage().contains("Duplicate SignalOS app id"),
                        "Duplicate app exception should name the app surface.");
            }
            Identifier recordId = testId("record/provider");
            SignalOsContentRegistry.registerDataProvider(new com.knoxhack.signalos.api.SignalOsDataProvider() {
                @Override
                public Identifier id() {
                    return testId("provider");
                }

                @Override
                public List<SignalOsDataRecord> records(Player ignored) {
                    return List.of(new SignalOsDataRecord(recordId, "Provider Record", "record", "test", "ok", 0, false));
                }

                @Override
                public SignalOsProviderStatus providerStatus(Player ignored) {
                    return new SignalOsProviderStatus(id(), "Provider Test", "ONLINE",
                            TerminalDiagnosticProvider.Severity.INFO, "healthy");
                }
            });
            helper.assertTrue(SignalOsContentRegistry.dataRecords(player).stream()
                            .anyMatch(record -> record.id().equals(recordId)),
                    "SignalOS data providers should feed desktop records.");
            helper.assertTrue(SignalOsApi.providerStatuses(player).stream()
                            .anyMatch(status -> status.id().equals(testId("provider"))
                                    && "ONLINE".equals(status.status())),
                    "SignalOS public API should expose provider health metadata.");
            SignalOsContentRegistry.replaceJsonContent(new SignalOsContentRegistry.LoadedContent(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(testId("template/b"), SignalOsDriveData.blankV2().withLabel("B"),
                            testId("template/a"), SignalOsDriveData.blankV2().withLabel("A")),
                    SignalOsContentRegistry.LoadReport.empty()));
            helper.assertTrue(SignalOsApi.driveTemplates().keySet().stream().toList().getFirst().equals(testId("template/a")),
                    "SignalOS public API should expose drive templates in stable id order.");
        });
        helper.succeed();
    }

    private static void dataDriveComponentFlow(GameTestHelper helper) {
        Identifier recordId = testId("drive/record");
        SignalOsDriveData legacy = new SignalOsDriveData("Legacy Drive", List.of());
        helper.assertFalse(legacy.isV2Supported(),
                "Drives decoded without schemaVersion should remain legacy V1 data.");
        SignalOsDriveData driveData = SignalOsDriveData.blankV2()
                .withLabel("Test Drive")
                .withRecord(new SignalOsDataRecord(recordId, "Drive Record", "record", "test", "stored", 0, false)
                        .withMetadata(SignalOsDriveFileSystem.META_PATH, "/records/drive_record.txt"))
                .withSetting("theme", "signal")
                .withSessionValue("selected_app", "signalos:files");
        ItemStack stack = new ItemStack(ModBlocks.DATA_DRIVE.get());
        stack.set(ModDataComponents.DRIVE_DATA.get(), driveData);
        helper.assertTrue(stack.get(ModDataComponents.DRIVE_DATA.get()).schemaVersion() == SignalOsDriveData.CURRENT_SCHEMA_VERSION,
                "SignalOS V2 data drives should carry an explicit schema version.");
        helper.assertTrue(stack.get(ModDataComponents.DRIVE_DATA.get()).records().getFirst().id().equals(recordId),
                "SignalOS data drives should persist typed records through their data component.");
        helper.assertTrue("/records/drive_record.txt".equals(stack.get(ModDataComponents.DRIVE_DATA.get())
                        .records().getFirst().metadataValue(SignalOsDriveFileSystem.META_PATH, "")),
                "SignalOS data records should persist bounded filesystem metadata.");
        helper.assertTrue("signal".equals(stack.get(ModDataComponents.DRIVE_DATA.get()).setting("theme", "")),
                "SignalOS data drives should persist OS settings through their data component.");
        helper.assertTrue("signalos:files".equals(stack.get(ModDataComponents.DRIVE_DATA.get()).sessionValue("selected_app", "")),
                "SignalOS data drives should persist OS session values through their data component.");
        helper.succeed();
    }

    private static void computerNetworkDiscovery(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos workstation = new BlockPos(1, 1, 1);
        BlockPos rackPos = new BlockPos(3, 1, 1);
        BlockPos relayPos = new BlockPos(4, 1, 1);
        helper.setBlock(workstation, ModBlocks.WORKSTATION.get());
        helper.setBlock(rackPos, ModBlocks.SERVER_RACK.get());
        helper.setBlock(relayPos, ModBlocks.NETWORK_RELAY.get());

        BlockPos workstationAbsolute = helper.absolutePos(workstation);
        player.setPos(workstationAbsolute.getX() + 0.5D, workstationAbsolute.getY() + 0.5D, workstationAbsolute.getZ() + 0.5D);
        SignalOsTerminalBlockEntity workstationTerminal;
        if (helper.getLevel().getBlockEntity(workstationAbsolute) instanceof SignalOsTerminalBlockEntity terminal) {
            terminal.setOwnerIfMissing(player);
            SignalOsTerminalServices.rememberTerminal(player, workstationAbsolute);
            workstationTerminal = terminal;
        } else {
            helper.fail("Workstation should create a SignalOS terminal block entity.");
            return;
        }

        Identifier recordId = testId("network/drive_record");
        ItemStack drive = new ItemStack(ModBlocks.DATA_DRIVE.get());
        drive.set(ModDataComponents.DRIVE_DATA.get(), SignalOsDriveData.blankV2().withLabel("Rack Drive").withRecord(
                new SignalOsDataRecord(recordId, "Rack Record", "record", "test", "rack", 0, false)
                        .withMetadata(SignalOsDriveFileSystem.META_PATH, "/records/rack_record.txt")));
        if (helper.getLevel().getBlockEntity(helper.absolutePos(rackPos)) instanceof SignalOsServerRackBlockEntity rack) {
            helper.assertTrue(rack.insertDrive(drive), "Server rack should accept SignalOS data drives.");
        } else {
            helper.fail("Server rack should create a SignalOS rack block entity.");
            return;
        }

        SignalOsComputerNetworkService.NetworkSnapshot snapshot = SignalOsComputerNetworkService.snapshot(player);
        helper.assertTrue(snapshot.online(), "Owned workstation should produce an online SignalOS network.");
        helper.assertFalse(snapshot.activeDrivePresent(),
                "Network snapshot should report no active boot drive before one is installed.");
        helper.assertTrue(snapshot.workstations() >= 1, "Network scan should count workstation access blocks.");
        helper.assertTrue(snapshot.serverRacks() >= 1, "Network scan should count server racks.");
        helper.assertTrue(snapshot.relays() >= 1, "Network scan should count network relays.");
        helper.assertTrue(snapshot.records().stream().anyMatch(record -> record.id().equals(recordId)),
                "Network scan should expose records from installed rack drives.");
        helper.assertTrue(snapshot.peripherals().stream().anyMatch(peripheral -> "rack".equals(peripheral.kind())),
                "Network scan should expose server racks as first-class peripherals.");
        helper.assertTrue(snapshot.peripherals().stream().anyMatch(peripheral -> "relay".equals(peripheral.kind())),
                "Network scan should expose network relays as first-class peripherals.");

        ItemStack bootDrive = new ItemStack(ModBlocks.DATA_DRIVE.get());
        helper.assertTrue(workstationTerminal.insertDrive(bootDrive), "Workstation terminal should accept a boot drive.");
        workstationTerminal.updateActiveDrive(driveData -> driveData.withRecord(
                new SignalOsDataRecord(testId("note/cache"), "Cache Note", "note", "Operator Notes",
                        "Network cache should refresh.", 1000, false),
                SignalOsDriveData.MAX_PLAYER_RECORDS));
        SignalOsComputerNetworkService.NetworkSnapshot refreshed = SignalOsComputerNetworkService.snapshot(player);
        helper.assertTrue(refreshed.activeDrivePresent() && "Blank Drive".equals(refreshed.activeDriveLabel()),
                "Network snapshot should expose active terminal boot-drive metadata.");
        helper.assertTrue(refreshed.activeDriveVersion() == SignalOsDriveData.CURRENT_SCHEMA_VERSION
                        && refreshed.activeDriveWritable(),
                "Network snapshot should expose active V2 drive schema and writable status.");
        helper.assertTrue(refreshed.records().stream().anyMatch(record -> "note".equals(record.type())
                        && "Cache Note".equals(record.title())),
                "Network snapshot cache should invalidate when active boot-drive records change.");
        helper.setBlock(relayPos, Blocks.AIR);
        SignalOsComputerNetworkService.NetworkSnapshot afterRelayRemoval = SignalOsComputerNetworkService.snapshot(player);
        helper.assertTrue(afterRelayRemoval.relays() == 0,
                "Network snapshot cache should invalidate when network relays are removed.");
        helper.succeed();
    }

    private static void noteEditingFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        SignalOsBuiltinActions.register();
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.WORKSTATION.get());
        BlockPos terminalAbsolute = helper.absolutePos(terminalPos);
        SignalOsTerminalBlockEntity terminal;
        if (helper.getLevel().getBlockEntity(terminalAbsolute) instanceof SignalOsTerminalBlockEntity found) {
            found.setOwnerIfMissing(player);
            SignalOsTerminalServices.rememberTerminal(player, terminalAbsolute);
            terminal = found;
        } else {
            helper.fail("Workstation should create a SignalOS terminal block entity.");
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("title", "Field Note");
        payload.addProperty("body", "Initial body");
        SignalOsActionResult noDriveSave = TerminalActionRegistry.handleResult(player, SignalOsBuiltinActions.PAGE_NOTES,
                SignalOsBuiltinActions.SAVE_NOTE, payload.toString());
        helper.assertTrue(noDriveSave.code() == SignalOsDriveResultCode.NO_ACTIVE_DRIVE,
                "Drive-only note saves should report a missing active drive.");
        helper.assertTrue(terminal.activeDriveData().records().isEmpty(),
                "Drive-only note saves should not create notes when no boot drive is installed.");

        ItemStack bootDrive = new ItemStack(ModBlocks.DATA_DRIVE.get());
        helper.assertTrue(terminal.insertDrive(bootDrive), "Terminal should accept a boot drive for note storage.");
        helper.assertTrue(TerminalActionRegistry.handle(player, SignalOsBuiltinActions.PAGE_NOTES,
                        SignalOsBuiltinActions.SAVE_NOTE, payload.toString()),
                "JSON note save should succeed with an active drive.");
        List<SignalOsDataRecord> notes = SignalOsDataDriveItem.data(terminal.activeDriveStack()).records().stream()
                .filter(record -> "note".equals(record.type()))
                .toList();
        helper.assertTrue(notes.size() == 1, "JSON note save should create a drive-backed note.");
        helper.assertTrue(notes.getFirst().metadataValue(SignalOsDriveFileSystem.META_PATH, "").startsWith("/notes/"),
                "Drive-backed notes should be stored as V2 filesystem records.");
        Identifier noteId = notes.getFirst().id();

        JsonObject update = new JsonObject();
        update.addProperty("id", noteId.toString());
        update.addProperty("title", "Updated Field Note");
        update.addProperty("body", "Updated body");
        helper.assertTrue(TerminalActionRegistry.handle(player, SignalOsBuiltinActions.PAGE_NOTES,
                        SignalOsBuiltinActions.SAVE_NOTE, update.toString()),
                "JSON note save should update an existing note by id.");
        List<SignalOsDataRecord> updated = SignalOsDataDriveItem.data(terminal.activeDriveStack()).records().stream()
                .filter(record -> "note".equals(record.type()))
                .toList();
        helper.assertTrue(updated.size() == 1 && "Updated Field Note".equals(updated.getFirst().title()),
                "Drive-backed note update should preserve one note and replace title/body.");

        helper.assertTrue(TerminalActionRegistry.handle(player, SignalOsBuiltinActions.PAGE_NOTES,
                        SignalOsBuiltinActions.SAVE_NOTE, "Legacy Title\nLegacy body"),
                "Legacy newline note payload should remain accepted.");
        helper.assertTrue(SignalOsDataDriveItem.data(terminal.activeDriveStack()).records().stream()
                        .filter(record -> "note".equals(record.type())).count() == 2,
                "Legacy note payload should create a second drive-backed note.");
        helper.assertTrue(TerminalActionRegistry.handle(player, SignalOsBuiltinActions.PAGE_NOTES,
                        SignalOsBuiltinActions.DELETE_NOTE, noteId.toString()),
                "Delete note action should be accepted.");
        helper.assertFalse(SignalOsDataDriveItem.data(terminal.activeDriveStack()).records().stream()
                        .anyMatch(note -> note.id().equals(noteId)),
                "Deleted note should be removed from the active drive.");
        TerminalActionRegistry.handle(player, SignalOsBuiltinActions.PAGE_NOTES,
                SignalOsBuiltinActions.CLEAR_NOTES, "");
        helper.assertTrue(SignalOsDataDriveItem.data(terminal.activeDriveStack()).records().stream()
                        .noneMatch(record -> "note".equals(record.type())),
                "Clear notes action should remove all drive-backed notes.");
        String longPreference = "x".repeat(SignalOsPlayerData.MAX_PREFERENCE_VALUE + 25);
        TerminalActionRegistry.handle(player, SignalOsBuiltinActions.PAGE_SETTINGS,
                SignalOsBuiltinActions.SET_PREFERENCE, "theme=" + longPreference);
        helper.assertTrue(SignalOsDataDriveItem.data(terminal.activeDriveStack()).setting("theme", "").length() <= 512,
                "Drive-backed SignalOS preferences should clamp stored values.");
        SignalOsDriveWriteResult fileResult = SignalOsApi.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.createFile("/files/api_note.txt", "API Note", "api body", "text/plain"));
        helper.assertTrue(fileResult.success(),
                "Public Drive API V2 filesystem writes should create files on the active drive.");
        helper.assertTrue(SignalOsDataDriveItem.data(terminal.activeDriveStack()).records().stream()
                        .anyMatch(record -> "/files/api_note.txt".equals(
                                record.metadataValue(SignalOsDriveFileSystem.META_PATH, ""))),
                "Drive API V2 files should persist absolute path metadata.");
        SignalOsActionResult badPath = TerminalActionRegistry.handleResult(player, SignalOsBuiltinActions.PAGE_FILES,
                SignalOsBuiltinActions.CREATE_FILE, "{\"path\":\"/../bad\"}");
        helper.assertTrue(badPath.code() == SignalOsDriveResultCode.INVALID_PATH,
                "File actions should return structured invalid-path failures.");
        helper.succeed();
    }

    private static void serverRackMenuActions(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos workstation = new BlockPos(1, 1, 1);
        BlockPos rackPos = new BlockPos(3, 1, 1);
        helper.setBlock(workstation, ModBlocks.WORKSTATION.get());
        helper.setBlock(rackPos, ModBlocks.SERVER_RACK.get());
        BlockPos workstationAbsolute = helper.absolutePos(workstation);
        if (helper.getLevel().getBlockEntity(workstationAbsolute) instanceof SignalOsTerminalBlockEntity terminal) {
            terminal.setOwnerIfMissing(player);
            SignalOsTerminalServices.rememberTerminal(player, workstationAbsolute);
        }
        SignalOsServerRackBlockEntity rack =
                (SignalOsServerRackBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rackPos));
        helper.assertTrue(rack != null, "Server rack should create a rack block entity.");
        BlockPos rackAbsolute = helper.absolutePos(rackPos);
        player.setPos(rackAbsolute.getX() + 0.5D, rackAbsolute.getY() + 0.5D, rackAbsolute.getZ() + 0.5D);

        ItemStack playerDrive = new ItemStack(ModBlocks.DATA_DRIVE.get());
        player.getInventory().setItem(0, playerDrive);
        SignalOsServerRackMenu menu = new SignalOsServerRackMenu(1, player.getInventory(), rack);
        int playerDriveMenuSlot = -1;
        for (int i = SignalOsServerRackMenu.DRIVE_SLOT_COUNT; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).getItem().is(ModBlocks.DATA_DRIVE.get())) {
                playerDriveMenuSlot = i;
                break;
            }
        }
        helper.assertTrue(playerDriveMenuSlot >= 0, "Rack menu should expose player inventory drive slots.");
        helper.assertTrue(!menu.quickMoveStack(player, playerDriveMenuSlot).isEmpty(),
                "Quick-moving a data drive from player inventory should succeed.");
        helper.assertTrue(rack.drives().getItem(0).is(ModBlocks.DATA_DRIVE.get()),
                "Quick-moved data drive should land in the rack drive bays.");
        helper.assertTrue(SignalOsDataDriveItem.data(rack.drives().getItem(0)).isV2Supported(),
                "No-component blank rack drives should initialize as V2.");
        player.getInventory().setItem(1, new ItemStack(Items.DIRT));
        int dirtMenuSlot = -1;
        for (int i = SignalOsServerRackMenu.DRIVE_SLOT_COUNT; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).getItem().is(Items.DIRT)) {
                dirtMenuSlot = i;
                break;
            }
        }
        helper.assertTrue(menu.quickMoveStack(player, dirtMenuSlot).isEmpty(),
                "Quick-moving non-drive items into rack bays should be rejected.");

        Identifier recordId = testId("rack/source_record");
        Identifier templateId = testId("template/field");
        SignalOsContentRegistry.withClearedForTests(() -> {
            SignalOsContentRegistry.registerDataProvider(new SignalOsDataProvider() {
                @Override
                public Identifier id() {
                    return testId("rack_provider");
                }

                @Override
                public List<SignalOsDataRecord> records(Player ignored) {
                    return List.of(new SignalOsDataRecord(recordId, "Source Record", "record", "test",
                            "copy me", 0, false));
                }
            });
            SignalOsContentRegistry.replaceJsonContent(new SignalOsContentRegistry.LoadedContent(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(templateId, SignalOsDriveData.blankV2().withLabel("Template Drive").withRecord(
                            new SignalOsDataRecord(testId("template/record"), "Template Record", "record",
                                    "template", "templated", 0, false)
                                    .withMetadata(SignalOsDriveFileSystem.META_PATH, "/template/record.txt"))),
                    SignalOsContentRegistry.LoadReport.empty()));

            player.containerMenu = menu;
            helper.assertFalse(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.COPY_RECORD, "not a valid id")),
                    "Rack copy action should reject malformed network record ids.");
            helper.assertFalse(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.APPLY_TEMPLATE, testId("template/missing").toString())),
                    "Rack template action should reject unavailable templates.");
            helper.assertTrue(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.COPY_RECORD, recordId.toString())),
                    "Rack copy action should write a selected network record to the selected drive.");
            helper.assertTrue(SignalOsDataDriveItem.data(rack.drives().getItem(0)).records().stream()
                            .anyMatch(record -> record.metadataValue("signalos.source_id", "").equals(recordId.toString())),
                    "Copied network record should persist on the data drive component.");
            helper.assertTrue(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.APPLY_TEMPLATE, templateId.toString())),
                    "Rack template action should merge loaded drive template records.");
            helper.assertTrue(SignalOsDataDriveItem.data(rack.drives().getItem(0)).records().stream()
                            .anyMatch(record -> record.id().equals(testId("template/record"))),
                    "Applied template record should persist on the data drive.");
            Identifier copiedRecordId = SignalOsDataDriveItem.data(rack.drives().getItem(0)).records().stream()
                    .filter(record -> record.metadataValue("signalos.source_id", "").equals(recordId.toString()))
                    .map(SignalOsDataRecord::id)
                    .findFirst()
                    .orElse(recordId);
            helper.assertTrue(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.REMOVE_RECORD, copiedRecordId.toString())),
                    "Rack remove action should delete a selected drive record.");
            helper.assertFalse(SignalOsDataDriveItem.data(rack.drives().getItem(0)).records().stream()
                            .anyMatch(record -> record.id().equals(copiedRecordId)),
                    "Removed record should no longer be present on the drive.");
            helper.assertTrue(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.RENAME_DRIVE, "Renamed Drive")),
                    "Rack rename action should update the drive label.");
            helper.assertTrue("Renamed Drive".equals(SignalOsDataDriveItem.data(rack.drives().getItem(0)).label()),
                    "Renamed drive label should persist on the component.");
            helper.assertTrue(SignalOsRackActions.handle(player,
                            new SignalOsRackActionPacket(helper.absolutePos(rackPos), 0,
                                    SignalOsRackActions.CLEAR_DRIVE, "")),
                    "Rack clear action should be accepted.");
            helper.assertTrue(SignalOsDataDriveItem.data(rack.drives().getItem(0)).records().isEmpty(),
                    "Clear action should remove drive records.");
        });
        helper.succeed();
    }

    private static void apiContextAndTemplates(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.WORKSTATION.get());
        BlockPos terminalAbsolute = helper.absolutePos(terminalPos);
        if (helper.getLevel().getBlockEntity(terminalAbsolute) instanceof SignalOsTerminalBlockEntity terminal) {
            terminal.setOwnerIfMissing(player);
            terminal.insertDrive(new ItemStack(ModBlocks.DATA_DRIVE.get()));
            SignalOsTerminalServices.rememberTerminal(player, terminalAbsolute);
        } else {
            helper.fail("Workstation should create a SignalOS terminal block entity.");
            return;
        }

        TerminalActionRegistry.withClearedForTests(() -> {
            Identifier appId = testId("app/context");
            Identifier actionId = testId("action/ping");
            final String[] observedNetwork = {""};
            final int[] observedTier = {-1};
            final boolean[] observedDrive = {false};
            final String[] observedDriveLabel = {""};
            final boolean[] observedWritable = {false};
            final int[] observedVersion = {0};
            SignalOsApi.registerAppAction(appId, actionId, (context, payload) -> {
                observedNetwork[0] = context.networkId();
                observedTier[0] = context.accessTier();
                observedDrive[0] = context.activeDrivePresent();
                observedDriveLabel[0] = context.activeDriveLabel();
                observedWritable[0] = context.activeDriveWritable();
                observedVersion[0] = context.activeDriveVersion();
            });
            helper.assertTrue(TerminalActionRegistry.handle(player, appId, actionId, "ping"),
                    "SignalOS app action handlers should be reachable through terminal actions.");
            helper.assertFalse("offline".equals(observedNetwork[0]),
                    "SignalOS app action context should include the active network id.");
            helper.assertTrue(observedTier[0] >= 2,
                    "SignalOS app action context should include workstation access tier.");
            helper.assertTrue(observedDrive[0] && "Blank Drive".equals(observedDriveLabel[0]),
                    "SignalOS app action context should include active boot-drive metadata.");
            helper.assertTrue(observedWritable[0] && observedVersion[0] == SignalOsDriveData.CURRENT_SCHEMA_VERSION,
                    "SignalOS app action context should include V2 drive write metadata.");
            Identifier resultActionId = testId("action/result_ping");
            SignalOsApi.registerAppActionResult(appId, resultActionId,
                    (context, payload) -> context.requireWritableDrive());
            helper.assertTrue(TerminalActionRegistry.handleResult(player, appId, resultActionId, "ping").success(),
                    "SignalOS result app actions should return structured success values.");
        });
        helper.succeed();
    }

    private static void signalNetJsonAndSearch(GameTestHelper helper) {
        JsonObject json = new JsonObject();
        json.addProperty("address", "echo.home");
        json.addProperty("title", "ECHO Home");
        json.addProperty("summary", "Test network");
        json.addProperty("requiredTier", 1);
        JsonArray tags = new JsonArray();
        tags.add("home");
        tags.add("status");
        json.add("tags", tags);
        JsonArray pages = new JsonArray();
        JsonObject home = new JsonObject();
        home.addProperty("path", "/");
        home.addProperty("title", "Home");
        home.addProperty("body", "Welcome to SignalNet.");
        JsonArray links = new JsonArray();
        JsonObject statusLink = new JsonObject();
        statusLink.addProperty("label", "Status");
        statusLink.addProperty("address", "echo.home/status");
        links.add(statusLink);
        home.add("links", links);
        pages.add(home);
        JsonObject status = new JsonObject();
        status.addProperty("path", "/status");
        status.addProperty("title", "Status");
        status.addProperty("body", "Network status page.");
        pages.add(status);
        json.add("pages", pages);

        SignalOsNetSite site = SignalOsJsonContentLoader.parseNetSiteForTests(testId("net/echo_home"), json);
        helper.assertTrue("echo.home".equals(site.address()), "SignalNet JSON should parse curated addresses.");
        helper.assertTrue(site.pages().size() == 2, "SignalNet JSON should parse page arrays.");
        helper.assertTrue("echo.home/status".equals(site.pageAddress(site.pages().get(1))),
                "SignalNet page addresses should combine site address and page path.");

        SignalOsContentRegistry.withClearedForTests(() -> {
            SignalOsContentRegistry.replaceJsonContent(new SignalOsContentRegistry.LoadedContent(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(site.id(), site),
                    SignalOsContentRegistry.LoadReport.empty()));
            helper.assertTrue(SignalOsNetService.visibleSites(null, 0).isEmpty(),
                    "SignalNet sites above the active access tier should be hidden.");
            List<SignalOsDataRecord> records = SignalOsNetService.records(null, 1);
            helper.assertTrue(records.size() == 2, "SignalNet visible sites should publish typed page records.");
            helper.assertTrue(SignalOsNetService.searchRecords(records, "status").size() == 2,
                    "SignalNet search should scan titles, addresses, tags, and body text.");
            helper.assertTrue(SignalOsNetService.recordForAddress(null, 1, "echo.home/status").isPresent(),
                    "SignalNet address lookup should resolve curated page addresses.");

            SignalOsNetSite duplicate = new SignalOsNetSite(testId("net/duplicate"), "echo.home",
                    "Duplicate", "", 0, List.of(), List.of(), 0);
            SignalOsContentRegistry.replaceJsonContent(new SignalOsContentRegistry.LoadedContent(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(site.id(), site, duplicate.id(), duplicate),
                    SignalOsContentRegistry.LoadReport.empty()));
            helper.assertTrue(SignalOsContentRegistry.netSites(null).size() == 1,
                    "SignalNet duplicate addresses should keep the first loaded site.");
        });
        helper.succeed();
    }

    private static void signalNetDriveActions(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        SignalOsBuiltinActions.register();
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.WORKSTATION.get());
        BlockPos terminalAbsolute = helper.absolutePos(terminalPos);
        SignalOsTerminalBlockEntity terminal;
        if (helper.getLevel().getBlockEntity(terminalAbsolute) instanceof SignalOsTerminalBlockEntity found) {
            found.setOwnerIfMissing(player);
            SignalOsTerminalServices.rememberTerminal(player, terminalAbsolute);
            terminal = found;
        } else {
            helper.fail("Workstation should create a SignalOS terminal block entity.");
            return;
        }

        SignalOsNetSite site = new SignalOsNetSite(testId("net/actions"), "echo.home", "ECHO Home",
                "Action test", 0, List.of("home"),
                List.of(new com.knoxhack.signalos.api.SignalOsNetPage(
                        "/", "Home", "Save me.", List.of(), 0)),
                0);
        SignalOsContentRegistry.withClearedForTests(() -> {
            SignalOsContentRegistry.replaceJsonContent(new SignalOsContentRegistry.LoadedContent(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(site.id(), site),
                    SignalOsContentRegistry.LoadReport.empty()));

            JsonObject payload = new JsonObject();
            payload.addProperty("address", "echo.home");
            SignalOsActionResult noDrive = TerminalActionRegistry.handleResult(player,
                    SignalOsBuiltinActions.PAGE_SIGNALNET, SignalOsBuiltinActions.SAVE_NET_PAGE, payload.toString());
            helper.assertTrue(noDrive.code() == SignalOsDriveResultCode.NO_ACTIVE_DRIVE,
                    "SignalNet save should report missing active drive.");

            helper.assertTrue(terminal.insertDrive(new ItemStack(ModBlocks.DATA_DRIVE.get())),
                    "Terminal should accept a V2 drive for SignalNet writes.");
            SignalOsActionResult bookmark = TerminalActionRegistry.handleResult(player,
                    SignalOsBuiltinActions.PAGE_SIGNALNET, SignalOsBuiltinActions.BOOKMARK_NET_PAGE, payload.toString());
            helper.assertTrue(bookmark.success(), "SignalNet bookmark should write to an active V2 drive.");
            SignalOsActionResult saved = TerminalActionRegistry.handleResult(player,
                    SignalOsBuiltinActions.PAGE_SIGNALNET, SignalOsBuiltinActions.SAVE_NET_PAGE, payload.toString());
            helper.assertTrue(saved.success(), "SignalNet saved page should copy the page record to an active V2 drive.");
            List<SignalOsDataRecord> driveRecords = SignalOsDataDriveItem.data(terminal.activeDriveStack()).records();
            helper.assertTrue(driveRecords.stream().anyMatch(record ->
                            "/signalnet/bookmarks/echo_home.url".equals(
                                    record.metadataValue(SignalOsDriveFileSystem.META_PATH, ""))),
                    "SignalNet bookmark should persist as a drive file.");
            helper.assertTrue(driveRecords.stream().anyMatch(record ->
                            "echo.home".equals(record.metadataValue(SignalOsNetService.META_ADDRESS, ""))),
                    "SignalNet saved page should preserve SignalNet address metadata.");
            SignalOsActionResult missing = TerminalActionRegistry.handleResult(player,
                    SignalOsBuiltinActions.PAGE_SIGNALNET, SignalOsBuiltinActions.SAVE_NET_PAGE, "missing.page");
            helper.assertTrue(missing.code() == SignalOsDriveResultCode.NOT_FOUND,
                    "SignalNet actions should reject unknown curated addresses.");
        });
        helper.succeed();
    }

    private static void runtimeSpineAction(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        SignalOsBuiltinActions.register();
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.WORKSTATION.get());
        BlockPos terminalAbsolute = helper.absolutePos(terminalPos);
        SignalOsTerminalBlockEntity terminal;
        if (helper.getLevel().getBlockEntity(terminalAbsolute) instanceof SignalOsTerminalBlockEntity found) {
            found.setOwnerIfMissing(player);
            helper.assertTrue(found.insertDrive(new ItemStack(ModBlocks.DATA_DRIVE.get())),
                    "Runtime-spine action proof needs a writable SignalOS drive.");
            SignalOsTerminalServices.rememberTerminal(player, terminalAbsolute);
            terminal = found;
        } else {
            helper.fail("Workstation should create a SignalOS terminal block entity.");
            return;
        }

        EchoRuntimeSpineBus.clearForTests();
        List<EchoRuntimeSpineEvent> events = new ArrayList<>();
        AutoCloseable registration = EchoRuntimeSpineBus.register(events::add);
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("title", "Runtime Spine Note");
            payload.addProperty("body", "Successful SignalOS writes should enter the shared runtime spine.");
            SignalOsActionResult result = TerminalActionRegistry.handleResult(player,
                    SignalOsBuiltinActions.PAGE_NOTES, SignalOsBuiltinActions.SAVE_NOTE, payload.toString());
            helper.assertTrue(result.success(), "SignalOS note save should produce a successful state mutation result.");
            helper.assertTrue(terminal.activeDriveData().records().stream()
                            .anyMatch(record -> "Runtime Spine Note".equals(record.title())),
                    "Successful SignalOS note action should mutate the active drive before runtime publication.");

            SignalOsActionPacket packet = new SignalOsActionPacket(
                    SignalOsBuiltinActions.PAGE_NOTES,
                    SignalOsBuiltinActions.SAVE_NOTE,
                    payload.toString());
            helper.assertTrue(SignalOsRuntimeSpineBridge.publishAction(player, packet, result),
                    "Successful SignalOS UI actions should publish into the runtime spine.");
            helper.assertTrue(events.size() == 1, "SignalOS runtime spine bridge should emit one event.");
            EchoRuntimeSpineEvent event = events.getFirst();
            helper.assertTrue(SignalOsRuntimeSpineBridge.SIGNALOS_ACTION_SUCCEEDED.equals(event.eventId()),
                    "SignalOS runtime event should use the SignalOS action-succeeded id.");
            helper.assertTrue(SignalOS.MODID.equals(event.sourceModule())
                            && SignalOsBuiltinActions.SAVE_NOTE.equals(event.targetId()),
                    "SignalOS runtime event should retain source module and action target.");
            helper.assertTrue("signalos".equals(event.contextValue("ui_surface"))
                            && SignalOsBuiltinActions.PAGE_NOTES.toString().equals(event.contextValue("action_page"))
                            && SignalOsBuiltinActions.SAVE_NOTE.toString().equals(event.contextValue("action_id")),
                    "SignalOS runtime event should carry UI page/action context.");

            events.clear();
            SignalOsActionResult failed = TerminalActionRegistry.handleResult(player,
                    SignalOsBuiltinActions.PAGE_FILES, SignalOsBuiltinActions.CREATE_FILE, "{\"path\":\"/../bad\"}");
            helper.assertFalse(failed.success(), "Invalid file writes should remain structured failures.");
            helper.assertFalse(SignalOsRuntimeSpineBridge.publishAction(player, new SignalOsActionPacket(
                            SignalOsBuiltinActions.PAGE_FILES, SignalOsBuiltinActions.CREATE_FILE, "{\"path\":\"/../bad\"}"),
                    failed), "Failed SignalOS UI actions must not publish runtime-spine success events.");
            helper.assertTrue(events.isEmpty(), "Failed SignalOS actions should not leak runtime-spine events.");
        } finally {
            try {
                registration.close();
            } catch (Exception ignored) {
            }
            EchoRuntimeSpineBus.clearForTests();
        }
        helper.succeed();
    }

    private static void customAppRecordView(GameTestHelper helper) {
        JsonObject json = new JsonObject();
        json.addProperty("title", "Filtered Records");
        json.addProperty("type", "field_records");
        json.addProperty("view", "records");
        JsonArray types = new JsonArray();
        types.add("record");
        json.add("recordTypes", types);
        JsonArray sources = new JsonArray();
        sources.add("SignalOS Core");
        json.add("recordSources", sources);
        json.addProperty("includeArchived", true);
        json.addProperty("emptyText", "No filtered records");
        SignalOsApp app = SignalOsJsonContentLoader.parseAppForTests(testId("filtered_app"), json);
        helper.assertTrue("records".equals(app.view()), "Custom app JSON should parse record view mode.");
        helper.assertTrue(app.recordTypes().contains("record"), "Custom app JSON should parse record type filters.");
        helper.assertTrue(app.recordSources().contains("signalos core"),
                "Custom app JSON should parse source filters case-insensitively.");
        helper.assertTrue(app.includeArchived(), "Custom app JSON should parse includeArchived.");
        helper.assertTrue("No filtered records".equals(app.emptyText()),
                "Custom app JSON should parse empty view text.");
        helper.succeed();
    }

    private static void missionCoreContentRegistration(GameTestHelper helper) {
        InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
        SignalOsMissionCoreIntegration.registerContent(registry);
        helper.assertTrue(registry.chapter(id("signalos")).isPresent(), "SignalOS MissionCore chapter should be owned by SignalOS.");
        assertMission(helper, registry, "boot_terminal", "boot", MissionObjectiveType.SCAN_BLOCK);
        assertMission(helper, registry, "rack_network_online", "rack", MissionObjectiveType.ESTABLISH_ROUTE);
        assertMission(helper, registry, "drive_record_flow", "record", MissionObjectiveType.UNLOCK_RESEARCH);
        helper.succeed();
    }

    private static void assertMission(
            GameTestHelper helper,
            InMemoryMissionRegistry registry,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type) {
        Identifier missionId = id(missionPath);
        MissionDefinition mission = registry.missionDefinition(missionId)
                .orElseThrow(() -> new AssertionError("Missing MissionCore mission: " + missionId));
        helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "SignalOS MissionCore missions should be side ops.");
        helper.assertTrue(!mission.rewards().isEmpty(), "SignalOS MissionCore mission should have a claimable reward: " + missionId);
        helper.assertTrue(mission.objectives().size() == 1, "SignalOS MissionCore mission should have one direct objective: " + missionId);
        helper.assertTrue(mission.objectives().getFirst().type() == type, "SignalOS objective type should stay stable: " + missionId);
        String target = mission.objectives().getFirst().criteria().get("target");
        helper.assertTrue(MissionHookTargets.objectiveTarget(SignalOS.MODID, missionId, objectiveKey).toString().equals(target),
                "SignalOS MissionCore objective target should use MissionHookTargets: " + missionId);
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
    }

    private static Identifier testId(String path) {
        return Identifier.fromNamespaceAndPath("signalos_test", path);
    }
}
