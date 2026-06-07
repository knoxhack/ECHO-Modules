package com.knoxhack.echogalacticcore;

import java.util.List;

public final class GalacticCoreIds {
    public static final String MOD_ID = "echogalacticcore";
    public static final String MOD_NAME = "ECHO: GalacticCore";
    public static final String VERSION = "0.1.0-native-alpha";
    public static final String NATIVE_ENTRYPOINT = "com.knoxhack.echogalacticcore.EchoGalacticCoreNativeModule";

    public static final List<String> OPTIONAL_INTEGRATIONS = List.of(
            "echopackcore",
            "echoindex",
            "echolens",
            "echoholomap",
            "echoscreencore",
            "echoashfallprotocol",
            "echoterminal",
            "echosoundcore",
            "echorendercore",
            "echoatmospherecore",
            "echopowercore",
            "echomachinecore",
            "echovehiclecore"
    );

    private GalacticCoreIds() {
    }

    public static String id(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return MOD_ID + ":" + path.trim();
    }
}
