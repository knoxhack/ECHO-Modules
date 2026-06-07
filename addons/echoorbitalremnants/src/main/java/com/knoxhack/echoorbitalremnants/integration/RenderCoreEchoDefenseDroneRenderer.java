package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobRenderState;
import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.knoxhack.echorendercore.client.EchoRenderCoreMobFamilyRenderer;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.entity.EchoDefenseDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class RenderCoreEchoDefenseDroneRenderer extends EchoRenderCoreMobFamilyRenderer<EchoDefenseDroneEntity> {
    public RenderCoreEchoDefenseDroneRenderer(EntityRendererProvider.Context context) {
        super(context, EchoOrbitalRemnants.MODID, "echo_defense_drone", EchoMobFamily.DRONE, 1.0F, 0.34F);
    }

    @Override
    public void extractRenderState(EchoDefenseDroneEntity entity, EchoMobRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        EchoOverheadDialogCards.configure(state, entity, 8.0D, "ECHO Defense Drone", entity.dialogCardStatus(),
                0xFF56D6FF, 0.4F);
    }
}
