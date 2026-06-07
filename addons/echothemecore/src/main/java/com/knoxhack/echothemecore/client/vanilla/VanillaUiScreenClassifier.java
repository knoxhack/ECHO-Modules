package com.knoxhack.echothemecore.client.vanilla;

import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class VanillaUiScreenClassifier {
    private VanillaUiScreenClassifier() {
    }

    public static VanillaUiSurface classify(Screen screen) {
        if (screen == null) {
            return VanillaUiSurface.UNKNOWN;
        }
        String name = screen.getClass().getName();
        return classifyClassName(name, screen instanceof AbstractContainerScreen<?>);
    }

    public static VanillaUiSurface classifyForTests(String className, boolean abstractContainerScreen) {
        if (className == null || className.isBlank()) {
            return VanillaUiSurface.UNKNOWN;
        }
        return classifyClassName(className, abstractContainerScreen);
    }

    private static VanillaUiSurface classifyClassName(String name, boolean abstractContainerScreen) {
        if (name.endsWith(".TitleScreen")) {
            return VanillaUiSurface.MAIN_MENU;
        }
        if (name.endsWith(".PauseScreen")) {
            return VanillaUiSurface.PAUSE_MENU;
        }
        if (containsAny(name, "OptionsScreen", "VideoSettingsScreen", "ControlsScreen", "AccessibilityOptionsScreen", "OnlineOptionsScreen", "LanguageSelectScreen", "SoundOptionsScreen", "SkinCustomizationScreen", "KeyBindsScreen", "MouseSettingsScreen")) {
            return VanillaUiSurface.OPTIONS_MENU;
        }
        if (containsAny(name, "SelectWorldScreen", "CreateWorldScreen", "EditWorldScreen")) {
            return VanillaUiSurface.WORLD_SELECT;
        }
        if (containsAny(name, "JoinMultiplayerScreen", "ServerSelectionList", "DirectJoinServerScreen", "EditServerScreen")) {
            return VanillaUiSurface.MULTIPLAYER;
        }
        if (containsAny(name, "PackSelectionScreen")) {
            return VanillaUiSurface.RESOURCE_PACKS;
        }
        if (containsAny(name, "SocialInteractionsScreen")) {
            return VanillaUiSurface.SOCIAL;
        }
        if (containsAny(name, "StatsScreen")) {
            return VanillaUiSurface.STATS;
        }
        if (isLoaderModsScreen(name)) {
            return VanillaUiSurface.LOADER_MODS;
        }
        if (containsAny(name, "LevelLoadingScreen", "ReceivingLevelScreen", "ProgressScreen")) {
            return VanillaUiSurface.LOADING;
        }
        if (name.endsWith(".InventoryScreen")) {
            return VanillaUiSurface.INVENTORY;
        }
        if (name.endsWith(".CreativeModeInventoryScreen")) {
            return VanillaUiSurface.CREATIVE_INVENTORY;
        }
        if (containsAny(name, "FurnaceScreen", "BlastFurnaceScreen", "SmokerScreen")) {
            return VanillaUiSurface.FURNACE;
        }
        if (containsAny(name, "CraftingScreen")) {
            return VanillaUiSurface.CRAFTING;
        }
        if (containsAny(name, "AnvilScreen")) {
            return VanillaUiSurface.ANVIL;
        }
        if (containsAny(name, "EnchantmentScreen")) {
            return VanillaUiSurface.ENCHANTING;
        }
        if (containsAny(name, "GrindstoneScreen")) {
            return VanillaUiSurface.GRINDSTONE;
        }
        if (containsAny(name, "SmithingScreen")) {
            return VanillaUiSurface.SMITHING;
        }
        if (containsAny(name, "AdvancementsScreen")) {
            return VanillaUiSurface.ADVANCEMENTS;
        }
        if (containsAny(name, "RecipeBook")) {
            return VanillaUiSurface.RECIPE_BOOK;
        }
        if (isEchoTerminalAppScreen(name)) {
            return VanillaUiSurface.ECHO_SCREEN;
        }
        if (containsAny(name, "ContainerScreen", "ChestScreen", "ShulkerBoxScreen", "DispenserScreen", "HopperScreen", "BrewingStandScreen", "BeaconScreen", "AbstractContainerScreen")) {
            return VanillaUiSurface.CONTAINER;
        }
        if (abstractContainerScreen) {
            return VanillaUiSurface.CONTAINER;
        }
        if (name.startsWith("com.knoxhack.echo") || name.startsWith("com.knoxhack.signalos")) {
            return VanillaUiSurface.ECHO_SCREEN;
        }
        return VanillaUiSurface.UNKNOWN;
    }

    public static boolean enabled(VanillaUiSurface surface) {
        return enabled(null, surface);
    }

    public static boolean enabled(Screen screen, VanillaUiSurface surface) {
        if (!ThemeCoreConfig.vanillaUiEnabled()) {
            return false;
        }
        if (screen != null && surface == VanillaUiSurface.OPTIONS_MENU && inWorldOptionsScreen(screen)) {
            return false;
        }
        return switch (surface) {
            case MAIN_MENU -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_MAIN_MENU) && ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_MAIN_MENU);
            case PAUSE_MENU -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_PAUSE_MENU);
            case OPTIONS_MENU -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_OPTIONS_MENU);
            case WORLD_SELECT, MULTIPLAYER, RESOURCE_PACKS, SOCIAL, STATS -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_WORLD_SELECT);
            case LOADING -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_LOADING_SCREEN);
            case INVENTORY -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_INVENTORY);
            case CREATIVE_INVENTORY -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_CREATIVE_INVENTORY);
            case CONTAINER, FURNACE, CRAFTING, ANVIL, ENCHANTING, GRINDSTONE, SMITHING -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_CONTAINERS);
            case ADVANCEMENTS -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_ADVANCEMENTS);
            case RECIPE_BOOK -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_RECIPE_BOOK);
            case LOADER_MODS -> ThemeCoreConfig.bool(ThemeCoreConfig.THEME_MODS_SCREEN);
            case ECHO_SCREEN -> true;
            case UNKNOWN -> !ThemeCoreConfig.disableUnknownScreens();
        };
    }

    private static boolean inWorldOptionsScreen(Screen screen) {
        if (!isInWorld()) {
            return false;
        }
        String simpleName = screen.getClass().getSimpleName();
        return "OptionsScreen".equals(simpleName) || "VideoSettingsScreen".equals(simpleName);
    }

    private static boolean isInWorld() {
        try {
            return Minecraft.getInstance().level != null;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isEchoTerminalAppScreen(String name) {
        return containsAny(name,
            "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen",
            "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen");
    }

    private static boolean isLoaderModsScreen(String name) {
        return containsAny(name,
            "ModsScreen",
            "ConfigurationScreen",
            "LoadingErrorScreen",
            "ModMismatchDisconnectedScreen");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
