package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreHostCallbacks {
    private final GalacticCoreRuntimeService runtime;
    private final GalacticCoreRuntimeGateway gateway;
    private final GalacticCoreRuntimeAdapters adapters;

    public GalacticCoreHostCallbacks(
            GalacticCoreRuntimeService runtime,
            GalacticCoreRuntimeGateway gateway,
            GalacticCoreRuntimeAdapters adapters
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.adapters = Objects.requireNonNull(adapters, "adapters");
    }

    public HostCallbackResult onMachineBlockEntityTick(MachineTickCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.MachineAdapterResult result = adapters.tickMachineBlockEntity(
                new GalacticCoreRuntimeAdapters.MachineBlockEntityAdapter(
                        callback.blockEntityId(),
                        callback.machinePath(),
                        callback.snapshot()
                ),
                callback.input()
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                result.tickAction(),
                "machine_block_entity_tick"
        );
        return new HostCallbackResult(
                true,
                "machine_tick_dispatched",
                List.of(action),
                Map.of(
                        "blockEntityId", callback.blockEntityId(),
                        "saveDataTarget", result.saveDataTarget(),
                        "mutated", result.mutated(),
                        "nextState", result.adapter().snapshot().toString()
                )
        );
    }

    public HostCallbackResult onPlayerLifeSupportTick(LifeSupportTickCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                gateway.publishLifeSupportTick(callback.gear(), callback.environment()),
                "player_life_support_tick"
        );
        return new HostCallbackResult(
                Boolean.TRUE.equals(action.evidence().get("canBreathe")),
                String.valueOf(action.evidence().get("status")),
                List.of(action),
                Map.of("playerId", callback.playerId(), "environment", callback.environment().id())
        );
    }

    public HostCallbackResult onCelestialRouteSelection(RouteSelectionCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                gateway.openCelestialSelection(callback.progression(), callback.selectedRoute()),
                "celestial_route_selection"
        );
        return new HostCallbackResult(
                Boolean.TRUE.equals(action.evidence().get("unlocked")),
                Boolean.TRUE.equals(action.evidence().get("unlocked")) ? "route_selectable" : "route_locked",
                List.of(action),
                Map.of("playerId", callback.playerId(), "route", callback.selectedRoute())
        );
    }

    public HostCallbackResult onHoloMapRouteSurfaceOpen(HoloMapRouteSurfaceCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.ScreenSurfaceAdapterPlan plan = adapters.openHoloMapRouteSurface(
                new GalacticCoreRuntimeAdapters.RouteSurfaceRequest(callback.progression(), callback.selectedRoute())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.screenAction(),
                "holomap_route_surface_open"
        );
        return new HostCallbackResult(
                plan.accepted(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "route", callback.selectedRoute(),
                        "screenId", plan.screenId(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onScreenCoreLaunchChecklistOpen(LaunchChecklistSurfaceCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.ScreenSurfaceAdapterPlan plan = adapters.openScreenCoreLaunchChecklist(
                new GalacticCoreRuntimeAdapters.LaunchChecklistSurfaceRequest(
                        callback.progression(),
                        callback.launchState(),
                        callback.routeId(),
                        callback.gear(),
                        callback.environment()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.screenAction(),
                "screencore_launch_checklist_open"
        );
        return new HostCallbackResult(
                plan.accepted(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "route", callback.routeId(),
                        "screenId", plan.screenId(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onHoloMapRouteMenuRender(HoloMapRouteSurfaceCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan plan = adapters.openRenderedHoloMapRouteMenu(
                new GalacticCoreRuntimeAdapters.RouteSurfaceRequest(callback.progression(), callback.selectedRoute())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.screenAction(),
                "holomap_route_menu_render"
        );
        return renderedMenuResult(callback.playerId(), plan, action);
    }

    public HostCallbackResult onScreenCoreLaunchChecklistMenuRender(LaunchChecklistSurfaceCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan plan = adapters.openRenderedLaunchChecklistMenu(
                new GalacticCoreRuntimeAdapters.LaunchChecklistSurfaceRequest(
                        callback.progression(),
                        callback.launchState(),
                        callback.routeId(),
                        callback.gear(),
                        callback.environment()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.screenAction(),
                "screencore_launch_checklist_menu_render"
        );
        return renderedMenuResult(callback.playerId(), plan, action);
    }

    public HostCallbackResult onHoloMapRouteInteraction(HoloMapRouteInteractionCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.ScreenInteractionAdapterPlan plan = adapters.interactHoloMapRoute(
                new GalacticCoreRuntimeAdapters.ScreenInteractionRequest(
                        callback.progression(),
                        callback.selectedRoute(),
                        callback.actionId()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.networkAction(),
                "holomap_route_interaction"
        );
        return new HostCallbackResult(
                plan.accepted(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "route", callback.selectedRoute(),
                        "screenId", plan.screenId(),
                        "actionId", plan.actionId(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onScreenCoreLaunchChecklistInteraction(LaunchChecklistInteractionCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.ScreenInteractionAdapterPlan plan = adapters.interactScreenCoreLaunchChecklist(
                new GalacticCoreRuntimeAdapters.LaunchChecklistInteractionRequest(
                        callback.progression(),
                        callback.launchState(),
                        callback.routeId(),
                        callback.gear(),
                        callback.environment(),
                        callback.actionId()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.networkAction(),
                "screencore_launch_checklist_interaction"
        );
        return new HostCallbackResult(
                plan.accepted(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "route", callback.routeId(),
                        "screenId", plan.screenId(),
                        "actionId", plan.actionId(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onDimensionTransferRequest(DimensionTransferCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.DimensionTransferPlan plan = adapters.prepareDimensionTransfer(
                new GalacticCoreRuntimeAdapters.DimensionTransferRequest(
                        callback.progression(),
                        callback.launchState(),
                        callback.destinationRoute(),
                        callback.destinationEnvironment(),
                        callback.gear(),
                        callback.anchor()
                )
        );
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = plan.actions().stream()
                .map(action -> withHostEvidence(action, "dimension_transfer_request"))
                .toList();
        return new HostCallbackResult(
                plan.ready(),
                plan.status(),
                actions,
                Map.of(
                        "playerId", callback.playerId(),
                        "route", plan.route(),
                        "environment", plan.environment()
                )
        );
    }

    public HostCallbackResult onTransferPlacementPrepare(TransferPlacementCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.TransferPlacementAdapterPlan plan = adapters.prepareTransferPlacement(
                new GalacticCoreRuntimeAdapters.TransferPlacementRequest(
                        callback.progression(),
                        callback.launchState(),
                        callback.destinationRoute(),
                        callback.gear(),
                        callback.anchor()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.worldgenAction(),
                "transfer_placement_prepare"
        );
        return new HostCallbackResult(
                plan.ready(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "route", callback.destinationRoute(),
                        "placementId", plan.placementId(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onDimensionTransferExecute(TransferPlacementCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.TransferExecutionAdapterPlan plan = adapters.executeDimensionTransfer(
                new GalacticCoreRuntimeAdapters.TransferPlacementRequest(
                        callback.progression(),
                        callback.launchState(),
                        callback.destinationRoute(),
                        callback.gear(),
                        callback.anchor()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.eventAction(),
                "dimension_transfer_execute"
        );
        return new HostCallbackResult(
                plan.ready(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "route", callback.destinationRoute(),
                        "environment", plan.environmentId(),
                        "x", plan.x(),
                        "y", plan.y(),
                        "z", plan.z(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onEnvironmentScan(EnvironmentScanCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                gateway.scanEnvironment(callback.environmentId()),
                "environment_scan"
        );
        return new HostCallbackResult(
                true,
                "environment_scan_ready",
                List.of(action),
                Map.of("requesterId", callback.requesterId(), "environment", callback.environmentId())
        );
    }

    public HostCallbackResult onDungeonStructurePrepare(DungeonStructureCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.DungeonStructureAdapterPlan plan = adapters.prepareDungeonStructure(
                new GalacticCoreRuntimeAdapters.DungeonStructureRequest(callback.dungeonId())
        );
        GalacticCoreRuntimeGateway.RuntimeAction hostAction = withHostEvidence(
                plan.worldgenAction(),
                "dungeon_structure_prepare"
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                GalacticCoreIds.id("worldgen/dungeon_structure_host/" + plan.body()),
                hostAction.surface(),
                hostAction.action(),
                hostAction.evidence()
        );
        return new HostCallbackResult(
                plan.ready(),
                plan.status(),
                List.of(action),
                Map.of(
                        "requesterId", callback.requesterId(),
                        "dungeonId", plan.dungeonId(),
                        "body", plan.body(),
                        "bossId", plan.bossId(),
                        "bossRoomId", plan.bossRoomId(),
                        "treasureRoomId", plan.treasureRoomId(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onBossEntitySpawn(BossEntitySpawnCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.BossEntitySpawnAdapterPlan plan = adapters.prepareBossEntitySpawn(
                new GalacticCoreRuntimeAdapters.BossEntitySpawnRequest(callback.dungeonId())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.eventAction(),
                "boss_entity_spawn"
        );
        return new HostCallbackResult(
                plan.ready(),
                plan.status(),
                List.of(action),
                Map.of(
                        "requesterId", callback.requesterId(),
                        "dungeonId", plan.dungeonId(),
                        "bossId", plan.bossId(),
                        "bossRoomId", plan.bossRoomId(),
                        "legacyEntitySource", plan.legacyEntitySource(),
                        "maxHealth", plan.maxHealth(),
                        "requiredHostActions", plan.requiredHostActions(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onBossEncounterTick(BossEncounterCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.BossEncounterAdapterPlan plan = adapters.tickBossEncounter(
                new GalacticCoreRuntimeAdapters.BossEncounterRequest(callback.state(), callback.input())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.eventAction(),
                "boss_encounter_tick"
        );
        return new HostCallbackResult(
                true,
                plan.status(),
                List.of(action),
                Map.of(
                        "requesterId", callback.requesterId(),
                        "bossId", plan.nextState().bossId(),
                        "defeated", plan.defeated(),
                        "droppedKeys", plan.droppedKeys(),
                        "saveDataTarget", plan.saveDataTarget(),
                        "nextState", plan.nextState().toString()
                )
        );
    }

    public HostCallbackResult onBossAiStep(BossAiCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.BossAiAdapterPlan plan = adapters.tickBossAi(
                new GalacticCoreRuntimeAdapters.BossAiRequest(callback.state(), callback.input())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.eventAction(),
                "boss_ai_step"
        );
        return new HostCallbackResult(
                true,
                plan.status(),
                List.of(action),
                Map.of(
                        "requesterId", callback.requesterId(),
                        "bossId", plan.nextState().bossId(),
                        "movementIntent", plan.movementIntent(),
                        "attackIntent", plan.attackIntent(),
                        "requiredHostActions", plan.requiredHostActions(),
                        "roomLocked", plan.roomLocked(),
                        "saveDataTarget", plan.saveDataTarget(),
                        "nextState", plan.nextState().toString()
                )
        );
    }

    public HostCallbackResult onTreasureInteraction(TreasureInteractionCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.TreasureInteractionPlan plan = adapters.openDungeonTreasure(
                new GalacticCoreRuntimeAdapters.TreasureInteractionRequest(callback.progression(), callback.interaction())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.networkAction(),
                "dungeon_treasure_interaction"
        );
        return new HostCallbackResult(
                plan.opened(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "opened", plan.opened(),
                        "consumedKey", plan.consumedKey(),
                        "loot", plan.loot(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onTreasureChestScreenOpen(TreasureInteractionCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.TreasureChestScreenPlan plan = adapters.openTreasureChestScreen(
                new GalacticCoreRuntimeAdapters.TreasureInteractionRequest(callback.progression(), callback.interaction())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.screenAction(),
                "treasure_chest_screen_open"
        );
        return new HostCallbackResult(
                plan.openable(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", callback.playerId(),
                        "screenId", plan.screenId(),
                        "lootPreview", plan.lootPreview(),
                        "schematicRewards", plan.schematicRewards(),
                        "unlockedRoutes", plan.unlockedRoutes(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    public HostCallbackResult onTreasureChestMenuRender(TreasureInteractionCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan plan = adapters.openRenderedTreasureChestMenu(
                new GalacticCoreRuntimeAdapters.TreasureInteractionRequest(callback.progression(), callback.interaction())
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.screenAction(),
                "treasure_chest_menu_render"
        );
        return renderedMenuResult(callback.playerId(), plan, action);
    }

    public HostCallbackResult onDungeonTreasureClaim(DungeonTreasureClaimCallback callback) {
        Objects.requireNonNull(callback, "callback");
        GalacticCoreRuntimeAdapters.DungeonEncounterPlan plan = adapters.resolveDungeonEncounter(
                new GalacticCoreRuntimeAdapters.DungeonEncounterRequest(
                        callback.progression(),
                        callback.dungeonId(),
                        callback.bossId(),
                        callback.keyId(),
                        callback.bossDefeated(),
                        callback.hasKey()
                )
        );
        GalacticCoreRuntimeGateway.RuntimeAction action = withHostEvidence(
                plan.rewardAction(),
                "dungeon_treasure_claim"
        );
        return new HostCallbackResult(
                plan.rewardClaimed(),
                plan.status(),
                List.of(action),
                Map.of("playerId", callback.playerId(), "dungeonId", callback.dungeonId(), "bossId", callback.bossId())
        );
    }

    public List<GalacticCoreRuntimeGateway.RuntimeAction> releaseHostCallbackSmokeActions() {
        GalacticCoreRuntimeService.PlayerProgression progression = GalacticCoreRuntimeService.PlayerProgression.starting();
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = new ArrayList<>();
        actions.addAll(onMachineBlockEntityTick(new MachineTickCallback(
                GalacticCoreIds.id("block_entity/oxygen_collector"),
                "oxygen_collector",
                new GalacticCoreRuntimeService.MachineSnapshot(
                        GalacticCoreRuntimeService.MachineType.OXYGEN_COLLECTOR,
                        100,
                        0,
                        0,
                        0,
                        0,
                        true,
                        false,
                        false,
                        ""
                ),
                new GalacticCoreRuntimeService.MachineInput(8, 0, 0, false, false)
        )).actions());
        actions.addAll(onPlayerLifeSupportTick(new LifeSupportTickCallback(
                "player/smoke",
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                )
        )).actions());
        actions.addAll(onCelestialRouteSelection(new RouteSelectionCallback(
                "player/smoke",
                progression,
                GalacticCoreIds.id("route/moon")
        )).actions());
        actions.addAll(onHoloMapRouteSurfaceOpen(new HoloMapRouteSurfaceCallback(
                "player/smoke",
                progression,
                GalacticCoreIds.id("route/mars")
        )).actions());
        actions.addAll(onHoloMapRouteInteraction(new HoloMapRouteInteractionCallback(
                "player/smoke",
                progression,
                GalacticCoreIds.id("route/moon"),
                "select_route"
        )).actions());
        actions.addAll(onDimensionTransferRequest(new DimensionTransferCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                GalacticCoreIds.id("moon")
        )).actions());
        actions.addAll(onTransferPlacementPrepare(new TransferPlacementCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true),
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
        )).actions());
        actions.addAll(onDimensionTransferExecute(new TransferPlacementCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true),
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
        )).actions());
        actions.addAll(onScreenCoreLaunchChecklistOpen(new LaunchChecklistSurfaceCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                )
        )).actions());
        actions.addAll(onHoloMapRouteMenuRender(new HoloMapRouteSurfaceCallback(
                "player/smoke",
                progression,
                GalacticCoreIds.id("route/mars")
        )).actions());
        actions.addAll(onScreenCoreLaunchChecklistMenuRender(new LaunchChecklistSurfaceCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                )
        )).actions());
        actions.addAll(onScreenCoreLaunchChecklistInteraction(new LaunchChecklistInteractionCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                ),
                "start_countdown"
        )).actions());
        actions.addAll(onEnvironmentScan(new EnvironmentScanCallback(
                "lens/smoke",
                GalacticCoreIds.id("venus")
        )).actions());
        actions.addAll(onDungeonStructurePrepare(new DungeonStructureCallback(
                "worldgen/smoke",
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
        )).actions());
        actions.addAll(onBossEntitySpawn(new BossEntitySpawnCallback(
                "boss/smoke",
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
        )).actions());
        HostCallbackResult boss = onBossEncounterTick(new BossEncounterCallback(
                "boss/smoke",
                runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
        ));
        actions.addAll(boss.actions());
        actions.addAll(onBossAiStep(new BossAiCallback(
                "boss/smoke",
                runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
        )).actions());
        actions.addAll(onTreasureInteraction(new TreasureInteractionCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        Boolean.TRUE.equals(boss.evidence().get("defeated")),
                        !boss.evidence().get("droppedKeys").toString().equals("[]"),
                        true,
                        true
                )
        )).actions());
        actions.addAll(onTreasureChestScreenOpen(new TreasureInteractionCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        Boolean.TRUE.equals(boss.evidence().get("defeated")),
                        !boss.evidence().get("droppedKeys").toString().equals("[]"),
                        true,
                        true
                )
        )).actions());
        actions.addAll(onTreasureChestMenuRender(new TreasureInteractionCallback(
                "player/smoke",
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        Boolean.TRUE.equals(boss.evidence().get("defeated")),
                        !boss.evidence().get("droppedKeys").toString().equals("[]"),
                        true,
                        true
                )
        )).actions());
        actions.addAll(onDungeonTreasureClaim(new DungeonTreasureClaimCallback(
                "player/smoke",
                progression,
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                "tier_1_key",
                true,
                true
        )).actions());
        return List.copyOf(actions);
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_host_callbacks",
                "typedReceiptsOnly", true,
                "callbacks", "machine_tick, life_support_tick, route_selection, holomap_route_surface, holomap_route_menu_render, holomap_route_interaction, screencore_launch_checklist, screencore_launch_checklist_menu_render, screencore_launch_checklist_interaction, dimension_transfer, transfer_placement, transfer_execution, environment_scan, dungeon_structure_prepare, boss_entity_spawn, boss_encounter_tick, boss_ai_step, dungeon_treasure_interaction, treasure_chest_screen_open, treasure_chest_menu_render, dungeon_treasure_claim",
                "replaces", "Forge event handlers, TileEntity.update, WorldProvider travel hooks, transfer execution, GuiHandler callbacks, GuiCelestialSelection, GuiPreLaunchChecklist, GuiTreasureChest, HoloMap widget clicks, ScreenCore checklist controls and rendered menus, DungeonConfiguration hooks, EntityBoss construction/AI/update/drop hooks, direct treasure chest reward mutation"
        );
    }

    private static HostCallbackResult renderedMenuResult(
            String playerId,
            GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan plan,
            GalacticCoreRuntimeGateway.RuntimeAction action
    ) {
        return new HostCallbackResult(
                plan.accepted(),
                plan.status(),
                List.of(action),
                Map.of(
                        "playerId", playerId,
                        "screenId", plan.screenId(),
                        "rendererId", plan.rendererId(),
                        "widgetCount", plan.widgetCount(),
                        "saveDataTarget", plan.saveDataTarget()
                )
        );
    }

    private static GalacticCoreRuntimeGateway.RuntimeAction withHostEvidence(
            GalacticCoreRuntimeGateway.RuntimeAction action,
            String callback
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>(action.evidence());
        evidence.put("gatewaySource", action.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_host_callbacks");
        evidence.put("hostCallback", callback);
        return new GalacticCoreRuntimeGateway.RuntimeAction(action.target(), action.surface(), action.action(), evidence);
    }

    public record MachineTickCallback(
            String blockEntityId,
            String machinePath,
            GalacticCoreRuntimeService.MachineSnapshot snapshot,
            GalacticCoreRuntimeService.MachineInput input
    ) {
        public MachineTickCallback {
            blockEntityId = requireText(blockEntityId, "blockEntityId");
            machinePath = requireText(machinePath, "machinePath");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            input = Objects.requireNonNull(input, "input");
        }
    }

    public record LifeSupportTickCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment
    ) {
        public LifeSupportTickCallback {
            playerId = requireText(playerId, "playerId");
            gear = Objects.requireNonNull(gear, "gear");
            environment = Objects.requireNonNull(environment, "environment");
        }
    }

    public record RouteSelectionCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute
    ) {
        public RouteSelectionCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            selectedRoute = requireText(selectedRoute, "selectedRoute");
        }
    }

    public record HoloMapRouteSurfaceCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute
    ) {
        public HoloMapRouteSurfaceCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            selectedRoute = requireText(selectedRoute, "selectedRoute");
        }
    }

    public record LaunchChecklistSurfaceCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String routeId,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment
    ) {
        public LaunchChecklistSurfaceCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            routeId = requireText(routeId, "routeId");
            gear = Objects.requireNonNull(gear, "gear");
            environment = Objects.requireNonNull(environment, "environment");
        }
    }

    public record HoloMapRouteInteractionCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute,
            String actionId
    ) {
        public HoloMapRouteInteractionCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            selectedRoute = requireText(selectedRoute, "selectedRoute");
            actionId = requireText(actionId, "actionId");
        }
    }

    public record LaunchChecklistInteractionCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String routeId,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment,
            String actionId
    ) {
        public LaunchChecklistInteractionCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            routeId = requireText(routeId, "routeId");
            gear = Objects.requireNonNull(gear, "gear");
            environment = Objects.requireNonNull(environment, "environment");
            actionId = requireText(actionId, "actionId");
        }
    }

    public record DimensionTransferCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String destinationRoute,
            String destinationEnvironment,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.TransferAnchor anchor
    ) {
        public DimensionTransferCallback(
                String playerId,
                GalacticCoreRuntimeService.PlayerProgression progression,
                GalacticCoreRuntimeService.RocketLaunchState launchState,
                String destinationRoute,
                String destinationEnvironment
        ) {
            this(
                    playerId,
                    progression,
                    launchState,
                    destinationRoute,
                    destinationEnvironment,
                    new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true),
                    new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
            );
        }

        public DimensionTransferCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            destinationRoute = requireText(destinationRoute, "destinationRoute");
            destinationEnvironment = requireText(destinationEnvironment, "destinationEnvironment");
            gear = Objects.requireNonNull(gear, "gear");
            anchor = Objects.requireNonNull(anchor, "anchor");
        }
    }

    public record TransferPlacementCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String destinationRoute,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.TransferAnchor anchor
    ) {
        public TransferPlacementCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            destinationRoute = requireText(destinationRoute, "destinationRoute");
            gear = Objects.requireNonNull(gear, "gear");
            anchor = Objects.requireNonNull(anchor, "anchor");
        }
    }

    public record EnvironmentScanCallback(String requesterId, String environmentId) {
        public EnvironmentScanCallback {
            requesterId = requireText(requesterId, "requesterId");
            environmentId = requireText(environmentId, "environmentId");
        }
    }

    public record DungeonStructureCallback(String requesterId, String dungeonId) {
        public DungeonStructureCallback {
            requesterId = requireText(requesterId, "requesterId");
            dungeonId = requireText(dungeonId, "dungeonId");
        }
    }

    public record BossEncounterCallback(
            String requesterId,
            GalacticCoreRuntimeService.BossEncounterState state,
            GalacticCoreRuntimeService.BossEncounterInput input
    ) {
        public BossEncounterCallback {
            requesterId = requireText(requesterId, "requesterId");
            state = Objects.requireNonNull(state, "state");
            input = Objects.requireNonNull(input, "input");
        }
    }

    public record BossEntitySpawnCallback(String requesterId, String dungeonId) {
        public BossEntitySpawnCallback {
            requesterId = requireText(requesterId, "requesterId");
            dungeonId = requireText(dungeonId, "dungeonId");
        }
    }

    public record BossAiCallback(
            String requesterId,
            GalacticCoreRuntimeService.BossEncounterState state,
            GalacticCoreRuntimeService.BossAiInput input
    ) {
        public BossAiCallback {
            requesterId = requireText(requesterId, "requesterId");
            state = Objects.requireNonNull(state, "state");
            input = Objects.requireNonNull(input, "input");
        }
    }

    public record TreasureInteractionCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.TreasureInteraction interaction
    ) {
        public TreasureInteractionCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            interaction = Objects.requireNonNull(interaction, "interaction");
        }
    }

    public record DungeonTreasureClaimCallback(
            String playerId,
            GalacticCoreRuntimeService.PlayerProgression progression,
            String dungeonId,
            String bossId,
            String keyId,
            boolean bossDefeated,
            boolean hasKey
    ) {
        public DungeonTreasureClaimCallback {
            playerId = requireText(playerId, "playerId");
            progression = Objects.requireNonNull(progression, "progression");
            dungeonId = requireText(dungeonId, "dungeonId");
            bossId = requireText(bossId, "bossId");
            keyId = requireText(keyId, "keyId");
        }
    }

    public record HostCallbackResult(
            boolean accepted,
            String status,
            List<GalacticCoreRuntimeGateway.RuntimeAction> actions,
            Map<String, Object> evidence
    ) {
        public HostCallbackResult {
            status = requireText(status, "status");
            actions = List.copyOf(actions == null ? List.of() : actions);
            evidence = Map.copyOf(evidence == null ? Map.of() : evidence);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
