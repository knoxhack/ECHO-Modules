package com.knoxhack.echomultiblockcore;

import com.echoplatform.echocore.api.config.EchoConfigCategory;
import com.echoplatform.echocore.api.config.EchoConfigEntry;
import com.echoplatform.echocore.api.config.EchoConfigModule;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;
import com.echoplatform.echocore.api.config.EchoConfigProvider;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import java.util.List;

public final class Config {
    public static final EchoNativeConfigSpec SPEC;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_INTEGRATION_DEBUG_LOGGING;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_PREVIEW_RENDERING;
    public static final EchoNativeConfigSpec.IntValue PREVIEW_MAX_RENDER_CELLS;
    public static final EchoNativeConfigSpec.IntValue MAX_VALIDATION_VOLUME;
    public static final EchoNativeConfigSpec.IntValue MAX_ACTIVE_MULTIBLOCKS_PER_CHUNK;
    public static final EchoNativeConfigSpec.IntValue TASK_QUEUE_CAPACITY;
    public static final EchoNativeConfigSpec.DoubleValue ROBOTIC_TASK_SPEED_MULTIPLIER;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ROBOTIC_ANIMATIONS;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_AUTO_REPAIR;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CHUNK_LOADED_TICKING;
    public static final EchoNativeConfigSpec.IntValue AUTO_BUILDER_MAX_PLACEMENTS;
    public static final EchoNativeConfigSpec.BooleanValue REQUIRE_AUTO_BUILDER_COMPONENT;

