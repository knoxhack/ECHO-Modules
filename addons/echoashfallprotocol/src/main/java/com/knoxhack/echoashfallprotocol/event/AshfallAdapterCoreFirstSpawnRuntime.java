package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePacket;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeStructurePlacement;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.gameplay.AshfallInteractionRules;
import com.knoxhack.echoashfallprotocol.integration.AshfallWikiIntegration;
import com.knoxhack.echoashfallprotocol.network.WelcomeScreenPacket;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRuntimeMutationEvidence;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.world.StartingDropPodData;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Live Native Loader implementation of the AdapterCore first-spawn host calls.
 */
public final class AshfallAdapterCoreFirstSpawnRuntime {
    private static final String FIRST_JOIN_FLAG = "ashes_of_tomorrow.received_kit";
    private static final String QUEST_DROP_POD_INITIALIZED = "quest.dropPodInitialized";
    private static final Identifier FIND_DROP_POD_ADVANCEMENT =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "find_drop_pod");
    private static final Identifier TERMINAL_REMOTE_ITEM =
            Identifier.fromNamespaceAndPath("echoterminal", "echo_terminal_remote");
    private static final Identifier FIRST_MISSION =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "secure_crash_outpost");
    private static final String FIND_DROP_POD_CRITERION = "found_drop_pod";
    private static final int MIN_STARTING_POD_RADIUS_CHUNKS = 2;
    private static final int MAX_STARTING_POD_RADIUS_CHUNKS = 8;
    private static final int MIN_STARTING_POD_SPACING_CHUNKS = 3;
    private static final int CHUNK_SIZE = 16;
    private static final int STARTING_POD_CANDIDATE_ATTEMPTS = 16;
    private static final int STARTING_SURFACE_SEARCH_RADIUS = 24;
    private static final int STARTING_SURFACE_SEARCH_STEP = 4;
    private static final int STARTING_SURFACE_FALLBACK_SEARCH_RADIUS = 96;
    private static final int STARTING_SURFACE_FALLBACK_SEARCH_STEP = 8;
    private static final int MIN_SAFE_SURFACE_ABOVE_BOTTOM = 16;
    private static final int MIN_STARTING_SURFACE_Y = 48;
    private static final int MIN_STARTING_POD_RADIUS_BLOCKS = MIN_STARTING_POD_RADIUS_CHUNKS * CHUNK_SIZE;
    private static final int MAX_STARTING_POD_RADIUS_BLOCKS = MAX_STARTING_POD_RADIUS_CHUNKS * CHUNK_SIZE;
    private static final int MIN_STARTING_POD_SPACING_BLOCKS = MIN_STARTING_POD_SPACING_CHUNKS * CHUNK_SIZE;
    private static final Set<String> RUNTIME_ACTION_IDS = Set.of(
            "inventory.write_starter_note",
            "inventory.write_terminal_remote_if_loaded",
            "world.place_personal_drop_pod",
            "player.teleport_to_drop_pod_interior",
            "player.bind_drop_pod_respawn",
            "player.write_first_join_state.quest",
            "player.write_first_join_state.flag",
            "player.grant_find_drop_pod_advancement",
            "ui.dispatch_welcome_screen",
            "hud.publish_opening_recovery_notice",
            "hud.publish_mission_tracker_line",
            "hud.publish_hazard_weather_readout",
            "terminal.publish_first_ten_minutes_link",
            "wiki.publish_ashfall_guide_link",
            "lens.publish_opening_route_scan",
            "holomap.publish_opening_recovery_layers",
            "codex.publish_opening_route_entries",
            "recovery.publish_drop_pod_field_cache_context",
            "holomap.publish_recovery_route_markers",
            "weather.feed_route_hazard_context",
            "sound.map_weather_state_to_ambience_cues",
            "atmosphere.publish_visibility_particle_sky_fog_profile",
            "hud.feed_hazard_weather_readout",
            "repair.first_objective_state",
            "repair.grant_find_drop_pod_advancement",
            "repair.reissue_terminal_remote_if_loaded",
            "repair.replace_missing_or_invalid_drop_pod.structure",
            "repair.rescue_underground_or_missing_respawn.teleport",
            "repair.rescue_underground_or_missing_respawn.respawn",
            "repair.rescue_underground_or_missing_respawn.respawn_repair",
            "missioncore.start_secure_crash_outpost",
            "player.scanner_used",
            "native.ui.use_scanner",
            "player.inventory.grant",
            "native.ui.terminal_command",
            "native.ui.index_search",
            "native.ui.hud_refresh",
            "native.ui.mission_log_update",
            "native.ui.surface_open",
            "native.ui.index_bookmark",
            "native.ui.holomap_state",
            "native.ui.signalos_terminal",
            "native.ui.ashfall_drone_command");

    private AshfallAdapterCoreFirstSpawnRuntime() {
    }

    public static FirstSpawnRuntimeResult execute(ServerPlayer player) {
        return execute(player, true);
    }

    public static FirstSpawnRuntimeResult executeForGameTest(ServerPlayer player) {
        return execute(player, false);
    }

    private static FirstSpawnRuntimeResult execute(ServerPlayer player, boolean skipGameTestServer) {
        if (player == null) {
            return FirstSpawnRuntimeResult.skipped("invalid_player");
        }
        if (skipGameTestServer && isGameTestServer(player)) {
            return FirstSpawnRuntimeResult.skipped("gametest_server");
        }
        if (EchoRuntimeModules.isLoaded("echoprimecore")) {
            return FirstSpawnRuntimeResult.skipped("echoprimecore_loaded");
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return FirstSpawnRuntimeResult.skipped("server_level_missing");
        }

        NativeLoaderEchoRuntimeHost host = NativeLoaderRuntimeHostFactory.create(player, level);
        if (player.getPersistentData().getBoolean(FIRST_JOIN_FLAG).orElse(false)) {
            return repairExistingPlayer(player, level, host);
        }
        return spawnNewPlayer(player, level, host);
    }

    public static FirstSpawnRuntimeResult startMissionCoreFirstMission(ServerPlayer player) {
        if (player == null) {
            return FirstSpawnRuntimeResult.skipped("invalid_player");
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return FirstSpawnRuntimeResult.skipped("server_level_missing");
        }
        NativeLoaderEchoRuntimeHost host = NativeLoaderRuntimeHostFactory.create(player, level);
        NativeResult result = startMissionCoreFirstMission(player, host);
        return FirstSpawnRuntimeResult.from("missioncore_first_mission", List.of(result), Map.of(
                "playerId", player.getUUID().toString(),
                "missionId", FIRST_MISSION.toString()));
    }

    private static FirstSpawnRuntimeResult spawnNewPlayer(
            ServerPlayer player,
            ServerLevel level,
            NativeLoaderEchoRuntimeHost host) {
        List<NativeResult> results = new ArrayList<>();
        NativePlayerRef playerRef = host.playerRef();

        NativeMutationContext starterNoteContext = host.context(
                "inventory.write_starter_note",
                "EchoNativeRuntimeHost.PlayerInventory",
                "grant");
        results.add(dispatchHostAction(
                host,
                "inventory.write_starter_note",
                Map.of("item", "echoashfallprotocol:field_manual", "count", 1),
                playerRef,
                null,
                null,
                starterNoteContext,
                true,
                false,
                runtimeHost -> runtimeHost.playerInventory().grant(
                        playerRef,
                        new NativeItemStack("echoashfallprotocol:field_manual", 1, Map.of(
                                "customNameKey", "item.EchoAshfallProtocol.echo_starter_note.name",
                                "message", starterNoteMessage())),
                        starterNoteContext)));
        results.add(giveTerminalRemoteIfAvailable(host, playerRef, "inventory.write_terminal_remote_if_loaded"));

        StartingDropPodData podData = StartingDropPodData.get(level);
        Optional<StartingDropPodData.Entry> existingPod = podData.findForPlayer(player.getUUID())
                .filter(AshfallAdapterCoreFirstSpawnRuntime::isReusableStartingPod);
        BlockPos origin = existingPod.map(StartingDropPodData.Entry::origin)
                .orElseGet(() -> findStartingPodOrigin(level, podData));
        NativeResult structureResult = existingPod
                .map(entry -> reusedDropPodResult(entry, host.context(
                        "world.place_personal_drop_pod",
                        "EchoNativeRuntimeHost.Structures",
                        "placeStructure")))
                .orElseGet(() -> {
                    NativeMutationContext context = host.context(
                            "world.place_personal_drop_pod",
                            "EchoNativeRuntimeHost.Structures",
                            "placeStructure");
                    return dispatchHostAction(
                            host,
                            "world.place_personal_drop_pod",
                            Map.of(
                                    "structure", "echoashfallprotocol:drop_pod",
                                    "origin", positionSnapshot(origin)),
                            playerRef,
                            new NativePosition(host.dimensionId(), origin.getX(), origin.getY(), origin.getZ(), 0.0F, 0.0F),
                            null,
                            context,
                            true,
                            false,
                            runtimeHost -> runtimeHost.structures().placeStructure(
                                    new NativeStructurePlacement(
                                            "echoashfallprotocol:drop_pod",
                                            host.dimensionId(),
                                            origin.getX(),
                                            origin.getY(),
                                            origin.getZ(),
                                            "personal_starting_drop_pod.interior",
                                            Map.of("minimumStartingSurfaceY", MIN_STARTING_SURFACE_Y)),
                                    context));
                });
        results.add(structureResult);

        BlockPos interior = existingPod.map(StartingDropPodData.Entry::interior)
                .orElseGet(() -> blockPosFromSnapshot(structureResult.snapshot(), "interior"));
        if (interior != null) {
            podData.addOrReplace(player.getUUID(), origin, interior);
            NativePosition interiorPosition = new NativePosition(
                    host.dimensionId(),
                    interior.getX() + 0.5,
                    interior.getY(),
                    interior.getZ() + 0.5,
                    0.0F,
                    0.0F);
            NativeMutationContext teleportContext = host.context(
                    "player.teleport_to_drop_pod_interior",
                    "EchoNativeRuntimeHost.PlayerState",
                    "teleport");
            results.add(dispatchHostAction(
                    host,
                    "player.teleport_to_drop_pod_interior",
                    Map.of("position", positionSnapshot(interior)),
                    playerRef,
                    interiorPosition,
                    null,
                    teleportContext,
                    true,
                    false,
                    runtimeHost -> runtimeHost.playerState().teleport(playerRef, interiorPosition, teleportContext)));
            NativePosition respawnPosition = new NativePosition(
                    host.dimensionId(),
                    interior.getX(),
                    interior.getY(),
                    interior.getZ(),
                    0.0F,
                    0.0F);
            NativeMutationContext respawnContext = host.context(
                    "player.bind_drop_pod_respawn",
                    "EchoNativeRuntimeHost.PlayerState",
                    "bindRespawn");
            results.add(dispatchHostAction(
                    host,
                    "player.bind_drop_pod_respawn",
                    Map.of("respawn", positionSnapshot(interior), "forced", true),
                    playerRef,
                    respawnPosition,
                    null,
                    respawnContext,
                    true,
                    false,
                    runtimeHost -> runtimeHost.playerState().bindRespawn(playerRef, respawnPosition, true, respawnContext)));
            NativeMutationContext questContext = host.context(
                    "player.write_first_join_state.quest",
                    "EchoNativeRuntimeHost.PlayerState",
                    "writePersistentState");
            results.add(dispatchHostAction(
                    host,
                    "player.write_first_join_state.quest",
                    Map.of("key", QUEST_DROP_POD_INITIALIZED, "value", true),
                    playerRef,
                    null,
                    null,
                    questContext,
                    true,
                    false,
                    runtimeHost -> runtimeHost.playerState().writePersistentState(
                            playerRef,
                            QUEST_DROP_POD_INITIALIZED,
                            true,
                            questContext)));
            NativeMutationContext advancementContext = host.context(
                    "player.grant_find_drop_pod_advancement",
                    "EchoNativeRuntimeHost.PlayerState",
                    "grantAdvancement");
            results.add(dispatchHostAction(
                    host,
                    "player.grant_find_drop_pod_advancement",
                    Map.of("advancement", FIND_DROP_POD_ADVANCEMENT.toString(), "criterion", FIND_DROP_POD_CRITERION),
                    playerRef,
                    null,
                    null,
                    advancementContext,
                    true,
                    false,
                    runtimeHost -> runtimeHost.playerState().grantAdvancement(
                            playerRef,
                            FIND_DROP_POD_ADVANCEMENT.toString(),
                            FIND_DROP_POD_CRITERION,
                            advancementContext)));
            EchoAshfallProtocol.LOGGER.info(
                    "Spawned {} inside personal starting drop pod at {} from origin {} through AdapterCore first-spawn runtime.",
                    player.getName().getString(),
                    interior,
                    origin);
        }

        NativeMutationContext firstJoinFlagContext = host.context(
                "player.write_first_join_state.flag",
                "EchoNativeRuntimeHost.PlayerState",
                "writePersistentState");
        results.add(dispatchHostAction(
                host,
                "player.write_first_join_state.flag",
                Map.of("key", FIRST_JOIN_FLAG, "value", true),
                playerRef,
                null,
                null,
                firstJoinFlagContext,
                true,
                false,
                runtimeHost -> runtimeHost.playerState().writePersistentState(
                        playerRef,
                        FIRST_JOIN_FLAG,
                        true,
                        firstJoinFlagContext)));
        results.add(startMissionCoreFirstMission(player, host));
        NativeMutationContext packetContext = host.context(
                "ui.dispatch_welcome_screen",
                "EchoNativeRuntimeHost.Packets",
                "sendToPlayer");
        NativePacket welcomePacket = new NativePacket(
                "echoashfallprotocol:welcome_screen",
                playerRef,
                "CLIENTBOUND_SYNC",
                Map.of("packet", "WelcomeScreenPacket"));
        results.add(dispatchHostAction(
                host,
                "ui.dispatch_welcome_screen",
                Map.of("packet", "WelcomeScreenPacket"),
                playerRef,
                null,
                null,
                packetContext,
                false,
                true,
                runtimeHost -> runtimeHost.packets().sendToPlayer(welcomePacket, packetContext)));
        NativeMutationContext hudContext = host.context(
                "hud.publish_opening_recovery_notice",
                "EchoNativeRuntimeHost.Hud",
                "publishNotification");
        Map<String, Object> hudPayload = Map.of(
                "lineKeys", List.of(
                        "message.EchoAshfallProtocol.starting.line",
                        "message.EchoAshfallProtocol.starting.kit",
                        "message.EchoAshfallProtocol.starting.buffer",
                        "message.EchoAshfallProtocol.starting.line"),
                "anchor", "chat_and_hud");
        results.add(dispatchHostAction(
                host,
                "hud.publish_opening_recovery_notice",
                hudPayload,
                playerRef,
                null,
                null,
                hudContext,
                false,
                true,
                runtimeHost -> runtimeHost.hud().publishNotification(playerRef, hudPayload, hudContext)));
        results.addAll(publishFirstJoinSurfaceHostState(host, player, playerRef, origin, interior));

        FirstSpawnRuntimeResult result = FirstSpawnRuntimeResult.from("new_player_first_spawn", results, Map.of(
                "playerId", player.getUUID().toString(),
                "dropPodOrigin", positionSnapshot(origin),
                "dropPodInterior", interior == null ? Map.of() : positionSnapshot(interior),
                "receivedKit", player.getPersistentData().getBoolean(FIRST_JOIN_FLAG).orElse(false)));
        return result;
    }

    private static List<NativeResult> publishFirstJoinSurfaceHostState(
            NativeLoaderEchoRuntimeHost host,
            ServerPlayer player,
            NativePlayerRef playerRef,
            BlockPos origin,
            BlockPos interior) {
        List<NativeResult> results = new ArrayList<>();
        Map<String, Object> originSnapshot = origin == null ? Map.of() : positionSnapshot(origin);
        Map<String, Object> interiorSnapshot = interior == null ? Map.of() : positionSnapshot(interior);
        List<String> recoveryLayers = List.of(
                "echoashfallprotocol:first_month_field_intel",
                "echoashfallprotocol:first_major_route");

        Map<String, Object> recoveryPayload = payload(
                "playerId", player.getUUID().toString(),
                "deathRecoveryHandoff", "echorecovery:field_cache_service",
                "routeObjective", "ashfall:recover_crash_cache",
                "recoveryCompassVisible", true,
                "graveContextSource", "first_join_drop_pod",
                "dropPodOrigin", originSnapshot,
                "dropPodInterior", interiorSnapshot,
                "fieldCacheVisibility", true);
        results.add(saveDataHostAction(
                host,
                "recovery.publish_drop_pod_field_cache_context",
                "echorecovery:field_cache_service",
                player.getUUID().toString(),
                recoveryPayload,
                playerRef));

        Map<String, Object> holomapPayload = payload(
                "playerId", player.getUUID().toString(),
                "layers", recoveryLayers,
                "fieldCacheVisibility", true,
                "compassRouteSource", "ashfall:recover_crash_cache",
                "dropPodOrigin", originSnapshot,
                "dropPodInterior", interiorSnapshot);
        results.add(routeStateHostAction(
                host,
                "holomap.publish_recovery_route_markers",
                "echoholomap:recovery_route_markers:" + player.getUUID(),
                holomapPayload,
                playerRef));
        results.add(routeStateHostAction(
                host,
                "holomap.publish_opening_recovery_layers",
                "echoholomap:opening_recovery_layers",
                holomapPayload,
                playerRef));

        results.add(hudHostAction(
                host,
                "hud.publish_mission_tracker_line",
                payload("message", "Place an Ash Campfire near the crash site", "anchor", "mission_tracker"),
                playerRef));
        results.add(hudHostAction(
                host,
                "hud.publish_hazard_weather_readout",
                payload("message", "AIR stable; hazards marked.", "anchor", "hazard_readout", "source", "first_join"),
                playerRef));

        results.add(packetHostAction(
                host,
                "terminal.publish_first_ten_minutes_link",
                "echoterminal:first_ten_minutes_card",
                payload("message", "Terminal card ready: first ten minutes.", "card", "ashfall:first_ten_minutes"),
                playerRef));
        results.add(packetHostAction(
                host,
                "wiki.publish_ashfall_guide_link",
                "echowiki:ashfall",
                payload("message", "Wiki guide available: Ashfall field manual.", "guide", "echowiki:ashfall"),
                playerRef));
        results.add(packetHostAction(
                host,
                "lens.publish_opening_route_scan",
                "echolens:opening_route_scan",
                payload("message", "Lens scan profile ready for the opening recovery route.",
                        "profiles", List.of("echoashfallprotocol:drop_pod", "echoashfallprotocol:survivor_cache")),
                playerRef));
        results.add(packetHostAction(
                host,
                "codex.publish_opening_route_entries",
                "echocodexcore:opening_entries",
                payload("message", "Codex entries unlocked for crash recovery.",
                        "entries", List.of("ashfall:first_minutes", "ashfall:recovery_cache", "ashfall:first_major_route")),
                playerRef));

        Map<String, Object> weatherPayload = payload(
                "routeId", "echoashfallprotocol:opening_route",
                "line", "AIR stable; hazards marked.",
                "screenSafe", true,
                "hazards", List.of("ashfall_opening_route_haze"));
        results.add(weatherStateHostAction(
                host,
                "weather.feed_route_hazard_context",
                "echoashfallprotocol:route_hazard_context",
                weatherPayload,
                playerRef));
        results.add(routeStateHostAction(
                host,
                "sound.map_weather_state_to_ambience_cues",
                "echosoundcore:ambience",
                payload("routeId", "echoashfallprotocol:opening_route",
                        "ambienceCue", "ashfall_opening_route",
                        "source", "echoashfallprotocol:route_hazard_context"),
                playerRef));
        results.add(weatherStateHostAction(
                host,
                "atmosphere.publish_visibility_particle_sky_fog_profile",
                "echoatmospherecore:storm_visibility",
                payload("profile", "echoatmospherecore:storm_visibility",
                        "fog", "ashfall_opening_route_haze",
                        "skyFog", "ashfall_opening_route_sky_fog",
                        "screenHazeIntensity", 0.18D,
                        "stormVisibility", 0.62D,
                        "particleProfile", "echoashfallprotocol:opening_route_ash_particles",
                        "particleDensity", 0.2D),
                playerRef));
        results.add(hudHostAction(
                host,
                "hud.feed_hazard_weather_readout",
                payload("message", "AIR stable; hazards marked.",
                        "source", "echoashfallprotocol:route_hazard_context",
                        "anchor", "hazard_weather_readout"),
                playerRef));
        return List.copyOf(results);
    }

    private static NativeResult saveDataHostAction(
            NativeLoaderEchoRuntimeHost host,
            String actionId,
            String scope,
            String key,
            Map<String, Object> payload,
            NativePlayerRef playerRef) {
        NativeMutationContext context = host.context(actionId, "EchoNativeRuntimeHost.SaveData", "write");
        return dispatchHostAction(
                host,
                actionId,
                payload,
                playerRef,
                null,
                null,
                context,
                true,
                false,
                runtimeHost -> runtimeHost.saveData().write(new NativeSaveData(scope, key, payload), context));
    }

    private static NativeResult routeStateHostAction(
            NativeLoaderEchoRuntimeHost host,
            String actionId,
            String routeId,
            Map<String, Object> payload,
            NativePlayerRef playerRef) {
        NativeMutationContext context = host.context(actionId, "EchoNativeRuntimeHost.WorldState", "writeRouteState");
        return dispatchHostAction(
                host,
                actionId,
                payload,
                playerRef,
                null,
                null,
                context,
                true,
                false,
                runtimeHost -> runtimeHost.worldState().writeRouteState(routeId, payload, context));
    }

    private static NativeResult weatherStateHostAction(
            NativeLoaderEchoRuntimeHost host,
            String actionId,
            String stateId,
            Map<String, Object> payload,
            NativePlayerRef playerRef) {
        NativeMutationContext context = host.context(actionId, "EchoNativeRuntimeHost.WorldState", "writeWeatherState");
        return dispatchHostAction(
                host,
                actionId,
                payload,
                playerRef,
                null,
                null,
                context,
                true,
                false,
                runtimeHost -> runtimeHost.worldState().writeWeatherState(stateId, payload, context));
    }

    private static NativeResult hudHostAction(
            NativeLoaderEchoRuntimeHost host,
            String actionId,
            Map<String, Object> payload,
            NativePlayerRef playerRef) {
        NativeMutationContext context = host.context(actionId, "EchoNativeRuntimeHost.Hud", "publishNotification");
        return dispatchHostAction(
                host,
                actionId,
                payload,
                playerRef,
                null,
                null,
                context,
                false,
                true,
                runtimeHost -> runtimeHost.hud().publishNotification(playerRef, payload, context));
    }

    private static NativeResult packetHostAction(
            NativeLoaderEchoRuntimeHost host,
            String actionId,
            String packetId,
            Map<String, Object> payload,
            NativePlayerRef playerRef) {
        NativeMutationContext context = host.context(actionId, "EchoNativeRuntimeHost.Packets", "sendToPlayer");
        NativePacket packet = new NativePacket(packetId, playerRef, "CLIENTBOUND_SYNC", payload);
        return dispatchHostAction(
                host,
                actionId,
                payload,
                playerRef,
                null,
                null,
                context,
                false,
                true,
                runtimeHost -> runtimeHost.packets().sendToPlayer(packet, context));
    }

    private static FirstSpawnRuntimeResult repairExistingPlayer(
            ServerPlayer player,
            ServerLevel level,
            NativeLoaderEchoRuntimeHost host) {
        List<NativeResult> results = new ArrayList<>();
        ReturningPodRepair podRepair = repairReturningPlayerDropPod(player, level, host, results);
        results.add(repairFirstObjectiveState(player, host));
        NativeMutationContext advancementContext = host.context(
                "repair.grant_find_drop_pod_advancement",
                "EchoNativeRuntimeHost.PlayerState",
                "grantAdvancement");
        results.add(dispatchHostAction(
                host,
                "repair.grant_find_drop_pod_advancement",
                Map.of("advancement", FIND_DROP_POD_ADVANCEMENT.toString(), "criterion", FIND_DROP_POD_CRITERION),
                host.playerRef(),
                null,
                null,
                advancementContext,
                true,
                false,
                runtimeHost -> runtimeHost.playerState().grantAdvancement(
                        host.playerRef(),
                        FIND_DROP_POD_ADVANCEMENT.toString(),
                        FIND_DROP_POD_CRITERION,
                        advancementContext)));
        results.add(startMissionCoreFirstMission(player, host));
        results.add(giveTerminalRemoteIfAvailable(
                host,
                host.playerRef(),
                "repair.reissue_terminal_remote_if_loaded"));

        FirstSpawnRuntimeResult result = FirstSpawnRuntimeResult.from("returning_player_repair", results, Map.of(
                "playerId", player.getUUID().toString(),
                "receivedKit", true,
                "dropPodRepairReason", podRepair.reason(),
                "dropPodReplaced", podRepair.replaced(),
                "undergroundPodRescued", podRepair.teleported(),
                "respawnRepaired", podRepair.respawnRepaired(),
                "respawnBound", player.getRespawnConfig() != null));
        return result;
    }

    private static NativeResult startMissionCoreFirstMission(ServerPlayer player, NativeLoaderEchoRuntimeHost host) {
        NativeMutationContext context = host.context(
                "missioncore.start_secure_crash_outpost",
                "EchoCoreServices",
                "startMission");
        return dispatchHostAction(
                host,
                "missioncore.start_secure_crash_outpost",
                Map.of("missionId", FIRST_MISSION.toString()),
                host.playerRef(),
                null,
                null,
                context,
                true,
                false,
                runtimeHost -> applyStartMissionCoreFirstMission(player, context));
    }

    private static NativeResult applyStartMissionCoreFirstMission(ServerPlayer player, NativeMutationContext context) {
        if (!EchoCoreServices.missionCoreAvailable()) {
            return result(false, "SKIPPED_OPTIONAL_MODULE_MISSING", "MissionCore is not loaded.", context, Map.of(
                    "module", "echomissioncore"));
        }
        boolean started = EchoCoreServices.startMission(player, FIRST_MISSION);
        return result(started, started ? "MUTATED" : "SKIPPED_MISSION_NOT_READY",
                started
                        ? "Started the opening Ashfall MissionCore route mission."
                        : "Opening Ashfall MissionCore route mission was unavailable or already started.",
                context,
                Map.of("missionId", FIRST_MISSION.toString()));
    }

    private static ReturningPodRepair repairReturningPlayerDropPod(
            ServerPlayer player,
            ServerLevel level,
            NativeLoaderEchoRuntimeHost host,
            List<NativeResult> results) {
        StartingDropPodData podData = StartingDropPodData.get(level);
        Optional<StartingDropPodData.Entry> savedPod = podData.findForPlayer(player.getUUID());
        Optional<StartingDropPodData.Entry> reusablePod = savedPod
                .filter(entry -> isReusableStartingPod(level, entry));
        if (reusablePod.isPresent()) {
            boolean repairedRespawn = false;
            if (player.getRespawnConfig() == null) {
                NativeResult repairResult = repairMissingDropPodRespawn(player, level, host);
                repairedRespawn = repairResult.mutated();
                results.add(repairResult);
            }
            return new ReturningPodRepair(
                    false,
                    false,
                    repairedRespawn,
                    repairedRespawn ? "missing_respawn" : "already_reusable",
                    reusablePod.get().interior());
        }

        String repairReason = savedPod.isPresent() ? "invalid_saved_drop_pod" : "missing_saved_drop_pod";
        BlockPos origin = findStartingPodOrigin(level, podData);
        NativeMutationContext structureContext = host.context(
                "repair.replace_missing_or_invalid_drop_pod.structure",
                "EchoNativeRuntimeHost.Structures",
                "placeStructure");
        NativeResult structureResult = dispatchHostAction(
                host,
                "repair.replace_missing_or_invalid_drop_pod.structure",
                Map.of(
                        "structure", "echoashfallprotocol:drop_pod",
                        "origin", positionSnapshot(origin),
                        "repair", repairReason),
                host.playerRef(),
                new NativePosition(host.dimensionId(), origin.getX(), origin.getY(), origin.getZ(), 0.0F, 0.0F),
                null,
                structureContext,
                true,
                false,
                runtimeHost -> runtimeHost.structures().placeStructure(
                        new NativeStructurePlacement(
                                "echoashfallprotocol:drop_pod",
                                host.dimensionId(),
                                origin.getX(),
                                origin.getY(),
                                origin.getZ(),
                                "personal_starting_drop_pod.interior",
                                Map.of("repair", repairReason)),
                        structureContext));
        results.add(structureResult);

        BlockPos interior = blockPosFromSnapshot(structureResult.snapshot(), "interior");
        if (interior == null) {
            return new ReturningPodRepair(false, false, false, repairReason + "_failed", null);
        }

        podData.addOrReplace(player.getUUID(), origin, interior);
        boolean shouldTeleport = player.blockPosition().getY() < MIN_STARTING_SURFACE_Y
                || (savedPod.isPresent() && !isReusableStartingPod(savedPod.get()));
        if (shouldTeleport) {
            NativePosition teleportPosition = new NativePosition(
                    host.dimensionId(),
                    interior.getX() + 0.5,
                    interior.getY(),
                    interior.getZ() + 0.5,
                    0.0F,
                    0.0F);
            NativeMutationContext teleportContext = host.context(
                    "repair.rescue_underground_or_missing_respawn.teleport",
                    "EchoNativeRuntimeHost.PlayerState",
                    "teleport");
            results.add(dispatchHostAction(
                    host,
                    "repair.rescue_underground_or_missing_respawn.teleport",
                    Map.of("position", positionSnapshot(interior), "repair", repairReason),
                    host.playerRef(),
                    teleportPosition,
                    null,
                    teleportContext,
                    true,
                    false,
                    runtimeHost -> runtimeHost.playerState().teleport(host.playerRef(), teleportPosition, teleportContext)));
        }
        NativePosition respawnPosition = new NativePosition(
                host.dimensionId(),
                interior.getX(),
                interior.getY(),
                interior.getZ(),
                0.0F,
                0.0F);
        NativeMutationContext respawnContext = host.context(
                "repair.rescue_underground_or_missing_respawn.respawn",
                "EchoNativeRuntimeHost.PlayerState",
                "bindRespawn");
        results.add(dispatchHostAction(
                host,
                "repair.rescue_underground_or_missing_respawn.respawn",
                Map.of("respawn", positionSnapshot(interior), "repair", repairReason, "forced", true),
                host.playerRef(),
                respawnPosition,
                null,
                respawnContext,
                true,
                false,
                runtimeHost -> runtimeHost.playerState().bindRespawn(host.playerRef(), respawnPosition, true, respawnContext)));
        player.sendSystemMessage(Component.literal(shouldTeleport
                ? "\u00A7b[ECHO-7]\u00A7r Unsafe or invalid crash site detected. Relocating to surface pod."
                : "\u00A7b[ECHO-7]\u00A7r Missing crash pod record repaired. Respawn rebound to a safe surface pod."));
        EchoAshfallProtocol.LOGGER.warn(
                "Repaired {} returning-player starting drop pod for {} at {} from origin {} through AdapterCore first-spawn runtime.",
                repairReason,
                player.getName().getString(),
                interior,
                origin);
        return new ReturningPodRepair(true, shouldTeleport, true, repairReason, interior);
    }

    private static NativeResult repairMissingDropPodRespawn(
            ServerPlayer player,
            ServerLevel level,
            NativeLoaderEchoRuntimeHost host) {
        NativeMutationContext context = host.context(
                "repair.rescue_underground_or_missing_respawn.respawn_repair",
                "EchoNativeRuntimeHost.PlayerState",
                "bindRespawn");
        if (player.getRespawnConfig() != null) {
            return result(false, "SKIPPED_RESPAWN_ALREADY_BOUND", "Drop-pod respawn repair was already satisfied.", context, Map.of());
        }

        Optional<StartingDropPodData.Entry> existingPod = StartingDropPodData.get(level).findForPlayer(player.getUUID())
                .filter(AshfallAdapterCoreFirstSpawnRuntime::isReusableStartingPod);
        if (existingPod.isEmpty()) {
            return result(false, "SKIPPED_NO_REUSABLE_POD", "No reusable drop pod existed for respawn repair.", context, Map.of());
        }

        BlockPos interior = existingPod.get().interior();
        NativePosition respawnPosition = new NativePosition(
                host.dimensionId(),
                interior.getX(),
                interior.getY(),
                interior.getZ(),
                0.0F,
                0.0F);
        return dispatchHostAction(
                host,
                "repair.rescue_underground_or_missing_respawn.respawn_repair",
                Map.of("respawn", positionSnapshot(interior), "forced", false),
                host.playerRef(),
                respawnPosition,
                null,
                context,
                true,
                false,
                runtimeHost -> runtimeHost.playerState().bindRespawn(host.playerRef(), respawnPosition, false, context));
    }

    private static NativeResult repairFirstObjectiveState(ServerPlayer player, NativeLoaderEchoRuntimeHost host) {
        NativeMutationContext context = host.context(
                "repair.first_objective_state",
                "EchoNativeRuntimeHost.PlayerState",
                "writePersistentState");
        return dispatchHostAction(
                host,
                "repair.first_objective_state",
                Map.of("key", QUEST_DROP_POD_INITIALIZED, "repairMissionState", true),
                host.playerRef(),
                null,
                null,
                context,
                true,
                false,
                runtimeHost -> applyRepairFirstObjectiveState(player, context));
    }

    private static NativeResult applyRepairFirstObjectiveState(ServerPlayer player, NativeMutationContext context) {
        QuestData quest = player.getData(ModAttachments.QUEST_DATA.get());
        boolean wasDropPodInitialized = quest.isDropPodInitialized();
        if (!wasDropPodInitialized) {
            quest.setDropPodInitialized(true);
        }
        boolean missionStateChanged = quest.repairMissionState(player);
        boolean changed = !wasDropPodInitialized || missionStateChanged;
        if (changed) {
            QuestData.saveAndSync(player, quest);
        }
        return result(changed, changed ? "MUTATED" : "SKIPPED_ALREADY_REPAIRED",
                changed
                        ? "Repaired QuestData first objective and drop-pod initialization state."
                        : "QuestData first objective and drop-pod initialization state were already repaired.",
                context,
                Map.of(
                        "dropPodInitialized", quest.isDropPodInitialized(),
                        "currentPhase", quest.getCurrentPhase(),
                        "currentMissionIndex", quest.getCurrentMissionIndex()));
    }

    private static NativeResult giveTerminalRemoteIfAvailable(
            NativeLoaderEchoRuntimeHost host,
            NativePlayerRef playerRef,
            String operationId) {
        NativeMutationContext context = host.context(
                operationId,
                "EchoNativeRuntimeHost.PlayerInventory",
                "grant");
        if (!EchoRuntimeModules.isLoaded("echoterminal")) {
            return result(false, "SKIPPED_OPTIONAL_MODULE_MISSING", "echoterminal is not loaded.", context, Map.of(
                    "module", "echoterminal"));
        }
        return dispatchHostAction(
                host,
                operationId,
                Map.of("item", TERMINAL_REMOTE_ITEM.toString(), "count", 1),
                playerRef,
                null,
                null,
                context,
                true,
                false,
                runtimeHost -> runtimeHost.playerInventory().grant(
                        playerRef,
                        new NativeItemStack(TERMINAL_REMOTE_ITEM.toString(), 1, Map.of("dedupe", true)),
                        context));
    }

    private static NativeResult reusedDropPodResult(StartingDropPodData.Entry entry, NativeMutationContext context) {
        return result(false, "REUSED_EXISTING_STRUCTURE", "Reused an existing personal drop pod.", context, Map.of(
                "structure", "echoashfallprotocol:drop_pod",
                "reused", true,
                "origin", positionSnapshot(entry.origin()),
                "interior", positionSnapshot(entry.interior())));
    }

    private static boolean isReusableStartingPod(StartingDropPodData.Entry entry) {
        return entry.origin().getY() >= MIN_STARTING_SURFACE_Y
                && entry.interior().getY() >= MIN_STARTING_SURFACE_Y;
    }

    private static boolean isReusableStartingPod(ServerLevel level, StartingDropPodData.Entry entry) {
        if (!isReusableStartingPod(entry)) {
            return false;
        }
        if (!level.isLoaded(entry.interior())) {
            return true;
        }
        return isSafeExistingPodInterior(level, entry.interior());
    }

    private static boolean isSafeExistingPodInterior(ServerLevel level, BlockPos interior) {
        return interior.getY() >= MIN_STARTING_SURFACE_Y
                && interior.getY() < level.getMaxY() - 1
                && level.getBlockState(interior).isAir()
                && level.getBlockState(interior.above()).isAir()
                && AshfallInteractionRules.supportsPlacement(level, interior.below());
    }

    private static boolean isGameTestServer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        String serverClassName = level.getServer().getClass().getName();
        return serverClassName.contains("GameTest") || serverClassName.contains("gametest");
    }

    private static BlockPos findStartingPodOrigin(ServerLevel level, StartingDropPodData podData) {
        BlockPos serverSpawn = getServerSpawn(level);
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < STARTING_POD_CANDIDATE_ATTEMPTS; attempt++) {
            BlockPos candidate = randomSurfaceAround(level, serverSpawn, random, false);
            if (candidate != null && podData.isFarEnoughFromExistingPods(candidate, MIN_STARTING_POD_SPACING_BLOCKS)) {
                return candidate;
            }
        }

        for (int attempt = 0; attempt < STARTING_POD_CANDIDATE_ATTEMPTS; attempt++) {
            BlockPos candidate = randomSurfaceAround(level, serverSpawn, random, true);
            if (candidate != null && podData.isFarEnoughFromExistingPods(candidate, MIN_STARTING_POD_SPACING_BLOCKS)) {
                EchoAshfallProtocol.LOGGER.warn(
                        "Used outer-radius fallback for starting pod after {} occupied random candidates near {}.",
                        STARTING_POD_CANDIDATE_ATTEMPTS,
                        serverSpawn);
                return candidate;
            }
        }

        BlockPos nearest = findNearestSafeStartingSurface(
                level,
                serverSpawn,
                STARTING_SURFACE_FALLBACK_SEARCH_RADIUS,
                STARTING_SURFACE_FALLBACK_SEARCH_STEP);
        if (nearest != null) {
            EchoAshfallProtocol.LOGGER.warn(
                    "Could not find an unoccupied random starting pod location after {} attempts. Using nearest safe surface near {}.",
                    STARTING_POD_CANDIDATE_ATTEMPTS * 2,
                    serverSpawn);
            return nearest;
        }

        int fallbackY = Math.min(
                level.getMaxY() - 2,
                Math.max(
                        Math.max(level.getMinY() + MIN_SAFE_SURFACE_ABOVE_BOTTOM, MIN_STARTING_SURFACE_Y),
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, serverSpawn.getX(), serverSpawn.getZ())));
        EchoAshfallProtocol.LOGGER.warn(
                "Could not find any safe starting pod surface after {} attempts near {}. Using capped last-resort Y {}.",
                STARTING_POD_CANDIDATE_ATTEMPTS * 2,
                serverSpawn,
                fallbackY);
        return new BlockPos(serverSpawn.getX(), fallbackY, serverSpawn.getZ());
    }

    private static BlockPos getServerSpawn(ServerLevel level) {
        BlockPos sharedSpawn = level.getRespawnData().pos();
        int x = sharedSpawn.getX();
        int z = sharedSpawn.getZ();
        BlockPos safeSpawn = resolveSafeStartingSurface(level, x, z);
        if (safeSpawn != null) {
            return safeSpawn;
        }

        int fallbackY = Math.min(
                level.getMaxY() - 2,
                Math.max(
                        Math.max(level.getMinY() + MIN_SAFE_SURFACE_ABOVE_BOTTOM, MIN_STARTING_SURFACE_Y),
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)));
        EchoAshfallProtocol.LOGGER.warn(
                "No fully safe shared spawn surface found near [{}, {}]. Using capped spawn reference Y {} for pod search.",
                x,
                z,
                fallbackY);
        return new BlockPos(x, fallbackY, z);
    }

    private static BlockPos randomSurfaceAround(ServerLevel level, BlockPos center, RandomSource random, boolean nearOuterRadius) {
        int radiusBlocks;
        if (nearOuterRadius) {
            int fallbackBand = Math.max(CHUNK_SIZE, MIN_STARTING_POD_SPACING_BLOCKS);
            radiusBlocks = MAX_STARTING_POD_RADIUS_BLOCKS - random.nextInt(fallbackBand);
        } else {
            radiusBlocks = MIN_STARTING_POD_RADIUS_BLOCKS
                    + random.nextInt(MAX_STARTING_POD_RADIUS_BLOCKS - MIN_STARTING_POD_RADIUS_BLOCKS + 1);
        }

        double angle = random.nextDouble() * Math.PI * 2.0;
        int x = center.getX() + (int) Math.round(Math.cos(angle) * radiusBlocks);
        int z = center.getZ() + (int) Math.round(Math.sin(angle) * radiusBlocks);
        return resolveSafeStartingSurface(level, x, z);
    }

    private static BlockPos resolveSafeStartingSurface(ServerLevel level, int x, int z) {
        return findNearestSafeStartingSurface(level, new BlockPos(x, MIN_STARTING_SURFACE_Y, z),
                STARTING_SURFACE_SEARCH_RADIUS, STARTING_SURFACE_SEARCH_STEP);
    }

    private static BlockPos findNearestSafeStartingSurface(ServerLevel level, BlockPos center, int radiusLimit, int step) {
        BlockPos exact = getSafeSurfaceColumn(level, center.getX(), center.getZ());
        if (exact != null) {
            return exact;
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = step; radius <= radiusLimit; radius += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    BlockPos candidate = getSafeSurfaceColumn(level, center.getX() + dx, center.getZ() + dz);
                    if (candidate == null) {
                        continue;
                    }

                    double distance = candidate.distSqr(new BlockPos(center.getX(), candidate.getY(), center.getZ()));
                    if (distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    private static BlockPos getSafeSurfaceColumn(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int motionBlockingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int worldSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

        BlockPos motionBlocking = new BlockPos(x, motionBlockingY, z);
        if (isSafeStartingSurface(level, motionBlocking)) {
            return motionBlocking;
        }

        BlockPos worldSurface = new BlockPos(x, worldSurfaceY, z);
        if (worldSurfaceY != motionBlockingY && isSafeStartingSurface(level, worldSurface)) {
            return worldSurface;
        }

        return null;
    }

    private static boolean isSafeStartingSurface(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() + MIN_SAFE_SURFACE_ABOVE_BOTTOM
                || pos.getY() < MIN_STARTING_SURFACE_Y
                || pos.getY() >= level.getMaxY() - 2) {
            return false;
        }

        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && AshfallInteractionRules.supportsPlacement(level, pos.below())
                && level.canSeeSky(pos.above());
    }

    private static String starterNoteMessage() {
        String terminalLine = EchoRuntimeModules.isLoaded("echoterminal")
                ? "\u00A7eTerminal:\u00A7r use the remote or press [M] and follow ECHO objectives.\n"
                : "\u00A7eGuide:\u00A7r follow HUD/chat goals; press [N] to reopen this briefing.\n";
        String wikiLine = AshfallWikiIntegration.isWikiLoaded()
                ? "\u00A7eWiki:\u00A7r field manuals remain craftable or lootable; start with ECHO objectives.\n"
                : "";
        return "\n\u00A7b[ECHO-7] FIRST 10 MINUTES\n"
                + "\n\u00A7eLockers:\u00A7r open OXYGEN, TOOLS, SCRAP, and LOGS before leaving.\n"
                + "\u00A7eWater:\u00A7r drink Clean Water from the OXYGEN locker before scouting.\n"
                + "\u00A7eShelter:\u00A7r deploy the Ash Campfire, chest, and torches near the ramp.\n"
                + "\u00A7eTool:\u00A7r take the sword, then craft a Scrap Knife from pod salvage.\n"
                + "\u00A7eScanner:\u00A7r store the Signal Scanner until base power is online.\n"
                + wikiLine
                + terminalLine
                + "\n\u00A77Emergency Buffer: 10 minutes. Use it to equip, drink, shelter, and craft.\n";
    }

    private static NativeResult result(
            boolean mutated,
            String status,
            String message,
            NativeMutationContext context,
            Map<String, Object> snapshot) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operationId", context.idempotencyKey());
        data.put("moduleId", context.moduleId());
        data.put("dimensionId", context.dimensionId());
        data.put("logicalSide", context.logicalSide());
        data.put("nativeInterface", context.metadata().getOrDefault("nativeInterface", ""));
        data.put("nativeMethod", context.metadata().getOrDefault("nativeMethod", ""));
        data.put("realNativeStateMutated", mutated);
        if (snapshot != null) {
            data.putAll(snapshot);
        }
        return new NativeResult(mutated, status, message, data);
    }

    private static NativeResult dispatchHostAction(
            NativeLoaderEchoRuntimeHost host,
            String actionId,
            Map<String, Object> payload,
            NativePlayerRef targetPlayer,
            NativePosition targetPosition,
            NativeBlockRef targetBlock,
            NativeMutationContext context,
            boolean saveTouchedWhenMutated,
            boolean hudOrEventWhenMutated,
            HostMutation mutation) {
        ensureRuntimeHostRegistered(host);
        Map<String, Object> safePayload = payload == null ? Map.of() : Map.copyOf(payload);
        EchoRuntimeAction action = new EchoRuntimeAction(
                actionId,
                NativeLoaderEchoRuntimeHost.RUNTIME_HOST_ID,
                safePayload,
                targetPlayer,
                context.dimensionId(),
                targetPosition,
                targetBlock,
                context);
        return EchoRuntimeActionDispatcher.global().dispatch(action, (registeredHost, dispatchedAction) -> {
            Map<String, Object> before = mutationPhaseSnapshot("before", action, null);
            NativeResult rawResult = mutation.apply(registeredHost);
            NativeResult result = enrichDispatchedResult(
                    rawResult,
                    action,
                    saveTouchedWhenMutated,
                    hudOrEventWhenMutated);
            Map<String, Object> after = mutationPhaseSnapshot("after", action, result);
            return EchoRuntimeActionOutcome.of(
                    before,
                    result,
                    after,
                    result.mutated() && saveTouchedWhenMutated,
                    result.mutated() && hudOrEventWhenMutated);
        });
    }

    private static void ensureRuntimeHostRegistered(NativeLoaderEchoRuntimeHost host) {
        EchoRuntimeHostRegistry.global().register(host, new EchoRuntimeHostCapabilities(
                NativeLoaderEchoRuntimeHost.RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.PlayerInventory",
                        "EchoNativeRuntimeHost.Structures",
                        "EchoNativeRuntimeHost.PlayerState",
                        "EchoNativeRuntimeHost.Packets",
                        "EchoNativeRuntimeHost.Hud",
                        "EchoNativeRuntimeHost.WorldState",
                        "EchoNativeRuntimeHost.SaveData",
                        "EchoCoreServices"),
                RUNTIME_ACTION_IDS,
                Set.of(
                        "echoashfallprotocol:drop_pod",
                        "echoashfallprotocol:field_manual",
                        "echoterminal:echo_terminal_remote",
                        FIRST_MISSION.toString(),
                        FIND_DROP_POD_ADVANCEMENT.toString()),
                true,
                true,
                true));
    }

    private static NativeResult enrichDispatchedResult(
            NativeResult result,
            EchoRuntimeAction action,
            boolean saveTouchedWhenMutated,
            boolean hudOrEventWhenMutated) {
        if (result == null) {
            return NativeResult.failed("Runtime host action returned no NativeResult.", Map.of(
                    "adapterCoreActionDispatched", true,
                    "adapterCoreActionId", action.actionId(),
                    "runtimeHostId", action.runtimeHostId(),
                    "runtimeHostResolved", true,
                    "saveTouched", false,
                    "hudOrEventEmitted", false,
                    "failureReason", "missing runtime result"));
        }
        Map<String, Object> snapshot = new LinkedHashMap<>(result.snapshot());
        snapshot.put("adapterCoreActionDispatched", true);
        snapshot.put("adapterCoreActionId", action.actionId());
        List<String> sourceOperationIds = sourceOperationIds(action.actionId());
        snapshot.put("adapterCoreSourceOperationId", sourceOperationIds.get(0));
        snapshot.put("adapterCoreSourceOperationIds", sourceOperationIds);
        List<String> hostCallAdapterIds = hostCallAdapterIds(action.actionId());
        if (!hostCallAdapterIds.isEmpty()) {
            snapshot.put("adapterCoreHostCallAdapterId", hostCallAdapterIds.get(0));
            snapshot.put("adapterCoreHostCallAdapterIds", hostCallAdapterIds);
        }
        snapshot.put("runtimeHostId", action.runtimeHostId());
        snapshot.put("runtimeHostResolved", true);
        snapshot.put("minecraftRuntimeAccessed", result.mutated());
        snapshot.put("minecraftRuntimeMutated", result.mutated());
        snapshot.put("saveTouched", result.mutated() && saveTouchedWhenMutated);
        snapshot.put("hudOrEventEmitted", result.mutated() && hudOrEventWhenMutated);
        snapshot.put("target", action.targetSnapshot());
        return new NativeResult(result.mutated(), result.status(), result.message(), snapshot);
    }

    private static Map<String, Object> mutationPhaseSnapshot(
            String phase,
            EchoRuntimeAction action,
            NativeResult result) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("phase", phase);
        snapshot.put("adapterCoreActionId", action.actionId());
        List<String> sourceOperationIds = sourceOperationIds(action.actionId());
        snapshot.put("adapterCoreSourceOperationId", sourceOperationIds.get(0));
        snapshot.put("adapterCoreSourceOperationIds", sourceOperationIds);
        List<String> hostCallAdapterIds = hostCallAdapterIds(action.actionId());
        if (!hostCallAdapterIds.isEmpty()) {
            snapshot.put("adapterCoreHostCallAdapterId", hostCallAdapterIds.get(0));
            snapshot.put("adapterCoreHostCallAdapterIds", hostCallAdapterIds);
        }
        snapshot.put("runtimeHostId", action.runtimeHostId());
        snapshot.put("target", action.targetSnapshot());
        if (result != null) {
            snapshot.put("resultStatus", result.status());
            snapshot.put("stateMutated", result.mutated());
        }
        return Map.copyOf(snapshot);
    }

    private static String sourceOperationId(String actionId) {
        return sourceOperationIds(actionId).get(0);
    }

    private static List<String> sourceOperationIds(String actionId) {
        return switch (actionId) {
            case "player.write_first_join_state.quest", "player.write_first_join_state.flag",
                 "repair.first_objective_state" ->
                    List.of("player.write_first_join_state", actionId);
            case "repair.rescue_underground_or_missing_respawn.teleport",
                 "repair.rescue_underground_or_missing_respawn.respawn",
                 "repair.rescue_underground_or_missing_respawn.respawn_repair" ->
                    List.of("repair.rescue_underground_or_missing_respawn", actionId);
            case "repair.reissue_terminal_remote_if_loaded" ->
                    List.of("inventory.write_terminal_remote_if_loaded", actionId);
            case "repair.grant_find_drop_pod_advancement" ->
                    List.of("player.grant_find_drop_pod_advancement", actionId);
            case "repair.replace_missing_or_invalid_drop_pod.structure" ->
                    List.of("world.place_personal_drop_pod", actionId);
            case "ui.dispatch_welcome_screen" ->
                    List.of("ui.dispatch_welcome_screen", "screen.dispatch_welcome_onboarding_surface");
            default -> List.of(actionId);
        };
    }

    private static String hostCallAdapterId(String actionId) {
        List<String> ids = hostCallAdapterIds(actionId);
        return ids.isEmpty() ? "" : ids.get(0);
    }

    private static List<String> hostCallAdapterIds(String actionId) {
        return switch (actionId) {
            case "inventory.write_starter_note", "inventory.write_terminal_remote_if_loaded",
                 "repair.reissue_terminal_remote_if_loaded" ->
                    List.of("minecraft_backed_inventory_writer");
            case "world.place_personal_drop_pod", "repair.replace_missing_or_invalid_drop_pod.structure" ->
                    List.of("minecraft_backed_structure_placer");
            case "player.teleport_to_drop_pod_interior" ->
                    List.of("minecraft_backed_player_positioner");
            case "repair.rescue_underground_or_missing_respawn.teleport" ->
                    List.of("minecraft_backed_player_positioner", "native_existing_player_repair_adapter");
            case "player.bind_drop_pod_respawn" ->
                    List.of("minecraft_backed_respawn_binder");
            case "repair.rescue_underground_or_missing_respawn.respawn",
                 "repair.rescue_underground_or_missing_respawn.respawn_repair" ->
                    List.of("minecraft_backed_respawn_binder", "native_existing_player_repair_adapter");
            case "player.write_first_join_state.quest", "player.write_first_join_state.flag",
                 "repair.first_objective_state" ->
                    List.of("minecraft_backed_player_state_writer");
            case "player.grant_find_drop_pod_advancement", "repair.grant_find_drop_pod_advancement" ->
                    List.of("minecraft_backed_advancement_granter");
            case "ui.dispatch_welcome_screen" ->
                    List.of("minecraft_backed_screen_packet_sender", "native_welcome_surface_adapter");
            case "hud.publish_opening_recovery_notice" ->
                    List.of("minecraft_backed_hud_notification_sender");
            case "hud.publish_mission_tracker_line" ->
                    List.of("native_hud_mission_tracker_adapter");
            case "hud.publish_hazard_weather_readout" ->
                    List.of("native_hud_hazard_readout_adapter");
            case "hud.feed_hazard_weather_readout" ->
                    List.of("native_hud_hazard_weather_adapter");
            case "terminal.publish_first_ten_minutes_link" ->
                    List.of("native_terminal_surface_adapter");
            case "wiki.publish_ashfall_guide_link" ->
                    List.of("native_wiki_surface_adapter");
            case "lens.publish_opening_route_scan" ->
                    List.of("native_lens_surface_adapter");
            case "holomap.publish_opening_recovery_layers" ->
                    List.of("native_holomap_surface_adapter");
            case "codex.publish_opening_route_entries" ->
                    List.of("native_codex_surface_adapter");
            case "recovery.publish_drop_pod_field_cache_context" ->
                    List.of("native_recovery_field_cache_adapter");
            case "holomap.publish_recovery_route_markers" ->
                    List.of("native_holomap_recovery_visibility_adapter");
            case "weather.feed_route_hazard_context" ->
                    List.of("native_weather_state_adapter");
            case "sound.map_weather_state_to_ambience_cues" ->
                    List.of("native_sound_ambience_adapter");
            case "atmosphere.publish_visibility_particle_sky_fog_profile" ->
                    List.of("native_atmosphere_visibility_adapter");
            default -> List.of();
        };
    }

    @FunctionalInterface
    private interface HostMutation {
        NativeResult apply(EchoNativeRuntimeHost host);
    }

    private static FirstSpawnRuntimeResult recordRuntimeMutationEvidence(FirstSpawnRuntimeResult result) {
        AshfallNativeRuntimeMutationEvidence.record(
                result.id(),
                NativeLoaderEchoRuntimeHost.RUNTIME_HOST_ID,
                result.branch(),
                result.status(),
                result.realNativeStateMutated(),
                result.mutationCount(),
                result.nativeResults(),
                result.snapshot());
        return result;
    }

    private static Map<String, Object> positionSnapshot(BlockPos pos) {
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }

    private static Map<String, Object> payload(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("payload entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return Map.copyOf(result);
    }

    private static BlockPos blockPosFromSnapshot(Map<String, Object> snapshot, String key) {
        Object value = snapshot.get(key);
        if (!(value instanceof Map<?, ?> raw)) {
            return null;
        }
        Object x = raw.get("x");
        Object y = raw.get("y");
        Object z = raw.get("z");
        if (x instanceof Number xNumber && y instanceof Number yNumber && z instanceof Number zNumber) {
            return new BlockPos(xNumber.intValue(), yNumber.intValue(), zNumber.intValue());
        }
        return null;
    }

    public record FirstSpawnRuntimeResult(
            String id,
            String branch,
            String status,
            boolean adapterCoreRuntime,
            boolean realNativeStateMutated,
            int mutationCount,
            List<Map<String, Object>> nativeResults,
            Map<String, Object> snapshot) {
        public FirstSpawnRuntimeResult {
            nativeResults = nativeResults == null ? List.of() : List.copyOf(nativeResults);
            snapshot = snapshot == null ? Map.of() : Map.copyOf(snapshot);
        }

        static FirstSpawnRuntimeResult skipped(String reason) {
            return recordRuntimeMutationEvidence(new FirstSpawnRuntimeResult(
                    "echoashfallprotocol:first_spawn_native_loader_runtime",
                    "skipped",
                    "SKIPPED",
                    true,
                    false,
                    0,
                    List.of(),
                    Map.of("reason", reason)));
        }

        static FirstSpawnRuntimeResult from(
                String branch,
                List<NativeResult> results,
                Map<String, Object> snapshot) {
            List<Map<String, Object>> nativeResults = new ArrayList<>();
            int mutationCount = 0;
            List<String> failed = new ArrayList<>();
            for (NativeResult result : results) {
                if (result.mutated()) {
                    mutationCount++;
                }
                if (result.status().startsWith("FAIL") || result.status().startsWith("INVALID")) {
                    failed.add(result.status());
                }
                nativeResults.add(Map.of(
                        "mutated", result.mutated(),
                        "status", result.status(),
                        "message", result.message(),
                        "snapshot", result.snapshot()));
            }

            Map<String, Object> finalSnapshot = new LinkedHashMap<>();
            finalSnapshot.putAll(snapshot);
            finalSnapshot.put("resultCount", results.size());
            finalSnapshot.put("failedResults", List.copyOf(failed));
            finalSnapshot.put("consumedSourceOperationIds", consumedValues(
                    nativeResults,
                    "adapterCoreSourceOperationId",
                    "adapterCoreSourceOperationIds"));
            finalSnapshot.put("consumedHostCallAdapterIds", consumedValues(
                    nativeResults,
                    "adapterCoreHostCallAdapterId",
                    "adapterCoreHostCallAdapterIds"));
            return recordRuntimeMutationEvidence(new FirstSpawnRuntimeResult(
                    "echoashfallprotocol:first_spawn_native_loader_runtime",
                    branch,
                    failed.isEmpty() ? "PASS" : "PARTIAL",
                    true,
                    mutationCount > 0,
                    mutationCount,
                    nativeResults,
                    finalSnapshot));
        }

        private static List<String> consumedValues(List<Map<String, Object>> nativeResults, String... keys) {
            Set<String> values = new java.util.LinkedHashSet<>();
            for (Map<String, Object> nativeResult : nativeResults) {
                if (!Boolean.TRUE.equals(nativeResult.get("mutated"))) {
                    continue;
                }
                Object snapshot = nativeResult.get("snapshot");
                if (snapshot instanceof Map<?, ?> map) {
                    for (String key : keys) {
                        Object value = map.get(key);
                        if (value instanceof Iterable<?> iterable) {
                            for (Object entry : iterable) {
                                if (entry != null && !String.valueOf(entry).isBlank()) {
                                    values.add(String.valueOf(entry));
                                }
                            }
                        } else if (value instanceof String text && !text.isBlank()) {
                            values.add(text);
                        } else if (value != null && !String.valueOf(value).isBlank()) {
                            values.add(String.valueOf(value));
                        }
                    }
                }
            }
            return List.copyOf(values);
        }
    }

    private record ReturningPodRepair(
            boolean replaced,
            boolean teleported,
            boolean respawnRepaired,
            String reason,
            BlockPos interior) {
    }
}

