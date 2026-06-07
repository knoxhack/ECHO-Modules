package com.knoxhack.echorendercore.particle;

import com.knoxhack.echorendercore.EchoRenderCore;
import net.minecraft.core.particles.ParticleType;
import java.util.List;
import java.util.function.Supplier;

public final class RenderCoreParticles {
   private static final List<NativeParticleHolder<? extends ParticleType<?>>> PARTICLES = new java.util.ArrayList<>();

   public static final NativeParticleHolder<RenderCoreSoftParticleType> SOFT_MOTE =
      register("soft_mote", new RenderCoreSoftParticleType(false));
   public static final NativeParticleHolder<RenderCoreSoftParticleType> SOFT_WISP =
      register("soft_wisp", new RenderCoreSoftParticleType(false));

   private RenderCoreParticles() {
   }

   public static List<NativeParticleHolder<? extends ParticleType<?>>> particles() {
      return List.copyOf(PARTICLES);
   }

   public static void register() {
   }

   private static <T extends ParticleType<?>> NativeParticleHolder<T> register(String id, T particleType) {
      NativeParticleHolder<T> holder = new NativeParticleHolder<>(EchoRenderCore.MODID + ":" + id, particleType);
      PARTICLES.add(holder);
      return holder;
   }

   public record NativeParticleHolder<T extends ParticleType<?>>(String id, T value) implements Supplier<T> {
      @Override
      public T get() {
         return value;
      }
   }
}
