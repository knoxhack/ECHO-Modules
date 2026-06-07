package com.knoxhack.echo.adaptercore.smoke;

import com.knoxhack.echo.adaptercore.EchoModuleSpineBusPublisher;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smoke test for {@link EchoModuleSpineBusPublisher}.
 */
public final class EchoModuleSpineBusPublisherSmoke {
    private EchoModuleSpineBusPublisherSmoke() {
    }

    public static void main(String[] args) {
        Map<String, Object> report = capture();
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new AssertionError("EchoModuleSpineBusPublisherSmoke failed: " + report);
        }
        System.out.println("echo module spine bus publisher smoke PASS ledgerEntries="
                + report.get("ledgerEntries") + " resultStatus=" + report.get("resultStatus"));
    }

    public static Map<String, Object> capture() {
        EchoRuntimeMutationLedger ledger = EchoRuntimeMutationLedger.global();
        ledger.clear();

        EchoModuleSpineBusPublisher publisher = EchoModuleSpineBusPublisher.forModule("echotest");
        NativeResult result = publisher.publishEvent(
                "test.event",
                "test-player",
                "test-target",
                1,
                Map.of("key", "value"));

        int ledgerSize = ledger.entries().size();
        boolean passed = ledgerSize >= 1 && result != null;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.module_spine_bus_publisher_smoke.v1");
        report.put("passed", passed);
        report.put("ledgerEntries", ledgerSize);
        report.put("resultStatus", result == null ? "null" : result.resultStatus().name());
        return Map.copyOf(report);
    }
}
