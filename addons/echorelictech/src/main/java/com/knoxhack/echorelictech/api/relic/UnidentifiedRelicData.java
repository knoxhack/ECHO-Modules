package com.knoxhack.echorelictech.api.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record UnidentifiedRelicData(
        Identifier targetRelicId,
        int seed
) {
    private static final Codec<Identifier> IDENTIFIER_CODEC = Codec.STRING.xmap(Identifier::parse, Identifier::toString);

    public static final Codec<UnidentifiedRelicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTIFIER_CODEC.fieldOf("target_relic_id").forGetter(UnidentifiedRelicData::targetRelicId),
            Codec.INT.optionalFieldOf("seed", 0).forGetter(UnidentifiedRelicData::seed)
    ).apply(instance, UnidentifiedRelicData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnidentifiedRelicData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString), UnidentifiedRelicData::targetRelicId,
            ByteBufCodecs.VAR_INT, UnidentifiedRelicData::seed,
            UnidentifiedRelicData::new);
}
