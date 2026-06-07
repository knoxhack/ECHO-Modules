package com.knoxhack.echoplayercore.command;

import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.knoxhack.echoplayercore.api.EchoPlayerCoreApi;
import com.knoxhack.echoplayercore.config.PlayerCoreConfig;
import com.knoxhack.echoplayercore.event.PlayerBackTeleportEvent;
import com.knoxhack.echoplayercore.event.PlayerDeathLocationStoredEvent;
import com.knoxhack.echoplayercore.event.PlayerHomeDeletedEvent;
import com.knoxhack.echoplayercore.event.PlayerHomeSetEvent;
import com.knoxhack.echoplayercore.event.PlayerHomeTeleportEvent;
import com.knoxhack.echoplayercore.event.PlayerRandomTeleportEvent;
import com.knoxhack.echoplayercore.event.PlayerSpawnTeleportEvent;
import com.knoxhack.echoplayercore.event.PlayerTpaTeleportEvent;
import com.knoxhack.echoplayercore.event.PlayerWarpTeleportEvent;
import com.knoxhack.echoplayercore.data.HomeLocation;
import com.knoxhack.echoplayercore.data.PlayerCoreSavedData;
import com.knoxhack.echoplayercore.data.PlayerTravelData;
import com.knoxhack.echoplayercore.data.TeleportLocation;
import com.knoxhack.echoplayercore.data.WarpLocation;
import com.knoxhack.echoplayercore.data.WarpSavedData;
import com.knoxhack.echoplayercore.service.CooldownService;
import com.knoxhack.echoplayercore.service.TpaService;
import com.knoxhack.echoplayercore.service.WarpService;
import com.knoxhack.echoplayercore.teleport.TeleportAction;
import com.knoxhack.echoplayercore.teleport.TeleportReason;
import com.knoxhack.echoplayercore.teleport.TeleportService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class PlayerCoreCommands {
    private static final Random RANDOM = new Random();

    private PlayerCoreCommands() {
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!PlayerCoreConfig.enabled()) {
            return;
        }
        if (PlayerCoreConfig.aliasCommandsEnabled()) {
            registerAlias(dispatcher, "sethome", ctx -> setHome(ctx.getSource().getPlayerOrException(), optionalArg(ctx, "name", PlayerCoreConfig.defaultHomeName())));
            registerAlias(dispatcher, "home", ctx -> home(ctx.getSource().getPlayerOrException(), optionalArg(ctx, "name", PlayerCoreConfig.defaultHomeName())));
            registerAlias(dispatcher, "delhome", ctx -> delHome(ctx.getSource().getPlayerOrException(), optionalArg(ctx, "name", PlayerCoreConfig.defaultHomeName())));
            dispatcher.register(Commands.literal("homes").executes(ctx -> listHomes(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("back").executes(ctx -> back(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("rtp").executes(ctx -> rtp(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("spawn").executes(ctx -> spawn(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("tpa")
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                            .executes(ctx -> tpa(ctx.getSource().getPlayerOrException(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"), false))));
            dispatcher.register(Commands.literal("tpahere")
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                            .executes(ctx -> tpa(ctx.getSource().getPlayerOrException(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"), true))));
            dispatcher.register(Commands.literal("tpaccept").executes(ctx -> tpaccept(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("tpdeny").executes(ctx -> tpdeny(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("warp")
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> warp(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
            dispatcher.register(Commands.literal("warps").executes(ctx -> listWarps(ctx.getSource().getPlayerOrException())));
            dispatcher.register(Commands.literal("setwarp")
                    .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> setWarp(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
            dispatcher.register(Commands.literal("delwarp")
                    .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> delWarp(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        }
    }

    public static void registerEchoSubcommands() {
        EchoCommandRegistry.register(Commands.literal("sethome")
                .executes(ctx -> setHome(ctx.getSource().getPlayerOrException(), PlayerCoreConfig.defaultHomeName()))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setHome(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        EchoCommandRegistry.register(Commands.literal("home")
                .executes(ctx -> home(ctx.getSource().getPlayerOrException(), PlayerCoreConfig.defaultHomeName()))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> home(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        EchoCommandRegistry.register(Commands.literal("delhome")
                .executes(ctx -> delHome(ctx.getSource().getPlayerOrException(), PlayerCoreConfig.defaultHomeName()))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> delHome(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        EchoCommandRegistry.register(Commands.literal("homes").executes(ctx -> listHomes(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("back").executes(ctx -> back(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("rtp").executes(ctx -> rtp(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("spawn").executes(ctx -> spawn(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("tpa")
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(ctx -> tpa(ctx.getSource().getPlayerOrException(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"), false))));
        EchoCommandRegistry.register(Commands.literal("tpahere")
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(ctx -> tpa(ctx.getSource().getPlayerOrException(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"), true))));
        EchoCommandRegistry.register(Commands.literal("tpaccept").executes(ctx -> tpaccept(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("tpdeny").executes(ctx -> tpdeny(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> warp(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        EchoCommandRegistry.register(Commands.literal("warps").executes(ctx -> listWarps(ctx.getSource().getPlayerOrException())));
        EchoCommandRegistry.register(Commands.literal("setwarp")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setWarp(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        EchoCommandRegistry.register(Commands.literal("delwarp")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> delWarp(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))));
        EchoCommandRegistry.register(Commands.literal("playercore")
                .then(Commands.literal("reload")
                        .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> {
                            tell(ctx.getSource().getPlayerOrException(), "Config reload requires a server restart in this version.", ChatFormatting.YELLOW);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("stats")
                        .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> showStats(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("debug")
                        .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(ctx -> debugPlayer(ctx.getSource().getPlayerOrException(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("clearcooldown")
                        .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("action", StringArgumentType.word())
                                        .executes(ctx -> clearCooldown(ctx.getSource().getPlayerOrException(),
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "action")))))));
    }

    private static void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher, String name, com.mojang.brigadier.Command<CommandSourceStack> executor) {
        dispatcher.register(Commands.literal(name)
                .executes(executor)
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(executor)));
    }

    private static String optionalArg(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String arg, String fallback) {
        try {
            return StringArgumentType.getString(ctx, arg);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static int setHome(ServerPlayer player, String rawName) {
        if (!PlayerCoreConfig.homesEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        String name = normalizeHomeName(rawName);
        if (!HomeLocation.validName(name)) {
            tell(player, "echoplayercore.message.home_name_invalid", ChatFormatting.RED);
            return 0;
        }
        int limit = maxHomes(player);
        var data = PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).getOrCreate(player.getUUID());
        if (!data.home(name).isPresent() && data.homeCount() >= limit) {
            tell(player, "echoplayercore.message.home_limit_reached", ChatFormatting.RED, String.valueOf(limit));
            return 0;
        }
        boolean overwriting = data.home(name).isPresent();
        long now = System.currentTimeMillis();
        HomeLocation home = new HomeLocation(
                name,
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                overwriting ? data.home(name).get().createdAt() : now,
                now
        );
        var setEvent = new PlayerHomeSetEvent(player, home);
        if (setEvent.isCanceled()) {
            return 0;
        }
        data.setHome(home);
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).markDirty();
        if (overwriting) {
            tell(player, "echoplayercore.message.home_overwritten", ChatFormatting.GREEN, name);
        } else {
            tell(player, "echoplayercore.message.home_set", ChatFormatting.GREEN, name);
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int home(ServerPlayer player, String rawName) {
        if (!PlayerCoreConfig.homesEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (CooldownService.isOnCooldown(player, TeleportAction.HOME)) {
            tellCooldown(player, TeleportAction.HOME);
            return 0;
        }
        String name = normalizeHomeName(rawName);
        Optional<HomeLocation> homeOpt = EchoPlayerCoreApi.getHome(player, name);
        if (homeOpt.isEmpty()) {
            tell(player, "echoplayercore.message.home_missing", ChatFormatting.RED, name);
            return 0;
        }
        HomeLocation home = homeOpt.get();
        if (!PlayerCoreConfig.allowCrossDimensionHome()) {
            if (!player.level().dimension().equals(home.dimension())) {
                tell(player, "echoplayercore.message.home_dimension_blocked", ChatFormatting.RED, name);
                return 0;
            }
        }
        ServerLevel targetLevel = player.level().getServer().getLevel(home.dimension());
        if (targetLevel == null) {
            tell(player, "echoplayercore.message.teleport_failed", ChatFormatting.RED);
            return 0;
        }
        TeleportLocation target = new TeleportLocation(
                home.dimension(), home.x(), home.y(), home.z(),
                home.yaw(), home.pitch(), "home", System.currentTimeMillis()
        );
        var homeEvent = new PlayerHomeTeleportEvent(player, home);
        if (homeEvent.isCanceled()) {
            return 0;
        }
        if (TeleportService.teleportTo(player, target, TeleportReason.HOME)) {
            tell(player, "echoplayercore.message.teleport_success", ChatFormatting.GREEN);
            CooldownService.applyCooldown(player, TeleportAction.HOME);
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.teleport_failed", ChatFormatting.RED);
        return 0;
    }

    private static int delHome(ServerPlayer player, String rawName) {
        if (!PlayerCoreConfig.homesEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        String name = normalizeHomeName(rawName);
        var delEvent = new PlayerHomeDeletedEvent(player, name);
        if (delEvent.isCanceled()) {
            return 0;
        }
        if (EchoPlayerCoreApi.deleteHome(player, name)) {
            tell(player, "echoplayercore.message.home_deleted", ChatFormatting.GREEN, name);
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.home_missing", ChatFormatting.RED, name);
        return 0;
    }

    private static int listHomes(ServerPlayer player) {
        if (!PlayerCoreConfig.homesEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        Collection<HomeLocation> homes = EchoPlayerCoreApi.getHomes(player);
        if (homes.isEmpty()) {
            tell(player, "echoplayercore.message.home_list_empty", ChatFormatting.YELLOW);
            return 0;
        }
        int limit = maxHomes(player);
        tell(player, "echoplayercore.message.home_list_header", ChatFormatting.AQUA, String.valueOf(homes.size()), String.valueOf(limit));
        for (HomeLocation home : homes) {
            String dim = home.dimension().identifier().toString();
            String line = String.format(" - %s [%s] %.1f %.1f %.1f", home.name(), dim, home.x(), home.y(), home.z());
            player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int back(ServerPlayer player) {
        if (!PlayerCoreConfig.backEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (CooldownService.isOnCooldown(player, TeleportAction.BACK)) {
            tellCooldown(player, TeleportAction.BACK);
            return 0;
        }
        Optional<TeleportLocation> backOpt = EchoPlayerCoreApi.getBackLocation(player);
        if (backOpt.isEmpty()) {
            tell(player, "echoplayercore.message.no_back_location", ChatFormatting.YELLOW);
            return 0;
        }
        TeleportLocation back = backOpt.get();
        var backEvent = new PlayerBackTeleportEvent(player, back);
        if (backEvent.isCanceled()) {
            return 0;
        }
        TeleportLocation current = TeleportLocation.fromPlayer(player, "back");
        if (TeleportService.teleportTo(player, back, TeleportReason.BACK)) {
            EchoPlayerCoreApi.setBackLocation(player, current);
            tell(player, "echoplayercore.message.teleport_success", ChatFormatting.GREEN);
            CooldownService.applyCooldown(player, TeleportAction.BACK);
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.teleport_failed", ChatFormatting.RED);
        return 0;
    }

    public static int spawn(ServerPlayer player) {
        if (!PlayerCoreConfig.spawnEnabled() || !PlayerCoreConfig.allowSpawnCommand()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (CooldownService.isOnCooldown(player, TeleportAction.SPAWN)) {
            tellCooldown(player, TeleportAction.SPAWN);
            return 0;
        }
        BlockPos spawnPos = player.level().getServer().overworld().getRespawnData().pos();
        ResourceKey<Level> dim = Level.OVERWORLD;
        ServerLevel target = player.level().getServer().getLevel(dim);
        if (target == null) {
            tell(player, "echoplayercore.message.spawn_failed", ChatFormatting.RED);
            return 0;
        }
        if (!PlayerCoreConfig.allowCrossDimensionSpawn() && !player.level().dimension().equals(dim)) {
            tell(player, "echoplayercore.message.spawn_failed", ChatFormatting.RED);
            return 0;
        }
        TeleportLocation targetLoc = new TeleportLocation(
                dim, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                player.getYRot(), player.getXRot(), "spawn", System.currentTimeMillis()
        );
        var spawnEvent = new PlayerSpawnTeleportEvent(player, targetLoc);
        if (spawnEvent.isCanceled()) {
            return 0;
        }
        if (TeleportService.teleportTo(player, targetLoc, TeleportReason.SPAWN)) {
            tell(player, "echoplayercore.message.spawn_success", ChatFormatting.GREEN);
            CooldownService.applyCooldown(player, TeleportAction.SPAWN);
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.spawn_failed", ChatFormatting.RED);
        return 0;
    }

    public static int rtp(ServerPlayer player) {
        if (!PlayerCoreConfig.rtpEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (CooldownService.isOnCooldown(player, TeleportAction.RTP)) {
            tellCooldown(player, TeleportAction.RTP);
            return 0;
        }
        String dimId = player.level().dimension().identifier().toString();
        if (PlayerCoreConfig.rtpBlockedDimensions().contains(dimId)) {
            tell(player, "echoplayercore.message.rtp_dimension_blocked", ChatFormatting.RED);
            return 0;
        }
        if (!PlayerCoreConfig.rtpAllowedDimensions().isEmpty() && !PlayerCoreConfig.rtpAllowedDimensions().contains(dimId)) {
            tell(player, "echoplayercore.message.rtp_dimension_blocked", ChatFormatting.RED);
            return 0;
        }
        tell(player, "echoplayercore.message.rtp_searching", ChatFormatting.AQUA);
        ServerLevel level = (ServerLevel) player.level();
        int minR = PlayerCoreConfig.rtpMinRadius();
        int maxR = PlayerCoreConfig.rtpMaxRadius();
        int attempts = PlayerCoreConfig.rtpMaxAttempts();
        int heightMin = PlayerCoreConfig.rtpSearchHeightMin();
        int heightMax = PlayerCoreConfig.rtpSearchHeightMax();

        for (int i = 0; i < attempts; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double distance = minR + RANDOM.nextDouble() * (maxR - minR);
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < heightMin || y > heightMax) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (TeleportService.isSafeSurface(level, pos, player)) {
                TeleportLocation target = new TeleportLocation(
                        level.dimension(), x + 0.5, y, z + 0.5,
                        player.getYRot(), player.getXRot(), "rtp", System.currentTimeMillis()
                );
                var rtpEvent = new PlayerRandomTeleportEvent(player, target);
                if (rtpEvent.isCanceled()) {
                    continue;
                }
                if (TeleportService.teleportTo(player, target, TeleportReason.RTP)) {
                    EchoPlayerCoreApi.setLastRtpLocation(player, target);
                    tell(player, "echoplayercore.message.rtp_success", ChatFormatting.GREEN,
                            String.valueOf(x), String.valueOf(y), String.valueOf(z));
                    CooldownService.applyCooldown(player, TeleportAction.RTP);
                    return Command.SINGLE_SUCCESS;
                }
            }
        }
        tell(player, "echoplayercore.message.rtp_failed", ChatFormatting.RED);
        return 0;
    }

    private static int tpa(ServerPlayer player, ServerPlayer target, boolean here) {
        if (!PlayerCoreConfig.tpaEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (CooldownService.isOnCooldown(player, here ? TeleportAction.TPA_HERE : TeleportAction.TPA)) {
            tellCooldown(player, here ? TeleportAction.TPA_HERE : TeleportAction.TPA);
            return 0;
        }
        if (TpaService.request(player, target, here)) {
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int tpaccept(ServerPlayer player) {
        if (!PlayerCoreConfig.tpaEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (TpaService.accept(player)) {
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int tpdeny(ServerPlayer player) {
        if (!PlayerCoreConfig.tpaEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (TpaService.deny(player)) {
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int warp(ServerPlayer player, String name) {
        if (!PlayerCoreConfig.warpsEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (CooldownService.isOnCooldown(player, TeleportAction.WARP)) {
            tellCooldown(player, TeleportAction.WARP);
            return 0;
        }
        String id = name == null ? "" : name.strip().toLowerCase(java.util.Locale.ROOT);
        if (id.isBlank()) {
            tell(player, "echoplayercore.message.warp_name_invalid", ChatFormatting.RED);
            return 0;
        }
        ServerLevel overworld = player.level().getServer().overworld();
        var warpOpt = WarpService.getWarp(overworld, id);
        if (warpOpt.isEmpty()) {
            tell(player, "echoplayercore.message.warp_missing", ChatFormatting.RED, id);
            return 0;
        }
        WarpLocation warp = warpOpt.get();
        var warpEvent = new PlayerWarpTeleportEvent(player, warp);
        if (warpEvent.isCanceled()) {
            return 0;
        }
        TeleportLocation target = new TeleportLocation(
                warp.dimension(), warp.x(), warp.y(), warp.z(),
                warp.yaw(), warp.pitch(), warp.id(), System.currentTimeMillis()
        );
        if (TeleportService.teleportTo(player, target, TeleportReason.WARP)) {
            tell(player, "echoplayercore.message.warp_success", ChatFormatting.GREEN, warp.displayName());
            CooldownService.applyCooldown(player, TeleportAction.WARP);
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.teleport_failed", ChatFormatting.RED);
        return 0;
    }

    private static int listWarps(ServerPlayer player) {
        if (!PlayerCoreConfig.warpsEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        ServerLevel overworld = player.level().getServer().overworld();
        var warps = WarpService.listWarps(overworld);
        if (warps.isEmpty()) {
            tell(player, "echoplayercore.message.warp_list_empty", ChatFormatting.YELLOW);
            return 0;
        }
        tell(player, "echoplayercore.message.warp_list_header", ChatFormatting.AQUA, String.valueOf(warps.size()));
        for (WarpLocation w : warps) {
            String dim = w.dimension().identifier().toString();
            String line = String.format(" - %s [%s] %.1f %.1f %.1f", w.displayName(), dim, w.x(), w.y(), w.z());
            player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setWarp(ServerPlayer player, String name) {
        if (!PlayerCoreConfig.warpsEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (!WarpService.canSetWarp(player)) {
            tell(player, "echoplayercore.message.no_permission", ChatFormatting.RED);
            return 0;
        }
        String id = name == null ? "" : name.strip().toLowerCase(java.util.Locale.ROOT);
        if (id.isBlank()) {
            tell(player, "echoplayercore.message.warp_name_invalid", ChatFormatting.RED);
            return 0;
        }
        WarpLocation warp = new WarpLocation(
                id, id, player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                true, ""
        );
        ServerLevel overworld = player.level().getServer().overworld();
        boolean overwriting = WarpService.getWarp(overworld, id).isPresent();
        if (WarpService.setWarp(overworld, warp)) {
            if (overwriting) {
                tell(player, "echoplayercore.message.warp_overwritten", ChatFormatting.GREEN, id);
            } else {
                tell(player, "echoplayercore.message.warp_set", ChatFormatting.GREEN, id);
            }
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.warp_set_failed", ChatFormatting.RED);
        return 0;
    }

    private static int delWarp(ServerPlayer player, String name) {
        if (!PlayerCoreConfig.warpsEnabled()) {
            tell(player, "echoplayercore.message.command_disabled", ChatFormatting.RED);
            return 0;
        }
        if (!WarpService.canDeleteWarp(player)) {
            tell(player, "echoplayercore.message.no_permission", ChatFormatting.RED);
            return 0;
        }
        String id = name == null ? "" : name.strip().toLowerCase(java.util.Locale.ROOT);
        if (id.isBlank()) {
            tell(player, "echoplayercore.message.warp_name_invalid", ChatFormatting.RED);
            return 0;
        }
        ServerLevel overworld = player.level().getServer().overworld();
        if (WarpService.deleteWarp(overworld, id)) {
            tell(player, "echoplayercore.message.warp_deleted", ChatFormatting.GREEN, id);
            return Command.SINGLE_SUCCESS;
        }
        tell(player, "echoplayercore.message.warp_missing", ChatFormatting.RED, id);
        return 0;
    }

    private static int showStats(ServerPlayer player) {
        var data = PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld());
        int players = data.playerCount();
        tell(player, "Players with travel data: " + players, ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int debugPlayer(ServerPlayer source, ServerPlayer target) {
        var data = PlayerCoreSavedData.get(target.level().getServer().overworld()).getOrCreate(target.getUUID());
        String homes = String.valueOf(data.homeCount());
        String cooldowns = data.cooldowns().toString();
        String back = data.lastBackLocation().map(TeleportLocation::toString).orElse("none");
        String death = data.lastDeathLocation().map(TeleportLocation::toString).orElse("none");
        tell(source, "echoplayercore.message.admin.debug.player", ChatFormatting.AQUA, homes, cooldowns, back, death);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearCooldown(ServerPlayer source, ServerPlayer target, String actionName) {
        try {
            TeleportAction action = TeleportAction.valueOf(actionName.toUpperCase(java.util.Locale.ROOT));
            CooldownService.clearCooldown(target, action);
            tell(source, "Cleared cooldown " + action.name() + " for " + target.getScoreboardName(), ChatFormatting.GREEN);
        } catch (IllegalArgumentException e) {
            tell(source, "Unknown action: " + actionName, ChatFormatting.RED);
        }
        return Command.SINGLE_SUCCESS;
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (!PlayerCoreConfig.enabled() || !PlayerCoreConfig.backEnabled() || !PlayerCoreConfig.allowBackAfterDeath()) {
            return;
        }
        if (player == null) {
            return;
        }
        TeleportLocation deathLoc = TeleportLocation.fromPlayer(player, "death");
        var deathEvent = new PlayerDeathLocationStoredEvent(player, deathLoc);
        if (deathEvent.isCanceled()) {
            return;
        }
        var data = PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).getOrCreate(player.getUUID());
        data.setLastDeathLocation(deathLoc);
        data.setLastBackLocation(deathLoc);
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).markDirty();
    }

    public static void onPlayerClone(ServerPlayer original, ServerPlayer clone) {
        if (!PlayerCoreConfig.enabled()) {
            return;
        }
        if (original == null || clone == null) {
            return;
        }
        var oldData = PlayerCoreSavedData.get(original.level().getServer().overworld()).get(original.getUUID());
        if (oldData.isEmpty()) {
            return;
        }
        var newData = PlayerCoreSavedData.get(clone.level().getServer().overworld()).getOrCreate(clone.getUUID());
        PlayerTravelData src = oldData.get();
        for (HomeLocation home : src.homes().values()) {
            newData.setHome(home);
        }
        src.lastBackLocation().ifPresent(newData::setLastBackLocation);
        src.lastDeathLocation().ifPresent(newData::setLastDeathLocation);
        src.lastRtpLocation().ifPresent(newData::setLastRtpLocation);
        for (java.util.Map.Entry<String, Long> e : src.cooldowns().entrySet()) {
            newData.setCooldown(e.getKey(), e.getValue());
        }
        PlayerCoreSavedData.get(clone.level().getServer().overworld()).markDirty();
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        if (!PlayerCoreConfig.enabled() || !PlayerCoreConfig.backEnabled() || !PlayerCoreConfig.allowBackAfterDeath()) {
            return;
        }
        if (player == null) {
            return;
        }
        if (PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).getOrCreate(player.getUUID()).lastDeathLocation().isPresent()) {
            tell(player, "echoplayercore.message.death_location_saved", ChatFormatting.YELLOW);
        }
    }

    private static String normalizeHomeName(String raw) {
        if (raw == null || raw.isBlank()) {
            return PlayerCoreConfig.defaultHomeName();
        }
        return PlayerCoreConfig.caseSensitiveHomeNames() ? raw.strip() : raw.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static int maxHomes(ServerPlayer player) {
        if (PlayerCoreConfig.opsBypassHomeLimit() && player.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return PlayerCoreConfig.maxHomesOp();
        }
        return PlayerCoreConfig.maxHomesDefault();
    }

    private static void tellCooldown(ServerPlayer player, TeleportAction action) {
        long ms = CooldownService.getCooldownRemaining(player, action);
        long sec = (ms + 999) / 1000;
        tell(player, "echoplayercore.message.cooldown", ChatFormatting.YELLOW, String.valueOf(sec));
    }

    private static void tell(ServerPlayer player, String key, ChatFormatting color, String... args) {
        Component msg = Component.translatable(key, (Object[]) args).withStyle(color);
        if (PlayerCoreConfig.useEchoStyleMessages()) {
            msg = Component.literal("[" + PlayerCoreConfig.messagePrefix() + "] ").withStyle(ChatFormatting.DARK_AQUA).append(msg);
        }
        player.sendSystemMessage(msg);
    }

    private static void tell(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }
}
