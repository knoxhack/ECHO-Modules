package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreHostBindingContracts {
    private final GalacticCoreHostExecutionBridge executionBridge;

    public GalacticCoreHostBindingContracts(GalacticCoreHostExecutionBridge executionBridge) {
        this.executionBridge = Objects.requireNonNull(executionBridge, "executionBridge");
    }

    public List<HostBindingContract> releaseHostBindingSmokeContracts() {
        List<GalacticCoreHostExecutionBridge.HostExecutionPlan> plans = executionPlans();
        return List.of(
                worldTransferBinding(plans.get(0)),
                bossEntitySpawnBinding(plans.get(1)),
                screenBinding(plans.get(2), "holomap_routes", "HoloMap route menu host binding"),
                screenBinding(plans.get(3), "screencore_launch_checklist", "ScreenCore launch checklist host binding"),
                screenBinding(plans.get(4), "treasure_chest", "Treasure chest menu host binding")
        );
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_concrete_host_bindings",
                "typedReceiptsOnly", true,
                "worldOwner", "echo.native.worldgen",
                "entityOwner", "echo.native.capabilities",
                "screenOwner", "echo.native.screens",
                "replaces", "WorldProvider live transfer hooks, EntityBoss live spawn hooks, and Gui* screen host wiring"
        );
    }

    private HostBindingContract worldTransferBinding(GalacticCoreHostExecutionBridge.HostExecutionPlan plan) {
        return new HostBindingContract(
                GalacticCoreIds.id("host_binding/world_dimension_transfer"),
                "echo.native.worldgen",
                "worldgen",
                "placeStructure",
                GalacticCoreIds.id("host_binding/world_dimension_transfer"),
                "dimension_transfer",
                plan.requiredHostActions(),
                plan.saveDataTarget(),
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_concrete_host_bindings"),
                        Map.entry("executionSource", plan.action().evidence().get("source")),
                        Map.entry("executionTarget", plan.action().target()),
                        Map.entry("hostSurface", "world"),
                        Map.entry("ownerService", "echo.native.worldgen"),
                        Map.entry("bindingKind", "dimension_transfer"),
                        Map.entry("operationId", plan.operationId()),
                        Map.entry("commitOrder", plan.action().evidence().get("commitOrder")),
                        Map.entry("requiredHostActions", plan.requiredHostActions()),
                        Map.entry("saveDataTarget", plan.saveDataTarget()),
                        Map.entry("typedReceiptsOnly", true),
                        Map.entry("replacement", "ASDK concrete world host binding for dimension transfer placement")
                )
        );
    }

    private HostBindingContract bossEntitySpawnBinding(GalacticCoreHostExecutionBridge.HostExecutionPlan plan) {
        return new HostBindingContract(
                GalacticCoreIds.id("host_binding/entity_boss_spawn"),
                "echo.native.capabilities",
                "capabilities",
                "registerIntegration",
                GalacticCoreIds.id("host_binding/entity_boss_spawn"),
                "boss_entity_spawn",
                plan.requiredHostActions(),
                plan.saveDataTarget(),
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_concrete_host_bindings"),
                        Map.entry("executionSource", plan.action().evidence().get("source")),
                        Map.entry("executionTarget", plan.action().target()),
                        Map.entry("hostSurface", "entity"),
                        Map.entry("ownerService", "echo.native.capabilities"),
                        Map.entry("bindingKind", "boss_entity_spawn"),
                        Map.entry("operationId", plan.operationId()),
                        Map.entry("commitOrder", plan.action().evidence().get("commitOrder")),
                        Map.entry("requiredHostActions", plan.requiredHostActions()),
                        Map.entry("saveDataTarget", plan.saveDataTarget()),
                        Map.entry("typedReceiptsOnly", true),
                        Map.entry("replacement", "ASDK concrete entity host binding for dungeon boss spawning")
                )
        );
    }

    private HostBindingContract screenBinding(
            GalacticCoreHostExecutionBridge.HostExecutionPlan plan,
            String screenId,
            String label
    ) {
        return new HostBindingContract(
                GalacticCoreIds.id("host_binding/screen_" + screenId),
                "echo.native.screens",
                "screens",
                "registerMenu",
                GalacticCoreIds.id("host_binding/screen_" + screenId),
                "screen_" + screenId,
                plan.requiredHostActions(),
                plan.saveDataTarget(),
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_concrete_host_bindings"),
                        Map.entry("executionSource", plan.action().evidence().get("source")),
                        Map.entry("executionTarget", plan.action().target()),
                        Map.entry("hostSurface", "screen"),
                        Map.entry("ownerService", "echo.native.screens"),
                        Map.entry("bindingKind", "screen_" + screenId),
                        Map.entry("screenId", screenId),
                        Map.entry("operationId", plan.operationId()),
                        Map.entry("requiredHostActions", plan.requiredHostActions()),
                        Map.entry("saveDataTarget", plan.saveDataTarget()),
                        Map.entry("typedReceiptsOnly", true),
                        Map.entry("replacement", "ASDK concrete screen host binding for " + label)
                )
        );
    }

    private List<GalacticCoreHostExecutionBridge.HostExecutionPlan> executionPlans() {
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
        GalacticCoreRuntimeService.PlayerProgression advanced = progression.withRoutes(List.of(GalacticCoreIds.id("route/mars")));
        GalacticCoreHostCallbacks.TransferPlacementCallback transfer = new GalacticCoreHostCallbacks.TransferPlacementCallback(
                "player/host-binding-smoke",
                progression,
                launch,
                GalacticCoreIds.id("route/moon"),
                gear,
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
        );
        GalacticCoreHostCallbacks.TreasureInteractionCallback treasure = new GalacticCoreHostCallbacks.TreasureInteractionCallback(
                "player/host-binding-smoke",
                advanced,
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
                executionBridge.executeDimensionTransfer(transfer),
                executionBridge.spawnBossEntity(new GalacticCoreHostCallbacks.BossEntitySpawnCallback(
                        "boss/host-binding-smoke",
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
                )),
                executionBridge.bindHoloMapRouteMenu(new GalacticCoreHostCallbacks.HoloMapRouteSurfaceCallback(
                        "player/host-binding-smoke",
                        progression,
                        GalacticCoreIds.id("route/mars")
                )),
                executionBridge.bindLaunchChecklistMenu(new GalacticCoreHostCallbacks.LaunchChecklistSurfaceCallback(
                        "player/host-binding-smoke",
                        progression,
                        launch,
                        GalacticCoreIds.id("route/moon"),
                        gear,
                        moon
                )),
                executionBridge.bindTreasureChestMenu(treasure)
        );
    }

    public record HostBindingContract(
            String bindingId,
            String serviceId,
            String surface,
            String action,
            String target,
            String bindingKind,
            List<String> requiredHostActions,
            String saveDataTarget,
            Map<String, Object> evidence
    ) {
        public HostBindingContract {
            bindingId = requireText(bindingId, "bindingId");
            serviceId = requireText(serviceId, "serviceId");
            surface = requireText(surface, "surface");
            action = requireText(action, "action");
            target = requireText(target, "target");
            bindingKind = requireText(bindingKind, "bindingKind");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
            saveDataTarget = requireText(saveDataTarget, "saveDataTarget");
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