    private Config() {
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoMultiblockCore.MODID, () -> new EchoConfigModule(
                EchoMultiblockCore.MODID,
                "MultiblockCore",
                List.of(
                        new EchoConfigCategory("client", "Client", List.of(
                                EchoConfigEntry.booleanSpec("enable_preview_rendering", "Enable Preview Rendering",
                                        "Enables blueprint and controller preview overlays on clients.",
                                        EchoConfigSide.CLIENT, ENABLE_PREVIEW_RENDERING, true, false, false),
                                EchoConfigEntry.intSpec("preview_max_render_cells", "Preview Max Render Cells",
                                        "Maximum multiblock preview cells rendered per frame.",
                                        EchoConfigSide.CLIENT, PREVIEW_MAX_RENDER_CELLS, 64, 4096, true, false, false))),
                        new EchoConfigCategory("debug", "Debug", List.of(
                                EchoConfigEntry.booleanSpec("enable_debug_logging", "Enable Debug Logging",
                                        "Enables verbose MultiblockCore validation and runtime logging.",
                                        EchoConfigSide.COMMON, ENABLE_DEBUG_LOGGING, true, false, false),
                                EchoConfigEntry.booleanSpec("enable_integration_debug_logging", "Enable Integration Debug Logging",
                                        "Enables verbose optional integration diagnostics.",
                                        EchoConfigSide.COMMON, ENABLE_INTEGRATION_DEBUG_LOGGING, true, false, false))),
                        new EchoConfigCategory("validation", "Validation", List.of(
                                EchoConfigEntry.intSpec("max_validation_volume", "Max Validation Volume",
                                        "Maximum block volume a single multiblock validation may scan.",
                                        EchoConfigSide.COMMON, MAX_VALIDATION_VOLUME, 1, 65536, true, false, false))),
                        new EchoConfigCategory("runtime", "Runtime", List.of(
                                EchoConfigEntry.intSpec("task_queue_capacity", "Task Queue Capacity",
                                        "Maximum persisted automation tasks per controller queue.",
                                        EchoConfigSide.COMMON, TASK_QUEUE_CAPACITY, 1, 32, true, false, false))),
                        new EchoConfigCategory("robotics", "Robotics", List.of(
                                EchoConfigEntry.booleanSpec("enable_robotic_animations", "Enable Robotic Animations",
                                        "Enables visible robotic task animation packets and particles.",
                                        EchoConfigSide.COMMON, ENABLE_ROBOTIC_ANIMATIONS, true, false, false),
                                EchoConfigEntry.booleanSpec("allow_auto_repair", "Allow Auto Repair",
                                        "Allows formed multiblocks to queue repair tasks when suitable tools exist.",
                                        EchoConfigSide.COMMON, ALLOW_AUTO_REPAIR, true, false, false))),
                        new EchoConfigCategory("construction", "Construction", List.of(
                                EchoConfigEntry.intSpec("auto_builder_max_placements", "Auto Builder Max Placements",
                                        "Maximum blocks an Auto Builder action may place at once.",
                                        EchoConfigSide.COMMON, AUTO_BUILDER_MAX_PLACEMENTS, 1, 256, true, false, false),
                                EchoConfigEntry.booleanSpec("require_auto_builder_component", "Require Auto Builder Component",
                                        "Requires a discovered Auto Builder component before construction automation can place blocks.",
                                        EchoConfigSide.COMMON, REQUIRE_AUTO_BUILDER_COMPONENT, true, false, false)))))));
    }

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
        builder.push("debug");
        ENABLE_DEBUG_LOGGING = builder.comment("Enables verbose MultiblockCore validation/runtime logging.")
                .define("enableDebugLogging", false);
        ENABLE_INTEGRATION_DEBUG_LOGGING = builder.comment("Enables verbose optional integration diagnostics for Terminal, Lens, HoloMap, RuntimeGuard, and RenderCore bridges.")
                .define("enableIntegrationDebugLogging", false);
        builder.pop();

        builder.push("client");
        ENABLE_PREVIEW_RENDERING = builder.comment("Enables blueprint and controller preview overlays on clients.")
                .define("enablePreviewRendering", true);
        PREVIEW_MAX_RENDER_CELLS = builder.comment("Maximum multiblock preview cells rendered per frame on clients.")
                .defineInRange("previewMaxRenderCells", 1024, 64, 4096);
        builder.pop();

        builder.push("validation");
        MAX_VALIDATION_VOLUME = builder.comment("Maximum block volume a single multiblock validation may scan.")
                .defineInRange("maxValidationVolume", 4096, 1, 65536);
        builder.pop();

        builder.push("runtime");
        MAX_ACTIVE_MULTIBLOCKS_PER_CHUNK = builder.comment("Maximum active formed multiblocks recorded per chunk.")
                .defineInRange("maxActiveMultiblocksPerChunk", 16, 1, 256);
        TASK_QUEUE_CAPACITY = builder.comment("Maximum persisted automation tasks per multiblock controller queue.")
                .defineInRange("taskQueueCapacity", 12, 1, 32);
        ALLOW_CHUNK_LOADED_TICKING = builder.comment("Allows formed multiblocks to tick while only chunk-loaded. Default false for safety.")
                .define("allowChunkLoadedTicking", false);
        builder.pop();

        builder.push("robotics");
        ROBOTIC_TASK_SPEED_MULTIPLIER = builder.comment("Multiplier applied to robotic task speed. Higher is faster.")
                .defineInRange("roboticTaskSpeedMultiplier", 1.0D, 0.1D, 10.0D);
        ENABLE_ROBOTIC_ANIMATIONS = builder.comment("Enables visible robotic task animation packets and particles.")
                .define("enableRoboticAnimations", true);
        ALLOW_AUTO_REPAIR = builder.comment("Allows formed multiblocks to queue repair tasks when suitable tools exist.")
                .define("allowAutoRepair", true);
        builder.pop();

        builder.push("construction");
        AUTO_BUILDER_MAX_PLACEMENTS = builder.comment("Maximum blocks an Auto Builder action may place at once.")
                .defineInRange("autoBuilderMaxPlacements", 16, 1, 256);
        REQUIRE_AUTO_BUILDER_COMPONENT = builder.comment("Requires a discovered Auto Builder component before construction automation can place blocks.")
                .define("requireAutoBuilderComponent", true);
        builder.pop();

        SPEC = builder.build();
    }
}
