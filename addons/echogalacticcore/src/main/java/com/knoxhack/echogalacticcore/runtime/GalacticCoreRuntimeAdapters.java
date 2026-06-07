package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreRuntimeAdapters {
    private final GalacticCoreRuntimeService runtime;
    private final GalacticCoreRuntimeGateway gateway;

    public GalacticCoreRuntimeAdapters(GalacticCoreRuntimeService runtime, GalacticCoreRuntimeGateway gateway) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public MachineAdapterResult tickMachineBlockEntity(MachineBlockEntityAdapter adapter, GalacticCoreRuntimeService.MachineInput input) {
        Objects.requireNonNull(adapter, "adapter");
        GalacticCoreRuntimeGateway.RuntimeAction tick = gateway.publishMachineTick(adapter.snapshot(), input);
        GalacticCoreRuntimeService.MachineSnapshot next = runtime.tickMachine(adapter.snapshot(), input);
        return new MachineAdapterResult(
                adapter.withSnapshot(next),
                tick,
                GalacticCoreIds.id("machine_state/" + adapter.path()),
                !adapter.snapshot().equals(next)
        );
    }

    public DimensionTransferPlan prepareDimensionTransfer(DimensionTransferRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeService.RouteRequirement route = runtime.routeRequirement(request.destinationRoute(), request.progression());
        GalacticCoreRuntimeGateway.RuntimeAction checklist = gateway.openLaunchChecklist(request.launchState(), route);
        GalacticCoreRuntimeGateway.RuntimeAction routeAction = gateway.sendRouteAction(request.launchState(), route);
        GalacticCoreRuntimeGateway.RuntimeAction scan = gateway.scanEnvironment(request.destinationEnvironment());
        TransferPlacementAdapterPlan placement = prepareTransferPlacement(new TransferPlacementRequest(
                request.progression(),
                request.launchState(),
                request.destinationRoute(),
                request.gear(),
                request.anchor()
        ));
        boolean ready = Boolean.TRUE.equals(routeAction.evidence().get("ready"));
        boolean transferReady = ready && placement.ready();
        String status = transferReady ? "transfer_ready" : (!ready ? String.valueOf(routeAction.evidence().get("reason")) : placement.status());
        return new DimensionTransferPlan(
                transferReady,
                request.destinationRoute(),
                request.destinationEnvironment(),
                status,
                List.of(checklist, routeAction, scan, placement.worldgenAction())
        );
    }

    public TransferPlacementAdapterPlan prepareTransferPlacement(TransferPlacementRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.placeTransferLandingZone(
                request.progression(),
                request.launchState(),
                request.destinationRoute(),
                request.gear(),
                request.anchor()
        );
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "dimension_transfer_placement");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new TransferPlacementAdapterPlan(
                Boolean.TRUE.equals(gatewayAction.evidence().get("ready")),
                String.valueOf(gatewayAction.evidence().get("status")),
                String.valueOf(gatewayAction.evidence().get("placementId")),
                GalacticCoreIds.id("transfer_state/" + request.destinationRoute().substring(request.destinationRoute().lastIndexOf('/') + 1)),
                action
        );
    }

    public TransferExecutionAdapterPlan executeDimensionTransfer(TransferPlacementRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishTransferExecution(
                request.progression(),
                request.launchState(),
                request.destinationRoute(),
                request.gear(),
                request.anchor()
        );
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "dimension_transfer_execution");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new TransferExecutionAdapterPlan(
                Boolean.TRUE.equals(gatewayAction.evidence().get("ready")),
                String.valueOf(gatewayAction.evidence().get("status")),
                String.valueOf(gatewayAction.evidence().get("environment")),
                (int) gatewayAction.evidence().get("x"),
                (int) gatewayAction.evidence().get("y"),
                (int) gatewayAction.evidence().get("z"),
                GalacticCoreIds.id("transfer_execution_state/" + request.destinationRoute().substring(request.destinationRoute().lastIndexOf('/') + 1)),
                action
        );
    }

    public ScreenSurfaceAdapterPlan openHoloMapRouteSurface(RouteSurfaceRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.openHoloMapRouteSurface(
                request.progression(),
                request.selectedRoute()
        );
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "holomap_route_surface");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new ScreenSurfaceAdapterPlan(
                Boolean.TRUE.equals(gatewayAction.evidence().get("selectedUnlocked")),
                String.valueOf(gatewayAction.evidence().get("status")),
                gatewayAction.target(),
                GalacticCoreIds.id("screen_state/holomap_routes"),
                action
        );
    }

    public ScreenSurfaceAdapterPlan openScreenCoreLaunchChecklist(LaunchChecklistSurfaceRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeService.RouteRequirement route = runtime.routeRequirement(request.routeId(), request.progression());
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.openScreenCoreLaunchChecklist(
                request.launchState(),
                route,
                request.gear(),
                request.environment()
        );
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "screencore_launch_checklist");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new ScreenSurfaceAdapterPlan(
                Boolean.TRUE.equals(gatewayAction.evidence().get("ready")),
                String.valueOf(gatewayAction.evidence().get("status")),
                gatewayAction.target(),
                GalacticCoreIds.id("screen_state/screencore_launch_checklist"),
                action
        );
    }

    public RenderedMenuAdapterPlan openRenderedHoloMapRouteMenu(RouteSurfaceRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.openRenderedHoloMapRouteMenu(
                request.progression(),
                request.selectedRoute()
        );
        return renderedMenuAdapterPlan(gatewayAction, "holomap_route_rendered_menu");
    }

    public RenderedMenuAdapterPlan openRenderedLaunchChecklistMenu(LaunchChecklistSurfaceRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeService.RouteRequirement route = runtime.routeRequirement(request.routeId(), request.progression());
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.openRenderedLaunchChecklistMenu(
                request.launchState(),
                route,
                request.gear(),
                request.environment()
        );
        return renderedMenuAdapterPlan(gatewayAction, "screencore_launch_checklist_rendered_menu");
    }

    public RenderedMenuAdapterPlan openRenderedTreasureChestMenu(TreasureInteractionRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.openRenderedTreasureChestMenu(
                request.progression(),
                request.interaction()
        );
        return renderedMenuAdapterPlan(gatewayAction, "treasure_chest_rendered_menu");
    }

    public ScreenInteractionAdapterPlan interactHoloMapRoute(ScreenInteractionRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishHoloMapRouteInteraction(
                request.progression(),
                request.selectedRoute(),
                request.actionId()
        );
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "holomap_route_interaction");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new ScreenInteractionAdapterPlan(
                Boolean.TRUE.equals(gatewayAction.evidence().get("accepted")),
                String.valueOf(gatewayAction.evidence().get("status")),
                String.valueOf(gatewayAction.evidence().get("screenId")),
                String.valueOf(gatewayAction.evidence().get("actionId")),
                GalacticCoreIds.id("screen_state/holomap_routes/interaction"),
                action
        );
    }

    public ScreenInteractionAdapterPlan interactScreenCoreLaunchChecklist(LaunchChecklistInteractionRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeService.RouteRequirement route = runtime.routeRequirement(request.routeId(), request.progression());
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishScreenCoreLaunchChecklistInteraction(
                request.launchState(),
                route,
                request.gear(),
                request.environment(),
                request.actionId()
        );
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "screencore_launch_checklist_interaction");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new ScreenInteractionAdapterPlan(
                Boolean.TRUE.equals(gatewayAction.evidence().get("accepted")),
                String.valueOf(gatewayAction.evidence().get("status")),
                String.valueOf(gatewayAction.evidence().get("screenId")),
                String.valueOf(gatewayAction.evidence().get("actionId")),
                GalacticCoreIds.id("screen_state/screencore_launch_checklist/interaction"),
                action
        );
    }

    public DungeonStructureAdapterPlan prepareDungeonStructure(DungeonStructureRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeService.DungeonStructurePlan plan = runtime.planDungeonStructure(request.dungeonId());
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishDungeonStructurePlan(request.dungeonId());
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("gatewayTarget", gatewayAction.target());
        evidence.put("adapter", "dungeon_structure_generation");
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                GalacticCoreIds.id("worldgen/dungeon_structure_adapter/" + plan.body()),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        return new DungeonStructureAdapterPlan(
                true,
                "structure_ready",
                plan.dungeonId(),
                plan.body(),
                plan.bossId(),
                plan.bossRoomId(),
                plan.treasureRoomId(),
                GalacticCoreIds.id("dungeon_structure_state/" + plan.body()),
                action
        );
    }

    public DungeonEncounterPlan resolveDungeonEncounter(DungeonEncounterRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction reward = gateway.claimDungeonReward(
                request.progression(),
                new GalacticCoreRuntimeService.DungeonRewardClaim(
                        request.dungeonId(),
                        request.bossId(),
                        request.keyId(),
                        request.bossDefeated(),
                        request.hasKey()
                )
        );
        boolean claimed = Boolean.TRUE.equals(reward.evidence().get("claimed"));
        String status = String.valueOf(reward.evidence().get("status"));
        return new DungeonEncounterPlan(
                claimed,
                status,
                request.dungeonId(),
                request.bossId(),
                reward
        );
    }

    public BossEntitySpawnAdapterPlan prepareBossEntitySpawn(BossEntitySpawnRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishBossEntitySpawn(request.dungeonId());
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "boss_entity_spawn");
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        GalacticCoreRuntimeService.BossEntitySpawnPlan plan = runtime.planBossEntitySpawn(request.dungeonId());
        return new BossEntitySpawnAdapterPlan(
                plan.ready(),
                plan.status(),
                plan.dungeonId(),
                plan.bossId(),
                plan.bossRoomId(),
                plan.legacyEntitySource(),
                plan.maxHealth(),
                plan.requiredHostActions(),
                GalacticCoreIds.id("boss_entity_state/" + plan.body()),
                action
        );
    }

    public BossEncounterAdapterPlan tickBossEncounter(BossEncounterRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishBossEncounterTick(request.state(), request.input());
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "boss_encounter");
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        GalacticCoreRuntimeService.BossEncounterResult result = runtime.tickBossEncounter(request.state(), request.input());
        String body = runtime.planDungeonStructure(request.state().dungeonId()).body();
        return new BossEncounterAdapterPlan(
                result.defeated(),
                result.status(),
                result.state(),
                result.droppedKeys(),
                GalacticCoreIds.id("boss_encounter_state/" + body),
                action
        );
    }

    public BossAiAdapterPlan tickBossAi(BossAiRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishBossAiStep(request.state(), request.input());
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "boss_ai_step");
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        GalacticCoreRuntimeService.BossAiStep step = runtime.planBossAiStep(request.state(), request.input());
        String body = runtime.planDungeonStructure(request.state().dungeonId()).body();
        return new BossAiAdapterPlan(
                step.nextState(),
                step.movementIntent(),
                step.attackIntent(),
                step.requiredHostActions(),
                step.roomLocked(),
                step.status(),
                GalacticCoreIds.id("boss_ai_state/" + body),
                action
        );
    }

    public TreasureInteractionPlan openDungeonTreasure(TreasureInteractionRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.publishTreasureInteraction(request.progression(), request.interaction());
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "treasure_interaction");
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        GalacticCoreRuntimeService.TreasureInteractionResult result = runtime.openDungeonTreasure(request.progression(), request.interaction());
        String body = runtime.planDungeonStructure(request.interaction().dungeonId()).body();
        return new TreasureInteractionPlan(
                result.opened(),
                result.consumedKey(),
                result.status(),
                result.progression(),
                result.loot(),
                GalacticCoreIds.id("treasure_state/" + body),
                action
        );
    }

    public TreasureChestScreenPlan openTreasureChestScreen(TreasureInteractionRequest request) {
        Objects.requireNonNull(request, "request");
        GalacticCoreRuntimeGateway.RuntimeAction gatewayAction = gateway.openTreasureChestScreen(request.progression(), request.interaction());
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", "treasure_chest_screen");
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        GalacticCoreRuntimeService.TreasureChestSurface surface = runtime.treasureChestSurface(request.progression(), request.interaction());
        String body = runtime.planDungeonStructure(request.interaction().dungeonId()).body();
        return new TreasureChestScreenPlan(
                surface.openable(),
                surface.status(),
                surface.screenId(),
                surface.lootPreview(),
                surface.schematicRewards(),
                surface.unlockedRoutes(),
                GalacticCoreIds.id("screen_state/treasure_chest/" + body),
                action
        );
    }

    public List<GalacticCoreRuntimeGateway.RuntimeAction> releaseAdapterSmokeActions() {
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = new ArrayList<>();
        MachineAdapterResult machine = tickMachineBlockEntity(
                new MachineBlockEntityAdapter(
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
                        )
                ),
                new GalacticCoreRuntimeService.MachineInput(8, 0, 0, false, false)
        );
        actions.add(machine.tickAction());

        GalacticCoreRuntimeService.PlayerProgression progression = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.RocketLaunchState launchState = new GalacticCoreRuntimeService.RocketLaunchState(
                1000,
                1000,
                true,
                true,
                true,
                1,
                200
        );
        DimensionTransferPlan transfer = prepareDimensionTransfer(new DimensionTransferRequest(
                progression,
                launchState,
                GalacticCoreIds.id("route/moon"),
                GalacticCoreIds.id("moon")
        ));
        actions.addAll(transfer.actions());
        TransferExecutionAdapterPlan execution = executeDimensionTransfer(new TransferPlacementRequest(
                progression,
                launchState,
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true),
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
        ));
        actions.add(execution.eventAction());

        ScreenSurfaceAdapterPlan routeSurface = openHoloMapRouteSurface(new RouteSurfaceRequest(
                progression,
                GalacticCoreIds.id("route/mars")
        ));
        actions.add(routeSurface.screenAction());
        ScreenInteractionAdapterPlan routeInteraction = interactHoloMapRoute(new ScreenInteractionRequest(
                progression,
                GalacticCoreIds.id("route/moon"),
                "select_route"
        ));
        actions.add(routeInteraction.networkAction());

        ScreenSurfaceAdapterPlan checklistSurface = openScreenCoreLaunchChecklist(new LaunchChecklistSurfaceRequest(
                progression,
                launchState,
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                )
        ));
        actions.add(checklistSurface.screenAction());
        RenderedMenuAdapterPlan routeMenu = openRenderedHoloMapRouteMenu(new RouteSurfaceRequest(
                progression,
                GalacticCoreIds.id("route/mars")
        ));
        actions.add(routeMenu.screenAction());
        RenderedMenuAdapterPlan checklistMenu = openRenderedLaunchChecklistMenu(new LaunchChecklistSurfaceRequest(
                progression,
                launchState,
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                )
        ));
        actions.add(checklistMenu.screenAction());
        ScreenInteractionAdapterPlan checklistInteraction = interactScreenCoreLaunchChecklist(new LaunchChecklistInteractionRequest(
                progression,
                launchState,
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(
                        GalacticCoreIds.id("moon"),
                        GalacticCoreRuntimeService.Atmosphere.VACUUM,
                        GalacticCoreRuntimeService.ThermalRisk.COLD
                ),
                "start_countdown"
        ));
        actions.add(checklistInteraction.networkAction());

        DungeonStructureAdapterPlan structure = prepareDungeonStructure(new DungeonStructureRequest(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
        ));
        actions.add(structure.worldgenAction());

        BossEntitySpawnAdapterPlan bossSpawn = prepareBossEntitySpawn(new BossEntitySpawnRequest(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
        ));
        actions.add(bossSpawn.eventAction());

        BossEncounterAdapterPlan boss = tickBossEncounter(new BossEncounterRequest(
                runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
        ));
        actions.add(boss.eventAction());

        BossAiAdapterPlan bossAi = tickBossAi(new BossAiRequest(
                runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
        ));
        actions.add(bossAi.eventAction());

        TreasureInteractionPlan treasure = openDungeonTreasure(new TreasureInteractionRequest(
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        boss.defeated(),
                        !boss.droppedKeys().isEmpty(),
                        true,
                        true
                )
        ));
        actions.add(treasure.networkAction());

        TreasureChestScreenPlan treasureScreen = openTreasureChestScreen(new TreasureInteractionRequest(
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        boss.defeated(),
                        !boss.droppedKeys().isEmpty(),
                        true,
                        true
                )
        ));
        actions.add(treasureScreen.screenAction());
        RenderedMenuAdapterPlan treasureMenu = openRenderedTreasureChestMenu(new TreasureInteractionRequest(
                progression,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        boss.defeated(),
                        !boss.droppedKeys().isEmpty(),
                        true,
                        true
                )
        ));
        actions.add(treasureMenu.screenAction());

        DungeonEncounterPlan encounter = resolveDungeonEncounter(new DungeonEncounterRequest(
                progression,
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                "tier_1_key",
                true,
                true
        ));
        actions.add(encounter.rewardAction());
        return List.copyOf(actions);
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_runtime_adapters",
                "typedReceiptsOnly", true,
                "adapters", "machine_block_entities, dimension_transfer, dimension_transfer_placement, dimension_transfer_execution, holomap_route_surface, holomap_route_rendered_menu, holomap_route_interaction, screencore_launch_checklist, screencore_launch_checklist_rendered_menu, screencore_launch_checklist_interaction, treasure_chest_screen, treasure_chest_rendered_menu, dungeon_structure_generation, boss_entity_spawns, boss_encounters, boss_ai_steps, treasure_interactions, dungeon_encounters, treasure_rewards",
                "replaces", "TileEntity tick hooks, WorldProvider transfer code, landing placement/execution, GuiCelestialSelection, GuiPreLaunchChecklist, GuiTreasureChest, HoloMap widget clicks, ScreenCore checklist controls and rendered menus, DungeonConfiguration room selection, EntityBoss construction/AI/update/reward mutation, TileEntityTreasureChest coupling"
        );
    }

    private RenderedMenuAdapterPlan renderedMenuAdapterPlan(
            GalacticCoreRuntimeGateway.RuntimeAction gatewayAction,
            String adapter
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>(gatewayAction.evidence());
        evidence.put("gatewaySource", gatewayAction.evidence().get("source"));
        evidence.put("source", "galacticraft_legacy_runtime_adapters");
        evidence.put("adapter", adapter);
        evidence.put("gatewayTarget", gatewayAction.target());
        GalacticCoreRuntimeGateway.RuntimeAction action = new GalacticCoreRuntimeGateway.RuntimeAction(
                gatewayAction.target(),
                gatewayAction.surface(),
                gatewayAction.action(),
                evidence
        );
        String screenId = String.valueOf(gatewayAction.evidence().get("screenId"));
        String path = screenId.substring(screenId.lastIndexOf('/') + 1);
        return new RenderedMenuAdapterPlan(
                true,
                String.valueOf(gatewayAction.evidence().get("status")),
                screenId,
                String.valueOf(gatewayAction.evidence().get("rendererId")),
                (int) gatewayAction.evidence().get("widgetCount"),
                GalacticCoreIds.id("screen_layout_state/" + path),
                action
        );
    }

    public record MachineBlockEntityAdapter(
            String blockEntityId,
            String path,
            GalacticCoreRuntimeService.MachineSnapshot snapshot
    ) {
        public MachineBlockEntityAdapter {
            blockEntityId = requireText(blockEntityId, "blockEntityId");
            path = requireText(path, "path");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }

        public MachineBlockEntityAdapter withSnapshot(GalacticCoreRuntimeService.MachineSnapshot next) {
            return new MachineBlockEntityAdapter(blockEntityId, path, next);
        }
    }

    public record MachineAdapterResult(
            MachineBlockEntityAdapter adapter,
            GalacticCoreRuntimeGateway.RuntimeAction tickAction,
            String saveDataTarget,
            boolean mutated
    ) {
        public MachineAdapterResult {
            adapter = Objects.requireNonNull(adapter, "adapter");
            tickAction = Objects.requireNonNull(tickAction, "tickAction");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
        }
    }

    public record DimensionTransferRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String destinationRoute,
            String destinationEnvironment,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.TransferAnchor anchor
    ) {
        public DimensionTransferRequest(
                GalacticCoreRuntimeService.PlayerProgression progression,
                GalacticCoreRuntimeService.RocketLaunchState launchState,
                String destinationRoute,
                String destinationEnvironment
        ) {
            this(
                    progression,
                    launchState,
                    destinationRoute,
                    destinationEnvironment,
                    new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true),
                    new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
            );
        }

        public DimensionTransferRequest {
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            destinationRoute = requireText(destinationRoute, "destinationRoute");
            destinationEnvironment = requireText(destinationEnvironment, "destinationEnvironment");
            gear = Objects.requireNonNull(gear, "gear");
            anchor = Objects.requireNonNull(anchor, "anchor");
        }
    }

    public record DimensionTransferPlan(
            boolean ready,
            String route,
            String environment,
            String status,
            List<GalacticCoreRuntimeGateway.RuntimeAction> actions
    ) {
        public DimensionTransferPlan {
            route = requireText(route, "route");
            environment = requireText(environment, "environment");
            status = requireText(status, "status");
            actions = List.copyOf(actions == null ? List.of() : actions);
        }
    }

    public record TransferPlacementRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String destinationRoute,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.TransferAnchor anchor
    ) {
        public TransferPlacementRequest {
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            destinationRoute = requireText(destinationRoute, "destinationRoute");
            gear = Objects.requireNonNull(gear, "gear");
            anchor = Objects.requireNonNull(anchor, "anchor");
        }
    }

    public record TransferPlacementAdapterPlan(
            boolean ready,
            String status,
            String placementId,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction worldgenAction
    ) {
        public TransferPlacementAdapterPlan {
            status = requireText(status, "status");
            placementId = requireText(placementId, "placementId");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            worldgenAction = Objects.requireNonNull(worldgenAction, "worldgenAction");
        }
    }

    public record TransferExecutionAdapterPlan(
            boolean ready,
            String status,
            String environmentId,
            int x,
            int y,
            int z,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction eventAction
    ) {
        public TransferExecutionAdapterPlan {
            status = requireText(status, "status");
            environmentId = requireText(environmentId, "environmentId");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            eventAction = Objects.requireNonNull(eventAction, "eventAction");
        }
    }

    public record RouteSurfaceRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute
    ) {
        public RouteSurfaceRequest {
            progression = Objects.requireNonNull(progression, "progression");
            selectedRoute = requireText(selectedRoute, "selectedRoute");
        }
    }

    public record LaunchChecklistSurfaceRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String routeId,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment
    ) {
        public LaunchChecklistSurfaceRequest {
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            routeId = requireText(routeId, "routeId");
            gear = Objects.requireNonNull(gear, "gear");
            environment = Objects.requireNonNull(environment, "environment");
        }
    }

    public record ScreenInteractionRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute,
            String actionId
    ) {
        public ScreenInteractionRequest {
            progression = Objects.requireNonNull(progression, "progression");
            selectedRoute = requireText(selectedRoute, "selectedRoute");
            actionId = requireText(actionId, "actionId");
        }
    }

    public record LaunchChecklistInteractionRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String routeId,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment,
            String actionId
    ) {
        public LaunchChecklistInteractionRequest {
            progression = Objects.requireNonNull(progression, "progression");
            launchState = Objects.requireNonNull(launchState, "launchState");
            routeId = requireText(routeId, "routeId");
            gear = Objects.requireNonNull(gear, "gear");
            environment = Objects.requireNonNull(environment, "environment");
            actionId = requireText(actionId, "actionId");
        }
    }

    public record ScreenSurfaceAdapterPlan(
            boolean accepted,
            String status,
            String screenId,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction screenAction
    ) {
        public ScreenSurfaceAdapterPlan {
            status = requireText(status, "status");
            screenId = requireText(screenId, "screenId");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            screenAction = Objects.requireNonNull(screenAction, "screenAction");
        }
    }

    public record ScreenInteractionAdapterPlan(
            boolean accepted,
            String status,
            String screenId,
            String actionId,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction networkAction
    ) {
        public ScreenInteractionAdapterPlan {
            status = requireText(status, "status");
            screenId = requireText(screenId, "screenId");
            actionId = requireText(actionId, "actionId");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            networkAction = Objects.requireNonNull(networkAction, "networkAction");
        }
    }

    public record RenderedMenuAdapterPlan(
            boolean accepted,
            String status,
            String screenId,
            String rendererId,
            int widgetCount,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction screenAction
    ) {
        public RenderedMenuAdapterPlan {
            status = requireText(status, "status");
            screenId = requireText(screenId, "screenId");
            rendererId = requireText(rendererId, "rendererId");
            widgetCount = Math.max(0, widgetCount);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            screenAction = Objects.requireNonNull(screenAction, "screenAction");
        }
    }

    public record DungeonEncounterRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            String dungeonId,
            String bossId,
            String keyId,
            boolean bossDefeated,
            boolean hasKey
    ) {
        public DungeonEncounterRequest {
            progression = Objects.requireNonNull(progression, "progression");
            dungeonId = requireText(dungeonId, "dungeonId");
            bossId = requireText(bossId, "bossId");
            keyId = requireText(keyId, "keyId");
        }
    }

    public record DungeonStructureRequest(String dungeonId) {
        public DungeonStructureRequest {
            dungeonId = requireText(dungeonId, "dungeonId");
        }
    }

    public record DungeonStructureAdapterPlan(
            boolean ready,
            String status,
            String dungeonId,
            String body,
            String bossId,
            String bossRoomId,
            String treasureRoomId,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction worldgenAction
    ) {
        public DungeonStructureAdapterPlan {
            status = requireText(status, "status");
            dungeonId = requireText(dungeonId, "dungeonId");
            body = requireText(body, "body");
            bossId = requireText(bossId, "bossId");
            bossRoomId = requireText(bossRoomId, "bossRoomId");
            treasureRoomId = requireText(treasureRoomId, "treasureRoomId");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            worldgenAction = Objects.requireNonNull(worldgenAction, "worldgenAction");
        }
    }

    public record BossEncounterRequest(
            GalacticCoreRuntimeService.BossEncounterState state,
            GalacticCoreRuntimeService.BossEncounterInput input
    ) {
        public BossEncounterRequest {
            state = Objects.requireNonNull(state, "state");
            input = Objects.requireNonNull(input, "input");
        }
    }

    public record BossEntitySpawnRequest(String dungeonId) {
        public BossEntitySpawnRequest {
            dungeonId = requireText(dungeonId, "dungeonId");
        }
    }

    public record BossEntitySpawnAdapterPlan(
            boolean ready,
            String status,
            String dungeonId,
            String bossId,
            String bossRoomId,
            String legacyEntitySource,
            int maxHealth,
            List<String> requiredHostActions,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction eventAction
    ) {
        public BossEntitySpawnAdapterPlan {
            status = requireText(status, "status");
            dungeonId = requireText(dungeonId, "dungeonId");
            bossId = requireText(bossId, "bossId");
            bossRoomId = requireText(bossRoomId, "bossRoomId");
            legacyEntitySource = requireText(legacyEntitySource, "legacyEntitySource");
            maxHealth = Math.max(1, maxHealth);
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            eventAction = Objects.requireNonNull(eventAction, "eventAction");
        }
    }

    public record BossEncounterAdapterPlan(
            boolean defeated,
            String status,
            GalacticCoreRuntimeService.BossEncounterState nextState,
            List<String> droppedKeys,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction eventAction
    ) {
        public BossEncounterAdapterPlan {
            status = requireText(status, "status");
            nextState = Objects.requireNonNull(nextState, "nextState");
            droppedKeys = List.copyOf(droppedKeys == null ? List.of() : droppedKeys);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            eventAction = Objects.requireNonNull(eventAction, "eventAction");
        }
    }

    public record BossAiRequest(
            GalacticCoreRuntimeService.BossEncounterState state,
            GalacticCoreRuntimeService.BossAiInput input
    ) {
        public BossAiRequest {
            state = Objects.requireNonNull(state, "state");
            input = Objects.requireNonNull(input, "input");
        }
    }

    public record BossAiAdapterPlan(
            GalacticCoreRuntimeService.BossEncounterState nextState,
            String movementIntent,
            String attackIntent,
            List<String> requiredHostActions,
            boolean roomLocked,
            String status,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction eventAction
    ) {
        public BossAiAdapterPlan {
            nextState = Objects.requireNonNull(nextState, "nextState");
            movementIntent = requireText(movementIntent, "movementIntent");
            attackIntent = requireText(attackIntent, "attackIntent");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
            status = requireText(status, "status");
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            eventAction = Objects.requireNonNull(eventAction, "eventAction");
        }
    }

    public record TreasureInteractionRequest(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.TreasureInteraction interaction
    ) {
        public TreasureInteractionRequest {
            progression = Objects.requireNonNull(progression, "progression");
            interaction = Objects.requireNonNull(interaction, "interaction");
        }
    }

    public record TreasureInteractionPlan(
            boolean opened,
            boolean consumedKey,
            String status,
            GalacticCoreRuntimeService.PlayerProgression progression,
            List<String> loot,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction networkAction
    ) {
        public TreasureInteractionPlan {
            status = requireText(status, "status");
            progression = Objects.requireNonNull(progression, "progression");
            loot = List.copyOf(loot == null ? List.of() : loot);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            networkAction = Objects.requireNonNull(networkAction, "networkAction");
        }
    }

    public record TreasureChestScreenPlan(
            boolean openable,
            String status,
            String screenId,
            List<String> lootPreview,
            List<String> schematicRewards,
            List<String> unlockedRoutes,
            String saveDataTarget,
            GalacticCoreRuntimeGateway.RuntimeAction screenAction
    ) {
        public TreasureChestScreenPlan {
            status = requireText(status, "status");
            screenId = requireText(screenId, "screenId");
            lootPreview = List.copyOf(lootPreview == null ? List.of() : lootPreview);
            schematicRewards = List.copyOf(schematicRewards == null ? List.of() : schematicRewards);
            unlockedRoutes = List.copyOf(unlockedRoutes == null ? List.of() : unlockedRoutes);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
            screenAction = Objects.requireNonNull(screenAction, "screenAction");
        }
    }

    public record DungeonEncounterPlan(
            boolean rewardClaimed,
            String status,
            String dungeonId,
            String bossId,
            GalacticCoreRuntimeGateway.RuntimeAction rewardAction
    ) {
        public DungeonEncounterPlan {
            status = requireText(status, "status");
            dungeonId = requireText(dungeonId, "dungeonId");
            bossId = requireText(bossId, "bossId");
            rewardAction = Objects.requireNonNull(rewardAction, "rewardAction");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
