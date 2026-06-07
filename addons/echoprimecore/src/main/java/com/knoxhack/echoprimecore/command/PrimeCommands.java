package com.knoxhack.echoprimecore.command;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.integration.PrimeIntegrationLoader;
import com.knoxhack.echoprimecore.integration.PrimeIntegrationRegistry;
import com.knoxhack.echoprimecore.progression.PrimePlayerData;
import com.knoxhack.echoprimecore.progression.PrimeProgressionService;
import com.knoxhack.echoprimecore.service.PrimeRouteService;
import com.knoxhack.echoprimecore.service.PrimeWorldSignalService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class PrimeCommands {
    private PrimeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echoprimecore").then(primeRoot()));
        dispatcher.register(Commands.literal("echo").then(primeRoot()));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> primeRoot() {
        return Commands.literal("prime")
                .then(Commands.literal("audit").executes(PrimeCommands::audit))
                .then(Commands.literal("routes").executes(PrimeCommands::routes))
                .then(Commands.literal("flags").executes(PrimeCommands::flags))
                .then(Commands.literal("unlock")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("flag", StringArgumentType.string())
                                .executes(PrimeCommands::unlock)))
                .then(Commands.literal("reset")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PrimeCommands::reset)))
                .then(Commands.literal("stage")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PrimeCommands::stage)))
                .then(Commands.literal("debug")
                        .then(Commands.literal("nearby").executes(PrimeCommands::debugNearby)));
    }

    private static int audit(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PrimeIntegrationRegistry registry = PrimeIntegrationLoader.registry();
        send(source, "ECHO: Prime // Audit");
        send(source, "  Installed modules: " + installedModuleCount() + " ECHO modules loaded");
        send(source, "  Registered Prime integrations: " + EchoPrimeIntegrations.integrations().size());
        send(source, "  Routes: " + registry.routes().size());
        send(source, "  Mission chains: " + registry.missionChains().size());
        send(source, "  Index categories: " + registry.categories().size());
        send(source, "  Lens scan types: " + registry.scanTypes().size());
        send(source, "  HoloMap marker types: " + registry.markerTypes().size());
        send(source, "  Loot pools: " + registry.pools().size());
        send(source, "  Missing translations: " + missingTranslations(registry));
        send(source, "  Missing assets: " + missingAssets(registry));
        send(source, "  Orphaned recipes: " + orphanedRecipeHints(registry));
        send(source, "  Missing route cards: " + missingRouteCards(registry));
        send(source, "  Missing scan data: " + missingScanData(registry));
        send(source, "  Missing marker data: " + missingMarkerData(registry));
        send(source, "  Diagnostics: " + registry.diagnostics().size());
        return registry.routes().size();
    }

    private static int routes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PrimeIntegrationRegistry registry = PrimeIntegrationLoader.registry();
        send(source, "ECHO: Prime // Routes");
        for (PrimeRouteRegistry.PrimeRoute route : registry.routes()) {
            PrimeRouteService.RouteReadiness readiness = PrimeRouteService.readiness(route);
            send(source, "  - " + route.title()
                    + " | " + route.id()
                    + " | " + (readiness.ready() ? "ready" : "dormant")
                    + " | modules " + readiness.loadedModules() + "/" + readiness.requiredModules()
                    + (readiness.missingModules().isEmpty() ? "" : " | missing " + readiness.missingModules()));
        }
        return registry.routes().size();
    }

    private static int flags(CommandContext<CommandSourceStack> context) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        send(context.getSource(), "ECHO: Prime // Flags (" + data.flags().size() + ")");
        data.flags().stream().sorted().forEach(flag -> send(context.getSource(), "  - " + flag));
        if (data.flags().isEmpty()) {
            send(context.getSource(), "  None unlocked yet.");
        }
        return data.flags().size();
    }

    private static int unlock(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Identifier flag = parseFlag(StringArgumentType.getString(context, "flag"));
        boolean changed = PrimeProgressionService.unlock(player, flag);
        send(context.getSource(), "ECHO: Prime // " + (changed ? "Unlocked " : "Already unlocked ") + flag);
        return changed ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        PrimeProgressionService.reset(player);
        send(context.getSource(), "ECHO: Prime // Reset " + player.getName().getString());
        return 1;
    }

    private static int stage(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        send(context.getSource(), "ECHO: Prime // Stage for " + player.getName().getString());
        PrimeWorldSignalService.PrimeWorldMetadata metadata = PrimeWorldSignalService.metadata(player);
        send(context.getSource(), "  Stage: " + PrimeProgressionService.currentStage(player));
        send(context.getSource(), "  Objective: " + PrimeProgressionService.currentObjective(player));
        send(context.getSource(), "  World signal level: " + metadata.worldSignalLevel());
        send(context.getSource(), "  Region stability: " + metadata.regionStability());
        send(context.getSource(), "  Region danger: " + metadata.regionDanger());
        send(context.getSource(), "  Discovered structures: " + metadata.discoveredStructures());
        send(context.getSource(), "  Anomaly records: " + metadata.anomalyRecords());
        send(context.getSource(), "  Resource richness: " + metadata.resourceRichness());
        return 1;
    }

    private static int debugNearby(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PrimePlayerData data = PrimePlayerData.get(player);
        send(context.getSource(), "ECHO: Prime // Nearby Debug");
        send(context.getSource(), "  Player: " + player.blockPosition().toShortString());
        if (data.starterRelayPlaced()) {
            long dx = player.blockPosition().getX() - data.relayPos().getX();
            long dy = player.blockPosition().getY() - data.relayPos().getY();
            long dz = player.blockPosition().getZ() - data.relayPos().getZ();
            send(context.getSource(), "  Starter relay: " + data.relayPos().toShortString()
                    + " | distance " + Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)));
        } else {
            send(context.getSource(), "  Starter relay: not placed. Use a Crude Scanner.");
        }
        return 1;
    }

    private static Identifier parseFlag(String input) {
        Identifier parsed = Identifier.tryParse(input == null ? "" : input);
        if (parsed != null && parsed.getNamespace().equals(EchoPrimeCore.MODID)) {
            return parsed;
        }
        String path = input == null ? "" : input;
        if (path.startsWith(EchoPrimeCore.MODID + ":")) {
            path = path.substring((EchoPrimeCore.MODID + ":").length());
        }
        return EchoPrimeCore.id(path);
    }

    private static int installedModuleCount() {
        return (int) PrimeIntegrationLoader.registry().routes().stream()
                .flatMap(route -> route.requiredModules().stream())
                .filter(EchoRuntimeModules::isLoaded)
                .distinct()
                .count();
    }

    private static int missingTranslations(PrimeIntegrationRegistry registry) {
        return (int) registry.recipeHints().stream().filter(hint -> hint.title().isBlank()).count();
    }

    private static int missingAssets(PrimeIntegrationRegistry registry) {
        return (int) registry.markerTypes().stream().filter(marker -> marker.icon().isBlank()).count();
    }

    private static int orphanedRecipeHints(PrimeIntegrationRegistry registry) {
        return (int) registry.recipeHints().stream()
                .filter(hint -> registry.categories().stream().noneMatch(category -> category.id().equals(hint.categoryId())))
                .count();
    }

    private static int missingRouteCards(PrimeIntegrationRegistry registry) {
        int missing = 0;
        for (PrimeRouteRegistry.PrimeRoute route : registry.routes()) {
            boolean hasCard = false;
            for (PrimeTerminalRegistry.PrimeTerminalCard card : registry.cards()) {
                if (card.routeId().equals(route.id())) {
                    hasCard = true;
                    break;
                }
            }
            if (!hasCard) {
                missing++;
            }
        }
        return missing;
    }

    private static int missingScanData(PrimeIntegrationRegistry registry) {
        int missing = 0;
        for (PrimeLensRegistry.PrimeScanType scanType : registry.scanTypes()) {
            boolean hasData = registry.scanData().stream().anyMatch(data -> data.scanType().equals(scanType.id()));
            if (!hasData) {
                missing++;
            }
        }
        return missing;
    }

    private static int missingMarkerData(PrimeIntegrationRegistry registry) {
        int missing = 0;
        for (PrimeHoloMapRegistry.PrimeMarkerType markerType : registry.markerTypes()) {
            boolean hasLayer = registry.layers().stream().anyMatch(layer -> layer.id().equals(markerType.layerId()));
            if (!hasLayer) {
                missing++;
            }
        }
        return missing;
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
