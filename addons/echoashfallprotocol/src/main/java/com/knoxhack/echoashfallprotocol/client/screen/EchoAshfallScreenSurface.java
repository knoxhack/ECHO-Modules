package com.knoxhack.echoashfallprotocol.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Ashfall-owned vanilla menu surfaces. This keeps visual parity decisions in one
 * place while vanilla screens keep their normal widgets and save-flow behavior.
 */
enum EchoAshfallScreenSurface {
    NONE("NONE", "UNKNOWN", "STANDBY", EchoTerminalBackgrounds.Plate.WORLD_ARCHIVE),
    PAUSE("PAUSE MENU", "FIELD SESSION", "SIMULATION PAUSED", EchoTerminalBackgrounds.Plate.WORLD_ARCHIVE),
    WORLD_ARCHIVE("WORLD ARCHIVE", "SAVE INDEX", "ARCHIVE ONLINE", EchoTerminalBackgrounds.Plate.WORLD_ARCHIVE),
    CREATE_WORLD("CREATE SIMULATION", "WORLD SETUP", "PARAMETERS READY", EchoTerminalBackgrounds.Plate.CREATE_SIMULATION),
    MULTIPLAYER("MULTIPLAYER UPLINK", "REMOTE LINK", "LINK STANDBY", EchoTerminalBackgrounds.Plate.MULTIPLAYER_UPLINK),
    OPTIONS("SYSTEM OPTIONS", "CONFIG ROUTE", "CONFIG READY", EchoTerminalBackgrounds.Plate.CREATE_SIMULATION),
    RESOURCE_PACKS("RESOURCE PACKS", "ASSET STACK", "PACK ROUTE READY", EchoTerminalBackgrounds.Plate.CREATE_SIMULATION),
    MODS("MODULE INDEX", "LOADER ROUTE", "MODULES READY", EchoTerminalBackgrounds.Plate.MULTIPLAYER_UPLINK),
    DIALOG("ECHO CONFIRMATION", "DECISION GATE", "AWAITING INPUT", EchoTerminalBackgrounds.Plate.WORLD_ARCHIVE),
    ERROR("ASHFALL WARNING", "RECOVERY ROUTE", "ATTENTION REQUIRED", EchoTerminalBackgrounds.Plate.LOADING_BOOT),
    LOADING("LOADING TERRAIN", "WORLD HANDOFF", "BOOT VECTOR SYNCING", EchoTerminalBackgrounds.Plate.TERRAIN_LOADING),
    MAINTENANCE("WORLD MAINTENANCE", "DATA REPAIR", "BOOT VECTOR SYNCING", EchoTerminalBackgrounds.Plate.LOADING_BOOT);

    static final String WORLD_SELECTION_PACKAGE = "net.minecraft.client.gui.screens.worldselection.";
    static final String MULTIPLAYER_PACKAGE = "net.minecraft.client.gui.screens.multiplayer.";
    static final String DIALOG_PACKAGE = "net.minecraft.client.gui.screens.dialog.";
    static final String OPTIONS_PACKAGE = "net.minecraft.client.gui.screens.options.";
    private static final String PACKS_PACKAGE = "net.minecraft.client.gui.screens.packs.";

    private final String label;
    private final String route;
    private final String status;
    private final EchoTerminalBackgrounds.Plate plate;

    EchoAshfallScreenSurface(String label, String route, String status, EchoTerminalBackgrounds.Plate plate) {
        this.label = label;
        this.route = route;
        this.status = status;
        this.plate = plate;
    }

    String label() {
        return this.label;
    }

    String route() {
        return this.route;
    }

    String status() {
        return this.status;
    }

    EchoTerminalBackgrounds.Plate plate() {
        return this.plate;
    }

    boolean owned() {
        return this != NONE;
    }

    boolean loadingLike() {
        return this == LOADING || this == MAINTENANCE;
    }

    boolean compactCenterPanel() {
        return this == PAUSE || this == DIALOG || this == ERROR || this.loadingLike();
    }

    int statusColor() {
        if (this == ERROR || this == DIALOG || this.loadingLike()) {
            return EchoTerminalStyle.AMBER;
        }
        if (this == PAUSE || this == WORLD_ARCHIVE || this == CREATE_WORLD) {
            return EchoTerminalStyle.GREEN;
        }
        return EchoTerminalStyle.CYAN;
    }

    String footer() {
        return switch (this) {
            case PAUSE -> "FIELD SESSION SHELL ACTIVE. VANILLA PAUSE ACTIONS PRESERVED.";
            case WORLD_ARCHIVE -> "ARCHIVE SHELL ACTIVE. SAVE DATA REMAINS VANILLA-SAFE.";
            case CREATE_WORLD -> "SIMULATION PARAMETERS REMAIN VANILLA-SAFE.";
            case MULTIPLAYER -> "REMOTE LINK SHELL ACTIVE.";
            case OPTIONS -> "CONFIGURATION SHELL ACTIVE.";
            case RESOURCE_PACKS -> "ASSET STACK SHELL ACTIVE.";
            case MODS -> "MODULE INDEX SHELL ACTIVE.";
            case DIALOG -> "CONFIRMATION ROUTE ACTIVE.";
            case ERROR -> "RECOVERY ROUTE ACTIVE.";
            case LOADING, MAINTENANCE -> "ECHO LISTENS WHILE THE WORLD LOADS.";
            case NONE -> "ASHFALL TERMINAL SHELL READY.";
        };
    }

