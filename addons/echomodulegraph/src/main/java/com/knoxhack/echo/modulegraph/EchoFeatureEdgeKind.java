package com.knoxhack.echo.modulegraph;

public enum EchoFeatureEdgeKind {
    PROVIDES("provides"),
    CONSUMES("consumes"),
    REQUIRES("requires"),
    OPTIONAL_CONSUMES("optional_consumes"),
    CONFLICTS("conflicts"),
    REPLACES("replaces"),
    DEPRECATES("deprecates");

    private final String serializedName;

    EchoFeatureEdgeKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
