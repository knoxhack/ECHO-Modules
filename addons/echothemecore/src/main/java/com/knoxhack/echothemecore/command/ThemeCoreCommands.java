package com.knoxhack.echothemecore.command;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeRenderPreset;
import com.knoxhack.echothemecore.api.ThemeVisualSettings;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.RenderPresetRegistry;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.knoxhack.echothemecore.integration.ThemeCoreRenderCoreBridge;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class ThemeCoreCommands {
    private ThemeCoreCommands() {
    }

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        CommandBuildContext buildContext,
        Commands.CommandSelection selection) {
        dispatcher.register(
            Commands.literal("echo_theme")
                .then(Commands.literal("current")
                    .executes(context -> current(context.getSource())))
                .then(Commands.literal("list")
                    .executes(context -> list(context.getSource())))
                .then(Commands.literal("set")
                    .requires(ThemeCoreCommands::canMutate)
                    .then(Commands.argument("theme_id", StringArgumentType.string())
                        .executes(context -> set(context.getSource(), StringArgumentType.getString(context, "theme_id")))))
                .then(Commands.literal("player")
                    .then(Commands.literal("set")
                        .requires(ThemeCoreCommands::canMutate)
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("theme_id", StringArgumentType.string())
                                .executes(context -> setPlayer(
                                    context.getSource(),
                                    EntityArgument.getPlayer(context, "player"),
                                    StringArgumentType.getString(context, "theme_id"))))))
                    .then(Commands.literal("clear")
                        .requires(ThemeCoreCommands::canMutate)
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> clearPlayer(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("reset")
                    .requires(ThemeCoreCommands::canMutate)
                    .executes(context -> reset(context.getSource())))
                .then(Commands.literal("reload")
                    .requires(ThemeCoreCommands::canMutate)
                    .executes(context -> reload(context.getSource())))
                .then(Commands.literal("preview")
                    .then(Commands.argument("theme_id", StringArgumentType.string())
                        .executes(context -> preview(context.getSource(), StringArgumentType.getString(context, "theme_id")))))
                .then(Commands.literal("visual")
                    .then(Commands.literal("current")
                        .executes(context -> visualCurrent(context.getSource())))
                    .then(Commands.literal("test")
                        .then(Commands.literal("terminal").executes(context -> visualTest(context.getSource(), "terminal_boot")))
                        .then(Commands.literal("holomap").executes(context -> visualTest(context.getSource(), "holomap_grid")))
                        .then(Commands.literal("lens").executes(context -> visualTest(context.getSource(), "lens_scan")))
                        .then(Commands.literal("particles").executes(context -> visualTest(context.getSource(), "particle_burst"))))
                    .then(Commands.literal("intensity")
                        .requires(ThemeCoreCommands::canMutate)
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 2.0D))
                            .executes(context -> visualIntensity(
                                context.getSource(),
                                DoubleArgumentType.getDouble(context, "value"))))))
                .then(Commands.literal("preset")
                    .then(Commands.literal("list")
                        .executes(context -> presetList(context.getSource())))
                    .then(Commands.literal("preview")
                        .then(Commands.argument("preset_id", StringArgumentType.string())
                            .executes(context -> presetPreview(
                                context.getSource(),
                                StringArgumentType.getString(context, "preset_id"))))))
                .then(Commands.literal("vanilla")
                    .then(Commands.literal("current")
                        .executes(context -> vanillaCurrent(context.getSource())))));
    }

    private static int current(CommandSourceStack source) {
        EchoTheme global = ThemeRegistry.getCurrentTheme();
        tell(source, "Global theme: " + global.id() + " (" + global.displayName() + ")", ChatFormatting.AQUA);
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            EchoTheme playerTheme = ThemeRegistry.getThemeFor(player);
            tell(source, "Your effective theme: " + playerTheme.id() + " (" + playerTheme.displayName() + ")", ChatFormatting.GRAY);
        }
        tell(source, "Fallback theme: " + ThemeRegistry.fallbackTheme().id(), ChatFormatting.DARK_AQUA);
        tell(source, "Server sync=" + ThemeCoreConfig.syncServerTheme()
            + ", vanilla_ui=" + ThemeCoreConfig.vanillaUiEnabled()
            + ", render_presets=" + RenderPresetRegistry.listPresets().size(), ChatFormatting.GRAY);
        tell(source, "Optional integrations: terminal=" + loaded("echoterminal")
            + ", signalos=" + loaded("signalos")
            + ", holomap=" + loaded("echoholomap")
            + ", lens=" + loaded("echolens")
            + ", rendercore=" + loaded("echorendercore")
            + ", runtimeguard=" + loaded("echoruntimeguard")
            + ", soundcore=" + loaded("echosoundcore")
            + ", blockworks=" + loaded("echoblockworks"), ChatFormatting.DARK_AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandSourceStack source) {
        String themes = ThemeRegistry.listThemes().stream()
            .map(theme -> theme.id() + " (" + theme.displayName() + ")")
            .collect(Collectors.joining(", "));
        tell(source, "Loaded themes: " + themes, ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(CommandSourceStack source, String rawId) {
        Identifier id = ThemeRegistry.parseThemeId(rawId);
        boolean exact = ThemeRegistry.setGlobalTheme(id);
        EchoTheme selected = ThemeRegistry.getCurrentTheme();
        tell(source, (exact ? "Set global theme to " : "Theme missing; using fallback ") + selected.id(), exact ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        return exact ? Command.SINGLE_SUCCESS : 0;
    }

    private static int setPlayer(CommandSourceStack source, ServerPlayer player, String rawId) {
        if (!ThemeCoreConfig.allowPlayerThemeOverride()) {
            tell(source, "Player theme overrides are disabled in ThemeCore config.", ChatFormatting.RED);
            return 0;
        }
        Identifier id = ThemeRegistry.parseThemeId(rawId);
        if (ThemeRegistry.find(id).isEmpty()) {
            tell(source, "Theme " + id + " is not loaded. Player theme was not changed.", ChatFormatting.RED);
            return 0;
        }
        ThemeRegistry.setPlayerTheme(player.getUUID(), id);
        tell(source, "Set " + player.getScoreboardName() + " theme to " + id, ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearPlayer(CommandSourceStack source, ServerPlayer player) {
        ThemeRegistry.clearPlayerTheme(player.getUUID());
        tell(source, "Cleared " + player.getScoreboardName() + " theme override; effective theme is "
            + ThemeRegistry.getThemeFor(player).id(), ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandSourceStack source) {
        ThemeRegistry.reset();
        tell(source, "Reset ThemeCore to CyberGlass.", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandSourceStack source) {
        tell(source, "Theme data is datapack-backed. Run /reload to rebuild the ThemeCore registry from data packs.", ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static int preview(CommandSourceStack source, String rawId) {
        EchoTheme theme = ThemeRegistry.get(ThemeRegistry.parseThemeId(rawId));
        source.sendSuccess(() -> Component.literal("[ECHO THEME] Preview " + theme.displayName() + " ")
            .append(chip("primary", theme.colors().primary()))
            .append(Component.literal(" "))
            .append(chip("accent", theme.colors().accent()))
            .append(Component.literal(" "))
            .append(chip("success", theme.colors().success()))
            .append(Component.literal(" "))
            .append(chip("warning", theme.colors().warning()))
            .append(Component.literal(" "))
            .append(chip("error", theme.colors().error())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int visualCurrent(CommandSourceStack source) {
        ThemeVisualSettings settings = ThemeVisualSettings.resolve(ThemeRegistry.getCurrentTheme());
        tell(source, "Visual settings: glow=" + settings.glowIntensity()
            + " particles=" + settings.particleIntensity()
            + " distortion=" + settings.distortionStrength()
            + " edge=" + settings.edgeGlowStrength(), ChatFormatting.AQUA);
        tell(source, "Enabled: glow=" + settings.glowEnabled()
            + " particles=" + settings.particlesEnabled()
            + " distortion=" + settings.distortionEnabled()
            + " edge=" + settings.edgeGlowEnabled()
            + " noise=" + settings.noiseEnabled(), ChatFormatting.GRAY);
        return Command.SINGLE_SUCCESS;
    }

    private static int visualTest(CommandSourceStack source, String testId) {
        if (!ThemeCoreRenderCoreBridge.isRenderCoreLoaded()) {
            tell(source, "RenderCore is unavailable; visual test '" + testId + "' was not played.", ChatFormatting.YELLOW);
            return 0;
        }
        tell(source, "RenderCore is present. ThemeCore provider data is available for '" + testId + "'.", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int visualIntensity(CommandSourceStack source, double value) {
        ThemeRegistry.setDebugVisualIntensity((float) value);
        tell(source, "Debug visual intensity set to " + ThemeRegistry.debugVisualIntensity(), ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int presetList(CommandSourceStack source) {
        String presets = RenderPresetRegistry.listPresets().stream()
            .map(preset -> preset.id() + " [" + preset.type() + " -> " + preset.theme() + "]")
            .collect(Collectors.joining(", "));
        tell(source, "Loaded render presets: " + (presets.isBlank() ? "<none>" : presets), ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int presetPreview(CommandSourceStack source, String rawId) {
        Identifier id = Identifier.tryParse(rawId == null ? "" : rawId.trim());
        if (id == null) {
            tell(source, "Invalid render preset id: " + rawId, ChatFormatting.RED);
            return 0;
        }
        EchoThemeRenderPreset preset = RenderPresetRegistry.find(id).orElse(null);
        if (preset == null) {
            tell(source, "Render preset " + id + " is not loaded.", ChatFormatting.RED);
            return 0;
        }
        tell(source, "Preset " + preset.id() + ": type=" + preset.type()
            + ", theme=" + preset.theme()
            + ", effects=" + preset.effects().size()
            + ", colors=" + preset.colors().keySet(), ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int vanillaCurrent(CommandSourceStack source) {
        tell(source, "Vanilla UI theming: " + ThemeCoreConfig.vanillaUiEnabled()
            + ", safe_mode=" + ThemeCoreConfig.vanillaSafeMode()
            + ", unknown_screens_skipped=" + ThemeCoreConfig.disableUnknownScreens(), ChatFormatting.AQUA);
        tell(source, "Enabled surfaces: main_menu=" + flag(ThemeCoreConfig.THEME_MAIN_MENU)
            + ", pause=" + flag(ThemeCoreConfig.THEME_PAUSE_MENU)
            + ", options=" + flag(ThemeCoreConfig.THEME_OPTIONS_MENU)
            + ", mods=" + flag(ThemeCoreConfig.THEME_MODS_SCREEN)
            + ", inventory=" + flag(ThemeCoreConfig.THEME_INVENTORY)
            + ", containers=" + flag(ThemeCoreConfig.THEME_CONTAINERS)
            + ", hotbar=" + flag(ThemeCoreConfig.THEME_HOTBAR)
            + ", chat=" + flag(ThemeCoreConfig.THEME_CHAT), ChatFormatting.GRAY);
        tell(source, "Current vanilla screen classification is client-only; enable debug screen names to inspect it in-game.", ChatFormatting.DARK_AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean canMutate(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static String flag(com.echoplatform.echocore.api.config.EchoNativeConfigSpec.BooleanValue value) {
        return String.valueOf(ThemeCoreConfig.bool(value));
    }

    private static boolean loaded(String modid) {
        return EchoRuntimeModules.isLoaded(modid);
    }

    private static Component chip(String label, int color) {
        return Component.literal(label).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF)));
    }

    private static void tell(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal("[ECHO THEME] " + message).withStyle(color), false);
    }
}
