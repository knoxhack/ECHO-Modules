package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationTarget;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoRuntimeMutationLedger {
    private static final EchoRuntimeMutationLedger GLOBAL = new EchoRuntimeMutationLedger();

    private final List<NativeMutationLedgerEntry> entries = new ArrayList<>();

    public static EchoRuntimeMutationLedger global() {
        return GLOBAL;
    }

    public synchronized NativeMutationLedgerEntry append(
            String actionId,
            String runtimeHostId,
            Map<String, Object> inputPayload,
            NativeMutationTarget target,
            Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary,
            NativeResult result,
            boolean saveTouched,
            boolean hudOrEventEmitted) {
        NativeMutationLedgerEntry entry = new NativeMutationLedgerEntry(
                actionId,
                runtimeHostId,
                inputPayload,
                target,
                beforeSummary,
                afterSummary,
                result == null ? EchoNativeRuntimeHost.NativeResultStatus.FAILED : result.resultStatus(),
                result == null ? "missing runtime result" : result.failureReason(),
                saveTouched,
                hudOrEventEmitted,
                result == null ? null : result.receipt());
        entries.add(entry);
        return entry;
    }

    public synchronized List<NativeMutationLedgerEntry> entries() {
        return List.copyOf(entries);
    }

    public synchronized Optional<NativeMutationLedgerEntry> latest() {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(entries.size() - 1));
    }

    public synchronized List<Map<String, Object>> snapshots() {
        return entries.stream()
                .map(NativeMutationLedgerEntry::snapshot)
                .toList();
    }

    public synchronized void clear() {
        entries.clear();
    }
}
