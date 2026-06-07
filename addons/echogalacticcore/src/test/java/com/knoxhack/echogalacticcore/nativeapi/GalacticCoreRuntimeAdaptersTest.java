package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
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

class GalacticCoreRuntimeAdaptersTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters adapters = new GalacticCoreRuntimeAdapters(runtime, gateway);

    @Test
    void machineBlockEntityAdapterTicksAndProducesSaveTarget() {
        GalacticCoreRuntimeAdapters.MachineAdapterResult result = adapters.tickMachineBlockEntity(
                new GalacticCoreRuntimeAdapters.MachineBlockEntityAdapter(
                        GalacticCoreIds.id("block_entity/oxygen_collector"),
                        "oxygen_collector",
                        new GalacticCoreRuntimeService.MachineSnapshot(OXYGEN_COLLECTOR, 100, 0, 0, 0, 0, true, false, false, "")
                ),
                new GalacticCoreRuntimeService.MachineInput(8, 0, 0, false, false)
        );

        assertTrue(result.mutated());
        assertEquals(16, result.adapter().snapshot().oxygen());
        assertEquals(GalacticCoreIds.id("machine_state/oxygen_collector"), result.saveDataTarget());
        assertEquals(GalacticCoreIds.id("event/machine_tick"), result.tickAction().target());
    }

    @Test
    void dimensionTransferAdapterCombinesChecklistRouteAndEnvironmentActions() {
        GalacticCoreRuntimeAdapters.DimensionTransferPlan plan = adapters.prepareDimensionTransfer(
                new GalacticCoreRuntimeAdapters.DimensionTransferRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        GalacticCoreIds.id("moon")
                )
        );

        assertTrue(plan.ready());
        assertEquals("transfer_ready", plan.status());
        assertEquals(4, plan.actions().size());
        assertTrue(plan.actions().stream().anyMatch(action -> GalacticCoreIds.id("screen/launch_checklist").equals(action.target())));
        assertTrue(plan.actions().stream().anyMatch(action -> GalacticCoreIds.id("packet/celestial_route_action").equals(action.target())));
        assertTrue(plan.actions().stream().anyMatch(action -> GalacticCoreIds.id("event/environment_scan").equals(action.target())));
        assertTrue(plan.actions().stream().anyMatch(action -> GalacticCoreIds.id("worldgen/transfer_placement/moon").equals(action.target())
                && "placeStructure".equals(action.action())));
    }

    @Test
    void dimensionTransferAdapterBlocksLockedOrUnderTierDestinations() {
        GalacticCoreRuntimeAdapters.DimensionTransferPlan plan = adapters.prepareDimensionTransfer(
                new GalacticCoreRuntimeAdapters.DimensionTransferRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/mars"),
                        GalacticCoreIds.id("mars")
                )
        );

        assertFalse(plan.ready());
        assertEquals("vehicle_tier_required", plan.status());
    }

    @Test
    void transferPlacementAdapterCreatesWorldgenPlacementAndSaveTarget() {
        GalacticCoreRuntimeAdapters.TransferPlacementAdapterPlan plan = adapters.prepareTransferPlacement(
                new GalacticCoreRuntimeAdapters.TransferPlacementRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
                )
        );

        assertTrue(plan.ready());
        assertEquals("transfer_placement_ready", plan.status());
        assertEquals(GalacticCoreIds.id("transfer_placement/moon"), plan.placementId());
        assertEquals(GalacticCoreIds.id("transfer_state/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("worldgen/transfer_placement/moon"), plan.worldgenAction().target());
        assertEquals("placeStructure", plan.worldgenAction().action());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.worldgenAction().evidence().get("source"));
    }

    @Test
    void transferExecutionAdapterPublishesHostExecutionEventAndSaveTarget() {
        GalacticCoreRuntimeAdapters.TransferExecutionAdapterPlan plan = adapters.executeDimensionTransfer(
                new GalacticCoreRuntimeAdapters.TransferPlacementRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
                )
        );

        assertTrue(plan.ready());
        assertEquals("transfer_execution_ready", plan.status());
        assertEquals(GalacticCoreIds.id("moon"), plan.environmentId());
        assertEquals(GalacticCoreIds.id("transfer_execution_state/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("event/dimension_transfer_execute"), plan.eventAction().target());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.eventAction().evidence().get("source"));
        assertTrue(plan.eventAction().evidence().get("requiredHostActions").toString().contains("place_player_at_destination"));
    }

    @Test
    void holoMapRouteSurfaceAdapterOpensScreenAndTracksSaveTarget() {
        GalacticCoreRuntimeAdapters.ScreenSurfaceAdapterPlan plan = adapters.openHoloMapRouteSurface(
                new GalacticCoreRuntimeAdapters.RouteSurfaceRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("route/mars")
                )
        );

        assertFalse(plan.accepted());
        assertEquals("route_locked", plan.status());
        assertEquals(GalacticCoreIds.id("screen/holomap_routes"), plan.screenId());
        assertEquals(GalacticCoreIds.id("screen_state/holomap_routes"), plan.saveDataTarget());
        assertEquals("screens", plan.screenAction().surface());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.screenAction().evidence().get("source"));
        assertTrue(plan.screenAction().evidence().get("lockedRoutes").toString().contains("route/mars"));
    }

    @Test
    void screenCoreLaunchChecklistAdapterOpensScreenWithLifeSupportChecks() {
        GalacticCoreRuntimeAdapters.ScreenSurfaceAdapterPlan plan = adapters.openScreenCoreLaunchChecklist(
                new GalacticCoreRuntimeAdapters.LaunchChecklistSurfaceRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(
                                GalacticCoreIds.id("moon"),
                                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                                GalacticCoreRuntimeService.ThermalRisk.COLD
                        )
                )
        );

        assertTrue(plan.accepted());
        assertEquals("launch_ready", plan.status());
        assertEquals(GalacticCoreIds.id("screen/screencore_launch_checklist"), plan.screenId());
        assertEquals(GalacticCoreIds.id("screen_state/screencore_launch_checklist"), plan.saveDataTarget());
        assertEquals("screens", plan.screenAction().surface());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.screenAction().evidence().get("source"));
        assertTrue(plan.screenAction().evidence().get("passedChecks").toString().contains("environment_life_support"));
    }

    @Test
    void renderedMenuAdaptersOpenScreenLayoutsAndTrackSaveTargets() {
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
        GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan routes = adapters.openRenderedHoloMapRouteMenu(
                new GalacticCoreRuntimeAdapters.RouteSurfaceRequest(progression, GalacticCoreIds.id("route/mars"))
        );
        GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan checklist = adapters.openRenderedLaunchChecklistMenu(
                new GalacticCoreRuntimeAdapters.LaunchChecklistSurfaceRequest(
                        progression,
                        launch,
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
                )
        );

        assertTrue(routes.accepted());
        assertEquals(GalacticCoreIds.id("screen_layout_state/holomap_routes"), routes.saveDataTarget());
        assertEquals(GalacticCoreIds.id("renderer/holomap_route_menu"), routes.rendererId());
        assertEquals(GalacticCoreIds.id("screen_layout/holomap_routes"), routes.screenAction().target());
        assertEquals("galacticraft_legacy_runtime_adapters", routes.screenAction().evidence().get("source"));

        assertTrue(checklist.accepted());
        assertEquals(GalacticCoreIds.id("screen_layout_state/screencore_launch_checklist"), checklist.saveDataTarget());
        assertEquals(GalacticCoreIds.id("renderer/screencore_launch_checklist_menu"), checklist.rendererId());
        assertTrue(checklist.widgetCount() >= 9);
    }

    @Test
    void holoMapRouteInteractionAdapterSendsTypedWidgetPacket() {
        GalacticCoreRuntimeAdapters.ScreenInteractionAdapterPlan plan = adapters.interactHoloMapRoute(
                new GalacticCoreRuntimeAdapters.ScreenInteractionRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("route/moon"),
                        "select_route"
                )
        );

        assertTrue(plan.accepted());
        assertEquals("route_selected", plan.status());
        assertEquals(GalacticCoreIds.id("screen/holomap_routes"), plan.screenId());
        assertEquals("select_route", plan.actionId());
        assertEquals(GalacticCoreIds.id("screen_state/holomap_routes/interaction"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("packet/holomap_route_interaction"), plan.networkAction().target());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.networkAction().evidence().get("source"));
    }

    @Test
    void screenCoreChecklistInteractionAdapterSendsCountdownIntent() {
        GalacticCoreRuntimeAdapters.ScreenInteractionAdapterPlan plan = adapters.interactScreenCoreLaunchChecklist(
                new GalacticCoreRuntimeAdapters.LaunchChecklistInteractionRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                        GalacticCoreIds.id("route/moon"),
                        new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                        new GalacticCoreRuntimeService.EnvironmentState(
                                GalacticCoreIds.id("moon"),
                                GalacticCoreRuntimeService.Atmosphere.VACUUM,
                                GalacticCoreRuntimeService.ThermalRisk.COLD
                        ),
                        "start_countdown"
                )
        );

        assertTrue(plan.accepted());
        assertEquals("countdown_armed", plan.status());
        assertEquals(GalacticCoreIds.id("screen/screencore_launch_checklist"), plan.screenId());
        assertEquals(GalacticCoreIds.id("screen_state/screencore_launch_checklist/interaction"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("packet/screencore_launch_checklist_interaction"), plan.networkAction().target());
        assertTrue(plan.networkAction().evidence().get("requiredHostActions").toString().contains("arm_countdown"));
    }

    @Test
    void dungeonEncounterAdapterBroadcastsRewardClaim() {
        GalacticCoreRuntimeAdapters.DungeonEncounterPlan plan = adapters.resolveDungeonEncounter(
                new GalacticCoreRuntimeAdapters.DungeonEncounterRequest(
                        GalacticCoreRuntimeService.PlayerProgression.starting(),
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        true,
                        true
                )
        );

        assertTrue(plan.rewardClaimed());
        assertEquals("reward_claimed", plan.status());
        assertEquals(GalacticCoreIds.id("packet/dungeon_reward_claim"), plan.rewardAction().target());
        assertTrue(plan.rewardAction().evidence().get("unlockedRoutes").toString().contains("route/mars"));
    }

    @Test
    void dungeonStructureAdapterCreatesWorldgenAndSaveDataPlan() {
        GalacticCoreRuntimeAdapters.DungeonStructureAdapterPlan plan = adapters.prepareDungeonStructure(
                new GalacticCoreRuntimeAdapters.DungeonStructureRequest(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"))
        );

        assertTrue(plan.ready());
        assertEquals("structure_ready", plan.status());
        assertEquals(GalacticCoreIds.id("boss/evolved_skeleton_boss"), plan.bossId());
        assertEquals(GalacticCoreIds.id("dungeon_room/moon/boss"), plan.bossRoomId());
        assertEquals(GalacticCoreIds.id("dungeon_room/moon/treasure"), plan.treasureRoomId());
        assertEquals(GalacticCoreIds.id("dungeon_structure_state/moon"), plan.saveDataTarget());
        assertEquals("worldgen", plan.worldgenAction().surface());
        assertEquals(GalacticCoreIds.id("worldgen/dungeon_structure_adapter/moon"), plan.worldgenAction().target());
        assertEquals(GalacticCoreIds.id("worldgen/dungeon_structure/moon"), plan.worldgenAction().evidence().get("gatewayTarget"));
    }

    @Test
    void bossEntitySpawnAdapterPublishesEventAndTracksSaveTarget() {
        GalacticCoreRuntimeAdapters.BossEntitySpawnAdapterPlan plan = adapters.prepareBossEntitySpawn(
                new GalacticCoreRuntimeAdapters.BossEntitySpawnRequest(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"))
        );

        assertTrue(plan.ready());
        assertEquals("boss_spawn_ready", plan.status());
        assertEquals(GalacticCoreIds.id("boss/evolved_skeleton_boss"), plan.bossId());
        assertEquals(GalacticCoreIds.id("dungeon_room/moon/boss"), plan.bossRoomId());
        assertTrue(plan.legacyEntitySource().contains("EntitySkeletonBoss"));
        assertEquals(160, plan.maxHealth());
        assertEquals(GalacticCoreIds.id("boss_entity_state/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("event/boss_entity_spawn"), plan.eventAction().target());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.eventAction().evidence().get("source"));
        assertTrue(plan.requiredHostActions().contains("instantiate_boss_entity"));
    }

    @Test
    void bossEncounterAdapterPublishesEventAndSaveTarget() {
        GalacticCoreRuntimeAdapters.BossEncounterAdapterPlan plan = adapters.tickBossEncounter(
                new GalacticCoreRuntimeAdapters.BossEncounterRequest(
                        runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                        new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
                )
        );

        assertTrue(plan.defeated());
        assertEquals("boss_defeated", plan.status());
        assertEquals(GalacticCoreIds.id("boss_encounter_state/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("event/boss_encounter_tick"), plan.eventAction().target());
        assertTrue(plan.droppedKeys().contains("tier_1_key"));
    }

    @Test
    void bossAiAdapterPublishesIntentAndTracksSaveTarget() {
        GalacticCoreRuntimeAdapters.BossAiAdapterPlan plan = adapters.tickBossAi(
                new GalacticCoreRuntimeAdapters.BossAiRequest(
                        runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                        new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
                )
        );

        assertEquals("boss_engaged", plan.status());
        assertEquals("hold_attack_range", plan.movementIntent());
        assertEquals("bone_slam", plan.attackIntent());
        assertTrue(plan.roomLocked());
        assertEquals(GalacticCoreIds.id("boss_ai_state/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("event/boss_ai_step"), plan.eventAction().target());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.eventAction().evidence().get("source"));
        assertTrue(plan.requiredHostActions().contains("execute_bone_slam"));
    }

    @Test
    void treasureInteractionAdapterBroadcastsOpenAndTracksSaveTarget() {
        GalacticCoreRuntimeAdapters.TreasureInteractionPlan plan = adapters.openDungeonTreasure(
                new GalacticCoreRuntimeAdapters.TreasureInteractionRequest(
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

        assertTrue(plan.opened());
        assertTrue(plan.consumedKey());
        assertEquals("reward_claimed", plan.status());
        assertEquals(GalacticCoreIds.id("treasure_state/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("packet/dungeon_treasure_interaction"), plan.networkAction().target());
        assertTrue(plan.loot().contains(GalacticCoreIds.id("loot/moon_dungeon_tier_1")));
    }

    @Test
    void treasureChestScreenAdapterOpensScreenAndTracksSaveTarget() {
        GalacticCoreRuntimeAdapters.TreasureChestScreenPlan plan = adapters.openTreasureChestScreen(
                new GalacticCoreRuntimeAdapters.TreasureInteractionRequest(
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

        assertTrue(plan.openable());
        assertEquals("reward_claimed", plan.status());
        assertEquals(GalacticCoreIds.id("screen/treasure_chest"), plan.screenId());
        assertEquals(GalacticCoreIds.id("screen_state/treasure_chest/moon"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("screen/treasure_chest"), plan.screenAction().target());
        assertEquals("galacticraft_legacy_runtime_adapters", plan.screenAction().evidence().get("source"));
        assertTrue(plan.lootPreview().contains(GalacticCoreIds.id("loot/moon_dungeon_tier_1")));
    }

    @Test
    void treasureChestRenderedMenuAdapterTracksLayoutState() {
        GalacticCoreRuntimeAdapters.RenderedMenuAdapterPlan plan = adapters.openRenderedTreasureChestMenu(
                new GalacticCoreRuntimeAdapters.TreasureInteractionRequest(
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

        assertTrue(plan.accepted());
        assertEquals(GalacticCoreIds.id("screen_layout_state/treasure_chest"), plan.saveDataTarget());
        assertEquals(GalacticCoreIds.id("renderer/treasure_chest_menu"), plan.rendererId());
        assertEquals(GalacticCoreIds.id("screen_layout/treasure_chest"), plan.screenAction().target());
        assertTrue(plan.screenAction().evidence().get("widgets").toString().contains("claim_reward"));
    }

    @Test
    void releaseAdapterSmokeActionsCoverMachineTransferEnvironmentAndRewardPaths() {
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = adapters.releaseAdapterSmokeActions();

        assertEquals(20, actions.size());
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/machine_tick").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/holomap_route_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/screencore_launch_checklist_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("worldgen/transfer_placement/moon").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/dimension_transfer_execute").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/celestial_route_action").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/environment_scan").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("worldgen/dungeon_structure_adapter/moon").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_entity_spawn").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_encounter_tick").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_ai_step").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/dungeon_treasure_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/dungeon_reward_claim").equals(action.target())));
    }
}