    static EchoAshfallScreenSurface classify(Screen screen) {
        if (screen == null || screen instanceof EchoMainMenuScreen || screen instanceof EchoNativeMainMenuScreen) {
            return NONE;
        }

        String name = screen.getClass().getName();
        if (name.startsWith("com.knoxhack.echoashfallprotocol.client.screen.")) {
            return NONE;
        }
        if (name.endsWith(".TitleScreen")) {
            return NONE;
        }
        if (name.endsWith(".PauseScreen")) {
            return PAUSE;
        }
        if (isTerrainLoading(name)) {
            return LOADING;
        }
        if (isMaintenance(name)) {
            return MAINTENANCE;
        }
        if (isError(name)) {
            return ERROR;
        }
        if (isDialog(name)) {
            return DIALOG;
        }
        if (isCreateWorld(name)) {
            return CREATE_WORLD;
        }
        if (isWorldArchive(name)) {
            return WORLD_ARCHIVE;
        }
        if (isMultiplayer(name)) {
            return MULTIPLAYER;
        }
        if (isResourcePacks(name)) {
            return RESOURCE_PACKS;
        }
        if (isOptions(name)) {
            return OPTIONS;
        }
        if (isMods(name)) {
            return MODS;
        }
        return NONE;
    }

    private static boolean isTerrainLoading(String name) {
        return name.equals("net.minecraft.client.gui.screens.LevelLoadingScreen")
                || name.equals("net.minecraft.client.gui.screens.ReceivingLevelScreen");
    }

    private static boolean isMaintenance(String name) {
        return name.equals("net.minecraft.client.gui.screens.ProgressScreen")
                || name.endsWith(".FileFixerProgressScreen")
                || name.endsWith(".OptimizeWorldScreen");
    }

    private static boolean isError(String name) {
        return name.equals("net.minecraft.client.gui.screens.DisconnectedScreen")
                || name.endsWith(".ModMismatchDisconnectedScreen")
                || name.endsWith(".LoadingErrorScreen");
    }

    private static boolean isDialog(String name) {
        return name.startsWith(DIALOG_PACKAGE)
                || name.equals("net.minecraft.client.gui.screens.ConfirmScreen")
                || name.equals("net.minecraft.client.gui.screens.AlertScreen")
                || name.equals("net.minecraft.client.gui.screens.BackupConfirmScreen");
    }

    private static boolean isCreateWorld(String name) {
        return name.endsWith(".CreateWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.CreateFlatWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.CreateBuffetWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.PresetFlatWorldScreen")
                || name.endsWith(".WorldCreationGameRulesScreen")
                || name.endsWith(".ExperimentsScreen")
                || name.equals("net.minecraft.client.gui.screens.options.WorldOptionsScreen")
                || name.equals("net.minecraft.client.gui.screens.options.InWorldGameRulesScreen");
    }

    private static boolean isWorldArchive(String name) {
        return name.startsWith(WORLD_SELECTION_PACKAGE) || name.contains("EditWorldScreen");
    }

    private static boolean isMultiplayer(String name) {
        return name.startsWith(MULTIPLAYER_PACKAGE)
                || name.equals("net.minecraft.client.gui.screens.DirectJoinServerScreen")
                || name.equals("net.minecraft.client.gui.screens.ManageServerScreen")
                || name.contains("EditServerScreen")
                || name.contains("ServerSelectionList")
                || name.equals("net.minecraft.client.gui.screens.ConnectScreen");
    }

    private static boolean isResourcePacks(String name) {
        return name.startsWith(PACKS_PACKAGE) || name.contains("PackSelectionScreen");
    }

    private static boolean isOptions(String name) {
        return name.startsWith(OPTIONS_PACKAGE)
                || name.equals("net.minecraft.client.gui.screens.LanguageSelectScreen")
                || name.contains("ControlsScreen")
                || name.contains("VideoSettingsScreen")
                || name.contains("AccessibilityOptionsScreen")
                || name.contains("SoundOptionsScreen")
                || name.contains("SkinCustomizationScreen")
                || name.contains("KeyBindsScreen")
                || name.contains("MouseSettingsScreen")
                || name.contains("OnlineOptionsScreen");
    }

    private static boolean isMods(String name) {
        return name.contains("ModsScreen")
                || name.contains("ConfigurationScreen");
    }

    static boolean isInWorld() {
        try {
            return Minecraft.getInstance().level != null;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
