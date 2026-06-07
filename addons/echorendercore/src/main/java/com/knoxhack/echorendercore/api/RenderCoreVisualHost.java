package com.knoxhack.echorendercore.api;

import com.knoxhack.echorendercore.profile.RenderCoreAnchor;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public interface RenderCoreVisualHost extends IVisualStateProvider {
   Identifier visualProfileId();

   default VisualVariant visualVariant() {
      return VisualVariant.DEFAULT;
   }

   default float visualProgress() {
      return 0.0F;
   }

   default boolean visualMoving() {
      return false;
   }

   default boolean visualDamaged() {
      return false;
   }

   default Map<String, RenderCoreAnchor> visualAnchors() {
      return Map.of();
   }

   default List<String> visualNamedParts() {
      return List.of();
   }

   default List<Identifier> visualDependencies() {
      return List.of();
   }

   default String visualDebugTarget() {
      Identifier profile = visualProfileId();
      return profile == null ? "rendercore:host" : profile.toString();
   }
}
