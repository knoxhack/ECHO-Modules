package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Live AdapterCore host path for the First Relay Station major-route contract.
 */
public final class AshfallAdapterCoreMajorRouteRuntime {
    private static final String RUNTIME_HOST_ID = "echoashfallprotocol:major_route_runtime";
    private static final String LAST_EVENT_KEY = "ashes_of_tomorrow.adaptercore.last_major_route_event";
    private static final String LAST_EVENT_TICK_KEY = "ashes_of_tomorrow.adaptercore.last_major_route_event_tick";
    private static final String SAVE_ROOT = "echoashfallprotocol.adaptercore.major_route";
    private static final String MISSION_ID = "echoashfallprotocol:first_relay_station_route";
    private static final String ROUTE_ID = "echoashfallprotocol:relay_station";
    private static final String TERMINAL_PAGE = "echoashfallprotocol:ashfall_major_route_records";
    private static final String RELAY_WEATHER_WINDOW = "echoashfallprotocol:relay_weather_window";
    private static final String RELAY_MARKER = "echoashfallprotocol:first_relay_station";
    private static final String RELAY_CONSOLE = "echoashfallprotocol:relay_station_console";
    private static final String POWERGRID_REPAIR = "echoashfallprotocol:ashfall_relay_station_repair";
    private static final String RELAY_CACHE = "echoashfallprotocol:relay_cache_lockbox";
    private static final String RETURN_TARGET = "echoashfallprotocol:first_relay_station_route/returned";

    private static final List<RouteAction> ROUTE_ACTIONS = List.of(
            new RouteAction(
                    "ashfall.terminal_page",
                    TERMINAL_PAGE,
                    "mission.record_terminal_page_objective",
                    "echoashfallprotocol:first_relay_station_route/open_major_route_records",
                    "native_terminal_page_event_bridge",
                    "echoterminal:route_records",
                    "Major route records opened"),
            new RouteAction(
                    "ashfall.hazard_check",
                    RELAY_WEATHER_WINDOW,
                    "mission.record_relay_weather_window",
                    "echoashfallprotocol:first_relay_station_route/check_weather_window",
                    "native_weather_hazard_check_bridge",
                    "echoweathercore:route_hazards",
                    "Relay weather window checked"),
            new RouteAction(
                    "holomap.marker_selected",
                    RELAY_MARKER,
                    "mission.track_first_relay_marker",
                    "echoashfallprotocol:first_relay_station_route/track_relay_marker",
                    "native_holomap_marker_bridge",
                    "echoholomap:first_major_route",
                    "First relay marker tracked"),
            new RouteAction(
                    "player.scanner_used",
                    RELAY_CONSOLE,
                    "mission.scan_relay_console",
                    "echoashfallprotocol:first_relay_station_route/scan_relay_console",
                    "native_lens_scan_bridge",
                    "echolens:ashfall_major_route_scans",
                    "Relay console scanned"),
            new RouteAction(
                    "powergrid.repair",
                    POWERGRID_REPAIR,
                    "mission.repair_relay_power_coupler",
                    "echoashfallprotocol:first_relay_station_route/repair_power_coupler",
                    "native_powergrid_repair_bridge",
                    "echopowergrid:repair_path",
                    "Relay power coupler repaired"),
            new RouteAction(
                    "player.terminal_opened",
                    RELAY_CACHE,
                    "mission.claim_relay_cache",
                    "echoashfallprotocol:first_relay_station_route/claim_relay_cache",
                    "native_loot_container_bridge",
                    "minecraft:loot_table",
                    "Relay cache claimed"),
            new RouteAction(
                    "terminal.route_record",
                    RETURN_TARGET,
                    "mission.return_and_update_terminal",
                    "echoashfallprotocol:first_relay_station_route/return_and_update_terminal",
                    "native_terminal_route_record_bridge",
                    "echoterminal:route_records",
                    "Terminal route record updated"));

