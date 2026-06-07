package com.knoxhack.echorendercore.client;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public interface RenderCorePartProvider {
   Map<String, ModelPart> renderCoreParts();
}
