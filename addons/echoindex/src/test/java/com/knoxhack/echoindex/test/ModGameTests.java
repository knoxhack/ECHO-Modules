package com.knoxhack.echoindex.test;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoServiceRegistry;
import com.knoxhack.echocore.api.mission.IMissionProgressView;
import com.knoxhack.echocore.api.mission.IMissionService;
import com.knoxhack.echocore.api.mission.InMemoryMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.network.EchoPacketDirection;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IIndexRecipeProvider;
import com.knoxhack.echocore.api.index.IIndexSourceProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexMachineLayout;
import com.knoxhack.echocore.api.index.IndexMachineLayoutGauge;
import com.knoxhack.echocore.api.index.IndexMachineLayoutSlot;
import com.knoxhack.echocore.api.index.IndexProviderDiagnostic;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexRelation;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echocore.api.index.IndexSourceFact;
import com.knoxhack.echocore.api.index.IndexSourceKind;
import com.knoxhack.echocore.api.index.IndexVisibility;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echonetcore.config.EchoNetCoreConfig;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.client.IndexActions;
import com.knoxhack.echoindex.client.IndexScreenCorePages;
import com.knoxhack.echoindex.client.IndexUiState;
import com.knoxhack.echoindex.integration.IndexMissionCoreIntegration;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echoindex.network.IndexStateSyncPacket;
import com.knoxhack.echoindex.network.ModNetwork;
import com.knoxhack.echoindex.service.IndexDiscoveryStore;
import com.knoxhack.echoindex.service.IndexRecipeDisplayMetadata;
import com.knoxhack.echoindex.service.IndexRecipeActionState;
import com.knoxhack.echoindex.service.IndexRecipeLayoutType;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipePlanner;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexRecipeSnapshotCodec;
import com.knoxhack.echoindex.service.IndexRecipeSourceKind;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoindex.service.IndexSourceRecipeProvider;
import com.knoxhack.echoindex.service.VanillaIndexRecipeProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoIndex.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_CORE_CONTENT =
            TEST_FUNCTIONS.register("missioncore_content_registration", () -> ModGameTests::missionCoreContentRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECIPE_LIKE_PROVIDER_CARDS =
            TEST_FUNCTIONS.register("recipe_like_provider_cards", () -> ModGameTests::recipeLikeProviderCards);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> QUERY_RESULT_LIMITING =
            TEST_FUNCTIONS.register("recipe_query_result_limiting", () -> ModGameTests::recipeQueryResultLimiting);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VANILLA_RECIPE_GRID_METADATA =
            TEST_FUNCTIONS.register("vanilla_recipe_grid_metadata", () -> ModGameTests::vanillaRecipeGridMetadata);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECIPE_TRANSFER_ACTION_RECORDS_AFTER_SUCCESS =
            TEST_FUNCTIONS.register("recipe_transfer_action_records_after_success",
                    () -> ModGameTests::recipeTransferActionRecordsAfterSuccess);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCREENCORE_BOOKMARK_CLICK_DISPATCHES_NETCORE_ACTION =
            TEST_FUNCTIONS.register("screencore_bookmark_click_dispatches_netcore_action",
                    () -> ModGameTests::screenCoreBookmarkClickDispatchesNetCoreAction);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SOURCE_PROVIDER_FACTS =
            TEST_FUNCTIONS.register("source_provider_facts", () -> ModGameTests::sourceProviderFacts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_REWARD_SOURCE_FACTS =
            TEST_FUNCTIONS.register("mission_reward_source_facts", () -> ModGameTests::missionRewardSourceFacts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_PROCESS_PLAN_STATE =
            TEST_FUNCTIONS.register("machine_process_plan_state", () -> ModGameTests::machineProcessPlanState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTENT_PROVIDER_SNAPSHOT =
            TEST_FUNCTIONS.register("content_provider_snapshot", () -> ModGameTests::contentProviderSnapshot);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_LAYOUT_VALIDATION =
            TEST_FUNCTIONS.register("machine_layout_validation", () -> ModGameTests::machineLayoutValidation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIRST_PARTY_PROVIDER_MIGRATION =
            TEST_FUNCTIONS.register("first_party_provider_migration", () -> ModGameTests::firstPartyProviderMigration);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(ModGameTests::registerTests);
        registerTerminalGameTests(eventBus);
    }

    private static void registerTerminalGameTests(IEventBus eventBus) {
        if (!ModList.get().isLoaded("echoterminal")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echoindex.test.IndexTerminalGameTests")
                    .getMethod("register", IEventBus.class)
                    .invoke(null, eventBus);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoIndex.LOGGER.warn("ECHO: Index terminal GameTests could not be registered.", exception);
        }
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("index_missioncore"));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                200,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id("missioncore_content_registration"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, MISSION_CORE_CONTENT.getId()), data));
        event.registerTest(id("recipe_like_provider_cards"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, RECIPE_LIKE_PROVIDER_CARDS.getId()), data));
        event.registerTest(id("recipe_query_result_limiting"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, QUERY_RESULT_LIMITING.getId()), data));
        event.registerTest(id("vanilla_recipe_grid_metadata"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, VANILLA_RECIPE_GRID_METADATA.getId()), data));
        event.registerTest(id("recipe_transfer_action_records_after_success"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                        RECIPE_TRANSFER_ACTION_RECORDS_AFTER_SUCCESS.getId()), data));
        event.registerTest(id("screencore_bookmark_click_dispatches_netcore_action"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                        SCREENCORE_BOOKMARK_CLICK_DISPATCHES_NETCORE_ACTION.getId()), data));
        event.registerTest(id("source_provider_facts"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, SOURCE_PROVIDER_FACTS.getId()), data));
        event.registerTest(id("mission_reward_source_facts"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, MISSION_REWARD_SOURCE_FACTS.getId()), data));
        event.registerTest(id("machine_process_plan_state"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, MACHINE_PROCESS_PLAN_STATE.getId()), data));
        event.registerTest(id("content_provider_snapshot"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, CONTENT_PROVIDER_SNAPSHOT.getId()), data));
        event.registerTest(id("machine_layout_validation"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, MACHINE_LAYOUT_VALIDATION.getId()), data));
        event.registerTest(id("first_party_provider_migration"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, FIRST_PARTY_PROVIDER_MIGRATION.getId()), data));
    }

    private static void missionCoreContentRegistration(GameTestHelper helper) {
        InMemoryMissionRegistry registry = new InMemoryMissionRegistry();
        IndexMissionCoreIntegration.registerContent(registry);
        helper.assertTrue(registry.chapter(id("index")).isPresent(), "Index MissionCore chapter should be owned by Index.");
        assertMission(helper, registry, "open_search_entry", "open", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "inspect_recipe_source", "recipe", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "follow_source_note", "source", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "bookmark_record", "bookmark", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "pin_recipe_plan", "pin", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "transfer_recipe_plan", "transfer", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "read_tutorial_entry", "read", MissionObjectiveType.UNLOCK_RESEARCH);
        assertMission(helper, registry, "use_lens_shortcut", "lens", MissionObjectiveType.UNLOCK_RESEARCH);
        helper.succeed();
    }

    private static void sourceProviderFacts(GameTestHelper helper) {
        IIndexSourceProvider provider = new DummyIndexSourceProvider(
                id("provider/source_fixture"),
                List.of(IndexSourceFact.of(
                        Identifier.withDefaultNamespace("paper"),
                        id("source/provider_cache"),
                        IndexSourceKind.CACHE,
                        "Provider Cache",
                        List.of("Published by an addon source provider."),
                        Items.CHEST,
                        EchoIndex.MODID)));
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshotForTests(
                null,
                List.of(IndexSourceRecipeProvider.INSTANCE),
                List.of(provider));
        helper.assertTrue(snapshot.sourceFactCount() >= 1, "Source provider fact should count in diagnostics.");
        helper.assertTrue(snapshot.recipes().stream().anyMatch(recipe -> recipe.title().equals("Provider Cache")),
                "Source provider fact should become a source recipe card.");
        IndexRecipeView providerCard = snapshot.recipes().stream()
                .filter(recipe -> recipe.title().equals("Provider Cache"))
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Provider source card should be present."));
        helper.assertTrue(providerCard.machine().is(Items.CHEST),
                "Source provider card should keep its addon-supplied icon.");
        helper.assertTrue(snapshot.recipesFor(Items.PAPER).stream().anyMatch(recipe -> recipe.title().equals("Provider Cache")),
                "Source provider card should index by output item.");
        helper.succeed();
    }

    private static void missionRewardSourceFacts(GameTestHelper helper) {
        Identifier providerId = id("provider/mission_reward_fixture");
        IndexSourceFact rewardFact = new IndexSourceFact(
                Identifier.withDefaultNamespace("potion"),
                id("source/mission_reward/clean_water"),
                IndexSourceKind.MISSION_REWARD,
                "Mission Reward: Clean Water",
                List.of("Source type: Mission Reward", "Mission: Purify Clean Water"),
                new ItemStack(Items.POTION),
                EchoIndex.MODID);
        IIndexContentProvider provider = new DummyIndexContentProvider(providerId, new IndexContentSnapshot(
                providerId,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(rewardFact),
                List.of(),
                List.of()));

        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshotForTests(
                null,
                List.of(IndexSourceRecipeProvider.INSTANCE),
                List.of(),
                List.of(provider));
        List<IndexRecipeView> cleanWaterViews = snapshot.recipesFor(Items.POTION);
        IndexRecipeView rewardCard = cleanWaterViews.stream()
                .filter(IndexRecipeSourceKind::isSourceCard)
                .filter(recipe -> recipe.title().equals("Mission Reward: Clean Water"))
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Mission reward should be visible as a source card."));
        helper.assertTrue(IndexRecipeSourceKind.of(rewardCard) == IndexRecipeSourceKind.MISSION_REWARD,
                "Mission reward source facts should keep their reward source kind.");
        helper.assertFalse(cleanWaterViews.stream()
                        .anyMatch(recipe -> !IndexRecipeSourceKind.isSourceCard(recipe)
                                && recipe.title().contains("Mission Reward")),
                "Mission rewards should not be indexed as executable recipe outputs.");
        helper.succeed();
    }

    private static void machineProcessPlanState(GameTestHelper helper) {
        IndexRecipeView recipe = new IndexRecipeView(
                id("recipe/water_purifier/clean_water"),
                id("recipe/water_purifier"),
                "Clean Water Bottle",
                new ItemStack(Items.FURNACE),
                List.of(
                        IndexRecipeSlot.input(new ItemStack(Items.GLASS_BOTTLE)),
                        IndexRecipeSlot.machine(new ItemStack(Items.FURNACE)),
                        IndexRecipeSlot.output(new ItemStack(Items.POTION))),
                List.of("Water purifier process fixture."),
                80,
                false,
                EchoIndex.MODID);

        IndexRecipePlan plan = IndexRecipePlanner.plan(null, recipe);
        helper.assertTrue(plan.state() == IndexRecipeActionState.PLAN_ONLY,
                "Machine processes should stay plan-only until opened at their station.");
        helper.assertFalse(plan.ready(), "Machine processes should not report as ready recipe transfers.");
        helper.assertFalse(plan.needs().stream().anyMatch(need -> need.role() == IndexSlotRole.MACHINE),
                "Machine slots should not be counted as ingredient needs.");
        helper.assertTrue(plan.transferBlocker().contains("Furnace"),
                "Machine process blockers should point at the required station.");
        helper.succeed();
    }

    private static void contentProviderSnapshot(GameTestHelper helper) {
        Identifier providerId = id("provider/content_fixture");
        Identifier categoryId = id("content/category");
        Identifier entryId = id("content/entry");
        Identifier recipeCategoryId = id("content/recipe_category");
        Identifier recipeId = id("content/recipe_card");
        Identifier sourceId = id("content/source_fact");
        IndexContentSnapshot content = new IndexContentSnapshot(
                providerId,
                List.of(new IndexCategory(
                        categoryId,
                        "Content Fixture",
                        "Content provider fixture category",
                        new ItemStack(Items.BOOK),
                        10,
                        EchoIndex.MODID)),
                List.of(new IndexEntry(
                        entryId,
                        categoryId,
                        "Content Fixture Entry",
                        "Provider-owned entry",
                        "Published through the Index content snapshot API.",
                        "A compact fixture entry for GameTest coverage.",
                        new ItemStack(Items.BOOK),
                        EchoIndex.MODID,
                        List.of("content", "provider"),
                        IndexEntryState.VISIBLE,
                        List.of(),
                        List.of(Identifier.withDefaultNamespace("paper")),
                        List.of(recipeId),
                        10)),
                List.of(new IndexRecipeCategory(
                        recipeCategoryId,
                        "Content Recipes",
                        new ItemStack(Items.CRAFTING_TABLE),
                        0xFF66E8FF,
                        20)),
                List.of(new IndexRecipeView(
                        recipeId,
                        recipeCategoryId,
                        "Content Recipe Card",
                        new ItemStack(Items.CRAFTING_TABLE),
                        List.of(
                                IndexRecipeSlot.input(new ItemStack(Items.IRON_INGOT)),
                                IndexRecipeSlot.machine(new ItemStack(Items.CRAFTING_TABLE)),
                                IndexRecipeSlot.output(new ItemStack(Items.PAPER))),
                        List.of("Provider-owned recipe card."),
                        40,
                        false,
                        EchoIndex.MODID)),
                List.of(new IndexMachineLayout(
                        recipeId,
                        "fixture_machine",
                        "Fixture Machine Layout",
                        120,
                        70,
                        true,
                        List.of(
                                new IndexMachineLayoutSlot(0, IndexSlotRole.INPUT, "Input", 10, 26, 18, false),
                                new IndexMachineLayoutSlot(1, IndexSlotRole.MACHINE, "Machine", 50, 26, 18, false),
                                new IndexMachineLayoutSlot(2, IndexSlotRole.OUTPUT, "Output", 90, 26, 18, false)),
                        List.of(new IndexMachineLayoutGauge("progress", "Progress", 42, 10, 36, 6, 0xFF66E8FF)))),
                List.of(IndexSourceFact.of(
                        Identifier.withDefaultNamespace("paper"),
                        sourceId,
                        IndexSourceKind.CACHE,
                        "Content Source Fact",
                        List.of("Published through the unified content provider."),
                        Items.CHEST,
                        EchoIndex.MODID)),
                List.of(new IndexRelation(
                        id("content/relation"),
                        entryId,
                        sourceId,
                        "source",
                        "Source",
                        IndexVisibility.VISIBLE,
                        EchoIndex.MODID)),
                List.of(IndexProviderDiagnostic.warning(providerId, "Fixture warning")));
        IIndexContentProvider provider = new DummyIndexContentProvider(providerId, content);

        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshotForTests(
                null,
                List.of(IndexSourceRecipeProvider.INSTANCE),
                List.of(),
                List.of(provider));
        helper.assertTrue(snapshot.providerStats().stream().anyMatch(stats -> stats.providerId().equals(providerId)
                        && stats.adaptedRecipeCount() == 1
                        && stats.sourceFactCount() == 1),
                "Content provider snapshot should contribute provider diagnostics.");
        helper.assertTrue(snapshot.recipe(recipeId).isPresent(),
                "Content provider recipe cards should enter the recipe snapshot.");
        IndexRecipeDisplayMetadata metadata = snapshot.metadata(recipeId)
                .orElseThrow(() -> helper.assertionException("Content provider machine layout should enter metadata."));
        helper.assertTrue(metadata.hasMachineLayout(), "Content provider machine layout should be retained.");
        helper.assertTrue(metadata.machineLayout().exact(), "Exact layout flag should survive snapshot build.");
        CompoundTag encoded = IndexRecipeSnapshotCodec.encodeQueryResult(
                Identifier.withDefaultNamespace("paper"),
                snapshot,
                List.of(snapshot.recipe(recipeId).orElseThrow()),
                List.of(),
                List.of(),
                "");
        IndexRecipeDisplayMetadata decoded = IndexRecipeSnapshotCodec.decodeDisplayMetadata(
                        encoded.getListOrEmpty("recipes"))
                .get(recipeId);
        helper.assertTrue(decoded != null && decoded.hasMachineLayout(),
                "Machine layout metadata should round-trip through query sync codec.");
        helper.assertTrue(snapshot.recipesFor(Items.PAPER).stream()
                        .anyMatch(recipe -> recipe.id().equals(recipeId)),
                "Content provider output items should index recipe lookups.");
        helper.assertTrue(snapshot.recipesFor(Items.PAPER).stream()
                        .anyMatch(recipe -> recipe.title().equals("Content Source Fact")),
                "Content provider source facts should become source cards.");
        helper.assertTrue(snapshot.warnings().stream().anyMatch(warning -> warning.contains("Fixture warning")),
                "Content provider warnings should surface in diagnostics.");
        helper.succeed();
    }

    private static void machineLayoutValidation(GameTestHelper helper) {
        Identifier providerId = id("provider/bad_machine_layout");
        Identifier categoryId = id("layout/category");
        Identifier recipeId = id("layout/recipe");
        IndexRecipeCategory category = new IndexRecipeCategory(
                categoryId,
                "Layout Recipes",
                new ItemStack(Items.CRAFTING_TABLE),
                0xFF66E8FF,
                20);
        IndexRecipeView recipe = new IndexRecipeView(
                recipeId,
                categoryId,
                "Bad Layout Recipe",
                new ItemStack(Items.CRAFTING_TABLE),
                List.of(IndexRecipeSlot.input(new ItemStack(Items.IRON_INGOT))),
                List.of(),
                20,
                false,
                EchoIndex.MODID);
        IIndexContentProvider provider = new DummyIndexContentProvider(providerId, new IndexContentSnapshot(
                providerId,
                List.of(),
                List.of(),
                List.of(category),
                List.of(recipe),
                List.of(new IndexMachineLayout(
                        recipeId,
                        "bad_fixture",
                        "Bad Fixture",
                        80,
                        40,
                        true,
                        List.of(new IndexMachineLayoutSlot(99, IndexSlotRole.INPUT, "Bad", 10, 10, 18, false)),
                        List.of())),
                List.of(),
                List.of(),
                List.of()));
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshotForTests(
                null,
                List.of(),
                List.of(),
                List.of(provider));
        helper.assertTrue(snapshot.metadata(recipeId).isEmpty(),
                "Invalid machine layout should be dropped from recipe metadata.");
        helper.assertTrue(snapshot.warnings().stream().anyMatch(warning -> warning.contains("invalid slot index")),
                "Invalid machine layout should surface a diagnostic warning.");
        helper.succeed();
    }

    private static void firstPartyProviderMigration(GameTestHelper helper) {
        Path root = workspaceRoot();
        if (root == null) {
            helper.succeed();
            return;
        }
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(ModGameTests::isFirstPartySource)
                    .filter(path -> !isLegacyIndexApiAllowlisted(root, path))
                    .forEach(path -> collectLegacyRegistrations(root, path, offenders));
        } catch (IOException exception) {
            throw helper.assertionException("Could not scan first-party Index provider registrations: " + exception.getMessage());
        }
        helper.assertTrue(offenders.isEmpty(),
                "First-party addons must register Index content through IIndexContentProvider: " + offenders);
        helper.succeed();
    }

    private static void recipeLikeProviderCards(GameTestHelper helper) {
        List<IIndexRecipeProvider> providers = List.of(
                fixtureProvider("echologisticsnetwork", "recipe/logistics_loadouts", "Logistics Loadouts",
                        "recipe/loadout/test", "Loadout Delivery", "Delivery/restock request"),
                fixtureProvider("echoconvoyprotocol", "recipe/convoy_routes", "Convoy Routes",
                        "recipe/route/test", "Convoy Route", "Route readiness"),
                fixtureProvider("echoagriculturereclamation", "recipe/agriculture_reclamation", "Agriculture Reclamation",
                        "recipe/crop/test", "Hydroponic Growth", "Accelerated crop growth"),
                fixtureProvider("echomultiblockcore", "recipe/multiblock_automation", "Multiblock Automation",
                        "recipe/automation/test", "Workcell Automation", "Capability output"),
                fixtureProvider("echomissioncore", "recipe/mission_rewards", "Mission Rewards",
                        "recipe/mission/test", "Mission Reward", "Unlock progress"),
                fixtureProvider("echoworldcore", "recipe/world_sources", "World Sources",
                        "recipe/world_source/test", "World Source", "Hazard/source discovery"));

        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshotForTests(null, providers);
        helper.assertTrue(snapshot.providerCount() == providers.size(),
                "Each recipe-like Echo fixture provider should be represented in provider stats.");
        helper.assertTrue(snapshot.recipes().size() == providers.size(),
                "Each recipe-like Echo fixture provider should publish one recipe card.");
        for (IIndexRecipeProvider provider : providers) {
            Identifier providerId = provider.id();
            helper.assertTrue(snapshot.recipesForProvider(providerId).size() == 1,
                    "Provider should own exactly one recipe card: " + providerId);
        }
        helper.assertTrue(snapshot.usesFor(Items.IRON_INGOT).size() == providers.size(),
                "Item-backed inputs should index use lookups across recipe-like cards.");
        helper.assertTrue(snapshot.recipes().stream()
                        .allMatch(recipe -> IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.OUTPUT)),
                "Recipe-like cards with text-only outputs should still satisfy output role coverage.");
        helper.assertTrue(snapshot.recipes().stream().anyMatch(ModGameTests::hasTextOnlyOutput),
                "Recipe-like cards should render non-item outputs as labeled text slots.");
        helper.succeed();
    }

    private static void recipeQueryResultLimiting(GameTestHelper helper) {
        List<IndexRecipeView> uses = new ArrayList<>();
        for (int i = 0; i < 520; i++) {
            uses.add(queryFixtureRecipe(i));
        }
        CompoundTag tag = IndexRecipeSnapshotCodec.encodeQueryResult(
                id("query/test_item"),
                IndexRecipeSnapshot.empty(),
                List.of(),
                uses,
                List.of(),
                "");
        int totalUses = tag.getIntOr("use_count", 0);
        int visibleUses = tag.getIntOr("visible_use_count", 0);
        helper.assertTrue(totalUses == uses.size(), "Query result should keep the full use count.");
        helper.assertTrue(visibleUses > 0 && visibleUses < totalUses,
                "Query result should cap the visible use payload.");
        helper.assertTrue(IndexRecipeSnapshotCodec.decodeRecipeViews(tag.getListOrEmpty("uses")).size() == visibleUses,
                "Visible use count should match the encoded payload.");
        helper.assertTrue(tag.getStringOr("query_warning", "").contains("Showing first"),
                "Limited query result should include a user-visible warning.");
        helper.succeed();
    }

    private static void vanillaRecipeGridMetadata(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshotForTests(
                player, List.of(VanillaIndexRecipeProvider.INSTANCE));
        Identifier recipeId = Identifier.withDefaultNamespace("diamond_pickaxe");
        IndexRecipeDisplayMetadata metadata = snapshot.metadata(recipeId)
                .orElseThrow(() -> helper.assertionException("Diamond pickaxe display metadata should be present."));
        assertDiamondPickaxeGrid(helper, metadata, "provider metadata");

        IndexRecipeSnapshot decoded = IndexRecipeSnapshotCodec.decode(IndexRecipeSnapshotCodec.encode(snapshot));
        IndexRecipeDisplayMetadata decodedMetadata = decoded.metadata(recipeId)
                .orElseThrow(() -> helper.assertionException("Diamond pickaxe display metadata should survive codec."));
        assertDiamondPickaxeGrid(helper, decodedMetadata, "decoded metadata");

        IndexRecipeDisplayMetadata blank = new IndexRecipeDisplayMetadata(
                id("test/blank_grid"),
                IndexRecipeLayoutType.CRAFTING_SHAPED,
                3,
                3,
                List.of(),
                new ItemStack(Items.CRAFTING_TABLE),
                new ItemStack(Items.DIAMOND_PICKAXE));
        IndexRecipeDisplayMetadata fallback = blank.withFallbackInputCellsFromSlots(List.of(
                IndexRecipeSlot.input(new ItemStack(Items.DIAMOND)),
                IndexRecipeSlot.input(new ItemStack(Items.STICK))));
        helper.assertTrue(fallback.hasRenderableInputCells(),
                "Slot-derived fallback metadata should prevent a blank vanilla grid.");
        assertCell(helper, fallback, 0, Items.DIAMOND, "fallback cell 0 should render the first input.");
        assertCell(helper, fallback, 1, Items.STICK, "fallback cell 1 should render the second input.");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void recipeTransferActionRecordsAfterSuccess(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService service = new RecordingMissionService();
            EchoCoreServices.registerMissionService(service);
            Identifier transferTarget = MissionHookTargets.objectiveTarget(
                    EchoIndex.MODID,
                    id("transfer_recipe_plan"),
                    "transfer");

            ServerPlayer failedPlayer = helper.makeMockServerPlayerInLevel();
            ModNetwork.handleActionForTests(
                    new IndexActionPacket(IndexActionPacket.Action.TRANSFER_RECIPE, id("missing_recipe")),
                    failedPlayer);
            helper.assertFalse(service.recorded(MissionObjectiveType.UNLOCK_RESEARCH, transferTarget),
                    "Failed Index recipe transfers must not record MissionCore transfer progress.");

            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            helper.assertTrue(player.containerMenu instanceof AbstractCraftingMenu,
                    "Index recipe transfer proof requires the player inventory crafting grid.");
            player.getInventory().add(new ItemStack(Items.OAK_PLANKS, 2));
            ModNetwork.handleActionForTests(
                    new IndexActionPacket(IndexActionPacket.Action.TRANSFER_RECIPE,
                            Identifier.withDefaultNamespace("stick")),
                    player);
            AbstractCraftingMenu menu = (AbstractCraftingMenu) player.containerMenu;
            helper.assertTrue(menu.getInputGridSlots().stream().anyMatch(slot -> slot.hasItem()),
                    "Successful Index recipe transfer should place items into the crafting grid.");
            helper.assertTrue(service.recorded(MissionObjectiveType.UNLOCK_RESEARCH, transferTarget),
                    "Successful Index recipe transfer action should record MissionCore transfer progress.");
        });
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void screenCoreBookmarkClickDispatchesNetCoreAction(GameTestHelper helper) {
        boolean previousDebugLogging = EchoNetCoreConfig.DEBUG_PACKET_LOGGING.get();
        boolean previousDroppedLogging = EchoNetCoreConfig.LOG_DROPPED_PACKETS.get();
        boolean previousDebugPackets = EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.get();
        List<CustomPacketPayload> sentPayloads = new ArrayList<>();
        Identifier recipeId = id("screencore/bookmark_recipe");
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService service = new RecordingMissionService();
            EchoCoreServices.registerMissionService(service);
            try {
                IndexActions.register();
                IndexUiState.INSTANCE.selection().selectRecipe(recipeId);
                IndexUiState.INSTANCE.setCurrentPage(IndexScreenCorePages.RECIPE_DETAIL);
                EchoNetDebug.clearCountersForTests();
                EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(true);
                EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(true);
                EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(true);

                try (EchoNetClientActions.TestActionOverrideHandle ignored =
                             EchoNetClientActions.installActionOverrideForTests(payload -> {
                                 sentPayloads.add(payload);
                                 return Optional.of(true);
                             })) {
                    EchoScreenEngine.ClickActionProbeResult clickProbe = EchoScreenEngine.clickActionForTests(
                            IndexScreenCorePages.RECIPE_DETAIL,
                            recipeDetailContext(recipeId),
                            "index.add_bookmark",
                            1024,
                            550);
                    String clickDiagnostics = String.join(" | ", clickProbe.diagnostics());
                    helper.assertTrue(clickProbe.found(),
                            "Real Index ScreenCore recipe detail page should expose a clickable bookmark button. "
                                    + clickDiagnostics);
                    helper.assertTrue(clickProbe.handled(),
                            "Index ScreenCore bookmark click should be handled by the input router. "
                                    + clickDiagnostics);
                    helper.assertTrue("index.add_bookmark".equals(clickProbe.action()),
                            "Clicked Index ScreenCore component should resolve the bookmark action. "
                                    + clickDiagnostics);
                    helper.assertTrue(recipeId.toString().equals(clickProbe.actionValue()),
                            "Clicked Index ScreenCore bookmark button should resolve a concrete recipe id action value. "
                                    + clickDiagnostics);
                }

                helper.assertTrue(sentPayloads.size() == 1,
                        "Index ScreenCore recipe bookmark click should send one serverbound NetCore action.");
                helper.assertTrue(sentPayloads.getFirst() instanceof IndexActionPacket,
                        "Index ScreenCore bookmark click should send an IndexActionPacket, not a fake success result.");
                IndexActionPacket packet = (IndexActionPacket) sentPayloads.getFirst();
                helper.assertTrue(packet.action() == IndexActionPacket.Action.PIN_RECIPE,
                        "Index ScreenCore bookmark click should send PIN_RECIPE for the selected recipe.");
                helper.assertTrue(recipeId.equals(packet.targetId()),
                        "Index ScreenCore bookmark click should target the selected recipe id.");
                helper.assertTrue(EchoNetDebug.counterSnapshot().entrySet().stream()
                                .anyMatch(entry -> IndexActionPacket.ID.equals(entry.getKey().payloadId())
                                        && entry.getKey().direction() == EchoPacketDirection.SERVERBOUND
                                        && entry.getKey().kind() == EchoPacketKind.SERVERBOUND_ACTION
                                        && entry.getKey().accepted()
                                        && entry.getValue() >= 1L),
                        "NetCore counters should record the accepted serverbound Index bookmark click.");

                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                try (EchoNetSend.TestSendOverrideHandle ignored =
                             EchoNetSend.installSendOverrideForTests((target, payload, kind) -> Optional.of(true))) {
                    ModNetwork.handleActionForTests(packet, player);
                }
                Identifier pinTarget = MissionHookTargets.objectiveTarget(
                        EchoIndex.MODID,
                        id("pin_recipe_plan"),
                        "pin");
                helper.assertTrue(IndexDiscoveryStore.INSTANCE.isRecipePinned(player, recipeId),
                        "Index ScreenCore bookmark server handling should save recipe pin state.");
                helper.assertTrue(service.recorded(MissionObjectiveType.UNLOCK_RESEARCH, pinTarget),
                        "Index ScreenCore bookmark server handling should record MissionCore pin progress.");
                helper.assertTrue(EchoNetDebug.counterSnapshot().entrySet().stream()
                                .anyMatch(entry -> IndexStateSyncPacket.ID.equals(entry.getKey().payloadId())
                                        && entry.getKey().direction() == EchoPacketDirection.CLIENTBOUND
                                        && entry.getKey().kind() == EchoPacketKind.CLIENTBOUND_SYNC
                                        && entry.getKey().accepted()
                                        && entry.getValue() >= 1L),
                        "Index ScreenCore bookmark server handling should sync saved Index state through NetCore.");
            } finally {
                EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(previousDebugLogging);
                EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(previousDroppedLogging);
                EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(previousDebugPackets);
                EchoNetDebug.clearCountersForTests();
            }
        });
        helper.succeed();
    }

    private static EchoDataContext recipeDetailContext(Identifier recipeId) {
        List<String> notes = List.of("Authored Index ScreenCore bookmark click fixture.");
        Map<String, Object> selectedRecipe = Map.of(
                "id", recipeId.toString(),
                "title", "ScreenCore Bookmark Fixture",
                "icon", "minecraft:book",
                "typeName", "GameTest recipe",
                "layoutType", "GENERIC",
                "processingTime", 0,
                "debugInfo", "click-dispatch-fixture",
                "inputs", List.of(),
                "outputs", List.of(),
                "notes", notes);
        return EchoDataContext.empty()
                .missingPlaceholder("")
                .provider("index", (context, path) -> {
                    String key = String.join(".", path);
                    return switch (key) {
                        case "nav.sections" -> List.of();
                        case "recipe.selected" -> selectedRecipe;
                        case "recipe.selected.id" -> recipeId.toString();
                        case "recipe.selected.title" -> selectedRecipe.get("title");
                        case "recipe.selected.icon" -> selectedRecipe.get("icon");
                        case "recipe.selected.typeName" -> selectedRecipe.get("typeName");
                        case "recipe.selected.layoutType" -> selectedRecipe.get("layoutType");
                        case "recipe.selected.processingTime" -> selectedRecipe.get("processingTime");
                        case "recipe.selected.debugInfo" -> selectedRecipe.get("debugInfo");
                        case "recipe.selected.notes" -> notes;
                        case "recipe.inputs", "recipe.outputs" -> List.of();
                        case "recipe.machine" -> Map.of("name", "ScreenCore Test Bench");
                        case "recipe.machine.name" -> "ScreenCore Test Bench";
                        default -> "";
                    };
                });
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
        helper.assertTrue(mission.kind() == MissionKind.SIDE_OP, "Index MissionCore missions should be side ops.");
        helper.assertTrue(!mission.rewards().isEmpty(), "Index MissionCore mission should have a claimable reward: " + missionId);
        helper.assertTrue(mission.objectives().size() == 1, "Index MissionCore mission should have one direct objective: " + missionId);
        helper.assertTrue(mission.objectives().getFirst().type() == type, "Index objective type should stay stable: " + missionId);
        String target = mission.objectives().getFirst().criteria().get("target");
        helper.assertTrue(MissionHookTargets.objectiveTarget(EchoIndex.MODID, missionId, objectiveKey).toString().equals(target),
                "Index MissionCore objective target should use MissionHookTargets: " + missionId);
    }

    private static void assertDiamondPickaxeGrid(GameTestHelper helper, IndexRecipeDisplayMetadata metadata, String source) {
        helper.assertTrue(metadata.type() == IndexRecipeLayoutType.CRAFTING_SHAPED,
                "Diamond pickaxe " + source + " should use shaped crafting layout.");
        helper.assertTrue(metadata.width() == 3 && metadata.height() == 3,
                "Diamond pickaxe " + source + " should keep a 3x3 grid.");
        helper.assertTrue(metadata.hasRenderableInputCells(),
                "Diamond pickaxe " + source + " should have renderable ingredient cells.");
        assertCell(helper, metadata, 0, Items.DIAMOND, "Diamond pickaxe top-left cell should contain diamond.");
        assertCell(helper, metadata, 1, Items.DIAMOND, "Diamond pickaxe top-middle cell should contain diamond.");
        assertCell(helper, metadata, 2, Items.DIAMOND, "Diamond pickaxe top-right cell should contain diamond.");
        assertEmptyCell(helper, metadata, 3, "Diamond pickaxe middle-left cell should remain empty.");
        assertCell(helper, metadata, 4, Items.STICK, "Diamond pickaxe center cell should contain stick.");
        assertEmptyCell(helper, metadata, 5, "Diamond pickaxe middle-right cell should remain empty.");
        assertEmptyCell(helper, metadata, 6, "Diamond pickaxe bottom-left cell should remain empty.");
        assertCell(helper, metadata, 7, Items.STICK, "Diamond pickaxe bottom-middle cell should contain stick.");
        assertEmptyCell(helper, metadata, 8, "Diamond pickaxe bottom-right cell should remain empty.");
    }

    private static void assertCell(GameTestHelper helper, IndexRecipeDisplayMetadata metadata, int index,
            Item item, String message) {
        helper.assertTrue(index >= 0 && index < metadata.cells().size(), message + " Cell index exists.");
        helper.assertTrue(metadata.cells().get(index).stream().anyMatch(stack -> stack.is(item)), message);
    }

    private static void assertEmptyCell(GameTestHelper helper, IndexRecipeDisplayMetadata metadata, int index,
            String message) {
        helper.assertTrue(index >= 0 && index < metadata.cells().size(), message + " Cell index exists.");
        helper.assertTrue(metadata.cells().get(index).stream().noneMatch(stack -> stack != null && !stack.isEmpty()), message);
    }

    private static final class RecordingMissionService implements IMissionService {
        private final List<RecordedObjective> recordedObjectives = new ArrayList<>();

        boolean recorded(MissionObjectiveType type, Identifier target) {
            return recordedObjectives.stream()
                    .anyMatch(objective -> objective.type() == type && objective.target().equals(target));
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void registerChapter(String source, MissionChapterDefinition chapter) {
        }

        @Override
        public void registerMission(String source, MissionDefinition mission) {
        }

        @Override
        public Optional<MissionChapterDefinition> chapter(Identifier chapterId) {
            return Optional.empty();
        }

        @Override
        public Optional<MissionDefinition> missionDefinition(Identifier missionId) {
            return Optional.empty();
        }

        @Override
        public List<MissionChapterDefinition> chapters() {
            return List.of();
        }

        @Override
        public List<MissionDefinition> missionDefinitions() {
            return List.of();
        }

        @Override
        public List<IMissionProgressView> missions(Player player) {
            return List.of();
        }

        @Override
        public List<IMissionProgressView> missions(Player player, Identifier chapterId) {
            return List.of();
        }

        @Override
        public Optional<IMissionProgressView> mission(Player player, Identifier missionId) {
            return Optional.empty();
        }

        @Override
        public boolean startMission(ServerPlayer player, Identifier missionId) {
            return false;
        }

        @Override
        public boolean completeMission(ServerPlayer player, Identifier missionId) {
            return false;
        }

        @Override
        public boolean claimReward(ServerPlayer player, Identifier missionId) {
            return false;
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return false;
        }

        @Override
        public boolean recordObjective(
                ServerPlayer player,
                MissionObjectiveType type,
                Identifier target,
                int amount,
                Map<String, String> context) {
            recordedObjectives.add(new RecordedObjective(type, target));
            return true;
        }

        @Override
        public String debugState(Player player, Identifier missionId) {
            return "Recording mission service.";
        }

        private record RecordedObjective(MissionObjectiveType type, Identifier target) {
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoIndex.MODID, path);
    }

    private static Identifier namespacedId(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private static IIndexRecipeProvider fixtureProvider(
            String namespace,
            String categoryPath,
            String categoryTitle,
            String recipePath,
            String recipeTitle,
            String textOutput) {
        Identifier categoryId = namespacedId(namespace, categoryPath);
        ItemStack machine = new ItemStack(Items.CRAFTING_TABLE);
        return new DummyIndexRecipeProvider(
                namespacedId(namespace, "provider/index_recipes"),
                new IndexRecipeCategory(categoryId, categoryTitle, machine, 0xFF66E8FF, 500),
                new IndexRecipeView(
                        namespacedId(namespace, recipePath),
                        categoryId,
                        recipeTitle,
                        machine,
                        List.of(
                                IndexRecipeSlot.input(new ItemStack(Items.IRON_INGOT)),
                                IndexRecipeSlot.machine(machine),
                                new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), textOutput)),
                        List.of("Fixture coverage for " + categoryTitle),
                        0,
                        false,
                        namespace));
    }

    private static IndexRecipeView queryFixtureRecipe(int index) {
        return new IndexRecipeView(
                id("query/recipe_" + index),
                id("query/category"),
                "Query Fixture " + index,
                new ItemStack(Items.CRAFTING_TABLE),
                List.of(
                        IndexRecipeSlot.input(new ItemStack(Items.IRON_INGOT)),
                        IndexRecipeSlot.machine(new ItemStack(Items.CRAFTING_TABLE)),
                        IndexRecipeSlot.output(new ItemStack(Items.STICK))),
                List.of("Fixture recipe for packet-size limiting."),
                0,
                false,
                EchoIndex.MODID);
    }

    private static boolean hasTextOnlyOutput(IndexRecipeView recipe) {
        return recipe.slots().stream()
                .anyMatch(slot -> slot.role() == IndexSlotRole.OUTPUT
                        && slot.stacks().isEmpty()
                        && !slot.label().isBlank());
    }

    private static Path workspaceRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.exists(candidate.resolve("settings.gradle"))
                    && Files.isDirectory(candidate.resolve("addons"))
                    && Files.isDirectory(candidate.resolve("core"))) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isFirstPartySource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/addons/") || normalized.contains("/src/main/java/com/knoxhack/echoashfallprotocol/");
    }

    private static boolean isLegacyIndexApiAllowlisted(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        return relative.equals("addons/echoindex/src/main/java/com/knoxhack/echoindex/EchoIndex.java")
                || relative.equals("addons/echoindex/src/main/java/com/knoxhack/echoindex/test/ModGameTests.java")
                || relative.startsWith("addons/echoindex/src/main/java/com/knoxhack/echoindex/service/");
    }

    private static void collectLegacyRegistrations(Path root, Path path, List<String> offenders) {
        try {
            int lineNumber = 0;
            for (String line : Files.readAllLines(path)) {
                lineNumber++;
                if (line.contains("registerIndexProvider(")
                        || line.contains("registerIndexRecipeProvider(")
                        || line.contains("registerIndexSourceProvider(")) {
                    offenders.add(root.relativize(path).toString().replace('\\', '/') + ":" + lineNumber);
                }
            }
        } catch (IOException exception) {
            offenders.add(root.relativize(path).toString().replace('\\', '/') + ": unreadable");
        }
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return false;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoIndex.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private record DummyIndexRecipeProvider(
            Identifier providerId,
            IndexRecipeCategory category,
            IndexRecipeView recipe) implements IIndexRecipeProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public List<IndexRecipeCategory> recipeCategories(Player player) {
            return List.of(category);
        }

        @Override
        public List<IndexRecipeView> recipes(Player player) {
            return List.of(recipe);
        }
    }

    private record DummyIndexSourceProvider(
            Identifier providerId,
            List<IndexSourceFact> facts) implements IIndexSourceProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public List<IndexSourceFact> sourceFacts(Player player) {
            return facts;
        }
    }

    private record DummyIndexContentProvider(
            Identifier providerId,
            IndexContentSnapshot content) implements IIndexContentProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public IndexContentSnapshot snapshot(IndexBuildContext context) {
            return content;
        }
    }
}
