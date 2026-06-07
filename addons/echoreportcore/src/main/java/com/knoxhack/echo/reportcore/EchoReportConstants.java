package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.schemacore.EchoSchemaId;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EchoReportConstants {
    public static final String MOD_ID = "echoreportcore";
    public static final String SCHEMA_VERSION_1 = "1.0.0";

    public static final EchoFeatureId FEATURE_REPORT_CONTRACTS = EchoFeatureId.of("reports.contracts");
    public static final EchoFeatureId FEATURE_COMMAND_CENTER_REPORTS = EchoFeatureId.of("commandcenter.reports");
    public static final EchoFeatureId FEATURE_LAUNCHER_STATUS = EchoFeatureId.of("launcher.status");
    public static final EchoFeatureId FEATURE_SUPPORT_BUNDLES = EchoFeatureId.of("support_bundles.manifest");
    public static final EchoFeatureId FEATURE_RELEASE_READINESS = EchoFeatureId.of("release.readiness");

    public static final EchoSchemaId SCHEMA_PLATFORM_VERIFICATION = EchoSchemaId.of("echo.report.platform_verification");
    public static final EchoSchemaId SCHEMA_WORKSPACE_SCAN = EchoSchemaId.of("echo.report.workspace_scan");
    public static final EchoSchemaId SCHEMA_SCANNED_MODULES = EchoSchemaId.of("echo.report.scanned_modules");
    public static final EchoSchemaId SCHEMA_METADATA_SCAN = EchoSchemaId.of("echo.report.metadata_scan.v1");
    public static final EchoSchemaId SCHEMA_LAUNCHER_STATUS = EchoSchemaId.of("echo.report.launcher_status");
    public static final EchoSchemaId SCHEMA_PACK_READINESS = EchoSchemaId.of("echo.report.pack_readiness");
    public static final EchoSchemaId SCHEMA_PACK_PROFILE = EchoSchemaId.of("echo.report.pack_profile");
    public static final EchoSchemaId SCHEMA_MODULE_GRAPH = EchoSchemaId.of("echo.report.module_graph");
    public static final EchoSchemaId SCHEMA_DEPENDENCY_GRAPH = EchoSchemaId.of("echo.report.dependency_graph");
    public static final EchoSchemaId SCHEMA_ROLE_GRAPH = EchoSchemaId.of("echo.report.role_graph");
    public static final EchoSchemaId SCHEMA_FEATURE_GRAPH = EchoSchemaId.of("echo.report.feature_graph");
    public static final EchoSchemaId SCHEMA_DIAGNOSTICS = EchoSchemaId.of("echo.report.diagnostics");
    public static final EchoSchemaId SCHEMA_HEALTH = EchoSchemaId.of("echo.report.health");
    public static final EchoSchemaId SCHEMA_RUNTIME_HEALTH = EchoSchemaId.of("echo.report.runtime_health");
    public static final EchoSchemaId SCHEMA_DEGRADED_FEATURES = EchoSchemaId.of("echo.report.degraded_features");
    public static final EchoSchemaId SCHEMA_RECOVERY_STATE = EchoSchemaId.of("echo.report.recovery_state");
    public static final EchoSchemaId SCHEMA_RECOVERY_PLAN = EchoSchemaId.of("echo.report.recovery_plan");
    public static final EchoSchemaId SCHEMA_LOCKFILE = EchoSchemaId.of("echo.report.lockfile");
    public static final EchoSchemaId SCHEMA_INSTALL_STATE = EchoSchemaId.of("echo.report.install_state");
    public static final EchoSchemaId SCHEMA_LOCKFILE_STATUS = EchoSchemaId.of("echo.report.lockfile_status");
    public static final EchoSchemaId SCHEMA_REPAIR_PLAN = EchoSchemaId.of("echo.report.repair_plan");
    public static final EchoSchemaId SCHEMA_PACK_DOCTOR = EchoSchemaId.of("echo.report.pack_doctor");
    public static final EchoSchemaId SCHEMA_AI_TASKS = EchoSchemaId.of("echo.report.ai_tasks");
    public static final EchoSchemaId SCHEMA_PROMPT_BUNDLE = EchoSchemaId.of("echo.report.prompt_bundle");
    public static final EchoSchemaId SCHEMA_BRIDGE_SESSIONS = EchoSchemaId.of("echo.report.bridge_sessions");
    public static final EchoSchemaId SCHEMA_CODEX_RUN_REPORT = EchoSchemaId.of("echo.report.codex_run_report");
    public static final EchoSchemaId SCHEMA_MISSING_ASSETS = EchoSchemaId.of("echo.report.missing_assets");
    public static final EchoSchemaId SCHEMA_ASSET_OWNERSHIP = EchoSchemaId.of("echo.report.asset_ownership");
    public static final EchoSchemaId SCHEMA_TEXTUREFORGE_REPORT = EchoSchemaId.of("echo.report.textureforge_report");
    public static final EchoSchemaId SCHEMA_SUPPORT_BUNDLE = EchoSchemaId.of("echo.report.support_bundle");
    public static final EchoSchemaId SCHEMA_COMPATIBILITY_MATRIX = EchoSchemaId.of("echo.report.compatibility_matrix");
    public static final EchoSchemaId SCHEMA_RELEASE_READINESS = EchoSchemaId.of("echo.report.release_readiness");

    public static final List<String> COMMON_SCHEMA_FIELDS = List.of(
            "schema",
            "generatedAt",
            "generator",
            "workspace",
            "addonSet",
            "packId",
            "status",
            "summary",
            "issues",
            "data"
    );

    public static final List<EchoReportDescriptor> DEFAULT_REPORT_DESCRIPTORS = List.of(
            descriptor(EchoReportKind.PLATFORM_VERIFICATION, "Platform Verification", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI, EchoReportAudience.HUMAN), SCHEMA_PLATFORM_VERIFICATION),
            descriptor(EchoReportKind.WORKSPACE_SCAN, "Workspace Scan", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI, EchoReportAudience.HUMAN), SCHEMA_WORKSPACE_SCAN),
            descriptor(EchoReportKind.SCANNED_MODULES, "Scanned Modules", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI), SCHEMA_SCANNED_MODULES),
            descriptor(EchoReportKind.METADATA_SCAN, "Metadata Scan", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI), SCHEMA_METADATA_SCAN),
            descriptor(EchoReportKind.LAUNCHER_STATUS, "Launcher Status", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER), SCHEMA_LAUNCHER_STATUS),
            descriptor(EchoReportKind.PACK_READINESS, "Pack Readiness", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT), SCHEMA_PACK_READINESS),
            descriptor(EchoReportKind.PACK_PROFILE, "Pack Profile", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.NATIVE_CLI), SCHEMA_PACK_PROFILE),
            descriptor(EchoReportKind.MODULE_GRAPH, "Module Graph", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI), SCHEMA_MODULE_GRAPH),
            descriptor(EchoReportKind.DEPENDENCY_GRAPH, "Dependency Graph", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI), SCHEMA_DEPENDENCY_GRAPH),
            descriptor(EchoReportKind.ROLE_GRAPH, "Role Graph", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI), SCHEMA_ROLE_GRAPH),
            descriptor(EchoReportKind.FEATURE_GRAPH, "Feature Graph", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.NATIVE_CLI), SCHEMA_FEATURE_GRAPH),
            descriptor(EchoReportKind.DIAGNOSTICS, "Diagnostics", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL, EchoReportAudience.AI_AGENT), SCHEMA_DIAGNOSTICS),
            descriptor(EchoReportKind.HEALTH, "Runtime Health", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_HEALTH),
            descriptor(EchoReportKind.RUNTIME_HEALTH, "Runtime Health Snapshot", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_RUNTIME_HEALTH),
            descriptor(EchoReportKind.DEGRADED_FEATURES, "Degraded Features", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_DEGRADED_FEATURES),
            descriptor(EchoReportKind.RECOVERY_STATE, "Recovery State", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_RECOVERY_STATE),
            descriptor(EchoReportKind.RECOVERY_PLAN, "Recovery Plan", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_RECOVERY_PLAN),
            descriptor(EchoReportKind.LOCKFILE, "Pack Lockfile", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.NATIVE_CLI), SCHEMA_LOCKFILE),
            descriptor(EchoReportKind.INSTALL_STATE, "Install State", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_INSTALL_STATE),
            descriptor(EchoReportKind.LOCKFILE_STATUS, "Lockfile Status", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.NATIVE_CLI), SCHEMA_LOCKFILE_STATUS),
            descriptor(EchoReportKind.REPAIR_PLAN, "Repair Plan", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL), SCHEMA_REPAIR_PLAN),
            descriptor(EchoReportKind.PACK_DOCTOR, "Pack Doctor", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.SUPPORT_TOOL, EchoReportAudience.AI_AGENT), SCHEMA_PACK_DOCTOR),
            descriptor(EchoReportKind.AI_TASKS, "AI Tasks", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.CYBERDEX, EchoReportAudience.AI_AGENT), SCHEMA_AI_TASKS),
            descriptor(EchoReportKind.PROMPT_BUNDLE, "Prompt Bundle", Set.of(EchoReportAudience.CYBERDEX, EchoReportAudience.AI_AGENT, EchoReportAudience.HUMAN), SCHEMA_PROMPT_BUNDLE),
            descriptor(EchoReportKind.BRIDGE_SESSIONS, "Bridge Sessions", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.CYBERDEX, EchoReportAudience.AI_AGENT), SCHEMA_BRIDGE_SESSIONS),
            descriptor(EchoReportKind.CODEX_RUN_REPORT, "Codex Run Report", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.CYBERDEX, EchoReportAudience.AI_AGENT, EchoReportAudience.HUMAN), SCHEMA_CODEX_RUN_REPORT),
            descriptor(EchoReportKind.MISSING_ASSETS, "Missing Assets", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.HUMAN), SCHEMA_MISSING_ASSETS),
            descriptor(EchoReportKind.ASSET_OWNERSHIP, "Asset Ownership", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.HUMAN), SCHEMA_ASSET_OWNERSHIP),
            descriptor(EchoReportKind.TEXTUREFORGE_REPORT, "TextureForge Report", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.HUMAN), SCHEMA_TEXTUREFORGE_REPORT),
            descriptor(EchoReportKind.SUPPORT_BUNDLE, "Support Bundle Manifest", Set.of(EchoReportAudience.SUPPORT_TOOL, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.HUMAN), SCHEMA_SUPPORT_BUNDLE),
            descriptor(EchoReportKind.COMPATIBILITY_MATRIX, "Compatibility Matrix", Set.of(EchoReportAudience.LAUNCHER, EchoReportAudience.COMMAND_CENTER, EchoReportAudience.NATIVE_CLI), SCHEMA_COMPATIBILITY_MATRIX),
            descriptor(EchoReportKind.RELEASE_READINESS, "Release Readiness", Set.of(EchoReportAudience.COMMAND_CENTER, EchoReportAudience.AI_AGENT, EchoReportAudience.HUMAN), SCHEMA_RELEASE_READINESS)
    );

    public static final List<EchoReportArtifact> DEFAULT_REPORT_ARTIFACTS = Arrays.stream(EchoReportKind.values())
            .map(EchoReportArtifact::planned)
            .toList();

    public static final List<EchoReportSchemaDescriptor> DEFAULT_REPORT_SCHEMA_DESCRIPTORS = List.of(
            schemaDescriptor(EchoReportKind.PLATFORM_VERIFICATION, SCHEMA_PLATFORM_VERIFICATION, List.of(
                    field("platformVerification", EchoReportSchemaFieldType.OBJECT),
                    field("platformVerification.workspace", EchoReportSchemaFieldType.STRING),
                    field("platformVerification.addonSet", EchoReportSchemaFieldType.STRING),
                    field("platformVerification.status", EchoReportSchemaFieldType.STRING),
                    field("platformVerification.noLaunch", EchoReportSchemaFieldType.BOOLEAN),
                    field("platformVerification.noSecrets", EchoReportSchemaFieldType.BOOLEAN),
                    field("platformVerification.defaultReportArtifactsChecked", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("platformVerification.checkedModules", EchoReportSchemaFieldType.ARRAY),
                    field("summary", EchoReportSchemaFieldType.OBJECT),
                    field("summary.platformModulesChecked", EchoReportSchemaFieldType.INTEGER),
                    field("summary.contractsChecked", EchoReportSchemaFieldType.INTEGER),
                    field("summary.forbiddenImports", EchoReportSchemaFieldType.INTEGER),
                    field("summary.clientLeakage", EchoReportSchemaFieldType.INTEGER),
                    field("summary.warnings", EchoReportSchemaFieldType.INTEGER),
                    field("summary.errors", EchoReportSchemaFieldType.INTEGER),
                    field("issues", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.WORKSPACE_SCAN, SCHEMA_WORKSPACE_SCAN, List.of(
                    field("workspaceScan", EchoReportSchemaFieldType.OBJECT),
                    field("workspaceScan.workspace", EchoReportSchemaFieldType.STRING),
                    field("workspaceScan.addonSet", EchoReportSchemaFieldType.STRING),
                    field("workspaceScan.scannerMode", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("workspaceScan.inputs", EchoReportSchemaFieldType.ARRAY),
                    field("workspaceScan.selectedModules", EchoReportSchemaFieldType.INTEGER),
                    field("workspaceScan.docsFilesObserved", EchoReportSchemaFieldType.INTEGER),
                    field("summary", EchoReportSchemaFieldType.OBJECT),
                    field("summary.modulesScanned", EchoReportSchemaFieldType.INTEGER),
                    field("summary.missingEchoMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.missingAiMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.invalidMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.duplicateModuleIds", EchoReportSchemaFieldType.INTEGER),
                    field("summary.errors", EchoReportSchemaFieldType.INTEGER),
                    field("summary.warnings", EchoReportSchemaFieldType.INTEGER),
                    field("summary.notices", EchoReportSchemaFieldType.INTEGER),
                    field("issues", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.SCANNED_MODULES, SCHEMA_SCANNED_MODULES, List.of(
                    field("scannedModules", EchoReportSchemaFieldType.OBJECT),
                    field("scannedModules.workspace", EchoReportSchemaFieldType.STRING),
                    field("scannedModules.addonSet", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("scannedModules.modules", EchoReportSchemaFieldType.ARRAY),
                    field("summary", EchoReportSchemaFieldType.OBJECT),
                    field("summary.modulesScanned", EchoReportSchemaFieldType.INTEGER),
                    field("summary.inferredModules", EchoReportSchemaFieldType.INTEGER),
                    field("summary.missingEchoMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.missingAiMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("issues", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.METADATA_SCAN, SCHEMA_METADATA_SCAN, List.of(
                    field("metadataScan", EchoReportSchemaFieldType.OBJECT),
                    field("metadataScan.workspace", EchoReportSchemaFieldType.STRING),
                    field("metadataScan.addonSet", EchoReportSchemaFieldType.STRING),
                    field("metadataScan.scannerMode", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("metadataScan.modules", EchoReportSchemaFieldType.ARRAY),
                    field("summary", EchoReportSchemaFieldType.OBJECT),
                    field("summary.modulesScanned", EchoReportSchemaFieldType.INTEGER),
                    field("summary.presentEchoMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.fallbackEchoMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.missingEchoMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.invalidEchoMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.presentAiMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.missingAiMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.invalidAiMetadata", EchoReportSchemaFieldType.INTEGER),
                    field("summary.errors", EchoReportSchemaFieldType.INTEGER),
                    field("summary.warnings", EchoReportSchemaFieldType.INTEGER),
                    field("summary.notices", EchoReportSchemaFieldType.INTEGER),
                    field("issues", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.LAUNCHER_STATUS, SCHEMA_LAUNCHER_STATUS, List.of(
                    field("workspace", EchoReportSchemaFieldType.OBJECT),
                    field("launcherStatus", EchoReportSchemaFieldType.OBJECT),
                    field("launcherStatus.launchable", EchoReportSchemaFieldType.BOOLEAN),
                    nonEmptyField("launcherStatus.validatedAddonSets", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("launcherStatus.officialPacks", EchoReportSchemaFieldType.ARRAY),
                    field("launcherStatus.packStates", EchoReportSchemaFieldType.ARRAY),
                    field("launcherStatus.degradedSurfaces", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.PACK_READINESS, SCHEMA_PACK_READINESS, List.of(
                    nonEmptyField("packReadiness", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.PACK_PROFILE, SCHEMA_PACK_PROFILE, List.of(
                    field("profile", EchoReportSchemaFieldType.OBJECT),
                    field("profile.id", EchoReportSchemaFieldType.STRING),
                    field("validation", EchoReportSchemaFieldType.OBJECT),
                    field("validation.status", EchoReportSchemaFieldType.STRING),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.MODULE_GRAPH, SCHEMA_MODULE_GRAPH, List.of(
                    field("moduleGraph", EchoReportSchemaFieldType.OBJECT),
                    field("moduleGraph.graphMode", EchoReportSchemaFieldType.STRING),
                    field("moduleGraph.coreProject", EchoReportSchemaFieldType.STRING),
                    field("moduleGraph.selectedAddonSet", EchoReportSchemaFieldType.STRING),
                    field("moduleGraph.betaConfiguredAddons", EchoReportSchemaFieldType.INTEGER),
                    field("moduleGraph.releaseConfiguredAddons", EchoReportSchemaFieldType.INTEGER),
                    field("moduleGraph.uniqueConfiguredAddons", EchoReportSchemaFieldType.INTEGER),
                    field("moduleGraph.addonDirectoriesObserved", EchoReportSchemaFieldType.INTEGER),
                    field("moduleGraph.unlistedAddonDirectories", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("moduleGraph.rootBuildWiring", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("moduleGraph.importantContractRoots", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.DEPENDENCY_GRAPH, SCHEMA_DEPENDENCY_GRAPH, List.of(
                    field("dependencyGraph", EchoReportSchemaFieldType.OBJECT),
                    nonEmptyField("dependencyGraph.nodes", EchoReportSchemaFieldType.ARRAY),
                    field("dependencyGraph.edges", EchoReportSchemaFieldType.ARRAY),
                    field("dependencyGraph.cycles", EchoReportSchemaFieldType.ARRAY),
                    field("dependencyGraph.missingRequired", EchoReportSchemaFieldType.ARRAY),
                    field("dependencyGraph.optionalMissing", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.ROLE_GRAPH, SCHEMA_ROLE_GRAPH, List.of(
                    field("roleGraph", EchoReportSchemaFieldType.OBJECT),
                    field("roleGraph.roles", EchoReportSchemaFieldType.ARRAY),
                    field("roleGraph.conflicts", EchoReportSchemaFieldType.ARRAY),
                    field("roleGraph.exclusiveRolePolicy", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.FEATURE_GRAPH, SCHEMA_FEATURE_GRAPH, List.of(
                    field("featureGraph", EchoReportSchemaFieldType.OBJECT),
                    field("featureGraph.graphMode", EchoReportSchemaFieldType.STRING),
                    field("featureGraph.completedPhaseRange", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("featureGraph.families", EchoReportSchemaFieldType.ARRAY),
                    field("featureGraph.optionalSurfacePolicy", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.DIAGNOSTICS, SCHEMA_DIAGNOSTICS, List.of(
                    field("summary", EchoReportSchemaFieldType.OBJECT),
                    field("summary.blockingCount", EchoReportSchemaFieldType.INTEGER),
                    field("summary.warningCount", EchoReportSchemaFieldType.INTEGER),
                    field("summary.noticeCount", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.HEALTH, SCHEMA_HEALTH, List.of(
                    field("health", EchoReportSchemaFieldType.OBJECT),
                    field("health.runtimeLaunched", EchoReportSchemaFieldType.BOOLEAN),
                    field("health.overallStatus", EchoReportSchemaFieldType.STRING),
                    field("health.buildHealth", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("health.observations", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.RUNTIME_HEALTH, SCHEMA_RUNTIME_HEALTH, List.of(
                    field("runtimeHealth", EchoReportSchemaFieldType.OBJECT),
                    field("runtimeHealth.runtimeLaunched", EchoReportSchemaFieldType.BOOLEAN),
                    field("runtimeHealth.collectionMode", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("runtimeHealth.metrics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("runtimeHealth.observations", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.DEGRADED_FEATURES, SCHEMA_DEGRADED_FEATURES, List.of(
                    field("degradedFeatures", EchoReportSchemaFieldType.OBJECT),
                    field("degradedFeatures.packId", EchoReportSchemaFieldType.STRING),
                    field("degradedFeatures.collectionMode", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("degradedFeatures.features", EchoReportSchemaFieldType.ARRAY),
                    field("degradedFeatures.featureCount", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.RECOVERY_STATE, SCHEMA_RECOVERY_STATE, List.of(
                    field("recoveryState", EchoReportSchemaFieldType.OBJECT),
                    field("recoveryState.packId", EchoReportSchemaFieldType.STRING),
                    field("recoveryState.mode", EchoReportSchemaFieldType.STRING),
                    field("recoveryState.safeModeRecommended", EchoReportSchemaFieldType.BOOLEAN),
                    field("recoveryState.triggers", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.RECOVERY_PLAN, SCHEMA_RECOVERY_PLAN, List.of(
                    field("recoveryPlan", EchoReportSchemaFieldType.OBJECT),
                    field("recoveryPlan.packId", EchoReportSchemaFieldType.STRING),
                    field("recoveryPlan.status", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("recoveryPlan.actions", EchoReportSchemaFieldType.ARRAY),
                    field("recoveryPlan.safetyPolicy", EchoReportSchemaFieldType.OBJECT),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.LOCKFILE, SCHEMA_LOCKFILE, List.of(
                    field("lockfile", EchoReportSchemaFieldType.OBJECT),
                    field("lockfile.packId", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("lockfile.lockedModules", EchoReportSchemaFieldType.ARRAY),
                    field("lockfile.lockedFeatures", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.INSTALL_STATE, SCHEMA_INSTALL_STATE, List.of(
                    field("installState", EchoReportSchemaFieldType.OBJECT),
                    field("installState.status", EchoReportSchemaFieldType.STRING),
                    field("installState.managedTarget", EchoReportSchemaFieldType.OBJECT),
                    field("installState.installedModules", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.LOCKFILE_STATUS, SCHEMA_LOCKFILE_STATUS, List.of(
                    field("lockfileStatus", EchoReportSchemaFieldType.OBJECT),
                    field("lockfileStatus.status", EchoReportSchemaFieldType.STRING),
                    field("lockfileStatus.issueCount", EchoReportSchemaFieldType.INTEGER),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.REPAIR_PLAN, SCHEMA_REPAIR_PLAN, List.of(
                    field("repairPlan", EchoReportSchemaFieldType.OBJECT),
                    field("repairPlan.automaticActions", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("repairPlan.manualRecommendations", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.PACK_DOCTOR, SCHEMA_PACK_DOCTOR, List.of(
                    field("packDoctor", EchoReportSchemaFieldType.OBJECT),
                    field("packDoctor.status", EchoReportSchemaFieldType.STRING),
                    field("packDoctor.packId", EchoReportSchemaFieldType.STRING),
                    field("packDoctor.childReportCount", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("childReports", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.AI_TASKS, SCHEMA_AI_TASKS, List.of(
                    field("taskQueue", EchoReportSchemaFieldType.OBJECT),
                    field("taskQueue.totalTasks", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("tasks", EchoReportSchemaFieldType.ARRAY),
                    field("tasksBySource", EchoReportSchemaFieldType.OBJECT),
                    field("tasksByLane", EchoReportSchemaFieldType.OBJECT),
                    field("tasksByPriority", EchoReportSchemaFieldType.OBJECT),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.PROMPT_BUNDLE, SCHEMA_PROMPT_BUNDLE, List.of(
                    field("promptBundle", EchoReportSchemaFieldType.OBJECT),
                    field("promptBundle.id", EchoReportSchemaFieldType.STRING),
                    nonEmptyField("promptBundle.sections", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("selectedTasks", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("reportInputs", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.BRIDGE_SESSIONS, SCHEMA_BRIDGE_SESSIONS, List.of(
                    field("bridgeSessions", EchoReportSchemaFieldType.OBJECT),
                    field("bridgeSessions.bridgeName", EchoReportSchemaFieldType.STRING),
                    field("bridgeSessions.localOnly", EchoReportSchemaFieldType.BOOLEAN),
                    field("bridgeSessions.executorStatus", EchoReportSchemaFieldType.STRING),
                    field("bridgeSessions.sessions", EchoReportSchemaFieldType.ARRAY),
                    field("bridgeSessions.jobs", EchoReportSchemaFieldType.ARRAY),
                    field("bridgeSessions.safeActionRequests", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("bridgeSessions.reportInputs", EchoReportSchemaFieldType.ARRAY),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.CODEX_RUN_REPORT, SCHEMA_CODEX_RUN_REPORT, List.of(
                    field("codexRunReport", EchoReportSchemaFieldType.OBJECT),
                    field("codexRunReport.id", EchoReportSchemaFieldType.STRING),
                    field("codexRunReport.status", EchoReportSchemaFieldType.STRING),
                    field("codexRunReport.localOnly", EchoReportSchemaFieldType.BOOLEAN),
                    field("codexRunReport.redacted", EchoReportSchemaFieldType.BOOLEAN),
                    field("codexRunReport.taskResults", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("codexRunReport.validationCommands", EchoReportSchemaFieldType.ARRAY),
                    field("codexRunReport.nextPhasePrompt", EchoReportSchemaFieldType.OBJECT),
                    field("diagnostics", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.MISSING_ASSETS, SCHEMA_MISSING_ASSETS, List.of(
                    field("assetSummary", EchoReportSchemaFieldType.OBJECT),
                    field("assetSummary.moduleMetadataFilesObserved", EchoReportSchemaFieldType.INTEGER),
                    field("assetSummary.moduleAssetDirectoriesObserved", EchoReportSchemaFieldType.INTEGER),
                    field("assetSummary.officialPackAssetDirectoriesObserved", EchoReportSchemaFieldType.INTEGER),
                    field("assetSummary.deepAssetValidationPerformed", EchoReportSchemaFieldType.BOOLEAN),
                    nonEmptyField("findings", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.ASSET_OWNERSHIP, SCHEMA_ASSET_OWNERSHIP, List.of(
                    field("assetOwnership", EchoReportSchemaFieldType.OBJECT),
                    field("assetOwnership.workspace", EchoReportSchemaFieldType.STRING),
                    field("assetOwnership.addonSet", EchoReportSchemaFieldType.STRING),
                    field("assetOwnership.totalAssets", EchoReportSchemaFieldType.INTEGER),
                    field("assetOwnership.missingAssets", EchoReportSchemaFieldType.INTEGER),
                    field("assetOwnership.blockingAssets", EchoReportSchemaFieldType.INTEGER),
                    field("assetOwnership.ownerCount", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("ownership", EchoReportSchemaFieldType.ARRAY),
                    field("summaryByOwner", EchoReportSchemaFieldType.OBJECT),
                    field("summaryByReleaseImpact", EchoReportSchemaFieldType.OBJECT),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.TEXTUREFORGE_REPORT, SCHEMA_TEXTUREFORGE_REPORT, List.of(
                    field("textureForge", EchoReportSchemaFieldType.OBJECT),
                    field("textureForge.available", EchoReportSchemaFieldType.BOOLEAN),
                    field("textureForge.requiresMinecraftLaunch", EchoReportSchemaFieldType.BOOLEAN),
                    field("textureForge.executesImageGeneration", EchoReportSchemaFieldType.BOOLEAN),
                    field("textureForge.executesAssetWrites", EchoReportSchemaFieldType.BOOLEAN),
                    nonEmptyField("textureForge.sourceReports", EchoReportSchemaFieldType.ARRAY),
                    field("textureForge.totalScannedAddons", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.totalSpecs", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.totalMissingAssets", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.missingTextures", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.missingModels", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.missingBlockstates", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.wrongSizeTextures", EchoReportSchemaFieldType.INTEGER),
                    field("textureForge.unusedTextures", EchoReportSchemaFieldType.INTEGER),
                    field("promptExports", EchoReportSchemaFieldType.OBJECT),
                    field("promptExports.markdownPath", EchoReportSchemaFieldType.STRING),
                    field("promptExports.sourcePromptPath", EchoReportSchemaFieldType.STRING),
                    field("promptExports.promptCount", EchoReportSchemaFieldType.INTEGER),
                    field("promptExports.fallbackPromptsUsed", EchoReportSchemaFieldType.BOOLEAN),
                    field("missingCoverage", EchoReportSchemaFieldType.OBJECT),
                    field("missingCoverage.findingsCovered", EchoReportSchemaFieldType.INTEGER),
                    field("missingCoverage.textureForgeFindings", EchoReportSchemaFieldType.INTEGER),
                    field("missingCoverage.blockingFindings", EchoReportSchemaFieldType.INTEGER),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.SUPPORT_BUNDLE, SCHEMA_SUPPORT_BUNDLE, List.of(
                    field("supportBundle", EchoReportSchemaFieldType.OBJECT),
                    field("supportBundle.manifestOnly", EchoReportSchemaFieldType.BOOLEAN),
                    nonEmptyField("supportBundle.allowedArtifacts", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("supportBundle.excludedByDefault", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.COMPATIBILITY_MATRIX, SCHEMA_COMPATIBILITY_MATRIX, List.of(
                    field("compatibility", EchoReportSchemaFieldType.OBJECT),
                    field("compatibility.javaTarget", EchoReportSchemaFieldType.STRING),
                    field("compatibility.minecraftVersion", EchoReportSchemaFieldType.STRING),
                    field("compatibility.minecraftVersionRange", EchoReportSchemaFieldType.STRING),
                    field("compatibility.loader", EchoReportSchemaFieldType.STRING),
                    field("compatibility.neoForgeVersion", EchoReportSchemaFieldType.STRING),
                    field("compatibility.nativeLoaderSupport", EchoReportSchemaFieldType.STRING),
                    field("compatibility.fabricImportsDetected", EchoReportSchemaFieldType.BOOLEAN),
                    field("compatibility.forgeImportsDetected", EchoReportSchemaFieldType.BOOLEAN),
                    nonEmptyField("compatibility.validatedAddonSets", EchoReportSchemaFieldType.ARRAY),
                    field("compatibility.blockingIssues", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            )),
            schemaDescriptor(EchoReportKind.RELEASE_READINESS, SCHEMA_RELEASE_READINESS, List.of(
                    field("workspace", EchoReportSchemaFieldType.OBJECT),
                    field("addonSetCoverage", EchoReportSchemaFieldType.OBJECT),
                    field("addonSetCoverage.betaAddons", EchoReportSchemaFieldType.INTEGER),
                    field("addonSetCoverage.releaseAddons", EchoReportSchemaFieldType.INTEGER),
                    field("addonSetCoverage.uniqueConfiguredAddons", EchoReportSchemaFieldType.INTEGER),
                    field("addonSetCoverage.addonDirectoriesObserved", EchoReportSchemaFieldType.INTEGER),
                    field("addonSetCoverage.unlistedAddonDirectories", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("buildResults", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("staticChecks", EchoReportSchemaFieldType.ARRAY),
                    field("blockingIssues", EchoReportSchemaFieldType.ARRAY),
                    field("nonBlockingFindings", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("reportArtifacts", EchoReportSchemaFieldType.ARRAY),
                    nonEmptyField("relatedDocs", EchoReportSchemaFieldType.ARRAY)
            ))
    );

    private static final Map<EchoReportKind, EchoReportDescriptor> DESCRIPTORS_BY_KIND = DEFAULT_REPORT_DESCRIPTORS.stream()
            .collect(Collectors.toUnmodifiableMap(EchoReportDescriptor::kind, Function.identity()));
    private static final Map<EchoReportKind, EchoReportSchemaDescriptor> SCHEMA_DESCRIPTORS_BY_KIND = DEFAULT_REPORT_SCHEMA_DESCRIPTORS.stream()
            .collect(Collectors.toUnmodifiableMap(EchoReportSchemaDescriptor::kind, Function.identity()));

    private EchoReportConstants() {
    }

    public static EchoReportDescriptor descriptor(EchoReportKind kind) {
        return DESCRIPTORS_BY_KIND.get(kind);
    }

    public static EchoReportSchemaDescriptor schemaDescriptor(EchoReportKind kind) {
        return SCHEMA_DESCRIPTORS_BY_KIND.get(kind);
    }

    private static EchoReportDescriptor descriptor(EchoReportKind kind, String title, Set<EchoReportAudience> audiences, EchoSchemaId schemaId) {
        return new EchoReportDescriptor(
                EchoReportId.of(kind.serializedName()),
                kind,
                title,
                "Deterministic " + title.toLowerCase(java.util.Locale.ROOT) + " report for ECHO tooling.",
                kind.defaultOutputPath(),
                EchoReportFormat.JSON,
                audiences,
                schemaId,
                Set.of(FEATURE_REPORT_CONTRACTS),
                true,
                true,
                false,
                false,
                kind == EchoReportKind.SUPPORT_BUNDLE ? EchoReportRedactionPolicy.supportBundleDefault() : EchoReportRedactionPolicy.localDefault(),
                Set.of("docs/echo/tooling/ECHO_REPORT_FORMATS.md"),
                Map.of("side", EchoRuntimeSide.COMMON.serializedName())
        );
    }

    private static EchoReportSchemaDescriptor schemaDescriptor(EchoReportKind kind, EchoSchemaId schemaId, List<EchoReportSchemaField> reportFields) {
        return new EchoReportSchemaDescriptor(
                kind,
                schemaId,
                SCHEMA_VERSION_1,
                "docs/echo/tooling/ECHO_REPORT_FORMATS.md",
                COMMON_SCHEMA_FIELDS,
                dataScopedFields(reportFields),
                true,
                true,
                false,
                false
        );
    }

    private static List<EchoReportSchemaField> dataScopedFields(List<EchoReportSchemaField> reportFields) {
        return reportFields.stream()
                .filter(field -> !field.path().equals("summary") && !field.path().startsWith("summary.") && !field.path().equals("issues"))
                .map(field -> new EchoReportSchemaField(
                        field.path().startsWith("data.") ? field.path() : "data." + field.path(),
                        field.type(),
                        field.required(),
                        field.nonEmpty(),
                        field.summary()
                ))
                .toList();
    }

    private static EchoReportSchemaField field(String path, EchoReportSchemaFieldType type) {
        return EchoReportSchemaField.required(path, type, "Required " + path + " field.");
    }

    private static EchoReportSchemaField nonEmptyField(String path, EchoReportSchemaFieldType type) {
        return EchoReportSchemaField.requiredNonEmpty(path, type, "Required non-empty " + path + " field.");
    }
}