    private static final AshfallAdapterCoreRuntimeTruthBridge.RuntimeBinding RUNTIME_BINDING =
            AshfallAdapterCoreRuntimeTruthBridge.binding(
                    RUNTIME_HOST_ID,
                    "major_route",
                    LAST_EVENT_KEY,
                    LAST_EVENT_TICK_KEY,
                    Set.of(
                            "ashfall.terminal_page",
                            "ashfall.hazard_check",
                            "holomap.marker_selected",
                            "player.scanner_used",
                            "powergrid.repair",
                            "player.terminal_opened",
                            "terminal.route_record"),
                    Set.of(
                            TERMINAL_PAGE,
                            RELAY_WEATHER_WINDOW,
                            RELAY_MARKER,
                            RELAY_CONSOLE,
                            POWERGRID_REPAIR,
                            RELAY_CACHE,
                            RETURN_TARGET),
                    AshfallAdapterCoreMajorRouteRuntime::apply);

    private AshfallAdapterCoreMajorRouteRuntime() {
    }

    public static NativeResult terminalPageOpened(ServerPlayer player, String pageId) {
        return publish(player, "ashfall.terminal_page", safe(pageId), "terminal_page_opened");
    }

    public static NativeResult relayWeatherWindowChecked(ServerPlayer player, String weatherWindowId) {
        return publish(player, "ashfall.hazard_check", safe(weatherWindowId), "weather_window_checked");
    }

    public static NativeResult holomapMarkerSelected(ServerPlayer player, String markerId) {
        return publish(player, "holomap.marker_selected", safe(markerId), "holomap_marker_selected");
    }

    public static NativeResult relayConsoleScanned(ServerPlayer player, String scanId) {
        return publish(player, "player.scanner_used", safe(scanId), "relay_console_scanned");
    }

    public static NativeResult relayPowerCouplerRepaired(ServerPlayer player, String repairId) {
        return publish(player, "powergrid.repair", safe(repairId), "relay_power_repaired");
    }

    public static NativeResult relayCacheClaimed(ServerPlayer player, String cacheId) {
        return publish(player, "player.terminal_opened", safe(cacheId), "relay_cache_claimed");
    }

    public static NativeResult terminalRouteRecordUpdated(ServerPlayer player, String routeRecordTarget) {
        return publish(player, "terminal.route_record", safe(routeRecordTarget), "terminal_route_record_updated");
    }

    private static NativeResult publish(ServerPlayer player, String eventId, String target, String source) {
        RouteAction action = action(eventId, target);
        if (action == null) {
            return NativeResult.noop("Major-route runtime ignored a non-route target.", Map.of(
                    "eventId", safe(eventId),
                    "target", safe(target),
                    "realNativeStateMutated", false));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", action.target());
        payload.put("source", source);
        payload.put("missionId", MISSION_ID);
        payload.put("route", ROUTE_ID);
        payload.put("objectiveId", action.objectiveId());
        payload.put("objectiveType", "custom");
        payload.put("amount", 1);
        payload.put("uiFeedbackSurface", action.uiSurface());
        payload.put("adapterCoreSourceOperationId", action.operationId());
        payload.put("adapterCoreHostCallAdapterId", action.hostAdapterId());
        return AshfallAdapterCoreRuntimeTruthBridge.publish(
                RUNTIME_BINDING,
                player,
                eventId,
                Map.copyOf(payload),
                null,
                true);
    }

    private static NativeResult apply(ServerPlayer player, NativeEvent event, NativeMutationContext context) {
        Map<String, Object> payload = event.payload();
        RouteAction action = action(event.eventId(), target(payload));
        if (action == null) {
            return NativeResult.noop("Major-route event did not match a canonical First Relay Station target.", Map.of(
                    "eventId", event.eventId(),
                    "target", target(payload),
                    "realNativeStateMutated", false));
        }
        if (!EchoCoreServices.missionCoreAvailable()) {
            return NativeResult.unsupported("MissionCore is not available for major-route mutation.", Map.of(
                    "eventId", event.eventId(),
                    "target", action.target(),
                    "missionId", MISSION_ID,
                    "realNativeStateMutated", false,
                    "failureReason", "missioncore_unavailable"));
        }

        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "major_route");
        boolean missionAdvanced = EchoCoreServices.recordMissionObjective(
                player,
                MissionObjectiveType.CUSTOM,
                targetId(action.target()),
                1,
                Map.of(
                        "source", "echoashfallprotocol",
                        "adapterCoreEvent", event.eventId(),
                        "adapterCoreObjective", action.objectiveId(),
                        "adapterCoreSourceOperation", action.operationId(),
                        "adapterCoreHostCallAdapter", action.hostAdapterId()));
        if (!missionAdvanced) {
            return NativeResult.noop("Major-route event was already recorded or did not advance MissionCore.", Map.of(
                    "eventId", event.eventId(),
                    "target", action.target(),
                    "missionId", MISSION_ID,
                    "objectiveId", action.objectiveId(),
                    "missionAdvanced", false,
                    "realNativeStateMutated", false));
        }

        int completedObjectiveCount = writeRouteSnapshot(player, action, event, context);
        publishHud(player, action, completedObjectiveCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.eventId());
        result.put("target", action.target());
        result.put("missionId", MISSION_ID);
        result.put("route", ROUTE_ID);
        result.put("objectiveId", action.objectiveId());
        result.put("missionAdvanced", true);
        result.put("completedObjectiveCount", completedObjectiveCount);
        result.put("requiredObjectiveCount", ROUTE_ACTIONS.size());
        result.put("nativeInterface", "EchoNativeRuntimeHost.Events");
        result.put("nativeMethod", "publish");
        result.put("adapterCoreSourceOperationId", action.operationId());
        result.put("adapterCoreHostCallAdapterId", action.hostAdapterId());
        result.put("hostSaveTouched", true);
        result.put("hudOrEventEmitted", true);
        result.put("realNativeStateMutated", true);
        result.put("playerId", player.getUUID().toString());
        return NativeResult.mutated("Published major-route event through AdapterCore and live MissionCore/save/HUD hooks.",
                Map.copyOf(result));
    }

