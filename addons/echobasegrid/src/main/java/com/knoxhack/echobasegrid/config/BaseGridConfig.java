package com.knoxhack.echobasegrid.config;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import java.util.List;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;

public final class BaseGridConfig {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.BooleanValue ENABLED;
    public static final EchoNativeConfigSpec.IntValue MAX_CLAIMS_PER_PLAYER;
    public static final EchoNativeConfigSpec.IntValue GRID_RADIUS;
    public static final EchoNativeConfigSpec.IntValue MAX_MEMBERS;
    public static final EchoNativeConfigSpec.BooleanValue OPS_BYPASS;
    public static final EchoNativeConfigSpec.BooleanValue PROTECT_EXPLOSIONS;
    public static final EchoNativeConfigSpec SPEC;

    static {
        BUILDER.push("base_grid");
        ENABLED = BUILDER.comment("Enable ECHO Base Grid claims and protection.")
                .define("enabled", true);
        MAX_CLAIMS_PER_PLAYER = BUILDER.comment("Maximum claimed chunks per player.")
                .defineInRange("maxClaimsPerPlayer", 64, 0, 4096);
        GRID_RADIUS = BUILDER.comment("Chunk radius shown around the player in the ScreenCore grid.")
                .defineInRange("gridRadius", 6, 1, 12);
        MAX_MEMBERS = BUILDER.comment("Maximum trusted members per claim.")
                .defineInRange("maxMembers", 16, 0, 128);
        OPS_BYPASS = BUILDER.comment("Allow gamemaster-level operators to bypass claim protection.")
                .define("opsBypass", true);
        PROTECT_EXPLOSIONS = BUILDER.comment("Remove claimed blocks from explosion damage lists.")
                .define("protectExplosions", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private BaseGridConfig() {
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoBaseGrid.MODID, () -> new EchoConfigModule(
                EchoBaseGrid.MODID,
                "ECHO: Base Grid",
                List.of(new EchoConfigCategory("claims", "Claims", List.of(
                        EchoConfigEntry.booleanSpec("enabled", "Enabled",
                                "Enable chunk claiming and claim protection.",
                                EchoConfigSide.COMMON, ENABLED, true, false, false),
                        EchoConfigEntry.intSpec("max_claims_per_player", "Max Claims Per Player",
                                "Maximum chunks each player can claim.",
                                EchoConfigSide.COMMON, MAX_CLAIMS_PER_PLAYER, 0, 4096, true, false, false),
                        EchoConfigEntry.intSpec("grid_radius", "Grid Radius",
                                "Radius of chunks visible around the player in the ScreenCore Base Grid.",
                                EchoConfigSide.COMMON, GRID_RADIUS, 1, 12, true, false, false),
                        EchoConfigEntry.intSpec("max_members", "Max Members",
                                "Maximum trusted members per claim.",
                                EchoConfigSide.COMMON, MAX_MEMBERS, 0, 128, true, false, false),
                        EchoConfigEntry.booleanSpec("ops_bypass", "Ops Bypass",
                                "Allow gamemaster operators to bypass claim protection.",
                                EchoConfigSide.COMMON, OPS_BYPASS, true, false, false),
                        EchoConfigEntry.booleanSpec("protect_explosions", "Protect Explosions",
                                "Prevent explosions from damaging claimed chunks.",
                                EchoConfigSide.COMMON, PROTECT_EXPLOSIONS, true, false, false)
                )))
        )));
    }
}
