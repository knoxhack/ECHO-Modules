package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreRuntimeGateway {
    private final GalacticCoreRuntimeService runtime;

    public GalacticCoreRuntimeGateway(GalacticCoreRuntimeService runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    public RuntimeAction publishMachineTick(GalacticCoreRuntimeService.MachineSnapshot state, GalacticCoreRuntimeService.MachineInput input) {
        GalacticCoreRuntimeService.MachineSnapshot next = runtime.tickMachine(state, input);
        return new RuntimeAction(
                GalacticCoreIds.id("event/machine_tick"),
                "events",
                "publish",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "machine", state.type().id(),
                        "before", state.toString(),
                        "after", next.toString(),
                        "mutated", !state.equals(next)
                )
        );
    }

    public RuntimeAction publishLifeSupportTick(
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment
    ) {
        GalacticCoreRuntimeService.LifeSupportResult result = runtime.evaluateLifeSupport(gear, environment);
        return new RuntimeAction(
                GalacticCoreIds.id("event/player_tick_life_support"),
                "events",
                "publish",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "environment", environment.id(),
                        "canBreathe", result.canBreathe(),
                        "oxygenConsumed", result.oxygenConsumed(),
                        "thermalProtected", result.thermalProtected(),
                        "status", result.status()
                )
        );
    }

    public RuntimeAction openCelestialSelection(GalacticCoreRuntimeService.PlayerProgression progression, String selectedRoute) {
        GalacticCoreRuntimeService.RouteRequirement route = runtime.routeRequirement(selectedRoute, progression);
        return new RuntimeAction(
                GalacticCoreIds.id("screen/celestial_selection"),
                "screens",
                "open",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "route", route.routeId(),
                        "requiredVehicleTier", route.requiredVehicleTier(),
                        "unlocked", route.unlocked(),
                        "replacement", "HoloMap route surface"
                )
        );
    }

    public RuntimeAction sendRouteAction(GalacticCoreRuntimeService.RocketLaunchState launchState, GalacticCoreRuntimeService.RouteRequirement route) {
        GalacticCoreRuntimeService.RocketLaunchDecision decision = runtime.prepareLaunch(launchState, route);
        return new RuntimeAction(
                GalacticCoreIds.id("packet/celestial_route_action"),
                "network",
                "sendToPlayer",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "route", route.routeId(),
                        "ready", decision.ready(),
                        "reason", decision.reason(),
                        "countdownTicks", decision.countdownTicks()
                )
        );
    }

    public RuntimeAction openLaunchChecklist(GalacticCoreRuntimeService.RocketLaunchState launchState, GalacticCoreRuntimeService.RouteRequirement route) {
        GalacticCoreRuntimeService.RocketLaunchDecision decision = runtime.prepareLaunch(launchState, route);
        return new RuntimeAction(
                GalacticCoreIds.id("screen/launch_checklist"),
                "screens",
                "open",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "route", route.routeId(),
                        "ready", decision.ready(),
                        "reason", decision.reason()
                )
        );
    }

    public RuntimeAction openHoloMapRouteSurface(GalacticCoreRuntimeService.PlayerProgression progression, String selectedRoute) {
        GalacticCoreRuntimeService.CelestialRouteSurface surface = runtime.routeSurface(progression, selectedRoute);
        return new RuntimeAction(
                GalacticCoreIds.id("screen/holomap_routes"),
                "screens",
                "open",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "selectedRoute", surface.selectedRoute(),
                        "selectedEnvironment", surface.selectedEnvironment(),
                        "selectedUnlocked", surface.selectedUnlocked(),
                        "status", surface.status(),
                        "routeCount", surface.routes().size(),
                        "unlockedRoutes", surface.routes().stream().filter(GalacticCoreRuntimeService.RouteSurfaceEntry::unlocked).map(GalacticCoreRuntimeService.RouteSurfaceEntry::routeId).toList(),
                        "lockedRoutes", surface.routes().stream().filter(route -> !route.unlocked()).map(GalacticCoreRuntimeService.RouteSurfaceEntry::routeId).toList(),
                        "replacement", "HoloMap celestial route surface"
                )
        );
    }

    public RuntimeAction openScreenCoreLaunchChecklist(
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            GalacticCoreRuntimeService.RouteRequirement route,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment
    ) {
        GalacticCoreRuntimeService.LaunchChecklistSurface surface = runtime.launchChecklistSurface(launchState, route, gear, environment);
        return new RuntimeAction(
                GalacticCoreIds.id("screen/screencore_launch_checklist"),
                "screens",
                "open",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "route", surface.routeId(),
                        "environment", surface.environmentId(),
                        "ready", surface.ready(),
                        "status", surface.status(),
                        "checkCount", surface.checks().size(),
                        "passedChecks", surface.checks().stream().filter(GalacticCoreRuntimeService.ChecklistEntry::passed).map(GalacticCoreRuntimeService.ChecklistEntry::id).toList(),
                        "failedChecks", surface.checks().stream().filter(check -> !check.passed()).map(GalacticCoreRuntimeService.ChecklistEntry::id).toList(),
                        "replacement", "ScreenCore launch checklist surface"
                )
        );
    }

    public RuntimeAction openRenderedHoloMapRouteMenu(
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute
    ) {
        GalacticCoreRuntimeService.RenderedMenuLayout layout = runtime.renderHoloMapRouteMenu(progression, selectedRoute);
        return renderedMenuAction(layout, "HoloMap celestial route rendered menu");
    }

    public RuntimeAction openRenderedLaunchChecklistMenu(
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            GalacticCoreRuntimeService.RouteRequirement route,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment
    ) {
        GalacticCoreRuntimeService.RenderedMenuLayout layout = runtime.renderLaunchChecklistMenu(launchState, route, gear, environment);
        return renderedMenuAction(layout, "ScreenCore launch checklist rendered menu");
    }

    public RuntimeAction openRenderedTreasureChestMenu(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.TreasureInteraction interaction
    ) {
        GalacticCoreRuntimeService.RenderedMenuLayout layout = runtime.renderTreasureChestMenu(progression, interaction);
        return renderedMenuAction(layout, "ScreenCore treasure chest rendered menu");
    }

    public RuntimeAction publishHoloMapRouteInteraction(
            GalacticCoreRuntimeService.PlayerProgression progression,
            String selectedRoute,
            String actionId
    ) {
        GalacticCoreRuntimeService.ScreenInteractionResult result = runtime.interactHoloMapRoute(
                progression,
                selectedRoute,
                actionId
        );
        return new RuntimeAction(
                GalacticCoreIds.id("packet/holomap_route_interaction"),
                "network",
                "sendToPlayer",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("screenId", result.screenId()),
                        Map.entry("actionId", result.actionId()),
                        Map.entry("route", result.routeId()),
                        Map.entry("environment", result.environmentId()),
                        Map.entry("accepted", result.accepted()),
                        Map.entry("status", result.status()),
                        Map.entry("requiredHostActions", result.requiredHostActions()),
                        Map.entry("failedChecks", result.failedChecks()),
                        Map.entry("replacement", "HoloMap route widget interaction")
                )
        );
    }

    public RuntimeAction publishScreenCoreLaunchChecklistInteraction(
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            GalacticCoreRuntimeService.RouteRequirement route,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.EnvironmentState environment,
            String actionId
    ) {
        GalacticCoreRuntimeService.ScreenInteractionResult result = runtime.interactLaunchChecklist(
                launchState,
                route,
                gear,
                environment,
                actionId
        );
        return new RuntimeAction(
                GalacticCoreIds.id("packet/screencore_launch_checklist_interaction"),
                "network",
                "sendToPlayer",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("screenId", result.screenId()),
                        Map.entry("actionId", result.actionId()),
                        Map.entry("route", result.routeId()),
                        Map.entry("environment", result.environmentId()),
                        Map.entry("accepted", result.accepted()),
                        Map.entry("status", result.status()),
                        Map.entry("requiredHostActions", result.requiredHostActions()),
                        Map.entry("failedChecks", result.failedChecks()),
                        Map.entry("replacement", "ScreenCore launch checklist widget interaction")
                )
        );
    }

    public RuntimeAction placeTransferLandingZone(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String destinationRoute,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.TransferAnchor anchor
    ) {
        GalacticCoreRuntimeService.TransferPlacementPlan plan = runtime.planTransferPlacement(
                progression,
                launchState,
                destinationRoute,
                gear,
                anchor
        );
        GalacticCoreRuntimeService.LandingPlacement placement = plan.placement();
        return new RuntimeAction(
                GalacticCoreIds.id("worldgen/transfer_placement/" + placement.environmentId().substring(placement.environmentId().indexOf(':') + 1)),
                "worldgen",
                "placeStructure",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("route", plan.routeId()),
                        Map.entry("environment", plan.environmentId()),
                        Map.entry("ready", plan.ready()),
                        Map.entry("status", plan.status()),
                        Map.entry("placementId", placement.placementId()),
                        Map.entry("x", placement.x()),
                        Map.entry("y", placement.y()),
                        Map.entry("z", placement.z()),
                        Map.entry("entryMode", placement.entryMode()),
                        Map.entry("parachuteRequired", placement.parachuteRequired()),
                        Map.entry("landingPadReady", placement.landingPadReady()),
                        Map.entry("safe", placement.safe()),
                        Map.entry("requiredHostActions", plan.requiredHostActions()),
                        Map.entry("replacement", "WorldProvider transfer placement and landing zone preparation")
                )
        );
    }

    public RuntimeAction publishTransferExecution(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.RocketLaunchState launchState,
            String destinationRoute,
            GalacticCoreRuntimeService.PlayerGearState gear,
            GalacticCoreRuntimeService.TransferAnchor anchor
    ) {
        GalacticCoreRuntimeService.TransferExecutionPlan plan = runtime.executeTransfer(
                progression,
                launchState,
                destinationRoute,
                gear,
                anchor
        );
        return new RuntimeAction(
                GalacticCoreIds.id("event/dimension_transfer_execute"),
                "events",
                "publish",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("route", plan.routeId()),
                        Map.entry("environment", plan.environmentId()),
                        Map.entry("ready", plan.ready()),
                        Map.entry("status", plan.status()),
                        Map.entry("x", plan.x()),
                        Map.entry("y", plan.y()),
                        Map.entry("z", plan.z()),
                        Map.entry("entryMode", plan.entryMode()),
                        Map.entry("gravity", plan.gravity().name()),
                        Map.entry("requiredHostActions", plan.requiredHostActions()),
                        Map.entry("replacement", "WorldProvider transferPlayerToDimension execution")
                )
        );
    }

    public RuntimeAction claimDungeonReward(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.DungeonRewardClaim claim
    ) {
        GalacticCoreRuntimeService.DungeonRewardResult result = runtime.claimDungeonReward(progression, claim);
        return new RuntimeAction(
                GalacticCoreIds.id("packet/dungeon_reward_claim"),
                "network",
                "broadcast",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "dungeon", claim.dungeonId(),
                        "boss", claim.bossId(),
                        "claimed", result.claimed(),
                        "status", result.status(),
                        "unlockedRoutes", result.unlockedRoutes(),
                        "schematicRewards", result.schematicRewards(),
                        "loot", result.loot()
                )
        );
    }

    public RuntimeAction publishDungeonStructurePlan(String dungeonId) {
        GalacticCoreRuntimeService.DungeonStructurePlan plan = runtime.planDungeonStructure(dungeonId);
        return new RuntimeAction(
                GalacticCoreIds.id("worldgen/dungeon_structure/" + plan.body()),
                "worldgen",
                "registerFeature",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("dungeon", plan.dungeonId()),
                        Map.entry("body", plan.body()),
                        Map.entry("tier", plan.tier()),
                        Map.entry("legacySource", plan.legacySource()),
                        Map.entry("roomCount", plan.rooms().size()),
                        Map.entry("boss", plan.bossId()),
                        Map.entry("bossRoom", plan.bossRoomId()),
                        Map.entry("key", plan.keyId()),
                        Map.entry("treasureRoom", plan.treasureRoomId()),
                        Map.entry("loot", plan.lootId()),
                        Map.entry("schematicRewards", plan.schematicRewards()),
                        Map.entry("unlockedRoutes", plan.unlockedRoutes())
                )
        );
    }

    public RuntimeAction publishBossEntitySpawn(String dungeonId) {
        GalacticCoreRuntimeService.BossEntitySpawnPlan plan = runtime.planBossEntitySpawn(dungeonId);
        return new RuntimeAction(
                GalacticCoreIds.id("event/boss_entity_spawn"),
                "events",
                "publish",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("dungeon", plan.dungeonId()),
                        Map.entry("boss", plan.bossId()),
                        Map.entry("bossRoom", plan.bossRoomId()),
                        Map.entry("body", plan.body()),
                        Map.entry("ready", plan.ready()),
                        Map.entry("status", plan.status()),
                        Map.entry("legacyEntitySource", plan.legacyEntitySource()),
                        Map.entry("maxHealth", plan.maxHealth()),
                        Map.entry("initialPhase", plan.initialPhase().name()),
                        Map.entry("x", plan.x()),
                        Map.entry("y", plan.y()),
                        Map.entry("z", plan.z()),
                        Map.entry("attributes", plan.attributes()),
                        Map.entry("requiredHostActions", plan.requiredHostActions()),
                        Map.entry("replacement", "EntityBoss construction and spawn hook")
                )
        );
    }

    public RuntimeAction publishBossEncounterTick(
            GalacticCoreRuntimeService.BossEncounterState state,
            GalacticCoreRuntimeService.BossEncounterInput input
    ) {
        GalacticCoreRuntimeService.BossEncounterResult result = runtime.tickBossEncounter(state, input);
        return new RuntimeAction(
                GalacticCoreIds.id("event/boss_encounter_tick"),
                "events",
                "publish",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "dungeon", state.dungeonId(),
                        "boss", state.bossId(),
                        "beforeHealth", state.health(),
                        "afterHealth", result.state().health(),
                        "phase", result.state().phase().name(),
                        "defeated", result.defeated(),
                        "droppedKeys", result.droppedKeys(),
                        "status", result.status()
                )
        );
    }

    public RuntimeAction publishBossAiStep(
            GalacticCoreRuntimeService.BossEncounterState state,
            GalacticCoreRuntimeService.BossAiInput input
    ) {
        GalacticCoreRuntimeService.BossAiStep step = runtime.planBossAiStep(state, input);
        return new RuntimeAction(
                GalacticCoreIds.id("event/boss_ai_step"),
                "events",
                "publish",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("dungeon", state.dungeonId()),
                        Map.entry("boss", state.bossId()),
                        Map.entry("bossRoom", step.bossRoomId()),
                        Map.entry("movementIntent", step.movementIntent()),
                        Map.entry("attackIntent", step.attackIntent()),
                        Map.entry("phase", step.nextState().phase().name()),
                        Map.entry("health", step.nextState().health()),
                        Map.entry("roomLocked", step.roomLocked()),
                        Map.entry("requiredHostActions", step.requiredHostActions()),
                        Map.entry("status", step.status()),
                        Map.entry("replacement", "EntityBoss AI tick and dungeon room lock hooks")
                )
        );
    }

    public RuntimeAction publishTreasureInteraction(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.TreasureInteraction interaction
    ) {
        GalacticCoreRuntimeService.TreasureInteractionResult result = runtime.openDungeonTreasure(progression, interaction);
        return new RuntimeAction(
                GalacticCoreIds.id("packet/dungeon_treasure_interaction"),
                "network",
                "broadcast",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "dungeon", interaction.dungeonId(),
                        "boss", interaction.bossId(),
                        "opened", result.opened(),
                        "consumedKey", result.consumedKey(),
                        "status", result.status(),
                        "unlockedRoutes", result.unlockedRoutes(),
                        "schematicRewards", result.schematicRewards(),
                        "loot", result.loot()
                )
        );
    }

    public RuntimeAction openTreasureChestScreen(
            GalacticCoreRuntimeService.PlayerProgression progression,
            GalacticCoreRuntimeService.TreasureInteraction interaction
    ) {
        GalacticCoreRuntimeService.TreasureChestSurface surface = runtime.treasureChestSurface(progression, interaction);
        return new RuntimeAction(
                GalacticCoreIds.id("screen/treasure_chest"),
                "screens",
                "open",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("screenId", surface.screenId()),
                        Map.entry("dungeon", surface.dungeonId()),
                        Map.entry("treasureRoom", surface.treasureRoomId()),
                        Map.entry("locked", surface.locked()),
                        Map.entry("openable", surface.openable()),
                        Map.entry("opened", surface.opened()),
                        Map.entry("consumedKey", surface.consumedKey()),
                        Map.entry("status", surface.status()),
                        Map.entry("requiredKey", surface.requiredKey()),
                        Map.entry("lootPreview", surface.lootPreview()),
                        Map.entry("schematicRewards", surface.schematicRewards()),
                        Map.entry("unlockedRoutes", surface.unlockedRoutes()),
                        Map.entry("actions", surface.actions()),
                        Map.entry("replacement", "ScreenCore treasure chest reward surface")
                )
        );
    }

    public RuntimeAction scanEnvironment(String environmentId) {
        GalacticCoreRuntimeService.EnvironmentScan scan = runtime.scanEnvironment(environmentId);
        return new RuntimeAction(
                GalacticCoreIds.id("event/environment_scan"),
                "events",
                "publish",
                Map.of(
                        "source", "galacticraft_legacy_runtime_gateway",
                        "environment", scan.id(),
                        "route", scan.routeId(),
                        "atmosphere", scan.atmosphere().name(),
                        "thermalRisk", scan.thermalRisk().name(),
                        "gravity", scan.gravity().name(),
                        "oxygenRequired", scan.oxygenRequired(),
                        "thermalProtectionRequired", scan.thermalProtectionRequired(),
                        "acidHazard", scan.acidHazard(),
                        "dungeon", scan.dungeonId()
                )
        );
    }

    public List<RuntimeAction> releaseSmokeActions() {
        GalacticCoreRuntimeService.PlayerProgression progression = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.RouteRequirement moon = runtime.routeRequirement(GalacticCoreIds.id("route/moon"), progression);
        GalacticCoreRuntimeService.RocketLaunchState launch = new GalacticCoreRuntimeService.RocketLaunchState(
                1000,
                1000,
                true,
                true,
                true,
                1,
                200
        );
        return List.of(
                publishMachineTick(
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
                ),
                publishLifeSupportTick(
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(
                                GalacticCoreIds.id("moon"),
                                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                                GalacticCoreRuntimeService.ThermalRisk.COLD
                        )
                ),
                openCelestialSelection(progression, GalacticCoreIds.id("route/moon")),
                openHoloMapRouteSurface(progression, GalacticCoreIds.id("route/mars")),
                publishHoloMapRouteInteraction(progression, GalacticCoreIds.id("route/moon"), "select_route"),
                sendRouteAction(launch, moon),
                openLaunchChecklist(launch, moon),
                openScreenCoreLaunchChecklist(
                        launch,
                        moon,
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(
                                GalacticCoreIds.id("moon"),
                                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                                GalacticCoreRuntimeService.ThermalRisk.COLD
                        )
                ),
                openRenderedHoloMapRouteMenu(progression, GalacticCoreIds.id("route/mars")),
                openRenderedLaunchChecklistMenu(
                        launch,
                        moon,
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(
                                GalacticCoreIds.id("moon"),
                                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                                GalacticCoreRuntimeService.ThermalRisk.COLD
                        )
                ),
                publishScreenCoreLaunchChecklistInteraction(
                        launch,
                        moon,
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(
                                GalacticCoreIds.id("moon"),
                                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                                GalacticCoreRuntimeService.ThermalRisk.COLD
                        ),
                        "start_countdown"
                ),
                placeTransferLandingZone(
                        progression,
                        launch,
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
                ),
                publishTransferExecution(
                        progression,
                        launch,
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
                ),
                scanEnvironment(GalacticCoreIds.id("venus")),
                publishDungeonStructurePlan(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                publishBossEntitySpawn(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                publishBossEncounterTick(
                        runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                        new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
                ),
                publishBossAiStep(
                        runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                        new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
                ),
                publishTreasureInteraction(
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
                ),
                openTreasureChestScreen(
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
                ),
                openRenderedTreasureChestMenu(
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
                ),
                claimDungeonReward(
                        progression,
                        new GalacticCoreRuntimeService.DungeonRewardClaim(
                                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                                GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                                "tier_1_key",
                                true,
                                true
                        )
                )
        );
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_runtime_gateway",
                "typedReceiptsOnly", true,
                "actions", "machine_tick, life_support_tick, route_action, holomap_routes, holomap_route_rendered_menu, holomap_route_interaction, launch_checklist, screencore_launch_checklist, screencore_launch_checklist_rendered_menu, screencore_launch_checklist_interaction, treasure_chest_screen, treasure_chest_rendered_menu, transfer_placement, transfer_execution, environment_scan, dungeon_structure_plan, boss_entity_spawn, boss_encounter_tick, boss_ai_step, dungeon_treasure_interaction, dungeon_reward_claim",
                "replaces", "PacketSimple callbacks, GuiHandler actions, GuiCelestialSelection, GuiPreLaunchChecklist, GuiTreasureChest, HoloMap widgets, ScreenCore checklist controls and rendered menus, WorldProvider transfer placement/execution, EntityBoss construction and AI hooks, Forge event bus runtime listeners"
        );
    }

    private static RuntimeAction renderedMenuAction(GalacticCoreRuntimeService.RenderedMenuLayout layout, String replacement) {
        String path = layout.screenId().substring(layout.screenId().lastIndexOf('/') + 1);
        return new RuntimeAction(
                GalacticCoreIds.id("screen_layout/" + path),
                "screens",
                "open",
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_runtime_gateway"),
                        Map.entry("screenId", layout.screenId()),
                        Map.entry("rendererId", layout.rendererId()),
                        Map.entry("title", layout.title()),
                        Map.entry("status", layout.status()),
                        Map.entry("regionCount", layout.regions().size()),
                        Map.entry("widgetCount", layout.widgets().size()),
                        Map.entry("actions", layout.actions()),
                        Map.entry("widgets", layout.widgets().stream().map(GalacticCoreRuntimeService.MenuWidget::id).toList()),
                        Map.entry("replacement", replacement)
                )
        );
    }

    public record RuntimeAction(String target, String surface, String action, Map<String, Object> evidence) {
        public RuntimeAction {
            if (target == null || target.isBlank()) {
                throw new IllegalArgumentException("target must not be blank");
            }
            if (surface == null || surface.isBlank()) {
                throw new IllegalArgumentException("surface must not be blank");
            }
            if (action == null || action.isBlank()) {
                throw new IllegalArgumentException("action must not be blank");
            }
            evidence = Map.copyOf(evidence == null ? Map.of() : evidence);
        }
    }
}
