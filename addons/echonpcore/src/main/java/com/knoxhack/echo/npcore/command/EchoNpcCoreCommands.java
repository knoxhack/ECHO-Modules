package com.knoxhack.echo.npcore.command;

import com.knoxhack.echo.npcore.conversion.EchoNpcReplacementService;
import com.knoxhack.echo.npcore.data.NpcContactData;
import com.knoxhack.echo.npcore.data.NpcDataBridge;
import com.knoxhack.echo.npcore.diagnostics.EchoNpcCoreDiagnostics;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogue;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueManager;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueRuntime;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueNode;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.registry.ModEntities;
import com.knoxhack.echo.npcore.service.EchoNpcInteractionService;
import com.knoxhack.echo.npcore.service.NpcFactionBridge;
import com.knoxhack.echo.npcore.service.NpcMissionBridge;
import com.knoxhack.echo.npcore.service.EchoNpcServiceManager;
import com.knoxhack.echo.npcore.service.EchoNpcServiceSet;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeManager;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeSet;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.WorldContextSnapshot;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySpawnReason;

public final class EchoNpcCoreCommands {
    private EchoNpcCoreCommands() {
    }

    public static void register(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echonpcore")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("profileId", StringArgumentType.string())
                                .executes(context -> spawn(context.getSource(),
                                        parse(StringArgumentType.getString(context, "profileId"))))))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("list")
                        .then(Commands.literal("profiles").executes(context -> listProfiles(context.getSource()))))
                .then(Commands.literal("convert_nearby_villagers")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                .executes(context -> convertNearby(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("smoke")
                        .then(Commands.literal("open")
                                .then(Commands.argument("profileId", StringArgumentType.string())
                                        .executes(context -> smokeOpen(context.getSource(),
                                                parse(StringArgumentType.getString(context, "profileId"))))))
                        .then(Commands.literal("state")
                                .then(Commands.argument("profileId", StringArgumentType.string())
                                        .executes(context -> smokeState(context.getSource(),
                                                parse(StringArgumentType.getString(context, "profileId"))))))
                        .then(Commands.literal("integrations")
                                .then(Commands.argument("profileId", StringArgumentType.string())
                                        .executes(context -> smokeIntegrations(context.getSource(),
                                                parse(StringArgumentType.getString(context, "profileId"))))))
                        .then(Commands.literal("all")
                                .executes(context -> smokeAll(context.getSource()))))
                .then(Commands.literal("diagnose").executes(context -> diagnose(context.getSource()))));
    }

    private static int spawn(CommandSourceStack source, Identifier profileId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        EchoNpcEntity npc = ModEntities.ECHO_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (npc == null) {
            source.sendFailure(Component.literal("Could not create ECHO NPC."));
            return 0;
        }
        configureAndSpawn(player, npc, profileId);
        source.sendSuccess(() -> Component.literal("Spawned ECHO NPC with profile " + profileId + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandSourceStack source) {
        source.getServer().reloadResources(source.getServer().getPackRepository().getSelectedIds());
        source.sendSuccess(() -> Component.literal("NPCore content reload requested."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int listProfiles(CommandSourceStack source) {
        String ids = EchoNpcProfileManager.ids().stream()
                .limit(20)
                .map(Identifier::toString)
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("NPCore profiles (" + EchoNpcProfileManager.count() + "): "
                + (ids.isBlank() ? "none" : ids)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int convertNearby(ServerPlayer player, int radius) {
        int converted = EchoNpcReplacementService.convertNearbyVillagers(player, radius);
        player.sendSystemMessage(Component.literal("[NPCore] Converted " + converted + " vanilla NPC(s)."), true);
        return converted;
    }

    private static int diagnose(CommandSourceStack source) {
        EchoNpcCoreDiagnostics.reportLines()
                .forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int smokeOpen(CommandSourceStack source, Identifier profileId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EchoNpcEntity npc = nearbyNpc(player, profileId);
        if (npc == null) {
            npc = ModEntities.ECHO_NPC.get().create(player.level(), EntitySpawnReason.EVENT);
            if (npc == null) {
                source.sendFailure(Component.literal("Could not create ECHO NPC for smoke test."));
                return 0;
            }
            configureAndSpawn(player, npc, profileId);
            EchoNpcEntity created = npc;
            source.sendSuccess(() -> Component.literal("Smoke spawned NPC " + created.getId() + " for " + profileId + "."), false);
        } else {
            EchoNpcEntity found = npc;
            source.sendSuccess(() -> Component.literal("Smoke using nearby NPC " + found.getId() + " for " + profileId + "."), false);
        }
        EchoNpcInteractionService.open(player, npc);
        return Command.SINGLE_SUCCESS;
    }

    private static int smokeState(CommandSourceStack source, Identifier profileId) {
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(profileId);
        EchoNpcDialogue dialogue = EchoNpcDialogueManager.getOrFallback(profile.dialogue());
        String nodeId = EchoNpcDialogueRuntime.safeStart(dialogue);
        EchoNpcDialogueNode node = dialogue.nodeOrFallback(nodeId);
        EchoNpcTradeSet trades = EchoNpcTradeManager.getOrEmpty(profile.trades());
        EchoNpcServiceSet services = EchoNpcServiceManager.getOrEmpty(profile.services());
        int tradeCount = trades.groups().stream().mapToInt(group -> group.offers().size()).sum();
        int restockingTrades = trades.groups().stream()
                .mapToInt(group -> (int) group.offers().stream().filter(offer -> offer.restockTime() > 0).count())
                .sum();
        int gatedTrades = trades.groups().stream()
                .mapToInt(group -> (int) group.offers().stream()
                        .filter(offer -> !offer.requiresMission().isBlank()
                                || offer.requiresFactionStanding() != Integer.MIN_VALUE)
                        .count())
                .sum();
        int gatedOptions = (int) node.options().stream()
                .filter(option -> !option.requiresMission().isBlank()
                        || option.requiresFactionStanding() != Integer.MIN_VALUE)
                .count();
        int gatedServices = (int) services.services().stream()
                .filter(service -> !service.requiresMission().isBlank()
                        || service.requiresFactionStanding() != Integer.MIN_VALUE)
                .count();
        int cooldownServices = (int) services.services().stream().filter(service -> service.cooldown() > 0).count();
        ServerPlayer player = source.getPlayer();
        EchoNpcEntity nearby = player == null ? null : nearbyNpc(player, profile.id());
        source.sendSuccess(() -> Component.literal("NPCore smoke state: profile=" + profile.id()
                + ", displayName=" + profile.displayName()
                + ", dialogueNode=" + nodeId
                + ", options=" + node.options().size()
                + ", gatedOptions=" + gatedOptions
                + ", tradeGroups=" + trades.groups().size()
                + ", trades=" + tradeCount
                + ", restockingTrades=" + restockingTrades
                + ", gatedTrades=" + gatedTrades
                + ", services=" + services.services().size()
                + ", cooldownServices=" + cooldownServices
                + ", gatedServices=" + gatedServices
                + ", behavior=" + profile.behavior().mode()
                + ", wander=" + profile.behavior().wanderRadius()
                + ", return=" + profile.behavior().returnRadius()
                + ", ambientCooldown=" + profile.behavior().ambientCooldown()
                + ", home=" + homeSummary(nearby, player)
                + ", terminalContact=" + profile.integrations().terminalContact()
                + ", mapMarker=" + profile.integrations().mapMarker()
                + ", ScreenCore=" + EchoRuntimeModules.isLoaded("echoscreencore")
                + ", storage=" + NpcDataBridge.storageMode()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int smokeIntegrations(CommandSourceStack source, Identifier profileId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(profileId);
        WorldContextSnapshot worldContext = EchoCoreServices.worldRegions().worldContext(player);
        String region = worldContext.currentRegionOptional()
                .map(current -> current.displayName())
                .orElse("none");
        String hazard = worldContext.hazard().summary().isBlank() ? "none" : worldContext.hazard().summary();
        source.sendSuccess(() -> Component.literal("NPCore smoke integrations: profile=" + profile.id()
                + ", relationship=" + NpcFactionBridge.relationshipLabel(player, profile.faction())
                + ", faction=" + profile.faction()
                + ", missions=" + profile.missions().size()
                + ", availableMissions=" + NpcMissionBridge.availableMissions(player, profile.id()).size()
                + ", terminalContactVisible=" + NpcContactData.discovered(player, profile.id())
                + ", terminalContactHint=" + profile.integrations().terminalContact()
                + ", mapMarkerHint=" + profile.integrations().mapMarker()
                + ", worldRegion=" + region
                + ", worldHazard=" + hazard
                + ", terminal=" + EchoRuntimeModules.isLoaded("echoterminal")
                + ", mission=" + EchoRuntimeModules.isLoaded("echomissioncore")
                + ", world=" + EchoRuntimeModules.isLoaded("echoworldcore")
                + ", holomap=" + EchoRuntimeModules.isLoaded("echoholomap")
                + ", datacore=" + EchoRuntimeModules.isLoaded("echodatacore")
                + ", storage=" + NpcDataBridge.storageMode()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int smokeAll(CommandSourceStack source) {
        EchoNpcProfileManager.ids().stream()
                .sorted(java.util.Comparator.comparing(Identifier::toString))
                .forEach(profileId -> {
                    EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(profileId);
                    EchoNpcDialogue dialogue = EchoNpcDialogueManager.getOrFallback(profile.dialogue());
                    EchoNpcDialogueNode node = dialogue.nodeOrFallback(EchoNpcDialogueRuntime.safeStart(dialogue));
                    EchoNpcTradeSet trades = EchoNpcTradeManager.getOrEmpty(profile.trades());
                    EchoNpcServiceSet services = EchoNpcServiceManager.getOrEmpty(profile.services());
                    int tradeCount = trades.groups().stream().mapToInt(group -> group.offers().size()).sum();
                    String actions = services.services().stream()
                            .map(service -> service.action().isBlank() ? "noop" : service.action())
                            .distinct()
                            .collect(Collectors.joining("|"));
                    source.sendSuccess(() -> Component.literal("NPCore smoke profile: " + profile.id()
                            + " / options=" + node.options().size()
                            + " / trades=" + tradeCount
                            + " / services=" + services.services().size()
                            + " / behavior=" + profile.behavior().mode()
                            + " / ambient=" + profile.ambientLines().size()
                            + " / actions=" + (actions.isBlank() ? "none" : actions)), false);
                });
        return Command.SINGLE_SUCCESS;
    }

    private static EchoNpcEntity nearbyNpc(ServerPlayer player, Identifier profileId) {
        return player.level().getEntitiesOfClass(EchoNpcEntity.class, player.getBoundingBox().inflate(8.0D),
                        npc -> npc != null && npc.isAlive() && profileId.equals(npc.npcProfileId()))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static void configureAndSpawn(ServerPlayer player, EchoNpcEntity npc, Identifier profileId) {
        ServerLevel level = player.level();
        npc.setPos(player.getX(), player.getY(), player.getZ());
        npc.setYRot(player.getYRot());
        npc.setXRot(0.0F);
        npc.configureProfile(profileId);
        npc.setHome(player.blockPosition());
        level.addFreshEntity(npc);
    }

    private static String homeSummary(EchoNpcEntity npc, ServerPlayer player) {
        if (npc == null) {
            return "not_spawned_nearby";
        }
        BlockPos home = npc.homePos();
        double distance = player == null ? 0.0D : Math.sqrt(player.distanceToSqr(
                home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D));
        return home.getX() + "," + home.getY() + "," + home.getZ() + " dist=" + Math.round(distance * 10.0D) / 10.0D;
    }

    private static Identifier parse(String value) {
        Identifier id = Identifier.tryParse(value);
        return id == null ? Identifier.fromNamespaceAndPath("echonpcore", value.toLowerCase(java.util.Locale.ROOT)) : id;
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> value ? (CommandDispatcher<CommandSourceStack>) value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
