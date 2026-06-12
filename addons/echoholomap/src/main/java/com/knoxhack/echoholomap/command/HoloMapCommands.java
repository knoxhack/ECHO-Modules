package com.knoxhack.echoholomap.command;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.map.HoloMapService;
import com.knoxhack.echoholomap.map.HoloMapTerrainScanner;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapTileBatchPacket;
import com.knoxhack.echoholomap.network.HoloMapTileRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapSync;
import com.knoxhack.echoholomap.world.HoloMapSavedData;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class HoloMapCommands {
    private HoloMapCommands() {
    }

    public static void register(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        // Player-facing subcommands
        dispatcher.register(Commands.literal("echoholomap")
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource().getPlayerOrException())))
                .then(Commands.literal("markers")
                        .executes(context -> playerMarkers(context.getSource().getPlayerOrException())))
                .then(Commands.literal("waypoints")
                        .executes(context -> waypoints(context.getSource().getPlayerOrException()))));

        // Gamemaster debug subcommands
        dispatcher.register(Commands.literal("echoholomap")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("debug")
                        .then(Commands.literal("add_marker")
                                .then(Commands.argument("layer", StringArgumentType.word())
                                        .executes(context -> addMarker(context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "layer")))))
                        .then(Commands.literal("add_route")
                                .executes(context -> addRoute(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("add_overlay")
                                .then(Commands.argument("layer", StringArgumentType.word())
                                        .executes(context -> addOverlay(context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "layer"))))
                                .executes(context -> addOverlay(context.getSource().getPlayerOrException(), "hazards")))
                        .then(Commands.literal("clear_markers")
                                .executes(context -> clearMarkers(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("scan_terrain")
                                .executes(context -> scanTerrain(context.getSource().getPlayerOrException(), configuredScanRadius()))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 24))
                                        .executes(context -> scanTerrain(context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "radius")))))
                        .then(Commands.literal("resample_terrain")
                                .executes(context -> resampleTerrain(context.getSource().getPlayerOrException(), configuredScanRadius()))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 24))
                                        .executes(context -> resampleTerrain(context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "radius")))))
                        .then(Commands.literal("clear_terrain")
                                .executes(context -> clearTerrain(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("dump_terrain")
                                .executes(context -> dumpTerrain(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("dump_snapshot")
                                .executes(context -> dumpSnapshot(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("dump")
                                .executes(context -> dump(context.getSource().getPlayerOrException())))));
    }

    private static int status(ServerPlayer player) {
        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(player);
        int providers = EchoCoreServices.mapMarkerService().providerCount();
        int richProviders = EchoCoreServices.mapMarkerService() instanceof HoloMapService service
                ? service.richProviderCount()
                : 0;
        int terrainTiles = 0;
        String terrainStats = "unavailable";
        if (player.level() instanceof ServerLevel serverLevel) {
            HoloMapTerrainSavedData terrain = HoloMapTerrainSavedData.get(serverLevel);
            terrainTiles = terrain.discoverableTileCount(player.getUUID(), serverLevel.dimension());
            terrainStats = terrain.stats(player.getUUID(), serverLevel.dimension()).summary();
        }
        int waypointCount = player.level().getServer() == null
                ? 0
                : HoloMapWaypointSavedData.get(player.level().getServer()).countFor(player.getUUID());
        player.sendSystemMessage(Component.literal("ECHO HoloMap // Status"));
        player.sendSystemMessage(Component.literal("  Map layers: " + snapshot.layers().size()));
        player.sendSystemMessage(Component.literal("  Visible markers: " + snapshot.markers().size()));
        player.sendSystemMessage(Component.literal("  Routes: " + snapshot.routes().size()));
        player.sendSystemMessage(Component.literal("  Overlays: " + snapshot.overlays().size()));
        player.sendSystemMessage(Component.literal("  Marker providers: " + providers));
        player.sendSystemMessage(Component.literal("  Rich providers: " + richProviders));
        player.sendSystemMessage(Component.literal("  Terrain tiles: " + terrainTiles + " (" + terrainStats + ")"));
        player.sendSystemMessage(Component.literal("  Waypoints: " + waypointCount));
        player.sendSystemMessage(Component.literal("  Diagnostics: " + snapshot.diagnostics().size()));
        player.sendSystemMessage(Component.literal("  Keys: [J] opens map, [K] toggles minimap, [ and ] zoom, [\\] moves corner"));
        return snapshot.markers().size();
    }

    private static int playerMarkers(ServerPlayer player) {
        var markers = HoloMapSnapshotPacket.from(player).markers();
        if (markers.isEmpty()) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // No visible markers."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("ECHO HoloMap // Visible markers (" + markers.size() + "):"));
        for (var marker : markers) {
            boolean field = marker.radius() > 0.0F && HoloMapVisibility.markerCanGenerateField(marker.kind());
            player.sendSystemMessage(Component.literal("  - " + marker.title()
                    + " | source " + marker.sourceId()
                    + " | layer " + marker.layerId().getPath().replace("layer/", "")
                    + " | kind " + marker.kind().name().toLowerCase()
                    + " | state " + marker.state().name().toLowerCase()
                    + " | radius " + Math.round(marker.radius())
                    + " | field " + (field ? "yes" : "no")
                    + " | xyz " + (int) marker.x() + " " + (int) marker.y() + " " + (int) marker.z()));
        }
        return markers.size();
    }

    private static int waypoints(ServerPlayer player) {
        if (player.level().getServer() == null) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Waypoint data is unavailable."));
            return 0;
        }
        var waypoints = HoloMapWaypointSavedData.get(player.level().getServer())
                .waypointsFor(player, configuredWaypointLimit());
        if (waypoints.isEmpty()) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // No personal waypoints set."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("ECHO HoloMap // Personal waypoints (" + waypoints.size() + "):"));
        for (var wp : waypoints) {
            player.sendSystemMessage(Component.literal("  - " + wp.title()
                    + " at (" + (int)wp.x() + ", " + (int)wp.y() + ", " + (int)wp.z() + ")"));
        }
        return waypoints.size();
    }

    private static int addMarker(ServerPlayer player, String layerInput) {
        if (!debugEnabled() || !(player.level() instanceof ServerLevel serverLevel)) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug markers are disabled."));
            return 0;
        }
        Identifier layer = HoloMapIds.layerFromInput(layerInput);
        HoloMapSavedData.get(serverLevel).addDebugMarker(player, layer);
        player.sendSystemMessage(Component.translatable("command.echoholomap.added"));
        HoloMapSync.send(player);
        return 1;
    }

    private static int addRoute(ServerPlayer player) {
        if (!debugEnabled() || !(player.level() instanceof ServerLevel serverLevel)) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug markers are disabled."));
            return 0;
        }
        int created = HoloMapSavedData.get(serverLevel).addDebugRoute(player).size();
        player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug route registered (" + created + " points)."));
        HoloMapSync.send(player);
        return created;
    }

    private static int addOverlay(ServerPlayer player, String layerInput) {
        if (!debugEnabled() || !(player.level() instanceof ServerLevel serverLevel)) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug markers are disabled."));
            return 0;
        }
        Identifier layer = HoloMapIds.layerFromInput(layerInput);
        HoloMapSavedData.get(serverLevel).addDebugOverlay(player, layer);
        player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug overlay registered."));
        HoloMapSync.send(player);
        return 1;
    }

    private static int clearMarkers(ServerPlayer player) {
        if (!debugEnabled() || !(player.level() instanceof ServerLevel serverLevel)) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug markers are disabled."));
            return 0;
        }
        int cleared = HoloMapSavedData.get(serverLevel).clearDebugMarkers();
        player.sendSystemMessage(Component.translatable("command.echoholomap.cleared")
                .append(Component.literal(" (" + cleared + ")")));
        HoloMapSync.send(player);
        return cleared;
    }

    private static int dump(ServerPlayer player) {
        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(player);
        int layers = snapshot.layers().size();
        int markers = snapshot.markers().size();
        int providers = EchoCoreServices.mapMarkerService().providerCount();
        player.sendSystemMessage(Component.translatable("command.echoholomap.dump", layers, markers, providers));
        HoloMapSync.send(player);
        return markers;
    }

    private static int dumpSnapshot(ServerPlayer player) {
        HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(player);
        player.sendSystemMessage(Component.literal("ECHO HoloMap // Snapshot"));
        player.sendSystemMessage(Component.literal("  Layers: " + snapshot.layers().size()
                + "/" + HoloMapSnapshotPacket.MAX_LAYERS));
        player.sendSystemMessage(Component.literal("  Markers: " + snapshot.markers().size()
                + "/" + HoloMapSnapshotPacket.maxMarkers()));
        player.sendSystemMessage(Component.literal("  Routes: " + snapshot.routes().size()
                + "/" + HoloMapSnapshotPacket.maxRoutes()));
        player.sendSystemMessage(Component.literal("  Overlays: " + snapshot.overlays().size()
                + "/" + HoloMapSnapshotPacket.maxOverlays()));
        player.sendSystemMessage(Component.literal("  Diagnostics: " + snapshot.diagnostics().size()
                + "/" + HoloMapSnapshotPacket.maxDiagnostics()));
        for (HoloMapSnapshotPacket.ProviderDiagnosticData diagnostic : snapshot.diagnostics()) {
            if (!diagnostic.healthy()) {
                player.sendSystemMessage(Component.literal("  ! " + diagnostic.providerId()
                        + " [" + diagnostic.providerType() + "]: " + diagnostic.message()
                        + " (" + diagnostic.failures() + " failure(s))"));
            }
        }
        return snapshot.markers().size();
    }

    private static int scanTerrain(ServerPlayer player, int radius) {
        if (!debugEnabled()) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug terrain commands are disabled."));
            return 0;
        }
        int safeRadius = Math.max(0, Math.min(24, radius));
        int maxChunks = Math.max(1, (safeRadius * 2 + 1) * (safeRadius * 2 + 1));
        int sampled = HoloMapTerrainScanner.scanAround(player, safeRadius, maxChunks);
        player.sendSystemMessage(Component.translatable("command.echoholomap.terrain_scanned", sampled, safeRadius));
        sendTerrainAround(player, safeRadius);
        return sampled;
    }

    private static int resampleTerrain(ServerPlayer player, int radius) {
        if (!debugEnabled()) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug terrain commands are disabled."));
            return 0;
        }
        int safeRadius = Math.max(0, Math.min(24, radius));
        int maxChunks = Math.max(1, (safeRadius * 2 + 1) * (safeRadius * 2 + 1));
        int sampled = HoloMapTerrainScanner.scanAround(player, safeRadius, maxChunks, true);
        player.sendSystemMessage(Component.translatable("command.echoholomap.terrain_resampled", sampled, safeRadius));
        sendTerrainAround(player, safeRadius);
        return sampled;
    }

    private static int clearTerrain(ServerPlayer player) {
        if (!debugEnabled() || !(player.level() instanceof ServerLevel serverLevel)) {
            player.sendSystemMessage(Component.literal("ECHO HoloMap // Debug terrain commands are disabled."));
            return 0;
        }
        int cleared = HoloMapTerrainSavedData.get(serverLevel).clear(player.getUUID());
        player.sendSystemMessage(Component.translatable("command.echoholomap.terrain_cleared", cleared));
        sendTerrainAround(player, configuredScanRadius());
        return cleared;
    }

    private static int dumpTerrain(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        int count = HoloMapTerrainSavedData.get(serverLevel)
                .discoverableTileCount(player.getUUID(), serverLevel.dimension());
        HoloMapTerrainSavedData.TerrainStats stats = HoloMapTerrainSavedData.get(serverLevel)
                .stats(player.getUUID(), serverLevel.dimension());
        player.sendSystemMessage(Component.translatable("command.echoholomap.terrain_dump", count,
                serverLevel.dimension().identifier().toString(), stats.summary()));
        sendTerrainAround(player, configuredScanRadius());
        return count;
    }

    private static void sendTerrainAround(ServerPlayer player, int radius) {
        HoloMapTileRequestPacket request = new HoloMapTileRequestPacket(
                player.level().dimension().identifier().toString(),
                Math.floorDiv(player.blockPosition().getX(), 16),
                Math.floorDiv(player.blockPosition().getZ(), 16),
                radius);
        EchoNetSend.toPlayer(player, HoloMapTileBatchPacket.from(player, request), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static boolean debugEnabled() {
        try {
            return Config.DEBUG_MARKERS.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static int configuredScanRadius() {
        try {
            return Math.max(0, Math.min(24, Config.TERRAIN_SCAN_RADIUS.get()));
        } catch (RuntimeException exception) {
            return 5;
        }
    }

    private static int configuredWaypointLimit() {
        try {
            return Math.max(16, Math.min(2048, Config.WAYPOINT_SYNC_LIMIT.get()));
        } catch (RuntimeException exception) {
            return 256;
        }
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> dispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> value
                    ? (CommandDispatcher<CommandSourceStack>) value
                    : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
