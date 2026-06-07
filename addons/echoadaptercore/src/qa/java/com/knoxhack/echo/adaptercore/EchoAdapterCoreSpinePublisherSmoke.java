package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Smoke test for {@link EchoAdapterCoreSpinePublisher}.
 *
 * <p>Acceptance: publishing a spine event through the truth-layer publisher
 * returns a {@link NativeResult} with truthful status and records a ledger entry.
 */
public final class EchoAdapterCoreSpinePublisherSmoke {
    private static final String RUNTIME_HOST_ID = EchoAdapterCoreSpinePublisher.RUNTIME_HOST_ID;

    private EchoAdapterCoreSpinePublisherSmoke() {
    }

    public static void main(String[] args) {
        Map<String, Object> report = capture();
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new AssertionError("EchoAdapterCoreSpinePublisherSmoke failed: " + report);
        }
        System.out.println("echo adaptercore spine publisher smoke PASS ledgerEntries="
                + report.get("ledgerEntryCount"));
    }

    public static Map<String, Object> capture() {
        // Use the global dispatcher/ledger so the publisher's internal
        // global() reference matches the test infrastructure.
        EchoRuntimeMutationLedger ledger = EchoRuntimeMutationLedger.global();
        ledger.clear();

        // Register the spine publisher on the global dispatcher
        EchoAdapterCoreSpinePublisher.register();

        // Publish a test event
        EchoAdapterCoreSpinePublisher publisher = EchoAdapterCoreSpinePublisher.instance();
        publisher.publish(
                "echomissioncore",
                EchoCanonicalContentIds.EVENT_PLAYER_ITEM_COLLECTED,
                "player-smoke-1",
                EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE,
                1,
                Map.of("source", "spine_publisher_smoke"),
                "spine-smoke-1");

        List<NativeMutationLedgerEntry> entries = ledger.entries();
        boolean hasEntry = !entries.isEmpty();
        NativeResultStatus status = hasEntry ? entries.get(0).resultStatus() : NativeResultStatus.FAILED;

        boolean publisherReturnedQueuedOrNoop = status == NativeResultStatus.NOOP
                || status == NativeResultStatus.QUEUED
                || status == NativeResultStatus.UNSUPPORTED;

        boolean ledgerRecorded = hasEntry && entries.get(0).actionId().contains("spine.event");

        boolean passed = publisherReturnedQueuedOrNoop && ledgerRecorded;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.adaptercore.spine_publisher_smoke.v1");
        report.put("passed", passed);
        report.put("ledgerEntryCount", entries.size());
        report.put("firstEntryStatus", status.name());
        report.put("firstEntryActionId", hasEntry ? entries.get(0).actionId() : "");
        report.put("publisherReturnedQueuedOrNoop", publisherReturnedQueuedOrNoop);
        report.put("ledgerRecorded", ledgerRecorded);
        return Map.copyOf(report);
    }
}
