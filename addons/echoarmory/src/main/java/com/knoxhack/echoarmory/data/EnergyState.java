package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EnergyState(int stored, int capacity, boolean overloaded) {
   public static final EnergyState EMPTY = new EnergyState(0, 0, false);
   public static final Codec<EnergyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.optionalFieldOf("stored", 0).forGetter(EnergyState::stored),
      Codec.INT.optionalFieldOf("capacity", 0).forGetter(EnergyState::capacity),
      Codec.BOOL.optionalFieldOf("overloaded", false).forGetter(EnergyState::overloaded)
   ).apply(instance, EnergyState::new));
   public static final StreamCodec<RegistryFriendlyByteBuf, EnergyState> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, EnergyState::stored,
      ByteBufCodecs.VAR_INT, EnergyState::capacity,
      ByteBufCodecs.BOOL, EnergyState::overloaded,
      EnergyState::new
   );

   public EnergyState {
      capacity = Math.max(0, capacity);
      stored = Math.max(0, Math.min(stored, capacity));
   }

   public EnergyState charged() {
      return new EnergyState(Math.max(stored, capacity), capacity, false);
   }

   public EnergyState spend(int amount) {
      return new EnergyState(Math.max(0, stored - Math.max(0, amount)), capacity, overloaded);
   }
}
