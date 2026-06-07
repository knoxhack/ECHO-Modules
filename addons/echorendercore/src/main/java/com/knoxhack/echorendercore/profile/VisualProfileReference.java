package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record VisualProfileReference(
   Identifier profileId,
   Set<VisualState> states,
   Set<VisualVariant> variants
) {
   public VisualProfileReference {
      states = states == null ? Set.of() : Set.copyOf(states);
      variants = variants == null ? Set.of() : Set.copyOf(variants);
   }

   public boolean hasFilters() {
      return !states.isEmpty() || !variants.isEmpty();
   }
}
