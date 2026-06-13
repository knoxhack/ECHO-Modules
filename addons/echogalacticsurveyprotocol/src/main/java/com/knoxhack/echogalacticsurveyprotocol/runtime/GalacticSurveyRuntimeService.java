package com.knoxhack.echogalacticsurveyprotocol.runtime;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

public final class GalacticSurveyRuntimeService {
    public enum ScanConfidence {
        UNKNOWN(0, "unknown"),
        TRACE(1, "trace"),
        PARTIAL(2, "partial"),
        CONFIRMED(3, "confirmed"),
        RESOLVED(4, "resolved");

        private final int rank;
        private final String id;

        ScanConfidence(int rank, String id) {
            this.rank = rank;
            this.id = id;
        }

        public int rank() {
            return rank;
        }

        public String id() {
            return id;
        }

        public boolean meets(ScanConfidence required) {
            return rank >= required.rank;
        }

        public static ScanConfidence fromId(String id) {
            for (ScanConfidence confidence : values()) {
                if (confidence.id.equals(id)) {
                    return confidence;
                }
            }
            return UNKNOWN;
        }

        public ScanConfidence boosted(int steps) {
            int nextRank = Math.min(RESOLVED.rank, rank + Math.max(0, steps));
            for (ScanConfidence confidence : values()) {
                if (confidence.rank == nextRank) {
                    return confidence;
                }
            }
            return this;
        }
    }

    public enum RouteMargin {
        SAFE("safe"),
        MARGINAL("marginal"),
        UNSAFE_WITHOUT_DEPOT("unsafe_without_depot"),
        BLOCKED("blocked");

        private final String id;

        RouteMargin(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static RouteMargin fromId(String id) {
            for (RouteMargin margin : values()) {
                if (margin.id.equals(id)) {
                    return margin;
                }
            }
            return BLOCKED;
        }
    }

    public enum SurveyNetworkState {
        OFFLINE("offline"),
        DEGRADED("degraded"),
        ONLINE("online");

        private final String id;

