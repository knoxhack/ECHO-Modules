package com.knoxhack.echoconvoyprotocol.client;

import com.knoxhack.echorendercore.animation.AnimationClip;
import com.knoxhack.echorendercore.animation.AnimationController;
import com.knoxhack.echorendercore.animation.ModelPose;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.client.ModelPoseApplier;
import com.knoxhack.echorendercore.client.RenderCorePartProvider;
import com.knoxhack.echorendercore.profile.AnimationProfile;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public final class ConvoyRenderCoreVehicleModel extends ConvoyVehicleModel implements RenderCorePartProvider {
   private final Map<String, ModelPart> renderCoreParts;
   private final ModelPose renderCorePose = new ModelPose();
   private final AnimationController renderCoreController = new AnimationController();

   public ConvoyRenderCoreVehicleModel(ModelPart root) {
      super(root);
      this.renderCoreParts = namedPartsForRenderCore();
   }

   @Override
   public Map<String, ModelPart> renderCoreParts() {
      return renderCoreParts;
   }

   @Override
   public void setupAnim(ConvoyVehicleRenderState state) {
      super.setupAnim(state);
      if (state.renderCoreProfileId == null) {
         return;
      }
      VisualProfile profile = RenderCoreProfiles.visual(state.renderCoreProfileId);
      if (profile == null || profile.animationProfile() == null) {
         return;
      }
      AnimationProfile animations = RenderCoreProfiles.animation(profile.animationProfile());
      if (animations == null) {
         return;
      }
      VisualState visualState = VisualState.byName(state.renderCoreVisualState, VisualState.IDLE);
      renderCoreController.transitionSeconds(profile.transitionSeconds());
      for (Map.Entry<VisualState, String> entry : profile.stateAnimations().entrySet()) {
         AnimationClip clip = animations.clip(entry.getValue());
         if (clip != null) {
            renderCoreController.bind(entry.getKey(), clip);
         }
      }
      renderCoreController.update(visualState, state.ageInTicks);
      renderCoreController.sample(state.ageInTicks, renderCorePose);
      ModelPoseApplier.apply(renderCoreParts, renderCorePose);
   }
}
