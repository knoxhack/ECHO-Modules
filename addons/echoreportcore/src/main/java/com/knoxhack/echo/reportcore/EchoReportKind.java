package com.knoxhack.echo.reportcore;

public enum EchoReportKind {
    PLATFORM_VERIFICATION("platform_verification", "reports/echo/platform-verification.json"),
    WORKSPACE_SCAN("workspace_scan", "reports/echo/workspace-scan.json"),
    SCANNED_MODULES("scanned_modules", "reports/echo/scanned-modules.json"),
    METADATA_SCAN("metadata_scan", "reports/echo/metadata-scan.json"),
    LAUNCHER_STATUS("launcher_status", "reports/echo/launcher-status.json"),
    PACK_READINESS("pack_readiness", "reports/echo/pack-readiness.json"),
    PACK_PROFILE("pack_profile", "reports/echo/pack-profile.json"),
    MODULE_GRAPH("module_graph", "reports/echo/module-graph.json"),
    DEPENDENCY_GRAPH("dependency_graph", "reports/echo/dependency-graph.json"),
    ROLE_GRAPH("role_graph", "reports/echo/role-graph.json"),
    FEATURE_GRAPH("feature_graph", "reports/echo/feature-graph.json"),
    DIAGNOSTICS("diagnostics", "reports/echo/diagnostics.json"),
    HEALTH("health", "reports/echo/health.json"),
    RUNTIME_HEALTH("runtime_health", "reports/echo/runtime-health.json"),
    DEGRADED_FEATURES("degraded_features", "reports/echo/degraded-features.json"),
    RECOVERY_STATE("recovery_state", "reports/echo/recovery-state.json"),
    RECOVERY_PLAN("recovery_plan", "reports/echo/recovery-plan.json"),
    LOCKFILE("lockfile", "reports/echo/lockfile.json"),
    INSTALL_STATE("install_state", "reports/echo/install-state.json"),
    LOCKFILE_STATUS("lockfile_status", "reports/echo/lockfile-status.json"),
    REPAIR_PLAN("repair_plan", "reports/echo/repair-plan.json"),
    PACK_DOCTOR("pack_doctor", "reports/echo/pack-doctor.json"),
    AI_TASKS("ai_tasks", "reports/echo/ai-tasks.json"),
    PROMPT_BUNDLE("prompt_bundle", "reports/echo/prompt-bundle.json"),
    BRIDGE_SESSIONS("bridge_sessions", "reports/echo/bridge-sessions.json"),
    CODEX_RUN_REPORT("codex_run_report", "reports/echo/codex-run-report.json"),
    MISSING_ASSETS("missing_assets", "reports/echo/missing-assets.json"),
    ASSET_OWNERSHIP("asset_ownership", "reports/echo/asset-ownership.json"),
    TEXTUREFORGE_REPORT("textureforge_report", "reports/echo/textureforge-report.json"),
    SUPPORT_BUNDLE("support_bundle", "reports/echo/support-bundle.json"),
    COMPATIBILITY_MATRIX("compatibility_matrix", "reports/echo/compatibility-matrix.json"),
    RELEASE_READINESS("release_readiness", "reports/echo/release-readiness.json");

    private final String serializedName;
    private final String defaultOutputPath;

    EchoReportKind(String serializedName, String defaultOutputPath) {
        this.serializedName = serializedName;
        this.defaultOutputPath = defaultOutputPath;
    }

    public String serializedName() {
        return serializedName;
    }

    public String defaultOutputPath() {
        return defaultOutputPath;
    }
}
