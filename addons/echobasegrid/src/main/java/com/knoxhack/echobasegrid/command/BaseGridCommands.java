package com.knoxhack.echobasegrid.command;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echobasegrid.api.ClaimActionResult;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.config.BaseGridConfig;
import com.knoxhack.echobasegrid.data.BaseGridSavedData;
import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class BaseGridCommands {
    private BaseGridCommands() {
    }

    public static void onRegisterCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            register(dispatcher, EchoBackendCommandEventBridge.buildContext(event));
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(root("basegrid"));
        dispatcher.register(root("echo_basegrid"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .executes(BaseGridCommands::status)
                .then(Commands.literal("status").executes(BaseGridCommands::status))
                .then(Commands.literal("claim").executes(BaseGridCommands::claim))
                .then(Commands.literal("unclaim").executes(BaseGridCommands::unclaim))
                .then(Commands.literal("inspect")
                        .executes(BaseGridCommands::inspectCurrent)
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .executes(BaseGridCommands::inspectChunk))));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) {
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        ChunkPos chunk = player.chunkPosition();
        String dimension = BaseGridClaimService.dimension(level);
        BaseGridSavedData data = BaseGridSavedData.get(level);
        int claimCount = data.claimCount(player.getUUID());
        int maxClaims = Math.max(0, BaseGridConfig.MAX_CLAIMS_PER_PLAYER.get());
        String state = stateFor(data.claim(dimension, chunk.x(), chunk.z()), player);
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.echobasegrid.status",
                BaseGridConfig.ENABLED.get(),
                dimension,
                chunk.x(),
                chunk.z(),
                state,
                claimCount,
                maxClaims), false);
        return 1;
    }

    private static int claim(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) {
            return 0;
        }
        ChunkPos chunk = player.chunkPosition();
        ClaimActionResult result = BaseGridClaimService.claim(player, BaseGridClaimService.dimension(player.level()),
                chunk.x(), chunk.z());
        sendResult(context.getSource(), result);
        return result.success() ? 1 : 0;
    }

    private static int unclaim(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) {
            return 0;
        }
        ChunkPos chunk = player.chunkPosition();
        ClaimActionResult result = BaseGridClaimService.unclaim(player, BaseGridClaimService.dimension(player.level()),
                chunk.x(), chunk.z());
        sendResult(context.getSource(), result);
        return result.success() ? 1 : 0;
    }

    private static int inspectCurrent(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) {
            return 0;
        }
        ChunkPos chunk = player.chunkPosition();
        return inspect(context.getSource(), player, chunk.x(), chunk.z());
    }

    private static int inspectChunk(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) {
            return 0;
        }
        return inspect(context.getSource(), player,
                IntegerArgumentType.getInteger(context, "chunkX"),
                IntegerArgumentType.getInteger(context, "chunkZ"));
    }

    private static int inspect(CommandSourceStack source, ServerPlayer player, int chunkX, int chunkZ) {
        ServerLevel level = (ServerLevel) player.level();
        String dimension = BaseGridClaimService.dimension(level);
        Optional<ClaimRecord> claim = BaseGridClaimService.claim(level, dimension, chunkX, chunkZ);
        if (claim.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.echobasegrid.inspect.unclaimed", dimension, chunkX, chunkZ), false);
            return 0;
        }
        ClaimRecord record = claim.get();
        String access = record.ownedBy(player.getUUID())
                ? "owner"
                : record.allows(player.getUUID(), ClaimPermission.MANAGE) ? "manager"
                : record.member(player.getUUID()).isPresent() ? "trusted" : "visitor";
        source.sendSuccess(() -> Component.translatable(
                "commands.echobasegrid.inspect.claimed",
                dimension,
                chunkX,
                chunkZ,
                record.ownerName(),
                record.members().size(),
                access), false);
        return 1;
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.echobasegrid.failure.player_required"));
        }
        return player;
    }

    private static void sendResult(CommandSourceStack source, ClaimActionResult result) {
        Component message = Component.translatable("commands.echobasegrid.action_result",
                result.title(), result.message());
        if (result.success()) {
            source.sendSuccess(() -> message, false);
        } else {
            source.sendFailure(message);
        }
    }

    private static String stateFor(Optional<ClaimRecord> claim, ServerPlayer player) {
        if (claim.isEmpty()) {
            return "unclaimed";
        }
        ClaimRecord record = claim.get();
        if (record.ownedBy(player.getUUID())) {
            return "owned";
        }
        if (record.member(player.getUUID()).isPresent()) {
            return "trusted";
        }
        return "occupied";
    }
}
