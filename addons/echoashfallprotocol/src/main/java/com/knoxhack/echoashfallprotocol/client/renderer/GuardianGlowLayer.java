package com.knoxhack.echoashfallprotocol.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

class GuardianGlowLayer extends RenderLayer<BiomeBossRenderer.State, GuardianBossModel<BiomeBossRenderer.State>> {
    GuardianGlowLayer(RenderLayerParent<BiomeBossRenderer.State, GuardianBossModel<BiomeBossRenderer.State>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       BiomeBossRenderer.State state, float yRot, float xRot) {
        if (state.glowTexture == null || state.isInvisible) {
            return;
        }

        OrderedSubmitNodeCollector ordered = collector.order(1);
        ordered.submitModel(
                getParentModel(),
                state,
                poseStack,
                RenderTypes.eyes(state.glowTexture),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                state.glowColor,
                null,
                state.outlineColor,
                null
        );
    }
}
