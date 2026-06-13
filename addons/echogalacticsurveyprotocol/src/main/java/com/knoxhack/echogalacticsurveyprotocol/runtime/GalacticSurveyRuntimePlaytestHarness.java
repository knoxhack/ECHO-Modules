package com.knoxhack.echogalacticsurveyprotocol.runtime;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiled-runtime playtest harness for Galactic Survey.
 *
 * <p>This validates the deterministic runtime service loops that back manual QA.
 * It is not a substitute for visible client playthrough evidence.</p>
 */
public final class GalacticSurveyRuntimePlaytestHarness {
    public static final String SCHEMA = "echo.galactic_survey.runtime-playtest.v1";

    private GalacticSurveyRuntimePlaytestHarness() {
    }

    public static void main(String[] args) throws IOException {
        Path out = outputPath(args);
        Map<String, Object> report = runReport();
        String json = toJson(report);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, json + System.lineSeparator(), StandardCharsets.UTF_8);
        System.out.println("Galactic Survey runtime playtest report written: " + out.toAbsolutePath());
        if (!Boolean.TRUE.equals(report.get("ok"))) {
            System.exit(1);
        }
    }

    public static Map<String, Object> runReport() {
        GalacticSurveyRuntimeService runtime = new GalacticSurveyRuntimeService();
        GalacticSurveyRuntimeService.SurveySaveSnapshot first30 = runtime.firstThirtyMinuteSnapshot();
        GalacticSurveyRuntimeService.SurveySaveSnapshot first2Hours = runtime.firstTwoHourSnapshot();
        GalacticSurveyRuntimeService.SurveySaveSnapshot surveyArrayReady = runtime.surveyArrayReadySnapshot();
        GalacticSurveyRuntimeService.SurveyArrayRestorationResult surveyArray = runtime.restoreSurveyArray(
                surveyArrayReady,
                List.of("block:survey_array_console")
        );
        GalacticSurveyRuntimeService.HoloMapPlan first2HourMap = runtime.buildHoloMapPlan(first2Hours);
        GalacticSurveyRuntimeService.SurveySaveSnapshot reloaded = reloadSnapshot(surveyArrayReady);
        GalacticSurveyRuntimeService.PublicAlphaReadinessReport releasePreview = runtime.evaluatePublicAlphaReadiness(
                surveyArrayReady,
                List.of()
        );

        Map<String, Object> runtimeChecks = object(
                "first30Loop", first30.completedProofs().containsAll(List.of(
                        "probe:starter_probe",
                        "holomap_layer:scan_cones",
                        "discovery:barren_moon_kg_01a",
                        "item:burned_navigation_core",
                        "mission:first_survey_hop"
                )),
                "first2HourLoop", first2Hours.completedProofs().containsAll(List.of(
                        "probe:starter_probe",
                        "probe:long_range_probe",
                        "route:near_sector_01_survey_hop",
                        "salvage:derelict_relay_osprey",
                        "depot:cinder_ring_remote_depot",
                        "mission:first_survey_circuit"
                )),
                "holomapMeaningful", first2HourMap.activeLayers().containsAll(List.of("scan_cones", "depot_coverage"))
                        && first2HourMap.markers().stream().anyMatch(marker -> marker.routeRisk().equals("high"))
                        && first2HourMap.nextActions().contains("publish_sector_atlas"),
                "surveyArrayRestored", surveyArray.restored(),
                "saveReloadEquivalent", surveyArrayReady.equals(reloaded),
                "publicAlphaStillRequiresExternalEvidence", !releasePreview.publicAlphaAllowed()
                        && releasePreview.blockers().contains("real_first_30_playthrough")
                        && releasePreview.blockers().contains("no_crash_evidence")
                        && releasePreview.blockers().contains("launcher_install_update_repair_rollback")
        );
        boolean ok = runtimeChecks.values().stream().allMatch(Boolean.TRUE::equals);

        return object(
                "schemaVersion", SCHEMA,
                "ok", ok,
                "generatedAt", Instant.now().toString(),
                "scope", "compiled-runtime-service",
                "moduleId", GalacticSurveyRuntimeContracts.MODULE_ID,
                "packId", GalacticSurveyRuntimeContracts.PACK_ID,
                "mode", GalacticSurveyRuntimeContracts.LONG_RANGE_SURVEY_MODE,
                "milestones", object(
                        "first30Minutes", snapshotSummary(first30),
                        "first2Hours", snapshotSummary(first2Hours),
                        "surveyArrayReady", snapshotSummary(surveyArrayReady)
                ),
                "holomap", object(
                        "schema", first2HourMap.schema(),
                        "activeLayers", first2HourMap.activeLayers(),
                        "markerCount", first2HourMap.markers().size(),
                        "visibleRoutes", first2HourMap.visibleRoutes(),
                        "warnings", first2HourMap.warnings(),
                        "nextActions", first2HourMap.nextActions()
                ),
                "surveyArray", object(
                        "restored", surveyArray.restored(),
                        "reason", surveyArray.reason(),
                        "requirements", surveyArray.requirements().stream()
                                .map(GalacticSurveyRuntimePlaytestHarness::requirementStatus)
                                .toList(),
                        "rewards", surveyArray.rewards()
                ),
                "saveReload", object(
                        "equivalent", surveyArrayReady.equals(reloaded),
                        "schema", reloaded.schema(),
                        "proofCount", reloaded.completedProofs().size()
                ),
                "runtimeChecks", runtimeChecks,
                "releaseGatePreview", object(
                        "publicAlphaAllowed", releasePreview.publicAlphaAllowed(),
                        "reason", releasePreview.reason(),
                        "blockers", releasePreview.blockers(),
                        "satisfiedRuntimeGateCount", releasePreview.gates().stream()
                                .filter(GalacticSurveyRuntimeService.ReleaseGateStatus::satisfied)
                                .count(),
                        "gates", releasePreview.gates().stream()
                                .map(GalacticSurveyRuntimePlaytestHarness::releaseGateStatus)
                                .toList()
                ),
                "residualRisks", List.of(
                        "This harness executes compiled runtime services, not a visible player/client playthrough.",
                        "Manual first-30-minute, first-2-hour, Survey Array, fresh-world, save/reload, and no-crash evidence remains required.",
                        "Downloaded GitHub Release launcher install/update/repair/rollback evidence remains required."
                )
        );
    }

    private static GalacticSurveyRuntimeService.SurveySaveSnapshot reloadSnapshot(GalacticSurveyRuntimeService.SurveySaveSnapshot snapshot) {
        return new GalacticSurveyRuntimeService.SurveySaveSnapshot(
                snapshot.schema(),
                snapshot.mode(),
                snapshot.outpost(),
                snapshot.probes(),
                snapshot.catalog(),
                snapshot.routes(),
                snapshot.depots(),
                snapshot.salvage(),
                snapshot.completedProofs(),
                snapshot.activeHoloMapLayers()
        );
    }

    private static Map<String, Object> snapshotSummary(GalacticSurveyRuntimeService.SurveySaveSnapshot snapshot) {
        return object(
                "schema", snapshot.schema(),
                "outpostOnline", snapshot.outpost().surveyNetworkOnline(),
                "probeCount", snapshot.probes().size(),
                "catalogedDiscoveries", snapshot.catalog().discoveryIds().size(),
                "routeCount", snapshot.routes().size(),
                "depotCount", snapshot.depots().size(),
                "salvageCount", snapshot.salvage().size(),
                "activeHoloMapLayers", snapshot.activeHoloMapLayers(),
                "completedProofs", snapshot.completedProofs()
        );
    }

    private static Map<String, Object> requirementStatus(GalacticSurveyRuntimeService.SurveyArrayRequirementStatus status) {
        return object(
                "id", status.id(),
                "proof", status.proof(),
                "satisfied", status.satisfied(),
                "evidenceSource", status.evidenceSource()
        );
    }

    private static Map<String, Object> releaseGateStatus(GalacticSurveyRuntimeService.ReleaseGateStatus status) {
        return object(
                "id", status.id(),
                "proof", status.proof(),
                "required", status.required(),
                "satisfied", status.satisfied(),
                "evidenceSource", status.evidenceSource()
        );
    }

    private static Path outputPath(String[] args) {
        Path out = Path.of("build", "reports", "galactic-survey", "runtime-playtest.json");
        for (int index = 0; index < args.length; index += 1) {
            if ("--out".equals(args[index])) {
                out = Path.of(args[++index]);
            } else {
                throw new IllegalArgumentException("Unknown argument: " + args[index]);
            }
        }
        return out;
    }

    private static Map<String, Object> object(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("object requires key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            result.put((String) pairs[index], pairs[index + 1]);
        }
        return result;
    }

    private static String toJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value, 0);
        return builder.toString();
    }

    private static void appendJson(StringBuilder builder, Object value, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(builder, map, indent);
        } else if (value instanceof Collection<?> collection) {
            appendCollection(builder, collection, indent);
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void appendMap(StringBuilder builder, Map<?, ?> map, int indent) {
        builder.append('{');
        if (!map.isEmpty()) {
            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            for (int index = 0; index < entries.size(); index += 1) {
                Map.Entry<?, ?> entry = entries.get(index);
                builder.append('\n').append(" ".repeat(indent + 2));
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
                appendJson(builder, entry.getValue(), indent + 2);
                if (index < entries.size() - 1) {
                    builder.append(',');
                }
            }
            builder.append('\n').append(" ".repeat(indent));
        }
        builder.append('}');
    }

    private static void appendCollection(StringBuilder builder, Collection<?> collection, int indent) {
        builder.append('[');
        if (!collection.isEmpty()) {
            List<?> values = new ArrayList<>(collection);
            for (int index = 0; index < values.size(); index += 1) {
                builder.append('\n').append(" ".repeat(indent + 2));
                appendJson(builder, values.get(index), indent + 2);
                if (index < values.size() - 1) {
                    builder.append(',');
                }
            }
            builder.append('\n').append(" ".repeat(indent));
        }
        builder.append(']');
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
