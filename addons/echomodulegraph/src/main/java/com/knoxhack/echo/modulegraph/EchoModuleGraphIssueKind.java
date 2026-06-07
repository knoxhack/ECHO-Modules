package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

public enum EchoModuleGraphIssueKind {
    MISSING_MANIFEST("missing_manifest", EchoDiagnosticSeverity.NOTICE, EchoValidationCategory.MODULE_MANIFEST),
    INVALID_MANIFEST("invalid_manifest", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_MANIFEST),
    UNSUPPORTED_SCHEMA("unsupported_schema", EchoDiagnosticSeverity.WARNING, EchoValidationCategory.SCHEMA),
    DUPLICATE_MODULE_ID("duplicate_module_id", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_MANIFEST),
    MISSING_DEPENDENCY("missing_dependency", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_DEPENDENCY),
    WRONG_DEPENDENCY_VERSION("wrong_dependency_version", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_DEPENDENCY),
    CIRCULAR_DEPENDENCY("circular_dependency", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_DEPENDENCY),
    OPTIONAL_DEPENDENCY_PRESENT("optional_dependency_present", EchoDiagnosticSeverity.INFO, EchoValidationCategory.MODULE_DEPENDENCY),
    OPTIONAL_DEPENDENCY_MISSING("optional_dependency_missing", EchoDiagnosticSeverity.NOTICE, EchoValidationCategory.MODULE_DEPENDENCY),
    FEATURE_PROVIDER_MISSING("feature_provider_missing", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.FEATURE_PROVIDER),
    FEATURE_PROVIDER_CONFLICT("feature_provider_conflict", EchoDiagnosticSeverity.WARNING, EchoValidationCategory.FEATURE_PROVIDER),
    GAME_MODE_INCOMPATIBLE("game_mode_incompatible", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.PACK_PROFILE),
    ROLE_CONFLICT("role_conflict", EchoDiagnosticSeverity.WARNING, EchoValidationCategory.MODULE_ROLE),
    CLIENT_ONLY_MODULE_IN_SERVER_PROFILE("client_only_module_in_server_profile", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.SERVER_CLIENT),
    SERVER_ONLY_MODULE_IN_CLIENT_PROFILE("server_only_module_in_client_profile", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.SERVER_CLIENT),
    UNOFFICIAL_MODULE_BLOCKED("unofficial_module_blocked", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.TRUST_LEVEL),
    TRUST_LEVEL_BLOCKED("trust_level_blocked", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.TRUST_LEVEL),
    DEPRECATED_FEATURE_USE("deprecated_feature_use", EchoDiagnosticSeverity.DEPRECATED, EchoValidationCategory.DEPRECATED_API),
    REPLACEMENT_AVAILABLE("replacement_available", EchoDiagnosticSeverity.NOTICE, EchoValidationCategory.DEPRECATED_API),
    CONFLICTING_MODULES("conflicting_modules", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_DEPENDENCY),
    STALE_DUPLICATE_JAR("stale_duplicate_jar", EchoDiagnosticSeverity.ERROR, EchoValidationCategory.MODULE_DEPENDENCY),
    NON_ECHO_JAR_DETECTED("non_echo_jar_detected", EchoDiagnosticSeverity.NOTICE, EchoValidationCategory.MODULE_MANIFEST);

    private final String serializedName;
    private final EchoDiagnosticSeverity defaultSeverity;
    private final EchoValidationCategory validationCategory;

    EchoModuleGraphIssueKind(
            String serializedName,
            EchoDiagnosticSeverity defaultSeverity,
            EchoValidationCategory validationCategory
    ) {
        this.serializedName = serializedName;
        this.defaultSeverity = defaultSeverity;
        this.validationCategory = validationCategory;
    }

    public String serializedName() {
        return serializedName;
    }

    public EchoDiagnosticSeverity defaultSeverity() {
        return defaultSeverity;
    }

    public EchoValidationCategory validationCategory() {
        return validationCategory;
    }
}
