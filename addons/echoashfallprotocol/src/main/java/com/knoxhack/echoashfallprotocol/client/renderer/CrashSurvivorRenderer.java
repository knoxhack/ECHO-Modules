package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.entity.CrashSurvivor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CrashSurvivorRenderer extends AshfallBoardHumanoidRenderer<CrashSurvivor> {
    public CrashSurvivorRenderer(EntityRendererProvider.Context context) {
        super(context, "crash_survivor", 0.5F);
    }
}
