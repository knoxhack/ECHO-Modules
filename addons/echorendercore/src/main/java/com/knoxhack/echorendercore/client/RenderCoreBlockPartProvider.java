package com.knoxhack.echorendercore.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

import java.util.List;
import java.util.Map;

public interface RenderCoreBlockPartProvider {
   Map<String, List<BlockStateModelPart>> renderCoreBlockParts();
}
