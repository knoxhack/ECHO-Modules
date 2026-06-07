package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.EchoDrone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class EchoDroneRenderer extends AshfallBoardDroneRenderer<EchoDrone> {
    public EchoDroneRenderer(EntityRendererProvider.Context context) {
        super(context, "echo_drone", 0.4F);
    }
}
