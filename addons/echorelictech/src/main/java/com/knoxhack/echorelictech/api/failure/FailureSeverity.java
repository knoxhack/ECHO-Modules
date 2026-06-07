package com.knoxhack.echorelictech.api.failure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;

public enum FailureSeverity implements StringRepresentable {
    MINOR, MEDIUM, MAJOR, CRITICAL;

    public static final Codec<FailureSeverity> CODEC = StringRepresentable.fromEnum(FailureSeverity::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
