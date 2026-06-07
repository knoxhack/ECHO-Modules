package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
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

class GalacticCoreRuntimeGatewayTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);

    @Test
    void machineTickActionPublishesBeforeAndAfterState() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishMachineTick(
                new GalacticCoreRuntimeService.MachineSnapshot(OXYGEN_COLLECTOR, 100, 0, 0, 0, 0, true, false, false, ""),
                new GalacticCoreRuntimeService.MachineInput(8, 0, 0, false, false)
        );

        assertEquals(GalacticCoreIds.id("event/machine_tick"), action.target());
        assertEquals("events", action.surface());
        assertEquals("publish", action.action());
        assertEquals(true, action.evidence().get("mutated"));
        assertTrue(action.evidence().get("after").toString().contains("oxygen=16"));
    }

    @Test
    void lifeSupportActionPublishesBreathabilityResult() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishLifeSupportTick(
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );

        assertEquals(GalacticCoreIds.id("event/player_tick_life_support"), action.target());
        assertEquals("oxygen_consumed", action.evidence().get("status"));
        assertEquals(1, action.evidence().get("oxygenConsumed"));
    }

    @Test
    void routeAndLaunchActionsExposeScreenAndNetworkPayloads() {
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

        GalacticCoreRuntimeGateway.RuntimeAction selection = gateway.openCelestialSelection(progression, GalacticCoreIds.id("route/moon"));
        GalacticCoreRuntimeGateway.RuntimeAction routeSurface = gateway.openHoloMapRouteSurface(progression, GalacticCoreIds.id("route/mars"));
        GalacticCoreRuntimeGateway.RuntimeAction routeInteraction = gateway.publishHoloMapRouteInteraction(
                progression,
                GalacticCoreIds.id("route/moon"),
                "select_route"
        );
        GalacticCoreRuntimeGateway.RuntimeAction routeAction = gateway.sendRouteAction(launch, moon);
        GalacticCoreRuntimeGateway.RuntimeAction checklist = gateway.openLaunchChecklist(launch, moon);
        GalacticCoreRuntimeGateway.RuntimeAction screenCoreChecklist = gateway.openScreenCoreLaunchChecklist(
                launch,
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );
        GalacticCoreRuntimeGateway.RuntimeAction routeLayout = gateway.openRenderedHoloMapRouteMenu(
                progression,
                GalacticCoreIds.id("route/mars")
        );
        GalacticCoreRuntimeGateway.RuntimeAction checklistLayout = gateway.openRenderedLaunchChecklistMenu(
                launch,
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );
        GalacticCoreRuntimeGateway.RuntimeAction checklistInteraction = gateway.publishScreenCoreLaunchChecklistInteraction(
                launch,
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD),
                "start_countdown"
        );

        assertEquals("screens", selection.surface());
        assertEquals("open", selection.action());
        assertEquals(true, selection.evidence().get("unlocked"));
        assertEquals(GalacticCoreIds.id("screen/holomap_routes"), routeSurface.target());
        assertEquals(false, routeSurface.evidence().get("selectedUnlocked"));
        assertTrue(routeSurface.evidence().get("lockedRoutes").toString().contains("route/mars"));
        assertEquals(GalacticCoreIds.id("packet/holomap_route_interaction"), routeInteraction.target());
        assertEquals("network", routeInteraction.surface());
        assertEquals("sendToPlayer", routeInteraction.action());
        assertEquals("route_selected", routeInteraction.evidence().get("status"));
        assertEquals("network", routeAction.surface());
        assertEquals("sendToPlayer", routeAction.action());
        assertEquals(true, routeAction.evidence().get("ready"));
        assertEquals(GalacticCoreIds.id("screen/launch_checklist"), checklist.target());
        assertEquals("ready", checklist.evidence().get("reason"));
        assertEquals(GalacticCoreIds.id("screen/screencore_launch_checklist"), screenCoreChecklist.target());
        assertEquals(true, screenCoreChecklist.evidence().get("ready"));
        assertTrue(screenCoreChecklist.evidence().get("passedChecks").toString().contains("environment_life_support"));
        assertEquals(GalacticCoreIds.id("screen_layout/holomap_routes"), routeLayout.target());
        assertEquals(GalacticCoreIds.id("renderer/holomap_route_menu"), routeLayout.evidence().get("rendererId"));
        assertTrue((int) routeLayout.evidence().get("widgetCount") >= 7);
        assertEquals(GalacticCoreIds.id("screen_layout/screencore_launch_checklist"), checklistLayout.target());
        assertTrue(checklistLayout.evidence().get("widgets").toString().contains("start_countdown"));
        assertEquals(GalacticCoreIds.id("packet/screencore_launch_checklist_interaction"), checklistInteraction.target());
        assertEquals("countdown_armed", checklistInteraction.evidence().get("status"));
        assertTrue(checklistInteraction.evidence().get("requiredHostActions").toString().contains("arm_countdown"));
    }

    @Test
    void transferPlacementActionPlacesLandingZoneThroughWorldgen() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.placeTransferLandingZone(
                GalacticCoreRuntimeService.PlayerProgression.starting(),
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
        );

        assertEquals(GalacticCoreIds.id("worldgen/transfer_placement/moon"), action.target());
        assertEquals("worldgen", action.surface());
        assertEquals("placeStructure", action.action());
        assertEquals(true, action.evidence().get("ready"));
        assertEquals("transfer_placement_ready", action.evidence().get("status"));
        assertEquals("lander_descent", action.evidence().get("entryMode"));
        assertTrue(action.evidence().get("requiredHostActions").toString().contains("prepare_landing_zone"));
    }

    @Test
    void transferExecutionActionPublishesHostTeleportIntent() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishTransferExecution(
                GalacticCoreRuntimeService.PlayerProgression.starting(),
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
        );

        assertEquals(GalacticCoreIds.id("event/dimension_transfer_execute"), action.target());
        assertEquals("events", action.surface());
        assertEquals("publish", action.action());
        assertEquals(true, action.evidence().get("ready"));
        assertEquals("transfer_execution_ready", action.evidence().get("status"));
        assertTrue(action.evidence().get("requiredHostActions").toString().contains("ticket_destination_chunk"));
        assertTrue(action.evidence().get("requiredHostActions").toString().contains("place_player_at_destination"));
    }

    @Test
    void dungeonRewardActionBroadcastsAttachmentBackedRewardClaim() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.claimDungeonReward(
                GalacticCoreRuntimeService.PlayerProgression.starting(),
                new GalacticCoreRuntimeService.DungeonRewardClaim(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        true,
                        true
                )
        );

        assertEquals(GalacticCoreIds.id("packet/dungeon_reward_claim"), action.target());
        assertEquals("network", action.surface());
        assertEquals("broadcast", action.action());
        assertEquals(true, action.evidence().get("claimed"));
        assertTrue(action.evidence().get("unlockedRoutes").toString().contains("route/mars"));
    }

    @Test
    void dungeonStructureActionRegistersWorldgenFeaturePlan() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishDungeonStructurePlan(
                GalacticCoreIds.id("dungeon/venus_dungeon_tier_3")
        );

        assertEquals(GalacticCoreIds.id("worldgen/dungeon_structure/venus"), action.target());
        assertEquals("worldgen", action.surface());
        assertEquals("registerFeature", action.action());
        assertEquals(GalacticCoreIds.id("boss/spider_queen"), action.evidence().get("boss"));
        assertEquals(GalacticCoreIds.id("dungeon_room/venus/boss"), action.evidence().get("bossRoom"));
        assertEquals(GalacticCoreIds.id("dungeon_room/venus/treasure"), action.evidence().get("treasureRoom"));
        assertTrue(action.evidence().get("schematicRewards").toString().contains("astro_miner"));
    }

    @Test
    void bossEntitySpawnActionPublishesSpawnIntentAndAttributes() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishBossEntitySpawn(
                GalacticCoreIds.id("dungeon/mars_dungeon_tier_2")
        );

        assertEquals(GalacticCoreIds.id("event/boss_entity_spawn"), action.target());
        assertEquals("events", action.surface());
        assertEquals("publish", action.action());
        assertEquals(GalacticCoreIds.id("boss/evolved_creeper_boss"), action.evidence().get("boss"));
        assertEquals(GalacticCoreIds.id("dungeon_room/mars/boss"), action.evidence().get("bossRoom"));
        assertEquals("boss_spawn_ready", action.evidence().get("status"));
        assertTrue(action.evidence().get("legacyEntitySource").toString().contains("EntityCreeperBoss"));
        assertTrue(action.evidence().get("attributes").toString().contains("primary_attack:charged_leap"));
        assertTrue(action.evidence().get("requiredHostActions").toString().contains("instantiate_boss_entity"));
    }

    @Test
    void bossEncounterActionPublishesDefeatAndKeyDrop() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishBossEncounterTick(
                runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
        );

        assertEquals(GalacticCoreIds.id("event/boss_encounter_tick"), action.target());
        assertEquals("events", action.surface());
        assertEquals("publish", action.action());
        assertEquals(true, action.evidence().get("defeated"));
        assertTrue(action.evidence().get("droppedKeys").toString().contains("tier_1_key"));
    }

    @Test
    void bossAiActionPublishesEntityIntentAndRoomLockActions() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishBossAiStep(
                runtime.defaultBossEncounter(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")),
                new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
        );

        assertEquals(GalacticCoreIds.id("event/boss_ai_step"), action.target());
        assertEquals("events", action.surface());
        assertEquals("publish", action.action());
        assertEquals("bone_slam", action.evidence().get("attackIntent"));
        assertEquals("hold_attack_range", action.evidence().get("movementIntent"));
        assertEquals(true, action.evidence().get("roomLocked"));
        assertTrue(action.evidence().get("requiredHostActions").toString().contains("lock_treasure_room"));
    }

    @Test
    void treasureInteractionActionBroadcastsOpenedLootAndUnlocks() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.publishTreasureInteraction(
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
        );

        assertEquals(GalacticCoreIds.id("packet/dungeon_treasure_interaction"), action.target());
        assertEquals("network", action.surface());
        assertEquals("broadcast", action.action());
        assertEquals(true, action.evidence().get("opened"));
        assertEquals(true, action.evidence().get("consumedKey"));
        assertTrue(action.evidence().get("unlockedRoutes").toString().contains("route/mars"));
    }

    @Test
    void treasureChestScreenActionOpensScreenCoreRewardSurface() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.openTreasureChestScreen(
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
        );

        assertEquals(GalacticCoreIds.id("screen/treasure_chest"), action.target());
        assertEquals("screens", action.surface());
        assertEquals("open", action.action());
        assertEquals(true, action.evidence().get("openable"));
        assertEquals("reward_claimed", action.evidence().get("status"));
        assertTrue(action.evidence().get("lootPreview").toString().contains("moon_dungeon_tier_1"));
        assertTrue(action.evidence().get("actions").toString().contains("claim_reward"));
    }

    @Test
    void treasureChestRenderedMenuActionOpensLayoutWithLootWidgets() {
        GalacticCoreRuntimeGateway.RuntimeAction action = gateway.openRenderedTreasureChestMenu(
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
        );

        assertEquals(GalacticCoreIds.id("screen_layout/treasure_chest"), action.target());
        assertEquals("screens", action.surface());
        assertEquals("open", action.action());
        assertEquals(GalacticCoreIds.id("renderer/treasure_chest_menu"), action.evidence().get("rendererId"));
        assertTrue(action.evidence().get("widgets").toString().contains("claim_reward"));
    }

    @Test
    void releaseSmokeActionsCoverEventsNetworkScreensAndRewards() {
        List<GalacticCoreRuntimeGateway.RuntimeAction> actions = gateway.releaseSmokeActions();

        assertEquals(22, actions.size());
        assertTrue(actions.stream().anyMatch(action -> "events".equals(action.surface()) && "publish".equals(action.action())));
        assertTrue(actions.stream().anyMatch(action -> "network".equals(action.surface()) && "sendToPlayer".equals(action.action())));
        assertTrue(actions.stream().anyMatch(action -> "network".equals(action.surface()) && "broadcast".equals(action.action())));
        assertTrue(actions.stream().anyMatch(action -> "screens".equals(action.surface()) && "open".equals(action.action())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/holomap_routes").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/screencore_launch_checklist").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("screen_layout/treasure_chest").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/holomap_route_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/screencore_launch_checklist_interaction").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> "worldgen".equals(action.surface()) && "registerFeature".equals(action.action())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("worldgen/transfer_placement/moon").equals(action.target())
                && "placeStructure".equals(action.action())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/dimension_transfer_execute").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_entity_spawn").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_encounter_tick").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("event/boss_ai_step").equals(action.target())));
        assertTrue(actions.stream().anyMatch(action -> GalacticCoreIds.id("packet/dungeon_treasure_interaction").equals(action.target())));
        assertFalse(actions.stream().anyMatch(action -> !"galacticraft_legacy_runtime_gateway".equals(action.evidence().get("source"))));
    }
}
