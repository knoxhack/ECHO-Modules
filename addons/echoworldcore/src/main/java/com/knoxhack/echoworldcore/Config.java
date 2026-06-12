package com.knoxhack.echoworldcore;

import com.echoplatform.echocore.api.config.EchoConfigCategory;
import com.echoplatform.echocore.api.config.EchoConfigEntry;
import com.echoplatform.echocore.api.config.EchoConfigModule;
import com.echoplatform.echocore.api.config.EchoConfigProvider;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;
import java.util.List;

public final class Config {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.IntValue PLAYER_SCAN_INTERVAL = BUILDER
            .comment("Ticks between server-side WorldCore region scans for each player.")
            .defineInRange("runtime.playerScanIntervalTicks", 40, 5, 400);

    public static final EchoNativeConfigSpec.IntValue ACTIVE_REGION_RADIUS = BUILDER
            .comment("Default radius used when resolving active marker-backed regions around a player.")
            .defineInRange("runtime.activeRegionRadius", 128, 16, 4096);

    public static final EchoNativeConfigSpec.IntValue MARKER_QUERY_RADIUS_CAP = BUILDER
            .comment("Maximum radius accepted by WorldCore marker and nearby-region queries.")
            .defineInRange("runtime.markerQueryRadiusCap", 4096, 64, 32768);

    public static final EchoNativeConfigSpec.BooleanValue DEBUG_COMMANDS_ENABLED = BUILDER
            .comment("Enables permission-gated /echoworld inspection and validation commands.")
            .define("debug.commandsEnabled", true);

    public static final EchoNativeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoWorldCore.MODID, () -> new EchoConfigModule(
                EchoWorldCore.MODID,
                "WorldCore",
                List.of(new EchoConfigCategory("runtime", "Runtime", List.of(
                        EchoConfigEntry.intSpec("player_scan_interval", "Player Scan Interval",
                                "Ticks between server-side WorldCore region scans for each player.",
                                EchoConfigSide.COMMON, PLAYER_SCAN_INTERVAL, 5, 400,
                                true, false, false),
                        EchoConfigEntry.intSpec("active_region_radius", "Active Region Radius",
                                "Default radius for marker-backed regions around a player.",
                                EchoConfigSide.COMMON, ACTIVE_REGION_RADIUS, 16, 4096,
                                true, false, false),
                        EchoConfigEntry.intSpec("marker_query_radius_cap", "Marker Query Radius Cap",
                                "Maximum radius accepted by WorldCore marker and nearby-region queries.",
                                EchoConfigSide.COMMON, MARKER_QUERY_RADIUS_CAP, 64, 32768,
                                true, false, false)))))));
    }

    public static int playerScanInterval() {
        return Math.max(1, PLAYER_SCAN_INTERVAL.get());
    }

    public static int activeRegionRadius() {
        return Math.max(16, ACTIVE_REGION_RADIUS.get());
    }

    public static int markerQueryRadiusCap() {
        return Math.max(64, MARKER_QUERY_RADIUS_CAP.get());
    }

    public static boolean debugCommandsEnabled() {
        return DEBUG_COMMANDS_ENABLED.get();
    }
}
