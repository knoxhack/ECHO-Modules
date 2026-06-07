package com.knoxhack.echoarmory.client;

import com.knoxhack.echoarmory.content.FiringModeDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class ArmoryProjectileRenderState extends EntityRenderState {
   public FiringModeDefinition.ProjectileKind kind = FiringModeDefinition.ProjectileKind.ENERGY_BOLT;
}