        SurveyNetworkState(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record ProbeLaunchRequest(
            String probeId,
            String targetSectorId,
            int availableLauncherPower,
            boolean surveyNetworkOnline,
            Collection<String> inventoryItemIds,
            Collection<String> completedProofs
    ) {
        public ProbeLaunchRequest {
            inventoryItemIds = orderedList(inventoryItemIds);
            completedProofs = orderedList(completedProofs);
        }
    }

    public record ProbeState(
            String probeId,
            String targetSectorId,
            ScanConfidence confidence,
            int rangeSectors,
            boolean recoverable,
            boolean lost,
            List<String> revealedDiscoveries,
            List<String> revealedLayers
    ) {
        public ProbeState {
            revealedDiscoveries = orderedList(revealedDiscoveries);
            revealedLayers = orderedList(revealedLayers);
        }
    }

    public record ProbeLaunchResult(
            boolean launched,
            String reason,
            ProbeState probeState,
            List<String> completedProofs,
            List<String> revealedLayers,
            List<String> revealedSignals
    ) {
        public ProbeLaunchResult {
            completedProofs = orderedList(completedProofs);
            revealedLayers = orderedList(revealedLayers);
            revealedSignals = orderedList(revealedSignals);
        }
    }

    public record CatalogState(
            List<String> discoveryIds,
            List<String> rewardProofs,
            List<String> certificationProofs,
            List<String> unlockedLayers,
            List<String> missionProofs
    ) {
        public CatalogState {
            discoveryIds = orderedList(discoveryIds);
            rewardProofs = orderedList(rewardProofs);
            certificationProofs = orderedList(certificationProofs);
            unlockedLayers = orderedList(unlockedLayers);
            missionProofs = orderedList(missionProofs);
        }

        public List<String> completedProofs() {
            SequencedSet<String> proofs = new LinkedHashSet<>();
            discoveryIds.forEach(id -> proofs.add("discovery:" + id));
            proofs.addAll(rewardProofs);
            proofs.addAll(certificationProofs);
            unlockedLayers.forEach(layer -> proofs.add("holomap_layer:" + layer));
            missionProofs.forEach(mission -> proofs.add("mission:" + mission));
            return List.copyOf(proofs);
        }
    }

    public record CatalogResult(
            boolean accepted,
            String reason,
            String discoveryId,
            String rewardProof,
            CatalogState catalogState
    ) {
    }

    public record RoutePlanRequest(
            String routeId,
            int fuelCanisters,
            double fuelQuality,
            Collection<String> completedProofs,
            Collection<String> inventoryItemIds,
            Collection<String> activeDepotIds,
            int cargoMass,
            boolean returnRequired
    ) {
        public RoutePlanRequest {
            completedProofs = orderedList(completedProofs);
            inventoryItemIds = orderedList(inventoryItemIds);
            activeDepotIds = orderedList(activeDepotIds);
        }
    }

    public record RoutePlanResult(
            boolean travelReady,
            String reason,
            String routeId,
            RouteMargin margin,
            int requiredFuel,
            int effectiveFuel,
            int returnReserve,
            List<String> requiredProofs,
            List<String> unlockedProofs
    ) {
        public RoutePlanResult {
            requiredProofs = orderedList(requiredProofs);
            unlockedProofs = orderedList(unlockedProofs);
        }
    }

    public record SalvageAttempt(
            String siteId,
            Collection<String> completedProofs,
            Collection<String> inventoryItemIds,
            Collection<String> revealedLayers,
            RouteMargin routeMargin
    ) {
        public SalvageAttempt {
            completedProofs = orderedList(completedProofs);
            inventoryItemIds = orderedList(inventoryItemIds);
            revealedLayers = orderedList(revealedLayers);
            if (routeMargin == null) {
                routeMargin = RouteMargin.SAFE;
            }
        }
    }

    public record SalvageResult(
            boolean recovered,
            String reason,
            String siteId,
            int hazardTier,
            List<String> hazards,
            List<String> rewardProofs,
            List<String> completedProofs
    ) {
        public SalvageResult {
            hazards = orderedList(hazards);
            rewardProofs = orderedList(rewardProofs);
            completedProofs = orderedList(completedProofs);
        }
    }

    public record DepotBuildRequest(
            String depotId,
            int fuelStored,
            Collection<String> completedProofs,
            Collection<String> inventoryItemIds
    ) {
        public DepotBuildRequest {
            completedProofs = orderedList(completedProofs);
            inventoryItemIds = orderedList(inventoryItemIds);
        }
    }

    public record DepotState(
            String id,
            String sectorId,
            int fuelStored,
            int capacity,
            String unlockProof
    ) {
    }

    public record DepotEstablishmentResult(
            boolean established,
            String reason,
            DepotState depot,
            List<String> completedProofs
    ) {
        public DepotEstablishmentResult {
            completedProofs = orderedList(completedProofs);
        }
    }

    public record OutpostState(
            String id,
            SurveyNetworkState networkState,
            int relayIntegrity,
            int storedPower,
            boolean terminalOnline,
            boolean launcherCharged,
            boolean signalDishAligned,
            List<String> availableTerminalPages,
            List<String> completedProofs
    ) {
        public OutpostState {
            networkState = networkState == null ? SurveyNetworkState.OFFLINE : networkState;
            relayIntegrity = clamp(relayIntegrity, 0, 100);
            storedPower = Math.max(0, storedPower);
            availableTerminalPages = orderedList(availableTerminalPages);
            completedProofs = orderedList(completedProofs);
        }

        public boolean surveyNetworkOnline() {
            return networkState == SurveyNetworkState.ONLINE && terminalOnline && signalDishAligned;
        }

        public int launcherPowerForProbe() {
            return launcherCharged ? Math.min(100, storedPower) : 0;
        }
    }

    public record OutpostRepairRequest(
            OutpostState outpost,
            int relayParts,
            int powerCells,
            boolean terminalBooted,
            boolean signalDishAligned
    ) {
    }

    public record OutpostRepairResult(
            boolean repaired,
            String reason,
            OutpostState outpost,
            List<String> completedProofs
    ) {
        public OutpostRepairResult {
            completedProofs = orderedList(completedProofs);
        }
    }

    public record HoloMapMarker(
            String id,
            String sectorId,
            String kind,
            ScanConfidence confidence,
            String routeRisk,
            String valueSignal,
            List<String> visibleLayers,
            List<String> requiredProofs
    ) {
        public HoloMapMarker {
            confidence = confidence == null ? ScanConfidence.UNKNOWN : confidence;
            visibleLayers = orderedList(visibleLayers);
            requiredProofs = orderedList(requiredProofs);
        }
    }

    public record HoloMapPlan(
            String schema,
            List<String> activeLayers,
            List<HoloMapMarker> markers,
            List<String> visibleRoutes,
            List<String> warnings,
            List<String> nextActions
    ) {
        public HoloMapPlan {
            activeLayers = orderedList(activeLayers);
            markers = List.copyOf(markers == null ? List.of() : markers);
            visibleRoutes = orderedList(visibleRoutes);
            warnings = orderedList(warnings);
            nextActions = orderedList(nextActions);
        }
    }

    public record SurveySaveSnapshot(
            String schema,
            String mode,
            OutpostState outpost,
            List<ProbeState> probes,
            CatalogState catalog,
            List<RoutePlanResult> routes,
            List<DepotState> depots,
            List<SalvageResult> salvage,
            List<String> completedProofs,
            List<String> activeHoloMapLayers
    ) {
        public SurveySaveSnapshot {
            outpost = outpost == null ? startingOutpost() : outpost;
            probes = List.copyOf(probes == null ? List.of() : probes);
            catalog = catalog == null ? emptyCatalog() : catalog;
            routes = List.copyOf(routes == null ? List.of() : routes);
            depots = List.copyOf(depots == null ? List.of() : depots);
            salvage = List.copyOf(salvage == null ? List.of() : salvage);
            completedProofs = orderedList(completedProofs);
            activeHoloMapLayers = orderedList(activeHoloMapLayers);
        }
    }

    public record SurveyArrayRequirementStatus(
            String id,
            String proof,
            boolean satisfied,
            String evidenceSource
    ) {
    }

    public record SurveyArrayRestorationResult(
            boolean restored,
            String reason,
            List<SurveyArrayRequirementStatus> requirements,
            List<String> completedProofs,
            List<String> rewards
    ) {
        public SurveyArrayRestorationResult {
            requirements = List.copyOf(requirements == null ? List.of() : requirements);
            completedProofs = orderedList(completedProofs);
            rewards = orderedList(rewards);
        }
    }

    public record ReleaseGateStatus(
            String id,
            String proof,
            boolean required,
            boolean satisfied,
            String evidenceSource
    ) {
    }

    public record PublicAlphaReadinessReport(
            boolean publicAlphaAllowed,
            String reason,
            List<ReleaseGateStatus> gates,
            List<String> blockers
    ) {
        public PublicAlphaReadinessReport {
            gates = List.copyOf(gates == null ? List.of() : gates);
            blockers = orderedList(blockers);
        }
    }

    private record RouteDefinition(
            String id,
            String origin,
            String destination,
            int fuelCost,
            RouteMargin declaredMargin,
            String requiredProof,
            String unlockProof
    ) {
    }

    private record SalvageSiteDefinition(
            String id,
            String sectorId,
            int hazardTier,
            List<String> hazards,
            String lootTable,
            String requiredPreparation
    ) {
        private SalvageSiteDefinition {
            hazards = orderedList(hazards);
        }
    }

    private record DepotDefinition(String id, String sectorId, int capacity, String unlockProof) {
    }

    private record SurveyArrayRequirementDefinition(String id, String proof) {
    }

    private record ReleaseGateDefinition(String id, boolean required, String proof) {
    }

    private static final Map<String, String> DISCOVERY_CATEGORIES = Map.of(
            "barren_moon_kg_01a", "moon",
            "ice_body_kg_01b", "body",
            "planet_candidate_ks_02", "planet",
            "signal_anomaly_veil_trace", "anomaly",
            "debris_belt_cinder_ring", "debris_belt",
            "derelict_relay_osprey", "derelict",
            "lost_survey_craft_lysander", "lost_survey_craft",
            "unstable_orbital_platform_ariadne", "unstable_platform",
            "deep_sector_beacon_ks_04", "deep_sector_beacon"
    );

    private static final Map<String, Integer> SECTOR_RANGE_REQUIREMENTS = Map.of(
            "near_sector_01", 1,
            "outer_sector_02", 2,
            "derelict_corridor_03", 3,
            "deep_sector_04", 3
    );

    private static final Map<String, List<String>> DISCOVERIES_BY_SECTOR = Map.of(
            "near_sector_01", List.of("barren_moon_kg_01a", "signal_anomaly_veil_trace"),
            "outer_sector_02", List.of("ice_body_kg_01b", "planet_candidate_ks_02", "debris_belt_cinder_ring", "derelict_relay_osprey"),
            "derelict_corridor_03", List.of("lost_survey_craft_lysander", "unstable_orbital_platform_ariadne"),
            "deep_sector_04", List.of("deep_sector_beacon_ks_04")
    );

    private static final Map<String, ScanConfidence> DISCOVERY_CONFIDENCE_REQUIREMENTS = Map.of(
            "barren_moon_kg_01a", ScanConfidence.PARTIAL,
            "ice_body_kg_01b", ScanConfidence.CONFIRMED,
            "planet_candidate_ks_02", ScanConfidence.PARTIAL,
            "signal_anomaly_veil_trace", ScanConfidence.TRACE,
            "debris_belt_cinder_ring", ScanConfidence.PARTIAL,
            "derelict_relay_osprey", ScanConfidence.CONFIRMED,
            "lost_survey_craft_lysander", ScanConfidence.CONFIRMED,
            "unstable_orbital_platform_ariadne", ScanConfidence.RESOLVED,
            "deep_sector_beacon_ks_04", ScanConfidence.RESOLVED
    );

    private static final Map<String, String> DISCOVERY_REWARDS = Map.of(
            "barren_moon_kg_01a", "item:catalog_badge",
            "ice_body_kg_01b", "holomap_layer:fuel_range",
            "planet_candidate_ks_02", "mission:catalog_local_bodies",
            "signal_anomaly_veil_trace", "item:stellar_chart_fragment",
            "debris_belt_cinder_ring", "item:orbital_scrap",
            "derelict_relay_osprey", "item:long_range_probe",
            "lost_survey_craft_lysander", "item:deep_space_lens",
            "unstable_orbital_platform_ariadne", "item:survey_array_key",
            "deep_sector_beacon_ks_04", "item:galactic_survey_badge"
    );

    private static final Map<String, String> DISCOVERY_PROOFS = Map.of(
            "barren_moon_kg_01a", "index:barren_moon_kg_01a",
            "ice_body_kg_01b", "index:ice_body_kg_01b",
            "planet_candidate_ks_02", "index:planet_candidate_ks_02",
            "signal_anomaly_veil_trace", "lens_scan:fallen_orbital_fragment",
            "debris_belt_cinder_ring", "holomap_layer:orbital_layers",
            "derelict_relay_osprey", "salvage:derelict_relay_osprey",
            "lost_survey_craft_lysander", "salvage:lost_survey_craft_lysander",
            "unstable_orbital_platform_ariadne", "salvage:unstable_orbital_platform_ariadne",
            "deep_sector_beacon_ks_04", "route:deep_sector_beacon_route"
    );

    private static final Map<String, RouteDefinition> ROUTES = mapRoutes(List.of(
            new RouteDefinition("outpost_to_near_sector_01", "quiet_survey_outpost", "near_sector_01", 1, RouteMargin.SAFE, "item:fuel_canister", "mission:first_survey_hop"),
            new RouteDefinition("near_sector_01_survey_hop", "quiet_survey_outpost", "near_sector_01", 2, RouteMargin.SAFE, "item:navigation_core", "mission:first_survey_circuit"),
            new RouteDefinition("cinder_ring_depot_route", "near_sector_01", "outer_sector_02", 3, RouteMargin.MARGINAL, "depot:cinder_ring_remote_depot", "holomap_layer:depot_coverage"),
            new RouteDefinition("deep_sector_beacon_route", "derelict_corridor_03", "deep_sector_04", 5, RouteMargin.UNSAFE_WITHOUT_DEPOT, "item:route_stabilizer", "discovery:deep_sector_beacon_ks_04")
    ));

    private static final Map<String, List<String>> SALVAGE_LOOT = Map.of(
            "starter_fragment_salvage", List.of("item:burned_navigation_core", "item:orbital_scrap", "item:fuel_quality_sample"),
            "derelict_relay_loot", List.of("item:navigation_core", "item:long_range_probe", "item:stellar_chart_fragment", "item:orbital_scrap"),
            "radiation_wreck_loot", List.of("item:radiation_shielding", "item:route_stabilizer", "item:deep_space_lens", "item:survey_array_key")
    );

    private static final Map<String, SalvageSiteDefinition> SALVAGE_SITES = mapSalvageSites(List.of(
            new SalvageSiteDefinition("fallen_orbital_fragment", "near_sector_01", 0, List.of("signal_interference"), "starter_fragment_salvage", "lens_scan:fallen_orbital_fragment"),
            new SalvageSiteDefinition("debris_belt_cinder_ring", "outer_sector_02", 1, List.of("unstable_orbit", "hull_hazards"), "derelict_relay_loot", "item:navigation_core"),
            new SalvageSiteDefinition("derelict_relay_osprey", "outer_sector_02", 1, List.of("signal_interference", "power_surge"), "derelict_relay_loot", "holomap_layer:derelict_beacons"),
            new SalvageSiteDefinition("lost_survey_craft_lysander", "derelict_corridor_03", 2, List.of("radiation", "navigation_drift"), "radiation_wreck_loot", "item:radiation_shielding"),
            new SalvageSiteDefinition("unstable_orbital_platform_ariadne", "derelict_corridor_03", 3, List.of("unstable_orbit", "radiation", "signal_interference"), "radiation_wreck_loot", "item:route_stabilizer")
    ));

    private static final Map<String, DepotDefinition> DEPOTS = mapDepots(List.of(
            new DepotDefinition("outpost_fuel_cache", "near_sector_01", 4, "route:outpost_to_near_sector_01"),
            new DepotDefinition("cinder_ring_remote_depot", "outer_sector_02", 8, "route:cinder_ring_depot_route"),
            new DepotDefinition("derelict_corridor_recovery_cache", "derelict_corridor_03", 6, "route:deep_sector_beacon_route")
    ));

    private static final List<SurveyArrayRequirementDefinition> SURVEY_ARRAY_REQUIREMENTS = List.of(
            new SurveyArrayRequirementDefinition("survey_array_console", "block:survey_array_console"),
            new SurveyArrayRequirementDefinition("complete_sector_atlas", "catalog:complete_sector_atlas"),
            new SurveyArrayRequirementDefinition("deep_sector_beacon", "discovery:deep_sector_beacon_ks_04"),
            new SurveyArrayRequirementDefinition("remote_depot_network", "depot:cinder_ring_remote_depot"),
            new SurveyArrayRequirementDefinition("advanced_probe_network", "item:long_range_probe"),
            new SurveyArrayRequirementDefinition("array_key_recovered", "item:survey_array_key")
    );

    private static final List<ReleaseGateDefinition> RELEASE_GATES = List.of(
            new ReleaseGateDefinition("probe_launch_works", true, "probe:starter_probe"),
            new ReleaseGateDefinition("holomap_reveals_meaningful_data", true, "holomap_layer:scan_cones"),
            new ReleaseGateDefinition("catalog_entries_unlock_from_discoveries", true, "discovery:barren_moon_kg_01a"),
            new ReleaseGateDefinition("fuel_route_limits_understandable", true, "route:near_sector_01_survey_hop"),
            new ReleaseGateDefinition("one_salvage_site_playable", true, "salvage:derelict_relay_osprey"),
            new ReleaseGateDefinition("one_probe_upgrade_matters", true, "item:long_range_probe"),
            new ReleaseGateDefinition("first_2_hour_loop_no_dead_end", true, "mission:first_survey_circuit"),
            new ReleaseGateDefinition("real_first_30_playthrough", true, "manual:real_first_30_playthrough"),
            new ReleaseGateDefinition("real_first_2_hour_playthrough", true, "manual:real_first_2_hour_playthrough"),
            new ReleaseGateDefinition("real_survey_array_playthrough", true, "manual:real_survey_array_playthrough"),
            new ReleaseGateDefinition("fresh_world_created", true, "manual:fresh_world_created"),
            new ReleaseGateDefinition("save_reload_verified", true, "manual:save_reload_verified"),
            new ReleaseGateDefinition("no_crash_evidence", true, "manual:no_crash_evidence"),
            new ReleaseGateDefinition("launcher_install_update_repair_rollback", true, "launcher:install_update_repair_rollback")
    );

    public static OutpostState startingOutpost() {
        return new OutpostState(
                "quiet_survey_outpost",
                SurveyNetworkState.OFFLINE,
                15,
                10,
                true,
                false,
                false,
                List.of("survey_network"),
                List.of("block_seen:survey_terminal", "terminal_page:survey_network")
        );
    }

    public OutpostRepairResult repairOutpostRelay(OutpostRepairRequest request) {
        OutpostState current = request.outpost() == null ? startingOutpost() : request.outpost();
        if (!current.terminalOnline() && !request.terminalBooted()) {
            return new OutpostRepairResult(false, "terminal_boot_required", current, current.completedProofs());
        }
        if (request.relayParts() < 2) {
            return new OutpostRepairResult(false, "relay_parts_missing", current, current.completedProofs());
        }
        if (request.powerCells() < 1) {
            return new OutpostRepairResult(false, "power_cell_required", current, current.completedProofs());
        }

        int storedPower = Math.min(100, current.storedPower() + 50 + request.powerCells() * 15);
        boolean launcherCharged = storedPower >= 25;
        SurveyNetworkState nextNetworkState = request.signalDishAligned()
                ? SurveyNetworkState.ONLINE
                : SurveyNetworkState.DEGRADED;

        SequencedSet<String> pages = new LinkedHashSet<>(current.availableTerminalPages());
        pages.addAll(List.of("probe_control", "route_planner", "salvage_log"));

        SequencedSet<String> proofs = new LinkedHashSet<>(current.completedProofs());
        proofs.add("power:small_relay_online");
        proofs.add("block:probe_launcher");
        proofs.add("holomap_layer:sector_grid");
        if (launcherCharged) {
            proofs.add("power:probe_launcher_charged");
        }
        for (String page : pages) {
            proofs.add("terminal_page:" + page);
        }

        OutpostState next = new OutpostState(
                current.id(),
                nextNetworkState,
                100,
                storedPower,
                true,
                launcherCharged,
                request.signalDishAligned(),
                List.copyOf(pages),
                List.copyOf(proofs)
        );
        return new OutpostRepairResult(next.surveyNetworkOnline(), nextNetworkState.id(), next, next.completedProofs());
    }

    public ProbeLaunchResult launchProbe(ProbeLaunchRequest request) {
        Optional<GalacticSurveyProbeRuntime.ProbeProfile> profile = GalacticSurveyProbeRuntime.probeFor(request.probeId());
        if (profile.isEmpty()) {
            return failedProbe("unknown_probe", request.probeId(), request.targetSectorId());
        }
        if (!request.surveyNetworkOnline()) {
            return failedProbe("survey_network_offline", request.probeId(), request.targetSectorId());
        }
        if (request.availableLauncherPower() < 25) {
            return failedProbe("launcher_power_too_low", request.probeId(), request.targetSectorId());
        }
        if (!SECTOR_RANGE_REQUIREMENTS.containsKey(request.targetSectorId())) {
            return failedProbe("unknown_sector", request.probeId(), request.targetSectorId());
        }

        GalacticSurveyProbeRuntime.ProbeProfile probe = profile.get();
        if (!proofSatisfied(probe.unlockProof(), request.completedProofs(), request.inventoryItemIds(), List.of(), List.of())) {
            return failedProbe("probe_unlock_missing", request.probeId(), request.targetSectorId());
        }

        int requiredRange = SECTOR_RANGE_REQUIREMENTS.get(request.targetSectorId());
        if (probe.rangeSectors() < requiredRange) {
            return failedProbe("probe_range_too_short", request.probeId(), request.targetSectorId());
        }

        int boosts = 0;
        if ("near_sector_01".equals(request.targetSectorId())) {
            boosts += 1;
        }
        if (request.availableLauncherPower() >= 80) {
            boosts += 1;
        }
        ScanConfidence confidence = ScanConfidence.fromId(probe.scanTier()).boosted(boosts);
        List<String> layers = layersForProbeResult(confidence, request.targetSectorId());
        List<String> signals = visibleDiscoveries(request.targetSectorId(), confidence);

        SequencedSet<String> proofs = new LinkedHashSet<>();
        proofs.add("probe:" + probe.id());
        proofs.add("sector:" + request.targetSectorId());
        layers.forEach(layer -> proofs.add("holomap_layer:" + layer));

        ProbeState state = new ProbeState(
                probe.id(),
                request.targetSectorId(),
                confidence,
                probe.rangeSectors(),
                requiredRange <= 2,
                false,
                signals,
                layers
        );
        return new ProbeLaunchResult(true, "launched", state, List.copyOf(proofs), layers, signals);
    }

    public CatalogResult catalogDiscovery(CatalogState state, String discoveryId, Collection<String> completedProofs, ScanConfidence confidence) {
        CatalogState current = state == null ? emptyCatalog() : state;
        if (!DISCOVERY_REWARDS.containsKey(discoveryId)) {
            return new CatalogResult(false, "unknown_discovery", discoveryId, "", current);
        }
        if (current.discoveryIds().contains(discoveryId)) {
            return new CatalogResult(true, "already_cataloged", discoveryId, DISCOVERY_REWARDS.get(discoveryId), current);
        }

        ScanConfidence requiredConfidence = DISCOVERY_CONFIDENCE_REQUIREMENTS.getOrDefault(discoveryId, ScanConfidence.RESOLVED);
        String discoveryProof = DISCOVERY_PROOFS.get(discoveryId);
        boolean proofAlreadyEarned = proofSatisfied(discoveryProof, completedProofs, List.of(), List.of(), List.of());
        if ((confidence == null || !confidence.meets(requiredConfidence)) && !proofAlreadyEarned) {
            return new CatalogResult(false, "scan_confidence_too_low", discoveryId, DISCOVERY_REWARDS.get(discoveryId), current);
        }

        SequencedSet<String> discoveries = new LinkedHashSet<>(current.discoveryIds());
        SequencedSet<String> rewards = new LinkedHashSet<>(current.rewardProofs());
        SequencedSet<String> certifications = new LinkedHashSet<>(current.certificationProofs());
        SequencedSet<String> layers = new LinkedHashSet<>(current.unlockedLayers());
        SequencedSet<String> missions = new LinkedHashSet<>(current.missionProofs());

        discoveries.add(discoveryId);
        String reward = DISCOVERY_REWARDS.get(discoveryId);
        rewards.add(reward);
        if (reward.startsWith("holomap_layer:")) {
            layers.add(reward.substring("holomap_layer:".length()));
        } else if (reward.startsWith("mission:")) {
            missions.add(reward.substring("mission:".length()));
        }

        int cataloged = discoveries.size();
        if (cataloged >= 1) {
            certifications.add("certification:catalog_rank_1");
            layers.add("catalog_overlay");
        }
        if (cataloged >= 4) {
            certifications.add("certification:first_survey_circuit");
        }
        if (cataloged >= DISCOVERY_REWARDS.size()) {
            certifications.add("catalog:complete_sector_atlas");
        }

        CatalogState next = new CatalogState(
                List.copyOf(discoveries),
                List.copyOf(rewards),
                List.copyOf(certifications),
                List.copyOf(layers),
                List.copyOf(missions)
        );
        return new CatalogResult(true, "cataloged", discoveryId, reward, next);
    }

    public RoutePlanResult planRoute(RoutePlanRequest request) {
        RouteDefinition route = ROUTES.get(request.routeId());
        if (route == null) {
            return failedRoute(request.routeId(), "unknown_route", RouteMargin.BLOCKED, 0, 0, List.of());
        }

        List<String> requiredProofs = List.of(route.requiredProof());
        if (!proofSatisfied(route.requiredProof(), request.completedProofs(), request.inventoryItemIds(), List.of(), request.activeDepotIds())) {
            return failedRoute(route.id(), "required_proof_missing", RouteMargin.BLOCKED, route.fuelCost(), 0, requiredProofs);
        }

        int cargoPenalty = request.cargoMass() > 80 ? 1 : 0;
        int requiredFuel = route.fuelCost() + cargoPenalty;
        int qualityBonus = Math.max(0, (int) Math.floor(request.fuelCanisters() * Math.max(0, request.fuelQuality() - 1.0d)));
        int depotBonus = request.activeDepotIds().stream()
                .map(DEPOTS::get)
                .filter(depot -> depot != null)
                .mapToInt(depot -> Math.max(1, depot.capacity() / 4))
                .sum();
        int stabilizerBonus = proofSatisfied("item:route_stabilizer", request.completedProofs(), request.inventoryItemIds(), List.of(), List.of()) ? 1 : 0;
        int effectiveFuel = Math.max(0, request.fuelCanisters()) + qualityBonus + depotBonus + stabilizerBonus;
        if (effectiveFuel < requiredFuel) {
            return failedRoute(route.id(), "insufficient_fuel", RouteMargin.BLOCKED, requiredFuel, effectiveFuel, requiredProofs);
        }

        boolean hasRemoteDepot = request.activeDepotIds().stream().anyMatch(id -> !"outpost_fuel_cache".equals(id));
        boolean hasRecoveryDepot = request.activeDepotIds().contains("derelict_corridor_recovery_cache")
                || request.activeDepotIds().contains("cinder_ring_remote_depot");
        RouteMargin margin = route.declaredMargin();
        String reason = "route_ready";
        if (route.declaredMargin() == RouteMargin.UNSAFE_WITHOUT_DEPOT) {
            if (!hasRecoveryDepot) {
                return failedRoute(route.id(), "depot_required_for_return_safety", RouteMargin.UNSAFE_WITHOUT_DEPOT, requiredFuel, effectiveFuel, requiredProofs);
            }
            margin = RouteMargin.SAFE;
        } else if (route.declaredMargin() == RouteMargin.MARGINAL && (hasRemoteDepot || request.fuelQuality() >= 1.25d || stabilizerBonus > 0)) {
            margin = RouteMargin.SAFE;
            reason = "route_stabilized";
        }

        SequencedSet<String> unlocked = new LinkedHashSet<>();
        unlocked.add("route:" + route.id());
        unlocked.add(route.unlockProof());
        if (margin == RouteMargin.SAFE) {
            unlocked.add("holomap_layer:fuel_range");
        }
        return new RoutePlanResult(
                margin == RouteMargin.SAFE || margin == RouteMargin.MARGINAL,
                reason,
                route.id(),
                margin,
                requiredFuel,
                effectiveFuel,
                effectiveFuel - requiredFuel,
                requiredProofs,
                List.copyOf(unlocked)
        );
    }

    public SalvageResult attemptSalvage(SalvageAttempt attempt) {
        SalvageSiteDefinition site = SALVAGE_SITES.get(attempt.siteId());
        if (site == null) {
            return new SalvageResult(false, "unknown_salvage_site", attempt.siteId(), 0, List.of(), List.of(), List.of());
        }
        if (attempt.routeMargin() == RouteMargin.BLOCKED || attempt.routeMargin() == RouteMargin.UNSAFE_WITHOUT_DEPOT) {
            return blockedSalvage(site, "safe_route_required");
        }
        if (!proofSatisfied(site.requiredPreparation(), attempt.completedProofs(), attempt.inventoryItemIds(), attempt.revealedLayers(), List.of())) {
            return blockedSalvage(site, "required_preparation_missing");
        }
        if (site.hazards().contains("radiation")
                && !proofSatisfied("item:radiation_shielding", attempt.completedProofs(), attempt.inventoryItemIds(), List.of(), List.of())) {
            return blockedSalvage(site, "radiation_shielding_required");
        }
        if (site.hazardTier() >= 3
                && !proofSatisfied("item:route_stabilizer", attempt.completedProofs(), attempt.inventoryItemIds(), List.of(), List.of())) {
            return blockedSalvage(site, "route_stabilizer_required");
        }
        if (site.hazardTier() >= 2 && attempt.routeMargin() == RouteMargin.MARGINAL) {
            return blockedSalvage(site, "safe_return_margin_required");
        }

        List<String> rewards = SALVAGE_LOOT.getOrDefault(site.lootTable(), List.of());
        SequencedSet<String> proofs = new LinkedHashSet<>();
        proofs.add("salvage:" + site.id());
        if (DISCOVERY_REWARDS.containsKey(site.id())) {
            proofs.add("discovery:" + site.id());
        }
        proofs.addAll(rewards);
        return new SalvageResult(true, "recovered", site.id(), site.hazardTier(), site.hazards(), rewards, List.copyOf(proofs));
    }

    public DepotEstablishmentResult establishDepot(DepotBuildRequest request) {
        DepotDefinition definition = DEPOTS.get(request.depotId());
        if (definition == null) {
            return new DepotEstablishmentResult(false, "unknown_depot", null, List.of());
        }
        if (request.fuelStored() <= 0) {
            return new DepotEstablishmentResult(false, "depot_requires_fuel", null, List.of());
        }
        if (!"outpost_fuel_cache".equals(definition.id())
                && !proofSatisfied("item:depot_manifest", request.completedProofs(), request.inventoryItemIds(), List.of(), List.of())) {
            return new DepotEstablishmentResult(false, "depot_manifest_required", null, List.of());
        }

        DepotState depot = new DepotState(
                definition.id(),
                definition.sectorId(),
                Math.min(request.fuelStored(), definition.capacity()),
                definition.capacity(),
                definition.unlockProof()
        );
        return new DepotEstablishmentResult(
                true,
                "depot_established",
                depot,
                List.of("depot:" + definition.id(), definition.unlockProof(), "holomap_layer:depot_coverage")
        );
    }

    public SurveySaveSnapshot createSnapshot(
            OutpostState outpost,
            Collection<ProbeState> probes,
            CatalogState catalog,
            Collection<RoutePlanResult> routes,
            Collection<DepotState> depots,
            Collection<SalvageResult> salvage
    ) {
        SequencedSet<String> proofs = new LinkedHashSet<>();
        SequencedSet<String> layers = new LinkedHashSet<>();

        for (ProbeState probe : probes == null ? List.<ProbeState>of() : probes) {
            proofs.add("probe:" + probe.probeId());
            proofs.add("sector:" + probe.targetSectorId());
            layers.addAll(probe.revealedLayers());
        }
        CatalogState effectiveCatalog = catalog == null ? emptyCatalog() : catalog;
        proofs.addAll(effectiveCatalog.completedProofs());
        layers.addAll(effectiveCatalog.unlockedLayers());
        for (RoutePlanResult route : routes == null ? List.<RoutePlanResult>of() : routes) {
            proofs.addAll(route.unlockedProofs());
        }
        for (DepotState depot : depots == null ? List.<DepotState>of() : depots) {
            proofs.add("depot:" + depot.id());
            proofs.add(depot.unlockProof());
            layers.add("depot_coverage");
        }
        for (SalvageResult result : salvage == null ? List.<SalvageResult>of() : salvage) {
            proofs.addAll(result.completedProofs());
        }
        layers.forEach(layer -> proofs.add("holomap_layer:" + layer));

        return new SurveySaveSnapshot(
                "echo.galactic_survey.save_snapshot.v1",
                GalacticSurveyRuntimeContracts.LONG_RANGE_SURVEY_MODE,
                outpost == null ? startingOutpost() : outpost,
                new ArrayList<>(probes == null ? List.<ProbeState>of() : probes),
                effectiveCatalog,
                new ArrayList<>(routes == null ? List.<RoutePlanResult>of() : routes),
                new ArrayList<>(depots == null ? List.<DepotState>of() : depots),
                new ArrayList<>(salvage == null ? List.<SalvageResult>of() : salvage),
                List.copyOf(proofs),
                List.copyOf(layers)
        );
    }

    public SurveySaveSnapshot createSnapshot(
            Collection<ProbeState> probes,
            CatalogState catalog,
            Collection<RoutePlanResult> routes,
            Collection<DepotState> depots,
            Collection<SalvageResult> salvage
    ) {
        return createSnapshot(null, probes, catalog, routes, depots, salvage);
    }

    public HoloMapPlan buildHoloMapPlan(SurveySaveSnapshot snapshot) {
        SurveySaveSnapshot effective = snapshot == null
                ? createSnapshot(List.of(), emptyCatalog(), List.of(), List.of(), List.of())
                : snapshot;
        SequencedSet<String> layers = new LinkedHashSet<>();
        layers.add("sector_grid");
        layers.addAll(effective.activeHoloMapLayers());

        List<HoloMapMarker> markers = new ArrayList<>();
        for (ProbeState probe : effective.probes()) {
            if (probe.revealedDiscoveries().isEmpty()) {
                markers.add(new HoloMapMarker(
                        "unknown_signal:" + probe.targetSectorId(),
                        probe.targetSectorId(),
                        "unknown_signal",
                        probe.confidence(),
                        "unknown",
                        "unresolved",
                        List.of("scan_cones"),
                        List.of("probe:" + probe.probeId())
                ));
            }
            for (String discoveryId : probe.revealedDiscoveries()) {
                markers.add(discoveryMarker(discoveryId, probe.confidence(), probe.revealedLayers()));
            }
        }
        for (SalvageResult result : effective.salvage()) {
            SalvageSiteDefinition site = SALVAGE_SITES.get(result.siteId());
            if (site != null) {
                markers.add(new HoloMapMarker(
                        "salvage:" + site.id(),
                        site.sectorId(),
                        "salvage_site",
                        result.recovered() ? ScanConfidence.CONFIRMED : ScanConfidence.PARTIAL,
                        site.hazardTier() >= 2 ? "high" : "manageable",
                        result.recovered() ? "recovered_upgrade_parts" : "recoverable_upgrade_parts",
                        List.of("orbital_layers", "derelict_beacons"),
                        List.of(site.requiredPreparation())
                ));
            }
        }

        SequencedSet<String> visibleRoutes = new LinkedHashSet<>();
        SequencedSet<String> warnings = new LinkedHashSet<>();
        for (RoutePlanResult route : effective.routes()) {
            visibleRoutes.add(route.routeId());
            if (!route.travelReady()) {
                warnings.add(route.routeId() + ": " + route.reason());
            } else if (route.margin() != RouteMargin.SAFE) {
                warnings.add(route.routeId() + ": " + route.margin().id());
            }
        }

        SequencedSet<String> nextActions = new LinkedHashSet<>();
        if (!effective.outpost().surveyNetworkOnline()) {
            nextActions.add("repair_survey_network");
        }
        if (effective.probes().isEmpty()) {
            nextActions.add("launch_starter_probe");
        }
        if (!effective.completedProofs().contains("item:catalog_badge")) {
            nextActions.add("catalog_first_body");
        }
        if (!effective.completedProofs().contains("depot:cinder_ring_remote_depot")) {
            nextActions.add("establish_remote_depot");
        }
        if (!effective.completedProofs().contains("catalog:complete_sector_atlas")) {
            nextActions.add("publish_sector_atlas");
        }

        return new HoloMapPlan(
                "echo.galactic_survey.holomap_plan.v1",
                List.copyOf(layers),
                markers,
                List.copyOf(visibleRoutes),
                List.copyOf(warnings),
                List.copyOf(nextActions)
        );
    }

    public SurveyArrayRestorationResult restoreSurveyArray(SurveySaveSnapshot snapshot, Collection<String> externalProofs) {
        SurveySaveSnapshot effective = snapshot == null
                ? createSnapshot(List.of(), emptyCatalog(), List.of(), List.of(), List.of())
                : snapshot;
        SequencedSet<String> proofs = new LinkedHashSet<>(effective.completedProofs());
        proofs.addAll(orderedList(externalProofs));

        List<SurveyArrayRequirementStatus> statuses = new ArrayList<>();
        for (SurveyArrayRequirementDefinition requirement : SURVEY_ARRAY_REQUIREMENTS) {
            String source = evidenceSource(requirement.proof(), effective.completedProofs(), externalProofs);
            statuses.add(new SurveyArrayRequirementStatus(
                    requirement.id(),
                    requirement.proof(),
                    source != null,
                    source == null ? "missing" : source
            ));
        }

        boolean restored = statuses.stream().allMatch(SurveyArrayRequirementStatus::satisfied);
        SequencedSet<String> rewards = new LinkedHashSet<>();
        if (restored) {
            proofs.add("mission:restore_survey_array");
            proofs.add("item:galactic_survey_badge");
            proofs.add("survey_array:restored");
            rewards.add("item:galactic_survey_badge");
            rewards.add("catalog:published_sector_atlas");
        }

        return new SurveyArrayRestorationResult(
                restored,
                restored ? "survey_array_restored" : "survey_array_requirements_missing",
                statuses,
                List.copyOf(proofs),
                List.copyOf(rewards)
        );
    }

    public PublicAlphaReadinessReport evaluatePublicAlphaReadiness(SurveySaveSnapshot snapshot, Collection<String> externalProofs) {
        SurveySaveSnapshot effective = snapshot == null
                ? createSnapshot(List.of(), emptyCatalog(), List.of(), List.of(), List.of())
                : snapshot;
        List<ReleaseGateStatus> gates = new ArrayList<>();
        SequencedSet<String> blockers = new LinkedHashSet<>();
        for (ReleaseGateDefinition gate : RELEASE_GATES) {
            String source = evidenceSource(gate.proof(), effective.completedProofs(), externalProofs);
            boolean satisfied = source != null;
            gates.add(new ReleaseGateStatus(
                    gate.id(),
                    gate.proof(),
                    gate.required(),
                    satisfied,
                    satisfied ? source : "missing"
            ));
            if (gate.required() && !satisfied) {
                blockers.add(gate.id());
            }
        }

        boolean allowed = blockers.isEmpty();
        return new PublicAlphaReadinessReport(
                allowed,
                allowed ? "all_required_gates_satisfied" : "required_release_evidence_missing",
                gates,
                List.copyOf(blockers)
        );
    }

    public SurveySaveSnapshot surveyArrayReadySnapshot() {
        SurveySaveSnapshot firstTwoHours = firstTwoHourSnapshot();
        CatalogState catalog = firstTwoHours.catalog();
        for (String discoveryId : DISCOVERY_REWARDS.keySet()) {
            catalog = catalogDiscovery(catalog, discoveryId, List.of(DISCOVERY_PROOFS.get(discoveryId)), ScanConfidence.RESOLVED).catalogState();
        }
        SalvageResult arrayKeySalvage = attemptSalvage(new SalvageAttempt(
                "unstable_orbital_platform_ariadne",
                List.of("item:route_stabilizer", "item:radiation_shielding"),
                List.of("route_stabilizer", "radiation_shielding"),
                List.of("derelict_beacons", "orbital_layers"),
                RouteMargin.SAFE
        ));
        RoutePlanResult deepRoute = planRoute(new RoutePlanRequest(
                "deep_sector_beacon_route",
                4,
                1.0d,
                List.of("item:route_stabilizer", "depot:derelict_corridor_recovery_cache"),
                List.of("route_stabilizer"),
                List.of("derelict_corridor_recovery_cache"),
                0,
                true
        ));
        DepotState recoveryCache = new DepotState(
                "derelict_corridor_recovery_cache",
                "derelict_corridor_03",
                4,
                6,
                "route:deep_sector_beacon_route"
        );
        return createSnapshot(
                firstTwoHours.outpost(),
                firstTwoHours.probes(),
                catalog,
                concat(firstTwoHours.routes(), List.of(deepRoute)),
                concat(firstTwoHours.depots(), List.of(recoveryCache)),
                concat(firstTwoHours.salvage(), List.of(arrayKeySalvage))
        );
    }

    public SurveySaveSnapshot firstThirtyMinuteSnapshot() {
        OutpostRepairResult repair = repairOutpostRelay(new OutpostRepairRequest(
                startingOutpost(),
                2,
                1,
                true,
                true
        ));
        ProbeLaunchResult launch = launchProbe(new ProbeLaunchRequest(
                "starter_probe",
                "near_sector_01",
                repair.outpost().launcherPowerForProbe(),
                repair.outpost().surveyNetworkOnline(),
                List.of("starter_probe"),
                repair.completedProofs()
        ));
        CatalogResult catalog = catalogDiscovery(emptyCatalog(), "barren_moon_kg_01a", launch.completedProofs(), launch.probeState().confidence());
        SalvageResult salvage = attemptSalvage(new SalvageAttempt(
                "fallen_orbital_fragment",
                List.of("lens_scan:fallen_orbital_fragment"),
                List.of("starter_probe"),
                launch.revealedLayers(),
                RouteMargin.SAFE
        ));
        RoutePlanResult route = planRoute(new RoutePlanRequest(
                "outpost_to_near_sector_01",
                1,
                1.0d,
                List.of("item:fuel_canister"),
                List.of("fuel_canister"),
                List.of(),
                0,
                true
        ));
        return createSnapshot(repair.outpost(), List.of(launch.probeState()), catalog.catalogState(), List.of(route), List.of(), List.of(salvage));
    }

    public SurveySaveSnapshot firstTwoHourSnapshot() {
        OutpostRepairResult repair = repairOutpostRelay(new OutpostRepairRequest(
                startingOutpost(),
                2,
                1,
                true,
                true
        ));
        ProbeLaunchResult starter = launchProbe(new ProbeLaunchRequest(
                "starter_probe",
                "near_sector_01",
                repair.outpost().launcherPowerForProbe(),
                repair.outpost().surveyNetworkOnline(),
                List.of("starter_probe"),
                repair.completedProofs()
        ));
        ProbeLaunchResult outer = launchProbe(new ProbeLaunchRequest(
                "long_range_probe",
                "outer_sector_02",
                80,
                true,
                List.of("long_range_probe"),
                List.of("item:long_range_probe", "route:near_sector_01_survey_hop")
        ));
        ProbeLaunchResult derelict = launchProbe(new ProbeLaunchRequest(
                "long_range_probe",
                "derelict_corridor_03",
                80,
                true,
                List.of("long_range_probe"),
                List.of("item:long_range_probe", "route:cinder_ring_depot_route")
        ));

        CatalogState catalog = emptyCatalog();
        catalog = catalogDiscovery(catalog, "barren_moon_kg_01a", starter.completedProofs(), starter.probeState().confidence()).catalogState();
        catalog = catalogDiscovery(catalog, "planet_candidate_ks_02", outer.completedProofs(), outer.probeState().confidence()).catalogState();
        catalog = catalogDiscovery(catalog, "signal_anomaly_veil_trace", List.of("lens_scan:fallen_orbital_fragment"), ScanConfidence.TRACE).catalogState();
        catalog = catalogDiscovery(catalog, "derelict_relay_osprey", List.of("salvage:derelict_relay_osprey"), ScanConfidence.CONFIRMED).catalogState();

        SalvageResult derelictSalvage = attemptSalvage(new SalvageAttempt(
                "derelict_relay_osprey",
                List.of("holomap_layer:derelict_beacons"),
                List.of("navigation_core"),
                outer.revealedLayers(),
                RouteMargin.SAFE
        ));
        DepotEstablishmentResult depot = establishDepot(new DepotBuildRequest(
                "cinder_ring_remote_depot",
                4,
                List.of("item:depot_manifest"),
                List.of("depot_manifest")
        ));
        RoutePlanResult route = planRoute(new RoutePlanRequest(
                "near_sector_01_survey_hop",
                2,
                1.0d,
                List.of("item:navigation_core"),
                List.of("navigation_core"),
                List.of("cinder_ring_remote_depot"),
                0,
                true
        ));

        return createSnapshot(
                repair.outpost(),
                List.of(starter.probeState(), outer.probeState(), derelict.probeState()),
                catalog,
                List.of(route),
                depot.established() ? List.of(depot.depot()) : List.of(),
                List.of(derelictSalvage)
        );
    }

    public static CatalogState emptyCatalog() {
        return new CatalogState(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.galactic_survey.runtime_service.v1");
        result.put("moduleId", GalacticSurveyRuntimeContracts.MODULE_ID);
        result.put("runtimeLoops", List.of(
                "probe_launch",
                "outpost_network_repair",
                "holomap_reveal",
                "holomap_route_planning",
                "catalog_progression",
                "fuel_route_planning",
                "remote_depot_establishment",
                "orbital_salvage_recovery",
                "survey_save_snapshot"
        ));
        result.put("scanConfidenceTiers", List.of("unknown", "trace", "partial", "confirmed", "resolved"));
        result.put("routeMargins", List.of("safe", "marginal", "unsafe_without_depot", "blocked"));
        result.put("routeIds", new ArrayList<>(ROUTES.keySet()));
        result.put("salvageSiteIds", new ArrayList<>(SALVAGE_SITES.keySet()));
        result.put("depotIds", new ArrayList<>(DEPOTS.keySet()));
        result.put("surveyArrayRequirementIds", SURVEY_ARRAY_REQUIREMENTS.stream().map(SurveyArrayRequirementDefinition::id).toList());
        result.put("releaseGateIds", RELEASE_GATES.stream().map(ReleaseGateDefinition::id).toList());
        result.put("outpostId", "quiet_survey_outpost");
        result.put("holomapPlanSchema", "echo.galactic_survey.holomap_plan.v1");
        result.put("snapshotSchema", "echo.galactic_survey.save_snapshot.v1");
        return Map.copyOf(result);
    }

    private static HoloMapMarker discoveryMarker(String discoveryId, ScanConfidence confidence, List<String> probeLayers) {
        String category = DISCOVERY_CATEGORIES.getOrDefault(discoveryId, "unknown_signal");
        String sectorId = sectorForDiscovery(discoveryId);
        String risk = switch (category) {
            case "derelict", "lost_survey_craft", "unstable_platform" -> "high";
            case "debris_belt", "anomaly", "deep_sector_beacon" -> "uncertain";
            default -> "low";
        };
        String value = switch (category) {
            case "derelict", "lost_survey_craft", "unstable_platform" -> "upgrade_salvage";
            case "ice_body" -> "fuel_route_support";
            case "deep_sector_beacon" -> "survey_array_restoration";
            default -> "catalog_progression";
        };
        SequencedSet<String> layers = new LinkedHashSet<>(probeLayers == null ? List.of() : probeLayers);
        layers.add("catalog_overlay");
        return new HoloMapMarker(
                "discovery:" + discoveryId,
                sectorId,
                category,
                confidence,
                risk,
                value,
                List.copyOf(layers),
                List.of(DISCOVERY_PROOFS.getOrDefault(discoveryId, "discovery:" + discoveryId))
        );
    }

    private static String sectorForDiscovery(String discoveryId) {
        for (Map.Entry<String, List<String>> entry : DISCOVERIES_BY_SECTOR.entrySet()) {
            if (entry.getValue().contains(discoveryId)) {
                return entry.getKey();
            }
        }
        return "unknown_sector";
    }

    private static String evidenceSource(String proof, Collection<String> snapshotProofs, Collection<String> externalProofs) {
        if (orderedSet(snapshotProofs).contains(proof)) {
            return "snapshot";
        }
        if (orderedSet(externalProofs).contains(proof)) {
            return "external";
        }
        return null;
    }

    private static ProbeLaunchResult failedProbe(String reason, String probeId, String targetSectorId) {
        ProbeState state = new ProbeState(probeId, targetSectorId, ScanConfidence.UNKNOWN, 0, false, true, List.of(), List.of());
        return new ProbeLaunchResult(false, reason, state, List.of(), List.of(), List.of());
    }

    private static RoutePlanResult failedRoute(String routeId, String reason, RouteMargin margin, int requiredFuel, int effectiveFuel, List<String> requiredProofs) {
        return new RoutePlanResult(false, reason, routeId, margin, requiredFuel, effectiveFuel, 0, requiredProofs, List.of());
    }

    private static SalvageResult blockedSalvage(SalvageSiteDefinition site, String reason) {
        return new SalvageResult(false, reason, site.id(), site.hazardTier(), site.hazards(), List.of(), List.of());
    }

    private static List<String> visibleDiscoveries(String sectorId, ScanConfidence confidence) {
        List<String> candidates = DISCOVERIES_BY_SECTOR.getOrDefault(sectorId, List.of());
        List<String> result = new ArrayList<>();
        for (String discoveryId : candidates) {
            ScanConfidence required = DISCOVERY_CONFIDENCE_REQUIREMENTS.getOrDefault(discoveryId, ScanConfidence.RESOLVED);
            if (confidence.meets(required)) {
                result.add(discoveryId);
            }
        }
        return result;
    }

    private static List<String> layersForProbeResult(ScanConfidence confidence, String targetSectorId) {
        SequencedSet<String> layers = new LinkedHashSet<>();
        layers.add("sector_grid");
        if (confidence.meets(ScanConfidence.TRACE)) {
            layers.add("scan_cones");
        }
        if (confidence.meets(ScanConfidence.PARTIAL)) {
            layers.add("catalog_overlay");
        }
        if (confidence.meets(ScanConfidence.CONFIRMED)) {
            layers.add("orbital_layers");
            if (!"near_sector_01".equals(targetSectorId)) {
                layers.add("derelict_beacons");
            }
        }
        if (confidence.meets(ScanConfidence.RESOLVED)) {
            layers.add("fuel_range");
        }
        return List.copyOf(layers);
    }

    private static boolean proofSatisfied(
            String proof,
            Collection<String> completedProofs,
            Collection<String> inventoryItemIds,
            Collection<String> revealedLayers,
            Collection<String> activeDepotIds
    ) {
        if (proof == null || proof.isBlank()) {
            return true;
        }
        Set<String> proofs = orderedSet(completedProofs);
        if (proofs.contains(proof)) {
            return true;
        }
        String[] parts = proof.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        String type = parts[0];
        String value = parts[1];
        if ("item".equals(type)) {
            return orderedSet(inventoryItemIds).contains(value);
        }
        if ("holomap_layer".equals(type)) {
            return orderedSet(revealedLayers).contains(value);
        }
        if ("depot".equals(type)) {
            return orderedSet(activeDepotIds).contains(value);
        }
        return false;
    }

    private static Map<String, RouteDefinition> mapRoutes(List<RouteDefinition> routes) {
        Map<String, RouteDefinition> result = new LinkedHashMap<>();
        for (RouteDefinition route : routes) {
            result.put(route.id(), route);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, SalvageSiteDefinition> mapSalvageSites(List<SalvageSiteDefinition> sites) {
        Map<String, SalvageSiteDefinition> result = new LinkedHashMap<>();
        for (SalvageSiteDefinition site : sites) {
            result.put(site.id(), site);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, DepotDefinition> mapDepots(List<DepotDefinition> depots) {
        Map<String, DepotDefinition> result = new LinkedHashMap<>();
        for (DepotDefinition depot : depots) {
            result.put(depot.id(), depot);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Set<String> orderedSet(Collection<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values == null ? List.of() : values));
    }

    private static List<String> orderedList(Collection<String> values) {
        return List.copyOf(new LinkedHashSet<>(values == null ? List.of() : values));
    }

    private static <T> List<T> concat(Collection<T> first, Collection<T> second) {
        List<T> result = new ArrayList<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return List.copyOf(result);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
