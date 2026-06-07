package com.knoxhack.echorelictech.api.relic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;

public enum RelicCategory implements StringRepresentable {
    MOBILITY, SURVIVAL, COMBAT, UTILITY, AI, WORLD, POWER, SCANNER, ARMOR, WEAPON;

    public static final Codec<RelicCategory> CODEC = StringRepresentable.fromEnum(RelicCategory::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
