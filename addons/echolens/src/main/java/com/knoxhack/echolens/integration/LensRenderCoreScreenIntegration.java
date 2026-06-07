package com.knoxhack.echolens.integration;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions;
import com.knoxhack.echorendercore.client.RenderCoreScreenVisuals;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class LensRenderCoreScreenIntegration {
   private static final Identifier LENS_PROFILE = Identifier.fromNamespaceAndPath(EchoLens.MODID, "screen/lens_overlay");
   private static final RenderCoreScreenFrameOptions LENS_FRAME_OPTIONS =
      RenderCoreScreenFrameOptions.hologram("")
         .drawLabel(false)
         .backdrop(false)
         .scanlines(false)
         .cornerBrackets(true)
         .glassGlints(true)
         .quietFallback(true)
         .build();

   private LensRenderCoreScreenIntegration() {
   }

   public static boolean drawLensFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
      RenderCoreScreenVisuals.drawFrame(graphics, Minecraft.getInstance().font, () -> LENS_PROFILE,
         x, y, width, height, RenderCoreScreenFrameOptions.fromProfile(RenderCoreProfiles.visual(LENS_PROFILE), LENS_FRAME_OPTIONS));
      return true;
   }
}
