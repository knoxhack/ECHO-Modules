package com.knoxhack.echoorbitalremnants.client;

import com.knoxhack.echocore.client.model.EchoMobRenderIds;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class EmergencyRocketRenderer extends EntityRenderer<EmergencyRocketEntity, EmergencyRocketRenderState> {
    protected static final Identifier TEXTURE = EchoMobRenderIds.baseTexture(
            EchoOrbitalRemnants.MODID, "emergency_rocket_vehicle");
    protected final EmergencyRocketModel model;

    public EmergencyRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EmergencyRocketModel(context.bakeLayer(EmergencyRocketModel.LAYER_LOCATION));
        this.shadowRadius = 0.7F;
    }

    @Override
    public EmergencyRocketRenderState createRenderState() {
        return new EmergencyRocketRenderState();
    }

    @Override
    public void extractRenderState(EmergencyRocketEntity entity, EmergencyRocketRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.launchState = entity.launchState().id();
        state.countdownTicks = entity.countdownTicks();
        state.launchTicks = entity.launchTicks();
        state.ascentProgress = state.launchState == EmergencyRocketEntity.LaunchState.LAUNCHING.id()
                ? Math.max(0.0F, Math.min(1.0F, (entity.launchTicks() + partialTicks) / EmergencyRocketEntity.ASCENT_TICKS))
                : 0.0F;
    }

    @Override
    public void submit(EmergencyRocketRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        submitNodeCollector.submitModel(
                model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
