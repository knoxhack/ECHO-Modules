package com.knoxhack.echoagriculturereclamation.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SeedProfile(String cropId, int contaminationTier, int stability) {
   public static final Codec<SeedProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("cropId", CropSpec.DEFAULT.path()).forGetter(SeedProfile::cropId),
      Codec.INT.optionalFieldOf("contaminationTier", 1).forGetter(SeedProfile::contaminationTier),
      Codec.INT.optionalFieldOf("stability", 25).forGetter(SeedProfile::stability)
   ).apply(instance, SeedProfile::new));

   public static final StreamCodec<RegistryFriendlyByteBuf, SeedProfile> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      SeedProfile::cropId,
      ByteBufCodecs.VAR_INT,
      SeedProfile::contaminationTier,
      ByteBufCodecs.VAR_INT,
      SeedProfile::stability,
      SeedProfile::new
   );

   public SeedProfile {
      cropId = CropSpec.byPath(cropId).path();
      contaminationTier = Math.max(0, Math.min(5, contaminationTier));
      stability = Math.max(0, Math.min(100, stability));
   }

   public CropSpec spec() {
      return CropSpec.byPath(cropId);
   }

   public SeedProfile stabilized() {
      return new SeedProfile(cropId, 0, 100);
   }

   public static SeedProfile contaminated(CropSpec spec, int tier, int stability) {
      return new SeedProfile(spec.path(), tier, stability);
   }
}
