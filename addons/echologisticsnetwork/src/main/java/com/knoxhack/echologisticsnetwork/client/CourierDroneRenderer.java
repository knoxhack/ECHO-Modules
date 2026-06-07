package com.knoxhack.echologisticsnetwork.client;

import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.knoxhack.echocore.client.model.EchoMobRenderState;
import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.entity.CourierDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class CourierDroneRenderer extends EchoMobFamilyRenderer<CourierDroneEntity> {
   public CourierDroneRenderer(EntityRendererProvider.Context context) {
      super(context, EchoLogisticsNetwork.MODID, "courier_drone", EchoMobFamily.DRONE, 1.0F, 0.35F);
   }

   @Override
   public void extractRenderState(CourierDroneEntity entity, EchoMobRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      EchoOverheadDialogCards.configure(state, entity, 8.0D, "Courier Drone", entity.dialogCardStatus(),
         0xFF66E8FF, 0.4F);
   }
}
