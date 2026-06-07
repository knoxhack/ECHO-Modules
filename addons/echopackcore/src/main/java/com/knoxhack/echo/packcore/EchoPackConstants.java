package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleName;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.platformcore.EchoPermissionSet;
import com.knoxhack.echo.platformcore.EchoPlatformConstants;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoPackConstants {
    public static final String MOD_ID = "echopackcore";
    public static final String MOD_NAME = "ECHO: PackCore";

    public static final EchoFeatureId FEATURE_PACK_PROFILE = EchoFeatureId.of("pack.profile");
    public static final EchoFeatureId FEATURE_PACK_VARIANT = EchoFeatureId.of("pack.variant");
    public static final EchoFeatureId FEATURE_PACK_CHANNEL = EchoFeatureId.of("pack.channel");
    public static final EchoFeatureId FEATURE_PACK_LOCKFILE = EchoFeatureId.of("pack.lockfile");
    public static final EchoFeatureId FEATURE_PACK_REPAIR = EchoFeatureId.of("pack.repair");
    public static final EchoFeatureId FEATURE_PACK_SNAPSHOT = EchoFeatureId.of("pack.snapshot");

    public static final EchoPackId PACK_ASHFALL = EchoPackId.of("ashfall");
    public static final EchoPackId PACK_ECHO_PRIME = EchoPackId.of("echo_prime");
    public static final EchoPackId PACK_ARCANA_DIVISION = EchoPackId.of("arcana_division");
    public static final EchoPackId PACK_CUSTOM = EchoPackId.of("custom");

    public static final EchoPackVariant VARIANT_STANDARD = variant("standard", "Standard", "Balanced default experience.", true);
    public static final EchoPackVariant VARIANT_PERFORMANCE = variant("performance", "Performance", "Reduced cost profile for lower-end hardware.", false);
    public static final EchoPackVariant VARIANT_CINEMATIC = variant("cinematic", "Cinematic", "Higher visual and audio budget profile.", false);
    public static final EchoPackVariant VARIANT_SERVER = variant("server", "Server", "Dedicated server oriented composition.", false);
    public static final EchoPackVariant VARIANT_CREATOR = variant("creator", "Creator", "Creator and debugging oriented composition.", false);
    public static final EchoPackVariant VARIANT_DEV = variant("dev", "Dev", "Local development composition.", false);

    public static final EchoPackChannel CHANNEL_STABLE = channel("stable", "Stable", EchoApiStability.STABLE, true, true);
    public static final EchoPackChannel CHANNEL_BETA = channel("beta", "Beta", EchoApiStability.BETA, true, false);
    public static final EchoPackChannel CHANNEL_ALPHA = channel("alpha", "Alpha", EchoApiStability.EXPERIMENTAL, true, false);
    public static final EchoPackChannel CHANNEL_NIGHTLY = channel("nightly", "Nightly", EchoApiStability.EXPERIMENTAL, false, false);
    public static final EchoPackChannel CHANNEL_DEV_LOCAL = channel("dev-local", "Dev Local", EchoApiStability.INTERNAL, false, false);
    public static final EchoPackChannel CHANNEL_EXPERIMENTAL = channel("experimental", "Experimental", EchoApiStability.EXPERIMENTAL, false, false);

    public static final List<EchoPackVariant> BUILTIN_VARIANTS = List.of(
            VARIANT_STANDARD,
            VARIANT_PERFORMANCE,
            VARIANT_CINEMATIC,
            VARIANT_SERVER,
            VARIANT_CREATOR,
            VARIANT_DEV
    );

    public static final List<EchoPackChannel> BUILTIN_CHANNELS = List.of(
            CHANNEL_STABLE,
            CHANNEL_BETA,
            CHANNEL_ALPHA,
            CHANNEL_NIGHTLY,
            CHANNEL_DEV_LOCAL,
            CHANNEL_EXPERIMENTAL
    );

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.PACK_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(
                    FEATURE_PACK_PROFILE,
                    FEATURE_PACK_VARIANT,
                    FEATURE_PACK_CHANNEL,
                    FEATURE_PACK_LOCKFILE,
                    FEATURE_PACK_REPAIR,
                    FEATURE_PACK_SNAPSHOT
            ),
            Set.of(),
            EchoPermissionSet.of(
                    EchoPlatformConstants.PERMISSION_PACK_READ,
                    EchoPlatformConstants.PERMISSION_DIAGNOSTICS_WRITE
            )
    );

    private EchoPackConstants() {
    }

    private static EchoPackVariant variant(String id, String name, String summary, boolean defaultVariant) {
        return new EchoPackVariant(
                EchoPackVariantId.of(id),
                name,
                summary,
                defaultVariant,
                Set.of(),
                Set.of(),
                Map.of(),
                List.of()
        );
    }

    private static EchoPackChannel channel(
            String id,
            String name,
            EchoApiStability stability,
            boolean publicChannel,
            boolean defaultChannel
    ) {
        return new EchoPackChannel(
                EchoPackChannelId.of(id),
                name,
                "",
                stability,
                publicChannel,
                defaultChannel
        );
    }
}
