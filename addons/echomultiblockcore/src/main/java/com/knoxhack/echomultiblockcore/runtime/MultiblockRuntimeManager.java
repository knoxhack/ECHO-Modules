package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.api.MultiblockState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class MultiblockRuntimeManager {
    private MultiblockRuntimeManager() {
    }

    public static void recordFormed(ServerLevel level, Identifier definitionId, BlockPos controllerPos, float integrity, MultiblockState state) {
        if (level != null) {
            MultiblockSavedData.get(level).record(definitionId, controllerPos, integrity, state == null ? "FORMED" : state.name());
        }
    }

    public static void remove(ServerLevel level, BlockPos controllerPos) {
        if (level != null) {
            MultiblockSavedData.get(level).remove(controllerPos);
        }
    }

    public static List<MultiblockSavedData.Entry> formed(ServerLevel level) {
        return level == null ? List.of() : MultiblockSavedData.get(level).entries();
    }
}
