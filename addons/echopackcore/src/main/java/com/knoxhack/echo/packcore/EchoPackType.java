package com.knoxhack.echo.packcore;

public enum EchoPackType {
    OFFICIAL_PACK("official_pack"),
    COMMUNITY_PACK("community_pack"),
    LOCAL_PACK("local_pack"),
    DEV_PACK("dev_pack"),
    SERVER_PACK("server_pack"),
    CUSTOM_PACK("custom_pack");

    private final String serializedName;

    EchoPackType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
