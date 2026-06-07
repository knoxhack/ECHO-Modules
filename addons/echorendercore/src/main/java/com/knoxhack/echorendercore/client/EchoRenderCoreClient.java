package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.particle.RenderCoreParticles;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class EchoRenderCoreClient {
   public EchoRenderCoreClient() {
   }

   public static Map<Identifier, RenderCoreClientReloadListener> clientReloadListeners() {
      return Map.of(Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, "profiles"),
         new RenderCoreClientReloadListener());
   }

   public static List<NativeParticleProvider> particleProviders() {
      return List.of(
         new NativeParticleProvider(RenderCoreParticles.SOFT_MOTE.id(), false),
         new NativeParticleProvider(RenderCoreParticles.SOFT_WISP.id(), true));
   }

   public record NativeParticleProvider(String particleId, boolean wisp) {
   }
}
