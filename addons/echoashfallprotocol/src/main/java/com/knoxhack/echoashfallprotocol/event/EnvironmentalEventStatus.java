package com.knoxhack.echoashfallprotocol.event;

import java.util.Locale;

/**
 * Shared presentation contract for environmental event UI, commands, and QA.
 */
public record EnvironmentalEventStatus(
        EnvironmentalEventType type,
        boolean active,
        String label,
        String commandAlias,
        int remainingTicks,
        int durationTicks,
        float intensity,
        float phase,
        int hudColor,
        EnvironmentalEventProfile.WeatherMode weatherMode,
        String counterGuidance,
        String survivalImpact,
        int survivalCount,
        boolean enabled
) {
    public static EnvironmentalEventStatus inactive(boolean nextTriggerEligible) {
        return new EnvironmentalEventStatus(
                EnvironmentalEventType.NONE,
                false,
                "CLEAR",
                "none",
                0,
                0,
                0.0F,
                0.0F,
                0xFF7FE8A6,
                EnvironmentalEventProfile.WeatherMode.NONE,
                nextTriggerEligible ? "No active event. Route conditions can escalate." : "No active event. Minimum interval still cooling down.",
                "No active weather pressure.",
                0,
                false);
    }

    public static EnvironmentalEventStatus fromData(EnvironmentalEventData data, long gameTime) {
        EnvironmentalEventType type = data.getCurrentEvent();
        if (type == EnvironmentalEventType.NONE) {
            return inactive(data.canTriggerEvent());
        }
        return fromValues(
                type,
                data.getRemainingEventTicks(gameTime),
                data.getEventDuration(),
                data.getEventIntensity(),
                data.getEventPhase(gameTime),
                data.getEventsSurvived(type));
    }

    public static EnvironmentalEventStatus fromSynced(String typeName, int remainingTicks, int durationTicks,
                                                      float intensity, float phase, int survivalCount) {
        EnvironmentalEventType type = parseType(typeName);
        if (type == EnvironmentalEventType.NONE) {
            return inactive(false);
        }
        return fromValues(type, remainingTicks, durationTicks, intensity, phase, survivalCount);
    }

    public static EnvironmentalEventType parseType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return EnvironmentalEventType.NONE;
        }
        try {
            return EnvironmentalEventType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EnvironmentalEventType.NONE;
        }
    }

    private static EnvironmentalEventStatus fromValues(EnvironmentalEventType type, int remainingTicks,
                                                       int durationTicks, float intensity, float phase,
                                                       int survivalCount) {
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        if (profile == null || remainingTicks <= 0 || durationTicks <= 0) {
            return inactive(false);
        }
        return new EnvironmentalEventStatus(
                type,
                true,
                profile.hudLabel(),
                profile.commandAlias(),
                Math.max(0, remainingTicks),
                Math.max(0, durationTicks),
                Math.max(0.0F, intensity),
                Math.max(0.0F, Math.min(1.0F, phase)),
                hudColor(type),
                profile.weatherMode(),
                counterGuidance(type),
                survivalImpact(type),
                Math.max(0, survivalCount),
                EnvironmentalEventProfiles.isEnabled(type));
    }

    public int remainingSeconds() {
        return Math.max(0, (remainingTicks + 19) / 20);
    }

    public int phasePercent() {
        return Math.round(phase * 100.0F);
    }

    public String intensityText() {
        return String.format(Locale.ROOT, "%.2f", intensity);
    }

    public String weatherLabel() {
        return switch (weatherMode) {
            case RAIN -> "RAIN";
            case THUNDER -> "THUNDER";
            case DRY -> "DRY";
            case BLACKOUT -> "BLACKOUT";
            case NONE -> "NONE";
        };
    }

    public String survivalCountText() {
        return survivalCount + " survived";
    }

    public String shortStatusText() {
        if (!active) {
            return "Weather clear. " + counterGuidance;
        }
        return label + " T-" + remainingSeconds() + "s / intensity " + intensityText();
    }

    public String qaSummary(boolean nextTriggerEligible) {
        if (!active) {
            return "No active environmental event. Next trigger eligible: " + nextTriggerEligible + ".";
        }
        return label + " active, remaining " + remainingSeconds() + "s, intensity " + intensityText()
                + ", phase " + phasePercent() + "%, weather " + weatherLabel()
                + ", " + survivalCountText() + ", enabled " + enabled + ".";
    }

    public String centerWarningTitle() {
        return switch (type) {
            case RADIATION_STORM -> "RADIATION STORM";
            case TOXIC_STORM -> "ACID RAIN";
            case BLACKOUT -> "GRID BLACKOUT";
            case ASH_STORM -> "ASH STORM";
            case CRYO_FRONT -> "CRYO FRONT";
            case NEXUS_SURGE -> "NEXUS SURGE";
            default -> label;
        };
    }

    public String centerWarningSubtitle() {
        return switch (type) {
            case RADIATION_STORM -> "EXPOSED TO GRIDFALL RAIN\nGET UNDER COVER";
            case TOXIC_STORM -> "FILTERS AND SCRUBBERS ADVISED\nAVOID OPEN EXPOSURE";
            case BLACKOUT -> "GRID POWER UNSTABLE\nCHECK RESERVE CELLS";
            case ASH_STORM -> "PARTICULATES RISING\nSHELTER AND CONSERVE WATER";
            case CRYO_FRONT -> "BODY HEAT FALLING\nFIRE OR THERMAL LINER REQUIRED";
            case NEXUS_SURGE -> "FIELD INSTABILITY RISING\nCLEAR NEXUS SOURCES";
            default -> counterGuidance.toUpperCase(Locale.ROOT);
        };
    }

    private static int hudColor(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> 0xFFFF5C5C;
            case TOXIC_STORM -> 0xFFB56AFF;
            case BLACKOUT -> 0xFF8EDCFF;
            case ASH_STORM -> 0xFFFFC95C;
            case CRYO_FRONT -> 0xFF7FE8FF;
            case NEXUS_SURGE -> 0xFFE09CFF;
            default -> 0xFFE4F2FF;
        };
    }

    private static String counterGuidance(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> "Get under cover or inside a scrubber pocket; use RadAway after exposure.";
            case TOXIC_STORM -> "Use gas mask filters, hazmat, or scrubbed shelter until the rain clears.";
            case BLACKOUT -> "Keep emergency cells and survival-priority power available.";
            case ASH_STORM -> "Stay sheltered, conserve water, keep filters charged, and watch for Ash Wraiths.";
            case CRYO_FRONT -> "Use fire, shelter, thermal protection, or leave open sky exposure.";
            case NEXUS_SURGE -> "Move away from Nexus/radiation sources and expect nearby machines to surge.";
            default -> "No active event.";
        };
    }

    private static String survivalImpact(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> "Radiation pressure rises in exposed terrain.";
            case TOXIC_STORM -> "Vanilla rain is forced and acid/toxic route pressure is elevated.";
            case BLACKOUT -> "Power networks fall back to emergency reserve behavior.";
            case ASH_STORM -> "Exposed players lose hydration and filter charge; threats can spawn nearby.";
            case CRYO_FRONT -> "Exposed players lose body heat without thermal protection.";
            case NEXUS_SURGE -> "Nexus/radiation exposure spikes and machines can receive surge pulses.";
            default -> "No active weather pressure.";
        };
    }
}
