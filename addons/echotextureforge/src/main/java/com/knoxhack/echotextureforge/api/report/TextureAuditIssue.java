package com.knoxhack.echotextureforge.api.report;

import com.knoxhack.echotextureforge.api.spec.TextureKind;

public record TextureAuditIssue(
        TextureAuditSeverity severity,
        String code,
        String namespace,
        String assetId,
        TextureKind assetKind,
        String path,
        String message) {
    public TextureAuditIssue {
        severity = severity == null ? TextureAuditSeverity.INFO : severity;
        code = clean(code, "INFO");
        namespace = clean(namespace, "");
        assetId = clean(assetId, "");
        path = clean(path, "");
        message = clean(message, "");
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
