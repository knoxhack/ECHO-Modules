package com.knoxhack.echotextureforge.common.config;

import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import com.knoxhack.echotextureforge.EchoTextureForgeMod;
import java.util.List;

public final class TextureForgeConfig {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enables TextureForge dev commands and startup hooks.")
            .define("enabled", true);

    public static final EchoNativeConfigSpec.BooleanValue DEV_MODE_ONLY = BUILDER
            .comment("Marks TextureForge as development-only. Pack authors should leave this true.")
            .define("devModeOnly", true);

    public static final EchoNativeConfigSpec.BooleanValue SCAN_ON_STARTUP = BUILDER
            .comment("Runs a scan when the server starts. Disabled by default to avoid surprise build output.")
            .define("scanOnStartup", false);

    public static final EchoNativeConfigSpec.BooleanValue EXPORT_ON_STARTUP = BUILDER
            .comment("Exports reports and prompts after startup scans.")
            .define("exportOnStartup", false);

    public static final EchoNativeConfigSpec.BooleanValue VALIDATE_32X32 = BUILDER
            .comment("Validates item/block PNGs against the default 32x32 ECHO standard unless specs override it.")
            .define("validate32x32", true);

    public static final EchoNativeConfigSpec.BooleanValue INCLUDE_GENERATED_RESOURCES = BUILDER
            .comment("Includes src/generated/resources when scanning addon assets.")
            .define("includeGeneratedResources", false);

    public static final EchoNativeConfigSpec.BooleanValue INCLUDE_EXTERNAL_NAMESPACES = BUILDER
            .comment("Includes non-ECHO namespaces during registry and resource scans.")
            .define("includeExternalNamespaces", false);

    public static final EchoNativeConfigSpec.StringValue OUTPUT_DIRECTORY = BUILDER
            .comment("Workspace-relative output directory for TextureForge reports, prompts, and cut maps.")
            .define("outputDirectory", "build/textureforge");

    public static final EchoNativeConfigSpec.BooleanValue STRICT_MODE = BUILDER
            .comment("Promotes optional validation warnings into stricter audit signals.")
            .define("strictMode", false);

    public static final EchoNativeConfigSpec SPEC = BUILDER.build();

    private TextureForgeConfig() {
    }

    public static boolean enabled() {
        return bool(ENABLED, true);
    }

    public static boolean devModeOnly() {
        return bool(DEV_MODE_ONLY, true);
    }

    public static boolean scanOnStartup() {
        return bool(SCAN_ON_STARTUP, false);
    }

    public static boolean exportOnStartup() {
        return bool(EXPORT_ON_STARTUP, false);
    }

    public static boolean validate32x32() {
        return bool(VALIDATE_32X32, true);
    }

    public static boolean includeGeneratedResources() {
        if (Boolean.getBoolean("textureForge.includeGeneratedResources")) {
            return true;
        }
        return bool(INCLUDE_GENERATED_RESOURCES, false);
    }

    public static boolean includeExternalNamespaces() {
        return bool(INCLUDE_EXTERNAL_NAMESPACES, false);
    }

    public static String outputDirectory() {
        String override = System.getProperty("textureForge.outputDir", "");
        if (!override.isBlank()) {
            return override;
        }
        try {
            return OUTPUT_DIRECTORY.get();
        } catch (RuntimeException exception) {
            return "build/textureforge";
        }
    }

    public static boolean strictMode() {
        if (Boolean.getBoolean("textureForge.strict")) {
            return true;
        }
        return bool(STRICT_MODE, false);
    }

    private static boolean bool(EchoNativeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoTextureForgeMod.MODID, () -> new EchoConfigModule(
                EchoTextureForgeMod.MODID,
                "TextureForge",
                List.of(new EchoConfigCategory("workflow", "Workflow", List.of(
                        EchoConfigEntry.booleanSpec("enabled", "Enabled",
                                "Enables TextureForge dev commands and startup hooks.",
                                EchoConfigSide.COMMON, ENABLED, true, false, false),
                        EchoConfigEntry.booleanSpec("dev_mode_only", "Dev Mode Only",
                                "Keeps this addon marked as a development-only workflow.",
                                EchoConfigSide.COMMON, DEV_MODE_ONLY, true, false, false),
                        EchoConfigEntry.booleanSpec("scan_on_startup", "Scan On Startup",
                                "Runs a scan when the server starts.",
                                EchoConfigSide.COMMON, SCAN_ON_STARTUP, true, false, false),
                        EchoConfigEntry.booleanSpec("export_on_startup", "Export On Startup",
                                "Exports reports and prompts after startup scans.",
                                EchoConfigSide.COMMON, EXPORT_ON_STARTUP, true, false, false),
                        EchoConfigEntry.booleanSpec("validate_32x32", "Validate 32x32",
                                "Validates item/block PNGs against TextureForge 32x32 defaults.",
                                EchoConfigSide.COMMON, VALIDATE_32X32, true, false, false),
                        EchoConfigEntry.booleanSpec("include_generated_resources", "Include Generated Resources",
                                "Includes src/generated/resources while scanning.",
                                EchoConfigSide.COMMON, INCLUDE_GENERATED_RESOURCES, true, false, false),
                        EchoConfigEntry.booleanSpec("include_external_namespaces", "Include External Namespaces",
                                "Includes non-ECHO namespaces while scanning.",
                                EchoConfigSide.COMMON, INCLUDE_EXTERNAL_NAMESPACES, true, false, false),
                        EchoConfigEntry.stringSpec("output_directory", "Output Directory",
                                "Workspace-relative output directory for reports and prompts.",
                                EchoConfigSide.COMMON, OUTPUT_DIRECTORY, true, false, false),
                        EchoConfigEntry.booleanSpec("strict_mode", "Strict Mode",
                                "Promotes optional findings into stricter audit signals.",
                                EchoConfigSide.COMMON, STRICT_MODE, true, false, false)))))));
    }
}
