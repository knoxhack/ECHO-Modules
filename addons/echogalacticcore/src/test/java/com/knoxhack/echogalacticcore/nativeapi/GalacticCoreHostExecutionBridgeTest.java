package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Atmosphere.VACUUM;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.ThermalRisk.COLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreHostExecutionBridgeTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters adapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks host = new GalacticCoreHostCallbacks(runtime, gateway, adapters);
    private final GalacticCoreHostExecutionBridge bridge = new GalacticCoreHostExecutionBridge(host);

    @Test
    void transferExecutionBridgePublishesCommitOrder() {
        GalacticCoreHostExecutionBridge.HostExecutionPlan plan = bridge.executeDimensionTransfer(
                new GalacticCoreHostCallbacks.TransferPlacementCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, true),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 0, 96, 0, true)
                )
        );

        assertTrue(plan.accepted());
        assertEquals(GalacticCoreIds.id("host_execution/dimension_transfer"), plan.operationId());
        assertEquals(GalacticCoreIds.id("event/host_execution_dimension_transfer"), plan.action().target());
        assertEquals("galacticraft_legacy_host_execution_bridge", plan.action().evidence().get("source"));
        assertTrue(plan.requiredHostActions().contains("ticket_destination_chunk"));
        assertTrue(plan.action().evidence().get("commitOrder").toString().contains("place_player"));
    }

    @Test
    void bossSpawnBridgePublishesEntityCommitOrder() {
        GalacticCoreHostExecutionBridge.HostExecutionPlan plan = bridge.spawnBossEntity(
                new GalacticCoreHostCallbacks.BossEntitySpawnCallback(
                        "boss/test",
                        GalacticCoreIds.id("dungeon/mars_dungeon_tier_2")
                )
        );

        assertTrue(plan.accepted());
        assertEquals(GalacticCoreIds.id("host_execution/boss_entity_spawn"), plan.operationId());
        assertEquals(GalacticCoreIds.id("event/host_execution_boss_spawn"), plan.action().target());
        assertTrue(plan.requiredHostActions().contains("instantiate_boss_entity"));
        assertTrue(plan.action().evidence().get("commitOrder").toString().contains("spawn_entity"));
        assertEquals(GalacticCoreIds.id("boss_entity_state/mars"), plan.saveDataTarget());
    }

    @Test
    void renderedMenuBridgeBindsScreenLayouts() {
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
        GalacticCoreRuntimeService.EnvironmentState environment = new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD);

        GalacticCoreHostExecutionBridge.HostExecutionPlan routes = bridge.bindHoloMapRouteMenu(
                new GalacticCoreHostCallbacks.HoloMapRouteSurfaceCallback("player/test", progression, GalacticCoreIds.id("route/mars"))
        );
        GalacticCoreHostExecutionBridge.HostExecutionPlan checklist = bridge.bindLaunchChecklistMenu(
                new GalacticCoreHostCallbacks.LaunchChecklistSurfaceCallback("player/test", progression, launch, GalacticCoreIds.id("route/moon"), gear, environment)
        );
        GalacticCoreHostExecutionBridge.HostExecutionPlan treasure = bridge.bindTreasureChestMenu(
                new GalacticCoreHostCallbacks.TreasureInteractionCallback(
                        "player/test",
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
                )
        );

        assertEquals(GalacticCoreIds.id("screen_layout/host_binding/holomap_routes"), routes.action().target());
        assertEquals(GalacticCoreIds.id("screen_layout/host_binding/screencore_launch_checklist"), checklist.action().target());
        assertEquals(GalacticCoreIds.id("screen_layout/host_binding/treasure_chest"), treasure.action().target());
        assertTrue(routes.requiredHostActions().contains("bind_screen_renderer"));
        assertTrue(checklist.action().evidence().get("rendererId").toString().contains("launch_checklist"));
        assertTrue(treasure.action().evidence().get("requiredHostActions").toString().contains("mount_widgets"));
    }

    @Test
    void releaseSmokeActionsCoverTransferBossAndMenus() {
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = bridge.releaseHostExecutionSmokeActions();

        assertEquals(5, actions.size());
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/host_execution_dimension_transfer").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/host_execution_boss_spawn").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/host_binding/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/host_binding/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/host_binding/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().allMatch(action -> "galacticraft_legacy_host_execution_bridge".equals(action.evidence().get("source"))));
    }
}
