package com.knoxhack.echoorbitalremnants.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class EmergencyRocketRenderState extends EntityRenderState {
    public float yRot;
    public int launchState;
    public int countdownTicks;
    public int launchTicks;
    public float ascentProgress;
}
