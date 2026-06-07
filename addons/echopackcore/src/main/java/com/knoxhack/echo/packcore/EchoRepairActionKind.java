package com.knoxhack.echo.packcore;

public enum EchoRepairActionKind {
    DOWNLOAD_MISSING("download_missing"),
    REPLACE_CORRUPT("replace_corrupt"),
    REMOVE_STALE("remove_stale"),
    REMOVE_DUPLICATE("remove_duplicate"),
    UPDATE_MODULE("update_module"),
    DOWNGRADE_MODULE("downgrade_module"),
    BACKUP_SAVE("backup_save"),
    RUN_MIGRATION("run_migration"),
    RESET_CONFIG("reset_config"),
    SWITCH_VARIANT("switch_variant"),
    DISABLE_OPTIONAL_MODULE("disable_optional_module"),
    ENABLE_REQUIRED_MODULE("enable_required_module"),
    RESTORE_SNAPSHOT("restore_snapshot"),
    EXPORT_SUPPORT_BUNDLE("export_support_bundle"),
    OPEN_DOCS("open_docs"),
    MANUAL_REVIEW("manual_review");

    private final String serializedName;

    EchoRepairActionKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
