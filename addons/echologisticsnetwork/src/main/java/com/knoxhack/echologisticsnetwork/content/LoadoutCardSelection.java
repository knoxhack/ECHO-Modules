package com.knoxhack.echologisticsnetwork.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LoadoutCardSelection(String loadoutId) {
   public static final LoadoutCardSelection EMPTY = new LoadoutCardSelection("");

   public static final Codec<LoadoutCardSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("loadoutId", "").forGetter(LoadoutCardSelection::loadoutId)
   ).apply(instance, LoadoutCardSelection::new));

   public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutCardSelection> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      LoadoutCardSelection::loadoutId,
      LoadoutCardSelection::new
   );

   public LoadoutCardSelection {
      loadoutId = loadoutId == null ? "" : loadoutId.strip();
   }
}
