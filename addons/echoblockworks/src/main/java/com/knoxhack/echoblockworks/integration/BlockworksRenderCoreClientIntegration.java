package com.knoxhack.echoblockworks.integration;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echorendercore.client.RenderCoreStaticSurfaceRegistry;
import net.minecraft.resources.Identifier;

public final class BlockworksRenderCoreClientIntegration {
   private static boolean registered;

   private BlockworksRenderCoreClientIntegration() {
   }

   public static void registerStaticSurfaces() {
      if (registered) {
         return;
      }
      registered = true;
      register("broken_monitor");
      register("hologram_floor_projector");
      register("flickering_warning_light");
      register("echo_strip_light");
      register("sparking_cable_panel");
      register("steam_vent");
   }

   private static void register(String id) {
      RenderCoreStaticSurfaceRegistry.register(
         Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, id),
         Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, "static/" + id),
         "static_block"
      );
   }
}
