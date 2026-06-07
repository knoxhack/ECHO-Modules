package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.RustWalker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RustWalkerRenderer extends AshfallBoardHeavyBossRenderer<RustWalker> {
    public RustWalkerRenderer(EntityRendererProvider.Context context) {
        super(context, "rust_walker", 0.7F);
    }
}
