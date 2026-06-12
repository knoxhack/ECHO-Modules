package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record WorldCoreValidationIssue(
        Identifier id,
        Severity severity,
        String category,
        String message) {
    public WorldCoreValidationIssue {
        severity = severity == null ? Severity.WARNING : severity;
        category = category == null ? "" : category;
        message = message == null ? "" : message;
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
