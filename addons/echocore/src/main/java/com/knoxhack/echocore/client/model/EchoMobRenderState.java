package com.knoxhack.echocore.client.model;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class EchoMobRenderState extends HumanoidRenderState {
    public int tint = 0xFFFFFFFF;
    public boolean dialogCardVisible;
    public boolean overheadDialogVisible;
    public String dialogTitle = "";
    public String dialogStatus = "";
    public int dialogColor = 0xFFFFFFFF;
    public float dialogYOffset = 0.3F;
    public double dialogRange = 8.0D;

    public void clearDialogCard() {
        dialogCardVisible = false;
        overheadDialogVisible = false;
        dialogTitle = "";
        dialogStatus = "";
    }
}
