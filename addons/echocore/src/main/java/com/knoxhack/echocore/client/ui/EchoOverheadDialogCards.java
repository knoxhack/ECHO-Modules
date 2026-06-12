package com.knoxhack.echocore.client.ui;

import com.knoxhack.echocore.client.model.EchoMobRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

public final class EchoOverheadDialogCards {
    private EchoOverheadDialogCards() {
    }

    public static void configure(EchoMobRenderState state, Entity entity, double range, String title,
            String status, int color, float yOffset) {
        if (state == null) {
            return;
        }
        state.dialogTitle = title == null ? "" : title;
        state.dialogStatus = status == null ? "" : status;
        state.dialogColor = color;
        state.dialogYOffset = yOffset;
        state.dialogRange = range;
        state.dialogCardVisible = !state.dialogTitle.isBlank() || !state.dialogStatus.isBlank();
        state.overheadDialogVisible = state.dialogCardVisible;
    }

    public static void submit(EchoMobRenderState state, Font font, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // SubmitNodeCollector text hooks are version-sensitive; the state contract is kept stable here.
    }
}
