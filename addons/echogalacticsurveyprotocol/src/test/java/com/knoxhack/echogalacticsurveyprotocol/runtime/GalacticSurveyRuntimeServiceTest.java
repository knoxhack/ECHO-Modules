package com.knoxhack.echogalacticsurveyprotocol.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyRuntimeService.RouteMargin.SAFE;
import static com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyRuntimeService.RouteMargin.UNSAFE_WITHOUT_DEPOT;
import static com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyRuntimeService.ScanConfidence.CONFIRMED;
import static com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyRuntimeService.ScanConfidence.PARTIAL;
import static com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyRuntimeService.ScanConfidence.TRACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticSurveyRuntimeServiceTest {
    private final GalacticSurveyRuntimeService runtime = new GalacticSurveyRuntimeService();

    @Test
    void outpostRepairBringsSurveyNetworkOnlineAndFeedsProbeLaunch() {
        GalacticSurveyRuntimeService.OutpostState dark = GalacticSurveyRuntimeService.startingOutpost();

        assertEquals(GalacticSurveyRuntimeService.SurveyNetworkState.OFFLINE, dark.networkState());
        assertFalse(dark.surveyNetworkOnline());
        assertTrue(dark.completedProofs().contains("terminal_page:survey_network"));

        GalacticSurveyRuntimeService.OutpostRepairResult blocked = runtime.repairOutpostRelay(new GalacticSurveyRuntimeService.OutpostRepairRequest(
                dark,
                1,
                1,
                true,
                true
        ));

        assertFalse(blocked.repaired());
        assertEquals("relay_parts_missing", blocked.reason());

        GalacticSurveyRuntimeService.OutpostRepairResult repaired = runtime.repairOutpostRelay(new GalacticSurveyRuntimeService.OutpostRepairRequest(
                dark,
                2,
                1,
                true,
                true
        ));

        assertTrue(repaired.repaired());
        assertEquals(GalacticSurveyRuntimeService.SurveyNetworkState.ONLINE, repaired.outpost().networkState());
        assertTrue(repaired.outpost().launcherCharged());
        assertTrue(repaired.completedProofs().contains("power:small_relay_online"));
        assertTrue(repaired.completedProofs().contains("terminal_page:probe_control"));

        GalacticSurveyRuntimeService.ProbeLaunchResult launch = runtime.launchProbe(new GalacticSurveyRuntimeService.ProbeLaunchRequest(
                "starter_probe",
                "near_sector_01",
                repaired.outpost().launcherPowerForProbe(),
                repaired.outpost().surveyNetworkOnline(),
                List.of("starter_probe"),
                repaired.completedProofs()
        ));

        assertTrue(launch.launched());
    }

    @Test
    void firstThirtyMinuteLoopLaunchesCatalogsSalvagesAndPlansFirstRoute() {
        GalacticSurveyRuntimeService.ProbeLaunchResult launch = runtime.launchProbe(new GalacticSurveyRuntimeService.ProbeLaunchRequest(
                "starter_probe",
                "near_sector_01",
                50,
                true,
                List.of("starter_probe"),
                List.of("power:small_relay_online", "terminal_page:survey_network")
        ));

        assertTrue(launch.launched());
        assertEquals(PARTIAL, launch.probeState().confidence());
        assertTrue(launch.completedProofs().contains("probe:starter_probe"));
        assertTrue(launch.revealedLayers().contains("scan_cones"));
        assertTrue(launch.revealedSignals().contains("barren_moon_kg_01a"));

        GalacticSurveyRuntimeService.CatalogResult catalog = runtime.catalogDiscovery(
                GalacticSurveyRuntimeService.emptyCatalog(),
                "barren_moon_kg_01a",
                launch.completedProofs(),
                launch.probeState().confidence()
        );

        assertTrue(catalog.accepted());
        assertEquals("item:catalog_badge", catalog.rewardProof());
        assertTrue(catalog.catalogState().certificationProofs().contains("certification:catalog_rank_1"));
        assertTrue(catalog.catalogState().unlockedLayers().contains("catalog_overlay"));

        GalacticSurveyRuntimeService.SalvageResult salvage = runtime.attemptSalvage(new GalacticSurveyRuntimeService.SalvageAttempt(
                "fallen_orbital_fragment",
                List.of("lens_scan:fallen_orbital_fragment"),
                List.of("starter_probe"),
                launch.revealedLayers(),
                SAFE
        ));

        assertTrue(salvage.recovered());
        assertTrue(salvage.rewardProofs().contains("item:burned_navigation_core"));
        assertTrue(salvage.completedProofs().contains("salvage:fallen_orbital_fragment"));

        GalacticSurveyRuntimeService.RoutePlanResult route = runtime.planRoute(new GalacticSurveyRuntimeService.RoutePlanRequest(
                "outpost_to_near_sector_01",
                1,
                1.0d,
                List.of("item:fuel_canister"),
                List.of("fuel_canister"),
                List.of(),
                0,
                true
        ));

        assertTrue(route.travelReady());
        assertEquals(SAFE, route.margin());
        assertTrue(route.unlockedProofs().contains("mission:first_survey_hop"));

        GalacticSurveyRuntimeService.SurveySaveSnapshot snapshot = runtime.createSnapshot(
                List.of(launch.probeState()),
                catalog.catalogState(),
                List.of(route),
                List.of(),
                List.of(salvage)
        );

        assertEquals("echo.galactic_survey.save_snapshot.v1", snapshot.schema());
        assertTrue(snapshot.completedProofs().contains("probe:starter_probe"));
        assertTrue(snapshot.completedProofs().contains("item:burned_navigation_core"));
        assertTrue(snapshot.completedProofs().contains("route:outpost_to_near_sector_01"));
    }

    @Test
    void firstTwoHourLoopSupportsProbeBatchDerelictSalvageDepotAndCatalogRank() {
        GalacticSurveyRuntimeService.ProbeLaunchResult starter = runtime.launchProbe(new GalacticSurveyRuntimeService.ProbeLaunchRequest(
                "starter_probe",
                "near_sector_01",
                50,
                true,
                List.of("starter_probe"),
                List.of("power:small_relay_online")
        ));
        GalacticSurveyRuntimeService.ProbeLaunchResult outer = runtime.launchProbe(new GalacticSurveyRuntimeService.ProbeLaunchRequest(
                "long_range_probe",
                "outer_sector_02",
                80,
                true,
                List.of("long_range_probe"),
                List.of("item:long_range_probe", "route:near_sector_01_survey_hop")
        ));
        GalacticSurveyRuntimeService.ProbeLaunchResult corridor = runtime.launchProbe(new GalacticSurveyRuntimeService.ProbeLaunchRequest(
                "long_range_probe",
                "derelict_corridor_03",
                80,
                true,
                List.of("long_range_probe"),
                List.of("item:long_range_probe", "route:cinder_ring_depot_route")
        ));

        assertTrue(starter.launched());
        assertTrue(outer.launched());
        assertTrue(corridor.launched());
        assertEquals(CONFIRMED, outer.probeState().confidence());
        assertTrue(outer.revealedLayers().contains("derelict_beacons"));

        GalacticSurveyRuntimeService.SalvageResult derelict = runtime.attemptSalvage(new GalacticSurveyRuntimeService.SalvageAttempt(
                "derelict_relay_osprey",
                List.of("holomap_layer:derelict_beacons"),
                List.of("navigation_core"),
                outer.revealedLayers(),
                SAFE
        ));

        assertTrue(derelict.recovered());
        assertTrue(derelict.rewardProofs().contains("item:long_range_probe"));
        assertTrue(derelict.completedProofs().contains("discovery:derelict_relay_osprey"));

        GalacticSurveyRuntimeService.CatalogState catalog = GalacticSurveyRuntimeService.emptyCatalog();
        catalog = runtime.catalogDiscovery(catalog, "barren_moon_kg_01a", starter.completedProofs(), starter.probeState().confidence()).catalogState();
        catalog = runtime.catalogDiscovery(catalog, "planet_candidate_ks_02", outer.completedProofs(), outer.probeState().confidence()).catalogState();
        catalog = runtime.catalogDiscovery(catalog, "signal_anomaly_veil_trace", List.of("lens_scan:fallen_orbital_fragment"), TRACE).catalogState();
        catalog = runtime.catalogDiscovery(catalog, "derelict_relay_osprey", derelict.completedProofs(), CONFIRMED).catalogState();

        assertEquals(4, catalog.discoveryIds().size());
        assertTrue(catalog.certificationProofs().contains("certification:first_survey_circuit"));
        assertTrue(catalog.rewardProofs().contains("item:long_range_probe"));

        GalacticSurveyRuntimeService.DepotEstablishmentResult depot = runtime.establishDepot(new GalacticSurveyRuntimeService.DepotBuildRequest(
                "cinder_ring_remote_depot",
                4,
                List.of("item:depot_manifest"),
                List.of("depot_manifest")
        ));

        assertTrue(depot.established());
        assertEquals("outer_sector_02", depot.depot().sectorId());
        assertTrue(depot.completedProofs().contains("depot:cinder_ring_remote_depot"));

        GalacticSurveyRuntimeService.RoutePlanResult route = runtime.planRoute(new GalacticSurveyRuntimeService.RoutePlanRequest(
                "cinder_ring_depot_route",
                1,
                1.0d,
                depot.completedProofs(),
                List.of("navigation_core"),
                List.of("cinder_ring_remote_depot"),
                0,
                true
        ));

        assertTrue(route.travelReady());
        assertEquals(SAFE, route.margin());
        assertTrue(route.unlockedProofs().contains("holomap_layer:depot_coverage"));

        GalacticSurveyRuntimeService.SurveySaveSnapshot snapshot = runtime.createSnapshot(
                List.of(starter.probeState(), outer.probeState(), corridor.probeState()),
                catalog,
                List.of(route),
                List.of(depot.depot()),
                List.of(derelict)
        );

        assertEquals(3, snapshot.probes().size());
        assertTrue(snapshot.completedProofs().contains("depot:cinder_ring_remote_depot"));
        assertTrue(snapshot.completedProofs().contains("item:long_range_probe"));
        assertTrue(snapshot.completedProofs().contains("certification:first_survey_circuit"));
    }

    @Test
    void deepSectorRouteBlocksUntilDepotMakesReturnMarginUnderstandable() {
        GalacticSurveyRuntimeService.RoutePlanResult blocked = runtime.planRoute(new GalacticSurveyRuntimeService.RoutePlanRequest(
                "deep_sector_beacon_route",
                5,
                1.0d,
                List.of("item:route_stabilizer"),
                List.of("route_stabilizer"),
                List.of(),
                0,
                true
        ));

        assertFalse(blocked.travelReady());
        assertEquals(UNSAFE_WITHOUT_DEPOT, blocked.margin());
        assertEquals("depot_required_for_return_safety", blocked.reason());

        GalacticSurveyRuntimeService.RoutePlanResult safe = runtime.planRoute(new GalacticSurveyRuntimeService.RoutePlanRequest(
                "deep_sector_beacon_route",
                4,
                1.0d,
                List.of("item:route_stabilizer", "depot:derelict_corridor_recovery_cache"),
                List.of("route_stabilizer"),
                List.of("derelict_corridor_recovery_cache"),
                0,
                true
        ));

        assertTrue(safe.travelReady());
        assertEquals(SAFE, safe.margin());
        assertTrue(safe.unlockedProofs().contains("discovery:deep_sector_beacon_ks_04"));
        assertTrue(safe.returnReserve() > 0);
    }

    @Test
    void hazardousSalvageRequiresPreparationAndProtectiveUpgrades() {
        GalacticSurveyRuntimeService.SalvageResult blocked = runtime.attemptSalvage(new GalacticSurveyRuntimeService.SalvageAttempt(
                "lost_survey_craft_lysander",
                List.of(),
                List.of(),
                List.of("derelict_beacons"),
                SAFE
        ));

        assertFalse(blocked.recovered());
        assertEquals("required_preparation_missing", blocked.reason());

        GalacticSurveyRuntimeService.SalvageResult radiationBlocked = runtime.attemptSalvage(new GalacticSurveyRuntimeService.SalvageAttempt(
                "lost_survey_craft_lysander",
                List.of("item:radiation_shielding"),
                List.of(),
                List.of("derelict_beacons"),
                GalacticSurveyRuntimeService.RouteMargin.MARGINAL
        ));

        assertFalse(radiationBlocked.recovered());
        assertEquals("safe_return_margin_required", radiationBlocked.reason());

        GalacticSurveyRuntimeService.SalvageResult recovered = runtime.attemptSalvage(new GalacticSurveyRuntimeService.SalvageAttempt(
                "lost_survey_craft_lysander",
                List.of("item:radiation_shielding"),
                List.of("radiation_shielding"),
                List.of("derelict_beacons"),
                SAFE
        ));

        assertTrue(recovered.recovered());
        assertTrue(recovered.hazards().contains("radiation"));
        assertTrue(recovered.rewardProofs().contains("item:route_stabilizer"));
        assertTrue(recovered.rewardProofs().contains("item:deep_space_lens"));
    }

    @Test
    void builtInSnapshotsExposeReleaseGateEvidenceWithoutClaimingPlaythroughCompletion() {
        GalacticSurveyRuntimeService.SurveySaveSnapshot first30 = runtime.firstThirtyMinuteSnapshot();
        GalacticSurveyRuntimeService.SurveySaveSnapshot first2Hours = runtime.firstTwoHourSnapshot();

        assertTrue(first30.outpost().surveyNetworkOnline());
        assertTrue(first30.completedProofs().contains("mission:first_survey_hop"));
        assertTrue(first30.completedProofs().contains("item:catalog_badge"));
        assertEquals(3, first2Hours.probes().size());
        assertTrue(first2Hours.completedProofs().contains("mission:first_survey_circuit"));
        assertTrue(first2Hours.completedProofs().contains("depot:cinder_ring_remote_depot"));

        GalacticSurveyRuntimeService.SurveySaveSnapshot copy = new GalacticSurveyRuntimeService.SurveySaveSnapshot(
                first2Hours.schema(),
                first2Hours.mode(),
                first2Hours.outpost(),
                first2Hours.probes(),
                first2Hours.catalog(),
                first2Hours.routes(),
                first2Hours.depots(),
                first2Hours.salvage(),
                first2Hours.completedProofs(),
                first2Hours.activeHoloMapLayers()
        );

        assertEquals(first2Hours, copy);

        Map<String, Object> manifest = GalacticSurveyRuntimeService.adapterManifest();
        assertEquals("echo.galactic_survey.runtime_service.v1", manifest.get("schema"));
        assertNotNull(manifest.get("runtimeLoops"));
    }

    @Test
    void holomapPlanClarifiesUnknownRiskyAndValuableTargets() {
        GalacticSurveyRuntimeService.SurveySaveSnapshot first2Hours = runtime.firstTwoHourSnapshot();
        GalacticSurveyRuntimeService.HoloMapPlan plan = runtime.buildHoloMapPlan(first2Hours);

        assertEquals("echo.galactic_survey.holomap_plan.v1", plan.schema());
        assertTrue(plan.activeLayers().contains("scan_cones"));
        assertTrue(plan.activeLayers().contains("depot_coverage"));
        assertTrue(plan.visibleRoutes().contains("near_sector_01_survey_hop"));
        assertTrue(plan.nextActions().contains("publish_sector_atlas"));
        assertTrue(plan.markers().stream().anyMatch(marker ->
                marker.id().equals("discovery:derelict_relay_osprey")
                        && marker.routeRisk().equals("high")
                        && marker.valueSignal().equals("upgrade_salvage")
        ));
        assertTrue(plan.markers().stream().anyMatch(marker ->
                marker.kind().equals("salvage_site")
                        && marker.visibleLayers().contains("derelict_beacons")
        ));
    }

    @Test
    void surveyArrayRestorationRequiresCompleteAtlasDeepBeaconDepotUpgradeAndArrayKey() {
        GalacticSurveyRuntimeService.SurveyArrayRestorationResult incomplete = runtime.restoreSurveyArray(
                runtime.firstTwoHourSnapshot(),
                List.of("block:survey_array_console")
        );

        assertFalse(incomplete.restored());
        assertEquals("survey_array_requirements_missing", incomplete.reason());
        assertTrue(incomplete.requirements().stream().anyMatch(requirement ->
                requirement.id().equals("complete_sector_atlas") && !requirement.satisfied()
        ));

        GalacticSurveyRuntimeService.SurveySaveSnapshot ready = runtime.surveyArrayReadySnapshot();
        GalacticSurveyRuntimeService.SurveyArrayRestorationResult restored = runtime.restoreSurveyArray(
                ready,
                List.of("block:survey_array_console")
        );

        assertTrue(restored.restored());
        assertEquals("survey_array_restored", restored.reason());
        assertTrue(restored.completedProofs().contains("survey_array:restored"));
        assertTrue(restored.completedProofs().contains("item:galactic_survey_badge"));
        assertTrue(restored.rewards().contains("catalog:published_sector_atlas"));
        assertTrue(restored.requirements().stream().allMatch(GalacticSurveyRuntimeService.SurveyArrayRequirementStatus::satisfied));
    }

    @Test
    void publicAlphaReadinessBlocksWithoutRealPlaythroughEvidence() {
        GalacticSurveyRuntimeService.PublicAlphaReadinessReport blocked = runtime.evaluatePublicAlphaReadiness(
                runtime.firstTwoHourSnapshot(),
                List.of()
        );

        assertFalse(blocked.publicAlphaAllowed());
        assertEquals("required_release_evidence_missing", blocked.reason());
        assertTrue(blocked.blockers().contains("real_first_30_playthrough"));

        GalacticSurveyRuntimeService.PublicAlphaReadinessReport stillBlockedWithoutDerelictEvidence = runtime.evaluatePublicAlphaReadiness(
                runtime.firstThirtyMinuteSnapshot(),
                List.of()
        );

        assertFalse(stillBlockedWithoutDerelictEvidence.publicAlphaAllowed());
        assertTrue(stillBlockedWithoutDerelictEvidence.blockers().contains("one_salvage_site_playable"));

        GalacticSurveyRuntimeService.PublicAlphaReadinessReport stillBlockedWithoutManualEvidence = runtime.evaluatePublicAlphaReadiness(
                runtime.firstTwoHourSnapshot(),
                List.of()
        );

        assertFalse(stillBlockedWithoutManualEvidence.publicAlphaAllowed());
        assertTrue(stillBlockedWithoutManualEvidence.blockers().contains("real_survey_array_playthrough"));

        GalacticSurveyRuntimeService.PublicAlphaReadinessReport ready = runtime.evaluatePublicAlphaReadiness(
                runtime.surveyArrayReadySnapshot(),
                List.of(
                        "manual:real_first_30_playthrough",
                        "manual:real_first_2_hour_playthrough",
                        "manual:real_survey_array_playthrough",
                        "manual:fresh_world_created",
                        "manual:save_reload_verified",
                        "manual:no_crash_evidence"
                )
        );

        assertTrue(ready.publicAlphaAllowed(), ready.blockers().toString());
        assertEquals("all_required_gates_satisfied", ready.reason());
        assertTrue(ready.gates().stream().allMatch(GalacticSurveyRuntimeService.ReleaseGateStatus::satisfied));
    }

    @Test
    void runtimePlaytestHarnessCoversMilestonesWithoutClaimingPublicAlphaReadiness() {
        Map<String, Object> report = GalacticSurveyRuntimePlaytestHarness.runReport();

        assertEquals(GalacticSurveyRuntimePlaytestHarness.SCHEMA, report.get("schemaVersion"));
        assertEquals(true, report.get("ok"));

        @SuppressWarnings("unchecked")
        Map<String, Object> runtimeChecks = (Map<String, Object>) report.get("runtimeChecks");
        assertEquals(true, runtimeChecks.get("first30Loop"));
        assertEquals(true, runtimeChecks.get("first2HourLoop"));
        assertEquals(true, runtimeChecks.get("holomapMeaningful"));
        assertEquals(true, runtimeChecks.get("surveyArrayRestored"));
        assertEquals(true, runtimeChecks.get("saveReloadEquivalent"));
        assertEquals(true, runtimeChecks.get("publicAlphaStillRequiresExternalEvidence"));

        @SuppressWarnings("unchecked")
        Map<String, Object> releaseGatePreview = (Map<String, Object>) report.get("releaseGatePreview");
        assertEquals(false, releaseGatePreview.get("publicAlphaAllowed"));
        @SuppressWarnings("unchecked")
        List<String> blockers = (List<String>) releaseGatePreview.get("blockers");
        assertTrue(blockers.contains("real_first_30_playthrough"));
        assertTrue(blockers.contains("no_crash_evidence"));
    }
}
