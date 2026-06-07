package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.SteamWraith;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SteamWraithRenderer extends AshfallBoardWraithRenderer<SteamWraith> {
    public SteamWraithRenderer(EntityRendererProvider.Context context) {
        super(context, "steam_wraith", 0.4F);
    }
}
