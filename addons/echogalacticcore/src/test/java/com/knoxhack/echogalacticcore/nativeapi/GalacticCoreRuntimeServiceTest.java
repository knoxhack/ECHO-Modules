package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Atmosphere.BREATHABLE;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Atmosphere.HOT_DENSE_ACIDIC;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Atmosphere.VACUUM;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Gravity.LOW_G;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.Gravity.STANDARD_G;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.MachineType.FUEL_LOADER;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.MachineType.OXYGEN_COLLECTOR;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.MachineType.OXYGEN_SEALER;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.MachineType.ROCKET_WORKBENCH;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.ThermalRisk.COLD;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.ThermalRisk.EXTREME_HEAT;
import static com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService.ThermalRisk.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreRuntimeServiceTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();

    @Test
    void oxygenCollectorProducesOxygenFromLeafScanAndEnergy() {
        GalacticCoreRuntimeService.MachineSnapshot state = new GalacticCoreRuntimeService.MachineSnapshot(
                OXYGEN_COLLECTOR,
                100,
                0,
                0,
                0,
                0,
                true,
                false,
                false,
                ""
        );

        GalacticCoreRuntimeService.MachineSnapshot next = runtime.tickMachine(
                state,
                new GalacticCoreRuntimeService.MachineInput(8, 0, 0, false, false)
        );

        assertEquals(96, next.energy());
        assertEquals(16, next.oxygen());
        assertEquals(1, next.progress());
    }

    @Test
    void oxygenSealerConsumesOxygenAndStoresSealedVolume() {
        GalacticCoreRuntimeService.MachineSnapshot state = new GalacticCoreRuntimeService.MachineSnapshot(
                OXYGEN_SEALER,
                100,
                200,
                0,
                0,
                0,
                true,
                false,
                false,
                ""
        );

        GalacticCoreRuntimeService.MachineSnapshot next = runtime.tickMachine(
                state,
                new GalacticCoreRuntimeService.MachineInput(0, 12, 0, false, false)
        );

        assertEquals(92, next.energy());
        assertEquals(176, next.oxygen());
        assertEquals(12, next.sealedVolume());
        assertEquals(1, next.progress());
    }

    @Test
    void fuelLoaderRequiresLinkedPadAndRocketPresence() {
        GalacticCoreRuntimeService.MachineSnapshot state = new GalacticCoreRuntimeService.MachineSnapshot(
                FUEL_LOADER,
                100,
                0,
                0,
                0,
                0,
                true,
                true,
                false,
                ""
        );

        GalacticCoreRuntimeService.MachineSnapshot next = runtime.tickMachine(
                state,
                new GalacticCoreRuntimeService.MachineInput(0, 0, 1000, true, false)
        );

        assertEquals(98, next.energy());
        assertEquals(50, next.fuel());
        assertEquals(1, next.progress());
    }

    @Test
    void rocketWorkbenchProducesTierOneRocketWhenSchematicAndRecipeAreReady() {
        GalacticCoreRuntimeService.MachineSnapshot state = new GalacticCoreRuntimeService.MachineSnapshot(
                ROCKET_WORKBENCH,
                0,
                0,
                0,
                0,
                0,
                true,
                false,
                true,
                ""
        );

        GalacticCoreRuntimeService.MachineSnapshot next = runtime.tickMachine(
                state,
                new GalacticCoreRuntimeService.MachineInput(0, 0, 0, false, true)
        );

        assertEquals(100, next.progress());
        assertEquals(GalacticCoreIds.id("tier_1_rocket"), next.craftedOutput());
    }

    @Test
    void lifeSupportConsumesOxygenInVacuumAndRequiresThermalProtectionForColdBodies() {
        GalacticCoreRuntimeService.LifeSupportResult unsafe = runtime.evaluateLifeSupport(
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, false, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );
        GalacticCoreRuntimeService.LifeSupportResult safe = runtime.evaluateLifeSupport(
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );
        GalacticCoreRuntimeService.LifeSupportResult overworld = runtime.evaluateLifeSupport(
                new GalacticCoreRuntimeService.PlayerGearState(false, false, 0, false, false),
                new GalacticCoreRuntimeService.EnvironmentState("minecraft:overworld", BREATHABLE, NONE)
        );

        assertFalse(unsafe.canBreathe());
        assertEquals("thermal_protection_required", unsafe.status());
        assertTrue(safe.canBreathe());
        assertEquals(1, safe.oxygenConsumed());
        assertTrue(overworld.canBreathe());
        assertEquals(0, overworld.oxygenConsumed());
    }

    @Test
    void energyTransferIsClampedBySourceTargetAndRequest() {
        GalacticCoreRuntimeService.EnergyTransfer transfer = runtime.transferEnergy(
                new GalacticCoreRuntimeService.EnergyBuffer("echogalacticcore:generator/coal", 100, 1000),
                new GalacticCoreRuntimeService.EnergyBuffer("echogalacticcore:machine/oxygen_collector", 995, 1000),
                50
        );

        assertEquals(5, transfer.moved());
        assertEquals(95, transfer.source().energy());
        assertEquals(1000, transfer.target().energy());
    }

    @Test
    void rocketLaunchRequiresFuelOxygenCrewTierAndUnlockedRoute() {
        GalacticCoreRuntimeService.RocketLaunchDecision ready = runtime.prepareLaunch(
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                new GalacticCoreRuntimeService.RouteRequirement(GalacticCoreIds.id("route/moon"), 1, true)
        );
        GalacticCoreRuntimeService.RocketLaunchDecision locked = runtime.prepareLaunch(
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                new GalacticCoreRuntimeService.RouteRequirement(GalacticCoreIds.id("route/mars"), 2, true)
        );

        assertTrue(ready.ready());
        assertEquals(200, ready.countdownTicks());
        assertFalse(locked.ready());
        assertEquals("vehicle_tier_required", locked.reason());
    }

    @Test
    void routeRequirementsFollowLegacyTierProgressionAndUnlockState() {
        GalacticCoreRuntimeService.PlayerProgression starting = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.RouteRequirement moon = runtime.routeRequirement(GalacticCoreIds.id("route/moon"), starting);
        GalacticCoreRuntimeService.RouteRequirement mars = runtime.routeRequirement(GalacticCoreIds.id("route/mars"), starting);
        GalacticCoreRuntimeService.PlayerProgression afterMoon = starting.withRoutes(java.util.List.of(GalacticCoreIds.id("route/mars")));
        GalacticCoreRuntimeService.RouteRequirement unlockedMars = runtime.routeRequirement(GalacticCoreIds.id("route/mars"), afterMoon);

        assertEquals(1, moon.requiredVehicleTier());
        assertTrue(moon.unlocked());
        assertEquals(2, mars.requiredVehicleTier());
        assertFalse(mars.unlocked());
        assertTrue(unlockedMars.unlocked());
    }

    @Test
    void environmentScansExposeGravityHazardsAndDungeons() {
        GalacticCoreRuntimeService.EnvironmentScan moon = runtime.scanEnvironment(GalacticCoreIds.id("moon"));
        GalacticCoreRuntimeService.EnvironmentScan venus = runtime.scanEnvironment(GalacticCoreIds.id("venus"));

        assertEquals(VACUUM, moon.atmosphere());
        assertEquals(COLD, moon.thermalRisk());
        assertEquals(LOW_G, moon.gravity());
        assertEquals(GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"), moon.dungeonId());

        assertEquals(HOT_DENSE_ACIDIC, venus.atmosphere());
        assertEquals(EXTREME_HEAT, venus.thermalRisk());
        assertEquals(STANDARD_G, venus.gravity());
        assertTrue(venus.acidHazard());
        assertEquals(GalacticCoreIds.id("dungeon/venus_dungeon_tier_3"), venus.dungeonId());
    }

    @Test
    void dungeonStructurePlansIncludeBossTreasureAndRewardTargets() {
        GalacticCoreRuntimeService.DungeonStructurePlan mars = runtime.planDungeonStructure(
                GalacticCoreIds.id("dungeon/mars_dungeon_tier_2")
        );

        assertEquals(GalacticCoreIds.id("dungeon/mars_dungeon_tier_2"), mars.dungeonId());
        assertEquals("mars", mars.body());
        assertEquals(2, mars.tier());
        assertEquals(GalacticCoreIds.id("boss/evolved_creeper_boss"), mars.bossId());
        assertEquals("tier_2_key", mars.keyId());
        assertTrue(mars.rooms().stream().anyMatch(GalacticCoreRuntimeService.DungeonRoomPlan::bossSpawn));
        assertTrue(mars.rooms().stream().anyMatch(GalacticCoreRuntimeService.DungeonRoomPlan::treasureLocked));
        assertTrue(mars.schematicRewards().contains(GalacticCoreIds.id("schematic/tier_3_rocket")));
        assertTrue(mars.unlockedRoutes().contains(GalacticCoreIds.id("route/venus")));
        assertTrue(mars.legacySource().contains("DungeonConfigurationMars"));
    }

    @Test
    void bossEntitySpawnPlanNamesLegacySourceAttributesAndHostActions() {
        GalacticCoreRuntimeService.BossEntitySpawnPlan spawn = runtime.planBossEntitySpawn(
                GalacticCoreIds.id("dungeon/venus_dungeon_tier_3")
        );

        assertTrue(spawn.ready());
        assertEquals("boss_spawn_ready", spawn.status());
        assertEquals(GalacticCoreIds.id("boss/spider_queen"), spawn.bossId());
        assertEquals(GalacticCoreIds.id("dungeon_room/venus/boss"), spawn.bossRoomId());
        assertTrue(spawn.legacyEntitySource().contains("EntitySpiderQueen"));
        assertEquals(280, spawn.maxHealth());
        assertEquals(GalacticCoreRuntimeService.BossPhase.DORMANT, spawn.initialPhase());
        assertTrue(spawn.attributes().contains("primary_attack:web_lunge"));
        assertTrue(spawn.requiredHostActions().contains("instantiate_boss_entity"));
        assertTrue(spawn.requiredHostActions().contains("attach_boss_encounter_state"));
    }

    @Test
    void bossEncounterDropsKeyOnlyAfterSafeDefeat() {
        GalacticCoreRuntimeService.BossEncounterState state = runtime.defaultBossEncounter(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
        );
        GalacticCoreRuntimeService.BossEncounterResult unsafe = runtime.tickBossEncounter(
                state,
                new GalacticCoreRuntimeService.BossEncounterInput(true, false, 200)
        );
        GalacticCoreRuntimeService.BossEncounterResult defeated = runtime.tickBossEncounter(
                state,
                new GalacticCoreRuntimeService.BossEncounterInput(true, true, 200)
        );

        assertFalse(unsafe.defeated());
        assertEquals("life_support_required", unsafe.status());
        assertTrue(defeated.defeated());
        assertEquals(GalacticCoreRuntimeService.BossPhase.DEFEATED, defeated.state().phase());
        assertTrue(defeated.droppedKeys().contains("tier_1_key"));
        assertTrue(defeated.state().keyDropped());
    }

    @Test
    void bossAiStepPlansRoomLockAttackAndDefeatActions() {
        GalacticCoreRuntimeService.BossEncounterState state = runtime.defaultBossEncounter(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
        );
        GalacticCoreRuntimeService.BossAiStep engaged = runtime.planBossAiStep(
                state,
                new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 40, false)
        );
        GalacticCoreRuntimeService.BossAiStep defeated = runtime.planBossAiStep(
                state,
                new GalacticCoreRuntimeService.BossAiInput(true, true, true, 4, true, 200, false)
        );
        GalacticCoreRuntimeService.BossAiStep unsealed = runtime.planBossAiStep(
                state,
                new GalacticCoreRuntimeService.BossAiInput(true, false, true, 4, true, 0, false)
        );

        assertEquals(GalacticCoreIds.id("dungeon_room/moon/boss"), engaged.bossRoomId());
        assertEquals("hold_attack_range", engaged.movementIntent());
        assertEquals("bone_slam", engaged.attackIntent());
        assertTrue(engaged.roomLocked());
        assertTrue(engaged.requiredHostActions().contains("lock_treasure_room"));

        assertEquals("boss_defeated", defeated.status());
        assertEquals("collapse", defeated.movementIntent());
        assertFalse(defeated.roomLocked());
        assertTrue(defeated.requiredHostActions().contains("unlock_treasure_room"));
        assertTrue(defeated.requiredHostActions().contains("despawn_boss"));

        assertEquals("boss_room_unsealed", unsealed.status());
        assertTrue(unsealed.requiredHostActions().contains("seal_boss_room"));
    }

    @Test
    void treasureInteractionRequiresTreasureRoomLockBossAndKey() {
        GalacticCoreRuntimeService.PlayerProgression starting = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.TreasureInteraction missingRoom = new GalacticCoreRuntimeService.TreasureInteraction(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                "tier_1_key",
                true,
                true,
                false,
                true
        );
        GalacticCoreRuntimeService.TreasureInteraction valid = new GalacticCoreRuntimeService.TreasureInteraction(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                "tier_1_key",
                true,
                true,
                true,
                true
        );

        GalacticCoreRuntimeService.TreasureInteractionResult blocked = runtime.openDungeonTreasure(starting, missingRoom);
        GalacticCoreRuntimeService.TreasureInteractionResult opened = runtime.openDungeonTreasure(starting, valid);

        assertFalse(blocked.opened());
        assertEquals("treasure_room_required", blocked.status());
        assertTrue(opened.opened());
        assertTrue(opened.consumedKey());
        assertTrue(opened.loot().contains(GalacticCoreIds.id("loot/moon_dungeon_tier_1")));
        assertTrue(opened.progression().unlockedRoutes().contains(GalacticCoreIds.id("route/mars")));
    }

    @Test
    void treasureChestSurfaceExposesLootPreviewAndClaimActions() {
        GalacticCoreRuntimeService.PlayerProgression starting = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.TreasureChestSurface locked = runtime.treasureChestSurface(
                starting,
                new GalacticCoreRuntimeService.TreasureInteraction(
                        GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                        GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                        "tier_1_key",
                        false,
                        false,
                        true,
                        true
                )
        );
        GalacticCoreRuntimeService.TreasureChestSurface open = runtime.treasureChestSurface(
                starting,
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

        assertEquals(GalacticCoreIds.id("screen/treasure_chest"), locked.screenId());
        assertFalse(locked.openable());
        assertEquals("boss_required", locked.status());
        assertTrue(locked.actions().contains("show_requirements"));
        assertTrue(locked.lootPreview().contains(GalacticCoreIds.id("loot/moon_dungeon_tier_1")));

        assertTrue(open.openable());
        assertTrue(open.opened());
        assertTrue(open.consumedKey());
        assertEquals("reward_claimed", open.status());
        assertTrue(open.actions().contains("claim_reward"));
        assertTrue(open.schematicRewards().contains(GalacticCoreIds.id("schematic/tier_2_rocket")));
        assertTrue(open.unlockedRoutes().contains(GalacticCoreIds.id("route/mars")));
    }

    @Test
    void routeSurfaceExposesHoloMapRouteLockAndEnvironmentState() {
        GalacticCoreRuntimeService.CelestialRouteSurface surface = runtime.routeSurface(
                GalacticCoreRuntimeService.PlayerProgression.starting(),
                GalacticCoreIds.id("route/mars")
        );

        assertEquals(GalacticCoreIds.id("route/mars"), surface.selectedRoute());
        assertEquals(GalacticCoreIds.id("mars"), surface.selectedEnvironment());
        assertFalse(surface.selectedUnlocked());
        assertEquals("route_locked", surface.status());
        assertEquals(5, surface.routes().size());
        assertTrue(surface.routes().stream()
                .anyMatch(route -> GalacticCoreIds.id("route/moon").equals(route.routeId()) && route.unlocked()));
        assertTrue(surface.routes().stream()
                .anyMatch(route -> GalacticCoreIds.id("route/venus").equals(route.routeId()) && route.acidHazard()));
    }

    @Test
    void renderedMenusExposeWidgetRegionsAndActions() {
        GalacticCoreRuntimeService.PlayerProgression progression = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.RouteRequirement moon = runtime.routeRequirement(GalacticCoreIds.id("route/moon"), progression);
        GalacticCoreRuntimeService.RenderedMenuLayout routes = runtime.renderHoloMapRouteMenu(
                progression,
                GalacticCoreIds.id("route/mars")
        );
        GalacticCoreRuntimeService.RenderedMenuLayout checklist = runtime.renderLaunchChecklistMenu(
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );
        GalacticCoreRuntimeService.RenderedMenuLayout treasure = runtime.renderTreasureChestMenu(
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

        assertEquals(GalacticCoreIds.id("renderer/holomap_route_menu"), routes.rendererId());
        assertTrue(routes.widgets().stream().anyMatch(widget -> widget.id().equals("select_route") && !widget.enabled()));
        assertTrue(routes.actions().contains("preview_route"));

        assertEquals(GalacticCoreIds.id("renderer/screencore_launch_checklist_menu"), checklist.rendererId());
        assertTrue(checklist.widgets().stream().anyMatch(widget -> widget.id().equals("start_countdown") && widget.enabled()));
        assertTrue(checklist.regions().stream().anyMatch(region -> region.id().equals("checks")));

        assertEquals(GalacticCoreIds.id("renderer/treasure_chest_menu"), treasure.rendererId());
        assertTrue(treasure.widgets().stream().anyMatch(widget -> widget.type().equals("loot_slot")));
        assertTrue(treasure.actions().contains("claim_reward"));
    }

    @Test
    void launchChecklistSurfaceCombinesRocketRouteAndLifeSupportReadiness() {
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

        GalacticCoreRuntimeService.LaunchChecklistSurface ready = runtime.launchChecklistSurface(
                launch,
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );
        GalacticCoreRuntimeService.LaunchChecklistSurface unsafe = runtime.launchChecklistSurface(
                launch,
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, false, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD)
        );

        assertTrue(ready.ready());
        assertEquals("launch_ready", ready.status());
        assertEquals(7, ready.checks().size());
        assertFalse(unsafe.ready());
        assertEquals("thermal_protection_required", unsafe.status());
        assertTrue(unsafe.checks().stream()
                .anyMatch(check -> "environment_life_support".equals(check.id()) && !check.passed()));
    }

    @Test
    void screenInteractionsExposeHoloMapAndScreenCoreWidgetActions() {
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

        GalacticCoreRuntimeService.ScreenInteractionResult selected = runtime.interactHoloMapRoute(
                progression,
                GalacticCoreIds.id("route/moon"),
                "select_route"
        );
        GalacticCoreRuntimeService.ScreenInteractionResult locked = runtime.interactHoloMapRoute(
                progression,
                GalacticCoreIds.id("route/mars"),
                "select_route"
        );
        GalacticCoreRuntimeService.ScreenInteractionResult countdown = runtime.interactLaunchChecklist(
                launch,
                moon,
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.EnvironmentState(GalacticCoreIds.id("moon"), VACUUM, COLD),
                "start_countdown"
        );

        assertTrue(selected.accepted());
        assertEquals("route_selected", selected.status());
        assertEquals(GalacticCoreIds.id("screen/holomap_routes"), selected.screenId());
        assertTrue(selected.requiredHostActions().contains("open_screencore_launch_checklist"));
        assertFalse(locked.accepted());
        assertEquals("route_locked", locked.status());
        assertTrue(locked.requiredHostActions().contains("show_route_requirements"));
        assertTrue(countdown.accepted());
        assertEquals("countdown_armed", countdown.status());
        assertTrue(countdown.requiredHostActions().contains("arm_countdown"));
        assertTrue(countdown.failedChecks().isEmpty());
    }

    @Test
    void transferPlacementPlansLandingZoneAndBlocksUnsafeArrival() {
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
        GalacticCoreRuntimeService.TransferAnchor anchor = new GalacticCoreRuntimeService.TransferAnchor(
                GalacticCoreIds.id("earth_orbit"),
                32,
                96,
                -48,
                true
        );

        GalacticCoreRuntimeService.TransferPlacementPlan moon = runtime.planTransferPlacement(
                progression,
                launch,
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                anchor
        );
        GalacticCoreRuntimeService.TransferPlacementPlan venusWithoutParachute = runtime.planTransferPlacement(
                progression.withRoutes(java.util.List.of(GalacticCoreIds.id("route/venus"))),
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 3, 200),
                GalacticCoreIds.id("route/venus"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                anchor
        );

        assertTrue(moon.ready());
        assertEquals("transfer_placement_ready", moon.status());
        assertEquals(GalacticCoreIds.id("transfer_placement/moon"), moon.placement().placementId());
        assertEquals(32, moon.placement().x());
        assertEquals(96, moon.placement().y());
        assertEquals(-48, moon.placement().z());
        assertEquals("lander_descent", moon.placement().entryMode());
        assertTrue(moon.requiredHostActions().contains("prepare_landing_zone"));

        assertFalse(venusWithoutParachute.ready());
        assertEquals("parachute_required", venusWithoutParachute.status());
        assertEquals("parachute_descent", venusWithoutParachute.placement().entryMode());
    }

    @Test
    void transferExecutionPlansHostTeleportAndChunkActions() {
        GalacticCoreRuntimeService.TransferExecutionPlan plan = runtime.executeTransfer(
                GalacticCoreRuntimeService.PlayerProgression.starting(),
                new GalacticCoreRuntimeService.RocketLaunchState(1000, 1000, true, true, true, 1, 200),
                GalacticCoreIds.id("route/moon"),
                new GalacticCoreRuntimeService.PlayerGearState(true, true, 20, true, false),
                new GalacticCoreRuntimeService.TransferAnchor(GalacticCoreIds.id("earth_orbit"), 12, 96, -12, true)
        );

        assertTrue(plan.ready());
        assertEquals("transfer_execution_ready", plan.status());
        assertEquals(GalacticCoreIds.id("moon"), plan.environmentId());
        assertEquals(12, plan.x());
        assertEquals(96, plan.y());
        assertEquals(-12, plan.z());
        assertEquals("lander_descent", plan.entryMode());
        assertTrue(plan.requiredHostActions().contains("load_destination_dimension"));
        assertTrue(plan.requiredHostActions().contains("ticket_destination_chunk"));
        assertTrue(plan.requiredHostActions().contains("place_player_at_destination"));
    }

    @Test
    void dungeonRewardsUnlockRoutesSchematicsAndLootOnce() {
        GalacticCoreRuntimeService.PlayerProgression starting = GalacticCoreRuntimeService.PlayerProgression.starting();
        GalacticCoreRuntimeService.DungeonRewardClaim moonClaim = new GalacticCoreRuntimeService.DungeonRewardClaim(
                GalacticCoreIds.id("dungeon/moon_dungeon_tier_1"),
                GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                "tier_1_key",
                true,
                true
        );

        GalacticCoreRuntimeService.DungeonRewardResult first = runtime.claimDungeonReward(starting, moonClaim);
        GalacticCoreRuntimeService.DungeonRewardResult second = runtime.claimDungeonReward(first.progression(), moonClaim);

        assertTrue(first.claimed());
        assertEquals("reward_claimed", first.status());
        assertTrue(first.unlockedRoutes().contains(GalacticCoreIds.id("route/mars")));
        assertTrue(first.schematicRewards().contains(GalacticCoreIds.id("schematic/tier_2_rocket")));
        assertTrue(first.loot().contains(GalacticCoreIds.id("loot/moon_dungeon_tier_1")));
        assertTrue(first.progression().unlockedRoutes().contains(GalacticCoreIds.id("route/mars")));
        assertTrue(first.progression().schematics().contains(GalacticCoreIds.id("schematic/moon_buggy")));

        assertFalse(second.claimed());
        assertEquals("reward_already_claimed", second.status());
    }

    @Test
    void marsRewardUnlocksAsteroidsVenusAndTierThreeRocket() {
        GalacticCoreRuntimeService.PlayerProgression afterMoon = GalacticCoreRuntimeService.PlayerProgression.starting()
                .withRoutes(java.util.List.of(GalacticCoreIds.id("route/mars")))
                .withSchematics(java.util.List.of(GalacticCoreIds.id("schematic/tier_2_rocket")));

        GalacticCoreRuntimeService.DungeonRewardResult result = runtime.claimDungeonReward(
                afterMoon,
                new GalacticCoreRuntimeService.DungeonRewardClaim(
                        GalacticCoreIds.id("dungeon/mars_dungeon_tier_2"),
                        GalacticCoreIds.id("boss/evolved_creeper_boss"),
                        "tier_2_key",
                        true,
                        true
                )
        );

        assertTrue(result.claimed());
        assertTrue(result.unlockedRoutes().contains(GalacticCoreIds.id("route/asteroids")));
        assertTrue(result.unlockedRoutes().contains(GalacticCoreIds.id("route/venus")));
        assertTrue(result.schematicRewards().contains(GalacticCoreIds.id("schematic/tier_3_rocket")));
    }
}
