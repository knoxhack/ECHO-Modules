package com.knoxhack.echorendercore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class RenderCoreAnchorDebugRenderer {
   private static final int ANCHOR_COLOR = 0xFF66E8FF;
   private static final int BOUNDS_COLOR = 0xFFFFB347;

   private RenderCoreAnchorDebugRenderer() {
   }

   public static void render(PoseStack poseStack, Vec3 camera) {
      if (!DebugVisualOverrides.anchorsEnabled()) {
         return;
      }
      VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderTypes.lines());
      for (RenderCoreDebugTargets.DebugTarget target : RenderCoreDebugTargets.visibleTargets()) {
         renderBox(poseStack, consumer, target.bounds(), camera, BOUNDS_COLOR, 0.45F);
         for (Vec3 anchor : target.anchorPositions().values()) {
            renderBox(poseStack, consumer, box(anchor, 0.055D), camera, ANCHOR_COLOR, 1.0F);
         }
      }
   }

   private static void renderBox(PoseStack poseStack, VertexConsumer consumer, AABB box, Vec3 camera, int color, float alpha) {
      if (box == null || camera == null) {
         return;
      }
      poseStack.pushPose();
      poseStack.translate(-camera.x, -camera.y, -camera.z);
      ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(box), 0, 0, 0, color, alpha);
      poseStack.popPose();
   }

   private static AABB box(Vec3 center, double radius) {
      return new AABB(
         center.x - radius,
         center.y - radius,
         center.z - radius,
         center.x + radius,
         center.y + radius,
         center.z + radius
      );
   }
}
