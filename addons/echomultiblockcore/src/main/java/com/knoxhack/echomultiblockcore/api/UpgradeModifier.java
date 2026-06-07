package com.knoxhack.echomultiblockcore.api;

import java.util.Locale;

public record UpgradeModifier(Type type, double value) {
    public UpgradeModifier {
        type = type == null ? Type.SPEED_MULTIPLIER : type;
    }

    public enum Type {
        SPEED_MULTIPLIER,
        REACH_BONUS,
        STRENGTH_BONUS,
        HEAT_REDUCTION,
        INTEGRITY_BONUS,
        STORAGE_BONUS,
        CAPABILITY_BONUS,
        AUTO_BUILDER_SPEED,
        POWER_COST_MULTIPLIER,
        EMERGENCY_SHUTDOWN;

        public static Type byName(String raw) {
            try {
                return valueOf(raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (RuntimeException exception) {
                return SPEED_MULTIPLIER;
            }
        }
    }
}
