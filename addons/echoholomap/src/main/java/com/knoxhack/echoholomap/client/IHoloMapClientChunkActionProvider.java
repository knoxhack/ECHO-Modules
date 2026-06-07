package com.knoxhack.echoholomap.client;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface IHoloMapClientChunkActionProvider {
    Identifier providerId();

    List<HoloMapChunkMenuAction> actions(String dimension, int chunkX, int chunkZ);
}
