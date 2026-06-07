package com.knoxhack.echo.healthcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoHealthCoreRuntimeReportContract {
    public static final String MODULE_ID = "echohealthcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echohealthcore:diagnostics/runtime_health_report";
    public static final String REFERENCE_REPORT_ID = "echohealthcore:report/native_bootstrap";
    public static final String REFERENCE_RUNTIME_NAME = "echo-native-loader";

    private EchoHealthCoreRuntimeReportContract() {
    }

    public static Map<String, Object> executeReferenceReport(String packId, String runtimeName) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        report.put("service", "echohealthcore:health_reporter");
        report.put("reportExecuted", true);
        report.put("reportId", REFERENCE_REPORT_ID);
        report.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        report.put("runtimeName", runtimeName == null || runtimeName.isBlank() ? REFERENCE_RUNTIME_NAME : runtimeName);
        report.put("runtimeVersion", "m17-contract");
        report.put("status", "OK");
        report.put("metrics", List.of(
                metric("echohealthcore:runtime_status", "status", 1, "OK"),
                metric("echohealthcore:module_health", "modules", 3, "OK"),
                metric("echohealthcore:budget_violation", "violations", 0, "OK")
        ));
        report.put("modules", List.of(
                module("echoadaptercore", "OK", "AdapterCore contract registry reachable"),
                module("echoscreencore", "OK", "ScreenCore composition service executed"),
                module(MODULE_ID, "OK", "Health reporter service executed")
        ));
        report.put("observations", List.of(
                observation("diagnostics.snapshot", "OK", "Health snapshot captured for native bootstrap"),
                observation("runtime.budget", "OK", "No runtime budget violations observed"),
                observation("crash.boundary", "OK", "No crash context present")
        ));
        report.put("diagnostics", List.of("health.report.ready", "health.modules.ok", "health.local_only"));
        report.put("supportBundle", Map.of(
                "bundleId", "echohealthcore:support/native_bootstrap",
                "localOnly", true,
                "containsCrashContext", false
        ));
        report.put("localOnly", true);
        report.put("referenceBehavior", "healthcore_writes_runtime_health_report");
        return Map.copyOf(report);
    }

    public static boolean referenceReportPassed(Map<String, Object> report) {
        return Boolean.TRUE.equals(report.get("reportExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(report.get("adapterCoreContract"))
                && REFERENCE_REPORT_ID.equals(report.get("reportId"))
                && "OK".equals(report.get("status"))
                && Boolean.TRUE.equals(report.get("localOnly"))
                && String.valueOf(report.get("metrics")).contains("echohealthcore:runtime_status")
                && String.valueOf(report.get("modules")).contains(MODULE_ID)
                && String.valueOf(report.get("observations")).contains("diagnostics.snapshot")
                && String.valueOf(report.get("supportBundle")).contains("containsCrashContext=false");
    }

    private static Map<String, Object> metric(String id, String unit, int value, String status) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("id", id);
        metric.put("unit", unit);
        metric.put("value", value);
        metric.put("status", status);
        return Map.copyOf(metric);
    }

    private static Map<String, String> module(String id, String status, String summary) {
        Map<String, String> module = new LinkedHashMap<>();
        module.put("id", id);
        module.put("status", status);
        module.put("summary", summary);
        return Map.copyOf(module);
    }

    private static Map<String, String> observation(String id, String status, String summary) {
        Map<String, String> observation = new LinkedHashMap<>();
        observation.put("id", id);
        observation.put("status", status);
        observation.put("summary", summary);
        return Map.copyOf(observation);
    }
}
