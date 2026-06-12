package com.knoxhack.echopowergrid.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class PowerGridConfig {
    public static final EchoNativeConfigSpec SPEC;

    public static final EchoNativeConfigSpec.BooleanValue ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_LOGGING;

    public static final EchoNativeConfigSpec.IntValue MAX_CABLE_SCAN_PER_TICK;
    public static final EchoNativeConfigSpec.IntValue NETWORK_REBUILD_BATCH_SIZE;
    public static final EchoNativeConfigSpec.BooleanValue CACHE_NETWORKS;
    public static final EchoNativeConfigSpec.IntValue NETWORK_UPDATE_INTERVAL_TICKS;
    public static final EchoNativeConfigSpec.IntValue MAX_NETWORK_SIZE;
    public static final EchoNativeConfigSpec.BooleanValue IDLE_NETWORK_SLEEP;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_POWER_LOSS;
    public static final EchoNativeConfigSpec.DoubleValue BASE_LOSS_PERCENT_PER_16_BLOCKS;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_OVERLOAD;
    public static final EchoNativeConfigSpec.BooleanValue TRIP_BREAKERS;
    public static final EchoNativeConfigSpec.BooleanValue DAMAGE_CABLES;
    public static final EchoNativeConfigSpec.BooleanValue EXPLODE_ON_EXTREME_OVERLOAD;
    public static final EchoNativeConfigSpec.IntValue OVERLOAD_GRACE_TICKS;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_BROWNOUT;
    public static final EchoNativeConfigSpec.BooleanValue MACHINE_SLOWDOWN;
    public static final EchoNativeConfigSpec.BooleanValue DECORATIVE_LIGHTS_DIM;
    public static final EchoNativeConfigSpec.IntValue BROWNOUT_THRESHOLD_PERCENT;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_FE_BRIDGE;
    public static final EchoNativeConfigSpec.DoubleValue FE_TO_EP_RATIO;
    public static final EchoNativeConfigSpec.DoubleValue EP_TO_FE_RATIO;

    public static final EchoNativeConfigSpec.BooleanValue USE_RUNTIMEGUARD_IF_AVAILABLE;
    public static final EchoNativeConfigSpec.IntValue GRID_UPDATES_PER_TICK;
    public static final EchoNativeConfigSpec.IntValue METER_UPDATE_INTERVAL_TICKS;
    public static final EchoNativeConfigSpec.IntValue CABLE_VISUAL_UPDATE_INTERVAL_TICKS;

    public static final EchoNativeConfigSpec.BooleanValue DISABLE_EXPLOSIONS_ON_SERVERS;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.comment("Enable ECHO PowerGrid systems.").define("enabled", true);
        DEBUG_LOGGING = builder.comment("Enable debug logging.").define("debugLogging", false);
        builder.pop();

        builder.push("network");
        MAX_CABLE_SCAN_PER_TICK = builder.comment("Max cable scan per tick.").defineInRange("maxCableScanPerTick", 512, 64, 4096);
        NETWORK_REBUILD_BATCH_SIZE = builder.comment("Networks to rebuild per batch.").defineInRange("networkRebuildBatchSize", 4, 1, 64);
        CACHE_NETWORKS = builder.comment("Cache network membership.").define("cacheNetworks", true);
        NETWORK_UPDATE_INTERVAL_TICKS = builder.comment("Ticks between network updates.").defineInRange("networkUpdateIntervalTicks", 20, 1, 200);
        MAX_NETWORK_SIZE = builder.comment("Maximum nodes per network.").defineInRange("maxNetworkSize", 4096, 256, 16384);
        IDLE_NETWORK_SLEEP = builder.comment("Skip updates for idle networks.").define("idleNetworkSleep", true);
        builder.pop();

        builder.push("loss");
        ENABLE_POWER_LOSS = builder.comment("Enable power loss over distance.").define("enablePowerLoss", true);
        BASE_LOSS_PERCENT_PER_16_BLOCKS = builder.comment("Base loss percent per 16 blocks of cable.").defineInRange("baseLossPercentPer16Blocks", 1.0, 0.0, 10.0);
        builder.pop();

        builder.push("overload");
        ENABLE_OVERLOAD = builder.comment("Enable overload behavior.").define("enableOverload", true);
        TRIP_BREAKERS = builder.comment("Trip breakers on overload.").define("tripBreakers", true);
        DAMAGE_CABLES = builder.comment("Damage cables on overload.").define("damageCables", false);
        EXPLODE_ON_EXTREME_OVERLOAD = builder.comment("Explode on extreme overload.").define("explodeOnExtremeOverload", false);
        OVERLOAD_GRACE_TICKS = builder.comment("Grace ticks before overload triggers.").defineInRange("overloadGraceTicks", 100, 0, 600);
        builder.pop();

        builder.push("brownout");
        ENABLE_BROWNOUT = builder.comment("Enable brownout behavior.").define("enableBrownout", true);
        MACHINE_SLOWDOWN = builder.comment("Slow machines during brownout.").define("machineSlowdown", true);
        DECORATIVE_LIGHTS_DIM = builder.comment("Dim decorative lights during brownout.").define("decorativeLightsDim", true);
        BROWNOUT_THRESHOLD_PERCENT = builder.comment("Demand/supply threshold percent for brownout.").defineInRange("brownoutThresholdPercent", 90, 50, 100);
        builder.pop();

        builder.push("compat");
        ENABLE_FE_BRIDGE = builder.comment("Enable FE bridge.").define("enableFeBridge", true);
        FE_TO_EP_RATIO = builder.comment("FE to EP conversion ratio.").defineInRange("feToEpRatio", 1.0, 0.01, 1000.0);
        EP_TO_FE_RATIO = builder.comment("EP to FE conversion ratio.").defineInRange("epToFeRatio", 1.0, 0.01, 1000.0);
        builder.pop();

        builder.push("performance");
        USE_RUNTIMEGUARD_IF_AVAILABLE = builder.comment("Use RuntimeGuard budgets if available.").define("useRuntimeGuardIfAvailable", true);
        GRID_UPDATES_PER_TICK = builder.comment("Grid updates per tick.").defineInRange("gridUpdatesPerTick", 16, 1, 64);
        METER_UPDATE_INTERVAL_TICKS = builder.comment("Meter update interval in ticks.").defineInRange("meterUpdateIntervalTicks", 20, 1, 200);
        CABLE_VISUAL_UPDATE_INTERVAL_TICKS = builder.comment("Cable visual update interval in ticks.").defineInRange("cableVisualUpdateIntervalTicks", 40, 1, 200);
        builder.pop();

        builder.push("rtg_safety");
        DISABLE_EXPLOSIONS_ON_SERVERS = builder.comment("Disable explosions on dedicated servers.").define("disableExplosionsOnServers", true);
        builder.pop();

        SPEC = builder.build();
    }

    private PowerGridConfig() {}
}
