package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.IrradiatedWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class IrradiatedWolfRenderer extends AshfallBoardQuadrupedRenderer<IrradiatedWolf> {
    public IrradiatedWolfRenderer(EntityRendererProvider.Context context) {
        super(context, "irradiated_wolf", 0.5F);
    }
}
