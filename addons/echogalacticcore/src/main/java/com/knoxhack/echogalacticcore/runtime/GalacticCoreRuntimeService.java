package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GalacticCoreRuntimeService {
    public static final int MACHINE_ENERGY_CAPACITY = 12000;
    public static final int MACHINE_OXYGEN_CAPACITY = 24000;
    public static final int MACHINE_FUEL_CAPACITY = 12000;
    private static final List<String> CELESTIAL_ROUTE_IDS = List.of(
            GalacticCoreIds.id("route/earth_orbit"),
            GalacticCoreIds.id("route/moon"),
            GalacticCoreIds.id("route/mars"),
            GalacticCoreIds.id("route/asteroids"),
            GalacticCoreIds.id("route/venus")
    );

    public MachineSnapshot defaultMachine(MachineType type) {
        return new MachineSnapshot(type, 0, 0, 0, 0, 0, false, false, false, "");
    }

    public MachineSnapshot tickMachine(MachineSnapshot state, MachineInput input) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(input, "input");
        if (!state.redstoneEnabled()) {
            return state;
        }
        return switch (state.type()) {
            case OXYGEN_COLLECTOR -> tickOxygenCollector(state, input);
            case OXYGEN_SEALER -> tickOxygenSealer(state, input);
            case FUEL_LOADER -> tickFuelLoader(state, input);
            case ROCKET_WORKBENCH -> tickRocketWorkbench(state, input);
        };
    }

    public EnergyTransfer transferEnergy(EnergyBuffer source, EnergyBuffer target, int requested) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        int moved = Math.min(Math.max(0, requested), Math.min(source.energy(), target.capacity() - target.energy()));
        return new EnergyTransfer(
                new EnergyBuffer(source.id(), source.energy() - moved, source.capacity()),
                new EnergyBuffer(target.id(), target.energy() + moved, target.capacity()),
                moved
        );
    }

    public LifeSupportResult evaluateLifeSupport(PlayerGearState gear, EnvironmentState environment) {
        Objects.requireNonNull(gear, "gear");
        Objects.requireNonNull(environment, "environment");
        if (environment.breathable()) {
            return new LifeSupportResult(true, 0, thermalProtected(gear, environment), "breathable_atmosphere");
        }
        boolean oxygenReady = gear.hasMask() && gear.hasOxygenGear() && gear.oxygenStored() > 0;
        boolean thermalReady = thermalProtected(gear, environment);
        if (oxygenReady && thermalReady) {
            return new LifeSupportResult(true, 1, true, "oxygen_consumed");
        }
        if (!oxygenReady) {
            return new LifeSupportResult(false, 0, thermalReady, "missing_oxygen_gear_or_storage");
        }
        return new LifeSupportResult(false, 0, false, "thermal_protection_required");
    }

    public RocketLaunchDecision prepareLaunch(RocketLaunchState state, RouteRequirement route) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(route, "route");
        if (!state.padAssembled()) {
            return RocketLaunchDecision.blocked("launch_pad_incomplete");
        }
        if (state.fuelStored() < state.requiredFuel()) {
            return RocketLaunchDecision.blocked("fuel_required");
        }
        if (!state.oxygenChecked()) {
            return RocketLaunchDecision.blocked("life_support_check_required");
        }
        if (!state.crewSeated()) {
            return RocketLaunchDecision.blocked("crew_not_seated");
        }
        if (state.vehicleTier() < route.requiredVehicleTier()) {
            return RocketLaunchDecision.blocked("vehicle_tier_required");
        }
        if (!route.unlocked()) {
            return RocketLaunchDecision.blocked("route_locked");
        }
        return new RocketLaunchDecision(true, "ready", Math.max(0, state.countdownTicks()));
    }

    public RouteRequirement routeRequirement(String routeId, PlayerProgression progression) {
        Objects.requireNonNull(progression, "progression");
        String id = requireId(routeId);
        int tier = switch (id) {
            case "echogalacticcore:route/earth_orbit", "echogalacticcore:route/moon" -> 1;
            case "echogalacticcore:route/mars" -> 2;
            case "echogalacticcore:route/asteroids", "echogalacticcore:route/venus" -> 3;
            default -> throw new IllegalArgumentException("Unknown GalacticCore route " + routeId);
        };
        return new RouteRequirement(id, tier, progression.unlockedRoutes().contains(id));
    }

    public EnvironmentScan scanEnvironment(String environmentId) {
        String id = requireId(environmentId);
        return switch (id) {
            case "echogalacticcore:earth_orbit" -> new EnvironmentScan(
                    id,
                    GalacticCoreIds.id("route/earth_orbit"),
                    Atmosphere.VACUUM,
                    ThermalRisk.COLD,
                    Gravity.ZERO_G,
                    true,
                    true,
                    false,
                    ""
            );
            case "echogalacticcore:moon" -> new EnvironmentScan(
                    id,
                    GalacticCoreIds.id("route/moon"),
                    Atmosphere.VACUUM,
                    ThermalRisk.COLD,
                    Gravity.LOW_G,
                    true,
                    true,
                    false,
                    GalacticCoreIds.id("dungeon/moon_dungeon_tier_1")
            );
            case "echogalacticcore:mars" -> new EnvironmentScan(
                    id,
                    GalacticCoreIds.id("route/mars"),
                    Atmosphere.THIN_CO2,
                    ThermalRisk.COLD,
                    Gravity.LOW_G,
                    true,
                    true,
                    false,
                    GalacticCoreIds.id("dungeon/mars_dungeon_tier_2")
            );
            case "echogalacticcore:asteroids" -> new EnvironmentScan(
                    id,
                    GalacticCoreIds.id("route/asteroids"),
                    Atmosphere.VACUUM,
                    ThermalRisk.COLD,
                    Gravity.MICRO_G,
                    true,
                    true,
                    false,
                    ""
            );
            case "echogalacticcore:venus" -> new EnvironmentScan(
                    id,
                    GalacticCoreIds.id("route/venus"),
                    Atmosphere.HOT_DENSE_ACIDIC,
                    ThermalRisk.EXTREME_HEAT,
                    Gravity.STANDARD_G,
                    true,
                    true,
                    true,
                    GalacticCoreIds.id("dungeon/venus_dungeon_tier_3")
            );
            default -> throw new IllegalArgumentException("Unknown GalacticCore environment " + environmentId);
        };
    }

    public DungeonStructurePlan planDungeonStructure(String dungeonId) {
        String id = requireId(dungeonId);
        DungeonRewardTrack track = rewardTrack(id);
        return switch (id) {
            case "echogalacticcore:dungeon/moon_dungeon_tier_1" -> dungeonStructure(
                    id,
                    "moon",
                    1,
                    "micdoodle8.mods.galacticraft.core.world.gen.dungeon.DungeonConfigurationMoon",
                    track,
                    List.of(
                            room("moon", "entrance", "legacy_moon_entrance", true, false, false),
                            room("moon", "corridor", "legacy_moon_corridor", true, false, false),
                            room("moon", "boss", "legacy_moon_boss_room", true, false, true),
                            room("moon", "treasure", "legacy_moon_treasure_room", true, true, false)
                    )
            );
            case "echogalacticcore:dungeon/mars_dungeon_tier_2" -> dungeonStructure(
                    id,
                    "mars",
                    2,
                    "micdoodle8.mods.galacticraft.planets.mars.world.gen.dungeon.DungeonConfigurationMars",
                    track,
                    List.of(
                            room("mars", "entrance", "legacy_mars_entrance", true, false, false),
                            room("mars", "corridor", "legacy_mars_corridor", true, false, false),
                            room("mars", "trap", "legacy_mars_trap_room", true, false, false),
                            room("mars", "boss", "legacy_mars_boss_room", true, false, true),
                            room("mars", "treasure", "legacy_mars_treasure_room", true, true, false)
                    )
            );
            case "echogalacticcore:dungeon/venus_dungeon_tier_3" -> dungeonStructure(
                    id,
                    "venus",
                    3,
                    "micdoodle8.mods.galacticraft.planets.venus.world.gen.dungeon.DungeonConfigurationVenus",
                    track,
                    List.of(
                            room("venus", "entrance", "legacy_venus_entrance", true, false, false),
                            room("venus", "corridor", "legacy_venus_corridor", true, false, false),
                            room("venus", "hazard", "legacy_venus_acid_hazard_room", true, false, false),
                            room("venus", "boss", "legacy_venus_boss_room", true, false, true),
                            room("venus", "treasure", "legacy_venus_treasure_room", true, true, false)
                    )
            );
            default -> throw new IllegalArgumentException("Unknown GalacticCore dungeon structure " + dungeonId);
        };
    }

    public BossEncounterState defaultBossEncounter(String dungeonId) {
        DungeonStructurePlan structure = planDungeonStructure(dungeonId);
        int health = switch (structure.tier()) {
            case 1 -> 160;
            case 2 -> 220;
            default -> 280;
        };
        return new BossEncounterState(
                structure.dungeonId(),
                structure.bossId(),
                health,
                health,
                BossPhase.DORMANT,
                false,
                false,
                "waiting_for_player"
        );
    }

    public BossEntitySpawnPlan planBossEntitySpawn(String dungeonId) {
        DungeonStructurePlan structure = planDungeonStructure(dungeonId);
        BossEncounterState encounter = defaultBossEncounter(dungeonId);
        BossProfile profile = bossProfile(structure.bossId());
        int x = switch (structure.body()) {
            case "mars" -> 12;
            case "venus" -> -12;
            default -> 0;
        };
        int z = switch (structure.body()) {
            case "mars" -> -8;
            case "venus" -> 8;
            default -> 0;
        };
        List<String> attributes = List.of(
                "preferred_range:" + profile.preferredRange(),
                "primary_attack:" + profile.primaryAttack(),
                "ranged_attack:" + profile.rangedAttack(),
                "summon_attack:" + profile.summonAttack()
        );
        return new BossEntitySpawnPlan(
                true,
                "boss_spawn_ready",
                structure.dungeonId(),
                structure.bossId(),
                structure.bossRoomId(),
                structure.body(),
                legacyBossEntitySource(structure.bossId()),
                encounter.maxHealth(),
                encounter.phase(),
                x,
                48,
                z,
                attributes,
                List.of(
                        "keep_boss_room_loaded",
                        "instantiate_boss_entity",
                        "apply_legacy_boss_attributes",
                        "attach_boss_encounter_state",
                        "seal_boss_room",
                        "lock_treasure_room",
                        "sync_boss_bar"
                )
        );
    }

    public BossEncounterResult tickBossEncounter(BossEncounterState state, BossEncounterInput input) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(input, "input");
        DungeonRewardTrack track = rewardTrack(state.dungeonId());
        if (!track.bossId().equals(state.bossId())) {
            return new BossEncounterResult(state, List.of(), false, "boss_track_mismatch");
        }
        if (!input.playerInBossRoom()) {
            return new BossEncounterResult(
                    state.withPhase(BossPhase.DORMANT).withStatus("waiting_for_player"),
                    List.of(),
                    false,
                    "waiting_for_player"
            );
        }
        if (!input.lifeSupportSafe()) {
            return new BossEncounterResult(
                    state.withPhase(BossPhase.ENRAGED).withStatus("life_support_required"),
                    List.of(GalacticCoreIds.id("event/life_support_warning")),
                    false,
                    "life_support_required"
            );
        }
        if (state.defeated()) {
            return new BossEncounterResult(state.withStatus("already_defeated"), List.of(track.keyId()), true, "already_defeated");
        }
        int nextHealth = Math.max(0, state.health() - input.damageDealt());
        boolean defeated = nextHealth == 0;
        BossPhase nextPhase = defeated ? BossPhase.DEFEATED : bossPhase(nextHealth, state.maxHealth());
        BossEncounterState next = new BossEncounterState(
                state.dungeonId(),
                state.bossId(),
                nextHealth,
                state.maxHealth(),
                nextPhase,
                defeated,
                defeated || state.keyDropped(),
                defeated ? "boss_defeated" : "boss_engaged"
        );
        return new BossEncounterResult(
                next,
                defeated ? List.of(track.keyId()) : List.of(),
                defeated,
                next.status()
        );
    }

    public BossAiStep planBossAiStep(BossEncounterState state, BossAiInput input) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(input, "input");
        BossEncounterResult encounter = tickBossEncounter(
                state,
                new BossEncounterInput(input.playerInBossRoom(), input.lifeSupportSafe(), input.damageDealt())
        );
        DungeonStructurePlan structure = planDungeonStructure(state.dungeonId());
        if (!input.playerInBossRoom()) {
            return new BossAiStep(
                    encounter.state(),
                    structure.bossRoomId(),
                    "patrol_boss_room",
                    "none",
                    List.of("keep_boss_room_loaded", "wait_for_player"),
                    false,
                    "waiting_for_player"
            );
        }
        if (!input.roomSealed()) {
            return new BossAiStep(
                    encounter.state().withStatus("boss_room_unsealed"),
                    structure.bossRoomId(),
                    "hold_room_center",
                    "none",
                    List.of("seal_boss_room", "lock_treasure_room"),
                    true,
                    "boss_room_unsealed"
            );
        }
        if (!input.lifeSupportSafe()) {
            return new BossAiStep(
                    encounter.state(),
                    structure.bossRoomId(),
                    "pressure_player",
                    "life_support_pressure",
                    List.of("seal_boss_room", "lock_treasure_room", "emit_life_support_warning"),
                    true,
                    encounter.status()
            );
        }
        if (encounter.defeated()) {
            return new BossAiStep(
                    encounter.state(),
                    structure.bossRoomId(),
                    "collapse",
                    "none",
                    List.of("unlock_treasure_room", "drop_key_if_missing", "play_boss_death", "despawn_boss"),
                    false,
                    encounter.status()
            );
        }
        BossProfile profile = bossProfile(state.bossId());
        boolean closeEnough = input.targetVisible() && input.targetDistance() <= profile.preferredRange();
        boolean summon = !input.minionWaveActive() && encounter.state().phase() == BossPhase.ENRAGED;
        String movement = closeEnough ? "hold_attack_range" : "pathfind_to_player";
        String attack = summon ? profile.summonAttack() : (closeEnough ? profile.primaryAttack() : profile.rangedAttack());
        List<String> actions = new ArrayList<>();
        actions.add("seal_boss_room");
        actions.add("lock_treasure_room");
        actions.add(closeEnough ? "face_target" : "pathfind_to_player");
        actions.add("execute_" + attack);
        if (summon) {
            actions.add("summon_minion_wave");
        }
        if (encounter.state().phase() == BossPhase.ENRAGED) {
            actions.add("apply_enraged_boss_modifiers");
        }
        return new BossAiStep(
                encounter.state(),
                structure.bossRoomId(),
                movement,
                attack,
                actions,
                true,
                encounter.status()
        );
    }

    public TreasureInteractionResult openDungeonTreasure(PlayerProgression progression, TreasureInteraction interaction) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(interaction, "interaction");
        if (!interaction.playerInTreasureRoom()) {
            return new TreasureInteractionResult(progression, List.of(), List.of(), List.of(), false, false, "treasure_room_required");
        }
        if (!interaction.treasureLocked()) {
            return new TreasureInteractionResult(progression, List.of(), List.of(), List.of(), false, false, "treasure_lock_required");
        }
        DungeonRewardResult reward = claimDungeonReward(
                progression,
                new DungeonRewardClaim(
                        interaction.dungeonId(),
                        interaction.bossId(),
                        interaction.keyId(),
                        interaction.bossDefeated(),
                        interaction.hasKey()
                )
        );
        return new TreasureInteractionResult(
                reward.progression(),
                reward.unlockedRoutes(),
                reward.schematicRewards(),
                reward.loot(),
                reward.claimed(),
                reward.claimed(),
                reward.status()
        );
    }

    public TreasureChestSurface treasureChestSurface(PlayerProgression progression, TreasureInteraction interaction) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(interaction, "interaction");
        DungeonStructurePlan structure = planDungeonStructure(interaction.dungeonId());
        DungeonRewardTrack track = rewardTrack(interaction.dungeonId());
        TreasureInteractionResult result = openDungeonTreasure(progression, interaction);
        boolean openable = result.opened();
        List<String> actions = openable
                ? List.of("claim_reward", "sync_progression", "close")
                : List.of("show_requirements", "close");
        return new TreasureChestSurface(
                GalacticCoreIds.id("screen/treasure_chest"),
                structure.dungeonId(),
                structure.treasureRoomId(),
                interaction.treasureLocked(),
                openable,
                result.opened(),
                result.consumedKey(),
                result.status(),
                track.keyId(),
                result.loot().isEmpty() ? List.of(track.lootId()) : result.loot(),
                result.schematicRewards().isEmpty() ? track.schematicRewards() : result.schematicRewards(),
                result.unlockedRoutes().isEmpty() ? track.unlockedRoutes() : result.unlockedRoutes(),
                actions
        );
    }

    public RenderedMenuLayout renderHoloMapRouteMenu(PlayerProgression progression, String selectedRoute) {
        CelestialRouteSurface surface = routeSurface(progression, selectedRoute);
        List<MenuWidget> routeWidgets = surface.routes().stream()
                .map(route -> new MenuWidget(
                        "route_" + route.routeId().substring(route.routeId().lastIndexOf('/') + 1),
                        "route_row",
                        route.routeId(),
                        route.routeId().equals(surface.selectedRoute()) ? "selected_route" : "route_entry",
                        route.unlocked() ? "select_route" : "preview_route",
                        route.unlocked(),
                        route.unlocked() ? "route_selectable" : "route_locked"
                ))
                .toList();
        List<MenuWidget> widgets = new ArrayList<>(routeWidgets);
        widgets.add(new MenuWidget("environment_preview", "preview_panel", surface.selectedEnvironment(), "selected_environment", "preview_route", true, surface.status()));
        widgets.add(new MenuWidget("select_route", "button", surface.selectedRoute(), "selected_route", "select_route", surface.selectedUnlocked(), surface.status()));
        return new RenderedMenuLayout(
                GalacticCoreIds.id("screen/holomap_routes"),
                GalacticCoreIds.id("renderer/holomap_route_menu"),
                "HoloMap Routes",
                surface.status(),
                List.of(
                        new MenuRegion("routes", "route_list"),
                        new MenuRegion("preview", "environment_preview"),
                        new MenuRegion("actions", "route_actions")
                ),
                widgets,
                List.of("preview_route", "select_route")
        );
    }

    public RenderedMenuLayout renderLaunchChecklistMenu(
            RocketLaunchState launchState,
            RouteRequirement route,
            PlayerGearState gear,
            EnvironmentState environment
    ) {
        LaunchChecklistSurface surface = launchChecklistSurface(launchState, route, gear, environment);
        List<MenuWidget> checkWidgets = surface.checks().stream()
                .map(check -> new MenuWidget(
                        "check_" + check.id(),
                        "check_row",
                        check.id(),
                        check.id(),
                        "refresh_checks",
                        true,
                        check.status()
                ))
                .toList();
        List<MenuWidget> widgets = new ArrayList<>(checkWidgets);
        widgets.add(new MenuWidget("start_countdown", "button", surface.routeId(), "countdown", "start_countdown", surface.ready(), surface.status()));
        widgets.add(new MenuWidget("abort_launch", "button", surface.routeId(), "countdown", "abort_launch", true, "launch_abort_available"));
        return new RenderedMenuLayout(
                GalacticCoreIds.id("screen/screencore_launch_checklist"),
                GalacticCoreIds.id("renderer/screencore_launch_checklist_menu"),
                "Launch Checklist",
                surface.status(),
                List.of(
                        new MenuRegion("checks", "checklist"),
                        new MenuRegion("route", "route_summary"),
                        new MenuRegion("actions", "launch_controls")
                ),
                widgets,
                List.of("refresh_checks", "start_countdown", "abort_launch")
        );
    }

    public RenderedMenuLayout renderTreasureChestMenu(PlayerProgression progression, TreasureInteraction interaction) {
        TreasureChestSurface surface = treasureChestSurface(progression, interaction);
        List<MenuWidget> widgets = new ArrayList<>();
        surface.lootPreview().forEach(loot -> widgets.add(new MenuWidget(
                "loot_" + loot.substring(loot.lastIndexOf('/') + 1),
                "loot_slot",
                loot,
                "loot_preview",
                "claim_reward",
                surface.openable(),
                surface.status()
        )));
        surface.schematicRewards().forEach(schematic -> widgets.add(new MenuWidget(
                "schematic_" + schematic.substring(schematic.lastIndexOf('/') + 1),
                "schematic_slot",
                schematic,
                "schematic_reward",
                "claim_reward",
                surface.openable(),
                surface.status()
        )));
        widgets.add(new MenuWidget("claim_reward", "button", surface.requiredKey(), "reward_claim", "claim_reward", surface.openable(), surface.status()));
        return new RenderedMenuLayout(
                surface.screenId(),
                GalacticCoreIds.id("renderer/treasure_chest_menu"),
                "Dungeon Treasure",
                surface.status(),
                List.of(
                        new MenuRegion("loot", "loot_preview"),
                        new MenuRegion("schematics", "schematic_rewards"),
                        new MenuRegion("actions", "reward_actions")
                ),
                widgets,
                surface.actions()
        );
    }

    public CelestialRouteSurface routeSurface(PlayerProgression progression, String selectedRoute) {
        Objects.requireNonNull(progression, "progression");
        String selected = requireId(selectedRoute);
        List<RouteSurfaceEntry> entries = CELESTIAL_ROUTE_IDS.stream()
                .map(route -> routeSurfaceEntry(progression, route))
                .toList();
        RouteSurfaceEntry selectedEntry = entries.stream()
                .filter(entry -> entry.routeId().equals(selected))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GalacticCore route surface selection " + selectedRoute));
        String status = selectedEntry.unlocked() ? "route_selectable" : "route_locked";
        return new CelestialRouteSurface(selected, selectedEntry.environmentId(), entries, selectedEntry.unlocked(), status);
    }

    public LaunchChecklistSurface launchChecklistSurface(
            RocketLaunchState launchState,
            RouteRequirement route,
            PlayerGearState gear,
            EnvironmentState environment
    ) {
        Objects.requireNonNull(launchState, "launchState");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(gear, "gear");
        Objects.requireNonNull(environment, "environment");
        RocketLaunchDecision launch = prepareLaunch(launchState, route);
        LifeSupportResult lifeSupport = evaluateLifeSupport(gear, environment);
        List<ChecklistEntry> checks = List.of(
                checklistEntry("launch_pad", launchState.padAssembled(), "launch_pad_incomplete"),
                checklistEntry("fuel", launchState.fuelStored() >= launchState.requiredFuel(), "fuel_required"),
                checklistEntry("oxygen_check", launchState.oxygenChecked(), "life_support_check_required"),
                checklistEntry("crew", launchState.crewSeated(), "crew_not_seated"),
                checklistEntry("vehicle_tier", launchState.vehicleTier() >= route.requiredVehicleTier(), "vehicle_tier_required"),
                checklistEntry("route_unlock", route.unlocked(), "route_locked"),
                checklistEntry("environment_life_support", lifeSupport.canBreathe(), lifeSupport.status())
        );
        boolean ready = launch.ready() && lifeSupport.canBreathe();
        String status = ready ? "launch_ready" : checks.stream()
                .filter(check -> !check.passed())
                .findFirst()
                .map(ChecklistEntry::status)
                .orElse(launch.reason());
        return new LaunchChecklistSurface(route.routeId(), environment.id(), ready, status, checks);
    }

    public ScreenInteractionResult interactHoloMapRoute(PlayerProgression progression, String selectedRoute, String actionId) {
        Objects.requireNonNull(progression, "progression");
        String action = requireText(actionId, "actionId");
        CelestialRouteSurface surface = routeSurface(progression, selectedRoute);
        boolean supported = action.equals("select_route") || action.equals("preview_route");
        boolean accepted = supported && (action.equals("preview_route") || surface.selectedUnlocked());
        String status;
        List<String> hostActions;
        if (!supported) {
            status = "unsupported_screen_action";
            hostActions = List.of("sync_screen_error");
        } else if (accepted && action.equals("select_route")) {
            status = "route_selected";
            hostActions = List.of("sync_selected_route", "preview_environment", "open_screencore_launch_checklist");
        } else if (accepted) {
            status = "route_preview_ready";
            hostActions = List.of("preview_environment", "render_hazard_badges");
        } else {
            status = surface.status();
            hostActions = List.of("show_route_requirements", "sync_locked_route_hint");
        }
        return new ScreenInteractionResult(
                GalacticCoreIds.id("screen/holomap_routes"),
                action,
                surface.selectedRoute(),
                surface.selectedEnvironment(),
                accepted,
                status,
                hostActions,
                List.of()
        );
    }

    public ScreenInteractionResult interactLaunchChecklist(
            RocketLaunchState launchState,
            RouteRequirement route,
            PlayerGearState gear,
            EnvironmentState environment,
            String actionId
    ) {
        String action = requireText(actionId, "actionId");
        LaunchChecklistSurface surface = launchChecklistSurface(launchState, route, gear, environment);
        List<String> failedChecks = surface.checks().stream()
                .filter(check -> !check.passed())
                .map(ChecklistEntry::id)
                .toList();
        boolean accepted;
        String status;
        List<String> hostActions;
        switch (action) {
            case "start_countdown" -> {
                accepted = surface.ready();
                status = surface.ready() ? "countdown_armed" : surface.status();
                hostActions = surface.ready()
                        ? List.of("arm_countdown", "lock_launch_controls", "sync_launch_state")
                        : List.of("show_failed_checks", "sync_launch_state");
            }
            case "refresh_checks" -> {
                accepted = true;
                status = surface.status();
                hostActions = List.of("refresh_life_support", "refresh_launch_pad", "sync_checklist");
            }
            case "abort_launch" -> {
                accepted = true;
                status = "launch_aborted";
                hostActions = List.of("clear_countdown", "unlock_launch_controls", "sync_launch_state");
            }
            default -> {
                accepted = false;
                status = "unsupported_screen_action";
                hostActions = List.of("sync_screen_error");
            }
        }
        return new ScreenInteractionResult(
                GalacticCoreIds.id("screen/screencore_launch_checklist"),
                action,
                surface.routeId(),
                surface.environmentId(),
                accepted,
                status,
                hostActions,
                failedChecks
        );
    }

    public TransferPlacementPlan planTransferPlacement(
            PlayerProgression progression,
            RocketLaunchState launchState,
            String destinationRoute,
            PlayerGearState gear,
            TransferAnchor anchor
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(launchState, "launchState");
        Objects.requireNonNull(gear, "gear");
        Objects.requireNonNull(anchor, "anchor");
        RouteRequirement route = routeRequirement(destinationRoute, progression);
        EnvironmentScan scan = scanEnvironment(routeEnvironmentId(route.routeId()));
        EnvironmentState environment = new EnvironmentState(scan.id(), scan.atmosphere(), scan.thermalRisk());
        RocketLaunchDecision launch = prepareLaunch(launchState, route);
        LifeSupportResult lifeSupport = evaluateLifeSupport(gear, environment);
        boolean parachuteRequired = scan.gravity() == Gravity.STANDARD_G && !scan.routeId().equals(GalacticCoreIds.id("route/earth_orbit"));
        boolean parachuteReady = !parachuteRequired || gear.hasParachute();
        boolean padReady = anchor.landingPadPresent() || scan.gravity() == Gravity.ZERO_G || scan.gravity() == Gravity.MICRO_G;
        boolean ready = launch.ready() && lifeSupport.canBreathe() && parachuteReady && padReady;
        String status = transferPlacementStatus(launch, lifeSupport, parachuteReady, padReady);
        LandingPlacement placement = landingPlacement(scan, anchor, parachuteRequired, padReady, ready);
        return new TransferPlacementPlan(
                route.routeId(),
                scan.id(),
                ready,
                status,
                placement,
                transferActions(scan, ready, parachuteRequired)
        );
    }

    public TransferExecutionPlan executeTransfer(
            PlayerProgression progression,
            RocketLaunchState launchState,
            String destinationRoute,
            PlayerGearState gear,
            TransferAnchor anchor
    ) {
        TransferPlacementPlan placementPlan = planTransferPlacement(
                progression,
                launchState,
                destinationRoute,
                gear,
                anchor
        );
        LandingPlacement placement = placementPlan.placement();
        if (!placementPlan.ready()) {
            return new TransferExecutionPlan(
                    placementPlan.routeId(),
                    placementPlan.environmentId(),
                    false,
                    placementPlan.status(),
                    placement.x(),
                    placement.y(),
                    placement.z(),
                    placement.entryMode(),
                    scanEnvironment(placementPlan.environmentId()).gravity(),
                    List.of("cancel_transfer", "sync_launch_state")
            );
        }
        boolean orbital = "orbital_insertion".equals(placement.entryMode());
        List<String> actions = new ArrayList<>();
        actions.add("load_destination_dimension");
        actions.add("ticket_destination_chunk");
        actions.add("place_player_at_destination");
        actions.add(orbital ? "preserve_orbit_velocity" : "dismount_lander");
        if (placement.parachuteRequired()) {
            actions.add("deploy_parachute");
        }
        actions.add("sync_player_progression");
        actions.add("clear_launch_countdown");
        return new TransferExecutionPlan(
                placementPlan.routeId(),
                placementPlan.environmentId(),
                true,
                "transfer_execution_ready",
                placement.x(),
                placement.y(),
                placement.z(),
                placement.entryMode(),
                scanEnvironment(placementPlan.environmentId()).gravity(),
                actions
        );
    }

    public DungeonRewardResult claimDungeonReward(PlayerProgression progression, DungeonRewardClaim claim) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(claim, "claim");
        if (!claim.bossDefeated()) {
            return new DungeonRewardResult(progression, List.of(), List.of(), List.of(), false, "boss_required");
        }
        if (!claim.hasKey()) {
            return new DungeonRewardResult(progression, List.of(), List.of(), List.of(), false, "key_required");
        }
        DungeonRewardTrack track = rewardTrack(claim.dungeonId());
        if (!track.bossId().equals(claim.bossId()) || !track.keyId().equals(claim.keyId())) {
            return new DungeonRewardResult(progression, List.of(), List.of(), List.of(), false, "reward_track_mismatch");
        }
        if (progression.claimedRewards().contains(track.dungeonId())) {
            return new DungeonRewardResult(progression, List.of(), List.of(), List.of(), false, "reward_already_claimed");
        }
        PlayerProgression updated = progression
                .withRoutes(track.unlockedRoutes())
                .withSchematics(track.schematicRewards())
                .withClaimedReward(track.dungeonId());
        return new DungeonRewardResult(
                updated,
                track.unlockedRoutes(),
                track.schematicRewards(),
                List.of(track.lootId()),
                true,
                "reward_claimed"
        );
    }

    public Map<String, Object> evidence() {
        return Map.ofEntries(
                Map.entry("runtimeModels", "machines, energy, oxygen, player_gear, rockets, celestial_routes, environments, dungeon_structures, dungeon_rewards"),
                Map.entry("machineTypes", MachineType.values().length),
                Map.entry("environmentModels", 5),
                Map.entry("dungeonStructurePlans", 3),
                Map.entry("bossEncounterModels", 3),
                Map.entry("bossEntitySpawnModels", 3),
                Map.entry("bossAiProfiles", 3),
                Map.entry("screenSurfaceModels", 2),
                Map.entry("screenInteractionModels", 2),
                Map.entry("treasureScreenModels", 3),
                Map.entry("renderedMenuLayouts", 3),
                Map.entry("transferPlacementModels", 5),
                Map.entry("transferExecutionModels", 5),
                Map.entry("dungeonRewardTracks", 3),
                Map.entry("source", "galacticraft_legacy_runtime_parity"),
                Map.entry("typedReceiptsOnly", true)
        );
    }

    private MachineSnapshot tickOxygenCollector(MachineSnapshot state, MachineInput input) {
        if (state.energy() < 4 || input.leafBlocks() <= 0) {
            return state;
        }
        int produced = Math.min(input.leafBlocks() * 2, MACHINE_OXYGEN_CAPACITY - state.oxygen());
        return state.withEnergy(state.energy() - 4)
                .withOxygen(state.oxygen() + produced)
                .withProgress(state.progress() + (produced > 0 ? 1 : 0));
    }

    private MachineSnapshot tickOxygenSealer(MachineSnapshot state, MachineInput input) {
        int requestedVolume = Math.max(0, input.requestedSealVolume());
        int oxygenCost = requestedVolume * 2;
        if (requestedVolume == 0 || state.energy() < 8 || state.oxygen() < oxygenCost) {
            return state.withSealedVolume(0);
        }
        return state.withEnergy(state.energy() - 8)
                .withOxygen(state.oxygen() - oxygenCost)
                .withSealedVolume(requestedVolume)
                .withProgress(state.progress() + 1);
    }

    private MachineSnapshot tickFuelLoader(MachineSnapshot state, MachineInput input) {
        if (state.energy() < 2 || !state.linkedPad() || !input.rocketPresent()) {
            return state;
        }
        int transferred = Math.min(Math.min(50, input.availableFuel()), MACHINE_FUEL_CAPACITY - state.fuel());
        if (transferred <= 0) {
            return state;
        }
        return state.withEnergy(state.energy() - 2)
                .withFuel(state.fuel() + transferred)
                .withProgress(state.progress() + 1);
    }

    private MachineSnapshot tickRocketWorkbench(MachineSnapshot state, MachineInput input) {
        if (!state.schematicInstalled() || !input.recipeComplete()) {
            return state.withCraftedOutput("");
        }
        return state.withProgress(100).withCraftedOutput(GalacticCoreIds.id("tier_1_rocket"));
    }

    private static boolean thermalProtected(PlayerGearState gear, EnvironmentState environment) {
        return switch (environment.thermalRisk()) {
            case NONE -> true;
            case COLD, EXTREME_HEAT -> gear.hasThermalPadding();
        };
    }

    public enum MachineType {
        OXYGEN_COLLECTOR("oxygen_collector"),
        OXYGEN_SEALER("oxygen_sealer"),
        FUEL_LOADER("fuel_loader"),
        ROCKET_WORKBENCH("rocket_workbench");

        private final String path;

        MachineType(String path) {
            this.path = path;
        }

        public String id() {
            return GalacticCoreIds.id("machine/" + path);
        }
    }

    public enum Atmosphere {
        BREATHABLE,
        VACUUM,
        THIN_CO2,
        HOT_DENSE_ACIDIC
    }

    public enum ThermalRisk {
        NONE,
        COLD,
        EXTREME_HEAT
    }

    public enum Gravity {
        STANDARD_G,
        LOW_G,
        MICRO_G,
        ZERO_G
    }

    public enum BossPhase {
        DORMANT,
        ENGAGED,
        ENRAGED,
        DEFEATED
    }

    public record MachineSnapshot(
            MachineType type,
            int energy,
            int oxygen,
            int fuel,
            int progress,
            int sealedVolume,
            boolean redstoneEnabled,
            boolean linkedPad,
            boolean schematicInstalled,
            String craftedOutput
    ) {
        public MachineSnapshot {
            type = Objects.requireNonNull(type, "type");
            energy = clamp(energy, 0, MACHINE_ENERGY_CAPACITY);
            oxygen = clamp(oxygen, 0, MACHINE_OXYGEN_CAPACITY);
            fuel = clamp(fuel, 0, MACHINE_FUEL_CAPACITY);
            progress = Math.max(0, progress);
            sealedVolume = Math.max(0, sealedVolume);
            craftedOutput = craftedOutput == null ? "" : craftedOutput;
        }

        public MachineSnapshot withEnergy(int value) {
            return new MachineSnapshot(type, value, oxygen, fuel, progress, sealedVolume, redstoneEnabled, linkedPad, schematicInstalled, craftedOutput);
        }

        public MachineSnapshot withOxygen(int value) {
            return new MachineSnapshot(type, energy, value, fuel, progress, sealedVolume, redstoneEnabled, linkedPad, schematicInstalled, craftedOutput);
        }

        public MachineSnapshot withFuel(int value) {
            return new MachineSnapshot(type, energy, oxygen, value, progress, sealedVolume, redstoneEnabled, linkedPad, schematicInstalled, craftedOutput);
        }

        public MachineSnapshot withProgress(int value) {
            return new MachineSnapshot(type, energy, oxygen, fuel, value, sealedVolume, redstoneEnabled, linkedPad, schematicInstalled, craftedOutput);
        }

        public MachineSnapshot withSealedVolume(int value) {
            return new MachineSnapshot(type, energy, oxygen, fuel, progress, value, redstoneEnabled, linkedPad, schematicInstalled, craftedOutput);
        }

        public MachineSnapshot withCraftedOutput(String value) {
            return new MachineSnapshot(type, energy, oxygen, fuel, progress, sealedVolume, redstoneEnabled, linkedPad, schematicInstalled, value);
        }
    }

    public record MachineInput(
            int leafBlocks,
            int requestedSealVolume,
            int availableFuel,
            boolean rocketPresent,
            boolean recipeComplete
    ) {
        public MachineInput {
            leafBlocks = Math.max(0, leafBlocks);
            requestedSealVolume = Math.max(0, requestedSealVolume);
            availableFuel = Math.max(0, availableFuel);
        }
    }

    public record EnergyBuffer(String id, int energy, int capacity) {
        public EnergyBuffer {
            id = requireId(id);
            capacity = Math.max(0, capacity);
            energy = clamp(energy, 0, capacity);
        }
    }

    public record EnergyTransfer(EnergyBuffer source, EnergyBuffer target, int moved) {
        public EnergyTransfer {
            source = Objects.requireNonNull(source, "source");
            target = Objects.requireNonNull(target, "target");
            moved = Math.max(0, moved);
        }
    }

    public record PlayerGearState(
            boolean hasMask,
            boolean hasOxygenGear,
            int oxygenStored,
            boolean hasThermalPadding,
            boolean hasParachute
    ) {
        public PlayerGearState {
            oxygenStored = Math.max(0, oxygenStored);
        }
    }

    public record EnvironmentState(String id, Atmosphere atmosphere, ThermalRisk thermalRisk) {
        public EnvironmentState {
            id = requireId(id);
            atmosphere = Objects.requireNonNull(atmosphere, "atmosphere");
            thermalRisk = Objects.requireNonNull(thermalRisk, "thermalRisk");
        }

        public boolean breathable() {
            return atmosphere == Atmosphere.BREATHABLE;
        }
    }

    public record LifeSupportResult(boolean canBreathe, int oxygenConsumed, boolean thermalProtected, String status) {
        public LifeSupportResult {
            oxygenConsumed = Math.max(0, oxygenConsumed);
            status = status == null || status.isBlank() ? "unknown" : status;
        }
    }

    public record RocketLaunchState(
            int fuelStored,
            int requiredFuel,
            boolean oxygenChecked,
            boolean padAssembled,
            boolean crewSeated,
            int vehicleTier,
            int countdownTicks
    ) {
        public RocketLaunchState {
            fuelStored = Math.max(0, fuelStored);
            requiredFuel = Math.max(0, requiredFuel);
            vehicleTier = Math.max(0, vehicleTier);
            countdownTicks = Math.max(0, countdownTicks);
        }
    }

    public record RouteRequirement(String routeId, int requiredVehicleTier, boolean unlocked) {
        public RouteRequirement {
            routeId = requireId(routeId);
            requiredVehicleTier = Math.max(0, requiredVehicleTier);
        }
    }

    public record RocketLaunchDecision(boolean ready, String reason, int countdownTicks) {
        public RocketLaunchDecision {
            reason = reason == null || reason.isBlank() ? "unknown" : reason;
            countdownTicks = Math.max(0, countdownTicks);
        }

        public static RocketLaunchDecision blocked(String reason) {
            return new RocketLaunchDecision(false, reason, 0);
        }
    }

    public record EnvironmentScan(
            String id,
            String routeId,
            Atmosphere atmosphere,
            ThermalRisk thermalRisk,
            Gravity gravity,
            boolean oxygenRequired,
            boolean thermalProtectionRequired,
            boolean acidHazard,
            String dungeonId
    ) {
        public EnvironmentScan {
            id = requireId(id);
            routeId = requireId(routeId);
            atmosphere = Objects.requireNonNull(atmosphere, "atmosphere");
            thermalRisk = Objects.requireNonNull(thermalRisk, "thermalRisk");
            gravity = Objects.requireNonNull(gravity, "gravity");
            dungeonId = dungeonId == null || dungeonId.isBlank() ? "" : requireId(dungeonId);
        }
    }

    public record PlayerProgression(Set<String> unlockedRoutes, Set<String> schematics, Set<String> claimedRewards) {
        public PlayerProgression {
            unlockedRoutes = normalizeIds(unlockedRoutes);
            schematics = normalizeIds(schematics);
            claimedRewards = normalizeIds(claimedRewards);
        }

        public static PlayerProgression starting() {
            return new PlayerProgression(
                    Set.of(GalacticCoreIds.id("route/earth_orbit"), GalacticCoreIds.id("route/moon")),
                    Set.of(GalacticCoreIds.id("schematic/tier_1_rocket")),
                    Set.of()
            );
        }

        public PlayerProgression withRoutes(List<String> routes) {
            Set<String> next = new LinkedHashSet<>(unlockedRoutes);
            routes.forEach(route -> next.add(requireId(route)));
            return new PlayerProgression(next, schematics, claimedRewards);
        }

        public PlayerProgression withSchematics(List<String> schematicIds) {
            Set<String> next = new LinkedHashSet<>(schematics);
            schematicIds.forEach(schematic -> next.add(requireId(schematic)));
            return new PlayerProgression(unlockedRoutes, next, claimedRewards);
        }

        public PlayerProgression withClaimedReward(String dungeonId) {
            Set<String> next = new LinkedHashSet<>(claimedRewards);
            next.add(requireId(dungeonId));
            return new PlayerProgression(unlockedRoutes, schematics, next);
        }
    }

    public record DungeonRewardClaim(
            String dungeonId,
            String bossId,
            String keyId,
            boolean bossDefeated,
            boolean hasKey
    ) {
        public DungeonRewardClaim {
            dungeonId = requireId(dungeonId);
            bossId = requireId(bossId);
            keyId = requireId(keyId);
        }
    }

    public record DungeonRoomPlan(
            String roomId,
            String role,
            String legacyTemplate,
            boolean sealed,
            boolean treasureLocked,
            boolean bossSpawn
    ) {
        public DungeonRoomPlan {
            roomId = requireId(roomId);
            role = requireText(role, "role");
            legacyTemplate = requireText(legacyTemplate, "legacyTemplate");
        }
    }

    public record DungeonStructurePlan(
            String dungeonId,
            String body,
            int tier,
            String legacySource,
            String bossId,
            String keyId,
            String lootId,
            List<String> schematicRewards,
            List<String> unlockedRoutes,
            List<DungeonRoomPlan> rooms
    ) {
        public DungeonStructurePlan {
            dungeonId = requireId(dungeonId);
            body = requireText(body, "body");
            tier = Math.max(1, tier);
            legacySource = requireText(legacySource, "legacySource");
            bossId = requireId(bossId);
            keyId = requireId(keyId);
            lootId = requireId(lootId);
            schematicRewards = normalizeIdList(schematicRewards);
            unlockedRoutes = normalizeIdList(unlockedRoutes);
            rooms = List.copyOf(rooms == null ? List.of() : rooms);
            if (rooms.stream().noneMatch(DungeonRoomPlan::bossSpawn)) {
                throw new IllegalArgumentException("dungeon structure must include a boss room");
            }
            if (rooms.stream().noneMatch(DungeonRoomPlan::treasureLocked)) {
                throw new IllegalArgumentException("dungeon structure must include a locked treasure room");
            }
        }

        public String bossRoomId() {
            return rooms.stream()
                    .filter(DungeonRoomPlan::bossSpawn)
                    .findFirst()
                    .orElseThrow()
                    .roomId();
        }

        public String treasureRoomId() {
            return rooms.stream()
                    .filter(DungeonRoomPlan::treasureLocked)
                    .findFirst()
                    .orElseThrow()
                    .roomId();
        }
    }

    public record BossEncounterState(
            String dungeonId,
            String bossId,
            int health,
            int maxHealth,
            BossPhase phase,
            boolean defeated,
            boolean keyDropped,
            String status
    ) {
        public BossEncounterState {
            dungeonId = requireId(dungeonId);
            bossId = requireId(bossId);
            maxHealth = Math.max(1, maxHealth);
            health = clamp(health, 0, maxHealth);
            phase = Objects.requireNonNull(phase, "phase");
            status = requireText(status, "status");
        }

        public BossEncounterState withPhase(BossPhase nextPhase) {
            return new BossEncounterState(dungeonId, bossId, health, maxHealth, nextPhase, defeated, keyDropped, status);
        }

        public BossEncounterState withStatus(String nextStatus) {
            return new BossEncounterState(dungeonId, bossId, health, maxHealth, phase, defeated, keyDropped, nextStatus);
        }
    }

    public record BossEntitySpawnPlan(
            boolean ready,
            String status,
            String dungeonId,
            String bossId,
            String bossRoomId,
            String body,
            String legacyEntitySource,
            int maxHealth,
            BossPhase initialPhase,
            int x,
            int y,
            int z,
            List<String> attributes,
            List<String> requiredHostActions
    ) {
        public BossEntitySpawnPlan {
            status = requireText(status, "status");
            dungeonId = requireId(dungeonId);
            bossId = requireId(bossId);
            bossRoomId = requireId(bossRoomId);
            body = requireText(body, "body");
            legacyEntitySource = requireText(legacyEntitySource, "legacyEntitySource");
            maxHealth = Math.max(1, maxHealth);
            initialPhase = Objects.requireNonNull(initialPhase, "initialPhase");
            attributes = List.copyOf(attributes == null ? List.of() : attributes);
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
        }
    }

    public record BossEncounterInput(boolean playerInBossRoom, boolean lifeSupportSafe, int damageDealt) {
        public BossEncounterInput {
            damageDealt = Math.max(0, damageDealt);
        }
    }

    public record BossEncounterResult(
            BossEncounterState state,
            List<String> droppedKeys,
            boolean defeated,
            String status
    ) {
        public BossEncounterResult {
            state = Objects.requireNonNull(state, "state");
            droppedKeys = normalizeIdList(droppedKeys);
            status = requireText(status, "status");
        }
    }

    public record BossAiInput(
            boolean playerInBossRoom,
            boolean roomSealed,
            boolean targetVisible,
            int targetDistance,
            boolean lifeSupportSafe,
            int damageDealt,
            boolean minionWaveActive
    ) {
        public BossAiInput {
            targetDistance = Math.max(0, targetDistance);
            damageDealt = Math.max(0, damageDealt);
        }
    }

    public record BossAiStep(
            BossEncounterState nextState,
            String bossRoomId,
            String movementIntent,
            String attackIntent,
            List<String> requiredHostActions,
            boolean roomLocked,
            String status
    ) {
        public BossAiStep {
            nextState = Objects.requireNonNull(nextState, "nextState");
            bossRoomId = requireId(bossRoomId);
            movementIntent = requireText(movementIntent, "movementIntent");
            attackIntent = requireText(attackIntent, "attackIntent");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
            status = requireText(status, "status");
        }
    }

    public record TreasureInteraction(
            String dungeonId,
            String bossId,
            String keyId,
            boolean bossDefeated,
            boolean hasKey,
            boolean playerInTreasureRoom,
            boolean treasureLocked
    ) {
        public TreasureInteraction {
            dungeonId = requireId(dungeonId);
            bossId = requireId(bossId);
            keyId = requireId(keyId);
        }
    }

    public record TreasureInteractionResult(
            PlayerProgression progression,
            List<String> unlockedRoutes,
            List<String> schematicRewards,
            List<String> loot,
            boolean opened,
            boolean consumedKey,
            String status
    ) {
        public TreasureInteractionResult {
            progression = Objects.requireNonNull(progression, "progression");
            unlockedRoutes = normalizeIdList(unlockedRoutes);
            schematicRewards = normalizeIdList(schematicRewards);
            loot = normalizeIdList(loot);
            status = requireText(status, "status");
        }
    }

    public record TreasureChestSurface(
            String screenId,
            String dungeonId,
            String treasureRoomId,
            boolean locked,
            boolean openable,
            boolean opened,
            boolean consumedKey,
            String status,
            String requiredKey,
            List<String> lootPreview,
            List<String> schematicRewards,
            List<String> unlockedRoutes,
            List<String> actions
    ) {
        public TreasureChestSurface {
            screenId = requireId(screenId);
            dungeonId = requireId(dungeonId);
            treasureRoomId = requireId(treasureRoomId);
            status = requireText(status, "status");
            requiredKey = requireText(requiredKey, "requiredKey");
            lootPreview = normalizeIdList(lootPreview);
            schematicRewards = normalizeIdList(schematicRewards);
            unlockedRoutes = normalizeIdList(unlockedRoutes);
            actions = List.copyOf(actions == null ? List.of() : actions);
        }
    }

    public record RenderedMenuLayout(
            String screenId,
            String rendererId,
            String title,
            String status,
            List<MenuRegion> regions,
            List<MenuWidget> widgets,
            List<String> actions
    ) {
        public RenderedMenuLayout {
            screenId = requireId(screenId);
            rendererId = requireId(rendererId);
            title = requireText(title, "title");
            status = requireText(status, "status");
            regions = List.copyOf(regions == null ? List.of() : regions);
            widgets = List.copyOf(widgets == null ? List.of() : widgets);
            actions = List.copyOf(actions == null ? List.of() : actions);
        }
    }

    public record MenuRegion(String id, String role) {
        public MenuRegion {
            id = requireText(id, "id");
            role = requireText(role, "role");
        }
    }

    public record MenuWidget(
            String id,
            String type,
            String binding,
            String role,
            String actionId,
            boolean enabled,
            String status
    ) {
        public MenuWidget {
            id = requireText(id, "id");
            type = requireText(type, "type");
            binding = requireText(binding, "binding");
            role = requireText(role, "role");
            actionId = requireText(actionId, "actionId");
            status = requireText(status, "status");
        }
    }

    public record CelestialRouteSurface(
            String selectedRoute,
            String selectedEnvironment,
            List<RouteSurfaceEntry> routes,
            boolean selectedUnlocked,
            String status
    ) {
        public CelestialRouteSurface {
            selectedRoute = requireId(selectedRoute);
            selectedEnvironment = requireId(selectedEnvironment);
            routes = List.copyOf(routes == null ? List.of() : routes);
            status = requireText(status, "status");
            String normalizedSelectedRoute = selectedRoute;
            if (routes.stream().noneMatch(route -> route.routeId().equals(normalizedSelectedRoute))) {
                throw new IllegalArgumentException("route surface must include selected route");
            }
        }
    }

    public record RouteSurfaceEntry(
            String routeId,
            String environmentId,
            int requiredVehicleTier,
            boolean unlocked,
            Atmosphere atmosphere,
            ThermalRisk thermalRisk,
            Gravity gravity,
            boolean oxygenRequired,
            boolean thermalProtectionRequired,
            boolean acidHazard,
            String dungeonId
    ) {
        public RouteSurfaceEntry {
            routeId = requireId(routeId);
            environmentId = requireId(environmentId);
            requiredVehicleTier = Math.max(0, requiredVehicleTier);
            atmosphere = Objects.requireNonNull(atmosphere, "atmosphere");
            thermalRisk = Objects.requireNonNull(thermalRisk, "thermalRisk");
            gravity = Objects.requireNonNull(gravity, "gravity");
            dungeonId = dungeonId == null || dungeonId.isBlank() ? "" : requireId(dungeonId);
        }
    }

    public record LaunchChecklistSurface(
            String routeId,
            String environmentId,
            boolean ready,
            String status,
            List<ChecklistEntry> checks
    ) {
        public LaunchChecklistSurface {
            routeId = requireId(routeId);
            environmentId = requireId(environmentId);
            status = requireText(status, "status");
            checks = List.copyOf(checks == null ? List.of() : checks);
        }
    }

    public record ScreenInteractionResult(
            String screenId,
            String actionId,
            String routeId,
            String environmentId,
            boolean accepted,
            String status,
            List<String> requiredHostActions,
            List<String> failedChecks
    ) {
        public ScreenInteractionResult {
            screenId = requireId(screenId);
            actionId = requireText(actionId, "actionId");
            routeId = requireId(routeId);
            environmentId = requireId(environmentId);
            status = requireText(status, "status");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
            failedChecks = List.copyOf(failedChecks == null ? List.of() : failedChecks);
        }
    }

    public record ChecklistEntry(String id, boolean passed, String status) {
        public ChecklistEntry {
            id = requireText(id, "id");
            status = requireText(status, "status");
        }
    }

    public record TransferAnchor(
            String sourceEnvironment,
            int sourceX,
            int sourceY,
            int sourceZ,
            boolean landingPadPresent
    ) {
        public TransferAnchor {
            sourceEnvironment = requireId(sourceEnvironment);
            sourceY = Math.max(-64, sourceY);
        }
    }

    public record LandingPlacement(
            String placementId,
            String environmentId,
            int x,
            int y,
            int z,
            String entryMode,
            boolean parachuteRequired,
            boolean landingPadReady,
            boolean safe
    ) {
        public LandingPlacement {
            placementId = requireId(placementId);
            environmentId = requireId(environmentId);
            entryMode = requireText(entryMode, "entryMode");
        }
    }

    public record TransferPlacementPlan(
            String routeId,
            String environmentId,
            boolean ready,
            String status,
            LandingPlacement placement,
            List<String> requiredHostActions
    ) {
        public TransferPlacementPlan {
            routeId = requireId(routeId);
            environmentId = requireId(environmentId);
            status = requireText(status, "status");
            placement = Objects.requireNonNull(placement, "placement");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
        }
    }

    public record TransferExecutionPlan(
            String routeId,
            String environmentId,
            boolean ready,
            String status,
            int x,
            int y,
            int z,
            String entryMode,
            Gravity gravity,
            List<String> requiredHostActions
    ) {
        public TransferExecutionPlan {
            routeId = requireId(routeId);
            environmentId = requireId(environmentId);
            status = requireText(status, "status");
            entryMode = requireText(entryMode, "entryMode");
            gravity = Objects.requireNonNull(gravity, "gravity");
            requiredHostActions = List.copyOf(requiredHostActions == null ? List.of() : requiredHostActions);
        }
    }

    public record DungeonRewardResult(
            PlayerProgression progression,
            List<String> unlockedRoutes,
            List<String> schematicRewards,
            List<String> loot,
            boolean claimed,
            String status
    ) {
        public DungeonRewardResult {
            progression = Objects.requireNonNull(progression, "progression");
            unlockedRoutes = normalizeIdList(unlockedRoutes);
            schematicRewards = normalizeIdList(schematicRewards);
            loot = normalizeIdList(loot);
            status = status == null || status.isBlank() ? "unknown" : status;
        }
    }

    private static DungeonStructurePlan dungeonStructure(
            String dungeonId,
            String body,
            int tier,
            String legacySource,
            DungeonRewardTrack track,
            List<DungeonRoomPlan> rooms
    ) {
        return new DungeonStructurePlan(
                dungeonId,
                body,
                tier,
                legacySource,
                track.bossId(),
                track.keyId(),
                track.lootId(),
                track.schematicRewards(),
                track.unlockedRoutes(),
                rooms
        );
    }

    private static BossPhase bossPhase(int health, int maxHealth) {
        return health * 3 <= maxHealth ? BossPhase.ENRAGED : BossPhase.ENGAGED;
    }

    private static String legacyBossEntitySource(String bossId) {
        return switch (requireId(bossId)) {
            case "echogalacticcore:boss/evolved_skeleton_boss" -> "micdoodle8.mods.galacticraft.core.entities.EntitySkeletonBoss";
            case "echogalacticcore:boss/evolved_creeper_boss" -> "micdoodle8.mods.galacticraft.planets.mars.entities.EntityCreeperBoss";
            case "echogalacticcore:boss/spider_queen" -> "micdoodle8.mods.galacticraft.planets.venus.entities.EntitySpiderQueen";
            default -> throw new IllegalArgumentException("Unknown GalacticCore boss entity source " + bossId);
        };
    }

    private static BossProfile bossProfile(String bossId) {
        return switch (requireId(bossId)) {
            case "echogalacticcore:boss/evolved_skeleton_boss" -> new BossProfile(
                    6,
                    "bone_slam",
                    "bone_volley",
                    "summon_evolved_skeletons"
            );
            case "echogalacticcore:boss/evolved_creeper_boss" -> new BossProfile(
                    5,
                    "charged_leap",
                    "shockwave_warning",
                    "summon_evolved_creepers"
            );
            case "echogalacticcore:boss/spider_queen" -> new BossProfile(
                    8,
                    "web_lunge",
                    "acid_web_burst",
                    "summon_cave_spiders"
            );
            default -> throw new IllegalArgumentException("Unknown GalacticCore boss profile " + bossId);
        };
    }

    private RouteSurfaceEntry routeSurfaceEntry(PlayerProgression progression, String routeId) {
        RouteRequirement route = routeRequirement(routeId, progression);
        EnvironmentScan scan = scanEnvironment(routeEnvironmentId(route.routeId()));
        return new RouteSurfaceEntry(
                route.routeId(),
                scan.id(),
                route.requiredVehicleTier(),
                route.unlocked(),
                scan.atmosphere(),
                scan.thermalRisk(),
                scan.gravity(),
                scan.oxygenRequired(),
                scan.thermalProtectionRequired(),
                scan.acidHazard(),
                scan.dungeonId()
        );
    }

    private static ChecklistEntry checklistEntry(String id, boolean passed, String failedStatus) {
        return new ChecklistEntry(id, passed, passed ? "ready" : failedStatus);
    }

    private static String routeEnvironmentId(String routeId) {
        return switch (requireId(routeId)) {
            case "echogalacticcore:route/earth_orbit" -> GalacticCoreIds.id("earth_orbit");
            case "echogalacticcore:route/moon" -> GalacticCoreIds.id("moon");
            case "echogalacticcore:route/mars" -> GalacticCoreIds.id("mars");
            case "echogalacticcore:route/asteroids" -> GalacticCoreIds.id("asteroids");
            case "echogalacticcore:route/venus" -> GalacticCoreIds.id("venus");
            default -> throw new IllegalArgumentException("Unknown GalacticCore route environment " + routeId);
        };
    }

    private static LandingPlacement landingPlacement(
            EnvironmentScan scan,
            TransferAnchor anchor,
            boolean parachuteRequired,
            boolean landingPadReady,
            boolean safe
    ) {
        String body = scan.id().substring(scan.id().indexOf(':') + 1);
        int y = switch (scan.gravity()) {
            case ZERO_G -> 192;
            case MICRO_G -> 160;
            case LOW_G -> 96;
            case STANDARD_G -> scan.acidHazard() ? 128 : 96;
        };
        String entryMode = switch (scan.gravity()) {
            case ZERO_G, MICRO_G -> "orbital_insertion";
            case LOW_G -> "lander_descent";
            case STANDARD_G -> parachuteRequired ? "parachute_descent" : "powered_descent";
        };
        return new LandingPlacement(
                GalacticCoreIds.id("transfer_placement/" + body),
                scan.id(),
                anchor.sourceX(),
                y,
                anchor.sourceZ(),
                entryMode,
                parachuteRequired,
                landingPadReady,
                safe
        );
    }

    private static String transferPlacementStatus(
            RocketLaunchDecision launch,
            LifeSupportResult lifeSupport,
            boolean parachuteReady,
            boolean landingPadReady
    ) {
        if (!launch.ready()) {
            return launch.reason();
        }
        if (!lifeSupport.canBreathe()) {
            return lifeSupport.status();
        }
        if (!parachuteReady) {
            return "parachute_required";
        }
        if (!landingPadReady) {
            return "landing_pad_target_required";
        }
        return "transfer_placement_ready";
    }

    private static List<String> transferActions(EnvironmentScan scan, boolean ready, boolean parachuteRequired) {
        if (!ready) {
            return List.of("block_transfer");
        }
        List<String> actions = new ArrayList<>();
        actions.add("load_dimension");
        actions.add("prepare_landing_zone");
        actions.add(scan.gravity() == Gravity.ZERO_G || scan.gravity() == Gravity.MICRO_G ? "place_orbiting_player" : "place_landed_player");
        if (parachuteRequired) {
            actions.add("deploy_parachute");
        }
        actions.add("sync_player_progression");
        return List.copyOf(actions);
    }

    private static DungeonRoomPlan room(
            String body,
            String role,
            String legacyTemplate,
            boolean sealed,
            boolean treasureLocked,
            boolean bossSpawn
    ) {
        return new DungeonRoomPlan(
                GalacticCoreIds.id("dungeon_room/" + body + "/" + role),
                role,
                legacyTemplate,
                sealed,
                treasureLocked,
                bossSpawn
        );
    }

    private record DungeonRewardTrack(
            String dungeonId,
            String bossId,
            String keyId,
            String lootId,
            List<String> schematicRewards,
            List<String> unlockedRoutes
    ) {
        private DungeonRewardTrack {
            dungeonId = requireId(dungeonId);
            bossId = requireId(bossId);
            keyId = requireId(keyId);
            lootId = requireId(lootId);
            schematicRewards = normalizeIdList(schematicRewards);
            unlockedRoutes = normalizeIdList(unlockedRoutes);
        }
    }

    private record BossProfile(int preferredRange, String primaryAttack, String rangedAttack, String summonAttack) {
        private BossProfile {
            preferredRange = Math.max(1, preferredRange);
            primaryAttack = requireText(primaryAttack, "primaryAttack");
            rangedAttack = requireText(rangedAttack, "rangedAttack");
            summonAttack = requireText(summonAttack, "summonAttack");
        }
    }

    private static DungeonRewardTrack rewardTrack(String dungeonId) {
        String id = requireId(dungeonId);
        return switch (id) {
            case "echogalacticcore:dungeon/moon_dungeon_tier_1" -> new DungeonRewardTrack(
                    id,
                    GalacticCoreIds.id("boss/evolved_skeleton_boss"),
                    "tier_1_key",
                    GalacticCoreIds.id("loot/moon_dungeon_tier_1"),
                    List.of(GalacticCoreIds.id("schematic/moon_buggy"), GalacticCoreIds.id("schematic/tier_2_rocket")),
                    List.of(GalacticCoreIds.id("route/mars"))
            );
            case "echogalacticcore:dungeon/mars_dungeon_tier_2" -> new DungeonRewardTrack(
                    id,
                    GalacticCoreIds.id("boss/evolved_creeper_boss"),
                    "tier_2_key",
                    GalacticCoreIds.id("loot/mars_dungeon_tier_2"),
                    List.of(GalacticCoreIds.id("schematic/cargo_rocket"), GalacticCoreIds.id("schematic/tier_3_rocket")),
                    List.of(GalacticCoreIds.id("route/asteroids"), GalacticCoreIds.id("route/venus"))
            );
            case "echogalacticcore:dungeon/venus_dungeon_tier_3" -> new DungeonRewardTrack(
                    id,
                    GalacticCoreIds.id("boss/spider_queen"),
                    "tier_3_key",
                    GalacticCoreIds.id("loot/venus_dungeon_tier_3"),
                    List.of(GalacticCoreIds.id("schematic/astro_miner")),
                    List.of(GalacticCoreIds.id("route/asteroids"))
            );
            default -> throw new IllegalArgumentException("Unknown GalacticCore dungeon reward track " + dungeonId);
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return id.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static Set<String> normalizeIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        ids.forEach(id -> normalized.add(requireId(id)));
        return Set.copyOf(normalized);
    }

    private static List<String> normalizeIdList(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        ids.forEach(id -> normalized.add(requireId(id)));
        return List.copyOf(normalized);
    }
}
