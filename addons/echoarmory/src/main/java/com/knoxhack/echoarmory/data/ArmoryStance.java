package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum ArmoryStance {
   BALANCED("Balanced", 1.0F, 20),
   QUICK("Quick", 0.82F, 12),
   HEAVY("Heavy", 1.28F, 32);

   public static final Codec<ArmoryStance> CODEC = Codec.STRING.xmap(ArmoryStance::byName, ArmoryStance::name);
   public static final StreamCodec<RegistryFriendlyByteBuf, ArmoryStance> STREAM_CODEC =
      ByteBufCodecs.idMapper(ArmoryStance::byId, ArmoryStance::ordinal).cast();
   private final String label;
   private final float damageScale;
   private final int cooldownTicks;

   ArmoryStance(String label, float damageScale, int cooldownTicks) {
      this.label = label;
      this.damageScale = damageScale;
      this.cooldownTicks = cooldownTicks;
   }

   public String label() {
      return label;
   }

   public float damageScale() {
      return damageScale;
   }

   public int cooldownTicks() {
      return cooldownTicks;
   }

   public ArmoryStance next() {
      return values()[(ordinal() + 1) % values().length];
   }

   public static ArmoryStance byId(int id) {
      ArmoryStance[] values = values();
      return id >= 0 && id < values.length ? values[id] : BALANCED;
   }

   public static ArmoryStance byName(String name) {
      if (name != null) {
         for (ArmoryStance stance : values()) {
            if (stance.name().equalsIgnoreCase(name) || stance.label.equalsIgnoreCase(name)) {
               return stance;
            }
         }
      }
      return BALANCED;
   }
}
