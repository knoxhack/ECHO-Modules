package com.knoxhack.echoashfallprotocol.command;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneCommandService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class CompanionDroneCommands {
    private CompanionDroneCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("drone")
                .then(Commands.literal("status").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "status")))
                .then(Commands.literal("recall").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "recall")))
                .then(Commands.literal("scan").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "scan_area")))
                .then(Commands.literal("scan_area").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "scan_area")))
                .then(Commands.literal("scout_ahead").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "scout_ahead")))
                .then(Commands.literal("collect_scrap").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "collect_scrap")))
                .then(Commands.literal("guard_here").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "guard_here")))
                .then(Commands.literal("toggle_assist").executes(ctx -> command(ctx.getSource().getPlayerOrException(), "toggle_assist")))
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .executes(ctx -> command(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "mode")))))
                .then(Commands.literal("battery")
                        .requires(CompanionDroneCommands::canDebug)
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> DroneCommandService.setBattery(ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("reset")
                        .requires(CompanionDroneCommands::canDebug)
                        .executes(ctx -> DroneCommandService.reset(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("debug")
                        .requires(CompanionDroneCommands::canDebug)
                        .then(Commands.literal("markers")
                                .executes(ctx -> DroneCommandService.debugMarkers(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("upgrades")
                        .requires(CompanionDroneCommands::canDebug)
                        .then(Commands.literal("list")
                                .executes(ctx -> DroneCommandService.listUpgrades(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("add")
                                .then(Commands.argument("upgrade", StringArgumentType.word())
                                        .executes(ctx -> DroneCommandService.addUpgrade(ctx.getSource().getPlayerOrException(),
                                                EchoDroneUpgrade.parse(StringArgumentType.getString(ctx, "upgrade")))))));
    }

    private static int command(ServerPlayer player, String command) {
        return DroneCommandService.execute(player, command);
    }

    private static boolean canDebug(CommandSourceStack source) {
        return Config.ENABLE_DRONE_DEBUG_COMMANDS.get()
                && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
