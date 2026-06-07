package com.knoxhack.echo.packcore;

public enum EchoSnapshotKind {
    KNOWN_GOOD("known_good"),
    BEFORE_UPDATE("before_update"),
    BEFORE_REPAIR("before_repair"),
    BEFORE_MIGRATION("before_migration"),
    USER_BACKUP("user_backup"),
    SUPPORT_EXPORT("support_export"),
    DEV_CHECKPOINT("dev_checkpoint");

    private final String serializedName;

    EchoSnapshotKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
