package com.knoxhack.echorelictech.api.failure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record FailureEntry(
        FailureSeverity severity,
        int weight,
        String message,
        List<FailureEffectType> effects
) {
    public static final Codec<FailureEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FailureSeverity.CODEC.fieldOf("severity").forGetter(FailureEntry::severity),
            Codec.INT.fieldOf("weight").forGetter(FailureEntry::weight),
            Codec.STRING.fieldOf("message").forGetter(FailureEntry::message),
            FailureEffectType.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(FailureEntry::effects)
    ).apply(instance, FailureEntry::new));
}
