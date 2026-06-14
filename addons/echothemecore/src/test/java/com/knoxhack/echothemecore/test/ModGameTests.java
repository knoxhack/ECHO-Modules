package com.knoxhack.echothemecore.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeApi;
import com.knoxhack.echothemecore.api.EchoThemeRenderPreset;
import com.knoxhack.echothemecore.api.EchoThemeSoundKey;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.api.ThemeVisualSettings;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.RenderPresetRegistry;
import com.knoxhack.echothemecore.content.RenderPresetReloadListener;
import com.knoxhack.echothemecore.content.ThemeJsonReloadListener;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.knoxhack.echothemecore.client.vanilla.VanillaUiProtectedBounds;
import com.knoxhack.echothemecore.client.vanilla.VanillaUiScreenClassifier;
import com.knoxhack.echothemecore.client.vanilla.VanillaUiSkinLayer;
import com.knoxhack.echothemecore.client.vanilla.VanillaUiSurface;
import com.knoxhack.echothemecore.integration.ThemeCoreTerminalBridge;
import com.knoxhack.echothemecore.service.ThemeCoreService;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        DeferredRegister.create(Registries.TEST_FUNCTION, EchoThemeCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> THEME_PARSE =
        TEST_FUNCTIONS.register("theme_json_parse", () -> ModGameTests::themeJsonParse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REGISTRY_FALLBACK =
        TEST_FUNCTIONS.register("registry_fallback", () -> ModGameTests::registryFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VISUAL_SETTINGS =
        TEST_FUNCTIONS.register("visual_settings", () -> ModGameTests::visualSettings);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VANILLA_UI_SCREEN_CLASSIFICATION =
        TEST_FUNCTIONS.register("vanilla_ui_screen_classification", () -> ModGameTests::vanillaUiScreenClassification);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VANILLA_BUTTON_CHROME_BOUNDS =
        TEST_FUNCTIONS.register("vanilla_button_chrome_bounds", () -> ModGameTests::vanillaButtonChromeBounds);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VANILLA_LOADING_PANEL_BOUNDS =
        TEST_FUNCTIONS.register("vanilla_loading_panel_bounds", () -> ModGameTests::vanillaLoadingPanelBounds);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NO_LEGACY_LINE_OVERLAYS =
        TEST_FUNCTIONS.register("no_legacy_line_overlay_terms", () -> ModGameTests::noLegacyLineOverlayTerms);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CYBERGLASS_FULL_THEME =
        TEST_FUNCTIONS.register("cyberglass_full_theme_contract", () -> ModGameTests::cyberglassFullThemeContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_THEME_CONTRACT =
        TEST_FUNCTIONS.register("nexus_theme_contract", () -> ModGameTests::nexusThemeContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RENDER_PRESET_REGISTRY =
        TEST_FUNCTIONS.register("render_preset_registry", () -> ModGameTests::renderPresetRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UI_TEXTURE_DIMENSIONS =
        TEST_FUNCTIONS.register("ui_texture_dimensions", () -> ModGameTests::uiTextureDimensions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLIENT_THEME_CYCLE =
        TEST_FUNCTIONS.register("client_theme_cycle_contract", () -> ModGameTests::clientThemeCycleContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PUBLIC_THEME_TOKENS =
        TEST_FUNCTIONS.register("public_theme_token_contract", () -> ModGameTests::publicThemeTokenContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MIXIN_CLIENT_ONLY =
        TEST_FUNCTIONS.register("mixin_client_only_contract", () -> ModGameTests::mixinClientOnlyContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCREENCORE_DYNAMIC_PICKER =
        TEST_FUNCTIONS.register("screencore_dynamic_picker_contract", () -> ModGameTests::screenCoreDynamicPickerContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLIENT_LOCAL_ONLY =
        TEST_FUNCTIONS.register("client_local_only_contract", () -> ModGameTests::clientLocalOnlyContract);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "theme_json_parse", THEME_PARSE.getId());
        register(event, "registry_fallback", REGISTRY_FALLBACK.getId());
        register(event, "visual_settings", VISUAL_SETTINGS.getId());
        register(event, "vanilla_ui_screen_classification", VANILLA_UI_SCREEN_CLASSIFICATION.getId());
        register(event, "vanilla_button_chrome_bounds", VANILLA_BUTTON_CHROME_BOUNDS.getId());
        register(event, "vanilla_loading_panel_bounds", VANILLA_LOADING_PANEL_BOUNDS.getId());
        register(event, "no_legacy_line_overlay_terms", NO_LEGACY_LINE_OVERLAYS.getId());
        register(event, "cyberglass_full_theme_contract", CYBERGLASS_FULL_THEME.getId());
        register(event, "nexus_theme_contract", NEXUS_THEME_CONTRACT.getId());
        register(event, "render_preset_registry", RENDER_PRESET_REGISTRY.getId());
        register(event, "ui_texture_dimensions", UI_TEXTURE_DIMENSIONS.getId());
        register(event, "client_theme_cycle_contract", CLIENT_THEME_CYCLE.getId());
        register(event, "public_theme_token_contract", PUBLIC_THEME_TOKENS.getId());
        register(event, "mixin_client_only_contract", MIXIN_CLIENT_ONLY.getId());
        register(event, "screencore_dynamic_picker_contract", SCREENCORE_DYNAMIC_PICKER.getId());
        register(event, "client_local_only_contract", CLIENT_LOCAL_ONLY.getId());
    }

    private static void themeJsonParse(GameTestHelper helper) {
        EchoTheme parsed = ThemeJsonReloadListener.parseThemeForTests(id("parse_test"),
            JsonParser.parseString("""
                {
                  "id": "echothemecore:parse_test",
                  "display_name": "Parse Test",
                  "colors": {
                    "primary": "#00E5FF",
                    "secondary": "#B44CFF",
                    "accent": "#FF2BD6",
                    "background": "#030711",
                    "panel": "#08111FCC",
                    "panel_alt": "#0D1A2ECC",
                    "glass": "#10243A88",
                    "border": "#2BEAFF",
                    "border_soft": "#1A6F8A",
                    "text": "#EAFBFF",
                    "muted_text": "#8AAFC2",
                    "success": "#45FFB0",
                    "warning": "#FFD166",
                    "error": "#FF4D6D",
                    "locked": "#3B4652",
                    "glow": "#00E5FF",
                    "selection": "#B44CFF"
                  }
                }
                """).getAsJsonObject());
        helper.assertTrue(parsed.id().equals(id("parse_test")), "Theme id should parse.");
        helper.assertTrue(parsed.colors().primary() == 0xFF00E5FF, "Primary color should parse to ARGB.");
        helper.assertTrue(parsed.uiAssets().panelTexture() != null, "UI assets should receive safe defaults.");
        EchoTheme iconParsed = ThemeJsonReloadListener.parseThemeForTests(id("parse_icon_test"),
            JsonParser.parseString("""
                {
                  "id": "echothemecore:parse_icon_test",
                  "display_name": "Parse Icon Test",
                  "colors": {
                    "primary": "#00E5FF",
                    "secondary": "#B44CFF",
                    "accent": "#FF2BD6",
                    "background": "#030711",
                    "panel": "#08111FCC",
                    "panel_alt": "#0D1A2ECC",
                    "glass": "#10243A88",
                    "border": "#2BEAFF",
                    "border_soft": "#1A6F8A",
                    "text": "#EAFBFF",
                    "muted_text": "#8AAFC2",
                    "success": "#45FFB0",
                    "warning": "#FFD166",
                    "error": "#FF4D6D",
                    "locked": "#3B4652",
                    "glow": "#00E5FF",
                    "selection": "#B44CFF"
                  },
                  "module_assets": {
                    "item_icon": {
                      "replacements": {
                        "minecraft:diamond": "echothemecore:textures/gui/themes/cyberglass/item_icon/mission_marker.png"
                      }
                    }
                  }
                }
                """).getAsJsonObject());
        helper.assertTrue(EchoThemeApi.itemIconReplacement(iconParsed, Identifier.withDefaultNamespace("diamond")).isPresent(),
            "Opt-in item icon replacement textures should parse into theme metadata.");
        helper.succeed();
    }

    private static void nexusThemeContract(GameTestHelper helper) {
        EchoTheme parsed = parsePackagedNexus();
        helper.assertTrue(parsed.id().equals(ThemeRegistry.NEXUS_ID), "Packaged Nexus id should match the registry id.");
        helper.assertTrue("1.3.0".equals(parsed.metadata().get("version")), "Packaged Nexus metadata should advertise ThemeCore 1.3.0.");
        for (EchoThemeTextureKey key : indexAndVanillaContractKeys()) {
            Identifier texture = parsed.moduleTexture(key)
                .orElseThrow(() -> new AssertionError("Nexus should expose module texture " + key));
            assertPackagedTexture(helper, texture);
        }
        assertAllReferencedThemeTextures(helper, packagedThemeJson("nexus"), "Nexus");
        ThemeRegistry.replaceLoaded(Map.of(parsed.id(), parsed));
        helper.assertTrue(ThemeRegistry.setGlobalTheme(ThemeRegistry.NEXUS_ID),
            "ThemeCore should be able to switch to packaged Nexus.");
        for (String token : List.of(
                "index.panel",
                "index.panel_wide",
                "index.panel_active",
                "index.button",
                "index.button_hover",
                "index.card",
                "index.card_selected",
                "index.status_chip",
                "index.progress_bar",
                "index.scrollbar",
                "index.icon",
                "ui.pause_panel",
                "ui.inventory_frame",
                "ui.container_frame")) {
            helper.assertTrue(ThemeCoreService.INSTANCE.resolveTexture(token).isPresent(),
                "ThemeCore should resolve Nexus texture token " + token);
        }
        ThemeRegistry.replaceLoaded(Map.of());
        helper.succeed();
    }

    private static void registryFallback(GameTestHelper helper) {
        ThemeRegistry.replaceLoaded(Map.of());
        helper.assertTrue(ThemeRegistry.get(id("missing")).id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "Missing themes should fall back to ECHO Platform.");
        ThemeRegistry.setGlobalTheme(id("missing"));
        helper.assertTrue(ThemeRegistry.getCurrentTheme().id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "Invalid global theme should resolve to ECHO Platform.");
        helper.succeed();
    }

    private static void visualSettings(GameTestHelper helper) {
        ThemeRegistry.setDebugVisualIntensity(0.5F);
        ThemeVisualSettings settings = ThemeVisualSettings.resolve(ThemeRegistry.getCurrentTheme());
        helper.assertTrue(settings.glowIntensity() <= 1.0F, "Debug scale should cap glow intensity.");
        ThemeRegistry.setDebugVisualIntensity(1.0F);
        helper.succeed();
    }

    private static void vanillaUiScreenClassification(GameTestHelper helper) {
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen", true) == VanillaUiSurface.ECHO_SCREEN,
            "ECHO Terminal should not be classified as a vanilla container.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen", true) == VanillaUiSurface.ECHO_SCREEN,
            "ScreenCore Terminal should not be classified as a vanilla container.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "net.minecraft.client.gui.screens.PauseScreen", false) == VanillaUiSurface.PAUSE_MENU,
            "Vanilla pause screens should receive bounded pause-menu theming.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "net.minecraft.client.gui.screens.inventory.ChestScreen", true) == VanillaUiSurface.CONTAINER,
            "Vanilla chest screens should keep container theming.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "com.knoxhack.echoindustrialnexus.client.IndustrialMachineScreen", true) == VanillaUiSurface.CONTAINER,
            "ECHO machine screens should keep container theming.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "net.neoforged.neoforge.client.gui.ModListScreen", false) == VanillaUiSurface.LOADER_MODS,
            "NeoForge mod list should receive CyberGlass Mods theming.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "net.neoforged.neoforge.client.gui.ConfigurationScreen", false) == VanillaUiSurface.LOADER_MODS,
            "NeoForge config screens should receive CyberGlass Mods theming.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "net.neoforged.neoforge.client.gui.LoadingErrorScreen", false) == VanillaUiSurface.LOADER_MODS,
            "NeoForge loading error screens should receive CyberGlass Mods theming.");
        helper.assertTrue(VanillaUiScreenClassifier.classifyForTests(
                "net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen", false) == VanillaUiSurface.LOADER_MODS,
            "NeoForge mismatch screens should receive CyberGlass Mods theming.");
        VanillaUiProtectedBounds pausePanel = VanillaUiSkinLayer.pausePanelBoundsForTests(320, 240, List.of(
                new VanillaUiProtectedBounds(120, 96, 80, 20),
                new VanillaUiProtectedBounds(84, 122, 72, 20),
                new VanillaUiProtectedBounds(164, 122, 72, 20),
                new VanillaUiProtectedBounds(84, 148, 152, 20),
                new VanillaUiProtectedBounds(84, 174, 152, 20),
                new VanillaUiProtectedBounds(84, 226, 152, 20)));
        helper.assertTrue(pausePanel.contains(120, 96) && pausePanel.contains(235, 193),
            "Bounded pause panels should cover the visible pause button cluster.");
        helper.assertTrue(pausePanel.width() <= 240 && pausePanel.height() <= 140 && pausePanel.y() >= 52,
            "Bounded pause panels should keep only a compact title allowance instead of extending into a tall slab.");
        helper.assertTrue(pausePanel.y() + pausePanel.height() <= 208,
            "Bounded pause panels should ignore far-below outlier controls.");
        helper.succeed();
    }

    private static void vanillaButtonChromeBounds(GameTestHelper helper) {
        VanillaUiProtectedBounds chrome = VanillaUiSkinLayer.buttonChromeBoundsForTests(100, 50, 120, 20);
        helper.assertTrue(chrome.x() == 100 && chrome.y() == 50,
            "Button theming should draw exactly within the vanilla widget bounds.");
        helper.assertTrue(chrome.width() == 120 && chrome.height() == 20,
            "Button theming should preserve vanilla widget dimensions without layout shifts.");
        helper.assertTrue(chrome.contains(100, 50) && chrome.contains(219, 69),
            "Button chrome bounds should contain the original widget area.");
        VanillaUiProtectedBounds pausePanel = VanillaUiSkinLayer.pausePanelBoundsForTests(2048, 1102, List.of(
            new VanillaUiProtectedBounds(862, 280, 326, 31),
            new VanillaUiProtectedBounds(862, 318, 154, 31),
            new VanillaUiProtectedBounds(1032, 318, 156, 31),
            new VanillaUiProtectedBounds(862, 356, 154, 31),
            new VanillaUiProtectedBounds(1032, 356, 156, 31),
            new VanillaUiProtectedBounds(862, 394, 154, 31),
            new VanillaUiProtectedBounds(1032, 394, 156, 31),
            new VanillaUiProtectedBounds(862, 432, 326, 31),
            new VanillaUiProtectedBounds(862, 472, 326, 31),
            new VanillaUiProtectedBounds(862, 760, 326, 31)));
        helper.assertTrue(pausePanel.y() + pausePanel.height() <= 516,
            "Pause panel should not leave a long lower CyberGlass tail below the last button.");
        VanillaUiProtectedBounds titleChip = VanillaUiSkinLayer.pauseTitleChipBoundsForTests(2048, 1102, List.of(
            new VanillaUiProtectedBounds(862, 280, 326, 31),
            new VanillaUiProtectedBounds(862, 318, 154, 31),
            new VanillaUiProtectedBounds(1032, 318, 156, 31),
            new VanillaUiProtectedBounds(862, 356, 154, 31),
            new VanillaUiProtectedBounds(1032, 356, 156, 31),
            new VanillaUiProtectedBounds(862, 394, 154, 31),
            new VanillaUiProtectedBounds(1032, 394, 156, 31),
            new VanillaUiProtectedBounds(862, 432, 326, 31),
            new VanillaUiProtectedBounds(862, 472, 326, 31),
            new VanillaUiProtectedBounds(862, 760, 326, 31)), 92);
        helper.assertTrue(titleChip.height() <= 20 && titleChip.y() + titleChip.height() < pausePanel.y(),
            "Pause title chip should stay compact and separate from the button panel.");
        helper.assertFalse(VanillaUiSkinLayer.shouldDecorateWidgetForTests(VanillaUiSurface.PAUSE_MENU, 2048, 1102,
                new VanillaUiProtectedBounds(1188, 396, 608, 2), pausePanel),
            "Pause widget chrome should not decorate oversized stray widgets that produce long horizontal rails.");
        helper.assertFalse(VanillaUiSkinLayer.shouldDecorateWidgetForTests(VanillaUiSurface.PAUSE_MENU, 2048, 1102,
                new VanillaUiProtectedBounds(900, 760, 90, 18), pausePanel,
                new VanillaUiProtectedBounds(862, 280, 326, 223)),
            "Pause widget chrome should not decorate sane-sized controls outside the selected stack.");
        helper.assertTrue(VanillaUiSkinLayer.shouldDecorateWidgetForTests(VanillaUiSurface.PAUSE_MENU, 2048, 1102,
                new VanillaUiProtectedBounds(900, 330, 90, 18), pausePanel,
                new VanillaUiProtectedBounds(862, 280, 326, 223)),
            "Pause widget chrome may still decorate compact controls inside the bounded panel.");
        try {
            String source = Files.readString(workspaceRoot().resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/client/vanilla/VanillaUiSkinLayer.java"));
            helper.assertTrue(source.contains("renderThemedButtonLabel(graphics, button"),
                "Button reskin should redraw labels after painting the CyberGlass surface.");
            helper.assertTrue(source.contains("graphics.fill(x, y, x + w, y + h, base)"),
                "Button reskin should cover the vanilla gray body with a themed CyberGlass surface.");
            helper.assertTrue(source.contains("drawPauseMenuPlate(graphics, theme, texture.orElse(null)"),
                "Pause panels should use the dedicated procedural plate instead of the generic texture body renderer.");
            helper.assertFalse(source.contains("""
                drawDecorPlate(graphics, theme, texture.orElse(null),
                    bounds.x(), bounds.y(), bounds.width(), bounds.height(), true);
                """),
                "Pause panels must not feed vanilla_pause_panel into the generic compact texture body path.");
            helper.assertTrue(source.contains("private static void drawPauseTextureAccents"),
                "Pause texture accents should stay isolated from generic compact texture rails.");
            helper.assertFalse(source.contains("drawPauseTextureAccents(graphics, texture, x, y + h -"),
                "Pause texture accents should not sample or draw bottom strips from vanilla_pause_panel.");
        } catch (IOException exception) {
            helper.fail("Could not inspect VanillaUiSkinLayer source for button text safety: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void vanillaLoadingPanelBounds(GameTestHelper helper) {
        VanillaUiProtectedBounds desktop = VanillaUiSkinLayer.loadingPanelBoundsForTests(2048, 1102);
        helper.assertTrue(desktop.width() == 760 && desktop.height() == 300,
            "Desktop loading panel should use the bounded maximum size instead of the full viewport.");
        helper.assertTrue(desktop.x() >= 48 && desktop.y() >= 48
                && desktop.x() + desktop.width() <= 2048 - 48
                && desktop.y() + desktop.height() <= 1102 - 48,
            "Desktop loading panel should keep at least 48px breathing room around the CyberGlass plate.");
        helper.assertTrue(desktop.width() < 2048 / 2 && desktop.height() < 1102 / 2,
            "Loading panel should not become a giant cropped full-screen slab.");

        VanillaUiProtectedBounds compact = VanillaUiSkinLayer.loadingPanelBoundsForTests(320, 240);
        helper.assertTrue(compact.x() >= 16 && compact.y() >= 16
                && compact.x() + compact.width() <= 320 - 16
                && compact.y() + compact.height() <= 240 - 16,
            "Small loading screens should clamp the panel inside the viewport.");
        try {
            String source = Files.readString(workspaceRoot().resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/client/vanilla/VanillaUiSkinLayer.java"));
            helper.assertFalse(source.contains("case MAIN_MENU, LOADING -> blitIfPresent(graphics, theme, EchoThemeTextureKey.VANILLA_BACKGROUND"),
                "Loading screens must not share the main-menu full-screen background blit.");
            helper.assertFalse(source.contains("case LOADING -> blitIfPresent(graphics, theme, EchoThemeTextureKey.VANILLA_BACKGROUND"),
                "Loading accents must not redraw the CyberGlass background texture full-screen.");
        } catch (IOException exception) {
            helper.fail("Could not inspect VanillaUiSkinLayer source for loading blit regression: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void noLegacyLineOverlayTerms(GameTestHelper helper) {
        String forbidden = "scan" + "line";
        for (EchoTheme theme : ThemeRegistry.listThemes()) {
            String combined = theme.id() + " " + theme.displayName() + " " + theme.description() + " " + theme.metadata();
            helper.assertFalse(combined.toLowerCase(java.util.Locale.ROOT).contains(forbidden),
                "Theme metadata should not contain forbidden legacy line-overlay terms.");
        }
        helper.succeed();
    }

    private static void cyberglassFullThemeContract(GameTestHelper helper) {
        EchoTheme parsed = parsePackagedCyberGlass();
        ThemeRegistry.replaceLoaded(Map.of());
        EchoTheme builtin = ThemeRegistry.get(ThemeRegistry.CYBERGLASS_ID);
        helper.assertTrue(ThemeRegistry.getCurrentTheme().id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "ThemeCore should start on ECHO Platform before datapack themes load.");
        helper.assertTrue(ThemeRegistry.fallbackTheme().id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "ThemeCore fallback should be ECHO Platform before datapack themes load.");
        helper.assertTrue(parsed.id().equals(builtin.id()), "Packaged and builtin CyberGlass ids should match.");
        helper.assertTrue(parsed.colors().primary() == builtin.colors().primary(), "Builtin CyberGlass primary color should match JSON.");
        helper.assertTrue(parsed.soundProfile().sound(EchoThemeSoundKey.UI_CLICK).isPresent(), "CyberGlass should expose themed UI click sound.");
        helper.assertFalse(builtin.blockPalette().recommendedBlocks().isEmpty(), "Builtin CyberGlass should expose block palette data.");
        helper.assertTrue("1.3.0".equals(parsed.metadata().get("version")), "Packaged CyberGlass metadata should advertise ThemeCore 1.3.0.");
        helper.assertTrue("1.3.0".equals(builtin.metadata().get("version")), "Builtin CyberGlass metadata should advertise ThemeCore 1.3.0.");
        helper.assertTrue("echothemecore:echo_platform".equals(ThemeCoreConfig.string(ThemeCoreConfig.DEFAULT_THEME)),
            "ThemeCore default theme config should default to ECHO Platform.");
        helper.assertTrue("echothemecore:echo_platform".equals(ThemeCoreConfig.string(ThemeCoreConfig.FALLBACK_THEME)),
            "ThemeCore fallback theme config should default to ECHO Platform.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_MAIN_MENU),
            "ThemeCore main menu theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_TERMINAL),
            "ThemeCore Terminal theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_INDEX),
            "ThemeCore Index theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_HOLOMAP),
            "ThemeCore HoloMap theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_LENS),
            "ThemeCore Lens theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_RENDERCORE),
            "ThemeCore RenderCore theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_SOUNDCORE),
            "ThemeCore SoundCore theming should default on.");
        helper.assertTrue(ThemeCoreConfig.vanillaUiEnabled(), "ThemeCore vanilla UI theming should default on.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.THEME_MODS_SCREEN),
            "ThemeCore NeoForge Mods screen theming should default on.");
        helper.assertFalse(ThemeCoreConfig.vanillaSafeMode(), "ThemeCore vanilla UI safe mode should default off.");
        helper.assertFalse(ThemeCoreConfig.disableNoise(), "ThemeCore CyberGlass noise should default on.");
        helper.assertTrue(ThemeCoreConfig.disableUnknownScreens(), "ThemeCore unknown-screen protection should stay enabled.");
        helper.assertTrue(ThemeCoreConfig.bool(ThemeCoreConfig.DO_NOT_MODIFY_SLOT_POSITIONS),
            "ThemeCore slot-position protection should stay enabled.");
        helper.assertTrue(ThemeCoreConfig.preserveTextContrast(),
            "ThemeCore text contrast preservation should stay enabled.");
        for (EchoThemeTextureKey key : new EchoThemeTextureKey[] {
            EchoThemeTextureKey.HOLOMAP_MARKER_NEXUS,
            EchoThemeTextureKey.HOLOMAP_MARKER_RECLAIMED,
            EchoThemeTextureKey.LENS_PROGRESS_ARC,
            EchoThemeTextureKey.LENS_NOISE_OVERLAY,
            EchoThemeTextureKey.VANILLA_TOOLTIP_PANEL,
            EchoThemeTextureKey.VANILLA_TOAST_ACCENT,
            EchoThemeTextureKey.VANILLA_BOSS_BAR_ACCENT,
            EchoThemeTextureKey.VANILLA_PAUSE_PANEL,
            EchoThemeTextureKey.VANILLA_INVENTORY_FRAME,
            EchoThemeTextureKey.VANILLA_CREATIVE_FRAME,
            EchoThemeTextureKey.VANILLA_WIDGET_OUTLINE,
            EchoThemeTextureKey.RENDERCORE_DISTORTION_OVERLAY,
            EchoThemeTextureKey.INDEX_PANEL,
            EchoThemeTextureKey.INDEX_PANEL_WIDE,
            EchoThemeTextureKey.INDEX_PANEL_ACTIVE,
            EchoThemeTextureKey.INDEX_BUTTON,
            EchoThemeTextureKey.INDEX_BUTTON_HOVER,
            EchoThemeTextureKey.INDEX_CARD,
            EchoThemeTextureKey.INDEX_CARD_SELECTED,
            EchoThemeTextureKey.INDEX_STATUS_CHIP,
            EchoThemeTextureKey.INDEX_PROGRESS_BAR,
            EchoThemeTextureKey.INDEX_SCROLLBAR,
            EchoThemeTextureKey.INDEX_ICON,
            EchoThemeTextureKey.SCREENCORE_SURFACE_BASE,
            EchoThemeTextureKey.SCREENCORE_SURFACE_RAISED,
            EchoThemeTextureKey.SCREENCORE_SURFACE_FLOATING,
            EchoThemeTextureKey.SCREENCORE_BUTTON,
            EchoThemeTextureKey.SCREENCORE_BUTTON_HOVER,
            EchoThemeTextureKey.SCREENCORE_STATUS_CHIP,
            EchoThemeTextureKey.SCREENCORE_PROGRESS_BAR,
            EchoThemeTextureKey.SCREENCORE_FOCUS_RING,
            EchoThemeTextureKey.SCREENCORE_CORNER_CUTS,
            EchoThemeTextureKey.SCREENCORE_EDGE_RAILS,
            EchoThemeTextureKey.SCREENCORE_PANEL_SHEEN,
            EchoThemeTextureKey.SCREENCORE_MICRO_TICKS
        }) {
            Identifier texture = parsed.moduleTexture(key)
                .orElseThrow(() -> new AssertionError("CyberGlass should expose module texture " + key));
            assertPackagedTexture(helper, texture);
            helper.assertTrue(builtin.moduleTexture(key).isPresent(), "Builtin CyberGlass should expose " + key);
        }
        List<String> tokens = ThemeCoreService.INSTANCE.knownTokens();
        helper.assertTrue(tokens.contains("index.panel"), "ThemeCore should advertise Index panel token.");
        helper.assertTrue(tokens.contains("index.button_hover"), "ThemeCore should advertise Index button hover token.");
        helper.assertTrue(tokens.contains("index.icon"), "ThemeCore should advertise Index icon token.");
        helper.assertTrue(tokens.contains("index.warning"), "ThemeCore should advertise Index warning color token.");
        helper.assertTrue(tokens.contains("screencore.surface.raised"), "ThemeCore should advertise ScreenCore raised surface token.");
        helper.assertTrue(tokens.contains("screencore.button.hover"), "ThemeCore should advertise ScreenCore button hover token.");
        helper.assertTrue(tokens.contains("screencore.edge_rails"), "ThemeCore should advertise ScreenCore edge rails token.");
        assertAllReferencedCyberGlassTextures(helper);
        ThemeRegistry.replaceLoaded(Map.of(parsed.id(), parsed));
        helper.assertTrue(ThemeCoreService.INSTANCE.resolveTexture("screencore.surface.raised").isPresent(),
            "ThemeCore service should resolve ScreenCore texture tokens.");
        helper.assertTrue(ThemeCoreService.INSTANCE.resolveTexture("screencore.edge_rails").isPresent(),
            "ThemeCore service should resolve ScreenCore layered texture tokens.");
        helper.assertTrue(ThemeCoreService.INSTANCE.resolveTexture("ui.pause_panel").isPresent(),
            "ThemeCore service should resolve the CyberGlass pause panel token.");
        helper.assertTrue(ThemeRegistry.getCurrentTheme().id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "ThemeCore should remain on ECHO Platform after datapack reload.");
        helper.assertTrue(ThemeRegistry.fallbackTheme().id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "ThemeCore fallback should remain ECHO Platform after datapack reload.");
        assertTerminalBridgeIfLoaded(helper);
        helper.succeed();
    }

    private static void renderPresetRegistry(GameTestHelper helper) {
        EchoThemeRenderPreset parsed = RenderPresetReloadListener.parsePresetForTests(id("cyberglass_terminal_boot"),
            JsonParser.parseString("""
                {
                  "id": "echothemecore:cyberglass_terminal_boot",
                  "theme": "echothemecore:cyberglass",
                  "type": "terminal_boot",
                  "colors": {
                    "primary": "theme:primary",
                    "glow": "theme:glow"
                  },
                  "effects": [
                    {"type": "hologram_sweep", "duration_ticks": 24, "strength": "theme:hologram_pulse_strength"}
                  ],
                  "metadata": {
                    "module": "terminal"
                  }
                }
                """).getAsJsonObject());
        RenderPresetRegistry.replaceLoaded(Map.of(parsed.id(), parsed));
        helper.assertTrue(RenderPresetRegistry.find(parsed.id()).isPresent(), "Render preset registry should find parsed presets by id.");
        helper.assertTrue(RenderPresetRegistry.forTheme(ThemeRegistry.CYBERGLASS_ID).size() == 1,
            "Render preset registry should list presets by theme id.");
        helper.assertTrue(RenderPresetRegistry.find(id("missing")).isEmpty(), "Missing render presets should return empty.");
        helper.assertTrue("terminal_boot".equals(parsed.type()), "Render preset type should parse.");
        helper.assertTrue(parsed.effects().size() == 1, "Render preset effects should parse.");
        RenderPresetRegistry.replaceLoaded(Map.of());
        helper.succeed();
    }

    private static void uiTextureDimensions(GameTestHelper helper) {
        for (String theme : List.of("cyberglass", "nexus", "cyberconsole", "ashfall", "magic")) {
            String base = "assets/echothemecore/textures/gui/themes/" + theme + "/";
            assertTextureSize(helper, base + "background.png", 512, 512);
            assertTextureSize(helper, base + "theme_desktop_wallpaper.png", 1920, 1080);
            assertTextureSize(helper, base + "theme_mobile_wallpaper.png", 1080, 1920);
            assertTextureSize(helper, base + "theme_banner.png", 1600, 600);
            assertTextureSize(helper, base + "theme_feature_sheet.png", 1600, 1200);
            assertTextureSize(helper, base + "theme_overview_card.png", 512, 512);
            assertTextureSize(helper, base + "icons/icon_index.png", 128, 128);
            assertTextureSize(helper, base + "icons/icon_terminal.png", 128, 128);
        }
        assertCyberGlassTooltipSprites(helper);
        helper.succeed();
    }

    private static void clientThemeCycleContract(GameTestHelper helper) {
        Map<Identifier, EchoTheme> loaded = Map.of(
            ThemeRegistry.ECHO_PLATFORM_ID, parsePackagedTheme("echo_platform"),
            ThemeRegistry.CYBERGLASS_ID, parsePackagedTheme("cyberglass"),
            ThemeRegistry.PRIME_ID, parsePackagedTheme("prime"),
            ThemeRegistry.CYBERCONSOLE_ID, parsePackagedTheme("cyberconsole"),
            ThemeRegistry.ASHFALL_ID, parsePackagedTheme("ashfall"),
            ThemeRegistry.MAGIC_ID, parsePackagedTheme("magic"),
            ThemeRegistry.NEXUS_ID, parsePackagedTheme("nexus")
        );
        ThemeRegistry.replaceLoaded(loaded);
        List<Identifier> ids = ThemeRegistry.listPublicThemes().stream().map(EchoTheme::id).toList();
        helper.assertTrue(ids.equals(List.of(
                ThemeRegistry.ECHO_PLATFORM_ID,
                ThemeRegistry.CYBERGLASS_ID,
                ThemeRegistry.PRIME_ID,
                ThemeRegistry.CYBERCONSOLE_ID,
                ThemeRegistry.ASHFALL_ID,
                ThemeRegistry.MAGIC_ID,
                ThemeRegistry.NEXUS_ID)),
            "Public client themes should follow stable cycle_order.");
        helper.assertTrue(ThemeRegistry.resolveAlias(ThemeRegistry.TECH_CONSOLE_ID).equals(ThemeRegistry.CYBERCONSOLE_ID),
            "tech_console should alias to cyberconsole.");
        helper.assertTrue(ThemeRegistry.resolveAlias(ThemeRegistry.MAGIC_GRIMOIRE_ID).equals(ThemeRegistry.MAGIC_ID),
            "magic_grimoire should alias to magic.");
        helper.assertTrue(ThemeRegistry.resolveAlias(ThemeRegistry.LEGACY_LOADER_PLATFORM_ID).equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "legacy_loader_platform should alias to the unified ECHO Platform theme.");
        helper.assertTrue(ThemeRegistry.nextPublicTheme(ThemeRegistry.ECHO_PLATFORM_ID, 1).id().equals(ThemeRegistry.CYBERGLASS_ID),
            "Next theme after ECHO Platform should be CyberGlass.");
        helper.assertTrue(ThemeRegistry.nextPublicTheme(ThemeRegistry.ECHO_PLATFORM_ID, -1).id().equals(ThemeRegistry.NEXUS_ID),
            "Previous theme before ECHO Platform should wrap to Nexus.");
        helper.assertTrue(ThemeRegistry.get(id("missing")).id().equals(ThemeRegistry.ECHO_PLATFORM_ID),
            "Invalid theme ids should fall back to ECHO Platform.");
        helper.assertTrue("echothemecore:echo_platform".equals(ThemeCoreConfig.string(ThemeCoreConfig.LOCAL_CLIENT_THEME)),
            "Client-local theme config should default to ECHO Platform.");
        helper.assertTrue(ThemeCoreConfig.fullReplacementEnabled(), "Client theme mode should default to FULL.");
        helper.succeed();
    }

    private static void publicThemeTokenContract(GameTestHelper helper) {
        Map<Identifier, EchoTheme> loaded = Map.of(
            ThemeRegistry.ECHO_PLATFORM_ID, parsePackagedTheme("echo_platform"),
            ThemeRegistry.CYBERGLASS_ID, parsePackagedTheme("cyberglass"),
            ThemeRegistry.PRIME_ID, parsePackagedTheme("prime"),
            ThemeRegistry.CYBERCONSOLE_ID, parsePackagedTheme("cyberconsole"),
            ThemeRegistry.ASHFALL_ID, parsePackagedTheme("ashfall"),
            ThemeRegistry.MAGIC_ID, parsePackagedTheme("magic"),
            ThemeRegistry.NEXUS_ID, parsePackagedTheme("nexus")
        );
        ThemeRegistry.replaceLoaded(loaded);
        for (EchoTheme theme : ThemeRegistry.listPublicThemes()) {
            for (EchoThemeTextureKey key : requiredUniversalTextureKeys()) {
                Identifier texture = theme.moduleTexture(key)
                    .orElseThrow(() -> new AssertionError(theme.id() + " should expose " + key));
                assertPackagedTexture(helper, texture);
            }
            assertAllReferencedThemeTextures(helper, packagedThemeJson(theme.id().getPath()), theme.displayName());
        }
        for (String token : List.of(
                "loading.background",
                "loading.panel",
                "loading.progress_bar",
                "menu.main_backplate",
                "menu.pause_panel",
                "hud.hotbar_frame",
                "hud.selected_slot",
                "hud.crosshair_accent",
                "item_icon.frame",
                "item_icon.rarity_ring",
                "item_icon.mission_marker",
                "screencore.surface.base",
                "screencore.button.hover",
                "rendercore.glow_overlay",
                "rendercore.multiblock_energy",
                "terminal.icon",
                "index.panel",
                "holomap.grid",
                "lens.scan_ring")) {
            helper.assertTrue(ThemeCoreService.INSTANCE.resolveTexture(token).isPresent(),
                "ThemeCore should resolve universal token " + token);
        }
        helper.succeed();
    }

    private static void mixinClientOnlyContract(GameTestHelper helper) {
        try {
            Path root = workspaceRoot();
            String mixins = Files.readString(root.resolve(
                "addons/echothemecore/src/main/resources/echothemecore.mixins.json"));
            String toml = Files.readString(root.resolve(
                "addons/echothemecore/src/main/templates/META-INF/neoforge.mods.toml"));
            helper.assertTrue(mixins.contains("\"client\""), "ThemeCore mixin config should declare client mixins only.");
            helper.assertTrue(mixins.contains("ThemeCoreButtonMixin"), "ThemeCore should register button replacement mixin.");
            helper.assertTrue(mixins.contains("ThemeCoreLoadingOverlayMixin"), "ThemeCore should register loading overlay replacement mixin.");
            helper.assertTrue(mixins.contains("ThemeCoreScreenMixin"), "ThemeCore should register guarded screen replacement mixin.");
            helper.assertTrue(mixins.contains("ThemeCoreContainerScreenMixin"),
                "ThemeCore should register guarded container replacement mixin.");
            helper.assertFalse(mixins.contains("\"mixins\""), "ThemeCore mixin config should not declare common mixins.");
            helper.assertTrue(toml.contains("config=\"${mod_id}.mixins.json\""),
                "ThemeCore mod metadata should register the mixin config.");
            String screenMixin = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/mixin/client/ThemeCoreScreenMixin.java"));
            String containerMixin = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/mixin/client/ThemeCoreContainerScreenMixin.java"));
            String loadingOverlayMixin = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/mixin/client/ThemeCoreLoadingOverlayMixin.java"));
            helper.assertTrue(screenMixin.contains("ThemeCoreConfig.safeFallbackEnabled()")
                    && containerMixin.contains("ThemeCoreConfig.safeFallbackEnabled()"),
                "Full replacement mixins should preserve the safe fallback guard.");
            helper.assertFalse(screenMixin.contains("ci.cancel()") || containerMixin.contains("ci.cancel()"),
                "Screen and container replacement hooks should draw chrome without cancelling input or vanilla layout.");
            helper.assertFalse(loadingOverlayMixin.contains("extractRenderStateWithTooltipAndSubtitles"),
                "Loading overlay replacement must not re-extract the current screen because that can request a second blur in the same frame.");
            helper.assertFalse(loadingOverlayMixin.contains("!this.reload.isDone()"),
                "Loading overlay replacement should own the whole reload overlay path instead of falling back to vanilla mid-reload.");
            helper.assertTrue(loadingOverlayMixin.contains("extractDeferredSubtitlesOnly"),
                "Loading overlay replacement should keep subtitles without re-running screen background extraction.");
        } catch (IOException exception) {
            helper.fail("Could not inspect ThemeCore mixin metadata: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void screenCoreDynamicPickerContract(GameTestHelper helper) {
        try {
            Path root = workspaceRoot();
            String picker = Files.readString(root.resolve(
                "addons/echothemecore/src/main/resources/assets/echothemecore/eui/pages/client_theme_picker.eui.xml"));
            String bridge = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/client/ThemeCoreScreenCoreBridge.java"));
            helper.assertTrue(picker.contains("<repeat source=\"themeCore.publicThemes\" item=\"theme\""),
                "Theme picker should render every public theme from the ScreenCore data provider.");
            helper.assertTrue(picker.contains("{theme.icon}") && picker.contains("{theme.loadingBackground}")
                    && picker.contains("{theme.hudAccent}") && picker.contains("{theme.itemFrame}"),
                "Theme picker should expose icon, loading, HUD, and item-icon preview bindings.");
            helper.assertFalse(picker.contains("action-value=\"echothemecore:cyberglass\""),
                "Theme picker should not hardcode built-in theme rows.");
            helper.assertTrue(bridge.contains("registerDataProvider\", String.class")
                    && bridge.contains("\"themeCore\"")
                    && bridge.contains("ClientThemeState.listPublicThemes()"),
                "ThemeCore should register a dynamic ScreenCore data provider backed by ClientThemeState.");
            helper.assertTrue(bridge.contains("echothemecore.set_client_theme")
                    && bridge.contains("echothemecore.cycle_client_theme")
                    && bridge.contains("echothemecore.reset_client_theme"),
                "Theme picker actions should apply, cycle, and reset local client themes.");
        } catch (IOException exception) {
            helper.fail("Could not inspect dynamic ThemeCore picker resources: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void clientLocalOnlyContract(GameTestHelper helper) {
        try {
            Path root = workspaceRoot();
            String state = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/client/ClientThemeState.java"));
            String cache = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/client/ClientThemeCache.java"));
            String packets = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/network/ThemeCoreClientPacketHooks.java"));
            String api = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/api/EchoThemeApi.java"));
            String terminal = Files.readString(root.resolve(
                "addons/echothemecore/src/main/java/com/knoxhack/echothemecore/integration/ThemeCoreTerminalBridge.java"));
            helper.assertTrue(state.contains("Client-local visual theme authority")
                    && state.contains("ignoreServerTheme"),
                "ClientThemeState should document and expose local-only server-ignore behavior.");
            helper.assertTrue(state.contains("syncTerminalTheme()"),
                "ClientThemeState should locally sync optional Terminal visuals when the client theme changes.");
            helper.assertTrue(cache.contains("ClientThemeState.ignoreServerTheme(themeId);"),
                "Legacy server packet application should reconcile only and not apply visual themes.");
            helper.assertTrue(packets.contains("applyServerTheme") && !packets.contains("setClientTheme"),
                "Client packet hooks must not call the local client theme mutation API.");
            helper.assertTrue(api.contains("getClientTheme") && api.contains("setClientTheme")
                    && api.contains("cycleClientTheme") && api.contains("listPublicClientThemes"),
                "EchoThemeApi should expose the supported client-local theme APIs.");
            helper.assertTrue(terminal.contains("ThemeRegistry.listPublicThemes()")
                    && terminal.contains("syncClientTheme")
                    && terminal.contains("TerminalClientOptions"),
                "Terminal bridge should register public ThemeCore themes and follow the local client theme.");
        } catch (IOException exception) {
            helper.fail("Could not inspect client-local ThemeCore source: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void assertCyberGlassTooltipSprites(GameTestHelper helper) {
        String base = "assets/echothemecore/textures/gui/sprites/tooltip/";
        assertTextureSize(helper, base + "cyberglass_background.png", 32, 32);
        assertTextureSize(helper, base + "cyberglass_frame.png", 32, 32);
        assertTooltipBackgroundMetadata(helper, base + "cyberglass_background.png.mcmeta");
    }

    private static void assertTerminalBridgeIfLoaded(GameTestHelper helper) {
        if (!ThemeCoreTerminalBridge.isTerminalLoaded()) {
            return;
        }
        ThemeCoreTerminalBridge.registerIfAvailable();
        try {
            Class<?> registry = Class.forName("com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry");
            boolean contains = ((Boolean) registry.getMethod("contains", Identifier.class)
                .invoke(null, ThemeRegistry.ECHO_PLATFORM_ID)).booleanValue();
            Object defaultId = registry.getMethod("defaultThemeId").invoke(null);
            Object theme = registry.getMethod("byId", Identifier.class).invoke(null, ThemeRegistry.ECHO_PLATFORM_ID);
            Object tokens = theme.getClass().getMethod("tokens").invoke(theme);
            Object assets = tokens.getClass().getMethod("assets").invoke(tokens);
            Object icons = theme.getClass().getMethod("icons").invoke(theme);
            helper.assertTrue(contains, "Terminal should register the ECHO Platform ThemeCore theme when loaded.");
            helper.assertTrue(ThemeRegistry.ECHO_PLATFORM_ID.equals(defaultId),
                "Terminal should use ECHO Platform as the active default when ThemeCore bridge is loaded.");
            helper.assertTrue(assets != null, "ECHO Platform Terminal theme should expose asset tokens.");
            helper.assertTrue(icons != null, "ECHO Platform Terminal theme should expose icons.");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not inspect ECHO Platform Terminal bridge.", exception);
        }
    }

    private static EchoTheme parsePackagedCyberGlass() {
        return ThemeJsonReloadListener.parseThemeForTests(ThemeRegistry.CYBERGLASS_ID, packagedCyberGlassJson());
    }

    private static EchoTheme parsePackagedNexus() {
        return ThemeJsonReloadListener.parseThemeForTests(ThemeRegistry.NEXUS_ID, packagedThemeJson("nexus"));
    }

    private static EchoTheme parsePackagedTheme(String theme) {
        return ThemeJsonReloadListener.parseThemeForTests(id(theme), packagedThemeJson(theme));
    }

    private static JsonObject packagedCyberGlassJson() {
        return packagedThemeJson("cyberglass");
    }

    private static JsonObject packagedThemeJson(String theme) {
        String resource = "data/echothemecore/themes/" + theme + ".json";
        try (InputStreamReader reader = new InputStreamReader(
            ModGameTests.class.getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | NullPointerException exception) {
            throw new AssertionError("Could not read packaged ThemeCore JSON " + theme + ".", exception);
        }
    }

    private static void assertAllReferencedCyberGlassTextures(GameTestHelper helper) {
        assertAllReferencedThemeTextures(helper, packagedCyberGlassJson(), "CyberGlass");
    }

    private static void assertAllReferencedThemeTextures(GameTestHelper helper, JsonObject json, String themeName) {
        List<Identifier> textures = new ArrayList<>();
        collectTextureRefs(json, textures);
        helper.assertTrue(!textures.isEmpty(), themeName + " JSON should reference packaged PNG assets.");
        for (Identifier texture : textures) {
            if (EchoThemeCore.MODID.equals(texture.getNamespace())) {
                assertPackagedTexture(helper, texture);
            }
        }
    }

    private static List<EchoThemeTextureKey> indexAndVanillaContractKeys() {
        return List.of(
            EchoThemeTextureKey.INDEX_PANEL,
            EchoThemeTextureKey.INDEX_PANEL_WIDE,
            EchoThemeTextureKey.INDEX_PANEL_ACTIVE,
            EchoThemeTextureKey.INDEX_BUTTON,
            EchoThemeTextureKey.INDEX_BUTTON_HOVER,
            EchoThemeTextureKey.INDEX_CARD,
            EchoThemeTextureKey.INDEX_CARD_SELECTED,
            EchoThemeTextureKey.INDEX_STATUS_CHIP,
            EchoThemeTextureKey.INDEX_PROGRESS_BAR,
            EchoThemeTextureKey.INDEX_SCROLLBAR,
            EchoThemeTextureKey.INDEX_ICON,
            EchoThemeTextureKey.VANILLA_CONTAINER_FRAME,
            EchoThemeTextureKey.VANILLA_INVENTORY_FRAME,
            EchoThemeTextureKey.VANILLA_CREATIVE_FRAME,
            EchoThemeTextureKey.VANILLA_TITLE_BACKPLATE,
            EchoThemeTextureKey.VANILLA_PAUSE_PANEL,
            EchoThemeTextureKey.VANILLA_SELECTED_SLOT,
            EchoThemeTextureKey.VANILLA_TOOLTIP_PANEL,
            EchoThemeTextureKey.VANILLA_TOAST_ACCENT,
            EchoThemeTextureKey.VANILLA_BOSS_BAR_ACCENT,
            EchoThemeTextureKey.VANILLA_WIDGET_OUTLINE
        );
    }

    private static List<EchoThemeTextureKey> requiredUniversalTextureKeys() {
        List<EchoThemeTextureKey> keys = new ArrayList<>(indexAndVanillaContractKeys());
        keys.addAll(List.of(
            EchoThemeTextureKey.TERMINAL_PANEL,
            EchoThemeTextureKey.TERMINAL_TAB,
            EchoThemeTextureKey.TERMINAL_TAB_ACTIVE,
            EchoThemeTextureKey.TERMINAL_MISSION_CARD,
            EchoThemeTextureKey.TERMINAL_STATUS_CHIP,
            EchoThemeTextureKey.TERMINAL_BUTTON,
            EchoThemeTextureKey.TERMINAL_ICON,
            EchoThemeTextureKey.HOLOMAP_GRID,
            EchoThemeTextureKey.HOLOMAP_PANEL,
            EchoThemeTextureKey.HOLOMAP_ROUTE,
            EchoThemeTextureKey.HOLOMAP_MARKER_SIGNAL,
            EchoThemeTextureKey.HOLOMAP_MARKER_HAZARD,
            EchoThemeTextureKey.HOLOMAP_MARKER_MISSION,
            EchoThemeTextureKey.HOLOMAP_MARKER_NEXUS,
            EchoThemeTextureKey.HOLOMAP_MARKER_RECLAIMED,
            EchoThemeTextureKey.HOLOMAP_SELECTED_RING,
            EchoThemeTextureKey.LENS_SCAN_RING,
            EchoThemeTextureKey.LENS_TARGET_BOX,
            EchoThemeTextureKey.LENS_WEAK_POINT,
            EchoThemeTextureKey.LENS_WARNING,
            EchoThemeTextureKey.LENS_ANOMALY_REVEAL,
            EchoThemeTextureKey.LENS_COMPLETION_PULSE,
            EchoThemeTextureKey.LENS_PROGRESS_ARC,
            EchoThemeTextureKey.LENS_NOISE_OVERLAY,
            EchoThemeTextureKey.RENDERCORE_GLOW_OVERLAY,
            EchoThemeTextureKey.RENDERCORE_DISTORTION_OVERLAY,
            EchoThemeTextureKey.RENDERCORE_ENTITY_HIGHLIGHT,
            EchoThemeTextureKey.RENDERCORE_MULTIBLOCK_ENERGY,
            EchoThemeTextureKey.SCREENCORE_SURFACE_BASE,
            EchoThemeTextureKey.SCREENCORE_SURFACE_RAISED,
            EchoThemeTextureKey.SCREENCORE_SURFACE_FLOATING,
            EchoThemeTextureKey.SCREENCORE_BUTTON,
            EchoThemeTextureKey.SCREENCORE_BUTTON_HOVER,
            EchoThemeTextureKey.SCREENCORE_STATUS_CHIP,
            EchoThemeTextureKey.SCREENCORE_PROGRESS_BAR,
            EchoThemeTextureKey.SCREENCORE_FOCUS_RING,
            EchoThemeTextureKey.SCREENCORE_CORNER_CUTS,
            EchoThemeTextureKey.SCREENCORE_EDGE_RAILS,
            EchoThemeTextureKey.SCREENCORE_PANEL_SHEEN,
            EchoThemeTextureKey.SCREENCORE_MICRO_TICKS,
            EchoThemeTextureKey.LOADING_BACKGROUND,
            EchoThemeTextureKey.LOADING_PANEL,
            EchoThemeTextureKey.LOADING_PROGRESS_BAR,
            EchoThemeTextureKey.LOADING_SPINNER,
            EchoThemeTextureKey.LOADING_LOGO_MARK,
            EchoThemeTextureKey.MENU_MAIN_BACKPLATE,
            EchoThemeTextureKey.MENU_PAUSE_PANEL,
            EchoThemeTextureKey.MENU_OPTIONS_PANEL,
            EchoThemeTextureKey.MENU_WORLD_ROW,
            EchoThemeTextureKey.MENU_MODS_PANEL,
            EchoThemeTextureKey.HUD_HOTBAR_FRAME,
            EchoThemeTextureKey.HUD_SELECTED_SLOT,
            EchoThemeTextureKey.HUD_CROSSHAIR_ACCENT,
            EchoThemeTextureKey.HUD_BOSS_BAR,
            EchoThemeTextureKey.HUD_CHAT_PANEL,
            EchoThemeTextureKey.HUD_NOTIFICATION_CHIP,
            EchoThemeTextureKey.ITEM_ICON_FRAME,
            EchoThemeTextureKey.ITEM_ICON_RARITY_RING,
            EchoThemeTextureKey.ITEM_ICON_BADGE,
            EchoThemeTextureKey.ITEM_ICON_LOCK_OVERLAY,
            EchoThemeTextureKey.ITEM_ICON_MISSION_MARKER
        ));
        return List.copyOf(keys);
    }

    private static void collectTextureRefs(JsonElement element, List<Identifier> textures) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectTextureRefs(entry.getValue(), textures);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectTextureRefs(child, textures);
            }
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.endsWith(".png")) {
                Identifier parsed = Identifier.tryParse(value);
                if (parsed != null) {
                    textures.add(parsed);
                }
            }
        }
    }

    private static void assertPackagedTexture(GameTestHelper helper, Identifier texture) {
        String path = "assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(stream != null, "Expected packaged texture " + path);
            byte[] signature = stream.readNBytes(8);
            helper.assertTrue(signature.length == 8
                    && signature[0] == (byte) 0x89
                    && signature[1] == 0x50
                    && signature[2] == 0x4E
                    && signature[3] == 0x47
                    && signature[4] == 0x0D
                    && signature[5] == 0x0A
                    && signature[6] == 0x1A
                    && signature[7] == 0x0A,
                "Expected valid PNG signature for " + path);
        } catch (IOException exception) {
            helper.fail("Could not read packaged texture " + path + ": " + exception.getMessage());
        }
    }

    private static void assertTextureSize(GameTestHelper helper, String path, int expectedWidth, int expectedHeight) {
        try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(stream != null, "Expected packaged texture " + path);
            byte[] header = stream.readNBytes(24);
            helper.assertTrue(header.length == 24
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47,
                "Expected valid PNG header for " + path);
            int width = readPngInt(header, 16);
            int height = readPngInt(header, 20);
            helper.assertTrue(width == expectedWidth && height == expectedHeight,
                "Expected " + path + " to be " + expectedWidth + "x" + expectedHeight
                    + " but was " + width + "x" + height);
        } catch (IOException exception) {
            helper.fail("Could not read packaged texture " + path + ": " + exception.getMessage());
        }
    }

    private static void assertTooltipBackgroundMetadata(GameTestHelper helper, String path) {
        try (InputStreamReader reader = new InputStreamReader(
                ModGameTests.class.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8)) {
            JsonObject scaling = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonObject("gui")
                    .getAsJsonObject("scaling");
            helper.assertTrue("nine_slice".equals(scaling.get("type").getAsString()),
                "CyberGlass tooltip background should use nine-slice scaling.");
            helper.assertTrue(scaling.get("width").getAsInt() == 32 && scaling.get("height").getAsInt() == 32,
                "CyberGlass tooltip background metadata should match the 32x32 sprite.");
            helper.assertTrue(scaling.get("border").getAsInt() == 8,
                "CyberGlass tooltip background should keep an 8px nine-slice border.");
            helper.assertTrue(scaling.has("stretch_inner") && scaling.get("stretch_inner").getAsBoolean(),
                "CyberGlass tooltip background should stretch its center instead of tiling.");
        } catch (IOException | NullPointerException | IllegalStateException exception) {
            helper.fail("Could not read CyberGlass tooltip background metadata " + path + ": " + exception.getMessage());
        }
    }

    private static int readPngInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
            | ((bytes[offset + 1] & 0xFF) << 16)
            | ((bytes[offset + 2] & 0xFF) << 8)
            | (bytes[offset + 3] & 0xFF);
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("themecore_" + testName));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1,
            false, 16);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoThemeCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
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
}
