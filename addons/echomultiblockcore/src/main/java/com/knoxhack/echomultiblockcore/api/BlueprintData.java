package com.knoxhack.echomultiblockcore.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record BlueprintData(Identifier definitionId) {
    public static final BlueprintData EMPTY = new BlueprintData(Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown"));
    public static final Codec<BlueprintData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("definition", EMPTY.definitionId().toString())
                    .forGetter(data -> data.definitionId().toString())
    ).apply(instance, id -> new BlueprintData(Identifier.parse(id))));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            data -> data.definitionId().toString(),
            id -> new BlueprintData(Identifier.parse(id)));

    public BlueprintData {
        definitionId = definitionId == null ? EMPTY.definitionId() : definitionId;
    }
}
