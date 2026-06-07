package com.knoxhack.echorelictech.api.failure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;

public enum FailureEffectType implements StringRepresentable {
    COOLDOWN_MULTIPLY, DAMAGE_ITEM, TELEPORT_OFFSET, ADD_DEBUFF,
    HOSTILE_SIGNAL, ADD_INSTABILITY, DRAIN_CHARGE, FIZZLE, FORCE_COOLDOWN;

    public static final Codec<FailureEffectType> CODEC = StringRepresentable.fromEnum(FailureEffectType::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
