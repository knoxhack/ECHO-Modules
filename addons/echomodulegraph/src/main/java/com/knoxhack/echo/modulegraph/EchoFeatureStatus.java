package com.knoxhack.echo.modulegraph;

public enum EchoFeatureStatus {
    PROVIDED("provided"),
    MISSING_REQUIRED("missing_required"),
    MISSING_OPTIONAL("missing_optional"),
    CONFLICTED("conflicted"),
    DEPRECATED("deprecated"),
    REPLACED("replaced"),
    DISABLED_BY_PACK("disabled_by_pack"),
    SIDE_INCOMPATIBLE("side_incompatible"),
    TRUST_BLOCKED("trust_blocked"),
    UNUSED("unused"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoFeatureStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
