package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.particle.RenderCoreParticles;
import com.knoxhack.echorendercore.particle.RenderCoreSoftParticleOptions;
import com.knoxhack.echorendercore.particle.RenderCoreSoftParticleType;
import com.knoxhack.echorendercore.profile.ParticleEmitter;
import com.knoxhack.echorendercore.profile.ParticleOptionsSpec;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class RenderCoreParticleOptionResolvers {
   private static final Map<String, ParticleOptionResolver> RESOLVERS = new ConcurrentHashMap<>();

   static {
      register("simple", RenderCoreParticleOptionResolvers::simple);
      register("dust", emitter -> new DustParticleOptions(emitter.options().color(), emitter.options().scale()));
      register("minecraft:dust", emitter -> new DustParticleOptions(emitter.options().color(), emitter.options().scale()));
      register("dust_transition", RenderCoreParticleOptionResolvers::dustTransition);
      register("minecraft:dust_color_transition", RenderCoreParticleOptionResolvers::dustTransition);
      register("color", RenderCoreParticleOptionResolvers::color);
      register("entity_effect", RenderCoreParticleOptionResolvers::color);
      register("minecraft:entity_effect", RenderCoreParticleOptionResolvers::color);
      register("item", RenderCoreParticleOptionResolvers::item);
      register("minecraft:item", RenderCoreParticleOptionResolvers::item);
      register("block", RenderCoreParticleOptionResolvers::block);
      register("minecraft:block", RenderCoreParticleOptionResolvers::block);
      register("trail", RenderCoreParticleOptionResolvers::trail);
      register("minecraft:trail", RenderCoreParticleOptionResolvers::trail);
      register("soft_mote", emitter -> soft(emitter, RenderCoreParticles.SOFT_MOTE::get, false));
      register("rendercore:soft_mote", emitter -> soft(emitter, RenderCoreParticles.SOFT_MOTE::get, false));
      register("echorendercore:soft_mote", emitter -> soft(emitter, RenderCoreParticles.SOFT_MOTE::get, false));
      register("soft_wisp", emitter -> soft(emitter, RenderCoreParticles.SOFT_WISP::get, true));
      register("rendercore:soft_wisp", emitter -> soft(emitter, RenderCoreParticles.SOFT_WISP::get, true));
      register("echorendercore:soft_wisp", emitter -> soft(emitter, RenderCoreParticles.SOFT_WISP::get, true));
   }

   private RenderCoreParticleOptionResolvers() {
   }

   public static void register(String type, ParticleOptionResolver resolver) {
      if (type != null && !type.isBlank() && resolver != null) {
         RESOLVERS.put(normalize(type), resolver);
      }
   }

   public static ParticleOptions resolve(ParticleEmitter emitter) {
      if (emitter == null) {
         return null;
      }
      String requested = emitter.options().type();
      if (!requested.isBlank()) {
         ParticleOptionResolver resolver = RESOLVERS.get(normalize(requested));
         return resolver == null ? null : resolver.resolve(emitter);
      }
      if ("minecraft:dust".equals(emitter.particle().toString())) {
         return RESOLVERS.get("minecraft:dust").resolve(emitter);
      }
      if ("minecraft:item".equals(emitter.particle().toString())) {
         return item(emitter);
      }
      if ("minecraft:block".equals(emitter.particle().toString())) {
         return block(emitter);
      }
      if ("minecraft:trail".equals(emitter.particle().toString())) {
         return trail(emitter);
      }
      if ("echorendercore:soft_mote".equals(emitter.particle().toString())) {
         return soft(emitter, RenderCoreParticles.SOFT_MOTE::get, false);
      }
      if ("echorendercore:soft_wisp".equals(emitter.particle().toString())) {
         return soft(emitter, RenderCoreParticles.SOFT_WISP::get, true);
      }
      return simple(emitter);
   }

   private static ParticleOptions simple(ParticleEmitter emitter) {
      ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(emitter.particle()).orElse(null);
      return type instanceof SimpleParticleType particleType ? particleType : null;
   }

   private static ParticleOptions dustTransition(ParticleEmitter emitter) {
      int from = parseColor(first(emitter.options().option("from_color"), emitter.options().option("fromColor")), emitter.options().color());
      int to = parseColor(first(emitter.options().option("to_color"), emitter.options().option("toColor")), emitter.color());
      return new DustColorTransitionOptions(from, to, emitter.options().scale());
   }

   private static ParticleOptions color(ParticleEmitter emitter) {
      return ColorParticleOption.create(particleType(emitter, ParticleTypes.ENTITY_EFFECT), emitter.options().color());
   }

   private static ParticleOptions item(ParticleEmitter emitter) {
      String itemId = emitter.options().option("item");
      if (itemId == null || itemId.isBlank()) {
         return null;
      }
      var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(Items.AIR);
      return item == Items.AIR ? null : new ItemParticleOption(particleType(emitter, ParticleTypes.ITEM), item);
   }

   private static ParticleOptions block(ParticleEmitter emitter) {
      String blockId = first(emitter.options().option("block_state"), emitter.options().option("blockState"), emitter.options().option("block"));
      if (blockId == null || blockId.isBlank()) {
         return null;
      }
      String normalized = blockId.contains("[") ? blockId.substring(0, blockId.indexOf('[')) : blockId;
      var block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(normalized)).orElse(Blocks.AIR);
      return block == Blocks.AIR ? null : new BlockParticleOption(particleType(emitter, ParticleTypes.BLOCK), block.defaultBlockState());
   }

   private static ParticleOptions trail(ParticleEmitter emitter) {
      Vec3 target = parseVector(emitter.options().option("target"), Vec3.ZERO);
      int duration = Math.max(1, emitter.options().optionInt("duration", Math.max(1, emitter.options().lifetime())));
      return new TrailParticleOption(target, emitter.options().color(), duration);
   }

   private static ParticleOptions soft(ParticleEmitter emitter, Supplier<RenderCoreSoftParticleType> particleType, boolean wisp) {
      ParticleOptionsSpec options = emitter.options();
      int color = options.color();
      int lifetime = options.lifetime() > 0 ? options.lifetime() : (wisp ? 34 : 22);
      float defaultAlpha = Math.max(0.05F, ARGB.alphaFloat(color));
      float alpha = options.optionFloat("alpha", defaultAlpha);
      float drag = options.optionFloat("drag", wisp ? 0.90F : 0.94F);
      String style = first(options.option("style"), wisp ? "soft_wisp" : "soft_mote");
      return new RenderCoreSoftParticleOptions(
         particleType.get(),
         color,
         options.scale(),
         lifetime,
         alpha,
         drag,
         options.optionBool("fade", true),
         style,
         options.optionBool("fullbright", true),
         options.optionFloat("gravity", wisp ? -0.004F : 0.0F),
         options.optionFloat("spin", wisp ? 0.01F : 0.015F),
         options.optionFloat("stretch", 1.0F),
         options.optionFloat("flicker", 0.0F)
      );
   }

   @SuppressWarnings("unchecked")
   private static <T extends ParticleOptions> ParticleType<T> particleType(ParticleEmitter emitter, ParticleType<T> fallback) {
      return (ParticleType<T>)BuiltInRegistries.PARTICLE_TYPE.getOptional(emitter.particle()).orElse(fallback);
   }

   private static int parseColor(String value, int fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      String normalized = value.trim();
      try {
         if (normalized.startsWith("[") && normalized.endsWith("]")) {
            String[] values = normalized.substring(1, normalized.length() - 1).split(",");
            if (values.length >= 3) {
               int r = colorComponent(Float.parseFloat(values[0].trim()));
               int g = colorComponent(Float.parseFloat(values[1].trim()));
               int b = colorComponent(Float.parseFloat(values[2].trim()));
               int a = values.length > 3 ? colorComponent(Float.parseFloat(values[3].trim())) : 255;
               return (a << 24) | (r << 16) | (g << 8) | b;
            }
         }
         if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
         }
         if (normalized.length() == 6) {
            normalized = "FF" + normalized;
         }
         return (int)Long.parseLong(normalized, 16);
      } catch (RuntimeException ignored) {
         return fallback;
      }
   }

   private static int colorComponent(float value) {
      float normalized = value <= 1.0F ? value * 255.0F : value;
      return Math.round(Math.max(0.0F, Math.min(255.0F, normalized)));
   }

   private static Vec3 parseVector(String value, Vec3 fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      try {
         String normalized = value.trim();
         if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
         }
         String[] values = normalized.split(",");
         if (values.length >= 3) {
            return new Vec3(Double.parseDouble(values[0].trim()), Double.parseDouble(values[1].trim()), Double.parseDouble(values[2].trim()));
         }
      } catch (RuntimeException ignored) {
         return fallback;
      }
      return fallback;
   }

   private static String first(String... values) {
      for (String value : values) {
         if (value != null && !value.isBlank()) {
            return value;
         }
      }
      return "";
   }

   private static String normalize(String value) {
      return value.trim().toLowerCase(Locale.ROOT);
   }
}
