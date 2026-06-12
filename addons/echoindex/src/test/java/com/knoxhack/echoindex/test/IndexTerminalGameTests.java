package com.knoxhack.echoindex.test;

import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IIndexRecipeProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.integration.IndexTerminalImportRecipeProvider;
import com.knoxhack.echoindex.integration.IndexTerminalRecipeProvider;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IndexTerminalGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoIndex.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_IMPORT_CONVERSION =
            TEST_FUNCTIONS.register("terminal_import_recipe_conversion",
                    () -> IndexTerminalGameTests::terminalImportRecipeConversion);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_REGISTRY_REVISION =
            TEST_FUNCTIONS.register("terminal_recipe_registry_revision",
                    () -> IndexTerminalGameTests::terminalRecipeRegistryRevision);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEMANTIC_DUPLICATE_FILTERING =
            TEST_FUNCTIONS.register("semantic_duplicate_filtering",
                    () -> IndexTerminalGameTests::semanticDuplicateFiltering);

    private IndexTerminalGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(IndexTerminalGameTests::registerTests);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        TestData<Holder<TestEnvironmentDefinition<?>>> data = testData(event);
        event.registerTest(id("terminal_import_recipe_conversion"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                        TERMINAL_IMPORT_CONVERSION.getId()), data));
        event.registerTest(id("terminal_recipe_registry_revision"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                        TERMINAL_REGISTRY_REVISION.getId()), data));
        event.registerTest(id("semantic_duplicate_filtering"),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                        SEMANTIC_DUPLICATE_FILTERING.getId()), data));
    }

    private static void terminalImportRecipeConversion(GameTestHelper helper) {
        TerminalRecipeEntry entry = new TerminalRecipeEntry(
                id("terminal/custom_recipe"),
                id("terminal/category"),
                "Terminal Recipe",
                new ItemStack(Items.FURNACE),
                List.of(
                        TerminalRecipeSlot.input(Items.IRON_ORE),
                        TerminalRecipeSlot.catalyst(Items.COAL),
                        TerminalRecipeSlot.output(Items.IRON_INGOT),
                        TerminalRecipeSlot.text(TerminalRecipeSlot.Role.OUTPUT, "Output fluid #2 x100")),
                List.of(TerminalRecipeNote.info("Process note"), TerminalRecipeNote.warning("Careful")),
                80,
                true);

        IndexRecipeView view = IndexTerminalImportRecipeProvider.convertForTests(entry);
        helper.assertTrue(view.id().equals(id("terminal_import/echoindex/terminal/custom_recipe")),
                "Terminal import should namespace recipe ids under EchoIndex.");
        helper.assertTrue(view.categoryId().equals(entry.categoryId()), "Terminal import should preserve category ids.");
        helper.assertTrue(view.processTicks() == 80 && view.locked(),
                "Terminal import should preserve timing and lock state.");
        helper.assertTrue(view.machine().is(Items.FURNACE), "Terminal import should preserve machine stack.");
        helper.assertTrue(view.itemsForRole(IndexSlotRole.INPUT).contains(Items.IRON_ORE),
                "Terminal import should index inputs.");
        helper.assertTrue(view.itemsForRole(IndexSlotRole.CATALYST).contains(Items.COAL),
                "Terminal import should index catalysts.");
        helper.assertTrue(view.itemsForRole(IndexSlotRole.OUTPUT).contains(Items.IRON_INGOT),
                "Terminal import should index item outputs.");
        helper.assertTrue(view.slots().stream().anyMatch(slot -> slot.role() == IndexSlotRole.OUTPUT
                        && slot.stacks().isEmpty()
                        && slot.label().equals("Output fluid #2 x100")),
                "Terminal import should preserve text-only output slots.");
        helper.assertTrue(view.notes().contains("Warning: Careful"),
                "Terminal warnings should remain visible in Index notes.");
        helper.assertFalse(IndexTerminalImportRecipeProvider.importableForTests(IndexTerminalRecipeProvider.INSTANCE),
                "Terminal import should skip the Index-to-Terminal export provider.");
        helper.succeed();
    }

    private static void terminalRecipeRegistryRevision(GameTestHelper helper) {
        long before = TerminalRecipeRegistry.revision();
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        TerminalRecipeRegistry.addChangeListener(listener);
        try {
            TerminalRecipeRegistry.withClearedForTests(() ->
                    TerminalRecipeRegistry.register(new DummyTerminalRecipeProvider(id("dummy_terminal_provider"))));
        } finally {
            TerminalRecipeRegistry.removeChangeListener(listener);
        }
        helper.assertTrue(TerminalRecipeRegistry.revision() > before,
                "Terminal recipe registry revision should advance after provider mutations.");
        helper.assertTrue(changes.get() > 0, "Terminal recipe registry listeners should fire on provider mutations.");
        helper.succeed();
    }

    private static void semanticDuplicateFiltering(GameTestHelper helper) {
        Identifier categoryId = id("duplicate/category");
        Identifier directRecipeId = id("duplicate/direct_recipe");
        Identifier importedRecipeId = id("terminal_import/echoindex/duplicate/recipe");
        IIndexRecipeProvider directProvider = new DummyIndexRecipeProvider(
                id("provider/direct_duplicate"),
                new IndexRecipeCategory(categoryId, "Duplicate Recipes", new ItemStack(Items.CRAFTING_TABLE),
                        0xFF66E8FF, 1),
                new IndexRecipeView(
                        directRecipeId,
                        categoryId,
                        "Direct Duplicate",
                        new ItemStack(Items.CRAFTING_TABLE),
                        duplicateSlots(),
                        List.of("Direct provider owns this semantic recipe."),
                        20,
                        false,
                        EchoIndex.MODID));

        TerminalRecipeRegistry.withClearedForTests(() -> {
            TerminalRecipeRegistry.register(new DuplicateTerminalRecipeProvider(id("duplicate_terminal_provider")));
            IndexRecipeSnapshot importFirst = IndexService.INSTANCE.recipeSnapshotForTests(null,
                    List.of(directProvider),
                    List.of(),
                    List.of(IndexTerminalImportRecipeProvider.INSTANCE));
            helper.assertTrue(importFirst.recipe(directRecipeId).isPresent(),
                    "Direct Index recipe should replace an earlier Terminal-import duplicate.");
            helper.assertFalse(importFirst.recipe(importedRecipeId).isPresent(),
                    "Terminal-import duplicate should be removed when a direct provider owns the same recipe.");
            helper.assertTrue(importFirst.warnings().stream()
                            .anyMatch(warning -> warning.contains("Terminal-import duplicate replaced")),
                    "Replacement should be visible in Index recipe diagnostics.");

            IIndexContentProvider directContentProvider = contentRecipeProvider(
                    id("provider/direct_content_duplicate"),
                    new IndexRecipeCategory(categoryId, "Duplicate Recipes", new ItemStack(Items.CRAFTING_TABLE),
                            0xFF66E8FF, 1),
                    directProvider.recipes(null).getFirst());
            IndexRecipeSnapshot directFirst = IndexService.INSTANCE.recipeSnapshotForTests(null,
                    List.of(),
                    List.of(),
                    List.of(directContentProvider, IndexTerminalImportRecipeProvider.INSTANCE));
            helper.assertTrue(directFirst.recipe(directRecipeId).isPresent(),
                    "Direct Index recipe should remain when it is seen before the Terminal import.");
            helper.assertFalse(directFirst.recipe(importedRecipeId).isPresent(),
                    "Terminal import should be skipped when the direct recipe already exists.");
            helper.assertTrue(directFirst.recipes().size() == 1,
                    "Semantic duplicate filtering should leave one visible recipe card.");
        });
        helper.succeed();
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("index_terminal"));
        return new TestData<>(
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
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoIndex.MODID, path);
    }

    private static List<IndexRecipeSlot> duplicateSlots() {
        return List.of(
                IndexRecipeSlot.input(new ItemStack(Items.IRON_INGOT)),
                IndexRecipeSlot.machine(new ItemStack(Items.CRAFTING_TABLE)),
                IndexRecipeSlot.output(new ItemStack(Items.STICK)));
    }

    private static IIndexContentProvider contentRecipeProvider(
            Identifier providerId,
            IndexRecipeCategory category,
            IndexRecipeView recipe) {
        return new DummyIndexContentProvider(providerId, new IndexContentSnapshot(
                providerId,
                List.of(),
                List.of(),
                List.of(category),
                List.of(recipe),
                List.of(),
                List.of(),
                List.of()));
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

    private record DuplicateTerminalRecipeProvider(Identifier providerId) implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            return List.of(new TerminalRecipeCategory(IndexTerminalGameTests.id("duplicate/category"),
                    "Duplicate Recipes", new ItemStack(Items.CRAFTING_TABLE), 0xFF66E8FF, 1));
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            return List.of(new TerminalRecipeEntry(
                    IndexTerminalGameTests.id("duplicate/recipe"),
                    IndexTerminalGameTests.id("duplicate/category"),
                    "Imported Duplicate",
                    new ItemStack(Items.CRAFTING_TABLE),
                    List.of(
                            TerminalRecipeSlot.input(Items.IRON_INGOT),
                            TerminalRecipeSlot.machine(new ItemStack(Items.CRAFTING_TABLE)),
                            TerminalRecipeSlot.output(Items.STICK)),
                    List.of(),
                    20,
                    false));
        }
    }

    private record DummyTerminalRecipeProvider(Identifier providerId) implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            return List.of(new TerminalRecipeCategory(IndexTerminalGameTests.id("dummy_category"), "Dummy",
                    new ItemStack(Items.CRAFTING_TABLE), 0xFF66E8FF, 1));
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            return List.of(new TerminalRecipeEntry(
                    IndexTerminalGameTests.id("dummy_recipe"),
                    IndexTerminalGameTests.id("dummy_category"),
                    "Dummy",
                    new ItemStack(Items.CRAFTING_TABLE),
                    List.of(TerminalRecipeSlot.output(Items.STICK)),
                    List.of(),
                    0,
                    false));
        }
    }
}
