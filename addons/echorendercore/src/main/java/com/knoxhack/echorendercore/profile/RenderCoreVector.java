package com.knoxhack.echorendercore.profile;

public record RenderCoreVector(float x, float y, float z) {
   public static final RenderCoreVector ZERO = new RenderCoreVector(0.0F, 0.0F, 0.0F);
}
