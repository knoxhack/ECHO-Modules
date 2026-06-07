package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.profile.VisualProfile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public abstract class AdvancedEntityVisualRenderer<T extends Entity, S extends EntityRenderState> extends EntityRenderer<T, S> {
   protected AdvancedEntityVisualRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   protected VisualProfile profile(VisualContext context) {
      return com.knoxhack.echorendercore.profile.RenderCoreProfiles.visual(context.profileId());
   }
}
