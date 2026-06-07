package com.knoxhack.echorendercore.profile;

public record ProfileHotSwapResult(
   boolean accepted,
   RenderCoreProfiles.LoadedContent previous,
   RenderCoreProfiles.LoadedContent current,
   String message
) {
   public ProfileHotSwapResult {
      previous = previous == null ? RenderCoreProfiles.LoadedContent.EMPTY : previous;
      current = current == null ? previous : current;
      message = message == null ? "" : message;
   }
}
