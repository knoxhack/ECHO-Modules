package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface MultiblockDataCoreProvider {
    MultiblockDataCoreProvider NOOP = player -> List.of();

    default Identifier providerId() {
        return MultiblockIntegrationServices.generatedProviderId(this, "data_core");
    }

    List<MultiblockRuntimeSnapshot> snapshots(Player player);
}