    private static int writeRouteSnapshot(
            ServerPlayer player,
            RouteAction action,
            NativeEvent event,
            NativeMutationContext context) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(SAVE_ROOT).copy();
        CompoundTag route = root.getCompoundOrEmpty(MISSION_ID).copy();
        route.putBoolean(action.operationId(), true);
        route.putString("lastEventId", event.eventId());
        route.putString("lastTarget", action.target());
        route.putString("lastObjectiveId", action.objectiveId());
        route.putString("lastOperationId", action.operationId());
        route.putString("lastHostAdapterId", action.hostAdapterId());
        route.putLong("lastGameTime", context.gameTime());
        route.putString("route", ROUTE_ID);
        int completed = completedObjectiveCount(route);
        route.putInt("completedObjectiveCount", completed);
        route.putInt("requiredObjectiveCount", ROUTE_ACTIONS.size());
        route.putBoolean("missionComplete", completed >= ROUTE_ACTIONS.size());
        root.put(MISSION_ID, route);
        player.getPersistentData().put(SAVE_ROOT, root);
        return completed;
    }

    private static int completedObjectiveCount(CompoundTag route) {
        int completed = 0;
        for (RouteAction action : ROUTE_ACTIONS) {
            if (route.getBooleanOr(action.operationId(), false)) {
                completed++;
            }
        }
        return completed;
    }

    private static void publishHud(ServerPlayer player, RouteAction action, int completedObjectiveCount) {
        player.sendSystemMessage(Component.literal("[ECHO-7] " + action.label()
                + " (" + completedObjectiveCount + "/" + ROUTE_ACTIONS.size() + ")."));
    }

    private static RouteAction action(String eventId, String target) {
        for (RouteAction action : ROUTE_ACTIONS) {
            if (action.eventId().equals(eventId) && action.target().equals(target)) {
                return action;
            }
        }
        return null;
    }

    private static String target(Map<String, Object> payload) {
        String target = stringValue(payload, "target");
        if (!target.isBlank()) {
            return target;
        }
        target = stringValue(payload, "terminalId");
        if (!target.isBlank()) {
            return target;
        }
        target = stringValue(payload, "cacheId");
        return target.isBlank() ? stringValue(payload, "marker") : target;
    }

    private static Identifier targetId(String target) {
        Identifier parsed = Identifier.tryParse(target);
        if (parsed != null) {
            return parsed;
        }
        return Identifier.fromNamespaceAndPath("echoashfallprotocol", target.replace(':', '/').replace(' ', '_'));
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record RouteAction(
            String eventId,
            String target,
            String operationId,
            String objectiveId,
            String hostAdapterId,
            String uiSurface,
            String label) {
    }
}
