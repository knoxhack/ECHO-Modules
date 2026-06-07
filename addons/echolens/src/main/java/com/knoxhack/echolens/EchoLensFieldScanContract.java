package com.knoxhack.echolens;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoLensFieldScanContract {
    public static final String MODULE_ID = "echolens";
    public static final String ADAPTERCORE_CONTRACT_ID = "echolens:scanner/field_inspection";
    public static final String REFERENCE_TARGET_ID = "echoashfallprotocol:damaged_power_node";
    public static final String REFERENCE_SCAN_MODE = "machine";

    private EchoLensFieldScanContract() {
    }

    public static Map<String, Object> executeReferenceScan(String targetId, String scanMode) {
        String normalizedTarget = normalizeId(targetId);
        String normalizedMode = normalize(scanMode);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        report.put("service", "echolens:inspection_service");
        report.put("targetId", normalizedTarget);
        report.put("scanMode", normalizedMode);
        report.put("scanExecuted", true);
        report.put("providerIds", List.of(
                "echolens:target_identity",
                "echolens:machine_status",
                "echolens:integration_status"
        ));
        report.put("sections", List.of(
                section("identity", List.of(
                        row("target", normalizedTarget),
                        row("kind", "machine"),
                        row("source", "adaptercore")
                )),
                section("machine", List.of(
                        row("status", "damaged"),
                        row("power", "offline"),
                        row("repair", "requires_power_cell")
                )),
                section("links", List.of(
                        row("index", "echoindex:recipe_search/index_query"),
                        row("terminal", "echoterminal:surface/field_ops")
                ))
        ));
        report.put("diagnosticCodes", List.of("lens.machine.offline", "lens.repair.power_cell_required"));
        report.put("referenceBehavior", "field_scan_resolves_damaged_power_node");
        return Map.copyOf(report);
    }

    public static boolean referenceScanPassed(Map<String, Object> report) {
        return Boolean.TRUE.equals(report.get("scanExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(report.get("adapterCoreContract"))
                && REFERENCE_TARGET_ID.equals(report.get("targetId"))
                && list(report.get("diagnosticCodes")).contains("lens.repair.power_cell_required")
                && sections(report).stream().anyMatch(section ->
                        "machine".equals(section.get("id"))
                                && String.valueOf(section.get("rows")).contains("requires_power_cell"));
    }

    private static Map<String, Object> section(String id, List<Map<String, String>> rows) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("id", id);
        section.put("rows", List.copyOf(rows));
        return Map.copyOf(section);
    }

    private static Map<String, String> row(String key, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("value", value);
        return Map.copyOf(row);
    }

    private static String normalizeId(Object value) {
        return String.valueOf(value).toLowerCase(Locale.ROOT).trim();
    }

    private static String normalize(Object value) {
        return String.valueOf(value).toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }

    private static List<String> list(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sections(Map<String, Object> report) {
        Object value = report.get("sections");
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }
}
