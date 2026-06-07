package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleName;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPermissionSet;
import com.knoxhack.echo.platformcore.EchoPlatformConstants;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Set;

public final class EchoModuleGraphConstants {
    public static final String MOD_ID = "echomodulegraph";
    public static final String MOD_NAME = "ECHO: ModuleGraph";

    public static final EchoFeatureId FEATURE_MODULE_GRAPH = EchoFeatureId.of("module.graph");
    public static final EchoFeatureId FEATURE_MODULE_SCANNER = EchoFeatureId.of("module.scanner");
    public static final EchoFeatureId FEATURE_FEATURE_GRAPH = EchoFeatureId.of("feature.graph");
    public static final EchoFeatureId FEATURE_DEPENDENCY_RESOLVER = EchoFeatureId.of("dependency.resolver");

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.MODULE_GRAPH,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(FEATURE_MODULE_GRAPH, FEATURE_MODULE_SCANNER, FEATURE_FEATURE_GRAPH, FEATURE_DEPENDENCY_RESOLVER),
            Set.of(),
            EchoPermissionSet.of(
                    EchoPlatformConstants.PERMISSION_PACK_READ,
                    EchoPlatformConstants.PERMISSION_DIAGNOSTICS_WRITE
            )
    );

    public static final List<EchoModuleGraphIssueKind> DIAGNOSTIC_READY_ISSUES = List.of(EchoModuleGraphIssueKind.values());

    private EchoModuleGraphConstants() {
    }
}
