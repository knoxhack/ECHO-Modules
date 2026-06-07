package com.knoxhack.echo.contentcore;

public enum EchoContentReferenceKind {
    PROVIDES("provides"),
    CONSUMES("consumes"),
    USES("uses"),
    REQUIRES("requires"),
    OPTIONAL("optional"),
    REPLACES("replaces"),
    CONFLICTS("conflicts"),
    GENERATES("generates"),
    DERIVES_FROM("derives_from"),
    TAGS("tags"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoContentReferenceKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean blockingWhenUnavailable() {
        return this == REQUIRES || this == CONFLICTS;
    }
}
