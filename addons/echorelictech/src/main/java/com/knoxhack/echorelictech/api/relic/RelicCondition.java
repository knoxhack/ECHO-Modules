package com.knoxhack.echorelictech.api.relic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;

public enum RelicCondition implements StringRepresentable {
    UNKNOWN, DAMAGED, STABILIZED, OVERCLOCKED, CONTAINED, CORRUPTED;

    public static final Codec<RelicCondition> CODEC = StringRepresentable.fromEnum(RelicCondition::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean canActivate() {
        return this != UNKNOWN;
    }

    public float failureMultiplier() {
        return switch (this) {
            case UNKNOWN -> 1.0f;
            case DAMAGED -> 0.20f;
            case STABILIZED -> 0.06f;
            case OVERCLOCKED -> 0.18f;
            case CONTAINED -> 0.03f;
            case CORRUPTED -> 0.30f;
        };
    }
}
