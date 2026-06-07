package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CosmeticTrim(String sigil, int color) {
   public static final CosmeticTrim EMPTY = new CosmeticTrim("", 0x66E8FF);
   public static final Codec<CosmeticTrim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("sigil", "").forGetter(CosmeticTrim::sigil),
      Codec.INT.optionalFieldOf("color", 0x66E8FF).forGetter(CosmeticTrim::color)
   ).apply(instance, CosmeticTrim::new));
   public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticTrim> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, CosmeticTrim::sigil,
      ByteBufCodecs.VAR_INT, CosmeticTrim::color,
      CosmeticTrim::new
   );

   public CosmeticTrim {
      sigil = sigil == null ? "" : sigil.strip();
   }
}
