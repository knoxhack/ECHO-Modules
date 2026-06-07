package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreHostExecutionBridge {
    private final GalacticCoreHostCallbacks hostCallbacks;

    public GalacticCoreHostExecutionBridge(GalacticCoreHostCallbacks hostCallbacks) {
        this.hostCallbacks = Objects.requireNonNull(hostCallbacks, "hostCallbacks");
    }

    public HostExecutionPlan executeDimensionTransfer(GalacticCoreHostCallbacks.TransferPlacementCallback callback) {
        GalacticCoreHostCallbacks.HostCallbackResult result = hostCallbacks.onDimensionTransferExecute(callback);
        GalacticCoreRuntimeGateway.RuntimeAction sourceAction = result.actions().getFirst();
        List<String> requiredActions = evidenceList(sourceAction, "requiredHostActions");
        return new HostExecutionPlan(
                result.accepted(),
                result.status(),
                GalacticCoreIds.id("host_execution/dimension_transfer"),
                requiredActions,
                String.valueOf(result.evidence().get("saveDataTarget")),
                new GalacticCoreRuntimeGateway.RuntimeAction(
                        GalacticCoreIds.id("event/host_execution_dimension_transfer"),
                        "events",
                        "publish",
                        Map.ofEntries(
                                Map.entry("source", "galacticraft_legacy_host_execution_bridge"),
                                Map.entry("callbackSource", sourceAction.evidence().get("source")),
                                Map.entry("callbackTarget", sourceAction.target()),
                                Map.entry("playerId", callback.playerId()),
                                Map.entry("route", callback.destinationRoute()),
                                Map.entry("accepted", result.accepted()),
                                Map.entry("status", result.status()),
                                Map.entry("requiredHostActions", requiredActions),
                                Map.entry("commitOrder", List.of("load_dimension", "ticket_chunk", "place_player", "sync_progression")),
                                Map.entry("saveDataTarget", result.evidence().get("saveDataTarget")),
                                Map.entry("replacement", "ASDK live host dimension transfer execution binding")
                        )
                )
        );
    }

    public HostExecutionPlan spawnBossEntity(GalacticCoreHostCallbacks.BossEntitySpawnCallback callback) {
        GalacticCoreHostCallbacks.HostCallbackResult result = hostCallbacks.onBossEntitySpawn(callback);
        GalacticCoreRuntimeGateway.RuntimeAction sourceAction = result.actions().getFirst();
        List<String> requiredActions = evidenceList(sourceAction, "requiredHostActions");
        return new HostExecutionPlan(
                result.accepted(),
                result.status(),
                GalacticCoreIds.id("host_execution/boss_entity_spawn"),
                requiredActions,
                String.valueOf(result.evidence().get("saveDataTarget")),
                new GalacticCoreRuntimeGateway.RuntimeAction(
                        GalacticCoreIds.id("event/host_execution_boss_spawn"),
                        "events",
                        "publish",
                        Map.ofEntries(
                                Map.entry("source", "galacticraft_legacy_host_execution_bridge"),
                                Map.entry("callbackSource", sourceAction.evidence().get("source")),
                                Map.entry("callbackTarget", sourceAction.target()),
                                Map.entry("requesterId", callback.requesterId()),
                                Map.entry("dungeonId", callback.dungeonId()),
                                Map.entry("bossId", result.evidence().get("bossId")),
                                Map.entry("bossRoomId", result.evidence().get("bossRoomId")),
                                Map.entry("accepted", result.accepted()),
                                Map.entry("status", result.status()),
                                Map.entry("requiredHostActions", requiredActions),
                                Map.entry("commitOrder", List.of("load_boss_room", "spawn_entity", "attach_state", "lock_room")),
                                Map.entry("saveDataTarget", result.evidence().get("saveDataTarget")),
                                Map.entry("replacement", "ASDK live host boss entity spawn binding")
                        )
                )
        );
    }

    public HostExecutionPlan bindHoloMapRouteMenu(GalacticCoreHostCallbacks.HoloMapRouteSurfaceCallback callback) {
        return bindRenderedMenu(
                hostCallbacks.onHoloMapRouteMenuRender(callback),
                GalacticCoreIds.id("host_execution/holomap_route_menu"),
                GalacticCoreIds.id("screen_layout/host_binding/holomap_routes"),
                "ASDK live HoloMap route menu binding"
        );
    }

    public HostExecutionPlan bindLaunchChecklistMenu(GalacticCoreHostCallbacks.LaunchChecklistSurfaceCallback callback) {
        return bindRenderedMenu(
                hostCallbacks.onScreenCoreLaunchChecklistMenuRender(callback),
                GalacticCoreIds.id("host_execution/screencore_launch_checklist_menu"),
                GalacticCoreIds.id("screen_layout/host_binding/screencore_launch_checklist"),
                "ASDK live ScreenCore launch checklist menu binding"
        );
    }

    public HostExecutionPlan bindTreasureChestMenu(GalacticCoreHostCallbacks.TreasureInteractionCallback callback) {
        return bindRenderedMenu(
                hostCallbacks.onTreasureChestMenuRender(callback),
                GalacticCoreIds.id("host_execution/treasure_chest_menu"),
                GalacticCoreIds.id("screen_layout/host_binding/treasure_chest"),
                "ASDK live treasure chest menu binding"
        );
    }

    public List<GalacticCoreRuntimeGateway.RuntimeAction> releaseHostExecutionSmokeActions() {
        GalacticCoreRuntimeService.PlayerProgression progression = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.RocketLaunchState launch = new GalacticCoreRuntimeService.RocketLaunchState(
                1000,
                1000,
                true,
                true,
                true,
                1,
                200
        );
        GalacticCoreRuntimeService.PlayerGearState gear = new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true);
        GalacticCoreRuntimeService.EnvironmentState moon = new GalacticCoreRuntimeService.EnvironmentState(
                GalacticCoreIds.id("moon"),
                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                GalacticCoreRuntimeService.ThermalRisk.COLD
        );
        GalacticCoreHostCallbacks.TransferPlacementCallback transfer = new GalacticCoreHostCallbacks.TransferPlacementCallback(
                "player/smoke",
                progression,
                launch,
                GalacticCoreIds.id("route/moon"),
                gear,
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
        );
        GalacticCoreHostCallbacks.TreasureInteractionCallback treasure = new GalacticCoreHostCallbacks.TreasureInteractionCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        true,
                        true,
                        true,
                        true
                )
        );
        return List.of(
                executeDimensionTransfer(transfer).action(),
                spawnBossEntity(new GalacticCoreHostCallbacks.BossEntitySpawnCallback("boss/smoke", GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"))).action(),
                bindHoloMapRouteMenu(new GalacticCoreHostCallbacks.HoloMapRouteSurfaceCallback("player/smoke", progression, GalacticCoreIds.id("route/mars"))).action(),
                bindLaunchChecklistMenu(new GalacticCoreHostCallbacks.LaunchChecklistSurfaceCallback("player/smoke", progression, launch, GalacticCoreIds.id("route/moon"), gear, moon)).action(),
                bindTreasureChestMenu(treasure).action()
        );
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_host_execution_bridge",
                "typedReceiptsOnly", true,
                "bindings", "dimension_transfer_execution, boss_entity_spawn, holomap_route_menu, screencore_launch_checklist_menu, treasure_chest_menu",
                "replaces", "WorldProvider transfer execution, EntityBoss construction, GuiCelestialSelection, GuiPreLaunchChecklist, GuiTreasureChest host binding"
        );
    }

    private static HostExecutionPlan bindRenderedMenu(
            GalacticCoreHostCallbacks.HostCallbackResult result,
            String operationId,
            String target,
            String replacement
    ) {
        GalacticCoreRuntimeGateway.RuntimeAction sourceAction = result.actions().getFirst();
        String rendererId = String.valueOf(result.evidence().get("rendererId"));
        return new HostExecutionPlan(
                result.accepted(),
                result.status(),
                operationId,
                List.of("bind_screen_renderer", "mount_widgets", "wire_screen_actions", "sync_screen_state"),
                String.valueOf(result.evidence().get("saveDataTarget")),
                new GalacticCoreRuntimeGateway.RuntimeAction(
                        target,
                        "screens",
                        "open",
                        Map.ofEntries(
                                Map.entry("source", "galacticraft_legacy_host_execution_bridge"),
                                Map.entry("callbackSource", sourceAction.evidence().get("source")),
                                Map.entry("callbackTarget", sourceAction.target()),
                                Map.entry("screenId", result.evidence().get("screenId")),
                                Map.entry("rendererId", rendererId),
                                Map.entry("widgetCount", result.evidence().get("widgetCount")),
                                Map.entry("accepted", result.accepted()),
                                Map.entry("status", result.status()),
                                Map.entry("requiredHostActions", List.of("bind_screen_renderer", "mount_widgets", "wire_screen_actions", "sync_screen_state")),
                                Map.entry("saveDataTarget", result.evidence().get("saveDataTarget")),
                                Map.entry("replacement", replacement)
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> evidenceList(GalacticCoreRuntimeGateway.RuntimeAction action, String key) {
        Object value = action.evidence().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public record HostExecutionPlan(
            boolean accepted,
            String status,
            String operationId,
            List<String> requiredHostActions,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction action
    ) {
        public HostExecutionPlan {
            status = requireText(status, "status");
            operationId = requireText(operationId, "operationId");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            action = Objects.requireNonNull(action, "action");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
