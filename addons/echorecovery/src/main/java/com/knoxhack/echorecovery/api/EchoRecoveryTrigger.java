package com.knoxhack.echorecovery.api;

public enum EchoRecoveryTrigger {
    REPEATED_LAUNCH_FAILURE("repeated_launch_failure"),
    MODULE_INIT_FAILURE("module_init_failure"),
    SCREEN_CRASH("screen_crash"),
    CLIENT_CLASSLOAD_FAILURE("client_classload_failure"),
    CONFIG_CORRUPTION("config_corruption"),
    MISSING_REQUIRED_MODULE("missing_required_module"),
    LOCKFILE_MISMATCH("lockfile_mismatch"),
    SAVE_MIGRATION_FAILURE("save_migration_failure"),
    PERFORMANCE_BUDGET_EXCEEDED("performance_budget_exceeded"),
    RENDERER_FAILURE("renderer_failure"),
    OPTIONAL_INTEGRATION_FAILURE("optional_integration_failure");

    private final String serializedName;

    EchoRecoveryTrigger(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
