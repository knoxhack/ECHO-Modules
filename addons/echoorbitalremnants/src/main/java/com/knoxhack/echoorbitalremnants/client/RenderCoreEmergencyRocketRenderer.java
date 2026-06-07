package com.knoxhack.echoorbitalremnants.client;

import com.knoxhack.echocore.client.model.EchoMobRenderIds;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echorendercore.client.RenderCoreEntityVisuals;
import com.knoxhack.echorendercore.client.VisualProfileRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RenderCoreEmergencyRocketRenderer extends EmergencyRocketRenderer {
    private static final Identifier PROFILE = EchoMobRenderIds.profile(
            EchoOrbitalRemnants.MODID, "emergency_rocket_vehicle");

    public RenderCoreEmergencyRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(EmergencyRocketEntity entity, EmergencyRocketRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        RenderCoreEntityVisuals.attach(entity, state, PROFILE, TEXTURE, partialTicks);
    }

    @Override
    public void submit(EmergencyRocketRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY,
                state.outlineColor, null);
        RenderCoreEntityVisuals.RenderData data = RenderCoreEntityVisuals.get(state);
        if (data != null && data.visualProfile() != null) {
            VisualProfileRenderer.submitEntityLayers(model, state, poseStack, collector, data.context(), data.visualProfile());
        }
        poseStack.popPose();
    }
}
