package com.knoxhack.echorendercore.profile;

public record RenderCoreAnchor(String part, RenderCoreVector offset) {
   public RenderCoreAnchor {
      part = part == null ? "" : part.trim();
      offset = offset == null ? RenderCoreVector.ZERO : offset;
   }
}
