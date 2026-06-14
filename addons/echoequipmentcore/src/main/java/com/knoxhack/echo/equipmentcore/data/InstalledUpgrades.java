package com.knoxhack.echo.equipmentcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record InstalledUpgrades(List<String> upgrades) {
    public static final InstalledUpgrades EMPTY = new InstalledUpgrades(List.of());
    public static final Codec<InstalledUpgrades> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("upgrades", List.of()).forGetter(InstalledUpgrades::upgrades)
    ).apply(instance, InstalledUpgrades::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstalledUpgrades> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            InstalledUpgrades::upgrades,
            InstalledUpgrades::new
    );

    public InstalledUpgrades {
        upgrades = upgrades == null ? List.of() : upgrades.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .distinct()
                .limit(8)
                .toList();
    }

    public boolean contains(String upgradeId) {
        return upgradeId != null && upgrades.contains(upgradeId);
    }

    public InstalledUpgrades with(String upgradeId) {
        if (upgradeId == null || upgradeId.isBlank() || contains(upgradeId)) {
            return this;
        }
        java.util.ArrayList<String> next = new java.util.ArrayList<>(upgrades);
        next.add(upgradeId.strip());
        return new InstalledUpgrades(next);
    }
}
