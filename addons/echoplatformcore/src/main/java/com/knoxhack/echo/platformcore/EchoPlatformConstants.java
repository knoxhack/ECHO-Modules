package com.knoxhack.echo.platformcore;

import java.util.List;
import java.util.Set;

public final class EchoPlatformConstants {
    public static final String MOD_ID = "echoplatformcore";
    public static final String MOD_NAME = "ECHO: PlatformCore";

    public static final EchoFeatureId FEATURE_CORE = EchoFeatureId.of("echo.core");
    public static final EchoFeatureId FEATURE_PLATFORM_CONTRACTS = EchoFeatureId.of("platform.contracts");
    public static final EchoFeatureId FEATURE_PLATFORM_CAPABILITIES = EchoFeatureId.of("platform.capabilities");
    public static final EchoFeatureId FEATURE_PLATFORM_ROLES = EchoFeatureId.of("platform.roles");
    public static final EchoFeatureId FEATURE_PLATFORM_PERMISSIONS = EchoFeatureId.of("platform.permissions");
    public static final EchoFeatureId FEATURE_PLATFORM_API_STABILITY = EchoFeatureId.of("platform.api_stability");
    public static final EchoFeatureId FEATURE_PLATFORM_DEPRECATIONS = EchoFeatureId.of("platform.deprecations");
    public static final EchoFeatureId FEATURE_ADAPTER_COMPAT = EchoFeatureId.of("adapter.compat");
    public static final EchoFeatureId FEATURE_ADAPTER_NATIVE_PLANNED = EchoFeatureId.of("adapter.native_planned");
    public static final EchoFeatureId FEATURE_REPORT_CONTRACTS = EchoFeatureId.of("reports.contracts");
    public static final EchoFeatureId FEATURE_COMMANDCENTER_REPORTS = EchoFeatureId.of("commandcenter.reports");
    public static final EchoFeatureId FEATURE_LAUNCHER_STATUS = EchoFeatureId.of("launcher.status");

    public static final EchoPermission PERMISSION_DIAGNOSTICS_WRITE = EchoPermission.of("diagnostics.write");
    public static final EchoPermission PERMISSION_PACK_READ = EchoPermission.of("pack.read");
    public static final EchoPermission PERMISSION_AI_SAFE_ACTIONS = EchoPermission.of("ai.safe_actions");
    public static final EchoPermission PERMISSION_BRIDGE_SAFE_ACTIONS = EchoPermission.of("bridge.execute_safe_action");

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.PLATFORM_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(
                    FEATURE_PLATFORM_CONTRACTS,
                    FEATURE_PLATFORM_CAPABILITIES,
                    FEATURE_PLATFORM_ROLES,
                    FEATURE_PLATFORM_PERMISSIONS,
                    FEATURE_PLATFORM_API_STABILITY,
                    FEATURE_PLATFORM_DEPRECATIONS,
                    FEATURE_ADAPTER_COMPAT,
                    FEATURE_ADAPTER_NATIVE_PLANNED
            ),
            Set.of(),
            EchoPermissionSet.of(PERMISSION_DIAGNOSTICS_WRITE, PERMISSION_PACK_READ)
    );

    public static final List<EchoFeatureDescriptor> PLATFORM_FEATURES = List.of(
            descriptor(FEATURE_PLATFORM_CONTRACTS, "Platform Contracts", "Shared ECHO unit identity, feature, role, side, compatibility, and diagnostic contracts."),
            descriptor(FEATURE_PLATFORM_CAPABILITIES, "Platform Capabilities", "Capability identifiers and reports for optional integrations and runtime adapters."),
            descriptor(FEATURE_PLATFORM_ROLES, "Platform Roles", "Stable module role declarations for addons, packs, tools, UI surfaces, and future Native services."),
            descriptor(FEATURE_PLATFORM_PERMISSIONS, "Platform Permissions", "Permission identifiers for registries, resources, packs, diagnostics, AI actions, and bridges."),
            descriptor(FEATURE_PLATFORM_API_STABILITY, "Platform API Stability", "API lifecycle declarations for stable, beta, experimental, internal, deprecated, and removed contracts."),
            descriptor(FEATURE_PLATFORM_DEPRECATIONS, "Platform Deprecations", "Machine-readable deprecation and replacement hints for modules and features."),
            descriptor(FEATURE_ADAPTER_COMPAT, "Runtime Adapter Contract", "Compatibility marker for legacy hosted ECHO modules."),
            descriptor(FEATURE_ADAPTER_NATIVE_PLANNED, "ECHO Native Adapter Planned", "Compatibility marker for contracts intended to move to the future ECHO Native Platform.")
    );

    public static final List<EchoCapability> PLATFORM_CAPABILITIES = List.of(
            capability("platform.contracts", "Defines shared platform contracts.", EchoRuntimeSide.COMMON),
            capability("platform.capabilities", "Models module capabilities and missing capability reports.", EchoRuntimeSide.COMMON),
            capability("platform.permissions", "Models explicit permissions for safe automation and runtime operations.", EchoRuntimeSide.COMMON),
            capability("adapter.compat", "Marks legacy hosted compatibility.", EchoRuntimeSide.COMMON),
            capability("adapter.native_planned", "Marks future ECHO Native compatibility planning.", EchoRuntimeSide.COMMON)
    );

    public static final List<EchoPermission> PLATFORM_PERMISSIONS = List.of(
            EchoPermission.of("registry.blocks"),
            EchoPermission.of("registry.items"),
            EchoPermission.of("registry.entities"),
            EchoPermission.of("registry.sounds"),
            EchoPermission.of("registry.menus"),
            EchoPermission.of("network.clientbound"),
            EchoPermission.of("network.serverbound"),
            EchoPermission.of("world.read"),
            EchoPermission.of("world.write"),
            EchoPermission.of("player.data"),
            EchoPermission.of("save.migrate"),
            EchoPermission.of("ui.screens"),
            EchoPermission.of("ui.hud"),
            EchoPermission.of("resources.assets"),
            EchoPermission.of("resources.data"),
            EchoPermission.of("pack.read"),
            EchoPermission.of("pack.modify"),
            EchoPermission.of("diagnostics.write"),
            EchoPermission.of("ai.safe_actions"),
            EchoPermission.of("bridge.execute_safe_action")
    );

    private EchoPlatformConstants() {
    }

    private static EchoFeatureDescriptor descriptor(EchoFeatureId id, String name, String summary) {
        return new EchoFeatureDescriptor(
                id,
                name,
                summary,
                EchoApiStability.BETA,
                Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.DEV, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.COMMAND_CENTER, EchoRuntimeSide.AI_AGENT),
                Set.of(),
                EchoDeprecationInfo.notDeprecated()
        );
    }

    private static EchoCapability capability(String id, String description, EchoRuntimeSide side) {
        return new EchoCapability(EchoCapabilityId.of(id), description, side, EchoApiStability.BETA);
    }
}
