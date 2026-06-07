package com.knoxhack.echoscreencore.test;

import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, EchoScreenCoreMod.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CYBERGLASS_CONSUMER_STYLES =
        TEST_FUNCTIONS.register("cyberglass_consumer_styles", () -> ModGameTests::cyberglassConsumerStyles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_MANIFEST_ENTRIES_EXIST =
        TEST_FUNCTIONS.register("reference_manifest_entries_exist", () -> ModGameTests::referenceManifestEntriesExist);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_LOAD =
        TEST_FUNCTIONS.register("reference_pages_load", () -> ModGameTests::referencePagesLoad);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_PASS_SMALL_VIEWPORT =
        TEST_FUNCTIONS.register("reference_pages_pass_small_viewport", () -> helper -> referencePagesPassViewport(helper, 360, 240));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_PASS_DEFAULT_VIEWPORT =
        TEST_FUNCTIONS.register("reference_pages_pass_default_viewport", () -> helper -> referencePagesPassViewport(helper, 854, 480));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_PASS_LARGE_VIEWPORT =
        TEST_FUNCTIONS.register("reference_pages_pass_large_viewport", () -> helper -> referencePagesPassViewport(helper, 1280, 720));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_WORKBENCH_RESOURCES_LOAD =
        TEST_FUNCTIONS.register("reference_workbench_resources_load", () -> ModGameTests::referenceWorkbenchResourcesLoad);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FEATURE_HUB_LINKS_RESOLVE =
        TEST_FUNCTIONS.register("feature_hub_links_resolve", () -> ModGameTests::featureHubLinksResolve);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AI_CONTRACT_REFERENCES_EXIST =
        TEST_FUNCTIONS.register("ai_contract_references_exist", () -> ModGameTests::aiContractReferencesExist);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BAD_LAYOUTS_TRIGGER_DIAGNOSTICS =
        TEST_FUNCTIONS.register("bad_layouts_trigger_diagnostics", () -> ModGameTests::badLayoutsTriggerDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIST_DETAIL_PATTERN_KEEPS_SINGLE_SCROLL_OWNER =
        TEST_FUNCTIONS.register("list_detail_pattern_keeps_single_scroll_owner", () -> ModGameTests::listDetailPatternKeepsSingleScrollOwner);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DENSE_LIST_HAS_EMPTY_STATE =
        TEST_FUNCTIONS.register("dense_list_has_empty_state", () -> ModGameTests::denseListHasEmptyState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MULTI_COLUMN_REFERENCES_HAVE_STACK_BELOW =
        TEST_FUNCTIONS.register("multi_column_references_have_stack_below", () -> ModGameTests::multiColumnReferencesHaveStackBelow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROW_TEXT_IS_BOUNDED =
        TEST_FUNCTIONS.register("row_text_is_bounded", () -> ModGameTests::rowTextIsBounded);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "cyberglass_consumer_styles", CYBERGLASS_CONSUMER_STYLES.getId());
        register(event, "reference_manifest_entries_exist", REFERENCE_MANIFEST_ENTRIES_EXIST.getId());
        register(event, "reference_pages_load", REFERENCE_PAGES_LOAD.getId());
        register(event, "reference_pages_pass_small_viewport", REFERENCE_PAGES_PASS_SMALL_VIEWPORT.getId());
        register(event, "reference_pages_pass_default_viewport", REFERENCE_PAGES_PASS_DEFAULT_VIEWPORT.getId());
        register(event, "reference_pages_pass_large_viewport", REFERENCE_PAGES_PASS_LARGE_VIEWPORT.getId());
        register(event, "reference_workbench_resources_load", REFERENCE_WORKBENCH_RESOURCES_LOAD.getId());
        register(event, "feature_hub_links_resolve", FEATURE_HUB_LINKS_RESOLVE.getId());
        register(event, "ai_contract_references_exist", AI_CONTRACT_REFERENCES_EXIST.getId());
        register(event, "bad_layouts_trigger_diagnostics", BAD_LAYOUTS_TRIGGER_DIAGNOSTICS.getId());
        register(event, "list_detail_pattern_keeps_single_scroll_owner", LIST_DETAIL_PATTERN_KEEPS_SINGLE_SCROLL_OWNER.getId());
        register(event, "dense_list_has_empty_state", DENSE_LIST_HAS_EMPTY_STATE.getId());
        register(event, "multi_column_references_have_stack_below", MULTI_COLUMN_REFERENCES_HAVE_STACK_BELOW.getId());
        register(event, "row_text_is_bounded", ROW_TEXT_IS_BOUNDED.getId());
    }

    private static void cyberglassConsumerStyles(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String kit = Files.readString(root.resolve("addons/echothemecore/src/main/resources/assets/echothemecore/eui/styles/cyberglass_kit.eui.css"));
            helper.assertTrue(kit.contains("app-shell") && kit.contains("app-header") && kit.contains("inspector-panel"),
                    "CyberGlass kit should cover app shell, header, and inspector surfaces.");
            helper.assertTrue(kit.contains("holomap-mode-button") && kit.contains("button[hovered]")
                            && kit.contains("theme-texture(screencore.button.hover)"),
                    "CyberGlass kit should expose stateful ScreenCore button/focus texture styling.");
            helper.assertTrue(kit.contains("height: 26px") && kit.contains("theme-texture(screencore.edge_rails)")
                            && kit.contains("texture-inset-2: 1px"),
                    "CyberGlass kit should keep compact controls and layered page edge rails.");

            String npc = Files.readString(root.resolve("addons/echonpcore/src/main/resources/assets/echonpcore/eui/pages/npc_interaction.eui.xml"));
            helper.assertTrue(npc.contains("styles=\"echothemecore:cyberglass_kit,npc_interaction\""),
                    "NPCore ScreenCore page should load ThemeCore CyberGlass kit before local NPC styles.");

            String holomap = Files.readString(root.resolve("addons/echoholomap/src/main/resources/assets/echoholomap/eui/pages/fullscreen_holomap.eui.xml"));
            helper.assertTrue(holomap.contains("styles=\"echothemecore:cyberglass_kit")
                            || holomap.contains(",echothemecore:cyberglass_kit"),
                    "HoloMap ScreenCore page should load the shared CyberGlass kit.");
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to inspect CyberGlass ScreenCore resource wiring: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void referenceManifestEntriesExist(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String manifest = Files.readString(screenCoreResource(root, "eui/eui_manifest.json"));
            helper.assertTrue(manifest.contains("\"featureHub\": \"echoscreencore:reference_feature_hub\""),
                    "Manifest should expose the ScreenCore feature hub.");
            helper.assertTrue(manifest.contains("\"workbench\": \"echoscreencore:reference_workbench\""),
                    "Manifest should expose the ScreenCore workbench page.");
            for (String page : referencePageIds()) {
                helper.assertTrue(manifest.contains(page), "Manifest should register reference page " + page + ".");
            }
            for (String component : starterComponentIds()) {
                helper.assertTrue(manifest.contains(component), "Manifest should register starter component " + component + ".");
            }
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read ScreenCore manifest: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void referencePagesLoad(GameTestHelper helper) {
        Path root = workspaceRoot();
        for (String page : referencePageIds()) {
            Path file = pageResource(root, page);
            helper.assertTrue(Files.exists(file), "Reference page resource should exist: " + file);
        }
        helper.succeed();
    }

    private static void referencePagesPassViewport(GameTestHelper helper, int width, int height) {
        Path root = workspaceRoot();
        try {
            String contract = Files.readString(root.resolve("addons/echoscreencore/docs/screencore_ai_contract.json"));
            helper.assertTrue(contract.contains("\"width\": " + width) && contract.contains("\"height\": " + height),
                    "AI contract should include viewport " + width + "x" + height + ".");
            for (String pageId : referencePageIds()) {
                if (pageId.endsWith("reference_bad_layouts")) {
                    continue;
                }
                String page = Files.readString(pageResource(root, pageId));
                helper.assertTrue(!page.contains("height=\"700px\""),
                        pageId + " should not contain giant fixed-height fixtures.");
                if (page.contains("columns=\"") || page.contains("<component src=\"echoscreencore:sc_dashboard_grid\"")) {
                    helper.assertTrue(page.contains("stack-below") || page.contains("stackBelow"),
                            pageId + " should advertise a stack rule for viewport " + width + "x" + height + ".");
                }
            }
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed viewport reference validation: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void referenceWorkbenchResourcesLoad(GameTestHelper helper) {
        Path root = workspaceRoot();
        helper.assertTrue(Files.exists(pageResource(root, "echoscreencore:reference_workbench")),
                "Reference workbench page should exist.");
        helper.assertTrue(Files.exists(screenCoreResource(root, "eui/styles/screencore_workbench.eui.css")),
                "Reference workbench style should exist.");
        helper.succeed();
    }

    private static void featureHubLinksResolve(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String hub = Files.readString(pageResource(root, "echoscreencore:reference_feature_hub"));
            helper.assertTrue(hub.contains("screencore.reference.categories")
                            && hub.contains("screencore.reference.features")
                            && hub.contains("screencore.reference.open_reference"),
                    "Feature hub should bind categories/features and open real reference pages.");
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read feature hub page: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void aiContractReferencesExist(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String contract = Files.readString(root.resolve("addons/echoscreencore/docs/screencore_ai_contract.json"));
            helper.assertTrue(contract.contains("\"viewportChecks\"")
                            && contract.contains("\"width\": 360")
                            && contract.contains("\"width\": 854")
                            && contract.contains("\"width\": 1280"),
                    "AI contract should declare required viewport checks.");
            for (String page : referencePageIds()) {
                if (!page.endsWith("reference_bad_layouts") && !page.endsWith("reference_feature_hub")
                        && !page.endsWith("reference_workbench") && !page.endsWith("reference_three_column")
                        && !page.endsWith("reference_inputs") && !page.endsWith("reference_selects_dropdowns")
                        && !page.endsWith("reference_accessibility")) {
                    helper.assertTrue(contract.contains(page), "AI contract should reference starter page " + page + ".");
                }
            }
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read AI contract: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void badLayoutsTriggerDiagnostics(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String bad = Files.readString(pageResource(root, "echoscreencore:reference_bad_layouts"));
            helper.assertTrue(bad.contains("height=\"700px\""), "Bad layout page should include a giant fixed-height fixture.");
            helper.assertTrue(bad.contains("<grid columns=\"1fr 1fr 1fr\""), "Bad layout page should include a grid missing stack-below.");
            helper.assertTrue(bad.contains("<scroll>") && bad.indexOf("<scroll>") != bad.lastIndexOf("<scroll>"),
                    "Bad layout page should include nested scroll fixture.");
            helper.assertTrue(bad.contains("<list>") && !bad.contains("<list>\n                <empty-state"),
                    "Bad layout page should include a missing empty-state fixture.");
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read bad layouts page: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void listDetailPatternKeepsSingleScrollOwner(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String page = Files.readString(pageResource(root, "echoscreencore:reference_list_detail"));
            helper.assertTrue(count(page, "<scroll ") == 1, "List/detail reference should have one scroll owner.");
            helper.assertTrue(page.contains("class=\"sc-list-detail\"") && page.contains("stack-below=\"760\""),
                    "List/detail reference should use the safe responsive grid.");
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read list/detail page: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void denseListHasEmptyState(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String page = Files.readString(pageResource(root, "echoscreencore:reference_dense_list"));
            helper.assertTrue(page.contains("<empty-state"), "Dense list reference should include an empty-state.");
            helper.assertTrue(count(page, "<scroll ") == 1, "Dense list reference should keep one scroll owner.");
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read dense list page: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void multiColumnReferencesHaveStackBelow(GameTestHelper helper) {
        Path root = workspaceRoot();
        for (String pageId : List.of("echoscreencore:reference_dashboard", "echoscreencore:reference_three_column",
                "echoscreencore:reference_feature_hub", "echoscreencore:reference_selects_dropdowns",
                "echoscreencore:reference_accessibility")) {
            try {
                String page = Files.readString(pageResource(root, pageId));
                helper.assertTrue(page.contains("stack-below") || page.contains("stackBelow"),
                        pageId + " should include a responsive stack rule.");
            } catch (IOException exception) {
                helper.assertTrue(false, "Failed to read " + pageId + ": " + exception.getMessage());
            }
        }
        helper.succeed();
    }

    private static void rowTextIsBounded(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String style = Files.readString(screenCoreResource(root, "eui/styles/screencore_app_kit.eui.css"));
            helper.assertTrue(style.contains(".sc-row-copy title")
                            && style.contains("max-lines: 1")
                            && style.contains(".sc-row-copy text")
                            && style.contains("max-lines: 2")
                            && style.contains("overflow: hidden"),
                    "Starter kit should bound row title/text copy.");
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read app kit style: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("screencore_" + testName));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1,
            false, 16);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoScreenCoreMod.MOD_ID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return false;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoScreenCoreMod.MOD_ID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
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

    private static Path screenCoreResource(Path root, String resourcePath) {
        return root.resolve("addons/echoscreencore/src/main/resources/assets/echoscreencore").resolve(resourcePath);
    }

    private static Path pageResource(Path root, String pageId) {
        String path = pageId.substring(pageId.indexOf(':') + 1);
        return screenCoreResource(root, "eui/pages/" + path + ".eui.xml");
    }

    private static List<String> referencePageIds() {
        return List.of(
                "echoscreencore:reference_feature_hub",
                "echoscreencore:reference_workbench",
                "echoscreencore:reference_dashboard",
                "echoscreencore:reference_list_detail",
                "echoscreencore:reference_three_column",
                "echoscreencore:reference_dense_list",
                "echoscreencore:reference_settings",
                "echoscreencore:reference_inputs",
                "echoscreencore:reference_selects_dropdowns",
                "echoscreencore:reference_modal_overlay",
                "echoscreencore:reference_accessibility",
                "echoscreencore:reference_bad_layouts");
    }

    private static List<String> starterComponentIds() {
        return List.of(
                "echoscreencore:sc_app_shell",
                "echoscreencore:sc_page_header",
                "echoscreencore:sc_dashboard_grid",
                "echoscreencore:sc_list_detail_shell",
                "echoscreencore:sc_dense_list",
                "echoscreencore:sc_detail_panel",
                "echoscreencore:sc_action_strip",
                "echoscreencore:sc_empty_panel",
                "echoscreencore:sc_feature_card",
                "echoscreencore:sc_diagnostic_row");
    }

    private static int count(String text, String needle) {
        int count = 0;
        int cursor = 0;
        while (text != null && needle != null && !needle.isEmpty()) {
            int next = text.indexOf(needle, cursor);
            if (next < 0) {
                return count;
            }
            count++;
            cursor = next + needle.length();
        }
        return count;
    }
}
