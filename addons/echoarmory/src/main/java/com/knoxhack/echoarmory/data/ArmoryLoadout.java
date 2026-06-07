package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ArmoryLoadout(String loadoutId, String label) {
   public static final ArmoryLoadout EMPTY = new ArmoryLoadout("", "");
   public static final Codec<ArmoryLoadout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("loadoutId", "").forGetter(ArmoryLoadout::loadoutId),
      Codec.STRING.optionalFieldOf("label", "").forGetter(ArmoryLoadout::label)
   ).apply(instance, ArmoryLoadout::new));
   public static final StreamCodec<RegistryFriendlyByteBuf, ArmoryLoadout> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, ArmoryLoadout::loadoutId,
      ByteBufCodecs.STRING_UTF8, ArmoryLoadout::label,
      ArmoryLoadout::new
   );

   public ArmoryLoadout {
      loadoutId = loadoutId == null ? "" : loadoutId.strip();
      label = label == null ? "" : label.strip();
   }
}
