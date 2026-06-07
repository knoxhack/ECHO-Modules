package com.knoxhack.echoweathercore.command;

import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.forecast.WeatherForecast;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherScope;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import com.knoxhack.echoweathercore.server.WeatherForecastManager;
import com.knoxhack.echoweathercore.server.WeatherStateManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class WeatherCoreCommands {
    private WeatherCoreCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("echoweathercore")
            .then(Commands.literal("current").executes(WeatherCoreCommands::current))
            .then(Commands.literal("forecast").executes(WeatherCoreCommands::forecast))
            .then(Commands.literal("trigger")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("weather", StringArgumentType.word())
                    .executes(ctx -> trigger(ctx, null, null))
                    .then(Commands.argument("severity", StringArgumentType.word())
                        .executes(ctx -> trigger(ctx, StringArgumentType.getString(ctx, "severity"), null))
                        .then(Commands.argument("scope", StringArgumentType.word())
                            .executes(ctx -> trigger(ctx, StringArgumentType.getString(ctx, "severity"), StringArgumentType.getString(ctx, "scope")))))))
            .then(Commands.literal("clear")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(WeatherCoreCommands::clearAll)
                .then(Commands.argument("weather", StringArgumentType.word()).executes(WeatherCoreCommands::clear)))
            .then(Commands.literal("set_severity")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("severity", StringArgumentType.word()).executes(WeatherCoreCommands::setSeverity)))
            .then(Commands.literal("debug").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).executes(WeatherCoreCommands::debug))
            .then(Commands.literal("locate_shelter").executes(WeatherCoreCommands::locateShelter))
            .then(Commands.literal("route_risk").executes(WeatherCoreCommands::routeRisk))
            .then(Commands.literal("list_profiles").executes(WeatherCoreCommands::listProfiles))
            .then(Commands.literal("reload").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).executes(WeatherCoreCommands::reload))
        );
    }

    private static int current(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be a player."));
            return 0;
        }
        WeatherForecast current = WeatherForecastManager.getCurrentWeatherForPlayer(player);
        if (current == null) {
            source.sendSuccess(() -> Component.literal("No active weather at your location."), false);
        } else {
            source.sendSuccess(() -> Component.literal(WeatherForecastManager.formatForecast(current)), false);
        }
        return 1;
    }

    private static int forecast(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be a player."));
            return 0;
        }
        List<WeatherForecast> forecasts = WeatherCoreApi.getForecast(player);
        if (forecasts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No weather forecast available."), false);
        } else {
            for (WeatherForecast f : forecasts) {
                source.sendSuccess(() -> Component.literal(WeatherForecastManager.formatForecast(f)), false);
            }
        }
        return 1;
    }

    private static int trigger(CommandContext<CommandSourceStack> ctx, String severityStr, String scopeStr) {
        CommandSourceStack source = ctx.getSource();
        String weatherArg = StringArgumentType.getString(ctx, "weather");
        WeatherType type;
        try {
            type = WeatherType.valueOf(weatherArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown weather type: " + weatherArg));
            return 0;
        }

        WeatherProfile target = null;
        for (WeatherProfile profile : WeatherDataReloadListener.INSTANCE.getProfiles().values()) {
            if (profile.type() == type) {
                target = profile;
                break;
            }
        }
        if (target == null) {
            source.sendFailure(Component.literal("No profile found for weather type: " + weatherArg));
            return 0;
        }

        WeatherSeverity severity = severityStr != null ? parseSeverity(severityStr) : target.defaultSeverity();
        WeatherScope scope = scopeStr != null ? parseScope(scopeStr) : target.scope();
        int radius = scope == WeatherScope.LOCAL ? 800 : scope == WeatherScope.REGIONAL ? 2400 : 0;

        if (source.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
            WeatherCoreApi.triggerWeather(sl, target.id(), severity, source.getEntity() != null ? source.getEntity().blockPosition() : sl.getRespawnData().pos(), radius);
            final WeatherProfile finalTarget = target;
            final WeatherSeverity finalSeverity = severity;
            final WeatherScope finalScope = scope;
            source.sendSuccess(() -> Component.literal("Triggered " + finalTarget.displayName() + " (" + finalSeverity + ", " + finalScope + ")"), false);
        }
        return 1;
    }

    private static int clearAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (source.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
            WeatherCoreApi.clearAllWeather(sl);
            source.sendSuccess(() -> Component.literal("Cleared all weather events."), false);
        }
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String weatherArg = StringArgumentType.getString(ctx, "weather");
        try {
            WeatherType type = WeatherType.valueOf(weatherArg.toUpperCase());
            if (source.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
                WeatherCoreApi.clearWeather(sl, type);
                source.sendSuccess(() -> Component.literal("Cleared " + type + " events."), false);
            }
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown weather type: " + weatherArg));
            return 0;
        }
        return 1;
    }

    private static int setSeverity(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("Use /echoweathercore trigger to start a weather with a specific severity."), false);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("ECHO WeatherCore Debug"), false);
        source.sendSuccess(() -> Component.literal("Profiles: " + WeatherDataReloadListener.INSTANCE.getProfiles().size()), false);
        if (source.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
            source.sendSuccess(() -> Component.literal("Active events: " + WeatherCoreApi.getActiveWeather(sl).size()), false);
        }
        return 1;
    }

    private static int locateShelter(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be a player."));
            return 0;
        }
        boolean sheltered = WeatherCoreApi.isSheltered(player);
        source.sendSuccess(() -> Component.literal(sheltered ? "You are currently sheltered." : "You are exposed to the elements."), false);
        return 1;
    }

    private static int routeRisk(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be a player."));
            return 0;
        }
        var risk = WeatherCoreApi.getRouteWeatherRisk(player, null);
        source.sendSuccess(() -> Component.literal("Route Weather Risk: " + risk), false);
        return 1;
    }

    private static int listProfiles(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("Weather Profiles:"), false);
        for (WeatherProfile profile : WeatherDataReloadListener.INSTANCE.getProfiles().values()) {
            source.sendSuccess(() -> Component.literal(" - " + profile.id() + " [" + profile.type() + "]"), false);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("Use /reload to reload data-driven weather profiles."), false);
        return 1;
    }

    private static WeatherSeverity parseSeverity(String s) {
        try {
            return WeatherSeverity.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WeatherSeverity.MODERATE;
        }
    }

    private static WeatherScope parseScope(String s) {
        try {
            return WeatherScope.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WeatherScope.REGIONAL;
        }
    }
}
