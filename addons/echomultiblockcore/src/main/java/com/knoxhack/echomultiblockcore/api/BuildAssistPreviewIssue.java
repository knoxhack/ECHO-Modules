package com.knoxhack.echomultiblockcore.api;

public record BuildAssistPreviewIssue(Kind kind, String expected, int count) {
    public BuildAssistPreviewIssue {
        kind = kind == null ? Kind.MISSING : kind;
        expected = expected == null || expected.isBlank() ? "unknown" : expected.strip();
        count = Math.max(0, count);
    }

    public String line() {
        return switch (kind) {
            case MISSING -> count + "x " + expected;
            case WRONG -> count + "x expected " + expected;
        };
    }

    public enum Kind {
        MISSING,
        WRONG
    }
}
