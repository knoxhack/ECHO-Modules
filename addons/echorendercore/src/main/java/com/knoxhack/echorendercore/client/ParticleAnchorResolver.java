package com.knoxhack.echorendercore.client;

import com.knoxhack.echocore.client.model.EchoNamedModelPartProvider;
import com.knoxhack.echorendercore.profile.RenderCoreAnchor;
import com.knoxhack.echorendercore.profile.RenderCoreVector;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ParticleAnchorResolver {
   private ParticleAnchorResolver() {
   }

   public static Vec3 resolve(Entity entity, VisualProfile profile, String anchorName) {
      return resolve(entity, profile, anchorName, null);
   }

   public static Vec3 resolve(Entity entity, VisualProfile profile, String anchorName, EntityModel<?> model) {
      if (entity == null) {
         return Vec3.ZERO;
      }
      RenderCoreVector offset = RenderCoreVector.ZERO;
      String partName = anchorName == null ? "" : anchorName;
      if (profile != null) {
         RenderCoreAnchor anchor = profile.anchor(anchorName);
         if (anchor != null) {
            offset = anchor.offset();
            if (!anchor.part().isBlank()) {
               partName = anchor.part();
            }
         }
      }
      RenderCoreVector animatedPartDelta = animatedPartDelta(model, partName);
      offset = new RenderCoreVector(
         offset.x() + animatedPartDelta.x(),
         offset.y() + animatedPartDelta.y(),
         offset.z() + animatedPartDelta.z()
      );
      float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
      double sin = Mth.sin(yaw);
      double cos = Mth.cos(yaw);
      double x = offset.x() * cos - offset.z() * sin;
      double z = offset.x() * sin + offset.z() * cos;
      return entity.position().add(x, offset.y(), z);
   }

   public static Vec3 resolve(Vec3 origin, VisualProfile profile, String anchorName) {
      RenderCoreVector offset = RenderCoreVector.ZERO;
      if (profile != null) {
         RenderCoreAnchor anchor = profile.anchor(anchorName);
         if (anchor != null) {
            offset = anchor.offset();
         }
      }
      return (origin == null ? Vec3.ZERO : origin).add(offset.x(), offset.y(), offset.z());
   }

   private static RenderCoreVector animatedPartDelta(EntityModel<?> model, String partName) {
      if (model == null || partName == null || partName.isBlank()) {
         return RenderCoreVector.ZERO;
      }
      ModelPart part = namedParts(model).get(partName);
      if (part == null) {
         return RenderCoreVector.ZERO;
      }
      PartPose initialPose = part.getInitialPose();
      return new RenderCoreVector(
         (part.x - initialPose.x()) / 16.0F,
         -(part.y - initialPose.y()) / 16.0F,
         (part.z - initialPose.z()) / 16.0F
      );
   }

   private static Map<String, ModelPart> namedParts(EntityModel<?> model) {
      if (model instanceof EchoNamedModelPartProvider provider) {
         return provider.echoNamedModelParts();
      }
      if (model instanceof RenderCorePartProvider provider) {
         return provider.renderCoreParts();
      }
      return Map.of();
   }
}
