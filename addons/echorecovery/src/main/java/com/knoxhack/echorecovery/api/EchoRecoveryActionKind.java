package com.knoxhack.echorecovery.api;

public enum EchoRecoveryActionKind {
    DISABLE_OPTIONAL_MODULE("disable_optional_module"),
    DISABLE_CINEMATIC_RENDER("disable_cinematic_render"),
    REDUCE_PARTICLES("reduce_particles"),
    USE_SAFE_UI_SCALE("use_safe_ui_scale"),
    RESET_USER_CONFIG("reset_user_config"),
    SWITCH_TO_PERFORMANCE_VARIANT("switch_to_performance_variant"),
    CREATE_SNAPSHOT("create_snapshot"),
    EXPORT_SUPPORT_BUNDLE("export_support_bundle"),
    PRESERVE_SAVES("preserve_saves"),
    RUN_VALIDATION("run_validation"),
    RESTORE_LAST_KNOWN_GOOD("restore_last_known_good"),
    DISABLE_EXPERIMENTAL_FEATURES("disable_experimental_features");

    private final String serializedName;

    EchoRecoveryActionKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
