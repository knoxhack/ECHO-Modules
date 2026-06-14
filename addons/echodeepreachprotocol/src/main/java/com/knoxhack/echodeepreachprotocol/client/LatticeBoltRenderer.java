package com.knoxhack.echodeepreachprotocol.client;

import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import com.knoxhack.echodeepreachprotocol.client.model.LatticeBoltModel;
import com.knoxhack.echodeepreachprotocol.entity.LatticeBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class LatticeBoltRenderer extends EntityRenderer<LatticeBoltEntity, LatticeBoltRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            EchoDeepReachProtocol.MODID, "textures/entity/lattice_bolt.png");

    private final LatticeBoltModel model;

    public LatticeBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LatticeBoltModel(context.bakeLayer(LatticeBoltModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public LatticeBoltRenderState createRenderState() {
        return new LatticeBoltRenderState();
    }

    @Override
    public void extractRenderState(LatticeBoltEntity entity, LatticeBoltRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.xRot = entity.getXRot(partialTick);
    }

    @Override
    public void submit(LatticeBoltRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
