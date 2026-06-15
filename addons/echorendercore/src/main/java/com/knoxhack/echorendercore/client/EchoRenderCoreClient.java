package com.knoxhack.echorendercore.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.particle.RenderCoreParticles;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class EchoRenderCoreClient {
   private static final String ADD_CLIENT_RELOAD_LISTENERS_EVENT =
      "net.neoforged.neoforge.client.event.AddClientReloadListenersEvent";
   private static final String REGISTER_PARTICLE_PROVIDERS_EVENT =
      "net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent";

   public EchoRenderCoreClient() {
      this(null);
   }

   public EchoRenderCoreClient(Object modEventBus) {
      EchoBackendLifecycleBridge.registerModListener(
         modEventBus,
         ADD_CLIENT_RELOAD_LISTENERS_EVENT,
         EchoRenderCoreClient::registerClientReloadListeners);
      EchoBackendLifecycleBridge.registerModListener(
         modEventBus,
         REGISTER_PARTICLE_PROVIDERS_EVENT,
         EchoRenderCoreClient::registerParticleProviders);
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

   private static void registerClientReloadListeners(Object event) {
      clientReloadListeners().forEach((id, listener) ->
         EchoBackendClientBridge.addClientReloadListener(event, id, listener));
   }

   private static void registerParticleProviders(Object event) {
      EchoBackendClientBridge.registerParticleProvider(
         event,
         RenderCoreParticles.SOFT_MOTE.get(),
         sprites -> RenderCoreSoftParticle.provider(sprites, false));
      EchoBackendClientBridge.registerParticleProvider(
         event,
         RenderCoreParticles.SOFT_WISP.get(),
         sprites -> RenderCoreSoftParticle.provider(sprites, true));
   }
}
