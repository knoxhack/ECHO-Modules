package com.knoxhack.echorelictech.api.failure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import java.util.List;

public record FailureTable(
        Identifier id,
        List<FailureEntry> entries
) {
    private static final Codec<Identifier> IDENTIFIER_CODEC = Codec.STRING.xmap(Identifier::parse, Identifier::toString);

    public static final Codec<FailureTable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTIFIER_CODEC.fieldOf("id").forGetter(FailureTable::id),
            FailureEntry.CODEC.listOf().fieldOf("entries").forGetter(FailureTable::entries)
    ).apply(instance, FailureTable::new));

    public int totalWeight() {
        return entries.stream().mapToInt(FailureEntry::weight).sum();
    }

    public FailureEntry roll(RandomSource random) {
        int total = totalWeight();
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        int current = 0;
        for (FailureEntry entry : entries) {
            current += entry.weight();
            if (roll < current) return entry;
        }
        return entries.isEmpty() ? null : entries.getLast();
    }
}
