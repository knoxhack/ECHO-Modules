package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoDeprecationInfo;
import com.knoxhack.echo.platformcore.EchoFeatureDescriptor;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleName;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPermission;
import com.knoxhack.echo.platformcore.EchoPermissionSet;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Set;

public final class EchoHealthConstants {
    public static final String MOD_ID = "echohealthcore";
    public static final String MOD_NAME = "ECHO: HealthCore";

    public static final EchoFeatureId FEATURE_RUNTIME_HEALTH = EchoFeatureId.of("runtime.health");
    public static final EchoFeatureId FEATURE_RUNTIME_OBSERVABILITY = EchoFeatureId.of("runtime.observability");
    public static final EchoFeatureId FEATURE_SUPPORT_BUNDLE_HEALTH = EchoFeatureId.of("support_bundle.health");

    public static final EchoHealthMetricId METRIC_MODULE_STARTUP_TIME = EchoHealthMetricId.of("module.startup_time");
    public static final EchoHealthMetricId METRIC_TICK_COST = EchoHealthMetricId.of("runtime.tick_cost");
    public static final EchoHealthMetricId METRIC_SCREEN_RENDER_COST = EchoHealthMetricId.of("screen.render_cost");
    public static final EchoHealthMetricId METRIC_PACKET_COUNT = EchoHealthMetricId.of("network.packet_count");
    public static final EchoHealthMetricId METRIC_MEMORY_PRESSURE = EchoHealthMetricId.of("runtime.memory_pressure");
    public static final EchoHealthMetricId METRIC_SAVE_DIRTY_DATA_COUNT = EchoHealthMetricId.of("save.dirty_data_count");
    public static final EchoHealthMetricId METRIC_FEATURE_ACTIVATION_TIME = EchoHealthMetricId.of("feature.activation_time");
    public static final EchoHealthMetricId METRIC_REGISTRY_COUNT = EchoHealthMetricId.of("registry.count");
    public static final EchoHealthMetricId METRIC_DEGRADED_FEATURE_COUNT = EchoHealthMetricId.of("feature.degraded_count");
    public static final EchoHealthMetricId METRIC_OPTIONAL_INTEGRATION_FAILURES = EchoHealthMetricId.of("integration.optional_failures");
    public static final EchoHealthMetricId METRIC_CRASH_CONTEXT = EchoHealthMetricId.of("runtime.crash_context");
    public static final EchoHealthMetricId METRIC_SAFE_ACTION_COUNT = EchoHealthMetricId.of("runtime.safe_action_count");

    public static final List<EchoHealthMetricId> TRACKED_METRIC_IDS = List.of(
            METRIC_MODULE_STARTUP_TIME,
            METRIC_TICK_COST,
            METRIC_SCREEN_RENDER_COST,
            METRIC_PACKET_COUNT,
            METRIC_MEMORY_PRESSURE,
            METRIC_SAVE_DIRTY_DATA_COUNT,
            METRIC_FEATURE_ACTIVATION_TIME,
            METRIC_REGISTRY_COUNT,
            METRIC_DEGRADED_FEATURE_COUNT,
            METRIC_OPTIONAL_INTEGRATION_FAILURES,
            METRIC_CRASH_CONTEXT,
            METRIC_SAFE_ACTION_COUNT
    );

    public static final List<EchoFeatureDescriptor> HEALTH_FEATURES = List.of(
            descriptor(FEATURE_RUNTIME_HEALTH, "Runtime Health", "Local-first runtime health status, module health, degraded features, crash context, and diagnostics."),
            descriptor(FEATURE_RUNTIME_OBSERVABILITY, "Runtime Observability", "Metrics, budgets, snapshots, runtime observations, and reporter contracts."),
            descriptor(FEATURE_SUPPORT_BUNDLE_HEALTH, "Support Bundle Health", "Support bundle metadata for health reports with secrets redacted by contract.")
    );

    public static final List<EchoHealthBudget> DEFAULT_BUDGETS = List.of(
            new EchoHealthBudget(METRIC_MODULE_STARTUP_TIME, "Module startup time", "Warn when a module takes too long to start.", 250.0D, 1000.0D, EchoHealthMetricUnit.MILLISECONDS, true, EchoRuntimeSide.COMMON),
            new EchoHealthBudget(METRIC_TICK_COST, "Tick cost", "Warn when runtime tick cost rises above the local budget.", 40.0D, 50.0D, EchoHealthMetricUnit.MILLISECONDS, true, EchoRuntimeSide.SERVER),
            new EchoHealthBudget(METRIC_SCREEN_RENDER_COST, "Screen render cost", "Warn when a screen render path exceeds the frame budget.", 12.0D, 20.0D, EchoHealthMetricUnit.MILLISECONDS, true, EchoRuntimeSide.CLIENT),
            new EchoHealthBudget(METRIC_MEMORY_PRESSURE, "Memory pressure", "Warn when memory pressure approaches unsafe runtime levels.", 75.0D, 90.0D, EchoHealthMetricUnit.PERCENT, true, EchoRuntimeSide.COMMON)
    );

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.HEALTH_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(FEATURE_RUNTIME_HEALTH, FEATURE_RUNTIME_OBSERVABILITY, FEATURE_SUPPORT_BUNDLE_HEALTH),
            Set.of(),
            EchoPermissionSet.of(EchoPermission.of("diagnostics.write"))
    );

    private EchoHealthConstants() {
    }

    private static EchoFeatureDescriptor descriptor(EchoFeatureId id, String name, String summary) {
        return new EchoFeatureDescriptor(
                id,
                name,
                summary,
                EchoApiStability.BETA,
                Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.CLIENT, EchoRuntimeSide.SERVER, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.COMMAND_CENTER, EchoRuntimeSide.AI_AGENT),
                Set.of(),
                EchoDeprecationInfo.notDeprecated()
        );
    }
}
