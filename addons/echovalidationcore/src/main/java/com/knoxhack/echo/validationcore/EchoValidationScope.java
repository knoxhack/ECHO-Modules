package com.knoxhack.echo.validationcore;

public enum EchoValidationScope {
    WORKSPACE("workspace"),
    ADDON_SET("addon_set"),
    MODULE("module"),
    PACK("pack"),
    PROFILE("profile"),
    SCHEMA_DOCUMENT("schema_document"),
    RESOURCE("resource"),
    DATA_PACK("data_pack"),
    CLIENT("client"),
    SERVER("server"),
    COMMON("common"),
    RUNTIME("runtime"),
    SAVE("save"),
    AI("ai"),
    BRIDGE("bridge"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoValidationScope(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
