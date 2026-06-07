package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Atmosphere.VACUUM;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.MachineType.OXYGEN_COLLECTOR;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.ThermalRisk.COLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreHostCallbacksTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters adapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks host = new GalacticCoreHostCallbacks(runtime, gateway, adapters);

    @Test
    void machineTickCallbackDispatchesAdapterActionWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onMachineBlockEntityTick(
                new GalacticCoreHostCallbacks.MachineTickCallback(
                        GalacticCoreIds.id("block_entity/oxygen_collector"),
                        "oxygen_collector",
                        new GalacticCoreRuntimeService.MachineSnapshot(OXYGEN_COLLECTOR, 100, 0, 0, 0, 0, true, false, false, ""),
                        new GalacticCoreRuntimeService.MachineInput(8, 0, 0, false, false)
                )
        );

        assertTrue(result.accepted());
        assertEquals("machine_tick_dispatched", result.status());
        assertEquals(GalacticCoreIds.id("event/machine_tick"), result.actions().getFirst().target());
        assertEquals("galacticraft_legacy_host_callbacks", result.actions().getFirst().evidence().get("source"));
        assertEquals("galacticraft_legacy_runtime_gateway", result.actions().getFirst().evidence().get("gatewaySource"));
        assertTrue(result.evidence().get("nextState").toString().contains("oxygen=16"));
    }

    @Test
    void lifeSupportCallbackReturnsBreathabilityStatus() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onPlayerLifeSupportTick(
                new GalacticCoreHostCallbacks.LifeSupportTickCallback(
                        "player/test",
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
                )
        );

        assertTrue(result.accepted());
        assertEquals("oxygen_consumed", result.status());
        assertEquals("player_life_support_tick", result.actions().getFirst().evidence().get("hostCallback"));
    }

    @Test
    void routeSelectionCallbackKeepsLockedRoutesBlocked() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onCelestialRouteSelection(
                new GalacticCoreHostCallbacks.RouteSelectionCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("route/mars")
                )
        );

        assertFalse(result.accepted());
        assertEquals("route_locked", result.status());
        assertEquals(false, result.actions().getFirst().evidence().get("unlocked"));
    }

    @Test
    void holoMapRouteSurfaceCallbackOpensRouteSurfaceWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onHoloMapRouteSurfaceOpen(
                new GalacticCoreHostCallbacks.HoloMapRouteSurfaceCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("route/mars")
                )
        );

        assertFalse(result.accepted());
        assertEquals("route_locked", result.status());
        assertEquals(GalacticCoreIds.id("screen/holomap_routes"), result.actions().getFirst().target());
        assertEquals("holomap_route_surface_open", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("screen_state/holomap_routes"), result.evidence().get("saveDataTarget"));
    }

    @Test
    void screenCoreLaunchChecklistCallbackOpensChecklistWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onScreenCoreLaunchChecklistOpen(
                new GalacticCoreHostCallbacks.LaunchChecklistSurfaceCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
                )
        );

        assertTrue(result.accepted());
        assertEquals("launch_ready", result.status());
        assertEquals(GalacticCoreIds.id("screen/screencore_launch_checklist"), result.actions().getFirst().target());
        assertEquals("screencore_launch_checklist_open", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("screen_state/screencore_launch_checklist"), result.evidence().get("saveDataTarget"));
    }

    @Test
    void renderedMenuCallbacksOpenLayoutScreensWithHostEvidence() {
        GalacticCoreRuntimeService.PlayerProgression progression = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreHostCallbacks.HostCallbackResult routes = host.onHoloMapRouteMenuRender(
                new GalacticCoreHostCallbacks.HoloMapRouteSurfaceCallback(
                        "player/test",
                        progression,
                        GalacticCoreIds.id("route/mars")
                )
        );
        GalacticCoreHostCallbacks.HostCallbackResult checklist = host.onScreenCoreLaunchChecklistMenuRender(
                new GalacticCoreHostCallbacks.LaunchChecklistSurfaceCallback(
                        "player/test",
                        progression,
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
                )
        );

        assertTrue(routes.accepted());
        assertEquals(GalacticCoreIds.id("screen_layout/holomap_routes"), routes.actions().getFirst().target());
        assertEquals("holomap_route_menu_render", routes.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("renderer/holomap_route_menu"), routes.evidence().get("rendererId"));
        assertEquals(GalacticCoreIds.id("screen_layout_state/holomap_routes"), routes.evidence().get("saveDataTarget"));

        assertTrue(checklist.accepted());
        assertEquals(GalacticCoreIds.id("screen_layout/screencore_launch_checklist"), checklist.actions().getFirst().target());
        assertEquals("screencore_launch_checklist_menu_render", checklist.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("renderer/screencore_launch_checklist_menu"), checklist.evidence().get("rendererId"));
    }

    @Test
    void holoMapRouteInteractionCallbackSendsWidgetPacketWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onHoloMapRouteInteraction(
                new GalacticCoreHostCallbacks.HoloMapRouteInteractionCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("route/moon"),
                        "select_route"
                )
        );

        assertTrue(result.accepted());
        assertEquals("route_selected", result.status());
        assertEquals(GalacticCoreIds.id("packet/holomap_route_interaction"), result.actions().getFirst().target());
        assertEquals("holomap_route_interaction", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("screen_state/holomap_routes/interaction"), result.evidence().get("saveDataTarget"));
    }

    @Test
    void screenCoreLaunchChecklistInteractionCallbackSendsCountdownIntent() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onScreenCoreLaunchChecklistInteraction(
                new GalacticCoreHostCallbacks.LaunchChecklistInteractionCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD),
                        "start_countdown"
                )
        );

        assertTrue(result.accepted());
        assertEquals("countdown_armed", result.status());
        assertEquals(GalacticCoreIds.id("packet/screencore_launch_checklist_interaction"), result.actions().getFirst().target());
        assertEquals("screencore_launch_checklist_interaction", result.actions().getFirst().evidence().get("hostCallback"));
        assertTrue(result.actions().getFirst().evidence().get("requiredHostActions").toString().contains("arm_countdown"));
    }

    @Test
    void dimensionTransferCallbackCombinesChecklistRouteAndEnvironmentActions() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onDimensionTransferRequest(
                new GalacticCoreHostCallbacks.DimensionTransferCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        GalacticCoreIds.id("moon")
                )
        );

        assertTrue(result.accepted());
        assertEquals("transfer_ready", result.status());
        assertEquals(4, result.actions().size());
        assertTrue(result.actions().stream().anyMatch(action -> GalacticCoreIds.id("worldgen/transfer_placement/moon").equals(action.target())));
        assertTrue(result.actions().stream().allMatch(action -> "galacticraft_legacy_host_callbacks".equals(action.evidence().get("source"))));
    }

    @Test
    void transferPlacementCallbackPlacesLandingZoneWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onTransferPlacementPrepare(
                new GalacticCoreHostCallbacks.TransferPlacementCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
                )
        );

        assertTrue(result.accepted());
        assertEquals("transfer_placement_ready", result.status());
        assertEquals(GalacticCoreIds.id("worldgen/transfer_placement/moon"), result.actions().getFirst().target());
        assertEquals("placeStructure", result.actions().getFirst().action());
        assertEquals("transfer_placement_prepare", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("transfer_state/moon"), result.evidence().get("saveDataTarget"));
    }

    @Test
    void dimensionTransferExecuteCallbackPublishesHostTeleportIntent() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onDimensionTransferExecute(
                new GalacticCoreHostCallbacks.TransferPlacementCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
                )
        );

        assertTrue(result.accepted());
        assertEquals("transfer_execution_ready", result.status());
        assertEquals(GalacticCoreIds.id("event/dimension_transfer_execute"), result.actions().getFirst().target());
        assertEquals("dimension_transfer_execute", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("transfer_execution_state/moon"), result.evidence().get("saveDataTarget"));
        assertTrue(result.actions().getFirst().evidence().get("requiredHostActions").toString().contains("ticket_destination_chunk"));
    }

    @Test
    void dungeonTreasureClaimCallbackBroadcastsRewardClaim() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onDungeonTreasureClaim(
                new GalacticCoreHostCallbacks.DungeonTreasureClaimCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        true,
                        true
                )
        );

        assertTrue(result.accepted());
        assertEquals("reward_claimed", result.status());
        assertEquals(GalacticCoreIds.id("packet/dungeon_reward_claim"), result.actions().getFirst().target());
        assertEquals("dungeon_treasure_claim", result.actions().getFirst().evidence().get("hostCallback"));
    }

    @Test
    void dungeonStructureCallbackRegistersWorldgenPlanWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onDungeonStructurePrepare(
                new GalacticCoreHostCallbacks.DungeonStructureCallback(
                        "worldgen/test",
                        GalacticCoreIds.id("dungeon/venus_dungeon_tier_3")
                )
        );

        assertTrue(result.accepted());
        assertEquals("structure_ready", result.status());
        assertEquals(GalacticCoreIds.id("worldgen/dungeon_structure_host/venus"), result.actions().getFirst().target());
        assertEquals("worldgen", result.actions().getFirst().surface());
        assertEquals("dungeon_structure_prepare", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("worldgen/dungeon_structure/venus"), result.actions().getFirst().evidence().get("gatewayTarget"));
        assertEquals(GalacticCoreIds.id("dungeon_room/venus/treasure"), result.evidence().get("treasureRoomId"));
    }

    @Test
    void bossEntitySpawnCallbackPublishesHostSpawnIntent() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onBossEntitySpawn(
                new GalacticCoreHostCallbacks.BossEntitySpawnCallback(
                        "boss/test",
                        GalacticCoreIds.id("dungeon/mars_dungeon_tier_2")
                )
        );

        assertTrue(result.accepted());
        assertEquals("boss_spawn_ready", result.status());
        assertEquals(GalacticCoreIds.id("event/boss_entity_spawn"), result.actions().getFirst().target());
        assertEquals("boss_entity_spawn", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("boss/evolved_creeper_boss"), result.evidence().get("bossId"));
        assertEquals(GalacticCoreIds.id("dungeon_room/mars/boss"), result.evidence().get("bossRoomId"));
        assertTrue(result.evidence().get("legacyEntitySource").toString().contains("EntityCreeperBoss"));
        assertEquals(GalacticCoreIds.id("boss_entity_state/mars"), result.evidence().get("saveDataTarget"));
        assertTrue(result.evidence().get("requiredHostActions").toString().contains("instantiate_boss_entity"));
    }

    @Test
    void bossEncounterCallbackPublishesDefeatWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onBossEncounterTick(
                new GalacticCoreHostCallbacks.BossEncounterCallback(
                        "boss/test",
                        runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                        new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
                )
        );

        assertTrue(result.accepted());
        assertEquals("boss_defeated", result.status());
        assertEquals(GalacticCoreIds.id("event/boss_encounter_tick"), result.actions().getFirst().target());
        assertEquals("boss_encounter_tick", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(true, result.evidence().get("defeated"));
        assertTrue(result.evidence().get("droppedKeys").toString().contains("tier_1_key"));
    }

    @Test
    void bossAiCallbackPublishesEntityIntentWithHostEvidence() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onBossAiStep(
                new GalacticCoreHostCallbacks.BossAiCallback(
                        "boss/test",
                        runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                        new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
                )
        );

        assertTrue(result.accepted());
        assertEquals("boss_engaged", result.status());
        assertEquals(GalacticCoreIds.id("event/boss_ai_step"), result.actions().getFirst().target());
        assertEquals("boss_ai_step", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals("bone_slam", result.evidence().get("attackIntent"));
        assertEquals("hold_attack_range", result.evidence().get("movementIntent"));
        assertEquals(GalacticCoreIds.id("boss_ai_state/moon"), result.evidence().get("saveDataTarget"));
        assertTrue(result.evidence().get("requiredHostActions").toString().contains("execute_bone_slam"));
    }

    @Test
    void treasureInteractionCallbackOpensTreasureWithoutDirectInventoryMutation() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onTreasureInteraction(
                new GalacticCoreHostCallbacks.TreasureInteractionCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
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

        assertTrue(result.accepted());
        assertEquals("reward_claimed", result.status());
        assertEquals(GalacticCoreIds.id("packet/dungeon_treasure_interaction"), result.actions().getFirst().target());
        assertEquals("dungeon_treasure_interaction", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(true, result.evidence().get("consumedKey"));
        assertTrue(result.evidence().get("loot").toString().contains("moon_dungeon_tier_1"));
    }

    @Test
    void treasureChestScreenCallbackOpensScreenWithRewardPreview() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onTreasureChestScreenOpen(
                new GalacticCoreHostCallbacks.TreasureInteractionCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
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

        assertTrue(result.accepted());
        assertEquals("reward_claimed", result.status());
        assertEquals(GalacticCoreIds.id("screen/treasure_chest"), result.actions().getFirst().target());
        assertEquals("treasure_chest_screen_open", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("screen_state/treasure_chest/moon"), result.evidence().get("saveDataTarget"));
        assertTrue(result.evidence().get("lootPreview").toString().contains("moon_dungeon_tier_1"));
    }

    @Test
    void treasureChestMenuCallbackOpensRenderedLayout() {
        GalacticCoreHostCallbacks.HostCallbackResult result = host.onTreasureChestMenuRender(
                new GalacticCoreHostCallbacks.TreasureInteractionCallback(
                        "player/test",
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
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

        assertTrue(result.accepted());
        assertEquals("reward_claimed", result.status());
        assertEquals(GalacticCoreIds.id("screen_layout/treasure_chest"), result.actions().getFirst().target());
        assertEquals("treasure_chest_menu_render", result.actions().getFirst().evidence().get("hostCallback"));
        assertEquals(GalacticCoreIds.id("renderer/treasure_chest_menu"), result.evidence().get("rendererId"));
        assertEquals(GalacticCoreIds.id("screen_layout_state/treasure_chest"), result.evidence().get("saveDataTarget"));
    }

    @Test
    void releaseHostCallbackSmokeActionsCoverAllCallbackPaths() {
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = host.releaseHostCallbackSmokeActions();

        assertEquals(24, actions.size());
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/machine_tick").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/player_tick_life_support").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/celestial_selection").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/holomap_route_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/screencore_launch_checklist_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/celestial_route_action").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("worldgen/transfer_placement/moon").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/dimension_transfer_execute").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/environment_scan").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("worldgen/dungeon_structure_host/moon").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_entity_spawn").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_encounter_tick").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_ai_step").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/dungeon_treasure_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/dungeon_reward_claim").equals(action.target())));
        assertTrue(actions.stream().allMatch(action -> "galacticraft_legacy_host_callbacks".equals(action.evidence().get("source"))));
    }
}
