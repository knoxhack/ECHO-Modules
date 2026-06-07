package com.knoxhack.echorendercore.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record RenderCoreSoftParticleOptions(
   ParticleType<RenderCoreSoftParticleOptions> type,
   int color,
   float scale,
   int lifetime,
   float alpha,
   float drag,
   boolean fade,
   String style,
   boolean fullbright,
   float gravity,
   float spin,
   float stretch,
   float flicker
) implements ParticleOptions {
   public static final int DEFAULT_COLOR = 0xFFFFFFFF;
   public static final float DEFAULT_SCALE = 0.55F;
   public static final int DEFAULT_LIFETIME = 24;
   public static final float DEFAULT_ALPHA = 0.85F;
   public static final float DEFAULT_DRAG = 0.92F;
   public static final String DEFAULT_STYLE = "";
   public static final float DEFAULT_GRAVITY = 0.0F;
   public static final float DEFAULT_SPIN = 0.015F;
   public static final float DEFAULT_STRETCH = 1.0F;
   public static final float DEFAULT_FLICKER = 0.0F;

   public RenderCoreSoftParticleOptions {
      scale = Math.max(0.01F, scale);
      lifetime = Math.max(1, lifetime);
      alpha = Mth.clamp(alpha, 0.0F, 1.0F);
      drag = Mth.clamp(drag, 0.0F, 1.0F);
      style = style == null ? DEFAULT_STYLE : style.trim().toLowerCase(Locale.ROOT);
      stretch = Math.max(0.1F, stretch);
      flicker = Mth.clamp(flicker, 0.0F, 1.0F);
   }

   public static MapCodec<RenderCoreSoftParticleOptions> codec(ParticleType<RenderCoreSoftParticleOptions> type) {
      return RecordCodecBuilder.mapCodec(instance -> instance.group(
         Codec.INT.optionalFieldOf("color", DEFAULT_COLOR).forGetter(RenderCoreSoftParticleOptions::color),
         Codec.FLOAT.optionalFieldOf("scale", DEFAULT_SCALE).forGetter(RenderCoreSoftParticleOptions::scale),
         Codec.INT.optionalFieldOf("lifetime", DEFAULT_LIFETIME).forGetter(RenderCoreSoftParticleOptions::lifetime),
         Codec.FLOAT.optionalFieldOf("alpha", DEFAULT_ALPHA).forGetter(RenderCoreSoftParticleOptions::alpha),
         Codec.FLOAT.optionalFieldOf("drag", DEFAULT_DRAG).forGetter(RenderCoreSoftParticleOptions::drag),
         Codec.BOOL.optionalFieldOf("fade", true).forGetter(RenderCoreSoftParticleOptions::fade),
         Codec.STRING.optionalFieldOf("style", DEFAULT_STYLE).forGetter(RenderCoreSoftParticleOptions::style),
         Codec.BOOL.optionalFieldOf("fullbright", true).forGetter(RenderCoreSoftParticleOptions::fullbright),
         Codec.FLOAT.optionalFieldOf("gravity", DEFAULT_GRAVITY).forGetter(RenderCoreSoftParticleOptions::gravity),
         Codec.FLOAT.optionalFieldOf("spin", DEFAULT_SPIN).forGetter(RenderCoreSoftParticleOptions::spin),
         Codec.FLOAT.optionalFieldOf("stretch", DEFAULT_STRETCH).forGetter(RenderCoreSoftParticleOptions::stretch),
         Codec.FLOAT.optionalFieldOf("flicker", DEFAULT_FLICKER).forGetter(RenderCoreSoftParticleOptions::flicker)
      ).apply(instance, (color, scale, lifetime, alpha, drag, fade, style, fullbright, gravity, spin, stretch, flicker) ->
         new RenderCoreSoftParticleOptions(type, color, scale, lifetime, alpha, drag, fade, style, fullbright, gravity, spin, stretch, flicker)));
   }

   public static StreamCodec<? super RegistryFriendlyByteBuf, RenderCoreSoftParticleOptions> streamCodec(
         ParticleType<RenderCoreSoftParticleOptions> type) {
      return StreamCodec.ofMember(
         (options, buffer) -> {
            buffer.writeInt(options.color());
            buffer.writeFloat(options.scale());
            buffer.writeVarInt(options.lifetime());
            buffer.writeFloat(options.alpha());
            buffer.writeFloat(options.drag());
            buffer.writeBoolean(options.fade());
            buffer.writeUtf(options.style());
            buffer.writeBoolean(options.fullbright());
            buffer.writeFloat(options.gravity());
            buffer.writeFloat(options.spin());
            buffer.writeFloat(options.stretch());
            buffer.writeFloat(options.flicker());
         },
         buffer -> new RenderCoreSoftParticleOptions(
            type,
            buffer.readInt(),
            buffer.readFloat(),
            buffer.readVarInt(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readBoolean(),
            buffer.readUtf(),
            buffer.readBoolean(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readFloat()
         )
      );
   }

   @Override
   public ParticleType<?> getType() {
      return type;
   }
}
