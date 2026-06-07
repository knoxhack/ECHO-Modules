package com.knoxhack.echologisticsnetwork.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SupplyTagSelection(String categoryId) {
   public static final SupplyTagSelection EMPTY = new SupplyTagSelection("");

   public static final Codec<SupplyTagSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("categoryId", "").forGetter(SupplyTagSelection::categoryId)
   ).apply(instance, SupplyTagSelection::new));

   public static final StreamCodec<RegistryFriendlyByteBuf, SupplyTagSelection> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      SupplyTagSelection::categoryId,
      SupplyTagSelection::new
   );

   public SupplyTagSelection {
      categoryId = categoryId == null ? "" : categoryId.strip();
   }
}
