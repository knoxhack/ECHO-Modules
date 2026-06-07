package com.knoxhack.echo.validationcore;

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

import java.util.Set;

public final class EchoValidationConstants {
    public static final String MOD_ID = "echovalidationcore";
    public static final String MOD_NAME = "ECHO: ValidationCore";

    public static final EchoFeatureId FEATURE_DIAGNOSTICS = EchoFeatureId.of("validation.diagnostics");
    public static final EchoFeatureId FEATURE_VALIDATION_ENGINE = EchoFeatureId.of("validation.engine");
    public static final EchoFeatureId FEATURE_VALIDATION_JSON = EchoFeatureId.of("validation.json_report");
    public static final EchoFeatureId FEATURE_REPAIR_SUGGESTIONS = EchoFeatureId.of("validation.repair_suggestions");

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.VALIDATION_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(
                    FEATURE_DIAGNOSTICS,
                    FEATURE_VALIDATION_ENGINE,
                    FEATURE_VALIDATION_JSON,
                    FEATURE_REPAIR_SUGGESTIONS
            ),
            Set.of(),
            EchoPermissionSet.of(
                    EchoPlatformConstants.PERMISSION_DIAGNOSTICS_WRITE,
                    EchoPlatformConstants.PERMISSION_AI_SAFE_ACTIONS,
                    EchoPlatformConstants.PERMISSION_BRIDGE_SAFE_ACTIONS
            )
    );

    private EchoValidationConstants() {
    }
}
