package com.knoxhack.signalos.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echorendercore.api.RenderCoreBlockVisualHost;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.client.RenderCoreBlockEntityRenderer;
import com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions;
import com.knoxhack.echorendercore.client.RenderCoreScreenVisuals;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.block.entity.SignalOsServerRackBlockEntity;
import com.knoxhack.signalos.block.entity.SignalOsTerminalBlockEntity;
import com.knoxhack.signalos.client.SignalOsServerRackScreen;
import com.knoxhack.signalos.client.SignalOsTerminalScreen;
import com.knoxhack.signalos.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class SignalOsRenderCoreClientIntegration {
   private static final Identifier TERMINAL_PROFILE = Identifier.fromNamespaceAndPath(SignalOS.MODID, "terminal");
   private static final Identifier RACK_PROFILE = Identifier.fromNamespaceAndPath(SignalOS.MODID, "server_rack");
   private static final Identifier TERMINAL_SCREEN_PROFILE = Identifier.fromNamespaceAndPath(SignalOS.MODID, "screen/terminal_hud");
   private static final Identifier RACK_SCREEN_PROFILE = Identifier.fromNamespaceAndPath(SignalOS.MODID, "screen/server_rack");
   private static final RenderCoreScreenFrameOptions TERMINAL_SCREEN_OPTIONS =
      RenderCoreScreenFrameOptions.terminal("SIGNALOS TERMINAL").build();
   private static final RenderCoreScreenFrameOptions RACK_SCREEN_OPTIONS =
      RenderCoreScreenFrameOptions.cyberglass("SIGNALOS RACK")
         .backdrop(false)
         .scanlines(false)
         .glassGlints(false)
         .chromaticEdge(true)
         .quietFallback(false)
         .build();
   private static boolean screenRegistered;

   private SignalOsRenderCoreClientIntegration() {
   }

   public static void registerBlockRenderers(Object event) {
      EchoBackendClientBridge.registerBlockEntityRenderer(event, ModBlockEntities.TERMINAL.get(),
         (BlockEntityRendererProvider) context ->
            new RenderCoreBlockEntityRenderer<>(context, SignalOsRenderCoreClientIntegration::terminalHost));
      EchoBackendClientBridge.registerBlockEntityRenderer(event, ModBlockEntities.SERVER_RACK.get(),
         (BlockEntityRendererProvider) context ->
            new RenderCoreBlockEntityRenderer<>(context, SignalOsRenderCoreClientIntegration::rackHost));
   }

   public static void registerScreenVisuals() {
      if (screenRegistered) {
         return;
      }
      screenRegistered = true;
      EchoBackendLifecycleBridge.registerGameEventHandler(SignalOsRenderCoreClientIntegration::renderScreenFrame);
   }

   private static RenderCoreBlockVisualHost terminalHost(SignalOsTerminalBlockEntity terminal, float partialTick) {
      return new RenderCoreBlockVisualHost() {
         @Override
         public Identifier visualProfileId() {
            return TERMINAL_PROFILE;
         }

         @Override
         public VisualState visualState() {
            return terminal.hasStoredRewards() ? VisualState.ACTIVE : VisualState.ONLINE;
         }

         @Override
         public float visualProgress() {
            return Math.min(1.0F, terminal.storedRewardCount() / 64.0F);
         }
      };
   }

   private static RenderCoreBlockVisualHost rackHost(SignalOsServerRackBlockEntity rack, float partialTick) {
      return new RenderCoreBlockVisualHost() {
         @Override
         public Identifier visualProfileId() {
            return RACK_PROFILE;
         }

         @Override
         public VisualState visualState() {
            return rack.driveCount() > 0 ? VisualState.ACTIVE : VisualState.IDLE;
         }

         @Override
         public float visualProgress() {
            return Math.min(1.0F, rack.driveCount() / (float)SignalOsServerRackBlockEntity.DRIVE_SLOTS);
         }
      };
   }

   private static void renderScreenFrame(Object event) {
      Object screen = invokeNoArg(event, "getScreen");
      Identifier profile = null;
      RenderCoreScreenFrameOptions options = null;
      if (screen instanceof SignalOsTerminalScreen) {
         profile = TERMINAL_SCREEN_PROFILE;
         options = RenderCoreScreenFrameOptions.fromProfile(RenderCoreProfiles.visual(profile), TERMINAL_SCREEN_OPTIONS);
      } else if (screen instanceof SignalOsServerRackScreen) {
         profile = RACK_SCREEN_PROFILE;
         options = RenderCoreScreenFrameOptions.fromProfile(RenderCoreProfiles.visual(profile), RACK_SCREEN_OPTIONS);
      }
      if (profile == null) {
         return;
      }
      Identifier finalProfile = profile;
      RenderCoreScreenVisuals.drawFrame(
         EchoBackendClientBridge.guiGraphics(event),
         Minecraft.getInstance().font,
         () -> finalProfile,
         6,
          6,
          Math.max(1, screenWidth(screen) - 12),
          Math.max(1, screenHeight(screen) - 12),
          options
       );
   }

   private static Object invokeNoArg(Object event, String methodName) {
      if (event == null) {
         return null;
      }
      try {
         return event.getClass().getMethod(methodName).invoke(event);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return null;
      }
   }

   private static int screenWidth(Object screen) {
      try {
         return screen == null ? 0 : screen.getClass().getField("width").getInt(screen);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return 0;
      }
   }

   private static int screenHeight(Object screen) {
      try {
         return screen == null ? 0 : screen.getClass().getField("height").getInt(screen);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return 0;
      }
   }
}
