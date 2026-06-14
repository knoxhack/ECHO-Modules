package com.knoxhack.echoterminal.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echorendercore.api.RenderCoreBlockVisualHost;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.client.RenderCoreBlockEntityRenderer;
import com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions;
import com.knoxhack.echorendercore.client.RenderCoreScreenVisuals;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.block.entity.EchoTerminalBlockEntity;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.registry.ModBlockEntities;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientRouteActionContext;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class TerminalRenderCoreClientIntegration {
   private static final String SCREEN_FRAME_ACTION = "terminal.screen.frame.render";
   private static final Identifier BLOCK_PROFILE = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "echo_terminal");
   private static final Identifier SCREEN_PROFILE = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "screen/terminal_hud");
   private static final RenderCoreScreenFrameOptions SCREEN_FRAME_OPTIONS =
      RenderCoreScreenFrameOptions.terminal("ECHO TERMINAL").build();
   private static final RenderCoreScreenFrameOptions REDUCED_SCREEN_FRAME_OPTIONS =
      RenderCoreScreenFrameOptions.terminal("ECHO TERMINAL")
         .scanlines(false)
         .edgeGlow(false)
         .glassGlints(false)
         .chromaticEdge(false)
         .build();
   private static final ThreadLocal<GuiGraphicsExtractor> CURRENT_GUI_GRAPHICS = new ThreadLocal<>();
   private static boolean screenRegistered;

   private TerminalRenderCoreClientIntegration() {
   }

   public static void registerBlockRenderer(Object event) {
      @SuppressWarnings({"rawtypes", "unchecked"})
      BlockEntityRendererProvider provider =
         context -> new RenderCoreBlockEntityRenderer<>(context, TerminalRenderCoreClientIntegration::host);
      EchoBackendClientBridge.registerBlockEntityRenderer(
         event,
         ModBlockEntities.ECHO_TERMINAL.get(),
         provider);
   }

   public static void registerScreenVisuals() {
      if (screenRegistered) {
         return;
      }
      screenRegistered = true;
      EchoBackendLifecycleBridge.registerGameEventHandler(TerminalRenderCoreClientIntegration::renderScreenFrame);
      registerNativeScreenFrameRoute();
   }

   private static RenderCoreBlockVisualHost host(EchoTerminalBlockEntity terminal, float partialTick) {
      return new RenderCoreBlockVisualHost() {
         @Override
         public Identifier visualProfileId() {
            return BLOCK_PROFILE;
         }

         @Override
         public VisualState visualState() {
            return terminal.getStoredRewardCount() > 0 ? VisualState.ACTIVE : VisualState.ONLINE;
         }

         @Override
         public float visualProgress() {
            return Math.min(1.0F, terminal.getStoredRewardCount() / 64.0F);
         }
      };
   }

   private static void renderScreenFrame(Object event) {
      Screen screen = EchoBackendClientBridge.screen(event);
      if (!EchoTerminalScreens.isManagedTerminalScreen(screen) || !TerminalClientOptions.useVisualAssets()) {
         return;
      }
      if (nativeLoaderActive()) {
         CURRENT_GUI_GRAPHICS.set(EchoBackendClientBridge.guiGraphics(event));
         try {
            EchoNativeClientRouteRegistries.get().dispatchStatus("terminal", SCREEN_FRAME_ACTION, Map.of(
               "source", "native_client_bridge",
               "eventType", "screen_render_post",
               "screenClass", screen.getClass().getName(),
               "screenWidth", screen.width,
               "screenHeight", screen.height,
               "partialTick", EchoBackendClientBridge.guiPartialTick(event)
            ));
         } finally {
            CURRENT_GUI_GRAPHICS.remove();
         }
         return;
      }
      drawScreenFrame(EchoBackendClientBridge.guiGraphics(event), screen.width, screen.height);
   }

   private static void registerNativeScreenFrameRoute() {
      if (!nativeLoaderActive()) {
         return;
      }
      EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
      if (registry == EchoNativeClientRouteRegistry.NOOP) {
         return;
      }
      registry.registerActions(EchoTerminal.MODID, "echoterminal:eui", "terminal", Map.of(
         SCREEN_FRAME_ACTION, Map.of(
            "kind", "terminal_screen_frame_render",
            "renderer", "echorendercore",
            "moduleId", EchoTerminal.MODID
         )
      ));
      registry.registerActionHandler("terminal", "echoterminal:eui:rendercore_screen_frame",
         TerminalRenderCoreClientIntegration::dispatchNativeScreenFrame);
   }

   public static boolean ensureNativeScreenFrameRouteRegisteredForNativeLoader() {
      registerNativeScreenFrameRoute();
      return nativeLoaderActive() && EchoNativeClientRouteRegistries.get() != EchoNativeClientRouteRegistry.NOOP;
   }

   private static boolean dispatchNativeScreenFrame(NativeClientRouteActionContext context) {
      if (!SCREEN_FRAME_ACTION.equals(context.actionId())) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (!EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen) || !TerminalClientOptions.useVisualAssets()) {
         return false;
      }
      GuiGraphicsExtractor graphics = currentGuiGraphics();
      if (graphics == null) {
         return false;
      }
      drawScreenFrame(graphics, minecraft.screen.width, minecraft.screen.height);
      return true;
   }

   private static GuiGraphicsExtractor currentGuiGraphics() {
      return CURRENT_GUI_GRAPHICS.get();
   }

   private static void drawScreenFrame(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
      int margin = 8;
      int x = margin;
      int y = margin;
      int w = Math.max(1, screenWidth - margin * 2);
      int h = Math.max(1, screenHeight - margin * 2);
      RenderCoreScreenVisuals.drawFrame(
         guiGraphics,
         Minecraft.getInstance().font,
         () -> SCREEN_PROFILE,
         x,
         y,
         w,
         h,
         screenFrameOptions()
      );
   }

   private static boolean nativeLoaderActive() {
      return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
   }

   private static RenderCoreScreenFrameOptions screenFrameOptions() {
      boolean reducedMotion = TerminalClientOptions.reduceMotion();
      return RenderCoreScreenFrameOptions.fromProfile(
         RenderCoreProfiles.visual(SCREEN_PROFILE),
         reducedMotion ? REDUCED_SCREEN_FRAME_OPTIONS : SCREEN_FRAME_OPTIONS,
         reducedMotion
      );
   }

   public static Identifier screenProfileForTests() {
      return SCREEN_PROFILE;
   }

   public static boolean shouldRenderScreenAccentForTests() {
      return TerminalClientOptions.useVisualAssets();
   }

   public static RenderCoreScreenFrameOptions screenFrameOptionsForTests(boolean reducedMotion) {
      return reducedMotion ? REDUCED_SCREEN_FRAME_OPTIONS : SCREEN_FRAME_OPTIONS;
   }
}
