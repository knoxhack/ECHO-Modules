package com.knoxhack.echo.modulegraph;

public enum EchoModuleGraphEdgeKind {
    REQUIRES("requires"),
    OPTIONAL("optional"),
    PROVIDES_FEATURE("provides_feature"),
    CONSUMES_FEATURE("consumes_feature"),
    REPLACES("replaces"),
    CONFLICTS("conflicts"),
    DEPRECATED_BY("deprecated_by"),
    PACK_INCLUDES("pack_includes");

    private final String serializedName;

    EchoModuleGraphEdgeKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
