package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.Config;
import java.util.List;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Single source of truth for environmental event tuning, commands, HUD labels,
 * and visual contracts.
 */
public final class EnvironmentalEventProfiles {
    private static final Map<EnvironmentalEventType, EnvironmentalEventProfile> PROFILES =
            new EnumMap<>(EnvironmentalEventType.class);
    private static final Map<String, EnvironmentalEventType> ALIASES = new java.util.HashMap<>();

    static {
        register(new EnvironmentalEventProfile(
                EnvironmentalEventType.RADIATION_STORM,
                "radiation_storm",
                "RAD STORM",
                6000,
                EnvironmentalEventProfile.WeatherMode.THUNDER,
                0x5538FF4A,
                0xFFE4FF4A,
                18,
                0.30F,
                0.14F,
                0.46F,
                0.22F));
        register(new EnvironmentalEventProfile(
                EnvironmentalEventType.TOXIC_STORM,
                "acid_rain",
                "ACID RAIN",
                5600,
                EnvironmentalEventProfile.WeatherMode.RAIN,
                0x554FCE64,
                0xFF6EBF5A,
                16,
                0.23F,
                0.26F,
                0.20F,
                0.20F));
        register(new EnvironmentalEventProfile(
                EnvironmentalEventType.BLACKOUT,
                "blackout",
                "BLACKOUT",
                4200,
                EnvironmentalEventProfile.WeatherMode.BLACKOUT,
                0x88000000,
                0xFF66E8FF,
                6,
                0.18F,
                0.22F,
                0.08F,
                0.20F));
        register(new EnvironmentalEventProfile(
                EnvironmentalEventType.ASH_STORM,
                "ash_storm",
                "ASH STORM",
                5200,
                EnvironmentalEventProfile.WeatherMode.DRY,
                0x664E4941,
                0xFFB7B0A0,
                26,
                0.16F,
                0.10F,
                0.18F,
                0.12F));
        register(new EnvironmentalEventProfile(
                EnvironmentalEventType.CRYO_FRONT,
                "cryo_front",
                "CRYO FRONT",
                4600,
                EnvironmentalEventProfile.WeatherMode.RAIN,
                0x5547D8FF,
                0xFFBDEFFF,
                16,
                0.08F,
                0.14F,
                0.04F,
                0.08F));
        register(new EnvironmentalEventProfile(
                EnvironmentalEventType.NEXUS_SURGE,
                "nexus_surge",
                "NEXUS SURGE",
                3600,
                EnvironmentalEventProfile.WeatherMode.DRY,
                0x664D28FF,
                0xFFE09CFF,
                14,
                0.05F,
                0.04F,
                0.04F,
                0.18F));
    }

    private EnvironmentalEventProfiles() {
    }

    private static void register(EnvironmentalEventProfile profile) {
        PROFILES.put(profile.type(), profile);
        ALIASES.put(profile.commandAlias(), profile.type());
        ALIASES.put(profile.type().name().toLowerCase(Locale.ROOT), profile.type());
        ALIASES.put(profile.type().getDisplayName().toLowerCase(Locale.ROOT).replace(' ', '_'), profile.type());
    }

    public static Collection<EnvironmentalEventProfile> activeProfiles() {
        return PROFILES.values();
    }

    public static EnvironmentalEventProfile get(EnvironmentalEventType type) {
        return PROFILES.get(type);
    }

    public static Optional<EnvironmentalEventType> byAlias(String alias) {
        if (alias == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ALIASES.get(alias.toLowerCase(Locale.ROOT)));
    }

    public static List<String> commandAliases() {
        return PROFILES.values().stream()
                .map(EnvironmentalEventProfile::commandAlias)
                .sorted()
                .toList();
    }

    public static boolean isEnabled(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> Config.ENABLE_RADIATION_STORMS.get();
            case ASH_STORM -> Config.ENABLE_ASH_STORMS.get();
            case CRYO_FRONT -> Config.ENABLE_CRYO_FRONTS.get();
            case NEXUS_SURGE -> Config.ENABLE_NEXUS_SURGES.get();
            case NONE -> false;
            default -> true;
        };
    }

    public static String hudLabel(EnvironmentalEventType type) {
        EnvironmentalEventProfile profile = get(type);
        return profile == null ? type.getDisplayName() : profile.hudLabel();
    }
}
