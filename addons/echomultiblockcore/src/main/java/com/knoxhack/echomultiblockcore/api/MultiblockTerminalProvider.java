package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface MultiblockTerminalProvider {
    MultiblockTerminalProvider NOOP = player -> List.of();

    default Identifier providerId() {
        return MultiblockIntegrationServices.generatedProviderId(this, "terminal");
    }

    List<MultiblockStatusSnapshot> snapshots(Player player);
}
