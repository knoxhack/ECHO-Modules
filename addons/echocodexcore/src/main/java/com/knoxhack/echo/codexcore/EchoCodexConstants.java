package com.knoxhack.echo.codexcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoCodexConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echocodexcore");
    public static final EchoFeatureId FEATURE_CODEX_ARCHIVE = EchoFeatureId.of("codex.archive");
    public static final EchoFeatureId FEATURE_CODEX_SEARCH = EchoFeatureId.of("codex.search");
    public static final EchoFeatureId FEATURE_CODEX_DISCOVERY = EchoFeatureId.of("codex.discovery");

    private EchoCodexConstants() {
    }
}
