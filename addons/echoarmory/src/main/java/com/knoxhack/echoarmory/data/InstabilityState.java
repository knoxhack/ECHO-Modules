package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record InstabilityState(int instability, int cooldownTicks) {
   public static final InstabilityState STABLE = new InstabilityState(0, 0);
   public static final Codec<InstabilityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.optionalFieldOf("instability", 0).forGetter(InstabilityState::instability),
      Codec.INT.optionalFieldOf("cooldownTicks", 0).forGetter(InstabilityState::cooldownTicks)
   ).apply(instance, InstabilityState::new));
   public static final StreamCodec<RegistryFriendlyByteBuf, InstabilityState> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, InstabilityState::instability,
      ByteBufCodecs.VAR_INT, InstabilityState::cooldownTicks,
      InstabilityState::new
   );

   public InstabilityState {
      instability = Math.max(0, Math.min(100, instability));
      cooldownTicks = Math.max(0, cooldownTicks);
   }

   public InstabilityState decay() {
      return new InstabilityState(Math.max(0, instability - 1), Math.max(0, cooldownTicks - 20));
   }
}
