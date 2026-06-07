package com.knoxhack.echowiki.content;

import com.knoxhack.echowiki.EchoWiki;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record GuideBookTarget(Identifier guideBookId) {
    public static final GuideBookTarget EMPTY = new GuideBookTarget(EchoWiki.id("missing"));

    public static final Codec<GuideBookTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("guideBookId", "").forGetter(target -> target.guideBookId().toString()))
            .apply(instance, raw -> new GuideBookTarget(parse(raw))));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuideBookTarget> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            target -> target.guideBookId().toString(),
            raw -> new GuideBookTarget(parse(raw)));

    public GuideBookTarget {
        guideBookId = guideBookId == null ? EchoWiki.id("missing") : guideBookId;
    }

    private static Identifier parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return EchoWiki.id("missing");
        }
        Identifier parsed = Identifier.tryParse(raw.strip());
        return parsed == null ? EchoWiki.id("missing") : parsed;
    }
}
