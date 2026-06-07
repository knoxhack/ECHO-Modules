package com.knoxhack.signalos.api;

import net.minecraft.resources.Identifier;

/**
 * Lightweight health row for SignalOS providers shown in diagnostics surfaces.
 */
public record SignalOsProviderStatus(
        Identifier id,
        String label,
        String status,
        TerminalDiagnosticProvider.Severity severity,
        String detail) {
    public SignalOsProviderStatus {
        id = TerminalIds.requireLowercase(id, "SignalOS provider status");
        label = label == null || label.isBlank() ? id.getPath() : label.strip();
        status = status == null || status.isBlank() ? "ONLINE" : status.strip().toUpperCase(java.util.Locale.ROOT);
        severity = severity == null ? TerminalDiagnosticProvider.Severity.INFO : severity;
        detail = detail == null ? "" : detail.strip();
    }

    public static SignalOsProviderStatus online(Identifier id, String label) {
        return new SignalOsProviderStatus(id, label, "ONLINE", TerminalDiagnosticProvider.Severity.INFO, "");
    }
}
