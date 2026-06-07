package com.knoxhack.echorelictech.api.relic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;

public enum RelicTier implements StringRepresentable {
    FIELD, PROTOTYPE, FORBIDDEN, NEXUS;

    public static final Codec<RelicTier> CODEC = StringRepresentable.fromEnum(RelicTier::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int containmentPriority() {
        return switch (this) {
            case FIELD -> 1;
            case PROTOTYPE -> 2;
            case FORBIDDEN -> 3;
            case NEXUS -> 4;
        };
    }
}
