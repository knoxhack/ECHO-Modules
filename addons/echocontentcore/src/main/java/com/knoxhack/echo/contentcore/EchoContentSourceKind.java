package com.knoxhack.echo.contentcore;

public enum EchoContentSourceKind {
    JAVA_REGISTRATION("java_registration"),
    DATAPACK("datapack"),
    RESOURCE_PACK("resource_pack"),
    GENERATED_RESOURCE("generated_resource"),
    CONFIG("config"),
    SAVE_DATA("save_data"),
    METADATA("metadata"),
    EXTERNAL_TOOL("external_tool"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoContentSourceKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
