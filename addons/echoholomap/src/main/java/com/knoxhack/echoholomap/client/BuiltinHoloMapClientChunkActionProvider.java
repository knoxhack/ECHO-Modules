package com.knoxhack.echoholomap.client;

import com.knoxhack.echoholomap.map.BuiltinHoloMapChunkActionProvider;
import java.util.List;
import net.minecraft.resources.Identifier;

public enum BuiltinHoloMapClientChunkActionProvider implements IHoloMapClientChunkActionProvider {
    INSTANCE;

    @Override
    public Identifier providerId() {
        return BuiltinHoloMapChunkActionProvider.PROVIDER_ID;
    }

    @Override
    public List<HoloMapChunkMenuAction> actions(String dimension, int chunkX, int chunkZ) {
        return List.of(new HoloMapChunkMenuAction(
                providerId(),
                BuiltinHoloMapChunkActionProvider.CREATE_PERSONAL_WAYPOINT,
                "Save waypoint",
                true,
                HoloMapVisualStyle.SUCCESS));
    }
}
