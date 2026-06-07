package com.knoxhack.echoconvoyprotocol.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class ConvoyVehicleRenderState extends EntityRenderState {
   public float yRot;
   public int kind;
   public float damageRatio;
   public float fuelRatio;
   public float batteryRatio;
   public float cargoRatio;
   public float shieldingRatio;
   public boolean docked;
   public boolean driven;
   public boolean hasTravelPower;
   public float speed;
   public Identifier renderCoreProfileId;
   public String renderCoreVisualState = "IDLE";
   public float renderCoreProgress;
   public float renderCorePartialTick;
   public boolean renderCoreMoving;
   public boolean renderCoreDamaged;
}
