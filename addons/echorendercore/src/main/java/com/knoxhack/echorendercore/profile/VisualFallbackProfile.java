package com.knoxhack.echorendercore.profile;

public record VisualFallbackProfile(
   VisualFallbackMode mode,
   String expectation,
   boolean allowAdvancedFxFallback,
   boolean allowMissingAssets
) {
   public static final VisualFallbackProfile DEFAULT =
      new VisualFallbackProfile(VisualFallbackMode.STABLE, "stable RenderCore fallback remains readable", true, false);

   public VisualFallbackProfile {
      mode = mode == null ? VisualFallbackMode.STABLE : mode;
      expectation = expectation == null ? "" : expectation.trim();
   }
}
