package com.knoxhack.echothemecore.client;

import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.EchoThemeCore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ThemeCoreClientCommands {
    private ThemeCoreClientCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echo_theme_client")
                .then(Commands.literal("current")
                    .executes(context -> current()))
                .then(Commands.literal("list")
                    .executes(context -> list()))
                .then(Commands.literal("set")
                    .then(Commands.argument("theme_id", StringArgumentType.string())
                        .executes(context -> set(StringArgumentType.getString(context, "theme_id")))))
                .then(Commands.literal("cycle")
                    .then(Commands.literal("next")
                        .executes(context -> cycle(1)))
                    .then(Commands.literal("previous")
                        .executes(context -> cycle(-1))))
                .then(Commands.literal("reset")
                    .executes(context -> reset()))
        );
    }

    private static int current() {
        EchoTheme theme = ClientThemeState.currentTheme();
        message("Client ThemeCore theme: " + theme.displayName() + " (" + theme.id() + ")");
        return 1;
    }

    private static int list() {
        String themes = ClientThemeState.listPublicThemes().stream()
            .map(theme -> theme.id() + " [" + theme.displayName() + "]")
            .collect(Collectors.joining(", "));
        message(themes.isBlank() ? "No public ThemeCore themes loaded." : "Public client themes: " + themes);
        return ClientThemeState.listPublicThemes().size();
    }

    private static int set(String rawId) {
        Identifier id = parseThemeId(rawId);
        if (id == null) {
            message("Invalid ThemeCore theme id: " + rawId);
            return 0;
        }
        EchoTheme selected = ClientThemeState.setTheme(id);
        message("Client ThemeCore theme set to " + selected.displayName() + " (" + selected.id() + ").");
        return 1;
    }

    private static int cycle(int direction) {
        EchoTheme selected = ClientThemeState.cycleTheme(direction);
        message("Client ThemeCore theme cycled to " + selected.displayName() + " (" + selected.id() + ").");
        return 1;
    }

    private static int reset() {
        EchoTheme selected = ClientThemeState.reset();
        message("Client ThemeCore theme reset to " + selected.displayName() + " (" + selected.id() + ").");
        return 1;
    }

    private static void message(String text) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(text));
        }
    }

    private static Identifier parseThemeId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim();
        return trimmed.contains(":")
            ? Identifier.tryParse(trimmed)
            : Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, trimmed);
    }
}
