package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.ToxicSlime;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ToxicSlimeRenderer extends AshfallBoardSlimeRenderer<ToxicSlime> {
    public ToxicSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, "toxic_slime", 0.35F);
    }
}
