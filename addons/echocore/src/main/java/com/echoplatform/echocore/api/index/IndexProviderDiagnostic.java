package com.echoplatform.echocore.api.index;

import net.minecraft.resources.Identifier;

public record IndexProviderDiagnostic(
        Identifier providerId,
        Severity severity,
        String message,
        String detail) {
    public IndexProviderDiagnostic {
        severity = severity == null ? Severity.INFO : severity;
        message = message == null ? "" : message;
        detail = detail == null ? "" : detail;
    }

    public static IndexProviderDiagnostic info(Identifier providerId, String message) {
        return new IndexProviderDiagnostic(providerId, Severity.INFO, message, "");
    }

    public static IndexProviderDiagnostic warning(Identifier providerId, String message) {
        return new IndexProviderDiagnostic(providerId, Severity.WARNING, message, "");
    }

    public static IndexProviderDiagnostic error(Identifier providerId, String message, String detail) {
        return new IndexProviderDiagnostic(providerId, Severity.ERROR, message, detail);
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
