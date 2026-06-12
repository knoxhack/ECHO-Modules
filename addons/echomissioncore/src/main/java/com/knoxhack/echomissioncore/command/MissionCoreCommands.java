package com.knoxhack.echomissioncore.command;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class MissionCoreCommands {
    private MissionCoreCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection) {
        // Player-facing subcommands (no permission requirements)
        dispatcher.register(Commands.literal("echomission")
                .then(Commands.literal("my")
                        .executes(context -> myMissions(context.getSource().getPlayerOrException())))
                .then(Commands.literal("status")
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .executes(context -> status(
                                        context.getSource().getPlayerOrException(),
                                        parse(StringArgumentType.getString(context, "mission"))))))
                .then(Commands.literal("claim")
                        .executes(context -> claimAll(context.getSource().getPlayerOrException()))
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .executes(context -> claim(
                                        context.getSource().getPlayerOrException(),
                                        parse(StringArgumentType.getString(context, "mission")))))));

        // Gamemaster subcommands
        dispatcher.register(Commands.literal("echomission")
                .requires(MissionCoreCommands::isGamemaster)
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .executes(context -> inspect(
                                        context.getSource(),
                                        parse(StringArgumentType.getString(context, "mission"))))))
                .then(Commands.literal("start")
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .executes(context -> start(
                                        context.getSource().getPlayerOrException(),
                                        parse(StringArgumentType.getString(context, "mission"))))))
                .then(Commands.literal("complete")
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .executes(context -> complete(
                                        context.getSource().getPlayerOrException(),
                                        parse(StringArgumentType.getString(context, "mission"))))))
                .then(Commands.literal("claim")
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .executes(context -> claim(
                                        context.getSource().getPlayerOrException(),
                                        parse(StringArgumentType.getString(context, "mission"))))))
                .then(Commands.literal("progress")
                        .then(Commands.argument("mission", StringArgumentType.string())
                                .then(Commands.argument("objective", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> progress(
                                                        context.getSource().getPlayerOrException(),
                                                        parse(StringArgumentType.getString(context, "mission")),
                                                        parse(StringArgumentType.getString(context, "objective")),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("record")
                        .then(Commands.argument("type", StringArgumentType.string())
                                .then(Commands.argument("target", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> record(
                                                        context.getSource().getPlayerOrException(),
                                                        MissionObjectiveType.byId(StringArgumentType.getString(context, "type")),
                                                        parse(StringArgumentType.getString(context, "target")),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("validate").executes(context -> validate(context.getSource())))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource()))));
    }

    private static boolean isGamemaster(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int list(CommandSourceStack source) {
        String missions = MissionCoreService.INSTANCE.missionDefinitions().stream()
                .map(mission -> mission.id().toString())
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        source.sendSuccess(() -> Component.literal("MissionCore missions: " + missions), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int myMissions(ServerPlayer player) {
        var missions = MissionCoreService.INSTANCE.missions(player);
        if (missions.isEmpty()) {
            tell(player, "No mission progress.");
            return 0;
        }
        int active = 0;
        for (var m : missions) {
            if (m.status() == com.echoplatform.echocore.api.mission.MissionStatus.ACTIVE
                    || m.status() == com.echoplatform.echocore.api.mission.MissionStatus.CLAIMABLE) {
                active++;
            }
        }
        tell(player, "Your missions (" + missions.size() + " total, " + active + " active/claimable):");
        for (var m : missions) {
            String label = m.definition() == null ? m.id().toString() : m.definition().title();
            String progress = m.status() == com.echoplatform.echocore.api.mission.MissionStatus.ACTIVE
                    ? " [" + Math.round(m.progress() * 100f) + "%]" : "";
            tell(player, "  - " + label + " [" + m.status().name().toLowerCase() + "]" + progress);
        }
        return missions.size();
    }

    private static int status(ServerPlayer player, Identifier missionId) {
        var opt = MissionCoreService.INSTANCE.mission(player, missionId);
        if (opt.isEmpty()) {
            tell(player, "Mission not found: " + missionId);
            return 0;
        }
        var m = opt.get();
        String label = m.definition() == null ? missionId.toString() : m.definition().title();
        tell(player, "Mission: " + label);
        tell(player, "  Status: " + m.status().name().toLowerCase());
        tell(player, "  Progress: " + Math.round(m.progress() * 100f) + "%");
        if (!m.objectives().isEmpty()) {
            tell(player, "  Objectives:");
            for (var obj : m.objectives()) {
                if (obj.hidden()) continue;
                tell(player, "    - " + obj.label() + ": " + obj.progress() + "/" + obj.required()
                        + (obj.complete() ? " [done]" : ""));
            }
        }
        long claimable = m.rewards().stream().filter(r -> r.claimable() && !r.claimed()).count();
        if (claimable > 0) {
            tell(player, "  Rewards: " + claimable + " claimable");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int claimAll(ServerPlayer player) {
        var missions = MissionCoreService.INSTANCE.missions(player);
        int claimed = 0;
        for (var m : missions) {
            for (var r : m.rewards()) {
                if (r.claimable() && !r.claimed()) {
                    if (MissionCoreService.INSTANCE.claimReward(player, m.id())) {
                        claimed++;
                        break;
                    }
                }
            }
        }
        tell(player, claimed > 0 ? "Claimed rewards for " + claimed + " mission(s)." : "No claimable rewards.");
        return claimed;
    }

    private static int inspect(CommandSourceStack source, Identifier missionId) {
        source.sendSuccess(() -> Component.literal(MissionCoreService.INSTANCE.debugState(source.getPlayer(), missionId)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int start(ServerPlayer player, Identifier missionId) {
        boolean ok = MissionCoreService.INSTANCE.startMission(player, missionId);
        tell(player, ok ? "Started mission " + missionId + "." : "Could not start mission " + missionId + ".");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int complete(ServerPlayer player, Identifier missionId) {
        boolean ok = MissionCoreService.INSTANCE.completeMission(player, missionId);
        tell(player, ok ? "Completed mission " + missionId + "." : "Could not complete mission " + missionId + ".");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int claim(ServerPlayer player, Identifier missionId) {
        boolean ok = MissionCoreService.INSTANCE.claimReward(player, missionId);
        tell(player, ok ? "Claimed reward for " + missionId + "." : "No claimable reward for " + missionId + ".");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int progress(ServerPlayer player, Identifier missionId, Identifier objectiveId, int amount) {
        boolean ok = MissionCoreService.INSTANCE.forceProgress(player, missionId, objectiveId, amount);
        tell(player, ok ? "Progressed " + objectiveId + " by " + amount + "." : "Could not progress objective.");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int record(ServerPlayer player, MissionObjectiveType type, Identifier target, int amount) {
        boolean ok = MissionCoreService.INSTANCE.recordObjective(player, type, target, amount, java.util.Map.of("debug", "true"));
        tell(player, ok ? "Recorded " + type.id() + " -> " + target + " by " + amount + "." : "No matching objective recorded.");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reload(CommandSourceStack source) {
        source.getServer().reloadResources(source.getServer().getPackRepository().getSelectedIds());
        source.sendSuccess(() -> Component.literal("MissionCore content reload requested."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int validate(CommandSourceStack source) {
        java.util.Map<String, Integer> sourceCounts = MissionCoreService.INSTANCE.sourceCounts();
        if (sourceCounts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("MissionCore source counts: none"), false);
        } else {
            source.sendSuccess(() -> Component.literal("MissionCore source counts: " + sourceCounts), false);
        }
        java.util.Map<String, String> hookCoverage = MissionCoreService.INSTANCE.missionHookCoverageBySource();
        if (!hookCoverage.isEmpty()) {
            source.sendSuccess(() -> Component.literal("MissionCore hook coverage: " + hookCoverage), false);
        }
        java.util.List<String> warnings = new java.util.ArrayList<>(MissionCoreService.INSTANCE.validateContent());
        warnings.addAll(legacyProviderWarnings(sourceCounts));
        if (warnings.isEmpty()) {
            source.sendSuccess(() -> Component.literal("MissionCore validation passed."), false);
            return Command.SINGLE_SUCCESS;
        }
        source.sendSuccess(() -> Component.literal("MissionCore validation warnings: " + warnings.size()), false);
        warnings.stream().limit(8).forEach(warning ->
                source.sendSuccess(() -> Component.literal("- " + warning), false));
        return warnings.size();
    }

    private static java.util.List<String> legacyProviderWarnings(java.util.Map<String, Integer> sourceCounts) {
        if (!EchoRuntimeModules.isLoaded("echoterminal") || sourceCounts.isEmpty()) {
            return java.util.List.of();
        }
        java.util.Map<String, String> providerClasses = java.util.Map.of(
                "echoagriculturereclamation", "com.knoxhack.echoagriculturereclamation.integration.ReclamationMissionProvider",
                "echoindustrialnexus", "com.knoxhack.echoindustrialnexus.integration.IndustrialMissionProvider",
                "echoconvoyprotocol", "com.knoxhack.echoconvoyprotocol.integration.ConvoyMissionProvider",
                "echoorbitalremnants", "com.knoxhack.echoorbitalremnants.integration.OrbitalMissionProvider",
                "echonexusprotocol", "com.knoxhack.echonexusprotocol.integration.NexusTerminalMissionProvider",
                "echoblackboxprotocol", "com.knoxhack.echoblackboxprotocol.integration.BlackboxMissionProvider",
                "echostationfall", "com.knoxhack.echostationfall.integration.StationfallTerminalCommonIntegration$Provider");
        try {
            Class<?> registry = Class.forName("com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry");
            Object value = registry.getMethod("providers").invoke(null);
            if (!(value instanceof java.util.List<?> providers)) {
                return java.util.List.of();
            }
            java.util.Set<String> activeClasses = providers.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(provider -> provider.getClass().getName())
                    .collect(java.util.stream.Collectors.toSet());
            java.util.List<String> warnings = new java.util.ArrayList<>();
            providerClasses.forEach((source, providerClass) -> {
                if (sourceCounts.containsKey(source) && activeClasses.contains(providerClass)) {
                    warnings.add("Legacy Terminal mission provider is still registered while MissionCore owns " + source + " missions: " + providerClass);
                }
            });
            return warnings;
        } catch (ReflectiveOperationException exception) {
            return java.util.List.of("Could not inspect Terminal mission providers for duplicate display suppression: " + exception.getClass().getSimpleName());
        }
    }

    private static Identifier parse(String value) {
        Identifier id = Identifier.tryParse(value);
        return id == null ? Identifier.fromNamespaceAndPath("echomissioncore", value.toLowerCase(java.util.Locale.ROOT)) : id;
    }

    private static void tell(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[MissionCore] " + message), true);
    }
}
