package com.knoxhack.echoscreencore.client.test;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.basic.ButtonComponent;
import com.knoxhack.echoscreencore.client.component.basic.CopyBlockComponent;
import com.knoxhack.echoscreencore.client.component.basic.ProgressBarComponent;
import com.knoxhack.echoscreencore.client.component.basic.TextComponent;
import com.knoxhack.echoscreencore.client.component.basic.TitleComponent;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.component.layout.ScrollPanelComponent;
import com.knoxhack.echoscreencore.client.api.EchoFitScreenSurface;
import com.knoxhack.echoscreencore.client.debug.EchoDiagnosticCatalog;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.engine.EchoBindingResolver;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import com.knoxhack.echoscreencore.client.input.EchoFocusManager;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.layout.EchoLayoutEngine;
import com.knoxhack.echoscreencore.client.layout.EchoResponsiveContext;
import com.knoxhack.echoscreencore.client.overlay.EchoOverlayManager;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.render.EchoThemeBridge;
import com.knoxhack.echoscreencore.client.style.EchoStyleParser;
import com.knoxhack.echoscreencore.client.style.EchoStyleResolver;
import com.knoxhack.echoscreencore.client.style.EchoStyleSheet;
import com.knoxhack.echoscreencore.client.style.EchoStyleState;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.core.Holder;
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
import org.lwjgl.glfw.GLFW;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, EchoScreenCoreMod.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> THEME_TEXTURE_VALUES =
        TEST_FUNCTIONS.register("client_theme_texture_values", () -> ModGameTests::themeTextureValues);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESPONSIVE_STACKED_LAYOUT =
        TEST_FUNCTIONS.register("client_responsive_stacked_layout", () -> ModGameTests::responsiveStackedLayout);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STYLE_SELECTOR_STATES =
        TEST_FUNCTIONS.register("client_style_selector_states", () -> ModGameTests::styleSelectorStates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TEXT_LAYOUT_SAFETY =
        TEST_FUNCTIONS.register("client_text_layout_safety", () -> ModGameTests::textLayoutSafety);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CYBERGLASS_CONSUMER_STYLES =
        TEST_FUNCTIONS.register("client_cyberglass_consumer_styles", () -> ModGameTests::cyberglassConsumerStyles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_MANIFEST_ENTRIES_EXIST =
        TEST_FUNCTIONS.register("client_reference_manifest_entries_exist", () -> ModGameTests::referenceManifestEntriesExist);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_LOAD =
        TEST_FUNCTIONS.register("client_reference_pages_load", () -> ModGameTests::referencePagesLoad);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_PASS_SMALL_VIEWPORT =
        TEST_FUNCTIONS.register("client_reference_pages_pass_small_viewport", () -> helper -> referencePagesPassViewport(helper, 360, 240));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_PASS_DEFAULT_VIEWPORT =
        TEST_FUNCTIONS.register("client_reference_pages_pass_default_viewport", () -> helper -> referencePagesPassViewport(helper, 854, 480));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_PAGES_PASS_LARGE_VIEWPORT =
        TEST_FUNCTIONS.register("client_reference_pages_pass_large_viewport", () -> helper -> referencePagesPassViewport(helper, 1280, 720));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFERENCE_WORKBENCH_RESOURCES_LOAD =
        TEST_FUNCTIONS.register("client_reference_workbench_resources_load", () -> ModGameTests::referenceWorkbenchResourcesLoad);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FEATURE_HUB_LINKS_RESOLVE =
        TEST_FUNCTIONS.register("client_feature_hub_links_resolve", () -> ModGameTests::featureHubLinksResolve);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AI_CONTRACT_REFERENCES_EXIST =
        TEST_FUNCTIONS.register("client_ai_contract_references_exist", () -> ModGameTests::aiContractReferencesExist);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DIAGNOSTIC_CATALOG_COVERS_KNOWN_CODES =
        TEST_FUNCTIONS.register("client_diagnostic_catalog_covers_known_codes", () -> ModGameTests::diagnosticCatalogCoversKnownCodes);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BAD_LAYOUTS_TRIGGER_DIAGNOSTICS =
        TEST_FUNCTIONS.register("client_bad_layouts_trigger_diagnostics", () -> ModGameTests::badLayoutsTriggerDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIST_DETAIL_PATTERN_KEEPS_SINGLE_SCROLL_OWNER =
        TEST_FUNCTIONS.register("client_list_detail_pattern_keeps_single_scroll_owner", () -> ModGameTests::listDetailPatternKeepsSingleScrollOwner);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DENSE_LIST_HAS_EMPTY_STATE =
        TEST_FUNCTIONS.register("client_dense_list_has_empty_state", () -> ModGameTests::denseListHasEmptyState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MULTI_COLUMN_REFERENCES_HAVE_STACK_BELOW =
        TEST_FUNCTIONS.register("client_multi_column_references_have_stack_below", () -> ModGameTests::multiColumnReferencesHaveStackBelow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROW_TEXT_IS_BOUNDED =
        TEST_FUNCTIONS.register("client_row_text_is_bounded", () -> ModGameTests::rowTextIsBounded);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIT_SCREEN_SURFACE_SCALES_SMALL_GUI =
        TEST_FUNCTIONS.register("client_fit_screen_surface_scales_small_gui", () -> ModGameTests::fitScreenSurfaceScalesSmallGui);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCROLL_PANEL_CULLS_OFFSCREEN_CHILDREN =
        TEST_FUNCTIONS.register("client_scroll_panel_culls_offscreen_children", () -> ModGameTests::scrollPanelCullsOffscreenChildren);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCROLL_PANEL_MOUSE_DRAG_SCROLLS =
        TEST_FUNCTIONS.register("client_scroll_panel_mouse_drag_scrolls", () -> ModGameTests::scrollPanelMouseDragScrolls);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCROLL_LAYOUT_CACHE_REUSES_MEASUREMENTS =
        TEST_FUNCTIONS.register("client_scroll_layout_cache_reuses_measurements", () -> ModGameTests::scrollLayoutCacheReusesMeasurements);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INPUT_ROUTER_CACHES_HOVER_HIT_TESTS =
        TEST_FUNCTIONS.register("client_input_router_caches_hover_hit_tests", () -> ModGameTests::inputRouterCachesHoverHitTests);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "client_theme_texture_values", THEME_TEXTURE_VALUES.getId());
        register(event, "client_responsive_stacked_layout", RESPONSIVE_STACKED_LAYOUT.getId());
        register(event, "client_style_selector_states", STYLE_SELECTOR_STATES.getId());
        register(event, "client_text_layout_safety", TEXT_LAYOUT_SAFETY.getId());
        register(event, "client_cyberglass_consumer_styles", CYBERGLASS_CONSUMER_STYLES.getId());
        register(event, "client_reference_manifest_entries_exist", REFERENCE_MANIFEST_ENTRIES_EXIST.getId());
        register(event, "client_reference_pages_load", REFERENCE_PAGES_LOAD.getId());
        register(event, "client_reference_pages_pass_small_viewport", REFERENCE_PAGES_PASS_SMALL_VIEWPORT.getId());
        register(event, "client_reference_pages_pass_default_viewport", REFERENCE_PAGES_PASS_DEFAULT_VIEWPORT.getId());
        register(event, "client_reference_pages_pass_large_viewport", REFERENCE_PAGES_PASS_LARGE_VIEWPORT.getId());
        register(event, "client_reference_workbench_resources_load", REFERENCE_WORKBENCH_RESOURCES_LOAD.getId());
        register(event, "client_feature_hub_links_resolve", FEATURE_HUB_LINKS_RESOLVE.getId());
        register(event, "client_ai_contract_references_exist", AI_CONTRACT_REFERENCES_EXIST.getId());
        register(event, "client_diagnostic_catalog_covers_known_codes", DIAGNOSTIC_CATALOG_COVERS_KNOWN_CODES.getId());
        register(event, "client_bad_layouts_trigger_diagnostics", BAD_LAYOUTS_TRIGGER_DIAGNOSTICS.getId());
        register(event, "client_list_detail_pattern_keeps_single_scroll_owner", LIST_DETAIL_PATTERN_KEEPS_SINGLE_SCROLL_OWNER.getId());
        register(event, "client_dense_list_has_empty_state", DENSE_LIST_HAS_EMPTY_STATE.getId());
        register(event, "client_multi_column_references_have_stack_below", MULTI_COLUMN_REFERENCES_HAVE_STACK_BELOW.getId());
        register(event, "client_row_text_is_bounded", ROW_TEXT_IS_BOUNDED.getId());
        register(event, "client_fit_screen_surface_scales_small_gui", FIT_SCREEN_SURFACE_SCALES_SMALL_GUI.getId());
        register(event, "client_scroll_panel_culls_offscreen_children", SCROLL_PANEL_CULLS_OFFSCREEN_CHILDREN.getId());
        register(event, "client_scroll_panel_mouse_drag_scrolls", SCROLL_PANEL_MOUSE_DRAG_SCROLLS.getId());
        register(event, "client_scroll_layout_cache_reuses_measurements", SCROLL_LAYOUT_CACHE_REUSES_MEASUREMENTS.getId());
        register(event, "client_input_router_caches_hover_hit_tests", INPUT_ROUTER_CACHES_HOVER_HIT_TESTS.getId());
    }

    private static void themeTextureValues(GameTestHelper helper) {
        EchoThemeBridge theme = new EchoThemeBridge();
        EchoScreenDiagnostics diagnostics = new EchoScreenDiagnostics();
        String directTexture = "echoscreencore:textures/gui/direct.png";
        helper.assertTrue(directTexture.equals(EchoStyleValues.texture(directTexture, "", theme, diagnostics)),
            "Direct texture identifiers should pass through unchanged.");

        String missing = EchoStyleValues.texture("theme-texture(screencore.missing_texture)", "", theme, diagnostics);
        helper.assertTrue(missing.isBlank(), "Missing theme-texture tokens should fall back to no texture.");
        helper.assertTrue(diagnostics.issues().stream()
                .anyMatch(issue -> "invalid_theme_texture".equals(issue.code())),
            "Missing theme-texture tokens should emit diagnostics.");

        String token = "screencore.surface.raised";
        String resolved = EchoStyleValues.texture("theme-texture(" + token + ")", "", theme, new EchoScreenDiagnostics());
        EchoCoreServices.themeService().resolveTexture(token).ifPresentOrElse(
            texture -> helper.assertTrue(texture.toString().equals(resolved),
                "theme-texture should resolve to the active ThemeCore texture id."),
            () -> helper.assertTrue(resolved.isBlank(),
                "theme-texture should fall back cleanly when ThemeCore is absent.")
        );
        helper.succeed();
    }

    private static void responsiveStackedLayout(GameTestHelper helper) {
        EchoLayoutEngine layout = new EchoLayoutEngine();
        EchoRenderContext compact = renderContext(600, 220);

        ContainerComponent first = component("section", Map.of("height", "70px"));
        ContainerComponent second = component("section", Map.of("height", "80px"));
        ContainerComponent grid = component("grid", Map.of("height", "120px", "gap", "5px", "columns", "1fr 1fr",
                "stack-below", "960"), first, second);
        layout.layout(grid, compact, 240, 120);
        helper.assertTrue(first.bounds().height() == 70 && second.bounds().height() == 80,
                "Stacked responsive grids should preserve child explicit heights.");
        helper.assertTrue(second.bounds().y() >= first.bounds().bottom() + 5,
                "Stacked responsive grids should lay children vertically without overlap.");

        ContainerComponent scrollFirst = component("section", Map.of("height", "80px"));
        ContainerComponent scrollSecond = component("section", Map.of("height", "90px"));
        ContainerComponent stackedGrid = component("grid", Map.of("height", "100px", "gap", "10px", "columns", "1fr 1fr",
                "stack-below", "960"), scrollFirst, scrollSecond);
        ScrollPanelComponent scroll = scroll("scroll", Map.of("height", "100px"), stackedGrid);
        layout.layout(scroll, compact, 240, 100);
        helper.assertTrue(scroll.maxScroll() > 0,
                "Scroll panels should measure stacked grid content and expose scroll range.");
        helper.assertTrue(scrollSecond.bounds().y() >= scrollFirst.bounds().bottom() + 10,
                "Stacked grid content inside scroll panels should not overlap.");

        ContainerComponent collapsedChild = component("section", Map.of("height", "50px"));
        ContainerComponent collapsed = component("grid", Map.of("height", "100px", "collapse-below", "960"),
                collapsedChild);
        layout.layout(collapsed, compact, 240, 100);
        helper.assertTrue(collapsedChild.bounds().equals(EchoRect.ZERO),
                "Responsive collapsed nodes should still hide their child tree.");

        ContainerComponent hidden = component("section", Map.of("height", "50px", "hide-below", "960"));
        layout.layout(hidden, compact, 240, 100);
        helper.assertTrue(hidden.bounds().equals(EchoRect.ZERO),
                "Responsive hidden nodes should still receive zero bounds.");
        helper.succeed();
    }

    private static void styleSelectorStates(GameTestHelper helper) {
        EchoStyleParser parser = new EchoStyleParser();
        EchoStyleResolver resolver = new EchoStyleResolver();
        EchoScreenDiagnostics diagnostics = new EchoScreenDiagnostics();
        EchoStyleSheet sheet = parser.parse(id("selector_states"), """
                .child { color: #111111ff; height: 20px; }
                .parent .child { color: #222222ff; }
                .parent .child title { color: #333333ff; }
                .parent .child:hover { background: #444444ff; height: 40px; }
                .parent .child[disabled] { border-color: #555555ff; }
                .parent .child[selected="true"][hovered] { accent-color: #666666ff; }
                """, diagnostics);
        EchoNode parent = new EchoNode("section", Map.of("class", "parent"), "", List.of(), "selector-test");
        EchoNode child = new EchoNode("list-row", Map.of("class", "child", "selected", "true"), "", List.of(), "selector-test");
        EchoNode title = new EchoNode("title", Map.of(), "", List.of(), "selector-test");

        EchoStyle orphan = resolver.resolve(child, List.of(), List.of(sheet), EchoAccessibilitySettings.DEFAULT,
                diagnostics, EchoStyleState.NONE);
        helper.assertTrue("#111111ff".equals(orphan.value("color", "")),
                "Direct class selectors should still work without ancestors.");

        EchoStyle nested = resolver.resolve(child, List.of(parent), List.of(sheet), EchoAccessibilitySettings.DEFAULT,
                diagnostics, EchoStyleState.NONE);
        helper.assertTrue("#222222ff".equals(nested.value("color", "")),
                "Descendant selectors should apply only when the ancestor chain matches.");
        helper.assertTrue("20px".equals(nested.value("height", "")),
                "Base layout style should remain separate from runtime pseudo-state overrides.");

        EchoStyle nestedTitle = resolver.resolve(title, List.of(parent, child), List.of(sheet),
                EchoAccessibilitySettings.DEFAULT, diagnostics, EchoStyleState.NONE);
        helper.assertTrue("#333333ff".equals(nestedTitle.value("color", "")),
                "Descendant selectors should match nested tag selectors.");

        EchoStyle hovered = resolver.resolve(child, List.of(parent), List.of(sheet), EchoAccessibilitySettings.DEFAULT,
                diagnostics, new EchoStyleState(true, false, false, true, false));
        helper.assertTrue("#444444ff".equals(hovered.value("background", "")),
                "Runtime hover pseudo-state should resolve into the effective render style.");
        helper.assertTrue("#666666ff".equals(hovered.value("accent-color", "")),
                "Multiple attribute and pseudo-state selectors should match together.");
        helper.assertTrue("40px".equals(hovered.value("height", "")),
                "Pseudo-state styles should be available to render-time resolution without mutating base layout style.");
        helper.assertTrue("20px".equals(nested.value("height", "")),
                "Base style resolution should not be mutated by a later hovered resolve.");

        EchoStyle disabled = resolver.resolve(child, List.of(parent), List.of(sheet), EchoAccessibilitySettings.DEFAULT,
                diagnostics, new EchoStyleState(false, false, true, true, false));
        helper.assertTrue("#555555ff".equals(disabled.value("border-color", "")),
                "Runtime disabled pseudo-state should resolve into the effective render style.");
        helper.succeed();
    }

    private static void textLayoutSafety(GameTestHelper helper) {
        EchoLayoutEngine layout = new EchoLayoutEngine();
        EchoRenderContext context = renderContext(320, 120);
        ArrayList<TextComponent.TextDrawRecord> drawRecords = new ArrayList<>();
        TextComponent title = text("title", Map.of(
                "value", "Visible row title",
                "line-height", "11px",
                "max-lines", "1",
                "wrap", "false"));
        TextComponent body = text("text", Map.of(
                "value", "Visible row summary",
                "line-height", "10px",
                "max-lines", "1",
                "wrap", "false"));
        ContainerComponent copy = component("column", Map.of("gap", "1px"), title, body);
        ContainerComponent leading = component("status-chip", Map.of("width", "44px", "height", "18px"));
        ContainerComponent trailing = component("status-chip", Map.of("width", "54px", "height", "18px"));
        ContainerComponent row = component("list-row", Map.of(
                "height", "32px",
                "min-height", "32px",
                "padding", "4px 6px",
                "gap", "6px"), leading, copy, trailing);

        layout.layout(row, context, 220, 32);
        helper.assertTrue(copy.bounds().width() > leading.bounds().width(),
                "Text copy column should receive remaining horizontal row width.");
        helper.assertTrue(title.bounds().width() > 0 && title.bounds().height() >= 8,
                "Title children in compact rows should retain readable nonzero bounds.");
        helper.assertTrue(body.bounds().width() > 0 && body.bounds().height() >= 8,
                "Text children in compact rows should retain readable nonzero bounds.");
        helper.assertTrue(body.bounds().y() >= title.bounds().bottom(),
                "Stacked title/text copy should not overlap inside the row column.");

        TextComponent scrollTitle = text("title", Map.of(
                "value", "Scrollable text title",
                "line-height", "12px",
                "max-lines", "1",
                "wrap", "false"));
        TextComponent scrollBody = text("text", Map.of(
                "value", "Scrollable text body stays visible under parent clipping.",
                "line-height", "10px",
                "max-lines", "1",
                "wrap", "false"));
        ContainerComponent scrollRow = component("list-row", Map.of(
                "height", "32px",
                "padding", "4px 6px",
                "gap", "6px"),
                component("status-chip", Map.of("width", "44px", "height", "18px")),
                component("column", Map.of("gap", "1px"), scrollTitle, scrollBody));
        ScrollPanelComponent scroll = scroll("scroll", Map.of("height", "28px"), scrollRow);

        layout.layout(scroll, context, 180, 28);
        helper.assertTrue(scroll.bounds().height() == 28 && scroll.maxScroll() > 0,
                "Scroll panels should own overflow while measuring text-heavy row content.");
        helper.assertTrue(scrollTitle.bounds().width() > 0 && scrollTitle.bounds().height() >= 8,
                "Title text inside a scroll panel should still receive drawable bounds.");
        helper.assertTrue(scrollBody.bounds().width() > 0 && scrollBody.bounds().height() >= 8,
                "Body text inside a scroll panel should still receive drawable bounds.");

        TextComponent clipped = text("text", Map.of(
                "value", "Explicitly clipped text",
                "overflow", "hidden",
                "line-height", "12px"));
        helper.assertTrue("hidden".equals(clipped.style().value("overflow", "")),
                "Explicit text overflow clipping should remain an opt-in style.");

        EchoStyleSheet textVisibility = new EchoStyleParser().parse(id("text_visibility"), """
                title.cyberglass-measured {
                  width: 152px;
                  line-height: 14px;
                  max-lines: 1;
                  wrap: false;
                  color: theme(textPrimary);
                }
                text.cyberglass-measured {
                  width: 168px;
                  line-height: 12px;
                  max-lines: 1;
                  wrap: false;
                  color: theme(textSecondary);
                }
                button.cyberglass-measured {
                  width: 132px;
                  height: 26px;
                  min-height: 24px;
                  color: theme(textPrimary);
                }
                """, new EchoScreenDiagnostics());
        TextComponent styledTitle = text("title", Map.of(
                "class", "cyberglass-measured",
                "value", "Styled CyberGlass title"));
        styledTitle.setStyleContext(List.of(textVisibility), List.of());
        var styledTitleMeasure = styledTitle.measure(context, 320, 80);
        helper.assertTrue(styledTitleMeasure.width() == 152 && styledTitleMeasure.height() >= 14,
                "Styled ScreenCore titles should measure from the same effective style used for rendering.");
        styledTitle.setBounds(new EchoRect(0, 0, styledTitleMeasure.width(), styledTitleMeasure.height()));
        ArrayList<TextComponent.TextDrawRecord> styledDraws = new ArrayList<>();
        try {
            TextComponent.setDrawProbeForTests(styledDraws::add);
            context.textLayer().beginFrame();
            styledTitle.render(context);
            context.textLayer().flush(context);
        } finally {
            TextComponent.setDrawProbeForTests(null);
        }
        helper.assertTrue(styledDraws.stream().anyMatch(record -> record.value().contains("Styled CyberGlass title")
                        && record.drawCalled()),
                "Styled ScreenCore titles should still reach an actual GuiGraphics text draw call.");

        ButtonComponent styledButton = button(Map.of(
                "class", "cyberglass-measured",
                "value", "Open Protocol"));
        styledButton.setStyleContext(List.of(textVisibility), List.of());
        var styledButtonMeasure = styledButton.measure(context, 320, 80);
        helper.assertTrue(styledButtonMeasure.width() == 132 && styledButtonMeasure.height() == 26,
                "Styled ScreenCore buttons should measure from effective kit dimensions so labels keep vertical room.");

        TextComponent routeTitle = text("title", Map.of(
                "class", "terminal-route-row-title",
                "value", "Anchor Pod Outpost",
                "line-height", "11px",
                "max-lines", "1",
                "wrap", "false"));
        TextComponent routeSubtitle = text("text", Map.of(
                "class", "terminal-route-row-subtitle",
                "value", "Craft and place the first outpost anchor.",
                "line-height", "10px",
                "max-lines", "1",
                "wrap", "false"));
        ProgressBarComponent routeProgress = progress(Map.of("height", "4px", "min-height", "4px"));
        ContainerComponent routeCopy = component("column", Map.of("class", "terminal-route-row-copy", "gap", "2px"),
                routeTitle, routeSubtitle, routeProgress);
        ContainerComponent routeRow = component("list-row", Map.of(
                "height", "64px",
                "min-height", "64px",
                "padding", "6px 8px",
                "gap", "7px"),
                component("status-chip", Map.of("width", "46px", "height", "18px")),
                routeCopy,
                component("status-chip", Map.of("width", "64px", "height", "18px")));
        layout.layout(routeRow, context, 420, 64);
        helper.assertTrue(routeCopy.bounds().width() >= 260,
                "Route row copy should receive the remaining width between fixed chips.");
        helper.assertTrue(routeTitle.bounds().width() > 0 && routeTitle.bounds().height() >= 8,
                "Route row title should receive drawable bounds.");
        helper.assertTrue(routeSubtitle.bounds().width() > 0 && routeSubtitle.bounds().height() >= 8,
                "Route row subtitle should receive drawable bounds.");
        helper.assertTrue(routeProgress.bounds().y() >= routeSubtitle.bounds().bottom(),
                "Route row progress should sit below title/subtitle text.");

        TextComponent actionTitle = text("title", Map.of(
                "value", "Recovered Blackbox",
                "line-height", "11px",
                "max-lines", "1",
                "wrap", "false"));
        TextComponent actionBody = text("text", Map.of(
                "value", "Pull signal notes and keep the row copy readable.",
                "line-height", "10px",
                "max-lines", "1",
                "wrap", "false"));
        ContainerComponent actionCopy = component("column", Map.of("class", "terminal-row-copy", "gap", "2px"),
                actionTitle, actionBody);
        ButtonComponent rowButton = button(Map.of("value", "Open"));
        ContainerComponent actionRow = component("list-row", Map.of(
                "height", "52px",
                "padding", "5px 7px",
                "gap", "7px"),
                component("status-chip", Map.of("width", "58px", "height", "18px")),
                actionCopy,
                rowButton);
        layout.layout(actionRow, context, 360, 52);
        helper.assertTrue(rowButton.bounds().width() >= 72 && actionCopy.bounds().width() >= 200,
                "Inline buttons should measure as fixed controls so row copy keeps the remaining readable width.");
        helper.assertTrue(actionTitle.bounds().width() > 0 && actionBody.bounds().width() > 0,
                "Action row title and body should keep drawable bounds next to a trailing button.");

        try {
            TextComponent.setDrawProbeForTests(drawRecords::add);
            context.textLayer().beginFrame();
            routeRow.render(context);
            context.textLayer().flush(context);
        } finally {
            TextComponent.setDrawProbeForTests(null);
        }
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Anchor Pod Outpost")
                        && record.drawCalled()),
                "Route row title should reach an actual GuiGraphics text draw call.");
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Craft and place")
                        && record.drawCalled()),
                "Route row subtitle should reach an actual GuiGraphics text draw call.");

        drawRecords.clear();
        ScrollPanelComponent routeScroll = scroll("scroll", Map.of("height", "48px"), routeRow);
        layout.layout(routeScroll, context, 420, 48);
        try {
            TextComponent.setDrawProbeForTests(drawRecords::add);
            context.textLayer().beginFrame();
            routeScroll.render(context);
            context.textLayer().flush(context);
        } finally {
            TextComponent.setDrawProbeForTests(null);
        }
        helper.assertTrue(routeScroll.maxScroll() > 0,
                "Route row inside scroll panel should expose overflow instead of collapsing text.");
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Anchor Pod Outpost")
                        && record.drawCalled()),
                "Text inside scroll panels should still reach an actual draw call under parent clipping.");
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Anchor Pod Outpost")
                        && record.drawCalled() && record.clipped()),
                "Text inside scroll panels should inherit the parent scroll clip.");

        drawRecords.clear();
        ProgressBarComponent copyProgress = progress(Map.of("height", "4px", "min-height", "4px"));
        CopyBlockComponent directCopy = copyBlock(Map.of(
                "class", "terminal-route-row-copy terminal-route-copy-block",
                "title", "Direct Copy Route",
                "subtitle", "Copy-block draws row text during its own render pass.",
                "title-line-height", "11px",
                "detail-line-height", "10px",
                "text-gap", "2px",
                "content-height", "4px"), copyProgress);
        ContainerComponent directRow = component("list-row", Map.of(
                "height", "64px",
                "min-height", "64px",
                "padding", "6px 8px",
                "gap", "7px"),
                component("status-chip", Map.of("width", "46px", "height", "18px")),
                directCopy,
                component("status-chip", Map.of("width", "64px", "height", "18px")));
        layout.layout(directRow, context, 420, 64);
        helper.assertTrue(directCopy.bounds().width() >= 260 && directCopy.bounds().height() >= 27,
                "copy-block should receive readable row-copy bounds between fixed controls.");
        helper.assertTrue(copyProgress.bounds().y() >= directCopy.bounds().y() + 23,
                "copy-block progress children should sit below title/subtitle text.");
        try {
            TextComponent.setDrawProbeForTests(drawRecords::add);
            directRow.render(context);
        } finally {
            TextComponent.setDrawProbeForTests(null);
        }
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Direct Copy Route")
                        && record.drawCalled()),
                "copy-block route titles should prove a direct GuiGraphics text draw call.");
        helper.assertTrue(drawRecords.stream().anyMatch(record -> record.value().contains("Copy-block draws")
                        && record.drawCalled()),
                "copy-block route subtitles should prove a direct GuiGraphics text draw call.");
        helper.succeed();
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

    private static void diagnosticCatalogCoversKnownCodes(GameTestHelper helper) {
        for (String code : List.of("root_overflows_viewport", "large_fixed_height", "grid_missing_stack_below",
                "nested_scroll_region", "row_overflow", "unbounded_row_text", "missing_list_empty_state",
                "unknown_reference_page", "reference_page_failed_contract")) {
            helper.assertTrue(EchoDiagnosticCatalog.entries().stream().anyMatch(entry -> code.equals(entry.code())),
                    "Diagnostic catalog should cover " + code + ".");
            helper.assertTrue(!EchoDiagnosticCatalog.fixHint(code).isBlank(),
                    "Diagnostic catalog should provide a fix hint for " + code + ".");
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

    private static void fitScreenSurfaceScalesSmallGui(GameTestHelper helper) {
        EchoScreenEngine.FitPolicy responsivePolicy = EchoScreenEngine.fitPolicy(EchoNode.builder("page").build());
        helper.assertTrue(!responsivePolicy.canvas()
                        && responsivePolicy.designWidth() == 0
                        && responsivePolicy.designHeight() == 0,
                "Pages without a declared canvas should stay responsive.");
        EchoScreenEngine.FitPolicy canvasPolicy = EchoScreenEngine.fitPolicy(EchoNode.builder("page")
                .attribute("fit-mode", "canvas")
                .attribute("design-width", "1280")
                .attribute("design-height", "720")
                .build());
        helper.assertTrue(canvasPolicy.canvas()
                        && canvasPolicy.designWidth() == 1280
                        && canvasPolicy.designHeight() == 720,
                "Canvas pages should expose their declared ScreenCore design canvas.");

        EchoFitScreenSurface.Fit responsive = EchoFitScreenSurface.responsiveFit(410, 220);
        helper.assertTrue(responsive.layoutWidth() == 410
                        && responsive.layoutHeight() == 220
                        && responsive.scale() == 1.0D
                        && responsive.scaledWidth() == 410
                        && responsive.scaledHeight() == 220,
                "Responsive pages should use the actual viewport with no hidden design canvas.");

        for (int[] viewport : List.of(new int[] {683, 367}, new int[] {512, 275}, new int[] {410, 220})) {
            EchoFitScreenSurface.Fit canvas = EchoFitScreenSurface.canvasFit(viewport[0], viewport[1], 1280, 720);
            helper.assertTrue(canvas.layoutWidth() == 1280 && canvas.layoutHeight() == 720,
                    "Terminal canvas should keep the 1280x720 layout canvas at " + viewport[0] + "x" + viewport[1]);
            helper.assertTrue(canvas.scale() > 0.0D && canvas.scale() < 1.0D,
                    "Terminal canvas should scale down inside small GUI viewport " + viewport[0] + "x" + viewport[1]);
            helper.assertTrue(canvas.scaledWidth() <= viewport[0] && canvas.scaledHeight() <= viewport[1],
                    "Scaled Terminal canvas should never exceed viewport " + viewport[0] + "x" + viewport[1]);
        }

        EchoFitScreenSurface.Fit tinyCanvas = EchoFitScreenSurface.canvasFit(410, 220, 1280, 720);
        double screenX = 17.0D + tinyCanvas.offsetX() + 100.0D * tinyCanvas.scale();
        double screenY = 23.0D + tinyCanvas.offsetY() + 80.0D * tinyCanvas.scale();
        helper.assertTrue(Math.abs(tinyCanvas.localX(screenX, 17) - 100.0D) < 0.0001D
                        && Math.abs(tinyCanvas.localY(screenY, 23) - 80.0D) < 0.0001D,
                "Canvas input coordinates should be transformed by the same surface scale.");
        helper.assertTrue(Math.abs(tinyCanvas.localDelta(12.0D) - (12.0D / tinyCanvas.scale())) < 0.0001D,
                "Canvas drag deltas should be transformed by the same surface scale.");
        helper.succeed();
    }

    private static void scrollPanelCullsOffscreenChildren(GameTestHelper helper) {
        EchoLayoutEngine layout = new EchoLayoutEngine();
        EchoRenderContext context = renderContext(240, 100);

        ButtonComponent visibleButton = button(Map.of("action", "noop", "width", "20px", "height", "20px"));
        ButtonComponent offscreenButton = button(Map.of("action", "noop", "width", "20px", "height", "20px"));
        CountingComponent first = counting("first", visibleButton);
        CountingComponent second = counting("second");
        CountingComponent third = counting("third");
        CountingComponent fourth = counting("fourth");
        CountingComponent fifth = counting("fifth");
        CountingComponent last = counting("last", offscreenButton);
        ScrollPanelComponent scroll = scroll("scroll", Map.of("height", "100px"),
                first, second, third, fourth, fifth, last);

        layout.layout(scroll, context, 240, 100);
        scroll.render(context);

        helper.assertTrue(first.renderCalls() == 1,
                "Scroll panels should render rows inside the viewport.");
        helper.assertTrue(last.renderCalls() == 0,
                "Scroll panels should skip rows beyond the viewport overscan.");
        helper.assertTrue(visibleButton.bounds().width() > 0 && visibleButton.bounds().height() > 0,
                "Visible scroll rows should receive full child layout.");
        helper.assertTrue(offscreenButton.bounds().equals(EchoRect.ZERO),
                "Offscreen scroll rows should keep child trees hidden until they enter the viewport.");

        scroll.setScrollOffset(120);
        layout.layout(scroll, context, 240, 100);
        helper.assertTrue(offscreenButton.bounds().width() > 0 && offscreenButton.bounds().height() > 0,
                "Rows should be laid out when scrolling brings them into the viewport overscan.");
        helper.succeed();
    }

    private static void scrollPanelMouseDragScrolls(GameTestHelper helper) {
        EchoLayoutEngine layout = new EchoLayoutEngine();
        EchoRenderContext context = renderContext(240, 80);
        ScrollPanelComponent scroll = scroll("scroll", Map.of("height", "80px"),
                counting("one"),
                counting("two"),
                counting("three"),
                counting("four"),
                counting("five"));

        layout.layout(scroll, context, 240, 80);
        helper.assertTrue(scroll.maxScroll() > 0,
                "Mouse-drag scroll test should create a real overflowing scroll panel.");

        double railX = scroll.bounds().right() - 5.0D;
        double railY = scroll.bounds().y() + 8.0D;
        boolean clicked = scroll.mouseClicked(railX, railY, GLFW.GLFW_MOUSE_BUTTON_LEFT,
                (action, component, inputEvent) -> false);
        boolean dragged = scroll.mouseDragged(railX, railY + 48.0D, GLFW.GLFW_MOUSE_BUTTON_LEFT,
                0.0D, 48.0D, (action, component, inputEvent) -> false);
        boolean released = scroll.mouseReleased(railX, railY + 48.0D, GLFW.GLFW_MOUSE_BUTTON_LEFT,
                (action, component, inputEvent) -> false);

        helper.assertTrue(clicked && dragged && released && scroll.scrollOffset() > 0,
                "Scroll panel thumbs should support click-drag scrolling with the mouse.");
        helper.succeed();
    }

    private static void scrollLayoutCacheReusesMeasurements(GameTestHelper helper) {
        EchoLayoutEngine layout = new EchoLayoutEngine();
        EchoRenderContext context = renderContext(240, 100);
        List<CountingComponent> rows = List.of(
                counting("one"),
                counting("two"),
                counting("three"),
                counting("four"),
                counting("five"),
                counting("six"));
        ScrollPanelComponent scroll = scroll("scroll", Map.of("height", "100px"), rows.toArray(EchoComponent[]::new));

        layout.layout(scroll, context, 240, 100);
        int firstPassMeasures = rows.stream().mapToInt(CountingComponent::measureCalls).sum();
        helper.assertTrue(firstPassMeasures == rows.size(),
                "Initial scroll layout should measure each uncached row once.");

        scroll.setScrollOffset(24);
        layout.layout(scroll, context, 240, 100);
        int secondPassMeasures = rows.stream().mapToInt(CountingComponent::measureCalls).sum();
        helper.assertTrue(secondPassMeasures == firstPassMeasures,
                "Pure scroll offset layout should reuse cached row measurements.");

        layout.layout(scroll, context, 260, 100);
        int resizedMeasures = rows.stream().mapToInt(CountingComponent::measureCalls).sum();
        helper.assertTrue(resizedMeasures > secondPassMeasures,
                "Changing scroll width should invalidate cached row measurements.");
        helper.succeed();
    }

    private static void inputRouterCachesHoverHitTests(GameTestHelper helper) {
        EchoInputRouter router = new EchoInputRouter(new EchoFocusManager());
        ContainerComponent root = component("section", Map.of());
        root.setBounds(new EchoRect(0, 0, 100, 100));

        router.updateHover(root, 10, 10);
        int first = router.hoverHitTestsForTests();
        router.updateHover(root, 10, 10);
        helper.assertTrue(router.hoverHitTestsForTests() == first,
                "Hover hit-testing should be skipped when mouse, tree, and layout state are unchanged.");

        router.updateHover(root, 11, 10);
        helper.assertTrue(router.hoverHitTestsForTests() == first + 1,
                "Moving the mouse should recompute hover hit-testing.");

        router.invalidateHover();
        router.updateHover(root, 11, 10);
        helper.assertTrue(router.hoverHitTestsForTests() == first + 2,
                "Explicit invalidation should force one fresh hover hit-test.");
        helper.succeed();
    }

    private static EchoRenderContext renderContext(int width, int height) {
        EchoThemeBridge theme = new EchoThemeBridge();
        return new EchoRenderContext(
                TestGuiGraphics.create(),
                new TestFont(),
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

    private static ContainerComponent component(String tag, Map<String, String> attributes, EchoComponent... children) {
        ContainerComponent component = new ContainerComponent(context(tag, attributes, children));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static ScrollPanelComponent scroll(String tag, Map<String, String> attributes, EchoComponent... children) {
        ScrollPanelComponent component = new ScrollPanelComponent(context(tag, attributes, children));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static TextComponent text(String tag, Map<String, String> attributes) {
        TextComponent component = "title".equals(tag)
                ? new TitleComponent(context(tag, attributes))
                : new TextComponent(context(tag, attributes));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static CopyBlockComponent copyBlock(Map<String, String> attributes, EchoComponent... children) {
        CopyBlockComponent component = new CopyBlockComponent(context("copy-block", attributes, children));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static ButtonComponent button(Map<String, String> attributes) {
        ButtonComponent component = new ButtonComponent(context("button", attributes));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static ProgressBarComponent progress(Map<String, String> attributes) {
        ProgressBarComponent component = new ProgressBarComponent(context("progress-bar", attributes));
        component.setStyle(new EchoStyle(attributes));
        return component;
    }

    private static CountingComponent counting(String id, EchoComponent... children) {
        CountingComponent component = new CountingComponent(context("list-row", Map.of("id", id), children));
        component.setStyle(new EchoStyle(Map.of()));
        return component;
    }

    private static EchoComponentFactory.Context context(String tag, Map<String, String> attributes, EchoComponent... children) {
        Map<String, String> safeAttributes = attributes == null ? Map.of() : attributes;
        EchoNode node = new EchoNode(tag, safeAttributes, "", List.of(), "responsive-test");
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

    private static final class CountingComponent extends ContainerComponent {
        private int measureCalls;
        private int renderCalls;

        private CountingComponent(EchoComponentFactory.Context context) {
            super(context);
        }

        @Override
        public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
            measureCalls++;
            return new EchoMeasureResult(Math.max(0, availableWidth), 40);
        }

        @Override
        public void render(EchoRenderContext context) {
            renderCalls++;
        }

        private int measureCalls() {
            return measureCalls;
        }

        private int renderCalls() {
            return renderCalls;
        }
    }

    private static final class TestFont extends Font {
        private TestFont() {
            super(new Provider() {
                @Override
                public net.minecraft.client.gui.GlyphSource glyphs(FontDescription description) {
                    return null;
                }

                @Override
                public net.minecraft.client.gui.font.glyphs.EffectGlyph effect() {
                    return null;
                }
            });
        }

        @Override
        public int width(String value) {
            return value == null ? 0 : value.length() * 6;
        }

        @Override
        public String plainSubstrByWidth(String value, int width) {
            return plainSubstrByWidth(value, width, false);
        }

        @Override
        public String plainSubstrByWidth(String value, int width, boolean reverse) {
            if (value == null || width <= 0) {
                return "";
            }
            int maxChars = Math.max(0, width / 6);
            if (value.length() <= maxChars) {
                return value;
            }
            return reverse ? value.substring(Math.max(0, value.length() - maxChars)) : value.substring(0, maxChars);
        }
    }

    private static final class TestGuiGraphics extends GuiGraphicsExtractor {
        private TestGuiGraphics(int width, int height) {
            super(null, null, width, height);
        }

        private static TestGuiGraphics create() {
            try {
                java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
                return (TestGuiGraphics) unsafe.allocateInstance(TestGuiGraphics.class);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                return null;
            }
        }

        @Override
        public void nextStratum() {
        }

        @Override
        public void enableScissor(int x0, int y0, int x1, int y1) {
        }

        @Override
        public void disableScissor() {
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int color) {
        }

        @Override
        public void outline(int x, int y, int width, int height, int color) {
        }

        @Override
        public void text(Font font, String text, int x, int y, int color) {
        }

        @Override
        public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        }

        @Override
        public void centeredText(Font font, String text, int x, int y, int color) {
        }

        @Override
        public void blit(Identifier texture, int x0, int y0, int x1, int y1,
                float u0, float u1, float v0, float v1) {
        }

        @Override
        public void item(ItemStack stack, int x, int y) {
        }

        @Override
        public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        }

        @Override
        public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
        }
    }
}
