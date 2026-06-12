package com.knoxhack.echowiki.test;

import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.basic.TextComponent;
import com.knoxhack.echoscreencore.client.component.basic.TitleComponent;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.engine.EchoBindingResolver;
import com.knoxhack.echoscreencore.client.input.EchoFocusManager;
import com.knoxhack.echoscreencore.client.layout.EchoLayoutEngine;
import com.knoxhack.echoscreencore.client.layout.EchoResponsiveContext;
import com.knoxhack.echoscreencore.client.overlay.EchoOverlayManager;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.render.EchoThemeBridge;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.api.EchoWikiApi;
import com.knoxhack.echowiki.client.WikiDataProviders;
import com.knoxhack.echowiki.client.WikiScreenCoreBridge;
import com.knoxhack.echowiki.client.WikiScreenCorePages;
import com.knoxhack.echowiki.client.WikiUiState;
import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookLabels;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.content.GuideBookTarget;
import com.knoxhack.echowiki.content.WikiArticleDefinition;
import com.knoxhack.echowiki.content.WikiArticleSection;
import com.knoxhack.echowiki.content.WikiCollectionDefinition;
import com.knoxhack.echowiki.content.WikiContentRegistry;
import com.knoxhack.echowiki.content.WikiJsonReloadListener;
import com.knoxhack.echowiki.integration.GuideBookIndexProvider;
import com.knoxhack.echowiki.item.GuideBookStacks;
import com.knoxhack.echowiki.registry.ModDataComponents;
import com.knoxhack.echowiki.registry.ModItems;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import javax.imageio.ImageIO;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoWiki.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_ARTICLE_PARSE =
            TEST_FUNCTIONS.register("json_article_parse", () -> ModGameTests::jsonArticleParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_COLLECTION_PARSE =
            TEST_FUNCTIONS.register("json_collection_parse", () -> ModGameTests::jsonCollectionParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REGISTRY_RELOAD_MERGE =
            TEST_FUNCTIONS.register("registry_reload_merge", () -> ModGameTests::registryReloadMerge);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_PARSE =
            TEST_FUNCTIONS.register("guide_book_parse", () -> ModGameTests::guideBookParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_REGISTRY_FILTERS =
            TEST_FUNCTIONS.register("guide_book_registry_filters", () -> ModGameTests::guideBookRegistryFilters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_STACK_COMPONENT =
            TEST_FUNCTIONS.register("guide_book_stack_component", () -> ModGameTests::guideBookStackComponent);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_API_STACKS =
            TEST_FUNCTIONS.register("guide_book_api_stacks", () -> ModGameTests::guideBookApiStacks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_LABELS =
            TEST_FUNCTIONS.register("guide_book_labels", () -> ModGameTests::guideBookLabels);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_STACK_FALLBACKS =
            TEST_FUNCTIONS.register("guide_book_stack_fallbacks", () -> ModGameTests::guideBookStackFallbacks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_SELECTION_FALLBACKS =
            TEST_FUNCTIONS.register("guide_book_selection_fallbacks", () -> ModGameTests::guideBookSelectionFallbacks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_RELOAD_WARNINGS =
            TEST_FUNCTIONS.register("guide_book_reload_warnings", () -> ModGameTests::guideBookReloadWarnings);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_INDEX_PROVIDER =
            TEST_FUNCTIONS.register("guide_book_index_provider", () -> ModGameTests::guideBookIndexProvider);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_CHAPTER_ROW_DATA =
            TEST_FUNCTIONS.register("guide_book_chapter_row_data", () -> ModGameTests::guideBookChapterRowData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_PROVIDER_CACHE =
            TEST_FUNCTIONS.register("guide_book_provider_cache", () -> ModGameTests::guideBookProviderCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUIDE_BOOK_READER_FAST_RESOURCES =
            TEST_FUNCTIONS.register("guide_book_reader_fast_resources", () -> ModGameTests::guideBookReaderFastResources);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WIKI_PAGED_PROVIDERS =
            TEST_FUNCTIONS.register("wiki_paged_providers", () -> ModGameTests::wikiPagedProviders);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARTICLE_BLOCK_PROVIDER_DATA =
            TEST_FUNCTIONS.register("article_block_provider_data", () -> ModGameTests::articleBlockProviderData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WIKI_SHELL_PROVIDER_TEXT =
            TEST_FUNCTIONS.register("wiki_shell_provider_text", () -> ModGameTests::wikiShellProviderText);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WIKI_BANNER_ASSET_BUDGET =
            TEST_FUNCTIONS.register("wiki_banner_asset_budget", () -> ModGameTests::wikiBannerAssetBudget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEEDED_GUIDE_BOOK_CHAPTERS =
            TEST_FUNCTIONS.register("seeded_guide_book_chapters", () -> ModGameTests::seededGuideBookChapters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_GUIDE_BOOK_CONTENT =
            TEST_FUNCTIONS.register("ashfall_guide_book_content", () -> ModGameTests::ashfallGuideBookContent);

    private static final List<String> SEEDED_GUIDES = List.of(
            "reclamation",
            "armory",
            "arcana_core",
            "arcane_index",
            "blackbox",
            "blockworks",
            "convoy",
            "cursecore",
            "grimoire",
            "industrial_nexus",
            "logistics",
            "multiblockcore",
            "nexus",
            "npcore",
            "orbital_remnants",
            "playercore",
            "powergrid",
            "recovery",
            "relictech",
            "ritualcore",
            "stationfall",
            "spellcore",
            "signalos",
            "weathercore",
            "worldcore",
            "holomap",
            "lens",
            "index",
            "wiki",
            "terminal",
            "tutorialcore");
    private static final List<String> SEEDED_CHAPTERS = List.of(
            "first_steps",
            "core_loop",
            "systems",
            "progression",
            "integrations",
            "troubleshooting");
    private static final List<String> REQUIRED_PLAYER_FACING_GUIDES = List.of(
            "arcana_core",
            "arcane_index",
            "cursecore",
            "grimoire",
            "npcore",
            "playercore",
            "ritualcore",
            "spellcore");
    private static final List<String> GENERATED_GUIDE_SUMMARY_PATTERNS = List.of(
            "guidance for",
            "actions that turn risk into a reliable habit",
            "neighboring echo systems",
            "optional addons optional");

    private record SeededGuideContent(
            GuideBookDefinition guide,
            WikiCollectionDefinition collection,
            WikiArticleDefinition overview,
            Map<Identifier, WikiArticleDefinition> chapters) {
    }

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(ModGameTests::registerTests);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("wiki"));
        register(event, environment, "json_article_parse", JSON_ARTICLE_PARSE.getId());
        register(event, environment, "json_collection_parse", JSON_COLLECTION_PARSE.getId());
        register(event, environment, "registry_reload_merge", REGISTRY_RELOAD_MERGE.getId());
        register(event, environment, "guide_book_parse", GUIDE_BOOK_PARSE.getId());
        register(event, environment, "guide_book_registry_filters", GUIDE_BOOK_REGISTRY_FILTERS.getId());
        register(event, environment, "guide_book_stack_component", GUIDE_BOOK_STACK_COMPONENT.getId());
        register(event, environment, "guide_book_api_stacks", GUIDE_BOOK_API_STACKS.getId());
        register(event, environment, "guide_book_labels", GUIDE_BOOK_LABELS.getId());
        register(event, environment, "guide_book_stack_fallbacks", GUIDE_BOOK_STACK_FALLBACKS.getId());
        register(event, environment, "guide_book_selection_fallbacks", GUIDE_BOOK_SELECTION_FALLBACKS.getId());
        register(event, environment, "guide_book_reload_warnings", GUIDE_BOOK_RELOAD_WARNINGS.getId());
        register(event, environment, "guide_book_index_provider", GUIDE_BOOK_INDEX_PROVIDER.getId());
        register(event, environment, "guide_book_chapter_row_data", GUIDE_BOOK_CHAPTER_ROW_DATA.getId());
        register(event, environment, "guide_book_provider_cache", GUIDE_BOOK_PROVIDER_CACHE.getId());
        register(event, environment, "guide_book_reader_fast_resources", GUIDE_BOOK_READER_FAST_RESOURCES.getId());
        register(event, environment, "wiki_paged_providers", WIKI_PAGED_PROVIDERS.getId());
        register(event, environment, "article_block_provider_data", ARTICLE_BLOCK_PROVIDER_DATA.getId());
        register(event, environment, "wiki_shell_provider_text", WIKI_SHELL_PROVIDER_TEXT.getId());
        register(event, environment, "wiki_banner_asset_budget", WIKI_BANNER_ASSET_BUDGET.getId());
        register(event, environment, "seeded_guide_book_chapters", SEEDED_GUIDE_BOOK_CHAPTERS.getId());
        register(event, environment, "ashfall_guide_book_content", ASHFALL_GUIDE_BOOK_CONTENT.getId());
    }

    private static void wikiBannerAssetBudget(GameTestHelper helper) {
        assertClasspathPngBudget(helper,
                "assets/echowiki/textures/gui/guide_books/index_banner.png",
                512, 160, 384 * 1024);
        assertClasspathPngBudget(helper,
                "assets/echowiki/textures/gui/guide_books/wiki_banner.png",
                512, 160, 384 * 1024);
        assertWorkspacePngBudgetIfPresent(helper,
                "src/main/resources/assets/echoashfallprotocol/textures/gui/wiki/ashfall_banner.png",
                512, 160, 384 * 1024);
        helper.succeed();
    }

    private static void jsonArticleParse(GameTestHelper helper) {
        WikiArticleDefinition article = WikiJsonReloadListener.parseArticleForTests(
                id("fallback/article"), JsonParser.parseString("""
                        {
                          "id": "example:field/manual",
                          "title": "Field Manual",
                          "category": "Survival",
                          "summary": "Spoiler-safe field notes.",
                          "sections": [
                            {"title": "Step One", "body": "Scan before moving.", "tone": "tip"},
                            {"title": "Step Two", "text": "Return before nightfall.", "tone": "warning"}
                          ],
                          "tags": ["starter", "route"],
                          "icon": "minecraft:compass",
                          "relatedArticles": ["echowiki:survival/first_hour"],
                          "relatedItems": ["minecraft:water_bucket"],
                          "unlockDiscovery": "example:signal/manual",
                          "spoilerLevel": 1,
                          "sortOrder": 42
                        }
                        """).getAsJsonObject());
        helper.assertTrue(article.id().equals(Identifier.fromNamespaceAndPath("example", "field/manual")),
                "Article parser should honor explicit ids.");
        helper.assertTrue(article.sections().size() == 2, "Article parser should preserve section rows.");
        helper.assertTrue(article.sections().get(0).type().equals("callout"),
                "Legacy warning/tip sections should become callout blocks.");
        helper.assertTrue(article.sections().get(1).body().equals("Return before nightfall."),
                "Article parser should accept text as a body alias.");
        helper.assertTrue(article.relatedItems().contains(Identifier.withDefaultNamespace("water_bucket")),
                "Article parser should preserve related item identifiers.");
        helper.assertTrue(article.unlockDiscovery().equals(Identifier.fromNamespaceAndPath("example", "signal/manual")),
                "Article parser should preserve discovery gating.");
        helper.assertTrue(article.sortOrder() == 42, "Article parser should preserve sort order.");

        WikiArticleDefinition legacyBody = WikiJsonReloadListener.parseArticleForTests(
                id("legacy/body"), JsonParser.parseString("""
                        {
                          "title": "Legacy Body",
                          "category": "Survival",
                          "summary": "Legacy single-body article.",
                          "body": "Single body fallback."
                        }
                        """).getAsJsonObject());
        helper.assertTrue(legacyBody.sections().size() == 1 && legacyBody.sections().get(0).type().equals("paragraph"),
                "Legacy body should become one paragraph block.");

        WikiArticleDefinition blocks = WikiJsonReloadListener.parseArticleForTests(
                id("rich/blocks"), JsonParser.parseString("""
                        {
                          "title": "Rich Blocks",
                          "category": "Systems",
                          "summary": "New block model.",
                          "blocks": [
                            {"type": "image", "title": "Plate", "body": "Route plate.", "image": "echowiki:textures/gui/guide_books/wiki_banner.png", "imageFit": "contain"},
                            {"type": "callout", "title": "Warning", "text": "Return before dark.", "tone": "warning", "icon": "minecraft:bell"},
                            {"type": "item", "title": "Water", "item": "minecraft:water_bucket", "count": 2, "targetKind": "item"},
                            {"type": "link", "targetKind": "region", "target": "example:crash_zone", "label": "Crash Zone", "subtitle": "Starter region"}
                          ]
                        }
                        """).getAsJsonObject());
        helper.assertTrue(blocks.sections().size() == 4, "Article parser should prefer rich blocks.");
        helper.assertTrue(blocks.sections().get(0).image().equals("echowiki:textures/gui/guide_books/wiki_banner.png"),
                "Image blocks should preserve texture ids.");
        helper.assertTrue(blocks.sections().get(1).body().equals("Return before dark.")
                        && blocks.sections().get(1).tone().equals("warning"),
                "Callout blocks should preserve text aliases and tone.");
        helper.assertTrue(blocks.sections().get(2).item().equals("minecraft:water_bucket")
                        && blocks.sections().get(2).count() == 2,
                "Item blocks should preserve item ids and counts.");
        helper.assertTrue(blocks.sections().get(3).targetKind().equals("region")
                        && blocks.sections().get(3).target().equals("example:crash_zone"),
                "Link blocks should preserve target fields.");
        helper.succeed();
    }

    private static void jsonCollectionParse(GameTestHelper helper) {
        WikiCollectionDefinition collection = WikiJsonReloadListener.parseCollectionForTests(
                id("fallback_collection"), JsonParser.parseString("""
                        {
                          "title": "Starter Route",
                          "summary": "Starter articles.",
                          "category": "Survival",
                          "articles": ["echowiki:survival/first_hour", "echowiki:systems/survival_codex"],
                          "sortOrder": 7
                        }
                        """).getAsJsonObject());
        helper.assertTrue(collection.id().equals(id("fallback_collection")),
                "Collection parser should infer ids from resource paths.");
        helper.assertTrue(collection.articles().size() == 2, "Collection parser should preserve article refs.");
        helper.assertTrue(collection.sortOrder() == 7, "Collection parser should preserve sort order.");
        helper.succeed();
    }

    private static void registryReloadMerge(GameTestHelper helper) {
        try {
            Identifier dataArticleId = id("data/article");
            Identifier dataCollectionId = id("data_collection");
            WikiArticleDefinition dataArticle = WikiJsonReloadListener.parseArticleForTests(
                    dataArticleId, JsonParser.parseString("""
                            {
                              "title": "Reloaded Article",
                              "category": "Systems",
                              "summary": "Published by a datapack reload.",
                              "body": "Single body fallback.",
                              "sortOrder": 5
                            }
                            """).getAsJsonObject());
            WikiCollectionDefinition dataCollection = new WikiCollectionDefinition(
                    dataCollectionId,
                    "Reloaded Collection",
                    "Synthetic reload collection.",
                    "Systems",
                    List.of(dataArticleId),
                    3);
            WikiContentRegistry.replaceData(
                    Map.of(dataArticleId, dataArticle),
                    Map.of(dataCollectionId, dataCollection),
                    List.of("Synthetic warning"));
            helper.assertTrue(WikiContentRegistry.article(dataArticleId).isPresent(),
                    "Data article should be visible through merged registry lookup.");
            helper.assertTrue(WikiContentRegistry.article(id("survival/first_hour")).isPresent(),
                    "Built-in starter article should remain visible after data replacement.");
            helper.assertTrue(WikiContentRegistry.dataArticleCount() == 1,
                    "Registry should track data article counts separately.");
            helper.assertTrue(WikiContentRegistry.warnings().contains("Synthetic warning"),
                    "Registry should retain reload warnings for diagnostics.");
            helper.assertTrue(WikiContentRegistry.collections().stream().anyMatch(collection -> collection.id().equals(dataCollectionId)),
                    "Data collection should be visible through merged registry lookup.");
        } finally {
            WikiContentRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    private static void guideBookParse(GameTestHelper helper) {
        GuideBookDefinition guide = WikiJsonReloadListener.parseGuideBookForTests(
                id("fallback_guide"), JsonParser.parseString("""
                        {
                          "id": "example:manuals/power",
                          "moduleId": "echopowergrid",
                          "requiredModId": "echopowergrid",
                          "title": "Power Manual",
                          "subtitle": "Grid basics",
                          "summary": "Build stable power loops.",
                          "icon": "minecraft:redstone",
                          "accent": "#FFFF4D6D",
                          "collectionId": "example:guides/power",
                          "homeArticleId": "example:guides/power",
                          "chapterArticleIds": ["example:guides/power", "example:guides/power/troubleshooting"],
                          "tags": ["power", "guide"],
                          "sortOrder": 12
                        }
                        """).getAsJsonObject());
        helper.assertTrue(guide.id().equals(Identifier.fromNamespaceAndPath("example", "manuals/power")),
                "Guide parser should honor explicit ids.");
        helper.assertTrue(guide.chapterArticleIds().size() == 2, "Guide parser should preserve chapter ids.");
        helper.assertTrue(guide.collectionId().equals(Identifier.fromNamespaceAndPath("example", "guides/power")),
                "Guide parser should preserve collection ids.");
        helper.assertTrue(guide.sortOrder() == 12, "Guide parser should preserve sort order.");
        helper.succeed();
    }

    private static void guideBookRegistryFilters(GameTestHelper helper) {
        try {
            GuideBookDefinition visible = syntheticGuide(id("guides/visible"), EchoWiki.MODID);
            GuideBookDefinition hidden = syntheticGuide(id("guides/hidden"), "definitely_missing_echo_module");
            GuideBookRegistry.replaceData(Map.of(visible.id(), visible, hidden.id(), hidden));
            helper.assertTrue(GuideBookRegistry.guideBooks().size() == 2,
                    "Guide registry should retain loaded definitions.");
            helper.assertTrue(GuideBookRegistry.visibleGuideBooks().stream().anyMatch(guide -> guide.id().equals(visible.id())),
                    "Guide registry should expose guides for loaded modules.");
            helper.assertTrue(GuideBookRegistry.visibleGuideBooks().stream().noneMatch(guide -> guide.id().equals(hidden.id())),
                    "Guide registry should hide guides whose required mod is not loaded.");
        } finally {
            GuideBookRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    private static void guideBookStackComponent(GameTestHelper helper) {
        GuideBookDefinition guide = syntheticGuide(id("guides/stack"), EchoWiki.MODID);
        net.minecraft.world.item.ItemStack stack = GuideBookStacks.stackFor(guide);
        GuideBookTarget target = stack.get(ModDataComponents.GUIDE_BOOK_TARGET.get());
        helper.assertTrue(target != null && target.guideBookId().equals(guide.id()),
                "Guide-book stack factory should attach the synced target component.");
        helper.assertTrue(stack.getHoverName().getString().equals(guide.title()),
                "Guide-book stack factory should assign the manual display name.");
        helper.succeed();
    }

    private static void guideBookApiStacks(GameTestHelper helper) {
        try {
            GuideBookDefinition visible = syntheticGuide(id("guides/api_visible"), EchoWiki.MODID);
            GuideBookDefinition hidden = syntheticGuide(id("guides/api_hidden"), "definitely_missing_echo_module");
            GuideBookRegistry.replaceData(Map.of(visible.id(), visible, hidden.id(), hidden));

            ItemStack targeted = EchoWikiApi.guideBookStack(visible.id());
            GuideBookTarget target = targeted.get(ModDataComponents.GUIDE_BOOK_TARGET.get());
            helper.assertTrue(!targeted.isEmpty() && targeted.is(ModItems.GUIDE_BOOK.get()),
                    "Wiki API should create visible targeted guide-book stacks.");
            helper.assertTrue(target != null && target.guideBookId().equals(visible.id()),
                    "Wiki API targeted stack should carry the requested guide id.");
            helper.assertTrue(targeted.get(DataComponents.ITEM_NAME) != null
                            && targeted.getHoverName().getString().equals(visible.title()),
                    "Wiki API targeted stack should preserve the manual name.");
            helper.assertTrue(EchoWikiApi.isGuideBookVisible(visible.id()),
                    "Wiki API should report visible guide books.");
            helper.assertTrue(EchoWikiApi.guideBookStack(hidden.id()).isEmpty(),
                    "Wiki API should not create targeted stacks for hidden guide books.");
            helper.assertTrue(!EchoWikiApi.isGuideBookVisible(hidden.id()),
                    "Wiki API should report hidden guide books as unavailable.");

            ItemStack library = EchoWikiApi.guideBookLibraryStack();
            helper.assertTrue(!library.isEmpty() && library.is(ModItems.GUIDE_BOOK.get()),
                    "Wiki API should create untagged library guide-book stacks.");
            helper.assertTrue(library.get(ModDataComponents.GUIDE_BOOK_TARGET.get()) == null,
                    "Wiki API library stack should not carry a guide target.");
        } finally {
            GuideBookRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    private static void guideBookLabels(GameTestHelper helper) {
        helper.assertTrue(GuideBookLabels.shortId(id("powergrid")).equals("powergrid"),
                "Guide labels should use short ids for echowiki manuals.");
        helper.assertTrue(GuideBookLabels.moduleLabel("echopowergrid").equals("PowerGrid"),
                "Guide labels should expose player-facing module labels.");
        helper.assertTrue(GuideBookLabels.chapterCountLabel(1).equals("1 chapter"),
                "Guide labels should render singular chapter counts.");
        helper.assertTrue(GuideBookLabels.chapterCountLabel(2).equals("2 chapters"),
                "Guide labels should render plural chapter counts.");
        helper.assertTrue(GuideBookLabels.sectionCountLabel(1).equals("1 section"),
                "Guide labels should render singular section counts.");
        helper.assertTrue(GuideBookLabels.sectionCountLabel(2).equals("2 sections"),
                "Guide labels should render plural section counts.");
        helper.assertTrue(GuideBookLabels.chapterRoleLabel(id("guides/powergrid"), id("guides/powergrid"), 0).equals("Overview"),
                "Guide labels should identify overview chapters.");
        helper.assertTrue(GuideBookLabels.chapterRoleLabel(id("guides/powergrid/first_steps"), id("guides/powergrid"), 1).equals("First Steps"),
                "Guide labels should identify named chapter roles.");
        helper.succeed();
    }

    private static void guideBookStackFallbacks(GameTestHelper helper) {
        try {
            GuideBookDefinition guide = syntheticGuide(id("guides/visible"), EchoWiki.MODID);
            GuideBookRegistry.replaceData(Map.of(guide.id(), guide));
            net.minecraft.world.item.ItemStack untagged = new net.minecraft.world.item.ItemStack(ModItems.GUIDE_BOOK.get());
            helper.assertTrue(GuideBookStacks.definition(untagged).isEmpty(),
                    "Untagged guide-book stacks should not resolve to the first visible guide.");

            net.minecraft.world.item.ItemStack missingTarget = GuideBookStacks.stackFor(guide);
            Identifier missingId = id("guides/missing");
            missingTarget.set(ModDataComponents.GUIDE_BOOK_TARGET.get(), new GuideBookTarget(missingId));
            helper.assertTrue(GuideBookStacks.definition(missingTarget).isEmpty(),
                    "Missing guide target stacks should not resolve to the first visible guide.");
            helper.assertTrue(GuideBookStacks.visibleDefinition(missingTarget).isEmpty(),
                    "Missing guide target stacks should fall back to the guide-book library behavior.");
        } finally {
            GuideBookRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    private static void guideBookSelectionFallbacks(GameTestHelper helper) {
        try {
            GuideBookDefinition visible = syntheticGuide(id("guides/visible_selection"), EchoWiki.MODID);
            GuideBookDefinition hidden = syntheticGuide(id("guides/hidden_selection"), "definitely_missing_echo_module");
            GuideBookRegistry.replaceData(Map.of(visible.id(), visible, hidden.id(), hidden));
            WikiUiState.INSTANCE.selectedGuideBook(hidden.id());
            helper.assertTrue(WikiUiState.INSTANCE.selectedGuideBook().equals(visible.id()),
                    "Guide selection should fall back to the first visible manual.");
            helper.assertTrue(!WikiUiState.INSTANCE.selectVisibleGuideBook(hidden.id()),
                    "Unavailable guide ids should not be selected for guide-book detail pages.");
            helper.assertTrue(WikiUiState.INSTANCE.selectVisibleGuideBook(visible.id()),
                    "Visible guide ids should remain selectable.");
        } finally {
            WikiUiState.INSTANCE.selectedGuideBook(null);
            GuideBookRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    private static void guideBookReloadWarnings(GameTestHelper helper) {
        GuideBookDefinition guide = new GuideBookDefinition(
                id("guides/missing_refs"),
                EchoWiki.MODID,
                EchoWiki.MODID,
                "Missing Ref Guide",
                "Synthetic subtitle",
                "Synthetic summary.",
                id("missing_icon_item"),
                "#FF66E8FF",
                id("missing_collection"),
                id("missing_home"),
                List.of(id("missing_home"), id("missing_chapter")),
                List.of("synthetic"),
                1);
        List<String> warnings = WikiJsonReloadListener.validateGuideReferencesForTests(
                Map.of(guide.id(), guide),
                Map.of(),
                Map.of());
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing collection")),
                "Guide reload validation should warn for missing collections.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing home article")),
                "Guide reload validation should warn for missing home articles.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing chapter article")),
                "Guide reload validation should warn for missing chapter articles.");
        helper.assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing icon item")),
                "Guide reload validation should warn for invalid guide icons.");
        helper.assertTrue(warnings.stream().filter(warning -> warning.contains("missing_home")).count() == 1,
                "Guide reload validation should not duplicate the same missing home article as a chapter warning.");
        helper.succeed();
    }

    private static void guideBookIndexProvider(GameTestHelper helper) {
        try {
            GuideBookDefinition guide = syntheticGuide(id("guides/indexed"), EchoWiki.MODID);
            GuideBookRegistry.replaceData(Map.of(guide.id(), guide));
            IndexContentSnapshot snapshot = GuideBookIndexProvider.INSTANCE.snapshot(null);
            helper.assertTrue(snapshot.entries().stream().anyMatch(entry -> entry.titleKey().equals(guide.title())),
                    "Guide-book Index provider should publish a visible entry.");
            helper.assertTrue(!snapshot.categories().isEmpty(), "Guide-book Index provider should publish its category.");
        } finally {
            GuideBookRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void guideBookChapterRowData(GameTestHelper helper) {
        try {
            Identifier guideId = id("guides/row_polish");
            Identifier homeId = id("guides/row_polish");
            Identifier firstId = id("guides/row_polish/first_steps");
            Identifier missingId = id("guides/row_polish/troubleshooting");
            Identifier externalId = id("survival/first_hour");
            GuideBookDefinition guide = new GuideBookDefinition(
                    guideId,
                    EchoWiki.MODID,
                    EchoWiki.MODID,
                    "Row Polish Manual",
                    "Synthetic subtitle",
                    "Synthetic summary.",
                    Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                    "#FF66E8FF",
                    homeId,
                    homeId,
                    List.of(homeId, firstId, missingId),
                    List.of("synthetic"),
                    1);
            WikiArticleDefinition home = article(
                    homeId,
                    "Row Polish Manual",
                    "Manual overview.",
                    List.of(new WikiArticleSection("Overview", "Start here.", "body")),
                    List.of(firstId, externalId),
                    List.of(Identifier.fromNamespaceAndPath("minecraft", "water_bucket")));
            WikiArticleDefinition first = article(
                    firstId,
                    "Row Polish: First Steps",
                    "First-step chapter.",
                    List.of(
                            new WikiArticleSection("One", "Do one thing.", "tip"),
                            new WikiArticleSection("Two", "Then do another.", "body")),
                    List.of(homeId, externalId),
                    List.of());
            GuideBookRegistry.replaceData(Map.of(guide.id(), guide));
            WikiContentRegistry.replaceData(
                    Map.of(home.id(), home, first.id(), first),
                    Map.of(homeId, new WikiCollectionDefinition(homeId, "Row Polish Manual", "Synthetic collection.", "Guide Books",
                            List.of(homeId, firstId, missingId), 1)),
                    List.of());
            WikiUiState.INSTANCE.selectedGuideBook(guide.id());

            List<Map<String, Object>> chapters = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    EchoDataContext.empty(), List.of("guideBook", "chapters"));
            helper.assertTrue(chapters.size() == 3, "Chapter row data should include loaded and missing manual chapters.");
            helper.assertTrue("Overview".equals(chapters.get(0).get("roleLabel")),
                    "First guide row should be labeled as the overview.");
            helper.assertTrue("1 section".equals(chapters.get(0).get("countLabel")),
                    "Overview row should use localized singular section count.");
            helper.assertTrue("First Steps".equals(chapters.get(1).get("roleLabel")),
                    "Named chapter rows should expose a clean chapter role label.");
            helper.assertTrue("2 sections".equals(chapters.get(1).get("countLabel")),
                    "Chapter rows should use localized plural section counts.");
            helper.assertTrue("missing".equals(chapters.get(2).get("status")),
                    "Missing chapter rows should expose missing status.");
            helper.assertTrue("Troubleshooting".equals(chapters.get(2).get("roleLabel")),
                    "Missing chapter rows should still expose the intended chapter role.");
            helper.assertTrue("Missing article".equals(chapters.get(2).get("countLabel")),
                    "Missing chapter rows should show a clean missing article label.");

            List<Map<String, Object>> related = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    EchoDataContext.empty(), List.of("guideBook", "related"));
            helper.assertTrue(related.stream().anyMatch(row -> homeId.toString().equals(row.get("id"))),
                    "Manual links should include the home overview.");
            helper.assertTrue(related.stream().anyMatch(row -> externalId.toString().equals(row.get("id"))),
                    "Manual links should include true external related articles.");
            helper.assertTrue(related.stream().noneMatch(row -> firstId.toString().equals(row.get("id"))),
                    "Manual links should not duplicate chapter rows.");
        } finally {
            WikiUiState.INSTANCE.selectedGuideBook(null);
            GuideBookRegistry.clearDataForTests();
            WikiContentRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void guideBookProviderCache(GameTestHelper helper) {
        try {
            long contentBefore = WikiContentRegistry.revision();
            long guideBefore = GuideBookRegistry.revision();
            Identifier alphaGuideId = id("guides/cache_alpha");
            Identifier alphaHomeId = id("guides/cache_alpha");
            Identifier alphaChapterId = id("guides/cache_alpha/first_steps");
            Identifier alphaMissingId = id("guides/cache_alpha/troubleshooting");
            Identifier betaGuideId = id("guides/cache_beta");
            Identifier betaHomeId = id("guides/cache_beta");
            GuideBookDefinition alpha = new GuideBookDefinition(
                    alphaGuideId,
                    "alpha_mod",
                    EchoWiki.MODID,
                    "Alpha Cache Manual",
                    "Alpha subtitle",
                    "Alpha manual summary.",
                    Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                    "#FF66E8FF",
                    alphaHomeId,
                    alphaHomeId,
                    List.of(alphaHomeId, alphaChapterId, alphaMissingId),
                    List.of("alpha", "cache"),
                    1);
            GuideBookDefinition beta = new GuideBookDefinition(
                    betaGuideId,
                    "beta_mod",
                    EchoWiki.MODID,
                    "Beta Cache Manual",
                    "Beta subtitle",
                    "Beta manual summary.",
                    Identifier.fromNamespaceAndPath("minecraft", "book"),
                    "#FF66E8FF",
                    betaHomeId,
                    betaHomeId,
                    List.of(betaHomeId),
                    List.of("beta", "cache"),
                    2);
            WikiArticleDefinition alphaHome = article(
                    alphaHomeId,
                    "Alpha Cache Manual",
                    "Alpha overview.",
                    List.of(new WikiArticleSection("Overview", "Start alpha here.", "body")),
                    List.of(),
                    List.of(Identifier.fromNamespaceAndPath("minecraft", "water_bucket")));
            WikiArticleDefinition alphaChapter = article(
                    alphaChapterId,
                    "Alpha Cache: First Steps",
                    "Alpha first-step chapter.",
                    List.of(new WikiArticleSection("One", "Do alpha setup.", "tip")),
                    List.of(alphaHomeId),
                    List.of());
            WikiArticleDefinition betaHome = article(
                    betaHomeId,
                    "Beta Cache Manual",
                    "Beta overview.",
                    List.of(new WikiArticleSection("Overview", "Start beta here.", "body")),
                    List.of(),
                    List.of());

            WikiContentRegistry.replaceData(
                    Map.of(alphaHome.id(), alphaHome, alphaChapter.id(), alphaChapter, betaHome.id(), betaHome),
                    Map.of(),
                    List.of());
            GuideBookRegistry.replaceData(Map.of(alpha.id(), alpha, beta.id(), beta));
            helper.assertTrue(WikiContentRegistry.revision() > contentBefore,
                    "Wiki content registry revision should advance after replaceData.");
            helper.assertTrue(GuideBookRegistry.revision() > guideBefore,
                    "Guide book registry revision should advance after replaceData.");
            helper.assertTrue(WikiContentRegistry.articles() == WikiContentRegistry.articles(),
                    "Wiki content registry should reuse its sorted article snapshot.");
            helper.assertTrue(GuideBookRegistry.guideBooks() == GuideBookRegistry.guideBooks(),
                    "Guide book registry should reuse its sorted guide snapshot.");

            WikiUiState.INSTANCE.clearFilters();
            WikiUiState.INSTANCE.selectedGuideBook(alpha.id());
            WikiDataProviders.clearCachesForTests();
            EchoDataContext context = EchoDataContext.empty();

            List<Map<String, Object>> visibleFirst = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBooks", "visible"));
            List<Map<String, Object>> visibleAgain = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBooks", "visible"));
            helper.assertTrue(visibleFirst == visibleAgain,
                    "Guide book provider should reuse visible manual rows until state or data changes.");

            Map<String, Object> selectedFirst = (Map<String, Object>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "selected"));
            Map<String, Object> selectedAgain = (Map<String, Object>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "selected"));
            helper.assertTrue(selectedFirst == selectedAgain,
                    "Guide book provider should reuse selected manual row data.");

            List<Map<String, Object>> chaptersFirst = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "chapters"));
            List<Map<String, Object>> chaptersAgain = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "chapters"));
            helper.assertTrue(chaptersFirst == chaptersAgain,
                    "Guide book provider should reuse selected manual chapter rows.");
            helper.assertTrue(chaptersFirst.size() == 3 && "missing".equals(chaptersFirst.get(2).get("status")),
                    "Cached chapter rows should preserve missing-chapter fallback behavior.");

            List<Map<String, Object>> relatedFirst = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "related"));
            List<Map<String, Object>> relatedAgain = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "related"));
            helper.assertTrue(relatedFirst == relatedAgain,
                    "Guide book provider should reuse selected manual related rows.");

            WikiUiState.INSTANCE.searchQuery("Beta");
            List<Map<String, Object>> betaRows = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBooks", "visible"));
            helper.assertTrue(betaRows != visibleFirst && betaRows.size() == 1
                            && beta.id().toString().equals(betaRows.get(0).get("id")),
                    "Changing the search query should invalidate cached visible manual rows.");

            WikiUiState.INSTANCE.searchQuery("");
            WikiUiState.INSTANCE.category("alpha_mod");
            List<Map<String, Object>> alphaRows = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBooks", "visible"));
            helper.assertTrue(alphaRows != betaRows && alphaRows.size() == 1
                            && alpha.id().toString().equals(alphaRows.get(0).get("id")),
                    "Changing the category filter should invalidate cached visible manual rows.");

            long guideRevisionBeforeRefresh = GuideBookRegistry.revision();
            GuideBookRegistry.replaceData(Map.of(alpha.id(), alpha));
            WikiUiState.INSTANCE.clearFilters();
            List<Map<String, Object>> refreshedRows = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBooks", "visible"));
            helper.assertTrue(GuideBookRegistry.revision() > guideRevisionBeforeRefresh,
                    "Guide book registry revision should advance after refresh.");
            helper.assertTrue(refreshedRows != visibleFirst && refreshedRows.size() == 1,
                    "Guide book provider should invalidate rows when the guide registry revision changes.");

            long contentBeforeClear = WikiContentRegistry.revision();
            WikiContentRegistry.clearDataForTests();
            helper.assertTrue(WikiContentRegistry.revision() > contentBeforeClear,
                    "Wiki content registry revision should advance after clearDataForTests.");
        } finally {
            WikiUiState.INSTANCE.selectedGuideBook(null);
            WikiUiState.INSTANCE.clearFilters();
            WikiDataProviders.clearCachesForTests();
            GuideBookRegistry.clearDataForTests();
            WikiContentRegistry.clearDataForTests();
            WikiDataProviders.clearCachesForTests();
        }
        helper.succeed();
    }

    private static void guideBookReaderFastResources(GameTestHelper helper) {
        helper.assertTrue(resourceExists("assets/echowiki/eui/pages/wiki_guide_book_reader.eui.xml"),
                "Lightweight guide-book reader page should be packaged.");
        helper.assertTrue(resourceExists("assets/echowiki/eui/styles/wiki_fast.eui.css"),
                "Fast wiki stylesheet should be packaged.");
        helper.assertTrue(WikiScreenCorePages.GUIDE_BOOK_READER.equals(WikiScreenCorePages.fromMode("reader")),
                "Reader mode should resolve to the lightweight guide-book reader page.");

        String reader = resourceText("assets/echowiki/eui/pages/wiki_guide_book_reader.eui.xml");
        helper.assertTrue(reader.contains("bind=\"wiki.guideBook.chapters\"")
                        && reader.contains("bind=\"wiki.guideBook.related\"")
                        && !reader.contains("wiki_shell"),
                "Reader page should expose chapters and related rows without the heavy wiki shell.");

        String articleCard = resourceText("assets/echowiki/eui/components/wiki_article_card.eui.xml");
        String guideCard = resourceText("assets/echowiki/eui/components/wiki_guide_book_card.eui.xml");
        String shelfRow = resourceText("assets/echowiki/eui/components/wiki_guide_book_shelf_row.eui.xml");
        helper.assertTrue(!articleCard.contains("<image") && !guideCard.contains("<image") && !shelfRow.contains("<image"),
                "Fast article and guide list rows should omit repeated thumbnail images.");
        helper.assertTrue(guideCard.contains("action=\"wiki.open_guide_book\"")
                        && articleCard.contains("action=\"wiki.open_article\""),
                "Fast rows should preserve action targets.");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void wikiPagedProviders(GameTestHelper helper) {
        try {
            Map<Identifier, WikiArticleDefinition> articles = new LinkedHashMap<>();
            Map<Identifier, GuideBookDefinition> guides = new LinkedHashMap<>();
            for (int index = 0; index < WikiUiState.ARTICLE_PAGE_SIZE + 7; index++) {
                Identifier articleId = id(String.format(java.util.Locale.ROOT, "paging/article_%02d", index));
                articles.put(articleId, article(
                        articleId,
                        String.format(java.util.Locale.ROOT, "Paged Article %02d", index),
                        "Paged article summary " + index,
                        List.of(new WikiArticleSection("Body", "Paged body " + index, "body")),
                        List.of(),
                        List.of()));
            }
            for (int index = 0; index < WikiUiState.GUIDE_BOOK_PAGE_SIZE + 3; index++) {
                Identifier guideId = id(String.format(java.util.Locale.ROOT, "paging/guide_%02d", index));
                Identifier homeArticle = id(String.format(java.util.Locale.ROOT, "paging/article_%02d", index));
                guides.put(guideId, new GuideBookDefinition(
                        guideId,
                        "echowiki",
                        "echowiki",
                        String.format(java.util.Locale.ROOT, "Paged Guide %02d", index),
                        "Paged guide subtitle",
                        "Paged guide summary.",
                        Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                        "#FF66E8FF",
                        null,
                        homeArticle,
                        List.of(homeArticle),
                        List.of("paged"),
                        index));
            }

            WikiContentRegistry.replaceData(articles, Map.of(), List.of());
            GuideBookRegistry.replaceData(guides);
            WikiUiState.INSTANCE.clearFilters();
            WikiDataProviders.clearCachesForTests();
            EchoDataContext context = EchoDataContext.empty();

            WikiUiState.INSTANCE.articlePage(999);
            List<Map<String, Object>> articleRows = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("articles", "visible"));
            String articleLabel = String.valueOf(WikiDataProviders.PROVIDER.resolve(context, List.of("articles", "pageLabel")));
            helper.assertTrue(articleRows.size() == expectedLastPageSize(WikiContentRegistry.articles().size(),
                            WikiUiState.ARTICLE_PAGE_SIZE) && articleLabel.startsWith("Page 2 / 2"),
                    "Article provider should clamp oversized page indexes to the last available page.");

            WikiUiState.INSTANCE.guideBookPage(999);
            List<Map<String, Object>> guideRows = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBooks", "visible"));
            String guideLabel = String.valueOf(WikiDataProviders.PROVIDER.resolve(context, List.of("guideBooks", "pageLabel")));
            helper.assertTrue(guideRows.size() == expectedLastPageSize(GuideBookRegistry.visibleGuideBooks().size(),
                            WikiUiState.GUIDE_BOOK_PAGE_SIZE) && guideLabel.startsWith("Page 2 / 2"),
                    "Guide provider should clamp oversized page indexes to the last available page.");

            WikiUiState.INSTANCE.articlePage(1);
            WikiUiState.INSTANCE.guideBookPage(1);
            WikiUiState.INSTANCE.searchQuery("Paged Article 00");
            helper.assertTrue(WikiUiState.INSTANCE.articlePage() == 0 && WikiUiState.INSTANCE.guideBookPage() == 0,
                    "Search changes should reset article and guide page indexes.");

            WikiUiState.INSTANCE.articlePage(1);
            WikiUiState.INSTANCE.guideBookPage(1);
            WikiUiState.INSTANCE.category("Guide Books");
            helper.assertTrue(WikiUiState.INSTANCE.articlePage() == 0 && WikiUiState.INSTANCE.guideBookPage() == 0,
                    "Category changes should reset article and guide page indexes.");

            Object previousDisabled = WikiDataProviders.PROVIDER.resolve(context, List.of("articles", "previousPageDisabled"));
            helper.assertTrue(Boolean.TRUE.equals(previousDisabled),
                    "Paged article provider should expose disabled state for the previous-page action.");
        } finally {
            WikiUiState.INSTANCE.selectedGuideBook(null);
            WikiUiState.INSTANCE.clearFilters();
            WikiDataProviders.clearCachesForTests();
            GuideBookRegistry.clearDataForTests();
            WikiContentRegistry.clearDataForTests();
            WikiDataProviders.clearCachesForTests();
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void articleBlockProviderData(GameTestHelper helper) {
        try {
            Identifier articleId = id("provider/rich_blocks");
            Identifier lockedId = id("provider/locked_blocks");
            WikiArticleDefinition rich = article(
                    articleId,
                    "Provider Rich Blocks",
                    "Provider summary.",
                    List.of(
                            new WikiArticleSection("Intro", "Readable long-form paragraph.", "body"),
                            new WikiArticleSection(
                                    "image",
                                    "Route Plate",
                                    "Image caption.",
                                    "body",
                                    "echowiki:textures/gui/guide_books/wiki_banner.png",
                                    "cover",
                                    "",
                                    "",
                                    1,
                                    "",
                                    "",
                                    "",
                                    ""),
                            new WikiArticleSection(
                                    "item",
                                    "Water Bucket",
                                    "Carry water.",
                                    "tip",
                                    "",
                                    "cover",
                                    "",
                                    "minecraft:water_bucket",
                                    2,
                                    "item",
                                    "",
                                    "Water Bucket",
                                    "Emergency stack."),
                            new WikiArticleSection(
                                    "link",
                                    "Crash Zone",
                                    "Open the region dossier.",
                                    "body",
                                    "",
                                    "cover",
                                    "",
                                    "",
                                    1,
                                    "region",
                                    "echowiki:crash_zone",
                                    "Crash Zone",
                                    "Region reference.")),
                    List.of(),
                    List.of());
            WikiArticleDefinition locked = new WikiArticleDefinition(
                    lockedId,
                    "Locked Blocks",
                    "Systems",
                    "Locked summary.",
                    List.of(new WikiArticleSection("Hidden", "Hidden body.", "body")),
                    List.of("locked"),
                    Identifier.fromNamespaceAndPath("minecraft", "book"),
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    id("signals/locked_blocks"),
                    1,
                    2);
            WikiContentRegistry.replaceData(Map.of(rich.id(), rich, locked.id(), locked), Map.of(), List.of());

            WikiUiState.INSTANCE.selectedArticle(articleId);
            List<Map<String, Object>> blocks = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    EchoDataContext.empty(), List.of("article", "blocks"));
            helper.assertTrue(blocks.size() == 4, "Provider should expose article blocks.");
            helper.assertTrue("image".equals(blocks.get(1).get("type")) && Boolean.TRUE.equals(blocks.get(1).get("hasImage")),
                    "Provider should expose image block visibility fields.");
            helper.assertTrue("item".equals(blocks.get(2).get("type"))
                            && "minecraft:water_bucket".equals(blocks.get(2).get("item"))
                            && Integer.valueOf(2).equals(blocks.get(2).get("count")),
                    "Provider should expose item block ids and counts.");
            helper.assertTrue("region".equals(blocks.get(3).get("targetKind"))
                            && "echowiki:crash_zone".equals(blocks.get(3).get("target")),
                    "Provider should expose link targets.");

            List<Map<String, Object>> toc = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    EchoDataContext.empty(), List.of("article", "toc"));
            helper.assertTrue(toc.size() == 4 && "01".equals(toc.get(0).get("ordinal")),
                    "Provider should expose a table-of-contents row for each block.");

            List<Map<String, Object>> related = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    EchoDataContext.empty(), List.of("article", "related"));
            helper.assertTrue(related.stream().anyMatch(row -> "item".equals(row.get("kind"))
                            && "minecraft:water_bucket".equals(row.get("id"))),
                    "Provider should publish item block targets as related links.");
            helper.assertTrue(related.stream().anyMatch(row -> "region".equals(row.get("kind"))
                            && "echowiki:crash_zone".equals(row.get("id"))),
                    "Provider should publish link block targets as related links.");

            WikiUiState.INSTANCE.selectedArticle(lockedId);
            List<Map<String, Object>> lockedBlocks = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    EchoDataContext.empty(), List.of("article", "blocks"));
            helper.assertTrue(lockedBlocks.size() == 1
                            && "locked".equals(lockedBlocks.get(0).get("tone"))
                            && "Locked Intel".equals(lockedBlocks.get(0).get("title")),
                    "Locked articles should expose a safe fallback block.");
        } finally {
            WikiUiState.INSTANCE.selectedArticle(null);
            WikiContentRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void wikiShellProviderText(GameTestHelper helper) {
        try {
            WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.GUIDE_BOOKS);
            EchoDataContext context = WikiScreenCoreBridge.context();
            helper.assertTrue("-".equals(context.missingPlaceholder()),
                    "Wiki ScreenCore context should keep missing bindings visible.");

            Map<String, Object> dashboard = (Map<String, Object>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("dashboard"));
            assertNonBlank(helper, dashboard, "title", "Wiki dashboard title should be nonblank.");
            assertNonBlank(helper, dashboard, "subtitle", "Wiki dashboard subtitle should be nonblank.");
            assertNonBlank(helper, dashboard, "mode", "Wiki dashboard mode should be nonblank.");
            assertNonBlank(helper, dashboard, "query", "Wiki dashboard query label should be nonblank.");

            List<Map<String, Object>> nav = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("nav", "sections"));
            helper.assertTrue(nav.size() >= 8, "Wiki navigation should expose the main page rows.");
            for (Map<String, Object> row : nav) {
                assertNonBlank(helper, row, "title", "Wiki navigation row title should be nonblank.");
                assertNonBlank(helper, row, "subtitle", "Wiki navigation row subtitle should be nonblank.");
                assertNonBlank(helper, row, "page", "Wiki navigation row page id should be nonblank.");
            }

            Identifier guideId = id("guides/shell_text");
            Identifier homeId = id("guides/shell_text");
            Identifier firstId = id("guides/shell_text/first_steps");
            GuideBookDefinition guide = new GuideBookDefinition(
                    guideId,
                    EchoWiki.MODID,
                    EchoWiki.MODID,
                    "Shell Text Manual",
                    "Readable manual subtitle",
                    "Readable manual summary.",
                    Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                    "#FF66E8FF",
                    homeId,
                    homeId,
                    List.of(homeId, firstId),
                    List.of("synthetic"),
                    1);
            WikiArticleDefinition home = article(
                    homeId,
                    "Shell Text Manual",
                    "Overview summary for shell text.",
                    List.of(new WikiArticleSection("Overview", "Start here.", "body")),
                    List.of(firstId, id("survival/first_hour")),
                    List.of(Identifier.fromNamespaceAndPath("minecraft", "water_bucket")));
            WikiArticleDefinition first = article(
                    firstId,
                    "Shell Text: First Steps",
                    "First-step summary for shell text.",
                    List.of(new WikiArticleSection("One", "Do one thing.", "tip")),
                    List.of(homeId),
                    List.of());
            GuideBookRegistry.replaceData(Map.of(guide.id(), guide));
            WikiContentRegistry.replaceData(
                    Map.of(home.id(), home, first.id(), first),
                    Map.of(homeId, new WikiCollectionDefinition(homeId, "Shell Text Manual", "Synthetic collection.",
                            "Guide Books", List.of(homeId, firstId), 1)),
                    List.of());
            WikiUiState.INSTANCE.selectedGuideBook(guide.id());

            Map<String, Object> selected = (Map<String, Object>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "selected"));
            assertNonBlank(helper, selected, "title", "Selected manual title should be nonblank.");
            assertNonBlank(helper, selected, "subtitlePreview", "Selected manual subtitle should be nonblank.");
            assertNonBlank(helper, selected, "summaryPreview", "Selected manual summary should be nonblank.");
            assertNonBlank(helper, selected, "moduleBadge", "Selected manual module badge should be nonblank.");
            assertNonBlank(helper, selected, "chapterBadge", "Selected manual chapter badge should be nonblank.");

            List<Map<String, Object>> chapters = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "chapters"));
            helper.assertTrue(chapters.size() == 2, "Selected manual should expose chapter rows.");
            chapters.forEach(row -> {
                assertNonBlank(helper, row, "title", "Chapter row title should be nonblank.");
                assertNonBlank(helper, row, "roleBadge", "Chapter row role badge should be nonblank.");
                assertNonBlank(helper, row, "countLabel", "Chapter row count label should be nonblank.");
                assertNonBlank(helper, row, "summaryPreview", "Chapter row summary should be nonblank.");
            });

            List<Map<String, Object>> related = (List<Map<String, Object>>) WikiDataProviders.PROVIDER.resolve(
                    context, List.of("guideBook", "related"));
            helper.assertTrue(related.stream().anyMatch(row -> homeId.toString().equals(row.get("id"))),
                    "Manual links should include an overview row.");
            related.forEach(row -> {
                assertNonBlank(helper, row, "label", "Related row label should be nonblank.");
                assertNonBlank(helper, row, "subtitlePreview", "Related row subtitle should be nonblank.");
            });

            assertWikiNavTextDraws(helper);
        } finally {
            WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.DASHBOARD);
            WikiUiState.INSTANCE.selectedGuideBook(null);
            GuideBookRegistry.clearDataForTests();
            WikiContentRegistry.clearDataForTests();
        }
        helper.succeed();
    }

    private static void seededGuideBookChapters(GameTestHelper helper) {
        helper.assertTrue(SEEDED_GUIDES.size() == 31,
                "Wiki-owned seeded guide coverage should include 31 player-facing manuals.");
        for (String slug : REQUIRED_PLAYER_FACING_GUIDES) {
            helper.assertTrue(SEEDED_GUIDES.contains(slug),
                    "Seeded guide coverage should include active player-facing guide " + slug + ".");
        }

        Map<Identifier, GuideBookDefinition> guideBooks = new LinkedHashMap<>();
        Map<Identifier, WikiArticleDefinition> articles = new LinkedHashMap<>();
        Map<Identifier, WikiCollectionDefinition> collections = new LinkedHashMap<>();
        for (String slug : SEEDED_GUIDES) {
            addSeededGuideContent(helper, slug, loadSeededGuide(slug, EchoWiki.MODID),
                    guideBooks, articles, collections);
        }
        List<String> warnings = WikiJsonReloadListener.validateGuideReferencesForTests(
                guideBooks, articles, collections);
        helper.assertTrue(warnings.isEmpty(),
                "Seeded guide-book chapters should resolve without guide reference warnings: " + warnings);
        helper.succeed();
    }

    private static void ashfallGuideBookContent(GameTestHelper helper) {
        Map<Identifier, GuideBookDefinition> guideBooks = new LinkedHashMap<>();
        Map<Identifier, WikiArticleDefinition> articles = new LinkedHashMap<>();
        Map<Identifier, WikiCollectionDefinition> collections = new LinkedHashMap<>();
        SeededGuideContent content = loadSeededGuide("ashfall", "echoashfallprotocol");
        addSeededGuideContent(helper, "ashfall", content, guideBooks, articles, collections);

        helper.assertTrue(content.guide().id().equals(id("ashfall")),
                "Ashfall guide id should remain stable for existing guide-book stacks.");
        helper.assertTrue("echoashfallprotocol".equals(content.guide().moduleId())
                        && "echoashfallprotocol".equals(content.guide().requiredModId()),
                "Ashfall guide should be owned by and gated on the root Ashfall mod.");
        helper.assertTrue(content.overview().heroArt() != null
                        && "echoashfallprotocol".equals(content.overview().heroArt().getNamespace()),
                "Ashfall overview should use an Ashfall-owned hero asset.");
        helper.assertTrue(content.overview().relatedMissions().contains(
                        Identifier.fromNamespaceAndPath("echoashfallprotocol", "ashfall_first_month_routes")),
                "Ashfall overview should link to the first-month route mission.");
        helper.assertTrue(content.chapters().values().stream().allMatch(article -> article.sections().size() >= 4),
                "Ashfall chapters should contain polished player-facing blocks.");
        WikiArticleDefinition systems = content.chapters().get(id("guides/ashfall/systems"));
        helper.assertTrue(systems != null && systems.sections().stream()
                        .noneMatch(section -> section.body().contains("A Ashfall")),
                "Ashfall systems chapter should not keep the old grammar typo.");

        List<String> warnings = WikiJsonReloadListener.validateGuideReferencesForTests(
                guideBooks, articles, collections);
        helper.assertTrue(warnings.isEmpty(),
                "Ashfall guide-book chapters should resolve without guide reference warnings: " + warnings);
        helper.succeed();
    }

    private static SeededGuideContent loadSeededGuide(String slug, String dataNamespace) {
        Identifier guideId = id(slug);
        Identifier overviewId = id("guides/" + slug);
        String base = "data/" + dataNamespace + "/echowiki";
        GuideBookDefinition guide = WikiJsonReloadListener.parseGuideBookForTests(
                guideId, resourceJson(base + "/guide_books/" + slug + ".json"));
        WikiCollectionDefinition collection = WikiJsonReloadListener.parseCollectionForTests(
                overviewId, resourceJson(base + "/collections/guides/" + slug + ".json"));
        WikiArticleDefinition overview = WikiJsonReloadListener.parseArticleForTests(
                overviewId, resourceJson(base + "/articles/guides/" + slug + ".json"));
        Map<Identifier, WikiArticleDefinition> chapters = new LinkedHashMap<>();
        for (String chapter : SEEDED_CHAPTERS) {
            Identifier chapterId = id("guides/" + slug + "/" + chapter);
            chapters.put(chapterId, WikiJsonReloadListener.parseArticleForTests(
                    chapterId, resourceJson(base + "/articles/guides/" + slug + "/" + chapter + ".json")));
        }
        return new SeededGuideContent(guide, collection, overview, Map.copyOf(chapters));
    }

    private static void addSeededGuideContent(
            GameTestHelper helper,
            String slug,
            SeededGuideContent content,
            Map<Identifier, GuideBookDefinition> guideBooks,
            Map<Identifier, WikiArticleDefinition> articles,
            Map<Identifier, WikiCollectionDefinition> collections) {
        Identifier overviewId = id("guides/" + slug);
        GuideBookDefinition guide = content.guide();
        WikiCollectionDefinition collection = content.collection();
        WikiArticleDefinition overview = content.overview();
        guideBooks.put(guide.id(), guide);
        collections.put(collection.id(), collection);
        articles.put(overview.id(), overview);
        assertNoGeneratedGuideSummary(helper, overview.id(), overview.summary());
        helper.assertTrue(overview.heroArt() != null,
                "Seeded guide overview " + overviewId + " should declare manual hero artwork.");
        helper.assertTrue(resourceExists(heroArtResourcePath(overview.heroArt())),
                "Seeded guide overview " + overviewId + " should reference an existing hero artwork texture.");

        helper.assertTrue(guide.chapterArticleIds().size() == 1 + SEEDED_CHAPTERS.size(),
                "Seeded guide " + slug + " should contain overview plus six chapters.");
        helper.assertTrue(guide.chapterArticleIds().get(0).equals(overviewId),
                "Seeded guide " + slug + " should keep the overview article first.");
        helper.assertTrue(collection.articles().equals(guide.chapterArticleIds()),
                "Seeded guide " + slug + " collection order should match guide chapter order.");

        for (String chapter : SEEDED_CHAPTERS) {
            Identifier chapterId = id("guides/" + slug + "/" + chapter);
            helper.assertTrue(guide.chapterArticleIds().contains(chapterId),
                    "Seeded guide " + slug + " should include chapter " + chapter + ".");
            WikiArticleDefinition chapterArticle = content.chapters().get(chapterId);
            helper.assertTrue(chapterArticle != null,
                    "Seeded guide chapter " + chapterId + " should have a loaded article.");
            if (chapterArticle == null) {
                continue;
            }
            helper.assertTrue(chapterArticle.category().equals("Guide Books"),
                    "Seeded guide chapter " + chapterId + " should stay in Guide Books.");
            helper.assertTrue(!chapterArticle.sections().isEmpty(),
                    "Seeded guide chapter " + chapterId + " should contain player-facing sections.");
            helper.assertTrue(chapterArticle.relatedArticles().contains(overviewId),
                    "Seeded guide chapter " + chapterId + " should link back to its manual overview.");
            assertNoGeneratedGuideSummary(helper, chapterArticle.id(), chapterArticle.summary());
            articles.put(chapterArticle.id(), chapterArticle);
        }
    }

    private static void assertNonBlank(GameTestHelper helper, Map<String, Object> row, String key, String message) {
        Object value = row.get(key);
        helper.assertTrue(value != null && !String.valueOf(value).isBlank(), message);
    }

    private static void assertWikiNavTextDraws(GameTestHelper helper) {
        EchoLayoutEngine layout = new EchoLayoutEngine();
        EchoRenderContext context = renderContext(240, 80);
        TextComponent title = screenText("title", Map.of(
                "value", "Guide Books",
                "height", "11px",
                "min-height", "11px",
                "line-height", "11px",
                "max-lines", "1",
                "wrap", "false"));
        TextComponent subtitle = screenText("text", Map.of(
                "value", "Physical manuals",
                "height", "10px",
                "min-height", "10px",
                "line-height", "10px",
                "max-lines", "1",
                "wrap", "false"));
        ContainerComponent copy = screenComponent("column", Map.of("gap", "2px", "min-width", "0"), title, subtitle);
        ContainerComponent row = screenComponent("list-row", Map.of(
                "layout", "row",
                "height", "42px",
                "min-height", "42px",
                "padding", "4px 6px",
                "gap", "6px"),
                screenComponent("status-chip", Map.of("width", "22px", "height", "22px", "min-height", "22px")),
                copy);

        layout.layout(row, context, 182, 42);
        helper.assertTrue(copy.bounds().width() > 110,
                "Wiki navigation text column should receive the remaining rail width.");
        helper.assertTrue(title.bounds().width() > 0 && title.bounds().height() >= 8,
                "Wiki navigation title should keep drawable bounds.");
        helper.assertTrue(subtitle.bounds().width() > 0 && subtitle.bounds().height() >= 8,
                "Wiki navigation subtitle should keep drawable bounds.");
        helper.assertTrue(subtitle.bounds().y() >= title.bounds().bottom(),
                "Wiki navigation subtitle should not overlap the title.");

        ArrayList<TextComponent.TextDrawRecord> drawRecords = new ArrayList<>();
        try {
            TextComponent.setDrawProbeForTests(drawRecords::add);
            row.render(context);
        } finally {
            TextComponent.setDrawProbeForTests(null);
        }
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Guide Books")),
                "Wiki navigation title should reach the text draw path.");
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Physical manuals")),
                "Wiki navigation subtitle should reach the text draw path.");
    }

    private static void assertNoGeneratedGuideSummary(GameTestHelper helper, Identifier articleId, String summary) {
        String normalized = summary.toLowerCase(java.util.Locale.ROOT);
        for (String pattern : GENERATED_GUIDE_SUMMARY_PATTERNS) {
            helper.assertTrue(!normalized.contains(pattern),
                    "Seeded guide article " + articleId + " should not keep generated summary pattern: " + pattern);
        }
    }

    private static String heroArtResourcePath(Identifier heroArt) {
        return "assets/" + heroArt.getNamespace() + "/" + heroArt.getPath();
    }

    private static GuideBookDefinition syntheticGuide(Identifier id, String requiredModId) {
        return new GuideBookDefinition(
                id,
                requiredModId,
                requiredModId,
                "Synthetic Guide",
                "Synthetic subtitle",
                "Synthetic guide summary.",
                Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                "#FF66E8FF",
                id("synthetic_collection"),
                id("synthetic_article"),
                List.of(id("synthetic_article")),
                List.of("synthetic", "guide"),
                1);
    }

    private static WikiArticleDefinition article(
            Identifier id,
            String title,
            String summary,
            List<WikiArticleSection> sections,
            List<Identifier> relatedArticles,
            List<Identifier> relatedItems) {
        return new WikiArticleDefinition(
                id,
                title,
                "Guide Books",
                summary,
                sections,
                List.of("synthetic"),
                Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                null,
                relatedArticles,
                relatedItems,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                0,
                1);
    }

    private static int expectedLastPageSize(int totalRows, int pageSize) {
        int safeSize = Math.max(1, pageSize);
        if (totalRows <= 0) {
            return 0;
        }
        int remainder = totalRows % safeSize;
        return remainder == 0 ? safeSize : remainder;
    }

    private static EchoRenderContext renderContext(int width, int height) {
        EchoThemeBridge theme = new EchoThemeBridge();
        return new EchoRenderContext(
                null,
                null,
                width,
                height,
                0,
                0,
                0.0F,
                theme.tokens(EchoAccessibilitySettings.DEFAULT),
                theme,
                new EchoRenderBridge(),
                EchoAccessibilitySettings.DEFAULT,
                EchoDataContext.empty(),
                new EchoBindingResolver(),
                new EchoFocusManager(),
                EchoResponsiveContext.of(width, height, 1.0D),
                new EchoOverlayManager(),
                new EchoScreenDiagnostics(),
                new com.knoxhack.echoscreencore.client.render.EchoTextLayer(),
                false);
    }

    private static ContainerComponent screenComponent(String tag, Map<String, String> attributes, EchoComponent... children) {
        ContainerComponent component = new ContainerComponent(screenContext(tag, attributes, children));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static TextComponent screenText(String tag, Map<String, String> attributes) {
        TextComponent component = "title".equals(tag)
                ? new TitleComponent(screenContext(tag, attributes))
                : new TextComponent(screenContext(tag, attributes));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static EchoComponentFactory.Context screenContext(String tag, Map<String, String> attributes,
            EchoComponent... children) {
        Map<String, String> safeAttributes = attributes == null ? Map.of() : attributes;
        EchoNode node = new EchoNode(tag, safeAttributes, "", List.of(), "wiki-shell-test");
        ArrayList<Object> childObjects = new ArrayList<>();
        if (children != null) {
            childObjects.addAll(List.of(children));
        }
        return new EchoComponentFactory.Context(
                tag,
                safeAttributes.getOrDefault("id", ""),
                java.util.Set.of(),
                safeAttributes,
                "",
                List.copyOf(childObjects),
                node);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
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

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return false;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoWiki.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWiki.MODID, path);
    }

    private static void assertClasspathPngBudget(GameTestHelper helper, String resourcePath,
            int expectedWidth, int expectedHeight, int maxBytes) {
        try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(resourcePath)) {
            helper.assertTrue(input != null, "Missing wiki banner resource " + resourcePath);
            assertPngBudget(helper, resourcePath, input.readAllBytes(), expectedWidth, expectedHeight, maxBytes);
        } catch (IOException exception) {
            helper.assertTrue(false, "Could not read wiki banner resource " + resourcePath + ": " + exception.getMessage());
        }
    }

    private static void assertWorkspacePngBudgetIfPresent(GameTestHelper helper, String relativePath,
            int expectedWidth, int expectedHeight, int maxBytes) {
        Path path = workspaceRoot().resolve(relativePath);
        if (!Files.exists(path)) {
            return;
        }
        try {
            assertPngBudget(helper, relativePath, Files.readAllBytes(path), expectedWidth, expectedHeight, maxBytes);
        } catch (IOException exception) {
            helper.assertTrue(false, "Could not read wiki banner resource " + relativePath + ": " + exception.getMessage());
        }
    }

    private static void assertPngBudget(GameTestHelper helper, String label, byte[] bytes,
            int expectedWidth, int expectedHeight, int maxBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        helper.assertTrue(image != null, "Wiki banner should be a readable PNG: " + label);
        helper.assertTrue(image.getWidth() == expectedWidth && image.getHeight() == expectedHeight,
                "Wiki banner should match declared EUI source size " + expectedWidth + "x" + expectedHeight + ": " + label);
        helper.assertTrue(bytes.length <= maxBytes,
                "Wiki banner should stay below " + maxBytes + " bytes: " + label + " was " + bytes.length);
    }

    private static com.google.gson.JsonObject resourceJson(String resourcePath) {
        return JsonParser.parseString(resourceText(resourcePath)).getAsJsonObject();
    }

    private static String resourceText(String resourcePath) {
        try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input != null) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read test resource " + resourcePath, exception);
        }
        Path fallback = workspaceResourcePath(resourcePath);
        if (!Files.exists(fallback)) {
            throw new IllegalStateException("Missing test resource " + resourcePath);
        }
        try {
            return Files.readString(fallback, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read test resource " + fallback, exception);
        }
    }

    private static boolean resourceExists(String resourcePath) {
        try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input != null) {
                return true;
            }
        } catch (IOException exception) {
            return false;
        }
        return Files.exists(workspaceResourcePath(resourcePath));
    }

    private static Path workspaceResourcePath(String resourcePath) {
        Path root = workspaceRoot();
        for (String sourceRoot : List.of("src/main/resources", "addons/echowiki/src/main/resources")) {
            Path candidate = root.resolve(sourceRoot).resolve(resourcePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return root.resolve(resourcePath);
    }

    private static Path workspaceRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path cursor = current;
        while (cursor != null) {
            if (Files.exists(cursor.resolve("settings.gradle")) || Files.exists(cursor.resolve("settings.gradle.kts"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return current;
    }
}
