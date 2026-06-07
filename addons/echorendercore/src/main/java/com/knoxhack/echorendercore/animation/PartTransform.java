package com.knoxhack.echorendercore.animation;

public record PartTransform(
   float x,
   float y,
   float z,
   float xRot,
   float yRot,
   float zRot,
   float xScale,
   float yScale,
   float zScale,
   boolean visible,
   float alpha
) {
   public static final PartTransform IDENTITY = new PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, true, 1.0F);

   public PartTransform blend(PartTransform next, float weight) {
      PartTransform target = next == null ? IDENTITY : next;
      float t = Math.max(0.0F, Math.min(1.0F, weight));
      return new PartTransform(
         lerp(x, target.x, t),
         lerp(y, target.y, t),
         lerp(z, target.z, t),
         lerp(xRot, target.xRot, t),
         lerp(yRot, target.yRot, t),
         lerp(zRot, target.zRot, t),
         lerp(xScale, target.xScale, t),
         lerp(yScale, target.yScale, t),
         lerp(zScale, target.zScale, t),
         t >= 0.5F ? target.visible : visible,
         lerp(alpha, target.alpha, t)
      );
   }

   public PartTransform add(PartTransform next) {
      PartTransform value = next == null ? IDENTITY : next;
      return new PartTransform(
         x + value.x,
         y + value.y,
         z + value.z,
         xRot + value.xRot,
         yRot + value.yRot,
         zRot + value.zRot,
         xScale + (value.xScale - 1.0F),
         yScale + (value.yScale - 1.0F),
         zScale + (value.zScale - 1.0F),
         visible && value.visible,
         alpha * value.alpha
      );
   }

   public PartTransform with(AnimationChannel channel, float value) {
      return switch (channel) {
         case POSITION_X -> new PartTransform(value, y, z, xRot, yRot, zRot, xScale, yScale, zScale, visible, alpha);
         case POSITION_Y -> new PartTransform(x, value, z, xRot, yRot, zRot, xScale, yScale, zScale, visible, alpha);
         case POSITION_Z -> new PartTransform(x, y, value, xRot, yRot, zRot, xScale, yScale, zScale, visible, alpha);
         case ROTATION_X -> new PartTransform(x, y, z, value, yRot, zRot, xScale, yScale, zScale, visible, alpha);
         case ROTATION_Y -> new PartTransform(x, y, z, xRot, value, zRot, xScale, yScale, zScale, visible, alpha);
         case ROTATION_Z -> new PartTransform(x, y, z, xRot, yRot, value, xScale, yScale, zScale, visible, alpha);
         case SCALE_X -> new PartTransform(x, y, z, xRot, yRot, zRot, value, yScale, zScale, visible, alpha);
         case SCALE_Y -> new PartTransform(x, y, z, xRot, yRot, zRot, xScale, value, zScale, visible, alpha);
         case SCALE_Z -> new PartTransform(x, y, z, xRot, yRot, zRot, xScale, yScale, value, visible, alpha);
         case VISIBILITY -> new PartTransform(x, y, z, xRot, yRot, zRot, xScale, yScale, zScale, value > 0.5F, alpha);
         case ALPHA -> new PartTransform(x, y, z, xRot, yRot, zRot, xScale, yScale, zScale, visible, Math.max(0.0F, Math.min(1.0F, value)));
      };
   }

   private static float lerp(float from, float to, float t) {
      return from + (to - from) * t;
   }
}
