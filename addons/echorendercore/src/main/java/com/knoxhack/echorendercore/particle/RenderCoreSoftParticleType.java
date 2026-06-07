package com.knoxhack.echorendercore.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class RenderCoreSoftParticleType extends ParticleType<RenderCoreSoftParticleOptions> {
   private final MapCodec<RenderCoreSoftParticleOptions> codec;
   private final StreamCodec<? super RegistryFriendlyByteBuf, RenderCoreSoftParticleOptions> streamCodec;

   public RenderCoreSoftParticleType(boolean overrideLimiter) {
      super(overrideLimiter);
      this.codec = RenderCoreSoftParticleOptions.codec(this);
      this.streamCodec = RenderCoreSoftParticleOptions.streamCodec(this);
   }

   @Override
   public MapCodec<RenderCoreSoftParticleOptions> codec() {
      return codec;
   }

   @Override
   public StreamCodec<? super RegistryFriendlyByteBuf, RenderCoreSoftParticleOptions> streamCodec() {
      return streamCodec;
   }
}
