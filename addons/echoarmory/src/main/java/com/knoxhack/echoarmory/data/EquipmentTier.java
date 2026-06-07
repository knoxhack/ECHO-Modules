package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EquipmentTier(int tier, String stage) {
   public static final EquipmentTier TIER_1 = new EquipmentTier(1, "Early survival");
   public static final Codec<EquipmentTier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.optionalFieldOf("tier", 1).forGetter(EquipmentTier::tier),
      Codec.STRING.optionalFieldOf("stage", "Early survival").forGetter(EquipmentTier::stage)
   ).apply(instance, EquipmentTier::new));
   public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentTier> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, EquipmentTier::tier,
      ByteBufCodecs.STRING_UTF8, EquipmentTier::stage,
      EquipmentTier::new
   );

   public EquipmentTier {
      tier = Math.max(1, Math.min(4, tier));
      stage = stage == null || stage.isBlank() ? "Tier " + tier : stage.strip();
   }
}
