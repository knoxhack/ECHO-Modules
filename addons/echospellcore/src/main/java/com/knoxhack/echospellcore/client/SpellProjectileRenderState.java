package com.knoxhack.echospellcore.client;

import com.knoxhack.echospellcore.entity.SpellProjectileKind;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SpellProjectileRenderState extends EntityRenderState {
    public SpellProjectileKind kind = SpellProjectileKind.AETHER_BOLT;
    public int remainingLife;
}
