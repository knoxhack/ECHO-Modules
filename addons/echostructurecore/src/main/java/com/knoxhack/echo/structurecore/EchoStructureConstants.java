package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoStructureConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of(EchoStructureCore.MODID);
    public static final EchoFeatureId FEATURE_STRUCTURES = EchoFeatureId.of("structures.profiles");
    public static final EchoFeatureId FEATURE_POI_METADATA = EchoFeatureId.of("structures.poi_metadata");
    public static final EchoFeatureId FEATURE_DISCOVERY_REFERENCES = EchoFeatureId.of("structures.discovery_references");

    private EchoStructureConstants() {
    }
}
