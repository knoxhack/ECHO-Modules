package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import net.minecraft.resources.Identifier;

/**
 * Client-only host contract for screens, HUDs, and overlays that want RenderCore visual styling.
 */
public interface RenderCoreScreenVisualHost {
   Identifier screenVisualProfileId();

   default VisualState screenVisualState() {
      return VisualState.ACTIVE;
   }

   default VisualVariant screenVisualVariant() {
      return VisualVariant.DEFAULT;
   }

   default float screenVisualProgress() {
      return 1.0F;
   }

   default String screenSurfaceType() {
      return "screen";
   }
}
