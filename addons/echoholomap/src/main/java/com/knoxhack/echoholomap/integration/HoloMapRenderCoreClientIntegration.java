package com.knoxhack.echoholomap.integration;

import com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions;
import com.knoxhack.echorendercore.client.RenderCoreScreenVisuals;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echoholomap.EchoHoloMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class HoloMapRenderCoreClientIntegration {
   private static final Identifier MINIMAP_PROFILE = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "screen/minimap");
   private static final RenderCoreScreenFrameOptions MINIMAP_FRAME_OPTIONS =
      RenderCoreScreenFrameOptions.hologram("HOLOMAP")
         .backdrop(false)
         .scanlines(false)
         .chromaticEdge(true)
         .build();

   private HoloMapRenderCoreClientIntegration() {
   }

   public static void drawMinimapFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
      RenderCoreScreenVisuals.drawFrame(graphics, Minecraft.getInstance().font, () -> MINIMAP_PROFILE,
         x, y, width, height, RenderCoreScreenFrameOptions.fromProfile(RenderCoreProfiles.visual(MINIMAP_PROFILE), MINIMAP_FRAME_OPTIONS));
   }
}
