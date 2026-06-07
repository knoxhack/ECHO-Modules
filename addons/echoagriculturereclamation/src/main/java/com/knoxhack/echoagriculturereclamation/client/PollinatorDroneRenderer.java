package com.knoxhack.echoagriculturereclamation.client;

import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.knoxhack.echocore.client.model.EchoMobRenderState;
import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class PollinatorDroneRenderer extends EchoMobFamilyRenderer<PollinatorDroneEntity> {
   public PollinatorDroneRenderer(EntityRendererProvider.Context context) {
      super(context, EchoAgricultureReclamation.MODID, "pollinator_drone", EchoMobFamily.DRONE, 0.78F, 0.28F);
   }

   @Override
   public void extractRenderState(PollinatorDroneEntity entity, EchoMobRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      EchoOverheadDialogCards.configure(state, entity, 8.0D, "Pollinator Drone", entity.dialogCardStatus(),
         0xFF8CFFB0, 0.35F);
   }
}
