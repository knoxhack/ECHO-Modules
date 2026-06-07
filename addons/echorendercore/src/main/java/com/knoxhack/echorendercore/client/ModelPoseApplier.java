package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.animation.ModelPose;
import com.knoxhack.echorendercore.animation.PartTransform;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public final class ModelPoseApplier {
   private ModelPoseApplier() {
   }

   public static void apply(Map<String, ModelPart> parts, ModelPose pose) {
      if (parts == null || pose == null || pose.isEmpty()) {
         return;
      }
      for (String partName : pose.parts()) {
         ModelPart part = parts.get(partName);
         if (part == null) {
            if (DebugVisualOverrides.missingPartWarnings()) {
               EchoRenderCore.LOGGER.warn("RenderCore animation skipped missing model part '{}'.", partName);
            }
            continue;
         }
         apply(part, pose.transform(partName));
      }
   }

   private static void apply(ModelPart part, PartTransform transform) {
      part.x += transform.x();
      part.y += transform.y();
      part.z += transform.z();
      part.xRot += (float)Math.toRadians(transform.xRot());
      part.yRot += (float)Math.toRadians(transform.yRot());
      part.zRot += (float)Math.toRadians(transform.zRot());
      part.xScale *= transform.xScale();
      part.yScale *= transform.yScale();
      part.zScale *= transform.zScale();
      part.visible = part.visible && transform.visible() && transform.alpha() > 0.001F;
   }
}
