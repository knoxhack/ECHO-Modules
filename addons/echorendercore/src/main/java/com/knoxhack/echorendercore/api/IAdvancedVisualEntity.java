package com.knoxhack.echorendercore.api;

import java.util.List;
import java.util.Map;
import com.knoxhack.echorendercore.profile.RenderCoreAnchor;
import net.minecraft.resources.Identifier;

public interface IAdvancedVisualEntity extends IVisualStateProvider {
   Identifier visualProfileId();

   default VisualVariant visualVariant() {
      return VisualVariant.DEFAULT;
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
      return profile == null ? "rendercore:entity" : profile.toString();
   }
}
