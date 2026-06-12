package com.knoxhack.echodatacore;

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

    public static final EchoNativeConfigSpec.BooleanValue DEBUG_COMMANDS = BUILDER
            .comment("Enables mutating /echodata debug commands. Inspect commands remain permission-gated.")
            .define("debug.commandsEnabled", false);

    public static final EchoNativeConfigSpec.IntValue SYNC_INTERVAL_TICKS = BUILDER
            .comment("Minimum ticks between dirty DataCore sync batches per player.")
            .defineInRange("sync.intervalTicks", 10, 1, 200);

    public static final EchoNativeConfigSpec.IntValue MAX_SYNC_KEYS_PER_BATCH = BUILDER
            .comment("Maximum changed keys sent to one player in one DataCore sync batch.")
            .defineInRange("sync.maxKeysPerBatch", 64, 1, 512);

    public static final EchoNativeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoDataCore.MODID, () -> new EchoConfigModule(
                EchoDataCore.MODID,
                "DataCore",
                List.of(new EchoConfigCategory("sync", "Sync", List.of(
                        EchoConfigEntry.intSpec("sync_interval_ticks", "Sync Interval Ticks",
                                "Minimum ticks between dirty DataCore sync batches per player.",
                                EchoConfigSide.COMMON, SYNC_INTERVAL_TICKS, 1, 200,
                                true, false, false),
                        EchoConfigEntry.intSpec("max_sync_keys", "Max Sync Keys",
                                "Maximum changed keys sent to one player in one DataCore sync batch.",
                                EchoConfigSide.COMMON, MAX_SYNC_KEYS_PER_BATCH, 1, 512,
                                true, false, false)))))));
    }
}
