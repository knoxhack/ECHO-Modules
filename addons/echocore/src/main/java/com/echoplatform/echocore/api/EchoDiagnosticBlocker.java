package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoDiagnosticBlocker(
        Identifier id,
        String chapterId,
        Severity severity,
        String title,
        String detail,
        String nextAction) {
    public EchoDiagnosticBlocker {
        chapterId = chapterId == null ? "" : chapterId;
        severity = severity == null ? Severity.INFO : severity;
        title = title == null ? "" : title;
        detail = detail == null ? "" : detail;
        nextAction = nextAction == null ? "" : nextAction;
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL,
        BLOCKED
    }
}
